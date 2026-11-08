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

## Stage 1 — Core foundation ⏳

Clean package boundaries, client lifecycle integration, configuration
infrastructure (MoulConfig), persistence infrastructure (safe-write
protocol), event/message-capture boundaries, test infrastructure.

**Deliverable:** stable, testable foundation.

## Stage 2 — Message model and classification

Capture incoming messages preserving original components; immutable
normalized representation; EarthMC-specific classification; structured
parsing (badges, nation/town, nicknames, events, PMs, system); unknown
fallback; regression fixtures for every known family.

**Deliverable:** reliable EarthMC message classification.

## Stage 3 — Chat feeds and routing

Channel/feed model, tabs, All aggregation, current server writing-channel
detection, outgoing channel routing, multi-channel selection with
conservative configurable dispatch.

**Deliverable:** separated and correctly routed EarthMC chat.

## Stage 4 — Chat presentation and QoL

Smooth chat, search (Ctrl+F), message copying, chat dimensions, configurable
appearance, player hover presentation, real-username display option.

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
