# ZEA Roadmap

Staged development plan. Stages move forward only when the previous stage's
correctness problems are resolved. Status is updated as work happens; the
authoritative record of what was actually done is [WORKLOG.md](WORKLOG.md).

## Stage 0 — Research and documentation ✅

Inspect the repository, LuckPerms engineering practices, reference mods
(Chatting, EarthMC Recruitment Addon, NotEnoughUpdates, Inventory Buttons),
MoulConfig compatibility, EarthMC API access. Establish permanent
documentation and the roadmap.

**Deliverable:** reviewed architecture and permanent documentation.

## Stage 1 — Core foundation ✅

Clean package boundaries, client lifecycle integration, configuration
infrastructure (MoulConfig), persistence infrastructure (safe-write
protocol), event/message-capture boundaries, test infrastructure.

Done: build toolchain (Gradle 9.8.2/Loom 1.19/Java 25, verified compiling),
MoulConfig 4.8.0 dependency (jar-in-jar), client lifecycle (join/leave/
stopping), EarthMC detection, safe persistence utilities, message capture
via Fabric client receive events, JUnit test infrastructure. MoulConfig
config classes are added as features begin to need options (Stage 3+).

**Deliverable:** stable, testable foundation.

## Stage 2 — Message model and classification ✅ (parsers pending fixtures)

Capture incoming messages preserving original components; immutable
normalized representation; EarthMC-specific classification; structured
parsing (badges, nation/town, nicknames, events, PMs, system); unknown
fallback; regression fixtures for every known family.

Done: all of the above for every family with observed samples (14 fixture
families, 83 tests passing). Pending: local/trade/nation/town parsers and
component-level fixtures — blocked on in-game captured samples, tracked in
docs/CHAT_CLASSIFICATION.md.

**Deliverable:** reliable EarthMC message classification.

## Stage 3 — Chat feeds and routing ✅

Channel/feed model, tabs, All aggregation, current server writing-channel
detection, outgoing channel routing, multi-channel selection with
conservative configurable dispatch.

Done: feed model and registry (All aggregation, default tabs, extra
feeds), the three deliberately separate states (server writing channel /
display selection / send targets), per-message outgoing routing,
staggered multi-channel dispatch with configurable interval (MoulConfig),
multi-send gated behind configuration (off by default), ChatSubsystem
wiring. See docs/CHAT_CHANNELS.md.

**Deliverable:** separated and correctly routed EarthMC chat.

## Stage 4 — Chat presentation and QoL ⏳ (core done)

Smooth chat, search (Ctrl+F), message copying, chat dimensions, configurable
appearance, player hover presentation, real-username display option.

Done: chat tab bar (EarthMC-only, flat design), feed filtering through
the official `setVisibleMessageFilter` hook (identity-keyed
classification cache), message copying (right-click plain,
alt+right-click formatted, legacy serializer, confirmation toast),
chat search (Ctrl+F, composes with the selected feed, survives resize,
clears on close/disconnect), and smooth chat (quartic ease-out for new
messages, wheel/page scrolling and the close-screen reset; configurable
duration, on by default). Chat dimensions need no ZEA work: vanilla 26.3
ships width and focused/unfocused height options. Remaining:
configurable controls, player hover, username option.

**Deliverable:** modernized chat interface.

## Stage 5 — Notifications

PM classification/feed/notifications, mention highlighting/sounds/destinations,
central notification coordinator, optional automatic mention responses
(disabled by default, loop/cooldown-guarded).

**Deliverable:** reliable notification system.

## Stage 6 — Inventory buttons

Button model, rendering, positioning, configuration, editing, safe
persistence (atomic writes, validation, no command execution on import).

**Deliverable:** stable inventory-button functionality.

## Stage 7 — Persistent history

Server identity key, session storage, persistent storage, search, retention,
recovery. Isolated to the EarthMC server context.

**Deliverable:** EarthMC-specific persistent chat history.

## Stage 8 — HelpChat configuration sharing

Versioned JSON share schemas, export, paste upload, import with validation
and preview, safe application, mock-server tests.

**Deliverable:** working configuration sharing through HelpChat.

## Beyond Phase 1

Future EarthMC integrations (map/towny-aware features, API-driven player
cards) are out of scope for Phase 1 and will be planned when the chat
subsystem is complete.
