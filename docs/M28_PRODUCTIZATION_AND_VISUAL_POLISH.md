# M28 - Productization & Visual Polish

Status: complete and manually accepted. The implementation sections below retain their
pre-acceptance checkpoint framing.

M28 resolves the accepted technical audit findings and the concrete M28 visual observations.
It does not change the Document/Math AST, transfer semantics, persistence V1, history model,
Figure ownership, or public extension surface. No M29 work is included.

## FIX NOW Resolution Matrix

| Finding | Class | Files / owner | Implementation | Tests | Status |
| --- | --- | --- | --- | --- | --- |
| B1 whole Equation fallback empty | Blocking | `FragmentPlainTextExporter` | Whole equations use `MathPlainTextSerializer`, including structured nested math. | `M28ProductizationTest` | RESOLVED |
| I1 bound Plot/Figure fallback loses data | Important | fragment/Figure exporters | Dataset-bound plots resolve against the source Document before external text serialization. | `M28ProductizationTest` | RESOLVED |
| I2 development runtime ungated | Important | `ScholarClient`, development command boundary | NeoForge `FMLEnvironment.production` gates registration; fixture commands live behind `DevelopmentClientCommands`. | `DevelopmentRuntimeGateTest` | RESOLVED |
| I3 repeated modal geometry/input | Important | shell UI / editor screen | Extracted pure viewport-clamped `ModalGeometry`; all six editor modal rectangles share it. Existing focus priority remains intact. | `M28ShellPolicyTest`, existing input tests | RESOLVED |
| I4 shortcut label/matcher split | Important | action metadata / client matcher | `ActionShortcut` owns semantic strokes and labels; `MinecraftShortcutMatcher` consumes the same strokes. | `M28ShellPolicyTest` | RESOLVED |
| I5 demo insertion defaults | Important | `ScientificContentDefaults` | Plot, Diagram, and Dataset insertion now creates minimal neutral editable values. Equation/Table were already minimal; Figure remains an explicit wrapper action. | `M28ProductizationTest` | RESOLVED |
| I6 EditorSession concentration | Important | editor defaults | Broad refactor rejected. Only M28-owned scientific default construction moved out of the session; remaining concentration stays deferred. | defaults and full editor regression | RECLASSIFIED WITH EVIDENCE |
| M1 stale M27 status | Minor | project docs | M27 is recorded as complete/accepted; M28 is recorded as technical complete/manual pending. | documentation review | RESOLVED |
| M2 duplicate scroll policy | Minor | shell layout / viewer / editor | Shared scroll step and pure `ScrollGeometry.clamp` now serve both document screens. | existing `ScrollGeometryTest` | RESOLVED |
| M3 raw Escape key | Minor | `ToolbarWidget` | Replaced raw 256 with `GLFW_KEY_ESCAPE`. | existing menu input regression | RESOLVED |

## Clipboard And Fallback

Whole `EquationBlock` copy exports readable infix/root/script text and never silently emits
an empty fallback for supported Math AST. Dataset-backed Plot roots and Plot-containing
Figures resolve source datasets before serialization, so the text includes the points the
user sees. Rich payloads remain authoritative for Scholar-to-Scholar transfer. Plain text
remains lossy external interoperability, not persistence or a round-trip schema.

## Development And Release Boundary

`ScholarClient` checks NeoForge's `FMLEnvironment.production`. In development it delegates
registration to `DevelopmentClientCommands`, preserving `/scholar_dev_viewer`, the default
M28 fixture, and all named stress profiles. In production it registers none of those
commands and does not execute fixture construction. File > New remains the ordinary empty
document. No environment guessing, config sentinel, or persistence coupling was added.

## Authoring Defaults

`ScientificContentDefaults` owns only current minimal production defaults:

- Plot: Untitled Plot, x/y axes, one empty line series.
- Diagram: Untitled Diagram, valid canvas, no elements or connections.
- Dataset: Untitled Dataset, x/y numeric columns, no rows.
- Equation: existing empty MathSequence.
- Table: existing empty 2x2 table.
- Figure: existing explicit wrap of a selected Plot or Diagram; no demo content is inserted.

Rich scientific examples remain in `DevelopmentDocument` and `DevelopmentStressDocument`.

## Shortcut Authority

`ActionShortcut` is platform-neutral and contains one or more semantic strokes. Its first
stroke generates the displayed label, while the client matcher checks all strokes. Redo
therefore displays Ctrl+Y while accepting both Ctrl+Y and Ctrl+Shift+Z from the same value.
GLFW remains confined to the client matcher. File actions use the same contract.

## UI Infrastructure And Context Menus

The screen architecture was not replaced. `ModalGeometry` centralizes centered, narrow-window
clamping for semantic token, CrossReference, Plot value, Diagram label/canvas, and electrical
component popups. `ScholarShellLayout.DOCUMENT_SCROLL_STEP` and `ScrollGeometry` remove the
viewer/editor scroll duplicate.

Context menus still execute existing `EditorAction` values. Diagram menus now inspect the
selected semantic element family: an electrical component no longer displays disabled
mechanical deletion/catalog actions, and a mechanical element does not display electrical
operations. Empty diagrams offer generic node creation; electrical/mechanical canvases offer
their relevant compact starter set plus workspace controls. Table menus retain formatting,
row, and column operations. Diagram context resolution supports both standalone diagrams and
diagrams owned by Figures without casting the outer Figure as a Diagram. Existing viewport
clamping and bounded scrolling remain.

