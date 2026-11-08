# ZEA Architecture

This is the permanent architectural reference for ZEA. It documents how the
system is constructed and why. When an architectural decision changes, this
document must be updated in the same change.

Everything below reflects verified decisions. Verified facts are sourced in
[WORKLOG.md](WORKLOG.md) and [docs/](docs/). Anything not yet verified is
explicitly listed as unresolved rather than assumed.

## 1. Overview

ZEA is a client-side Fabric mod for Minecraft 26.3 (Java 25, official Mojang
mappings — Yarn mappings no longer exist past Minecraft 1.21.11). It integrates
with the EarthMC server purely through:

1. **Observed chat output** — messages received from the server.
2. **The official EarthMC HTTP API (v4)** — public data plus optional, explicit
   player-authorized key access.
3. **User-initiated chat commands** — ZEA only sends commands the user asked
   for, through the same path as manual typing.

ZEA must never require server-side installation and never automates player
communication unless the user explicitly configures it.

## 2. Source and build layout

- **Single Gradle project**, one Minecraft version (26.3). Multi-version builds
  (à la Chatting/Stonecutter) are explicitly rejected for now: one target,
  one build, until there is a concrete requirement.
- **Single source set** (`src/main/java`) with `"environment": "client"` in
  `fabric.mod.json`. ZEA is 100% client code; a split main/client source set
  would carry an empty common side forever.
- All mixins (when they appear) live under `net.zaminmc.zea.mixin` and are
  configuration-only shims: they capture state and forward it into the
  core. No business logic lives in a mixin. Message capture currently
  needs none — Fabric's `ClientReceiveMessageEvents` is the official hook;
  the chat tab strip uses one `ChatScreenMixin` to add its widget.
- **Mixins need no refmap.** Minecraft 26.3 ships unobfuscated (its version
  manifest has no `client_mappings`/`server_mappings` downloads), so the
  official Mojang names ZEA compiles against are the runtime names. Loom
  performs no intermediary remap and the built jar works as-is; annotation
  strings like `method = "init"` are valid in production.

## 3. Module responsibilities

Packages represent responsibilities. The mapping:

| Package | Responsibility |
|---|---|
| `net.zaminmc.zea` | Entrypoint and mod identity only |
| `net.zaminmc.zea.lifecycle` | Client lifecycle: init, shutdown, server join/leave |
| `net.zaminmc.zea.mixin` | Minecraft integration boundary (mixins/accessors) |
| `net.zaminmc.zea.chat.capture` | Incoming message capture (from mixins) |
| `net.zaminmc.zea.chat.model` | Immutable message + structured domain models |
| `net.zaminmc.zea.chat.classify` | Message classification and EarthMC parsing |
| `net.zaminmc.zea.chat.feeds` | Feed model, tab state, filtering, outgoing routing |
| `net.zaminmc.zea.chat.render` | Chat presentation: tabs, smooth chat, search, copy |
| `net.zaminmc.zea.chat.history` | Session and persistent chat history, search |
| `net.zaminmc.zea.notify` | Central notification coordinator and destinations |
| `net.zaminmc.zea.mention` | Mention detection and (opt-in) responses |
| `net.zaminmc.zea.player` | Player hover cards, username resolution |
| `net.zaminmc.zea.buttons` | Inventory buttons |
| `net.zaminmc.zea.share` | HelpChat paste client, share schemas, import/export |
| `net.zaminmc.zea.earthmc` | EarthMC API client, server identity, ZEA-REPO data |
| `net.zaminmc.zea.config` | Configuration (MoulConfig) |
| `net.zaminmc.zea.persistence` | Atomic file IO, JSON store utilities |
| `net.zaminmc.zea.util` | Small shared utilities only |

Rules enforced on this layout:

- No god classes. If a class needs a `Manager` suffix and a static instance,
  its responsibilities are probably not drawn correctly.
- Render classes (`*.render`) never parse messages, own domain state, or make
  decisions. They read view models and draw.
- No circular package dependencies. Dependency direction is:
  `mixin → capture → classify → model → {feeds, history, render}` with
  `config`, `persistence`, `earthmc`, `share`, `notify` as leaf services that
  depend on models but never on rendering.
- UI components never call EarthMC parsing or networking directly.

