# EarthMC API (v4)

Verified reference for ZEA's EarthMC integration. Sources: official docs at
<https://earthmc.net/docs/api>, the live OpenAPI spec at
<https://api.earthmc.net/docs> (title "EarthMC API", version 4), and the
server implementation at <https://github.com/EarthMC/EMCAPI> (a Paper/Folia
plugin, not an SDK). Verified 2026-11-08.

## Base URL and versioning

- Base: `https://api.earthmc.net/v4`
- v1/v2 were disabled 2025-01-14; v3 returns 404. Current version: **4**.
- Map data is versioned separately: `https://api.earthmc.net/map/v1/...`

## Endpoints

| Endpoint | Method | Auth | Purpose |
|---|---|---|---|
| `/v4` | GET | public | Server info, stats, vote party |
| `/v4/towns` | GET | public | List `{name, uuid}` |
| `/v4/towns` | POST | public | Full town objects by query |
| `/v4/nations` | GET/POST | public | Same pattern as towns |
| `/v4/players` | GET/POST | public | Player objects (opt-out applies) |
| `/v4/quarters` | GET/POST | public | Quarters |
| `/v4/location` | POST | public | X,Z → town/nation/wilderness |
| `/v4/nearby` | POST | public | Nearby towns by radius |
| `/v4/online` | GET | public | Online count + `{name, uuid}` list |
| `/v4/mm` | GET | public | Mystery Master top 50 |
| `/v4/advancements` | GET | public | Advancements |
| `/v4/events` | GET (SSE) | **key** | Server events stream |
| `/v4/shop` | POST | **key** | Your shops |
| `/v4/mcmmo`, `/v4/mcmmo-top` | POST | **key** | Your mcMMO data |
| `/v4/pursuits` | POST | **key** | Your pursuits |
| `map/v1/players` | GET | public | Live player coordinates (hidden when sneaking/invisible/no sky light) |

POST bodies: `{"query": ["name" | "uuid" | "discordId"], "template": {...}, "key": "..."}`.
Templates select top-level fields; absent data is omitted. Lists (GET) return
`[{name, uuid}]`.

## Authentication model (verified)

Core data is **public, no key**. The in-game command system:

- `/api key create` / `/api key delete` / `/api key copy` — personal API keys
  (60s cooldown between modifications). Keys are passed as `"key"` in POST
  bodies, or `Authorization: Bearer <key>` for SSE.
- `/api auth` — opens the Auth Editor GUI authorizing **specific other
  players** (UUID sets) to query your shops / receive your shop SSE events.
  This is per-player shop authorization, not general API opt-in.
- `/api opt-out` — granular opt-out editor (TOWNY_RESIDENT, ONLINE_STATUS,
  QUICKSHOPS, MCMMO, override all). Opted-out players disappear from most
  endpoints; your own key still fetches your own data.

What keys unlock: SSE access, own resident data even if opted out, own
shops, mcMMO, mcmmo-top, pursuits, bank history for towns/nations you lead.

**ZEA obligations:** keys are stored locally and privately, never included
in share documents, logs, or ZEA-REPO. Onboarding must explain the command,
what it unlocks, what is stored, and how to revoke. Never circumvent
opt-outs.

## Rate limits

- 180 requests/minute globally; excess → HTTP 429 with `Retry-After` header.
  Enforcement point is infrastructure-level (Caddy/Cloudflare), per-IP vs
  per-key scope unverified.
- Per-key endpoint cooldowns: `/mcmmo` 1h, `/mcmmo-top` 2min, `/pursuits`
  60s, `/shop` 1min. SSE: one client per key (new connection kicks old).

ZEA enforces a conservative client-side budget below these limits.

## Data notes for ZEA features

- Nation objects: king, capital, residents, towns, allies, enemies, balance,
  spawn, timestamps.
- Town objects: mayor, nation, board, founder, status flags, stats, spawn,
  homeBlock, townBlocks, residents, trusted, outlaws, ranks.
- Player objects: names/titles, town, nation, timestamps
  (registered/lastOnline), `status.isOnline`, balance, friends, discord ID.
- **Nicknames are not exposed by the API** (verified 2026-11-09 live):
  player objects carry no nickname field, and lookups by nickname 404 —
  only real usernames are indexed (observed sample: nickname `Coriolanus`
  404s, real name `pengpy` resolves). Nickname→username resolution must
  therefore come from chat component data (hover/click events on the
  displayed name), pending in-game capture, or not at all; it cannot be
  built on the API.

## Unverified / open

- Exact enforcement scope of the 180 req/min limit (proxy-level).
- `/v4/nearby` works live but is absent from the OpenAPI path list (spec gap).