## Visual Decisions

- Fixture headings no longer contain hand-authored numeric prefixes; `DocumentStructure`
  remains the only numbering authority.
- H3 is bold, H4 regular, H5 italic, and H6 italic with a slightly tighter line height;
  existing level-sensitive before/after spacing completes a restrained grayscale hierarchy.
- TOC indentation is 8 px per level instead of 12 px; derived entries stay aligned and valid
  at narrow widths.
- The complex QA equation now contains a real `MathRoot` instead of literal `sqrt` plus a
  separate Root. `MathGroup` is classified as a complete structured atom, restoring relation
  spacing after a closing group without changing named-operator delimiter spacing.
- Table visual metrics remain centralized in `TableLayoutEngine`; wrapping, one-pixel rules,
  header fill, padding, and active-cell rendering already satisfy the audited moderate scope.
- Plot labels/ticks/legend already have bounded pure layout and focused tests. M28 preserves
  those metrics and viewport culling because investigation found no model-level clipping bug.
- Figure content/caption spacing remains semantically owned by `DocumentLayoutEngine`; the
  existing six-pixel gap and wrapping behavior were retained.
- Electrical labels use orientation-aware alternate gutters, full component footprints, and a
  deterministic occupied-rectangle check. Junction labels therefore move to the first clear
  side instead of colliding with neighboring component annotations. No automatic schematic
  layout was introduced.
- Mechanical insertion now searches deterministic non-overlapping positions before falling
  back to center. Authored positions and manual dragging remain authoritative.
- Mechanical dimension labels use a shared three-pixel clearance on the clear side of horizontal,
  vertical, radial, diameter, aligned, and angular dimension geometry.
- The vertical mark immediately left of the focused complex equation is the expected blinking
  math caret. It is rendered only while `EquationEditingSelection` owns the active editor scope.

## Focus, History, Persistence, And Performance

Input priority remains modal, context menu, menu/toolbar, nested editor, then document.
Opening/closing visual UI creates no history. Transfer and persistence semantics are unchanged.
The integrated M28 fixture test validates zero document errors, valid selection, V1 semantic
round trip, resource-carrying Figure transfer, and exact Undo/Redo restoration. M27 full-layout,
viewport-culling, large-document, and stress policies remain intact; no cache, concurrency, or
incremental layout was introduced.

## Test Coverage

Focused coverage includes fallback, neutral defaults, environment gating, shortcut
display/matching, modal/menu geometry, contextual diagram filtering, heading fixture text,
typography hierarchy, deep TOC indentation, complex Root/group relation spacing, math caret
geometry, deterministic mechanical placement, and the integrated persistence/transfer/history
scenario. Existing M24-M27 suites remain enabled. Closeout validation passes all 1476 tests
with zero failures, errors, or skips.

## Manual Minecraft QA

Run `/scholar_dev_editor` in a development runtime, then perform these 15 checks:

1. Confirm H1-H6 hierarchy is readable and headings do not show duplicate manual numbering.
2. Inspect the TOC at normal and narrow widths; deep entries stay compact and clickable.
3. Enter all three equations; inspect the nested radical/fraction/script and `) =` spacing.
4. Inspect authored and dataset-backed tables, including wrapping, empty/missing cells, and active-cell focus.
5. Inspect both plots: title, axes, ticks, labels, legend inset, and viewport clipping.
6. Inspect both Figures and the long wrapped caption; spacing to the next block remains clear.
7. Inspect the branched electrical diagram and symbol sheet for label/terminal collisions.
8. Inspect mechanical dimensions, constraints, notes, leaders, symbols, and edge clearance.
9. Right-click prose near the lower-right edge; the text menu is relevant and clamped.
10. Right-click a table cell; formatting plus row/column actions remain reachable.
11. Right-click an electrical component; verify no mechanical command catalog appears.
12. Move among equation, table, plot, Figure caption, and diagram scopes; Escape unwinds one layer and input has one owner.
13. Open File and a field modal; verify shortcut labels, focus, buttons, and bounds.
14. Resize to a narrow window and use a larger GUI scale; no popup, text, or scientific label escapes its viewport.
15. Save As, edit, Save, close/reopen, then copy/paste and undo/redo one scientific block; semantics and selection restore exactly.

Stress checks remain available through `/scholar_dev_editor small`, `medium`, `large`,
`data_heavy`, `diagram_heavy`, and `mixed_stress` in development only.

## Remaining Deferred Work

- Broad `EditorSession` decomposition, localization, diagnostics/recovery UI, production CSV,
  legacy payload retirement, pagination, incremental layout, and large-resource policy.
- New BlockNode/InlineNode/Figure content remains high integration cost. M28 deliberately does
  not add registries, visitors, plugins, generic Figure containment, or media/bibliography.
- A normal production entry point is a product decision; M28 only prevents dev commands from
  leaking into production.

## Completion

M28 is technically complete when the full test/build/diff/boundary validation is green.
M28 remains incomplete until the user performs and accepts the Minecraft visual QA above.
