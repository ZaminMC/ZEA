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

- ZEA: `README.md`, `ARCHITECTURE.md`, `ROADMAP.md`, this file,
  `docs/{DEPENDENCIES,EARTHMC_API,EARTHMC_MESSAGE_FORMATS,TESTING,PASTE_SERVICE}.md`,
  `LICENSE`, build scaffolding and Stage 1 foundation (see commits).
- ZEA-REPO: `docs/DESIGN.md`, `README.md`, `manifest.json`, `schemas/`,
  `datasets/{badges,channels}.json`, `CONTRIBUTING.md`, `scripts/validate.py`,
  `.github/workflows/validate.yml`.

### Tests performed

- Build: `./gradlew build` (results recorded per commit in the commit that
  introduces the change; see below).
- Unit tests: recorded per commit.
- In-game testing: **none performed this session.** No claims are made about
  in-game behavior.

### Outstanding problems

- `paste.helpcha.at` unreachable from this environment — live protocol
  verification pending (mock-server tests planned for Stage 8).
- Nickname → real username resolution source unresolved (Stage 4
  investigation).
- Server identity key for history isolation unresolved (Stage 7).
- In-game verification of the full writable channel list pending; the
  ZEA-REPO `channels` dataset allows corrections without mod releases.
