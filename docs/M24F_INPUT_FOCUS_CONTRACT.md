# M24F Input, Focus, And Interaction Contract

Status: Accepted.

M24F defines the editor input boundary for the current Scholar shell. It does not add new authoring domains. Its purpose is to make every keyboard, character, mouse, scroll, context-menu, and popup interaction resolve to one authoritative consumer.

## Dispatch Priority

Input is consumed in this order:

1. Modal editor popups such as semantic math token, cross-reference, plot value, diagram label, diagram canvas, and electrical component popups.
2. Open context menu.
3. Open menu bar or toolbar dropdown.
4. Active nested editor interaction such as table, plot, diagram, equation, or figure caption.
5. Document-level editing and navigation.
6. Screen fallback.

Higher-priority consumers either handle the event or intentionally consume it as isolation. Lower layers must not also mutate state from the same event.

## Focus Owner

The core editor exposes one semantic focus owner derived from `EditorState.selection()`:

- `DOCUMENT_TEXT` for prose/heading text selections.
- `BLOCK` for atomic block selection.
- `EQUATION` for equation content editing.
- `TABLE` for table cell editing.
- `PLOT` for plot target editing.
- `DIAGRAM` for diagram target editing.
- `FIGURE_CAPTION` for figure caption editing.

The focus owner is not persisted document data. It is a deterministic view of transient selection state.

## Event Consumption

- Character input goes only to the active popup or focused editor domain.
- Control shortcuts do not also emit text.
- Context menu actions route through normal `EditorAction` enable checks and execution.
- Menu and toolbar popups isolate keyboard and pointer input until closed or consumed.
- Diagram drag preview is transient; commit is the single semantic mutation.
- Diagram pan/zoom viewport state remains transient UI state.

## Key Behavior Matrix

| Event | Popup | Context/Menu/Toolbar | Nested Editor | Document |
| --- | --- | --- | --- | --- |
| Character | popup text field | consumed if menu open | focused nested domain | prose insertion |
| Escape | close/cancel popup | close menu | exit/cancel one nested layer | no semantic mutation |
| Enter | apply popup | menu activate | nested-domain action where supported | split/enter block |
| Tab | popup field switch | menu focus behavior | table/plot/diagram target traversal | no document mutation |
| Ctrl+A | popup-specific or consumed | consumed if menu open | active nested scope | contiguous text scope |
| Ctrl+C/X/V | popup-specific or consumed | consumed if menu open | active selection/action | active selection/action |
| Arrows | popup-specific or consumed | menu navigation | active nested movement where supported | document navigation |
| Home/End | popup-specific or consumed | menu navigation | active nested movement where supported | visual line navigation |
| Mouse click | popup hit-test | menu/context hit-test | nested hit-test | document hit-test |
| Drag | consumed by active transient interaction | isolated | selection/diagram drag | text selection drag |
| Scroll | popup/menu where applicable | popup/menu where applicable | document viewport unless owned | document viewport |

## Nested Entry And Exit

Entering an equation, table, plot, diagram, or figure caption changes only transient selection/focus and does not create undo history. Exiting a nested editor returns to an appropriate block or outer selection without mutating the document.

Escape unwinds one interaction layer:

- popup open: close/cancel popup;
- diagram drag active: cancel drag preview;
- diagram connection/constraint active: cancel that transient mode;
- nested table/plot/diagram focus: exit nested editing;
- otherwise no semantic mutation.

## History

Only semantic document mutations create history transactions. Focus moves, selection changes, context menu open/close, popup open/close, diagram drag preview, outline navigation, scroll, zoom, and pan do not.

Undo/redo restores a complete semantic editor snapshot, including the selection that owns focus. Transient interaction state such as drag preview, pending diagram connection, and pending mechanical constraint state is cleared when explicit current state changes occur.

## Minecraft Boundary

The dispatch order lives in the client screen. The semantic focus owner, selection validation, nested-editing APIs, and history behavior remain in Minecraft-independent editor core.

