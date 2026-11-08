# ZEA Worklog

Chronological record of actual work: research, findings, decisions, files
changed, tests performed, results, outstanding problems. Newest entries last.

---

## 2026-11-08 — Stage 0: research, documentation, licensing, ZEA-REPO design

### Repository state

- `ZEA` repository was **empty** (no branches, no commits). No existing work
  to preserve. Project starts from scratch: Java + Fabric, per spec.
- `ZEA-REPO` existed with one commit on `main` (MIT `LICENSE` +
  one-line `README.md`).
- Work happens on `develop` (ZEA). `main` is never touched.

### Research performed and findings

**LuckPerms engineering practices** (target standard for commits):

- Commit subjects: imperative mood, capitalized verb, no conventional-commit
  prefixes, no bodies (0 of the last 80 commits had one). Typical verbs:
  Add / Update / Fix / Improve / Use.
- Size: median 4 files / 37 lines; 41% of commits ≤ 20 lines. Large commits
  exist but are bulk refactors or platform ports, never mixed concerns.
- Regression tests ship **in the same commit** as the fix.
- Refactors are isolated, behavior-preserving, often net-negative in lines.
- Dependency bumps get their own commit ("Update Gradle to v9.4").

**Reference mods:**

- *Polyfrost/Chatting 3.2.5* — verified to support MC 26.3 (Stonecutter
  multi-version). Useful machinery to extract conceptually: tab model with
  prefix-based outgoing routing, shift-click multi-select, versioned JSON
  config migration chain, quartic ease-out smooth-chat translation, custom
  chat width + separate focused/unfocused heights, mixin surface
  (`ChatComponent#render/addMessage/scrollChat/getWidth/getHeight/...`).
  Its config framework is OneConfig — **not usable here** (project
  constraint); its default tab regexes are Hypixel-specific — not portable.
- *JR1258/EarthMC-Recruitment-Addon* — targets 1.21.11 only, but the API
  access pattern is verified and current: `https://api.earthmc.net/v4`,
  `POST /players {"query":[...]}`, `java.net.http.HttpClient` on virtual
  threads, 60s profile TTL, batch cap 50, single-flight fetch flag.
- *NotEnoughUpdates* — inventory buttons: model `{x, y, playerInvOnly,
  anchorRight, anchorBottom, backgroundIndex, command, icon}`; rendering via
  post-draw hooks; editor without drag repositioning; clipboard
  import/export. The valuable part is **`ConfigUtil.saveConfig`**: temp
  file → re-parse verify → `ATOMIC_MOVE` → `.corrupted` backup on failure.
  ZEA adopts this protocol for all stores.
- *afranz29/Inventory-Buttons* — Fabric port mixin approach verified
  (`AbstractContainerScreen` render + cancellable `mouseClicked`), drag
  editor with grid snap. Its persistence is a plain `Files.newBufferedWriter`
  write — exactly the corruption risk ZEA must avoid.

**MoulConfig (configuration framework):**

