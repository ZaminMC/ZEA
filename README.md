# ZEA — Zamin's EarthMC Addon

A client-side Fabric mod for [EarthMC](https://earthmc.net). ZEA is an all-in-one
EarthMC addon: a modern chat experience with EarthMC-aware message classification,
feeds and notifications, player information, persistent chat history, inventory
buttons and configuration sharing.

ZEA is exclusively client-side. It does not require (or support) installation on
the server, and it never sends anything on your behalf unless you explicitly
configure it to.

## Status

Early development. See [ROADMAP.md](ROADMAP.md) for the staged plan and
[WORKLOG.md](WORKLOG.md) for actual progress.

Automated dev builds are published to
[releases](https://github.com/ZaminMC/ZEA/releases) on every push to
`develop`.

## Requirements

| Component | Version |
|---|---|
| Minecraft | 26.3 |
| Loader | Fabric (loader 0.20.1+) |
| Java | 25+ |

## Building

```
./gradlew build
```

The build requires a Java 25 toolchain (Minecraft 26.3 runs on Java 25).

## Documentation

- [ARCHITECTURE.md](ARCHITECTURE.md) — permanent architectural reference
- [ROADMAP.md](ROADMAP.md) — staged development plan
- [WORKLOG.md](WORKLOG.md) — chronological record of work and decisions
- [docs/](docs/) — subsystem specifications

## Repository data

Community-maintained EarthMC data consumed by ZEA lives in
[ZEA-REPO](https://github.com/ZaminMC/ZEA-REPO).

## License

ZEA is licensed under [LGPL-3.0-or-later](LICENSE).
