# HelpChat paste service — configuration sharing backend

ZEA's configuration sharing uses HelpChat's paste service. This document
records the protocol and ZEA's obligations. The hostname currently does not
resolve from the development environment (NXDOMAIN, 2026-11-08), so live
protocol verification is **pending**; the implementation is tested against
an embedded mock HTTP server and a fake transport, and must be verified
live before any claim of working integration.

## Service

- Base: `https://paste.helpcha.at`
- Upload: `POST https://paste.helpcha.at/documents`
  - `Content-Type: text/plain; charset=utf-8`
  - Body: UTF-8 document bytes (the document itself, not a wrapped API call)
  - Response: JSON containing a `key`
  - Resulting URL: `https://paste.helpcha.at/{key}`
- Raw retrieval: `https://paste.helpcha.at/raw/{key}`
  - The `.java`-style suffix seen in URLs like
    `https://paste.helpcha.at/wozajojele.java` is presentation-only syntax
    highlighting; it is not required and does not imply content type. The
    document format ZEA uses is JSON regardless.

## ZEA share document format

Versioned JSON envelope:

```json
{
  "format": "zea-share",
  "type": "inventory-buttons",
  "version": 1,
  "data": { }
}
```

- `format` must be exactly `zea-share`.
- `type` is a share category (`inven-` → `inventory-buttons`, `gen-` →
  `general`). New types only when a real category warrants one.
- `version` is the schema version of that category's `data`.
- Unknown fields are ignored on import (forward compatibility); schema
  versions ZEA does not understand are rejected, not guessed.

Export excludes by construction: API keys, tokens, credentials, local
filesystem paths, session-specific state, and anything not explicitly marked
shareable. Only share-scoped configuration fields are serialized.

## Hard rules

1. Network operations never run on the render/client thread; bounded
   executor, explicit connect/read timeouts, response bodies always closed.
2. Imports are restricted to HTTPS on exactly `paste.helpcha.at`; redirects
   to other hosts and other schemes are rejected. Paste keys are validated
   before URL construction. Response size is capped.
3. Uploads require an explicit user action; nothing uploads automatically.
4. Imported data is fully validated before application; inventory buttons
   are never executed on import; partial application is forbidden (a failing
   entry rejects the import per the documented policy of its category).
5. The service is an optional external dependency: unavailable service =
   graceful, visible failure.
6. Uploaded documents are not private or encrypted; never put secrets in
   them; never log full private configuration contents.
7. Success is not assumed from a TCP connection: HTTP status, response JSON
   shape, and key validity are all checked before presenting a URL.

## Workflow

Implemented as client commands (no UI dependency):

```
/zea share inven     export the inventory-button set
/zea share gen       export shareable general settings
/zea import <key>    fetch and validate a paste (key or full URL)
/zea import confirm  apply the pending import
/zea import cancel   discard it
```

Export: serialize the live state → round-trip the document (an export
that cannot be imported back never uploads) → upload async → the paste
URL is shown in chat. Unfinished buttons (no command or icon) are left
out and noted.

Import: paste key/URL → validate host/path → fetch raw async → size
cap → parse → validate format/type/version → validate every entry →
preview in chat → **nothing applies until `/zea import confirm`**. At
most one import is pending; a new fetch discards the old one.

## Import policies

Per category, decided and pinned by tests:

- **Inventory buttons — strict.** An entry without a command or icon, a
  duplicated identity, or a set over 100 buttons rejects the whole
  import. The local file tolerates hand-editing damage (per-entry
  recovery at load), but an import replaces the set wholesale and must
  be trustworthy as a whole. Imported buttons are inert data; nothing
  in the import path can send a command.
- **General — allowlist, field-optional.** Only the shareable fields
  decode; anything the share omits keeps the current value; every field
  validates before any is applied (one bad value rejects the import);
  numeric ranges clamp through the same bounds the config accessors use.
  Excluded by construction: chat history (local storage policy), schema
  versions, save hooks — and any future field is excluded unless it is
  explicitly added to the allowlist.

## Threading

Uploads and downloads run on the `ZEA-Share` executor (one daemon
thread, bounded queue); results marshal back to the client thread
before any feedback. The executor and HTTP client close on client
shutdown. Redirects are never followed: the service's endpoints answer
directly, so a redirect is a failure, not a silent detour to another
host.