## 4. Message pipeline (event flow)

```
Minecraft chat component
        │
        ▼
  Message capture ── mixin boundary; forwards on the client thread
        │              with no interpretation
        ▼
  CapturedMessage ── immutable: original Component (never destroyed),
        │             text, timestamp, connection context
        ▼
  Classification ─── chain of classifiers, each declaring confidence
        │             and a target feed; failure = pass-through
        ▼
  ClassifiedMessage  structured model (badges, nation/town, sender,
        │             direction, event type) + original component kept
        ▼
  Feed filtering ─── which open feeds display this message
        │
        ▼
  Presentation ───── rendering, interactions, notification triggers
        │
        ▼
  History ────────── session ring buffer + async persistent store
```

Invariants:

- The original `Component` stays attached to every representation of the
  message. Normalized models are additive, never replacements.
- A message that cannot be classified is always preserved and shown in the
  unclassified/general feed. Classification failure must never make a message
  disappear.
- Parsing works on component structure first (styles, hover/click events,
  siblings), plain-text regexes only as a last resort. EarthMC formats vary;
  a single brittle regex is forbidden.

## 5. Client lifecycle

`ZeaClient` (client entrypoint) performs wiring only. Lifecycle-sensitive work
is owned by `lifecycle`:

- **Init**: register mixins' callbacks, load configuration, prepare (not
  execute) async infrastructure.
- **Server join**: detect EarthMC (verified server address match), establish
  server identity, activate EarthMC-specific subsystems.
- **Server leave / disconnect**: cancel in-flight multi-channel dispatches,
  stop mention auto-responses, flush pending persistence writes.
- **Shutdown**: flush and close persistence, shut down bounded executors.

Every subsystem must tolerate a disconnect at any point. Multi-channel message
dispatch is cancelled on disconnect by design.

## 6. Threading model

| Work | Thread |
|---|---|
| Message capture, classification, rendering | Client thread only |
| Persistence writes | Single dedicated executor, sequential per store |
| History queries for search | Background executor, results marshalled back |
| EarthMC API + ZEA-REPO + HelpChat HTTP | Shared bounded executor, never the client thread |

Rules:

- No filesystem or network IO on the render/client thread. Ever.
- All executors are bounded and named. No thread-per-request, no unbounded
  queues. A rejected work item is logged and dropped, never blocks rendering.
- HTTP clients use explicit connect/read timeouts and always close response
  bodies.

## 7. Data ownership

- The **original message component** is owned by the pipeline record; nothing
  may mutate it.
- **Server writing channel state** (what channel the server will write to) is
  owned by `chat.feeds` and updated only from verified server responses
  (e.g. `You are currently in global (write) and 5 other channels`) or from
  acknowledged channel-switch commands. It is never inferred from the most
  recently received message.
- **Visual feed selection** (which tab is displayed) is owned by the UI state.
- **Multi-send target selection** (shift-click selection) is owned by the
  outgoing routing service.
- These three are deliberately separate state variables. Overloading them into
  one is the primary modeling error this architecture exists to prevent.

## 8. Persistence boundaries

