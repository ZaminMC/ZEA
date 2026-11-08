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
  (`ChannelChat`, `PrivateMessage`, `VoteMessage`, `CrateWinMessage`,
  `DeathMessage`, `SystemMessage`, `WritingChannelReport`,
  `DiscordMessage`) plus `None`. Sealed so consumers (feeds,
  notifications, history) pattern-match exhaustively.
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
8. `GlobalChatClassifier` — weakest anchor (bracket + name + colon), so
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

- **Local / Trade / Nation / Town message formats are not yet known.** No
  observed samples exist, so no classifiers were written for them;
  messages of those kinds currently land in UNCLASSIFIED. Fixtures from
  in-game capture are the prerequisite. Risk to note: if these channels
  use bracket-leading markers like `[Town] Name: message`, the global
  classifier would currently claim them (a bracket parses as an
  affiliation). The ZEA-REPO channels dataset is the planned data-driven
  mechanism to carry per-channel message patterns so this can be
  corrected without a mod release.
- **Component tree structure of EarthMC messages is unverified** (we have
  rendered text, not the wire components). The parser is component-first
  where structure exists and text-anchored otherwise; in-game captured
  component fixtures will refine this.
- Nickname → real username resolution (Stage 4, needs API + component
  investigation).
