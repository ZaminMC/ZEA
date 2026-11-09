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

---

## 2026-11-09 (session 4, continued) — Chat search, smooth chat

### Work done

- **Chat search** (`Ctrl+F`): `FeedFilter` holds the query — the single
  source of truth — and composes it with the selected feed's predicate
  (a search narrows even on All; a blank open query narrows nothing;
  `null` means closed). `ChatSearchBar` is a borderless vanilla
  `EditBox` with the flat chat background. `ChatScreenMixin` wires it:
  Ctrl+F toggles (macOS: Cmd+F via the vanilla control quirk), live
  filtering per keystroke, Escape closes the search before it closes
  the screen, Enter while focused returns focus to the input (so the
  input's text can never be sent accidentally mid-search), and
  `removed()` closes the search when the screen goes away — the
  visible-message filter also affects the HUD, which must never stay
  narrowed invisibly. The query survives window resize (state lives in
  the filter, the widget is rebuilt by init) and resets on disconnect.
- **Smooth chat**: `SmoothChat` keeps one vertical offset (unscaled
  chat units) that starts at a scroll discontinuity and eases to zero
  (quartic ease-out; mid-flight discontinuities accumulate from the
  remaining offset). `ChatComponentMixin` records the anchor around
  `scrollChat`, `resetChatScroll` and `addMessageToDisplayQueue`
  (message line count captured by wrapping `GuiMessage.splitLines`,
  because the trimmed list's 100-entry cap makes growth under-report
  on busy channels; the add-path's internal pin scroll is skipped via
  a flag), and translates the chat's pushed pose by the eased
  remainder at render time (chat units × chat scale = gui pixels;
  `pushMatrix` wrapped so the package-private `DisplayMode` parameter
  never has to be referenced). EarthMC-gated, configurable, on by
  default (200 ms).
- **Configuration**: new "Chat interface" category — smooth chat
  toggle + duration slider (50–500 ms, clamped accessor). Additive
  fields; older files keep defaults, no migration needed.
- Verified from the mapped jar before writing code: 26.3 focus
  cycling skips invisible widgets (`isActive` = visible ∧ active), Tab
  focus cannot land on the hidden search field; `KeyEvent` carries
  `shortcutKey`/`isEscape`/`isConfirmation`/`hasControlDownWithQuirk`;
  `resetChatScroll` fires from `ChatScreen.removed`.
- **Chat dimensions dropped as a work item**: vanilla 26.3 ships
  `chatWidth`, `chatHeightFocused` and `chatHeightUnfocused` options
  itself. Re-implementing them would duplicate vanilla settings and
  fight its own options screen.

### Design decisions

1. **Query state in `FeedFilter`, not the screen** — one writer of
   `setVisibleMessageFilter`, resize-safe, resettable on disconnect.
2. **Search composes rather than replaces the feed filter** — the tab
   you are viewing stays the search domain.
3. **Trigger hooks around anchor changes, render hook applies the
   offset** — the animation is a pure function of (anchor history,
   clock), so the two halves stay independently testable.
4. **`@WrapOperation` on `pushMatrix`** for the render hook: the
   handler needs no target-method parameters, dodging the
   package-private `DisplayMode`, and composes with other mods'
   wraps of the same call.

### Tests performed

- `./gradlew build` successful; jar contains the new classes and mixin
  registration.
- `./gradlew test`: **171 tests, 0 failures** (15 new: 6 search
   composition in FeedFilterTest — blank query, All narrowing,
   case-insensitivity, feed composition, styled flatten, close
   restore; 8 SmoothChat — full start, negative start, exact-zero end,
   quartic shape at t=¼ and t=½, mid-flight accumulation,
   zero-duration collapse, zero-shift no-op, finished-state reset;
   1 config clamp).
- Mixin descriptors re-audited against `javap` output (method
  signatures, the `splitLines`/`pushMatrix` targets, the public
  `extractRenderState` descriptor).
- In-game testing: **none.** Both features are compile- and
  unit-verified; the pose translate, easing feel, search focus flow
  and copy hit-testing need a live client.

### Outstanding problems

- Nickname → real username resolution unresolved (Stage 4 remainder).
- Player hover presentation, configurable controls: Stage 4 remainder.
- Smooth chat: hover/click hit regions follow the settled anchor, so
  they can lag the visuals by up to one animation (~200 ms); clicks in
  practice land after the glide. Tab bar width with chat scale below 1
  (cosmetic) and writing-channel report on join — unchanged.

---

## 2026-11-09 (session 4, wrap-up) — Key binding, nickname investigation

### Work done

- **Rebindable "toggle chat search" key binding** (`keybind.ZeaKeys`):
  registered through Fabric's `fabric-key-mapping-api-v1`
  (`KeyMappingHelper.registerKeyMapping`), under a ZEA category in the
  vanilla controls screen, unbound by default. `ChatScreenMixin`
  accepts it alongside Ctrl+F. Verified from the mapped jar: 26.3
  renamed `InputConstants.Type.KEYSYM` → `KEYBOARD`, `KeyMapping` takes
  an int key code, `KeyMapping.matches(KeyEvent)` exists, and
  `KeyMapping.Category.register(Identifier)` is public (label key:
  `key.category.<namespace>.<path>`).
- **Why the built-in shortcuts stay fixed**: vanilla key mappings are a
  single key without modifiers — they cannot express right-click,
  alt+right-click or Ctrl+F. Rebinding those would need a custom
  capture UI; recorded as a future option if users ask.

### Research performed

- **Nickname → username resolution, settled against the API** (live
  probes, 2026-11-09): player objects carry no nickname field; lookup
  by nickname 404s while the real name resolves (observed sample:
  `Coriolanus` → 404, `pengpy` → found). The API indexes real names
  only, so resolution cannot be built on it. Remaining candidate: the
  chat component tree (hover/click events on the displayed name) —
  needs in-game captured components. Full player object shape (title,
  surname, formattedName, about, town, nation, timestamps, status,
  stats, perms, ranks) recorded for the future player-card work.
- The simple `GET /v4/players` response is an online-player
  `{name, uuid}` index (query parameter ignored) — noted for the
  future API client.

### Tests performed

- `./gradlew build` successful; **171 tests, 0 failures** (no behavior
  change to test — the binding is registration + a `matches` check).
- In-game testing: **none.** The controls-screen entry and the
  keybind-triggered toggle need a live client.

### Outstanding problems

- Stage 4 leftovers (player hover, real-username display) are blocked
  on in-game component capture; next unblocked work is Stage 5
  (notifications).


---

## 2026-11-09 (session 5) — Stage 5: notifications, mentions

### Work done

- **Notification coordinator** (`notify`): the single owner of the action
  bar, title and subtitle. PM-over-mention priority (a PM preempts a
  showing mention; a mention never preempts a PM), equal-priority
  queueing bounded at 4 (oldest dropped first), temporary notifications
  (mentions, 4 s), persistent ones (PM notices) that resolve by key —
  the sender, so replying dismisses them — or expire at a hard 5-minute
  cap, and a `clear()` for the disconnect path. All display goes through
  a `NotificationSurface` interface; time is injected, so the policy is
  fully unit-tested without a client.
- **PM notifications**: incoming PMs post a persistent notice ("PM from
  <sender>: <content>", 60-char preview); any outgoing PM to that sender
  resolves it. Config: enable, sound, placement (action bar default,
  title optional — both re-show every 2.5 s while a persistent notice is
  active, just inside vanilla's ~3 s overlay fade).
- **Mention detection** (`mention`): case-insensitive, word-boundary
  matching of the local player's name in chat-like categories only (PM,
  system, vote, crate and death lines never ping). Channel chat scans the
  parsed content and excludes the speaker's own echo; Discord and
  unclassified lines only match behind a `Name: content` separator —
  lines that cannot be attributed never ping.
- **Mention presentation**: gold tint (alpha 0.30, blended with the
  line's own alpha) behind mentioned chat lines; notification with
  40-char preview and configurable placement; experience-orb cue (pitch
  1.0 mentions, 0.6 PMs). Highlight state is a bounded identity-keyed
  set — the render mixin looks up `GuiMessage.Line#parent#content`,
  the same instance capture classified, so no component is ever
  modified.
- **Opt-in auto-response**: off by default and inert until a response
  text is configured; cooldown (10–300 s slider, default 30); hard
  session cap (20 per connection, reset on disconnect); loop guard (an
  incoming mention already containing the response text is never
  answered, so two ZEA users cannot ping-pong); only verified writable
  channels (global/local/trade/nation/town via the Channels table);
  dispatches through the existing conservative router + dispatcher.

### Research performed and findings

- **26.3 HUD split**: titles and the action bar moved from `Gui` to the
  new `Hud` (`Minecraft.getInstance().gui.hud`): `setOverlayMessage`
  (60-tick display), `setTitle`/`setSubtitle`/`setTimes`/`clearTitles`.
  Verified from the mapped jar via javap and Vineflower decompilation.
- **Action bar lifetime**: `overlayMessageTime = 60` ticks (~3 s),
  decremented in `Hud.tick` — measured to pick the 2.5 s refresh.
- **Line background rendering**: `extractRenderState` draws each visible
  line's background inside `lambda$extractRenderState$1` — one
  `ChatGraphicsAccess.fill(IIIII)V` call per line with the lambda's
  captured params (including the `GuiMessage.Line` and its alpha)
  available to a `@WrapOperation` handler. The three
  `ChatGraphicsAccess` implementations (focused, background,
  click-capture) make the wrap safe in every mode — the click-capture
  one's `fill` is a no-op.
- **MoulConfig dropdowns accept enum-typed fields** (decompiled
  `GuiOptionEditorDropdown`: `Enum.class.isAssignableFrom(clazz)` path
  sets the constant, displays `toString()`), so placements are
  `Notification.Destination` enums — type-safe in code, serialized by
  name, displayed via label toString. The config structure test now
  allows String or enum dropdowns, and pins text fields to String with
  exactly one editor annotation per option.
- **`SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP,
  pitch)`** for the cue (UI path, no world attenuation).
- `Minecraft.getInstance().getUser().getName()` supplies the local name
  — fetched lazily per message, since the session does not exist at mod
  init.

### Design decisions

1. **Render-time tint over component wrapping.** Wrapping the displayed
   component in a styled prefix would break the classifiers' anchored
   parsing on re-classification and diverge from the identity-keyed
   cache; drawing over vanilla's own per-line background rect changes
   color only — hit-testing, search, copying and hover stay untouched.
2. **`Name: content` attribution for unstructured lines.** EarthMC's
   local/trade/nation/town classifiers are pending fixtures, so those
   lines arrive unclassified; requiring the separator keeps join and
   system lines from pinging while still catching real mentions there.
3. **PM notices persist, mentions do not.** A mention is a 4-second
   attention cue; a PM waits for the reply that usually ends it, with
   the 5-minute cap as the escape hatch.
4. **Auto-response sends through the same dispatcher as manual
   multi-send** — one message at a time, spaced by the configured
   interval, cancelled on disconnect — instead of a private send path.
5. **Loop guard by text containment**: cheap, and it breaks the exact
   ping-pong shape (each side's message contains the shared response
   text) without heuristics about message timing.

### Tests performed

- `./gradlew build` successful; jar contains all new classes and the
  mixin registration.
- `./gradlew test`: **254 tests, 0 failures** (83 new: 18 coordinator —
  preemption, queueing, capacity drop, persistence, refresh window,
  hard expiry, resolve paths, disconnect clear, title times; 12
  notifications service — PM post/resolve/preview, config gates,
  placement; 28 mention detector — boundaries, case, self-echo,
  category gating, unstructured attribution; 9 mentions service — fanout
  and per-option gating; 6 highlight set — identity, eviction, clear;
  10 auto-responder — guards, channels, session cap, reset). Config
  structure test now also enforces single-editor and text/dropdown
  types.
- In-game testing: **none.** The action bar/title behavior, tint
  visibility, sound and the controls-screen entries (new Notifications
  and Mentions categories) need a live client; the mixin target was
  verified against 26.3 bytecode but not against a running game.

### Outstanding problems

- Nickname-based mention matching remains blocked on EarthMC data (API
  has no nicknames); nickname self-echo can false-ping until then.
- `lambda$extractRenderState$1` is pinned to 26.3's compiled shape —
  re-verify on any Minecraft version bump.
- Persistent notices on the title placement re-show every 2.5 s, which
  is assertive by design; if users report it as annoying, gate title
  refresh behind a follow-up option.
- Stage 4 leftovers (player hover, real-username display) stay blocked
  on in-game component capture. Next unblocked work is Stage 6
  (inventory buttons).

## 2026-11-09 (session 6) — Release pipeline, Stage 6: inventory buttons

### Work done

- **Release pipeline** (`.github/workflows/release.yml`): every push to
  `develop` cuts the next numbered dev build automatically — compute N
  over the `vMAJOR.MINOR.PATCH-dev.N` tag ledger, stamp `mod_version` in
  `gradle.properties`, sync the bump back to develop as an Abasing
  `[skip ci]` commit before building (loop-breaker), `./gradlew build`
  (tests included), tag, publish `zea-<version>.jar` as a prerelease
  with auto-generated notes. Failure safety and escape hatches mirror
  the ZaminTorch pipeline: no tag/release on build failure, orphan-tag
  repair path, `[skip ci]` postpones, re-running a published run is a
  no-op. The `ABASING_TOKEN` repo secret was set so every
  pipeline-authored commit, tag and release is Abasing, never
  github-actions[bot]. Verified live: `v0.1.0-dev.1` through `dev.4`
  published.
- **Stage 6a — model and store** (`buttons` package): `InventoryButton`
  (immutable record, self-sanitizing constructor), `ButtonAnchor`
  (four container corners), `ButtonGeometry` (pure anchor math, hit
  tests, nearest-corner re-anchoring), `InventoryButtonStore`
  (versioned `{"version":1,"buttons":[…]}` file through the safe-write
  protocol; tolerant `ButtonEntry` wire record because Gson rejects
  explicit nulls on primitive record components — one damaged entry must
  not quarantine the file; unusable entries dropped with a warning,
  duplicate ids re-issued).
- **Stage 6b — rendering**: `ButtonRenderer` (slot-like box: 1px border
  over dark fill), `ButtonIcons` (cached id→ItemStack, barrier
  fallback), `ButtonTooltips` (name, command, unknown-icon warning).
  `ContainerScreenMixin` on `AbstractContainerScreen`: buttons draw at
  TAIL of `extractContents` (pose popped → screen coordinates), a
  hovered button cancels `extractTooltip` (HEAD) so slot tooltips never
  stack. Config category *Inventory buttons* (master toggle, on by
  default — inert with no buttons). Gated on the EarthMC connection.
- **Stage 6c — execution**: `ButtonActivator` is the only path from a
  click to a sent command: re-validates the command (non-blank, ≤256,
  no control chars — a hand-edited file cannot smuggle a malformed
  command past the click), 500 ms per-button cooldown (double-click =
  one command), failed sends don't arm the cooldown, resets on
  disconnect. Sends via `ClientPacketListener.sendCommand` — the same
  path as the chat dispatcher. Mouse releases over buttons are consumed
  so vanilla never treats the area as "outside the GUI" (which would
  drop a carried item); mid-quick-craft releases pass through.
- **Stage 6d — editing**: `ButtonDragSession` (grab → preview →
  re-anchor at nearest corner; 3px jitter threshold so a plain press
  never repositions), `ButtonEditScreen` (vanilla LinearLayout form:
  name/command/icon fields, anchor cycler, enabled toggle, Done with
  inline validation, two-click delete), edit mode toggled by the
  rebindable *Toggle button editing* key while a container screen is
  open (Esc leaves edit mode first), a **+** ghost beside the container
  creates buttons, `InventoryButtons` CRUD (upsert/delete persist
  immediately through the store).
- **Docs**: `docs/INVENTORY_BUTTONS.md` (behavior, anchoring, store
  policy, the no-execution guarantees, mixin target table, limitations).

### Research performed and findings

- **26.3 container screen internals** (decompiled from the mapped jar):
  `extractRenderState` → `extractContents` (background + slots, pose
  translated by leftPos/topPos and popped at the end) →
  `extractCarriedItem` (own stratum) → `extractTooltip`. Injecting at
  TAIL of `extractContents` draws in screen coordinates below carried
  items and tooltips.
- **`mouseClicked`/`mouseReleased` semantics**: the container screen
  consumes nearly all clicks; releases outside the GUI with a carried
  item drop it (`slotClicked(null, -999, …, PICKUP)`). Hence the
  release-consume. `mouseReleased` resets `isQuickCrafting` at its end —
  cancelling it mid-quick-craft would strand the state, so those
  releases pass through (shadowed `isQuickCrafting` to check).
- **`ClientPacketListener.sendCommand`** sends `ServerboundChatCommandPacket`
  directly (no parse-error popups; `commands.parse` runs only to find
  signable arguments). `sendUnattendedCommand` exists but opens
  confirmation windows for parse/permission issues — wrong UX for
  buttons, which send plugin commands the client tree may not know.
- **`menu.removed(player)` is a client-side no-op** (server-only logic),
  so an edit screen can open over a container and return with
  `gui.setScreen(parent)` without closing the container — the same
  pattern vanilla's `sendUnattendedCommand` confirmation flow uses.
- **Gson 2.14 record behavior** (probed): a missing key leaves the
  component default; an explicit `null` on a primitive component throws
  `JsonParseException` — which would quarantine the whole buttons file.
  The `ButtonEntry` wire record (all-nullable boxed fields) isolates
  this: one weird entry degrades to a dropped button, not a lost set.
- **26.3 API notes**: `Registry.get(Identifier)` now returns
  `Optional<Holder.Reference<T>>` (use `getOptional`); `Identifier.tryParse`
  for null-safe parsing; widgets render through
  `extractWidgetRenderState`; screens build layouts with
  `LinearLayout`/`FrameLayout` + `visitWidgets(addRenderableWidget)`
  (pattern taken from `ConfirmScreen`); `Button.Builder` defaults
  150×20; `CommonComponents.OPTION_ON`/`optionStatus(boolean)`.

### Design decisions

1. **Anchors relative to the container, not the screen** — buttons keep
   their place beside the GUI across container sizes (inventory vs
   chest), matching how NEU positions its buttons conceptually without
   copying its Hypixel-oriented machinery.
2. **Offset (0,0) is the flush corner inside the GUI; new buttons use
   (22, 2) beside it** — pure math stays trivially invertible for drag
   re-anchoring, and the default still lands buttons outside the GUI.
3. **Self-sanitizing record + `isUsable()` at the boundary** — partial
   data is representable (the edit form builds incomplete buttons);
   the store boundary decides what persists. This keeps one validation
   ruleset (`InventoryButton.validCommand`/`validIcon`) used by the
   form, the store and the activator.
4. **The activator as the single execution path** — loading, importing
   and editing are inert by construction (no sender reference), which
   is the strongest form of the "no command execution on import" rule.
5. **Edit mode is screen-scoped and keybind-toggled, not a config
   toggle** — a persisted edit flag would outlive its purpose and
   confuse players who forget it on.
6. **Two-click delete instead of a confirmation screen** — the same
   restraint pattern as the copy toast: no modal for a recoverable-by-
   recreation action.
7. **Unknown-but-well-formed icon ids are accepted** — they render as
   barrier with a tooltip warning, so a typo is visible and fixable
   instead of silently dropping a user's button.

### Tests performed

- `./gradlew build` green; **307 tests, 0 failures** (53 new: model
  normalization/validity 8, geometry 8, store round-trip/corruption/
  per-entry tolerance/duplicate ids 8, tooltips 2, hit testing 4,
  activator 6, drag session 8, coordinator CRUD/edit mode 5, plus the
  extended config structure suite covering the new category).
- The mixin targets (`extractContents`, `extractTooltip`,
  `mouseClicked(MouseButtonEvent, boolean)`, `mouseDragged`,
  `mouseReleased`, `removed`) were verified against the 26.3 mapped
  bytecode before writing the injections; compile + jar contents
  checked.
- In-game testing: **none.** Rendering, hover, tooltips, the edit
  screen and drag interaction need a live client.

### Outstanding problems

- Stage 4 leftovers (player hover cards, real-username display) remain
  blocked on in-game component capture.
- The edit screen has no automated tests (pure widget assembly); its
  validation logic lives in `InventoryButton`/`Identifier.tryParse`,
  which are covered.
- Share/import of button sets (`inven-` share type) is deferred to
  Stage 8 as planned; the store's per-entry policy is the foundation.

## 2026-11-09 (session 7) — Fix dev.5 launch crash: inherited @Shadow

### Incident

User's instance (EarthSMP profile, Modrinth App, MC 26.3 + loader 0.20.1)
crashed at launch with `dev.5` installed. The crash fired while the
*recruitmentaddon* mod's entrypoint loaded `ChatScreen`, but the failing
component was ZEA's `ChatScreenMixin`:

```
InvalidMixinException: @Shadow method
addRenderableWidget(Lnet/minecraft/client/gui/components/events/GuiEventListener;)
Lnet/minecraft/client/gui/components/events/GuiEventListener;
in zea.mixins.json:ChatScreenMixin from mod zea
was not located in the target class net.minecraft.client.gui.screens.ChatScreen.
```

### Root cause

Mixin resolves `@Shadow` (and injection) members only against the target
class's **own declared** members — never inherited ones.
`addRenderableWidget` is `protected` on `Screen`, not declared on
`ChatScreen` (verified by `javap` on the 26.3 mapped jar: exact generic
signature exists on `Screen`). The shadow therefore failed during mixin
apply, which killed the `ChatScreen` transformation, which aborted the
`client` entrypoint stage — for whichever mod loaded `ChatScreen` first.
Any later class load of `ChatScreen` would have crashed the game just the
same; recruitmentaddon merely got there first. No ZEA behavior was at
fault; the entrypoint failure attribution was collateral.

### Work done

- **Fix**: new `ScreenMixin` (targets `Screen`, where the method is
  declared) bridges `addRenderableWidget` through the
  `ScreenWidgetAccess` duck interface; `ChatScreenMixin` drops the
  unresolvable shadow and calls
  `((ScreenWidgetAccess) (Object) this).zea$addRenderableWidget(...)`.
  JVM superclass loading guarantees the augmented `Screen` is in place
  before any subclass code runs. The bridge delegates to the vanilla
  method unchanged, so widget registration keeps 26.3 semantics.
- **Regression test**: `MixinSurfaceTest` pins every mixin's target
  surface (all `@Shadow` fields/methods, all injection points, both
  `@WrapOperation` call targets) to members actually declared by the
  target classes — the test that would have caught this bug class at
  build time. Convention: a mixin change pins its new targets in the
  same commit.

### Research performed and findings

- Full re-audit of the other four mixins against the 26.3 mapped jar
  (javap): `ChatScreen` declares `input`/`init`/`keyPressed(KeyEvent)`/
  `removed`/`mouseClicked(MouseButtonEvent, boolean)`;
  `ChatComponent` declares `chatScrollbarPos`/`trimmedMessages`/
  `getScale`/`getWidth`/`addMessageToDisplayQueue`/`scrollChat`/
  `resetChatScroll`/`extractRenderState(…;DisplayMode;Z)V`/
  `lambda$extractRenderState$1` and `ChatGraphicsAccess.fill(IIIII)V`
  exists; `AbstractContainerScreen` declares every field and method
  `ContainerScreenMixin` touches. No other inherited-member lookups
  exist in ZEA's mixins — this was the only instance.
- `ButtonEditScreen` (a real `Screen` subclass) already used
  `this::addRenderableWidget` — compiling fine, because subclass code
  *may* call inherited protected members. The mixin author applied the
  same pattern where the lookup rules differ; that asymmetry was the
  trap.

### Tests performed

- `./gradlew build` green; **312 tests, 0 failures** (5 new
  `MixinSurfaceTest` methods).
- In-game testing: none available in this environment; the mixin surface
  is now statically pinned by the new test instead.

### Outstanding problems

- Unchanged from session 6 (Stage 4 leftovers, edit-screen test gap,
  Stage 8 share/import).

## 2026-11-09 (session 8) — Stage 7: persistent chat history

### Work done

Built out in five commits, each independently verifiable:

- `HistoryEntry` + `ServerIdentity` — the immutable recorded row
  (timestamp, category, plain text, formatted text) and the stable
  per-server key. Identity is the address the client actually dialed
  (lower-cased host, default port stripped), not the MOTD brand —
  `earthmc.net` and a direct node address key different files.
- History configuration — `history.enabled`, `history.size`
  (entries, default 5000), `history.retention` (ONE_DAY … FOREVER,
  default 30 days) grouped under `history` in the config screen.
- `ChatHistory` — the bounded in-memory buffer (lock-guarded, immutable
  snapshots) that already served Stage 4 search; session-only or
  persistent is purely a matter of who seeds and flushes it.
- `HistoryStore` — per-server JSON through the existing safe-write
  protocol (serialize in memory, round-trip verify, atomic replace,
  corrupt file quarantined as `*.corrupted`). Schema `{"version": 1,
  "entries": [{"t", "c", "p", "f"}]}`; per-entry tolerance mirrors
  inventory buttons — one hand-edited row never quarantines the file.
- `HistoryRecorder` — the session coordinator: seeds on join
  (asynchronous load), batches dirty writes at most every 30 s onto a
  bounded single-thread executor, re-enforces retention every 5 min
  (before the flush, so cleanup persists in the same write), flushes on
  disconnect and on client shutdown. Writes coalesce while one is in
  flight; a rejected submission is retried on a later tick.
  `ZeaClient` owns the executor (daemon thread, queue of 4) and drains
  it with a 5 s bounded wait on CLIENT_STOPPING.

Search needed no new work: Stage 4's `FeedFilter` already searches the
`ChatHistory` buffer, which persistence now seeds on join — so chat
search transparently covers previous sessions' messages.

### Threading decisions

- Recorder state (`dirty`, `writing`, `store`) is confined to the
  client thread; the executor only does file IO and marshals results
  back through `Minecraft::execute`.
- The load is submitted before any save can be (executor is
  sequential), so the first save always observes the file the load
  read — no clobber window.
- Configuration is sampled per session; toggling persistence or
  retention applies on the next join, never retroactively.

### Tests performed

- `./gradlew build` green; 356 tests, 0 failures (10 new
  `HistoryRecorderTest` methods: seeding, disabled persistence,
  flush interval, clean-history no-writes, in-flight coalescing,
  disconnect flush, retention schedule, rejected-write retry,
  per-server isolation, rejected load).
- Fixed three test bugs from the interrupted authoring session before
  landing: a self-contradictory assertion (pre-created file asserted
  absent — now asserts its content unchanged), a rejection list that
  rejected the join load instead of the save, and a deferring executor
  whose queued load/save tasks were never drained.
- In-game testing: none available in this environment. Mixin surface
  unchanged this stage, so no new bytecode risk.

### Outstanding problems

- Stage 4 leftovers and the edit-screen test gap are unchanged.
- Stage 8 (HelpChat sharing) is the last Phase 1 stage.

## 2026-11-09 (session 9) — Stage 8: HelpChat configuration sharing

### Work done

Five commits, each landing with its tests:

- `ShareType`/`ShareDocument` — the `{format, type, version, data}`
  envelope. Strict identity: the format marker must match, the type must
  be known, versions this build does not understand are rejected, never
  guessed; unknown envelope fields are ignored for forward compatibility.
- `PasteKey` — accepts a bare key or a paste URL, but only
  `https://paste.helpcha.at`, only one key segment (with optional
  presentation extension like `.java`), no queries or fragments. The
  key charset `[A-Za-z0-9_-]` keeps every traversal shape out before
  any URL is built.
- `PasteService` — HTTP client on an injected bounded executor.
  POST `text/plain; charset=utf-8` to `/documents`, validate the
  returned key; GET `/raw/{key}` with a 1 MB cap (bytes buffered, then
  decoded once — chunk-wise UTF-8 decoding can split a character).
  Redirects are never followed. Every failure is a player-readable
  `ShareException`.
- Payload codecs — `InventoryButtonsPayload` (wire shape mirrors the
  button store's file, but imports validate strictly: unusable entry,
  duplicated identity or >100 buttons rejects the whole import) and
  `GeneralConfigPayload` (allowlist of behavior settings; history and
  internals excluded by construction; absent fields keep the current
  value; validate-everything-then-apply).
- Wiring — `ShareCoordinator` (exports round-trip their own document
  before uploading; imports hold as pending until confirmed; futures
  marshal to the client thread), `/zea share|import` commands, the
  `ZEA-Share` executor closed on shutdown, and `applyImport` on the
  buttons facade.

### Research performed and findings

- fabric-command-api-v2 3.1.2 (the version fabric-api 0.165.2 resolves)
  has no `ClientCommandManager`; the entry points are
  `ClientCommands.literal/argument` and the callback's second parameter
  is a `CommandBuildContext`. Pinned by compiling against them.
- Brigadier's `getRelevantNodes` prefers an exactly-matching literal
  child over a sibling argument, so `/zea import confirm` coexists with
  the greedy paste argument; a paste key that literally spells "confirm"
  is still importable by pasting the full URL.
- MC 26.3 feedback surface: `Hud.getChat()` → `ChatComponent
  .addClientSystemMessage` exists, but `FabricClientCommandSource
  .sendFeedback/sendError` is the idiomatic path and needs no direct
  chat access — used instead.

### Tests performed

- `./gradlew build` green; 443 tests, 0 failures. New: 15 envelope, 18
  paste key, 15 paste client (mock HTTP server — no test touches
  paste.helpcha.at), 14 button payload, 12 general payload (including
  the allowlist exclusion test: an export with history configured must
  not contain the word "history"), 14 coordinator (fake transport).
- Test bugs fixed before landing: a self-contradictory URL assertion,
  fixtures missing their icon field, and a text block whose JSON escapes
  were processed at the Java level instead of the JSON level.
- In-game testing: none available in this environment. Live protocol
  verification against the real paste service remains blocked by the
  NXDOMAIN from here — mock-tested only, as documented.

### Outstanding problems

- Live HelpChat verification (needs a network that resolves
  paste.helpcha.at, ideally an in-game pass).
- Stage 4 leftovers (nickname username display depends on data EarthMC's
  API does not expose) and the inventory edit-screen test gap remain.
- Phase 1 stages are now all implemented to the unit-tested level; the
  remaining Phase 1 work is in-game verification of the whole build.

## 2026-11-09 (session 10) — Tagged channel classifiers; edit form extraction

### Work done

Three commits:

**Add local, trade, nation and town chat classifiers** — closing the last
Stage 2 parser gap:

- `TaggedChannelChat` — payload for player chat on the tagged channels:
  bracket label, the sender's town (nation chat only — observed
  `[Nation Chat|Sosnovka]`, where the nation is implicit because only
  nation members can write there), display name, content.
- `TaggedChannelChatClassifier` — exact bracket label opening the
  message: `Local`, `Trade`, `Town Chat`, and `Nation Chat|<town>`
  (town required, non-blank, bounded). Case-sensitive; declined shapes:
  non-leading brackets, missing space, missing `": "`, bracketed names,
  the unobserved town-less nation form. Runs before the global
  classifier, which would otherwise read the bracket as an affiliation —
  the exact misroute risk this session's doc previously tracked.
- `MentionDetector` — structured attribution for the new payload, same
  path as global chat; the unstructured fallback stays for messages the
  classifiers do not recognize.
- Fixtures: local, trade, nation, town entries; docs updated
  (EARTHMC_MESSAGE_FORMATS.md records the observed legacy codes,
  CHAT_CLASSIFICATION.md the parse and the residual ambiguity).

**Extract the button edit form's decisions** — closing the edit-screen
test gap from session 6:

- `ButtonEditForm` — the edit form's logic without any Screen
  machinery: applying field values, validation in a fixed order
  (command before icon, first failure wins, one inline error at a
  time), the anchor/enabled mutations of the working copy, and the
  two-click delete confirmation. A refused save leaves the working
  copy untouched — the text fields stay the source of truth.
- `ButtonEditScreen` — now widget assembly and wiring only,
  delegating every decision to the form. The refusal reason carries
  its translation-key suffix explicitly (`Rejection.key()`), so the
  lang keys and the enum cannot drift apart silently.

**Reindent remaining files to tabs** — finishing the migration the
earlier `Reindent ZeaClient to tabs` commit started. 63 files, no
content changes: the conversion is text-block-aware (leading whitespace
inside a text block is string data and was left byte-identical — the
share-document tests that compare exact JSON confirm it).

### Research performed and findings

- The observed samples for all four channels were in the project brief
  all along (legacy-coded in-game logs) — the "no observed samples"
  blocker was a bookkeeping gap, not a data gap. The nation-chat bracket
  carries the sender's town, not the nation; trade's label is `Trade`
  (not "Trade Chat", which the brief's prose typo suggested) and town's
  is `Town Chat`, per the legacy strings.
- Residual ambiguity accepted and documented: a nation or town actually
  named "Local"/"Trade"/"Town Chat" would be claimed by the tag
  classifier before global. EarthMC names are geographic; the tags are
  data in ZEA-REPO so a wrong tag is correctable without a mod release.

### Tests performed

- `./gradlew build` green after each commit; 478 tests, 0 failures
  (was 443) — the count unchanged through the reindent, which is the
  verification that no string content moved. New: 15 classifier tests
  (observed shapes, town handling, declines, pipeline ordering), 8
  fixture assertions (4 families ×
  category + original retention), 2 mention tests (structured
  attribution, self-echo), 10 edit-form tests (validation order,
  refusal reasons, untouched working copy after refusal, slash
  stripping, anchor/enabled mutations, two-click delete, key/enum
  agreement).
- In-game testing: none available in this environment. Text-anchored
  parsing only; component-level structure remains unverified pending
  in-game capture. The edit screen's widget assembly remains
  untested; its decisions no longer are.

### Outstanding problems

- Component-level fixtures for every family (unchanged).
- Nation chat without a town is unobserved; if it exists it currently
  falls through to the global classifier's single-segment handling.
- Stage 4 leftovers (nickname username display) remain. Live HelpChat
  verification still blocked (paste.helpcha.at does not resolve from
  this environment).

## 2026-11-09 (session 11) — Chat component capture

### Work done

Two commits:

**Remove a dead duplicate of the shutdown path** — `ZeaClient` carried
both `shutdownBackgroundWork` (registered on CLIENT_STOPPING) and an
older private `shutdownHistory` that nothing called; the dead one goes.

**Add chat component capture** — the tool every "pending in-game
capture" item was waiting for:

- `ComponentJson` — a component tree to its vanilla JSON form through
  the shared component codec with plain JSON ops (no registry access).
  Wire form: no client-only style insertion; item/entity hovers cannot
  encode without registries and surface as errors instead of crashes.
- `CaptureRecorder` — explicit-request, bounded sessions (100 entries,
  auto-stop with chat feedback), timestamped files under
  `zea/capture/`, intermediate flushes every 25 entries through the
  safe-write protocol, final write always attempted, disconnect only
  flushes (a session spans rejoins), client shutdown ends it. Same
  threading contract as the history recorder: client-thread state, IO
  on a sequential executor, completion marshaled back.
- `/zea capture on|off|status` — `on` refuses off EarthMC; bare
  `/zea capture` is `status`. The `/zea` root is now assembled once in
  the entrypoint (`ShareCommands.register(root, …)` /
  `CaptureCommands.register(root, …)` contribute branches) instead of
  each subsystem registering its own root — this also fixes a trap the
  refactor exposed: the original tree kept `import` a sibling of
  `share`, which a naive branch extraction would have nested.
- File schema: `zea-capture` envelope (format + version, like the
  share documents), entries carrying timestamp, category, plain text,
  legacy codes, the component JSON, or the serialization error.
- `CaptureFixtures` (test side) loads capture files back into live
  components through the same codec — an in-game capture drops
  straight into a regression test.
- `VanillaBootstrap` (test side) — the bare test JVM must run
  `SharedConstants.tryDetectVersion()` + `Bootstrap.bootStrap()` before
  anything touches `BuiltInRegistries`; the component codec's static
  chain (Style → ClickEvent → SHOW_DIALOG → Dialog → registries) fails
  without it. Found by the round-trip tests, pinned by them.

### Research performed and findings

- `ComponentSerialization.CODEC` is a plain `Codec<Component>` usable
  with `JsonOps.INSTANCE` — no `HolderLookup.Provider` needed for
  text/style/hover-text/click components. Verified by javap against the
  mapped 26.3 jar before writing code, then empirically by the tests.
- Brigadier merges same-name roots at the dispatcher level, but two
  registration callbacks each owning `zea` would be structural
  ambiguity all the same; one root owner is the honest shape.

### Tests performed

- `./gradlew build` green; 488 tests, 0 failures (was 478). New: 2
  component round-trips (literal; styled with hover SHOW_TEXT, click
  RUN_COMMAND, colored sibling), 7 recorder tests (inactive by default,
  stamped-file recording, blank skipping, cap auto-stop with feedback
  and post-stop silence, error-keeping entries, same-second file
  separation, interval flushes mid-session), 1 fixture loop test
  (record → stop → load → component, style, hover and click intact).
- In-game testing: none available in this environment. The capture
  command paths and file writes are unit-tested; the first real
  capture happens on EarthMC.

### Outstanding problems

- The first real capture session (needs EarthMC) — then component
  fixtures, badge hover verification and the nation-chat-without-town
  question all become answerable from data.
- Stage 4 leftovers (nickname username display) and live HelpChat
  verification remain blocked as before.

## 2026-11-09 — dev.16 launch crash: duck interface inside the mixin package

### What happened

Live test report from the player: dev.16 crashes during startup, before
the title screen. Log signature (three identical uploads, one session):

```
net.fabricmc.loader.impl.FormattedException:
  java.lang.RuntimeException: Mixin transformation of
  net.zaminmc.zea.mixin.ScreenWidgetAccess failed
Caused by: org.spongepowered.asm.mixin.transformer.throwables.IllegalClassLoadError:
  net.zaminmc.zea.mixin.ScreenWidgetAccess is in a defined mixin package
  net.zaminmc.zea.mixin.* owned by zea.mixins.json and cannot be referenced directly
```

The first failure fires while populating the crash report itself
(`YumiEvents.<clinit>` → classloading cascade), the fatal one at
`Main.main:251` — every path resolves to the same missing class.

### Root cause

`ScreenWidgetAccess` — the duck interface `ScreenMixin` adds to `Screen`
so screen-targeting mixins can call `addRenderableWidget` (built after
the dev.5 crash) — lived in the mixin package. Mixin semantics, verified
by disassembling `sponge-mixin-0.17.5+mixin.0.8.7` (Fabric's fork):

- `MixinProcessor.applyMixins` owns every class under a config's
  package. A classload of any of them either gets the accessor
  carve-out or throws `IllegalClassLoadError`.
- The carve-out lives in `MixinCoprocessorAccessor`: it passes through
  **registered accessor mixins only** (`@Mixin` interfaces whose members
  are all `@Accessor`/`@Invoker`, like `ChatComponentAccessor`).
- `ScreenWidgetAccess` is a plain duck interface: unregistered, not
  `@Mixin`. It is merged into the transformed `Screen`'s interface list,
  and the JVM resolves a class's superinterfaces eagerly at define time.
  So the first load of `Screen` — deep in startup — tried to load the
  interface, Mixin refused, game over. Every build carrying it was
  unlaunchable in production; nothing before dev.15 shipped it to a
  real client.

Why the dev environment never saw it: the dev classpath loads mod
classes through a different code path, and the unit tests never define
the transformed `Screen`.

### Fix

- `ScreenWidgetAccess` moved to the new `net.zaminmc.zea.duck` package,
  outside Mixin's ownership. `ScreenMixin` (implements) and
  `ChatScreenMixin` (two casts) now import it from there. The duck
  interface's javadoc states the placement rule and points at the
  hygiene test.
- `MixinPackageHygieneTest` scans the compiled production classes with
  ASM and turns both loadability rules into build failures:
  1. every class under `net/zaminmc/zea/mixin/` must be registered in
     `zea.mixins.json` (stray classes — including nested ones — are
     unloadable in production);
  2. any bytecode reference to a mixin-package class must target a
     registered accessor mixin (interface, `@Mixin`, members all
     `@Accessor`/`@Invoker`).
  Detail that cost a debugging round: Mixin's annotations have CLASS
  retention, so ASM reports them as *invisible* annotations
  (`RetentionPolicy.CLASS`), not visible ones.

### Tests performed

- `./gradlew test` green; 490 tests, 0 failures (was 488). The two new
  hygiene tests pass against the fixed tree; rule 2 was observed firing
  (correctly flagging the shape) during development before the fix
  landed.
- Jar verification: `net/zaminmc/zea/duck/ScreenWidgetAccess.class`
  present, `mixin/ScreenWidgetAccess` gone, `ScreenMixin.class`
  references the new location.
- In-game testing: none available in this environment. The fix is a
  bytecode-level constraint, so the launch crash is structurally
  impossible now; the player's next dev-build run confirms it.

### Outstanding problems

- The untracked multi-send interaction layer (`MultiSendInteraction`
  policy class) remains to be wired into the tab bar and chat screen —
  continuing next.