All persistent stores (chat history, inventory buttons, tab configuration,
share caches) go through `persistence` which implements a single safe-write
protocol (the pattern proven by NEU's `ConfigUtil`, verified in research):

1. Serialize completely in memory.
2. Write to `<file>.tmp` alongside the target.
3. Re-parse the temp file to verify it round-trips.
4. Atomically move over the target (`ATOMIC_MOVE` where supported, with
   `REPLACE_EXISTING` fallback).
5. On load: if parsing fails, rename the corrupt file aside (`*.corrupted`),
   back it up, and start from defaults — never execute or apply partially
   parsed configuration.

Additional rules:

- Imports never execute commands; they validate first and apply fully or not
  at all (documented import policy per share type).
- Retention and cleanup are enforced on a schedule, not on every write.

## 9. Configuration

Framework: **MoulConfig 4.8.0 (`modern-26.3`)** — verified published for
Minecraft 26.3 at `https://maven.notenoughupdates.org/releases/`, LGPL-3.0-or-later.
Integration mode: Fabric jar-in-jar (`include`), keeping MoulConfig as its own
unmodified mod jar (clean LGPL compliance, idiomatic Fabric). If JiB proves
unworkable at build time, the documented fallback is shadow+relocate following
MoulConfig's official recipe; OneConfig is excluded by project constraint.

- One config class per logical category (General, Chat, Channels,
  Notifications, Mentions, Player hover, Chat history, Inventory buttons,
  Sharing, Advanced).
- Potentially disruptive behavior defaults to **off**: mention auto-responses,
  multi-channel sending, persistent history beyond the selected retention.
- Schema versioning with migration support; old settings are migrated, never
  silently dropped.

## 10. Networking architecture

Three independent networked subsystems, none of which know about each other:

| Subsystem | Purpose | Notes |
|---|---|---|
| EarthMC API client (`earthmc`) | v4 REST + SSE | Public endpoints; `/api key` based auth for private data; keys stored locally, never shared; 180 req/min budget enforced client-side |
| ZEA-REPO client (`earthmc`) | Manifest-driven data download | Static GitHub raw files; no server backend |
| HelpChat paste client (`share`) | Config sharing | `paste.helpcha.at`, exact-hostname allowlist, HTTPS only, size limits |

Common rules: bounded executor, explicit timeouts, validated responses, no
render-thread IO, graceful failure when the service is unavailable.

## 11. Notifications

A single **notification coordinator** owns the action bar, title and subtitle.
PM notifications and mention notifications both target these surfaces, so no
feature may write to them directly. The coordinator implements the smallest
sensible policy:

- Sources declare priority (PM > mention) and duration.
- A higher-priority notification replaces a lower one; equal priority queues.
- Optional persistence (e.g. PM notice until replied) with explicit expiry.
- Disconnect clears all notifications.

The priority set is deliberately tiny and documented here; extending it
requires updating this document.

## 12. Extension points

- **Classifier chain**: new message families plug in as classifiers with
  fixtures; unknown messages fall through by default.
- **Feed registry**: feeds are registered declaratively; tabs render from the
  registry plus user visibility config.
- **Share schemas**: versioned `zea-share` documents; new categories add a
  schema + validator, never mutate existing ones incompatibly.
- **ZEA-REPO datasets**: new datasets appear in the repo manifest; ZEA reads
  the manifest and only consumes datasets it understands.

## 13. Error handling

- Classification errors degrade to "unclassified", never to "hidden".
- Persistence errors fall back to defaults with the corrupt file preserved.
- Network errors surface as user-visible failures in the originating feature
  (chat message or config screen), never as silent logs, never as crashes.
- Imported data that fails validation is rejected wholesale with the reason.

## 14. Testing strategy

JUnit 5 unit tests, no Minecraft runtime needed for the core:

- **Classification fixtures**: every known EarthMC message family as recorded
  component fixtures; regression tests per family plus malformed variants
  (missing badges, missing town, nickname cosmetics).
- **Feed filtering**: aggregation, multi-selection, unknown fallback, outgoing
  routing.
- **Notification coordinator**: overlap, priority, persistence, expiry,
  disconnect.
- **Persistence**: safe-write protocol, corruption recovery, migration.
- **Sharing**: mock HTTP server (embedded) for paste upload/download paths;
  ordinary tests never touch the real service.

Minecraft-rendering behavior (mixins, scrolling, hitboxes, hover/click,
inventory rendering, screen transitions) is verified manually in-game and
tracked per release in WORKLOG.md. Compilation alone is never claimed as
in-game verification.

## 15. Architectural constraints

1. Never confuse visual feeds with server channels.
2. Never let rendering own business logic.
3. Never block the client thread on IO.
4. Never discard unknown messages.
5. Never let imported configurations execute commands.
6. Never upload private configuration or credentials.
7. No god classes, no giant static managers, no circular dependencies.
8. No generic frameworks without a concrete requirement.
9. One coherent change per commit, LuckPerms-style engineering discipline.

## 16. Unresolved (recorded, not assumed)

- Exact EarthMC server identity key for history isolation (candidate: server
  address + verified EarthMC detection; to be settled in Stage 7).
- Nickname → real username resolution source (needs component + API
  investigation in Stage 4).
- Whether additional writable channels exist beyond Global/Local/Trade/
  Nation/Town (to be verified in-game; ZEA-REPO `channels` dataset exists so
  the list can be corrected without a mod release).
