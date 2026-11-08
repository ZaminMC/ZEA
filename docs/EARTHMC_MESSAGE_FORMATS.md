# EarthMC message formats

Observed EarthMC chat message structures, recorded as fixtures for the
classifier (Stage 2 — implemented, see
[CHAT_CLASSIFICATION.md](CHAT_CLASSIFICATION.md)). These are **observed
samples, not universal templates**: formats vary (badges come and go, towns
may be absent, nicknames carry per-character formatting). Parsers read
component structure first (styles, siblings, hover/click events) and anchor
on text structure; unknown messages are never dropped.

Each family below has a fixture in `src/test/resources/fixtures/messages.json`
and a regression test.

## Global

```
🌎❣ [Yugoslavia|Pest] GavriloPrincip: lemme speak with his driver rq
```

Components, in order: zero or more **badges** (emoji, optional hover
descriptions, the snowstorm badge carries a clickable store link), then
`[Nation|Town]` (nation and town stored separately; town may be absent;
original colors preserved), then the display name (may be a nickname with
per-character formatting), then `:` and the message.

Variants observed: multiple badges, no badges, different badge/town colors,
nicknames. Do **not** build one regex assuming a fixed shape.

## Badges (known so far — see ZEA-REPO `datasets/badges.json`)

| Symbol | Meaning |
|---|---|
| 🌎 | EarthMC 10th anniversary Globe |
| ❣ | Server Heart |
| + | Supporter |
| 🐇 | Rabbit |
| 🔨 | Golden Hammer |
| 🐉 | Dragon |
| 🎉 | New Player |
| 💎 | Top 10 Mining |
| 🔥 | Snowstorm badge |

Badge model preserves: symbol, style, hover description, click event, and a
known semantic identity **only when verified**. Unknown symbols pass through
untouched with no invented meaning. The list is not assumed exhaustive — the
ZEA-REPO dataset exists so it can be corrected without a mod release.

## Nation / town segment

`[Yugoslavia|Pest]` — split on the `|` into nation and town. Not a username,
not a message prefix. Town may be missing entirely.

## Nicknames

Displayed name may differ from the account name (observed: displayed
`Coriolanus`, actual `pengpy`). Rules: option to show the real username when
an authoritative source identifies it (EarthMC API by name/UUID); never
invent a username; keep the nickname and its cosmetics available.

## Vote messages

```
AdamGladio voted and received a gold crate /vote
```

Class: Votes. Kept out of the main Global feed by default.

## Crate wins

```
akirarocks won 16 gold from a bruh box
jediknight7491 won 32 gold from a gold crate /vote
```

Class: Crate Wins. Amounts and crate names are parsed, never hardcoded.

## Deaths

```
☠ xfw_x was killed by dex247
☠ CherryPinkVelvet was killed by FORDaLoZ
```

Class: Deaths. Hover information (e.g. `Damage dealt: FORDaLoZ: 8.5 hearts
(100%)`) must be preserved with the message.

## Private messages

```
[FORDaLoZ → me] hi
[me → FORDaLoZ] please reply with hi
```

Class: Private Messages. Parse sender, recipient, direction, content,
timestamp. Clicking a sender opens the existing PM interaction. Not every
message containing an arrow is a PM — direction is only inferred from this
exact structure.

## System messages

```
EarthMC » You are now AFK.
EarthMC » You are no longer AFK.
```

Class: System. Includes the writing-channel report:

```
You are currently in global (write) and 5 other channels
```

This response is the source of truth for the current **server writing
channel** — it must be detected and applied to the client's channel model.
The writing channel is never inferred from the most recently received
message, and players do not necessarily start in Global.

## Discord

Messages forwarded from Discord carry a `[Discord]` prefix. Class: Discord.
Players reply through normal in-game chat; **there is no `/discordchat`
command** and ZEA must never invent one. Discord is an incoming category
only — it is not a writable channel.

## Channels (writable, server-side)

| Channel | Commands |
|---|---|
| Global | `/g`, `/global` |
| Local | `/local` |
| Trade | `/tradechat` |
| Nation | `/nc`, `/nationchat` |
| Town | `/tc` |

Standalone commands like `/global` **change the server's writing channel**;
short forms like `/g <message>` send a single message without switching.
Selecting a visual tab in ZEA does not silently change the server's writing
channel; the transition must be explicit and correct (Stage 3). The channel
list is data (ZEA-REPO `datasets/channels.json`) precisely because it may
change server-side.

## Unclassified

Anything not confidently matched lands in the general/unclassified feed and
stays visible. This is an invariant, not a fallback of last resort.

## Unresolved — formats still needing captured samples

The following have **no observed samples yet** and therefore no classifiers
(they currently land in Unclassified). Add fixtures here as they are
captured in-game:

- Local channel messages.
- Trade channel messages.
- Nation channel messages.
- Town channel messages.
- The exact component tree (sibling/style structure) behind each observed
  text format — text-level fixtures exist; component-level structure needs
  in-game capture.
- Whether bracket-less global messages exist (nationless/townless
  senders).
