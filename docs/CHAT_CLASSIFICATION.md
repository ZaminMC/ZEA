# Chat classification

How incoming EarthMC messages become structured, classified data. This
documents the Stage 2 implementation; message shapes are recorded in
[EARTHMC_MESSAGE_FORMATS.md](EARTHMC_MESSAGE_FORMATS.md).

## Pipeline

```
Fabric ClientReceiveMessageEvents (GAME + CHAT, non-overlay)
        │  MessageCapture — gated on EarthMC connection
        ▼
CapturedMessage            immutable: original Component + flattened text + timestamp
        │  ClassificationPipeline — fixed order, first confident match wins
        ▼
ClassifiedMessage          category + structured payload, original always retained
```

Capture uses Fabric's official client message events — no custom mixins.
Overlay (action bar) messages are skipped: they are not chat.

## Model

- `CapturedMessage` — the original `Component` (formatting, colors, hover
  and click events, insertion — never destroyed), the flattened text
  computed once, and the receive timestamp.
- `MessageCategory` — GLOBAL, LOCAL, TRADE, NATION, TOWN, PRIVATE_MESSAGE,
  DISCORD, VOTE, CRATE_WIN, DEATH, SYSTEM, UNCLASSIFIED.
- `MessagePayload` — sealed interface; one record per structured family
  (`ChannelChat`, `TaggedChannelChat`, `PrivateMessage`, `VoteMessage`,
  `CrateWinMessage`, `DeathMessage`, `SystemMessage`,
  `WritingChannelReport`, `DiscordMessage`) plus `None`. Sealed so
  consumers (feeds, notifications, history) pattern-match exhaustively.
- `ClassifiedMessage` — capture + category + payload.

## Confidence policy

Confidence is binary: a classifier either recognizes the message with an
anchored structural pattern (full match, from the observed shape) and
returns a result, or declines. There is no fuzzy partial matching, so a
match is always confident and there is nothing to weigh. The guaranteed
fallback is the point: a message no classifier recognizes is preserved as
`UNCLASSIFIED` with payload `None` — classification failure can never make
a message disappear. If a future classifier genuinely needs graded
confidence (e.g. heuristic nickname resolution), `Result` gains a
confidence field and the pipeline policy is documented here at that time.

## Classifier order

Specific before generic; the first match wins:

1. `WritingChannelReportClassifier` — the server's writing-channel report
   is the source of truth for channel state; most specific shape.
2. `PrivateMessageClassifier` — anchored `[name → me]` header.
3. `DeathClassifier`, 4. `VoteClassifier`, 5. `CrateWinClassifier` —
   anchored event shapes.
6. `DiscordClassifier` — `[Discord]` prefix; must run before global so a
   Discord header is never read as an affiliation bracket.
7. `SystemClassifier` — `EarthMC » ` prefix.
8. `TaggedChannelChatClassifier` — local/trade/nation/town; exact bracket
   label at the start of the message, so it must run before global (which
   would otherwise read the bracket as an affiliation).
9. `GlobalChatClassifier` — weakest anchor (bracket + name + colon), so
   last.

## Global chat parsing

Text-level anchored segmentation (badge prefix → `[Nation|Town]` → display
name → `: ` → content), with component enrichment for badges:

- **Badges** (`BadgeExtractor`): components are walked first — EarthMC
  builds messages from siblings, and badges carry their own hover/click
  events, so siblings lying wholly inside the badge region become badges
  with their original components (events preserved, e.g. the Snowstorm
  store link). A flat single-component message falls back to greedy
  known-symbol matching (nothing to preserve there — flat components
  cannot carry per-badge events). A multi-symbol sibling is kept as one
  cluster badge with no invented identity. Unknown symbols are never
  assigned meanings.
- **Affiliation**: `[nation|town]` fills both; a single-segment bracket
  keeps the raw value without guessing whether it names a nation or a
  town (unresolved until observed/API data — see open items).
- Bracket-less messages are **declined**: `Name: content` alone is too
  weak an anchor, so such messages stay unclassified rather than
  misclassified.

## Tagged channel parsing (local / trade / nation / town)

Text-level anchored parsing, mirroring the global rules:

- **Tag**: the bracket must open the message (`[Local]`, `[Trade]`,
  `[Town Chat]` are exact matches; nation chat continues with
  `|Town`, where the town is the sender's). The nation form without a
  town is unobserved and declined.
- **Name and content**: one space, then the display name up to the first
  `": "`, then the content — same rules as global (no brackets in the
  name, first separator wins).
- No badges have been observed on these channels, so none are parsed;
  the original component is retained regardless.

Residual ambiguity, accepted and documented: a nation or town actually
named "Local", "Trade" or "Town Chat" would be claimed by the tag
classifier before global could see it. EarthMC names are geographic and
none collide with these labels in practice; the tags are mirrored from
ZEA-REPO `datasets/channels.json` so a wrong tag is corrected in data,
not code, once the repository client exists.

## Capturing component samples

Component-level ground truth — the wire components with their hover and
click events, sibling structure and per-segment styles — cannot be
reconstructed from logs. `/zea capture` records it:

1. Join EarthMC and run `/zea capture on`. Messages are recorded to
   `config/zea/capture/capture-<timestamp>.json` — plain text, legacy
   codes, the classification, and the component as vanilla JSON.
2. Reproduce the messages worth capturing (a badge with hover text, a
   nickname, nation chat without a town, a death message).
3. Run `/zea capture off`. A session stops on its own after 100
   messages, and disconnects only flush it — the session survives a
   rejoin.

Capture files are `zea-capture` documents; `CaptureFixtures` (test
sources) loads them back into live components, so a file captured
in-game drops straight into a regression test. Entries whose component
cannot serialize (item hovers need registries) keep their text and the
error instead. Capture is a local diagnostic: off unless started, never
uploaded, never part of share documents.

## Known badges

`KnownBadges` mirrors the ZEA-REPO `datasets/badges.json` ids exactly
(anniversary-globe, server-heart, supporter, rabbit, golden-hammer,
dragon, new-player, top-10-mining, snowstorm). ZEA-REPO becomes the
runtime source once the repository client exists; until then the verified
list ships in code. The list is not assumed exhaustive.

## Fixtures and regression tests

- `src/test/resources/fixtures/messages.json` — one entry per observed
  message family, run through the full pipeline by
  `ClassificationFixturesTest` (category + original-retention assertions).
- Per-classifier tests cover structure-dependent behavior: badge
  extraction with hover/click preservation, single-segment affiliations,
  nickname display names, non-PM arrows, variable crate amounts, optional
  `/vote` suffixes, hover preservation for deaths, Discord-before-global
  ordering.

New families are added by: recording the observed message as a fixture,
adding a classifier + tests, and documenting the shape in
EARTHMC_MESSAGE_FORMATS.md.

## Open items (unresolved, never assumed)

- **Nation chat without a town** is unobserved; it is declined by the
  tag classifier and would currently be claimed by the global
  classifier's single-segment affiliation handling. An observed fixture
  will settle the real shape.
- **Component tree structure of EarthMC messages is unverified** (we have
  rendered text and legacy codes, not the wire components). The parser is
  component-first where structure exists and text-anchored otherwise;
  `/zea capture` now provides the recording tool, and captured files
  will refine this once a session on EarthMC produces them.
- Nickname → real username resolution (Stage 4, needs API + component
  investigation).
