# Dependencies

Every runtime dependency and the reason it exists. Verified versions only;
compatibility is never assumed.

## Build toolchain

| Component | Version | Source of truth |
|---|---|---|
| Minecraft | 26.3 | `fabric-example-mod` default branch (now `26.3`); requires Java 25 |
| Fabric Loader | 0.20.1 | `meta.fabricmc.net`, example mod |
| Fabric API | 0.165.2+26.3 | `maven.fabricmc.net` metadata |
| Loom | 1.19-SNAPSHOT | `fabricmc.net/develop` recommendation |
| Gradle | 9.8.2 (wrapper) | example mod wrapper |
| Java | 25 | Minecraft 26.3 `javaVersion` (piston-meta: `java-runtime-epsilon`, major 25) |
| Mappings | official Mojang | Yarn ends at 1.21.11; example mod declares none — Loom defaults to official |

## Runtime dependencies

### Fabric API

Standard Fabric module set (lifecycle events, client events, commands,
keybindings). Required — ZEA uses its event hooks instead of adding more
mixins wherever an official hook exists.

### MoulConfig (`org.notenoughupdates.moulconfig:modern-26.3:4.8.0`)

- **Why:** the intended configuration framework. Verified published for
  MC 26.3 at `https://maven.notenoughupdates.org/releases/` (metadata
  `lastUpdated 20261013192826`). LGPL-3.0-or-later.
- **Integration:** Fabric jar-in-jar (`include`). MoulConfig remains a
  separate, unmodified mod jar at runtime — clean LGPL compliance and the
  idiomatic Fabric mechanism. Loader deduplicates JiB copies if another mod
  bundles a different version.
- **Fallback (if JiB proves unworkable):** shadow + relocate
  (`io.github.notenoughupdates.moulconfig` → `net.zaminmc.zea.dep.moulconfig`)
  per MoulConfig's official consumer recipe. Requires the shadow plugin and
  copying its access widener. Not used unless necessary.
- **Excluded:** OneConfig (Polyfrost) — project constraint. Lattice is
  GUI-only without persistence and does not fit.

## What is deliberately NOT a dependency

- **No HTTP client library.** `java.net.http.HttpClient` (JDK) with virtual
  threads, as proven by the EarthMC Recruitment Addon reference. ZEA's needs
  (REST + uploads + SSE later) are covered.
- **No JSON library beyond Gson.** Gson ships with Minecraft/Fabric; all ZEA
  formats are designed for it.
- **No UI framework.** Vanilla widget extension plus MoulConfig for options.
- **No database engine.** Chat history uses append-oriented JSON storage
  with atomic replacement (Stage 7 will document the exact format).

Adding a dependency requires a concrete requirement this list cannot satisfy,
and an entry here explaining it.
