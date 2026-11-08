# Chat channels and feeds

How EarthMC's server channels, ZEA's visual feeds, and outgoing routing
relate — and why they are three separate things. Stage 3 implementation.

## The three states (never one variable)

| State | Class | Meaning | Changed by |
|---|---|---|---|
| Server writing channel | `ChannelState` | what the server writes to on plain chat | **only** the server's report message; cleared on disconnect |
| Display feed selection | `FeedSelection` | which feed the player is viewing | the player (tab click) |
| Multi-send targets | `SendTargets` | explicit shift-click destinations for one send | the player (shift-click toggle) |

The server writing channel is never inferred from received messages,
never assumed on join (players do not necessarily start in Global), and
never changed by selecting a visual tab.

## Server channels (writable)

Verified table (mirrors ZEA-REPO `datasets/channels.json`, which becomes
the runtime source when the repository client exists):

| Channel | Per-message command |
|---|---|
| Global | `/g <message>` |
| Local | `/local <message>` |
| Trade | `/tradechat <message>` |
| Nation | `/nc <message>` |
| Town | `/tc <message>` |

`WriteChannel` models only the per-message command form. Standalone
switch commands (e.g. `/global`, which changes the server's writing
channel) are deliberately **not** actions ZEA performs: if the player runs
one themselves, the server's next writing-channel report updates
`ChannelState`. ZEA routing a message through `/g` never changes server
state — this is the accurate model, and it means there is no "transition"
to get wrong.

## Visual feeds

`Feed` = id + label + set of categories. Registry (`Feeds`):

- **Default tabs**: All, Global, Local, Trade, Nation, Town, PMs.
- **Extra feeds** (accessible, hidden until configured): Discord, Votes,
  Crate Wins, Deaths, System, General (unclassified).
- `All` aggregates every category including unclassified. It is a view,
  not a channel — there is no send-to-all.

## Outgoing routing

`OutgoingRouter` turns (targets, message) into `OutgoingMessage`s using
the per-message command form. Single target: one command. Multi-target:
one command per destination, in selection order.

### Multi-channel dispatch (`MultiChannelDispatcher`)

Explicit multi-target sends are staggered:

- First message goes out on the next client tick; each subsequent message
  waits for the interval (default **1000 ms**, conservative against server
  spam limits; becomes configurable when the options UI lands).
- Timing is uniform and documented — automated sending is never disguised
  as human typing.
- A send failure (e.g. disconnect) cancels all remaining messages and is
  reported (currently logged; surfaced in the chat interface in Stage 4).
- `cancel()` (disconnect wiring, user cancel) stops everything pending.
- A new batch replaces (cancels) the previous one, still respecting the
  interval from the last send.

The dispatcher is a tick-driven state machine (`tick(nowMillis)`), wired
to `ClientTickEvents.END_CLIENT_TICK` in `ChatSubsystem`; sends go through
`ClientPacketListener#sendCommand` on the client thread.

## What is not here yet

- Tab rendering and the send UI (Stage 4) — the model is ready for it.
- MoulConfig options for interval and default-visible tabs.
- Whether the server reports the writing channel on join (unverified) —
  until observed, `ChannelState` starts unknown after each join.