- Canonical repo: `NotEnoughUpdates/MoulConfig` (Moulberry's original is a
  404). License LGPL-3.0-or-later (matches ZEA's own licensing).
- **4.8.0 ships `modern-26.3`** at `https://maven.notenoughupdates.org/releases/`
  (`org.notenoughupdates.moulconfig:modern-26.3:4.8.0`). Verified published.
  Fabric-only for modern versions. Distinct from Lattice (GUI-only, MIT) and
  from OneConfig (Polyfrost; excluded by constraint).
- Integration decision: **jar-in-jar (`include`)** — MoulConfig stays a
  separate unmodified Fabric mod jar. Cleanest LGPL compliance and idiomatic
  Fabric. Fallback if JiB breaks the build: official shadow+relocate recipe.
  MoulConfig has no built-in HUD editor; ZEA's chat dimensions editing will
  be a native implementation.

**EarthMC API (docs + live verification):**

- Current version **v4**, base `https://api.earthmc.net/v4`. Public core
  endpoints (towns/nations/players/quarters/location/nearby/online) need no
  key. `POST` endpoints take `{"query": [names|uuids], "template": {...}}`.
- The newer authorization model is verified: `/api key create|delete|copy`
  creates personal API keys in-game; `/api auth` opens a per-player shop
  authorization editor; `/api opt-out` opens a granular opt-out editor.
  Keys unlock SSE, own resident data, shops, mcMMO, pursuits.
- Rate limit 180 req/min (429 + `Retry-After`), per-endpoint cooldowns.
  Live coordinates only via `https://api.earthmc.net/map/v1/players`.
- Full details recorded in [docs/EARTHMC_API.md](docs/EARTHMC_API.md).

**Toolchain (all verified):**

- Minecraft 26.3 (released 2026-09-15) requires **Java 25**.
- Fabric: loader 0.20.1, Fabric API 0.165.2+26.3, Loom 1.19-SNAPSHOT,
  Gradle 9.8.2 wrapper — per the official `fabric-example-mod` default
  branch (now `26.3`).
- **Yarn mappings end at 1.21.11.** MC 26.3 uses official Mojang mappings
  (the example mod declares no mappings at all; Loom defaults to official).

**NEU-REPO analysis** (for ZEA-REPO design): flat ID-keyed files, external
schemas, commit-sha-pinned zip downloads, changed-file-only CI. Weaknesses to
avoid: 9k-file flat dirs, triple-stored display text, loose typing, no
manifest. Full report lives in ZEA-REPO `docs/DESIGN.md`.

**HelpChat paste service:** protocol recorded from spec. The hostname does
not resolve from this development environment (NXDOMAIN), so live protocol
verification is pending; Stage 8 tests will use an embedded mock HTTP server
regardless.

### Decisions

1. **Single source set, `environment: client`** — ZEA is 100% client code;
   a split common/client layout would be an empty shell.
2. **Official Mojang mappings** — Yarn is dead past 1.21.11; matches
   MoulConfig modern-26.3.
3. **MoulConfig via jar-in-jar** — see above; OneConfig excluded.
4. **LGPL-3.0-or-later for ZEA** (MoulConfig is LGPL; mod + config framework
   licensing aligned). **MIT for ZEA-REPO** (data repo, maximally permissive).
5. **ZEA-REPO is curated/supplementary data only** — volatile live data
   (residents, balances, coordinates) always comes from the official API at
   runtime; the repo stores what the API cannot: badge meanings, channel
   definitions, curated nation/town descriptions, history. Keyed by slug +
   UUID, manifest-driven selective download, CI-validated + CI-regenerated
   manifest.
6. **NEU safe-save protocol adopted** for every persistent store.
7. Commit discipline mirrors LuckPerms (see research above).

### Files changed (this session)

ZEA (all on `develop`):

- Documentation: `README.md`, `ARCHITECTURE.md`, `ROADMAP.md`, `WORKLOG.md`,
  `docs/{DEPENDENCIES,EARTHMC_API,EARTHMC_MESSAGE_FORMATS,TESTING,PASTE_SERVICE}.md`.
- Licensing: `LICENSE` (LGPL-3.0-or-later, full text).
- Build: Gradle 9.8.2 wrapper, Loom 1.19 setup, single source set,
  `fabric.mod.json` (environment client), MoulConfig 4.8.0 via jar-in-jar.
- Foundation (Stage 1, partial): `ZeaClient` wiring,
  `lifecycle/ClientLifecycle` (join/leave/stopping events),
  `earthmc/EarthMcDetector` (address-based detection),
  `persistence/SafeFileWriter` (atomic replacement),
  `persistence/JsonStore` (round-trip-verified saves, corruption recovery).
- Tests: `EarthMcDetectorTest`, `SafeFileWriterTest`, `JsonStoreTest`.

ZEA-REPO (on `main`):

- `docs/DESIGN.md` (the assessment above), `README.md`, `CONTRIBUTING.md`,
  `manifest.json`, `schemas/` (manifest/badges/channels/nation/town),
  `datasets/{badges,channels}.json` (seeded with verified observations),
  `scripts/validate.py`, `.github/workflows/validate.yml`, `.vscode/`.

### Tests performed

- Build: `./gradlew build` — **successful** (Java 25, Minecraft 26.3,
  Loom 1.19). Jar verified to contain `ZeaClient`, expanded
  `fabric.mod.json` version, `LICENSE_zea`, and the bundled
  `META-INF/jars/modern-26.3-4.8.0.jar` (MoulConfig).
- Unit tests: **21 tests, 0 failures** (EarthMcDetector 13,
  SafeFileWriter 4, JsonStore 4) via JUnit 6.1.3.
- ZEA-REPO validation: `scripts/validate.py` passes on the seeded data;
  negative tests confirmed it rejects bad schema versions, missing
  required fields, stray file locations and stale manifest hashes.
- In-game testing: **none performed this session.** No claims are made
  about in-game behavior, MoulConfig runtime initialization, or the
  EarthMC connection flow beyond what compiles and passes unit tests.

### Outstanding problems

- `paste.helpcha.at` unreachable from this environment — live protocol
  verification pending (mock-server tests planned for Stage 8).
- Nickname → real username resolution source unresolved (Stage 4
  investigation).
- Server identity key for history isolation unresolved (Stage 7).
- In-game verification of the full writable channel list pending; the
  ZEA-REPO `channels` dataset allows corrections without mod releases.

---

## 2026-11-08 (session 2) — Stage 1 complete, Stage 2 message classification

### Research performed

- Verified the 26.3 chat integration surface before writing capture code:
  Fabric API ships `ClientReceiveMessageEvents` (GAME + CHAT) in
  `fabric-message-api-v1` 7.0.10 for 26.3 — official hooks, so **no custom
  mixins are needed for capture**.
- Verified the 26.3 component API: `ClickEvent`/`HoverEvent` are now
  sealed interfaces with record implementations (`OpenUrl.uri()`,
  `RunCommand.command()`, `ShowText.value()`); `MutableComponent` builders
  unchanged. Minecraft `Component` classes load in plain JUnit tests, so
  classifiers are testable without a client.

### Work done

- **Stage 1 completed** with `chat.capture.MessageCapture`: registers the
  Fabric events, skips overlay (action bar) text, gates on the EarthMC
  connection, forwards immutable captures. Wired in `ZeaClient`.
- **Stage 2 implemented**:
  - `chat.model`: `CapturedMessage` (original component + flattened text +
    timestamp), `MessageCategory`, sealed `MessagePayload` (one record per
    family + `None`), `Badge`, `Affiliation`, `ChannelChat`,
    `PrivateMessage`, `VoteMessage`, `CrateWinMessage`, `DeathMessage`,
    `SystemMessage`, `WritingChannelReport`, `DiscordMessage`,
    `ClassifiedMessage`.
  - `chat.classify`: `MessageClassifier` (binary confidence: anchored match
    or decline), `ClassificationPipeline` (fixed order, first match wins,
    guaranteed UNCLASSIFIED fallback), per-family classifiers, `KnownBadges`
    (mirrors ZEA-REPO ids), `BadgeExtractor` (component-first sibling walk
    preserving hover/click, text fallback for flat messages, unknown
    symbols never assigned meanings).
- **Tests**: 14 fixture families in `fixtures/messages.json` run through
  the full pipeline; per-classifier tests for component-structure behavior
  (badge events preserved, single-segment affiliations, nickname display
  names, non-PM arrows, variable crate amounts, optional `/vote`,
  death hover preservation, Discord-before-global ordering).

### Tests performed

- `./gradlew build` successful. `./gradlew test`: **83 tests, 0 failures**
  (14 new fixture-pipeline assertions ×2, 27 classifier tests, 2 model
  tests, plus the existing 21).
- In-game testing: **none** — capture/classification runtime behavior on
  live EarthMC is unverified; only compile + unit results are claimed.

### Decisions

1. **Fabric events over mixins** for capture (official integration point).
2. **Binary confidence** — anchored patterns are always confident; the
   guaranteed UNCLASSIFIED fallback is the safety property that matters.
3. **Bracket required for global classification** — `Name: content` alone
   is too weak an anchor; bracket-less messages stay unclassified rather
   than risk misclassification.
4. **Single-segment affiliations kept raw** — nation-vs-town is not
   guessable from text.
5. **KnownBadges in code for now**, mirroring ZEA-REPO ids; ZEA-REPO
   becomes the source when the repository client exists.

### Outstanding problems

- Local/trade/nation/town message formats: **no observed samples** — no
  classifiers written (documented in CHAT_CLASSIFICATION.md). Noted risk:
  bracket-leading channel markers (`[Town] Name: msg`) would currently be
  claimed by the global classifier; ZEA-REPO channels dataset is the
  planned data-driven fix.
- Component-tree structure of EarthMC messages unverified (text fixtures
  only) — needs in-game capture.
- Nickname → real username resolution unresolved (Stage 4).

---

## 2026-11-08 (session 2, continued) — Stage 3 core: feeds and routing

### Work done

- `chat.feeds`: `Feed` (+ `Feeds` registry: 7 default tabs, 6 extra feeds,
  All aggregates everything including unclassified), `FeedSelection`,
  `SendTargets`, `ChannelState` (server writing channel — updated only
  from server reports, cleared on disconnect), `WriteChannel`/`Channels`
  (verified per-message command table), `OutgoingRouter`,
  `MultiChannelDispatcher` (staggered dispatch, 1000 ms default interval,
  failure cancels remaining, batch replacement respects the interval).
- `chat.ChatSubsystem`: wiring — capture → classify → channel state;
  dispatcher ticked on client tick, cancelled on disconnect, sends via
  `ClientPacketListener#sendCommand`.
- `docs/CHAT_CHANNELS.md`: the three-state model, channel table, dispatch
  policy, what is deliberately not modeled (switch commands).

### Design decisions

1. **Per-message routing, no switch commands.** ZEA sends `/g <message>`
   style commands; it never runs `/global` itself. The server writing
   channel therefore only ever changes through the player's own actions,
   tracked from server reports — no transition to get wrong.
2. **Three separate state classes** (`ChannelState`, `FeedSelection`,
   `SendTargets`) so the spec's "never one overloaded variable" rule is
   enforced by the type system.
3. **Uniform dispatch timing** (1000 ms) — conservative against spam
   limits, never randomized to look human.

### Tests performed

- `./gradlew test`: **110 tests, 0 failures** (27 new: feed filtering and
  aggregation, router routing/order/rejections, dispatcher interval/
  cancel/failure/replacement behavior, channel state report/clear).
- In-game testing: **none.** Runtime behavior (tick wiring, sendCommand)
  is compile-verified only.

### Outstanding problems

- Server writing-channel report on join unverified — `ChannelState`
  starts unknown until a report arrives.
- Dispatch interval and default tabs not yet configurable (MoulConfig
  options with the Stage 4 UI).
- Tab rendering, send UI, "All is read-only as a selector" UI enforcement:
  Stage 4.

---

## 2026-11-09 (session 3) — Configuration, chat tab bar, feed filtering

### Work done

- **MoulConfig configuration** (`config` package): `ZeaConfig` with two
  categories — Chat (multi-channel sending off by default, dispatch
  interval slider 500–5000 ms) and Tabs (per-feed visibility for the six
  extra feeds). `ZeaConfigStore` persists it through the safe-write
  protocol (round-trip verified, atomic replace, corrupt files preserved
  as `*.corrupted` with defaults restored). `saveNow` is routed through
  the store so MoulConfig editor saves are safe writes. `configVersion`
  gates migrations (none needed at v1).
- **Wiring**: `ChatSubsystem` reads the dispatch interval and gates
  multi-target sends behind the config flag (`MultiSendResult`);
  `Feeds.visibleTabs` resolves the tab strip from enabled extra feeds;
  `ZeaClient` loads the config at init.
- **Chat tab bar** (`chat.render.ChatTabBar` + `mixin.ChatScreenMixin`):
  a flat, borderless tab strip above the chat input, EarthMC-only. Plain
  click switches the display feed; rendering is a translucent fill and a
  label, nothing else.
- **Feed filtering**: applies the selected feed as the vanilla chat's
  official `setVisibleMessageFilter` — visual only, every message stays
  in history (All restores vanilla behavior by clearing the filter).
  `ClassificationCache` maps displayed components back to classifications
  by identity, bounded at 512 entries, single classification per message
  shared between capture and filtering. Display state resets to All on
  disconnect so other servers see untouched chat.

### Research performed

- Verified the 26.3 chat integration surface by decompiling the mapped
  jar: `ChatComponent.setVisibleMessageFilter(Predicate<GuiMessage>)` is
  an official hook that immediately refreshes trimmed messages;
  `ChatScreen` draws input background → chat panel → widgets (widgets on
  top, so a widget tab bar works).
- **Minecraft 26.3 ships unobfuscated**: its version manifest has no
  `client_mappings`/`server_mappings` downloads. Official Mojang names
  are runtime names, so ZEA's jar needs no intermediary remap and its
  mixins need no refmap — documented in ARCHITECTURE.md.
- MoulConfig 4.8.0 annotation surface verified from the jar
  (`@Category`, `@ConfigOption`, `@ConfigEditorBoolean/Slider`, runtime
  reflection processing); the MoulConfig base class loads in plain JUnit.

### Tests performed

- `./gradlew build` successful; jar contains the mixin, `zea.mixins.json`
  and the bundled MoulConfig jar.
- `./gradlew test`: **135 tests, 0 failures** (22 new: config defaults/
  clamping/round-trip/corruption/migration, annotation-contract
  structure, visible-tabs resolution, cache identity/bounds, feed
  filter predicates).
- In-game testing: **none.** The tab bar's rendering, hitboxes and the
  filter's live behavior are compile- and unit-verified only; first
  in-game verification is pending.

### Design decisions

1. **Official filter hook over a rendering mixin** — filtering happens
   inside vanilla's own pipeline; ZEA adds no message-rendering mixin.
2. **`null` filter for All** — aggregates skip the predicate entirely;
   vanilla shows everything, including anything ZEA never classified.
3. **Identity-keyed classification cache** — one classification per
   message, shared by capture and display filtering; misses re-classify
   lazily, so cache eviction can never hide a message.
4. **Tabs anchored above the input strip** — exact, scale-independent
   geometry (`height - 14 - TAB_HEIGHT`); panel-top anchoring would
   depend on scaled chat geometry.


### Late addition (same session) — message copying

- **`util.LegacyText`**: Component → legacy `§`-code serializer (26.3 ships
  none). Style runs emit `§r` + full code set on change, mirroring how
  EarthMC emits segments; inherited child styles produce no extra codes;
  non-legacy colors are skipped.
- **`chat.render.ChatGeometry` + `ChatHitTester`**: the vanilla line
  geometry mirrored from ChatComponent's bytecode (`chatBottom =
  floor((guiHeight - 40) / scale)`, entry metrics, text-region slot math),
  plus accessor reads (`ChatComponentAccessor`) of the live line list and
  scroll position.
- **`ChatScreenMixin`**: right-click copies the message under the cursor,
  alt + right-click copies the formatted variant. Clicks not over a
  message fall through to vanilla.

Tests: **152 total, 0 failures** (LegacyText emission rules including the
observed EarthMC segment style; geometry boundaries and slot stacking).
In-game hit-test accuracy remains **unverified** — the math mirrors
vanilla's own formulas, but no in-game check has happened yet.

### Outstanding problems

- Nickname → real username resolution unresolved (Stage 4).
- Smooth chat, search, dimensions: Stage 4 remainder.
- Tab bar width uses the unscaled chat width; users with chat scale
  below 1 see a strip wider than the panel (cosmetic, tracked).
- Writing-channel report on join still unverified in-game.

---

## 2026-11-09 (session 4) — Copy wiring completed, confirmation toast

### Problem found and fixed

- Commit 432f3a6 ("Add chat message copying") had landed **incomplete**:
  the support classes (geometry, hit-tester, legacy serializer, accessor)
  and their tests existed, but the `mouseClicked` injection itself was
  missing — the mixin carried only its imports and constants. Copying
  compiled but did nothing. Rebuilt the wiring properly.

### Work done

- **`chat.render.ChatCopier`**: the copy policy — hit-test the cursor,
  pick plain or legacy-formatted serialization, write the clipboard.
  Returns `PLAIN` / `FORMATTED` / `NO_MESSAGE` so callers can leave
  missed clicks to vanilla. `textFor` is unit-testable without a client.
- **`chat.render.CopyToast`**: the "message copied" confirmation, shown
  through the vanilla system toast with ZEA's own
  `SystemToastId` token (never replaces a vanilla notification). First
  localized strings: `assets/zea/lang/en_us.json`.
- **`ChatScreenMixin`**: right-click copies the message under the cursor,
  alt + right-click the formatted variant; the click is consumed only
  when a message was hit, otherwise vanilla proceeds untouched. Verified
  against the 26.3 signatures by javap: `ChatScreen.mouseClicked(
  MouseButtonEvent, boolean)`, `MouseButtonEvent.{x,y,button,modifiers}`,
  `GuiMessage.content()`, `Gui.gui.hud.getChat()`, `Gui.toastManager()`.
- Cleaned the dead imports the incomplete commit had left behind.

### Tests performed

- `./gradlew build` successful; jar contains the new classes and the
  lang file.
- `./gradlew test`: **156 tests, 0 failures** (4 new: plain flattening,
  formatted emission incl. the no-leading-reset rule, both-modes parity,
  EarthMC segment round-trip).
- In-game testing: **none.** Hit-test accuracy on a live client is still
  the outstanding verification for the copying feature as a whole.

### Outstanding problems

- Unchanged from the previous session, plus: toast text is
  English-only until more languages are contributed.

