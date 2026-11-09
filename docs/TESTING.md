# Testing

Tests are part of implementation, not a finishing step. ZEA uses JUnit 5.

## Layout

- Unit tests live in `src/test/java` mirroring package structure.
- Fixtures (recorded message families, corrupted-store samples, share
  documents) live in `src/test/resources/fixtures/`.
- Tests must not require a Minecraft client, network access, or a specific
  machine.

## Required coverage per subsystem

### Message classification (Stage 2)

Fixture families: global (with/without badges, multiple badges, nickname
cosmetics, missing town), local, trade, nation, town, badges, nicknames,
vote, crate win (variable amounts/names), death (hover preserved), PM in/out
(not every arrow is a PM), Discord, system, AFK, writing-channel report,
unknown. Malformed/missing-component variants for every family.

### Feed filtering (Stage 3)

Individual feed filtering, All aggregation, unknown fallback, multi-selected
tabs, channel switching, outgoing routing, conservative multi-send dispatch
(interval, cancellation on disconnect, partial-failure reporting).

### Notifications (Stage 5)

Overlapping PM + mention, multiple mentions, persistent PM vs temporary
mention, priority replacement, expiration, disconnect clearing.

### Persistence (Stages 6–7)

Normal shutdown, simulated unexpected shutdown (interrupt mid-write),
corrupted files, missing files, configuration migration, history recovery,
atomic replacement semantics.

### Sharing (Stage 8)

Upload: valid response, missing key, invalid JSON, HTTP errors, timeout.
Import: invalid URLs, invalid keys, oversized documents, unsupported schema
versions, malformed configuration, secret exclusion, cancellation.
Networked paths run against an **embedded mock HTTP server**
(`com.sun.net.httpserver.HttpServer`); ordinary unit tests never touch
`paste.helpch.at`.

## Minecraft integration verification

Rendering and interaction behavior (mixins, chat scrolling, hitboxes,
hover/click events, chat dimensions, inventory rendering, screen transitions,
multiplayer disconnects) is verified manually in-game on the target version.
Results are recorded in WORKLOG.md per release. Compilation and unit tests
are never claimed as in-game verification.

The multi-send interaction round (after dev.17) covers: shift-click
toggling on each default tab (All and PMs must refuse with the read-only
toast), the aqua marker on targeted tabs, plain-click select while
targets are active, sending with targets selected (staggered server
echoes, up-arrow recall of the plain text, close-on-submit unchanged),
slash commands and blank sends staying vanilla, the acceptance toast,
and target clearing on disconnect.

Component-level fixtures come out of that round through `/zea capture`
(see CHAT_CLASSIFICATION.md): captured files load through the test-side
`CaptureFixtures`, which the bootstrap helper (`VanillaBootstrap`) makes
possible in a bare test JVM.
