# Inventory buttons

Custom buttons beside container GUIs (inventory, chests, crafting) that
run a configured chat command when clicked — the EarthMC equivalent of
NEU's inventory buttons, with ZEA's safe-persistence rules.

## User-facing behavior

- Buttons render beside the container, anchored to one of its four
  corners, and run their command on left click.
- Hovering shows the button name, its command, and a warning when the
  configured icon does not exist.
- Editing is a mode, not a screen chain: while a container screen is
  open, the rebindable *Toggle button editing* key (Controls → ZEA,
  unbound by default) turns edit mode on. In edit mode:
  - buttons draw with a gold border; disabled buttons stay visible,
    dimmed;
  - a **+** ghost beside the container's top-left corner creates a new
    button;
  - dragging a button moves it (a few pixels of jitter is ignored) and
    releasing it re-anchors it to the nearest corner and saves;
  - right-clicking a button opens the edit form: name, command, icon
    item, anchor, enabled, delete (delete asks for a second click);
  - Esc leaves edit mode before it closes the screen.
- The master toggle (config category *Inventory buttons*) hides the
  whole subsystem without touching the stored buttons. It is on by
  default — with no buttons configured, nothing renders.

## Model

`net.zaminmc.zea.buttons`:

| Class | Role |
|---|---|
| `InventoryButton` | The immutable record: id, name, command, icon, anchor, offsets, enabled. Sanitizes partial data in its constructor (nulls/blank → defaults, control characters stripped, lengths truncated, offsets clamped). |
| `ButtonAnchor` | The four container corners; semantics only. |
| `ButtonGeometry` | Pure layout math: position from anchor + offset, hit tests, inverse (position → offset), nearest anchor. |
| `InventoryButtonStore` | Safe persistence (see below). |
| `InventoryButtons` | Coordinator: the loaded set, edit-mode state, hit tests, CRUD. |
| `ButtonActivator` | The single path from a click to a sent command. |
| `ButtonRenderer` | Presentation only: boxes, icons, tooltips, ghost, hint. |
| `ButtonIcons` | Icon id → `ItemStack`, cached; barrier fallback for unknown ids. |
| `ButtonTooltips` | Pure tooltip text assembly. |
| `ButtonDragSession` | The edit-mode drag state machine. |
| `ButtonEditForm` | The edit form's decision logic: apply/validate/refuse, two-click delete. |
| `ButtonEditScreen` | The edit form (vanilla widgets), delegating to `ButtonEditForm`. |

## Anchoring

A button's top-left corner sits at the anchor's *flush position* (the
container corner inset by the button size) plus its offset; offsets are
always right/down positive. New buttons default to `TOP_RIGHT` with
offset (22, 2), which places them beside the container's right edge.

When a drag ends, the button re-anchors to the corner nearest its final
position. Offsets are clamped to ±512, so buttons stay near the GUI
across the different container sizes (inventory, chest, crafting).

## Persistence

- File: `<configdir>/zea/buttons.json`, shape
  `{"version": 1, "buttons": [...]}`. The `version` field gates future
  migrations like `configVersion` does for the main configuration.
- Writes go through the safe-write protocol (in-memory serialize,
  round-trip verify, atomic replace); corrupt files are preserved as
  `*.corrupted` and fall back to an empty set.
- A tolerant wire record (`ButtonEntry`) deserializes each entry: absent
  or null fields become model defaults, because Gson rejects explicit
  nulls on primitive record components and one damaged entry must not
  quarantine the whole file.
- Per-entry policy on load and save: unusable entries (no command or
  icon after normalization) are dropped with a warning; duplicated ids
  are re-issued. Buttons are independent, so dropping one entry never
  invalidates the others.
- Saving happens only on real changes (drag end, edit form Done/Delete),
  never on a timer.

## Command execution — the guarantees

1. `ButtonActivator` is the **only** code path that can send a button's
   command. The store, the coordinator, the renderer and the edit screen
   have no reference to any sender; loading or importing buttons is
   inert by construction.
2. Every click re-validates the command (non-blank, ≤ 256 chars, no
   control characters) — a hand-edited or imported file cannot smuggle a
   malformed command past the click.
3. Each button has a 500 ms cooldown, so a double-click sends exactly
   one command. A failed send (not connected) does not arm the cooldown.
4. Commands are sent through `ClientPacketListener.sendCommand` — the
   same path as the chat dispatcher, no confirmation popups.

## Minecraft integration (26.3, verified from bytecode)

`ContainerScreenMixin` targets `AbstractContainerScreen`:

| Target | Injection | Purpose |
|---|---|---|
| `extractContents` | TAIL | Draw buttons after the container contents; the pose is popped, so buttons draw in screen coordinates. Carried items (next stratum) and tooltips stay on top. |
| `extractTooltip` | HEAD, cancellable | A hovered button suppresses the slot tooltip so the two never stack. |
| `keyPressed` | HEAD, cancellable | Esc leaves edit mode first; the edit keybind toggles it. |
| `mouseClicked` | HEAD, cancellable | Buttons take priority over slots; edit mode branches to drag/edit/ghost. |
| `mouseDragged` | HEAD, cancellable | Moves the drag preview while a button is grabbed. |
| `mouseReleased` | HEAD, cancellable | Ends the drag (re-anchor + save); otherwise consumes releases over buttons so vanilla never treats the button area as "outside the GUI" (which drops a carried item). Mid-quick-craft releases pass through. |
| `removed` | HEAD | Cancels a half-finished drag when the screen closes. |

Notes on the render architecture: 26.3 replaced `render(GuiGraphics)`
with `extractRenderState(GuiGraphicsExtractor, …)`; icons draw through
`GuiGraphicsExtractor.item(ItemStack, x, y)`, tooltips through
`setComponentTooltipForNextFrame`. The edit screen uses the vanilla
`LinearLayout`/`FrameLayout` pattern from `ConfirmScreen`, and returns
to the container screen with `gui.setScreen(parent)` — `menu.removed`
is a no-op on the client, so the container survives.

Everything is gated on the EarthMC connection; on other servers,
container screens are untouched.

## Known limitations

- The 500 ms click cooldown is fixed, not configurable (a deliberate
  restraint; nothing in the spec calls for a setting).
- Buttons can be dragged over the container itself; their clicks then
  take priority over the slots underneath. This is allowed — the drag is
  the user's explicit choice.
- Rendering, hover and tooltips are verified against 26.3 bytecode and
  unit tests only; they have not been exercised in a live client yet
  (see WORKLOG). The edit form's decisions (validation order, refusal
  reasons, two-click delete) are covered by `ButtonEditFormTest`; the
  drag interaction remains bytecode-verified only.
- Commands are sent as typed; there is no client-side command allowlist
  beyond the validation rules above. ZEA never invents commands — the
  user configures exactly what runs.
