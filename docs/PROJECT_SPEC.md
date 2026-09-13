# Project Specification

Scholar is a scientific document engine for Minecraft. It should eventually support structured documents, Markdown import and export, WYSIWYG editing, mathematical notation, tables, plots, diagrams, and extension APIs for other mods and disciplines.

This document captures approved high-level scope for the current project. It should be revised as decisions become concrete.

## Approved Principles

- Scholar represents knowledge-oriented documents inside Minecraft.
- Markdown must not be the internal source of truth.
- Scholar uses a dedicated semantic document model as its source of truth.
- The initial document model uses explicit document, block, and inline node categories.
- The core document model is immutable value data.
- The initial semantic model does not include persistent node IDs.
- Inline text is represented by text content plus semantic text marks.
- Read-only rendering uses a Minecraft-independent layout model before Minecraft-specific rendering.
- Layout uses a small text measurement abstraction rather than depending directly on Minecraft fonts.
- Scholar should remain independent from the future Physics/STEM Lab project.
- Core logic should be kept independent from Minecraft where practical.
- Scientific readability has priority over forcing every visual element into pixel-art aesthetics.
- Minecraft shell typography and Scholar document typography are separate concerns.
- Document-surface typography is resolved through roles and profiles rather than semantic AST font data.
- Scholar represents knowledge; it does not determine whether that knowledge is correct.
- Large technologies should be implemented as carefully chosen subsets rather than completeness projects.
- The project should develop incrementally and remain functional at each milestone.

## Current Context Menu Slice

- Right-click menus are transient Minecraft shell UI.
- Context menus resolve relevant existing `EditorAction`s from the current semantic `EditorState`.
- Context menus do not own editing behavior and do not bypass action enable predicates.
- Right-click inside an active text or table-cell text selection preserves that selection.
- Right-click outside the active selection first targets the clicked semantic object.
- Empty document space preserves editor selection and exposes only empty-area actions.
- Context menu placement is viewport bounded and long menus scroll.
- Nested submenus remain deferred.

## Current Structural Editing Hardening Slice

- Current structural edits must leave a document with no `DocumentValidator` errors and a valid `EditorSelection`.
- Atomic block boundary deletion is two-step: first select the adjacent atomic block, then an explicit Delete/Backspace removes it.
- Deleting the only selected block leaves one empty paragraph as the editor fallback.
- Paragraph splits preserve inline content and text marks.
- Heading middle splits keep the original stable heading ID only on the left heading; the new right heading is ID-less.
- Text block merges keep the left block style and stable identity.
- Plot/Diagram wrapping creates a unique `FigureBlock`; unwrapping restores the contained semantic block.
- Dataset-backed table and plot view deletion does not delete dataset resources.
- Structural paste remaps duplicate stable IDs for supported ID-bearing payloads.
- Validators are used as structural regression oracles in tests, not as mandatory production checks after every keystroke.

## Current History Transaction Slice

- One logical semantic user edit creates at most one undoable history transaction.
- `EditorHistory` stores `EditorState` snapshots: semantic `Document`, coherent editor selection, and explicit typing marks when present.
- Undo/redo does not store menus, popups, hover, layout geometry, renderer objects, Minecraft widgets, scroll state, or diagram viewport cameras.
- Selection and caret movement, nested-editor entry/exit, outline/TOC navigation, context-menu open/close, drag begin/preview/cancel, zoom, pan, and Fit are transient and do not create undo entries.
- Semantic edits after undo clear redo; transient navigation after undo does not.
- Redo restores snapshots rather than re-executing commands, so generated stable IDs remain deterministic.
- Contiguous typing coalesces according to the current editor policy; structural and formatting edits close the typing group.
- Diagram drag commit, figure wrap/unwrap/caption edits, table edits, plot edits, dataset edits, cut, paste, and structural edits are one transaction when they change semantics.
- Copy is not history; cut and paste are history when they mutate the document.
- Derived labels, TOC/outline entries, dataset-backed views, diagram layout, and electrical nets are recomputed from restored document snapshots.

## Long-Term Goals

- Provide a structured document model suitable for scientific and educational content.
- Support a small, explicit Markdown interchange format for import and export.
- Render read-only documents inside Minecraft.
- Support a carefully scoped mathematical notation subset.
- Enable WYSIWYG editing against the document model rather than raw Markdown.
- Support tables, plots, diagrams, and extension APIs after the core model is stable.
- Allow other mods to contribute document elements or integrations through stable APIs.

## Current Table Slice

- Scholar supports an immutable `TableBlock` AST with rectangular rows and cells.
- Table cells currently contain inline document content only.
- Static table layout uses equal columns across the available document width.
- Header rows are semantic table metadata and may be rendered with derived presentation styling.
- Tables participate in document navigation and selection as atomic non-text blocks outside table editing mode.
- Table editing uses a dedicated `TableEditingSelection` with cell-local text offsets.
- Table cells support single-cell text editing, local text selection, Bold/Italic, plain-text cell clipboard, Tab and Shift+Tab cell traversal, visual-line navigation, Home, End, and global undo/redo.
- Table editing supports explicit structural row and column operations from the active cell: insert row above/below, delete row, insert column left/right, and delete column.
- Row and column operations preserve rectangular table invariants and keep editing in a collapsed caret at offset 0 in the deterministic target cell.
- Delete Row is unavailable for a one-row table, and Delete Column is unavailable for a one-column table.
- Semantic header row count remains table metadata through row and column mutations; header presentation is still derived rather than authored as Bold marks.
- Whole selected tables can be copied and cut through the shared clipboard actions using a native `TableClipboardPayload` plus TSV plain-text fallback.
- Native whole-table paste into document text inserts the copied `TableBlock` structurally; native paste over a selected `TableBlock` replaces that table.
- TSV fallback uses tab-separated cells and LF-separated rows, with no trailing tab or trailing newline. TSV does not encode header metadata or inline formatting.
- Table-cell paste remains plain text and does not distribute TSV across cells or replace the surrounding table.
- External TSV is not inferred as a `TableBlock`.
- Scholar Markdown supports a restricted pipe-table subset for rectangular tables with exactly one semantic header row.
- Markdown table serialization is unsupported for headerless tables and does not encode alignment, captions, multiline cells, CSV, or spreadsheet semantics.
- Enter and Shift+Enter inside a table cell are reserved no-ops in the first editing slice.
- Escape exits table editing and returns to whole-table `BlockSelection`.
- Multi-cell selection, TSV import, CSV import/export, table alignment, captions, and multiline cell content remain deferred.


## Current Plot Slice

- Scholar supports an immutable `PlotBlock(PlotDefinition)` semantic document block.
- The first plot model is limited to static XY data with `LINE` and `SCATTER` series over authored-order finite `double` points.
- Plot axes currently support labels, optional explicit finite ranges, and linear scale semantics only.
- Plot width is responsive to the document content width; plot height is semantic/configured with a default of 180 document pixels and a minimum of 96.
- `PlotLayoutEngine` is pure Java and computes plot bounds, label placement, resolved linear ranges, deterministic nice-number ticks, tick-label gutters, and a data-to-plot coordinate transform.
- Automatic XY ranges inspect all authored static series, do not force zero, add deterministic five-percent padding, use `[0, 1]` for empty axes, and expand constant values into a non-zero span. Explicit ranges take precedence.
- Linear ticks use deterministic 1/2/5 × 10^n steps and formatted labels that avoid ordinary floating-point noise. M16G hardening keeps subnormal finite ranges representable and falls back to finite endpoint ticks when a nice step cannot be represented.
- `PlotCoordinateTransform` maps X left-to-right and scientific Y bottom-to-top in pure Java.
- Plot tick density is layout-responsive: narrow plot areas request fewer semantic ticks and overlapping laid-out tick labels are deterministically thinned rather than rendered on top of one another.
- M16D derives visible LINE/SCATTER geometry in pure Java from the resolved ranges and coordinate transform. Scatter points outside explicit ranges are omitted; line segments crossing a resolved range are clipped to the plot area while preserving authored point order.
- LINE series render as clipped polylines and SCATTER series render as discrete markers. Empty series remain valid and one-point LINE series have no visible segment in the current slice.
- Series presentation is assigned deterministically during layout rather than authored into `PlotDefinition`: line series cycle non-color line patterns and scatter series cycle marker shapes, while the Minecraft renderer maps the same style slot to a deterministic color palette.
- When enabled and space permits, a compact derived legend is laid out inside the plot area using the same series style samples and semantic series names.
- `PlotBlock` participates in document navigation, mouse hit testing, insertion, deletion, and undo/redo as an atomic `BlockSelection` outside plot editing mode.
- Plot editing uses a dedicated `PlotEditingSelection` with typed semantic targets for title, X/Y axis labels, series, and authored data points.
- Enter on a selected plot enters plot editing; Escape returns to whole-block `BlockSelection`; Tab and Shift+Tab traverse semantic edit targets without adding history entries.
- A second click on an already-selected plot enters plot editing at the hit title/axis label, legend/series geometry, or visible authored point. `PlotHitTester` performs this mapping in pure Java from `LaidOutPlot`.
- Plot title, axis labels, series names, and finite point X/Y values can be edited through the compact plot-value popup. Plot semantic edits use immutable replacement through global `EditorHistory`.
- The Plot menu exposes explicit Grid/Legend toggles, add LINE/SCATTER series, selected-series kind changes, point insertion/deletion, and series deletion. New points begin at `(0, 0)` and no plot structure is inferred from ordinary typing.
- Whole selected PlotBlock copy/cut uses a native process-local PlotClipboardPayload for lossless Scholar-to-Scholar transfer plus a deterministic human-readable plain-text data summary for the OS clipboard.
- Native PlotBlock paste structurally inserts at document text positions and replaces a selected PlotBlock in place; it does not replace EquationBlock/TableBlock selections or act inside PlotEditingSelection.
- Plot plain-text fallback is intentionally not parsed or inferred back into PlotBlock. External plot-like text remains ordinary text when no matching Scholar clipboard payload exists.
- Plot clipboard sidecar validity follows the existing exact-plain-text match rule, so externally changing the OS clipboard invalidates native plot paste.
- Function plots, authored RGB/style controls, explicit axis-range editing UI, plot height editing UI, advanced legend placement, Markdown plot syntax, CSV/TSV plot import, and dynamic data sources remain deferred.
- Markdown explicitly rejects `PlotBlock`; no plot syntax is inferred or invented in M16B.
- Presentation `MathExpression` nodes are not executable plot-function logic.


## Current Figure Slice

- Scholar supports immutable semantic `FigureBlock(id, content, caption)` document blocks.
- Figure content is deliberately limited in M20 to existing `PlotBlock` and `DiagramBlock` values. Images and broader scientific media are deferred.
- A figure's stable `id` is stored for future references; the displayed `Figure N.` number is derived from current document order and is not stored in the AST.
- Captions are semantic `InlineContent`. Layout prepends the generated bold `Figure N.` prefix and renders the authored caption below the visual content.
- Figure layout keeps the scientific content above the caption and exposes a single figure block whose bounds include both.
- A selected figure is an atomic document block. Enter edits the contained plot or diagram, while the Figure menu exposes wrapping, caption editing, and unwrapping.
- Native whole-figure clipboard uses `FigureClipboardPayload(FigureBlock)` plus a deterministic readable plain-text fallback. Pasting preserves content/caption structure and remaps the stable ID only when needed to avoid duplicates.
- Markdown explicitly rejects `FigureBlock`; figure Markdown/media interchange remains a future milestone.


## Current Diagram Foundation

M17 is complete through M17G and manually accepted. The governing design remains `DIAGRAM_SPEC.md` plus ADRs 0178-0193.

- The document boundary is immutable `DiagramBlock(DiagramDefinition)`.
- `DiagramDefinition` owns a finite positive logical canvas, diagram-scoped element/port IDs, generic rectangular `DiagramNode` elements, perimeter ports, and validated port-to-port `DiagramConnection` references.
- Diagram semantics use a top-left logical origin with Y increasing downward. `DiagramLayoutEngine` uniformly maps the authored canvas into the available Scholar document width while preserving aspect ratio and exposes a reversible `DiagramCoordinateTransform`.
- `LaidOutDiagram` contains positioned title/node/port/connection geometry. The Minecraft renderer only consumes that geometry; it does not resolve endpoints or transform semantic coordinates.
- M17C derives side-aware deterministic orthogonal routes through `DiagramConnectionRouter`: short port exits honor perimeter orientation, all route vertices remain constrained to the laid-out canvas, and connection paths expose derived bounds for interaction.
- Perimeter port centers are mapped directly from semantic bounds + normalized offset, and laid-out ports expose explicit hit bounds.
- `DiagramHitTester` maps `LaidOutDiagram` geometry to typed title/canvas, element, port, and connection targets in pure Java with priority `port > element > connection > canvas` and bounded connection tolerance.
- Ordinary document navigation still treats `DiagramBlock` as an atomic `BlockSelection`; Enter or a second click on a selected diagram enters dedicated `DiagramEditingSelection` mode.
- M17D maps the M17C hit targets into internal selection, provides deterministic target traversal, and reverse-maps mouse positions through `DiagramCoordinateTransform` for node dragging.
- Drag previews are transient presentation documents outside `EditorHistory`; mouse release commits the final clamped logical node position as one immutable global-history transaction, while Escape cancels without a history entry.
- M17E edits diagram/node/port/connection labels, adds generic four-port nodes, removes selected nodes together with incident connections, and creates/deletes semantic port-to-port connections through explicit structural commands.
- Incomplete connection authoring is ephemeral `EditorSession` state: a source port is armed first and no AST/history mutation occurs until a distinct target port is selected and the connection is explicitly finished.
- Whole selected diagrams now use native process-local `DiagramClipboardPayload(DiagramBlock)` for lossless Scholar-to-Scholar copy/cut/paste while the OS clipboard receives a deterministic readable structural summary.
- Native DiagramBlock paste structurally inserts at ordinary text positions or replaces a selected DiagramBlock in place as one global history transaction; other selected atomic block types are not replaced.
- Diagram plain-text fallback is intentionally descriptive only. External diagram-like text is never inferred back into DiagramBlock, and changing the OS clipboard text invalidates the native sidecar through the existing exact-text match rule.
- M17G hardening keeps rounded node rectangles and fitting node/port labels within the laid-out canvas under narrow responsive reflow without mutating authored logical bounds.
- Degenerate connections whose distinct semantic endpoints resolve to the same laid-out coordinate remain valid as deterministic zero-length routed segments rather than crashing layout.
- Markdown explicitly rejects `DiagramBlock`; no diagram syntax or external text inference exists.
- Electrical and mechanical vocabulary is explicitly excluded from M17 and belongs to M18/M19.
- Obstacle-avoiding routing, multi-select, resize handles, arbitrary interior ports, rich labels, SVG/Mermaid/Graphviz interchange, simulation state, and public extension APIs remain deferred.

## Current Electrical Diagram Design

M18A-M18G are accepted and manually validated. M18H final electrical visual polish is implemented and pending authoritative Gradle/manual QA. The electrical editor now spans bounded schematic symbols, full component authoring, explicit junctions and derived semantic nets, workspace zoom/pan/canvas scaling, lossless whole-diagram clipboard/interchange regression, and hardened edge-case behavior. The governing design remains `ELECTRICAL_DIAGRAM_SPEC.md` plus ADRs 0194-0219.

- M18 extends the existing `DiagramBlock(DiagramDefinition)` rather than creating a separate electrical document block.
- The domain element is immutable `ElectricalComponent implements DiagramElement`, carrying semantic kind, logical bounds, quarter-turn orientation, reference designator, and value annotation.
- Electrical component kinds own stable terminal schemas. Generic `DiagramEndpoint(elementId, portId)` references remain the connectivity contract; rotation changes derived terminal placement but never terminal identity. M18C now covers all approved basic kinds.
- The first approved component vocabulary is resistor, capacitor, DC voltage source, ground, diode, LED, and SPST switch. M18C completes that bounded vocabulary without adding a generic user-defined symbol language.
- Electrical symbols are derived from semantic component kinds in pure Java normalized geometry. M18B implements line/polyline/circle presentation primitives and positioned electrical layout; symbol strokes/pixels/Minecraft draw calls are not stored in the AST.
- Orientation is limited initially to 0/90/180/270 degrees. Reference/value labels remain upright for document readability.
- Reference designators and value labels are authored strings, not executable quantities or simulation state. Scholar does not initially parse `10 kΩ` or `5 V` into solver input.
- Electrical wiring reuses `DiagramConnection` and M17 orthogonal routing. Crossing/touching wire geometry never implies connectivity. M18E adds explicit `ElectricalJunction` elements and derives semantic nets only from shared junction internals plus explicit endpoint-to-endpoint connections; equal net-label text does not merge disconnected nets.
- M18 reuses `DiagramEditingSelection`, M17 connection-draft interaction, logical dragging, hit testing, and global `EditorHistory`; no parallel electrical editor/history stack is introduced. M18C maps electrical terminals to generic `DiagramPortTarget` and component symbol/reference/value hits to generic `DiagramElementTarget` in pure Java. M18D generalizes immutable diagram movement through `DiagramElement.withBounds(...)`, so domain elements reuse the same drag preview/commit/cancel pipeline.
- M18D adds explicit Diagram-menu insertion for every approved electrical kind. Convenience local IDs/designators are deterministic, while authored reference/value strings remain editable and semantically non-executable.
- M18D quarter-turn rotation preserves semantic terminal IDs and existing connection endpoints, swaps authoring bounds when orientation parity changes, preserves center where possible, and clamps the component inside the logical canvas.
- M18D edits reference designator and value together through one two-field UI transaction and deletes a component plus all incident wires as one global-history edit.
- Native whole-diagram clipboard remains the lossless Scholar boundary. M18F is accepted: electrical components, terminal schemas, junctions, net labels, connections, mixed diagram content, and authored workspace height survive native transfer; the deterministic external fallback is descriptive only and external text is never parsed back into diagrams.
- M18E.5 separates authored logical canvas size and authored workspace height from transient per-diagram `DiagramViewport` zoom/pan. Fit/zoom/pan do not dirty the document or enter global history.
- M18G rebases transient per-diagram viewport state across immutable document block-index shifts so cut/paste/insertion/deletion cannot attach one diagram camera to a different neighboring diagram. Drag-preview documents deliberately do not reconcile the registry.
- M18G preserves deterministic semantic iteration for derived net endpoints/maps with insertion-ordered unmodifiable collections. This is an observability/reproducibility guarantee, not authored net ordering.
- M18G regression-hardens canvas-edge rotations, extreme responsive widths and viewport zoom, degenerate wires, empty/mixed diagrams, symbol scaling/canvas shrink, and chained undo/redo without adding new electrical semantics.
- M18H keeps terminal anchors on the authored component perimeter but makes electrical wire exits presentation-adaptive: the maximum straight exit is reduced to 8 px, exits collapse to zero when the port faces away from the counterpart, and redundant collinear electrical-route vertices are removed so no out-and-back spike protrudes beyond a connection point. This is derived layout only; AST, snapping, port hit targets, endpoint IDs, and derived nets are unchanged.
- Schematic components are explicitly separate from future physical STEM Lab breadboard/perfboard components, footprints, lead spacing, board holes, and Minecraft-scale 3D/pixel models.
- Electrical solving, SPICE/netlist semantics, PCB layout, physical board editing, arbitrary-angle rotation/mirroring, user-defined symbols, and public extension APIs remain deferred.


## Current Editor Navigation

- Left and Right move through logical user-character boundaries and across editable text block boundaries.
- Up and Down move through visual layout lines using the current layout snapshot.
- Consecutive Up and Down movement preserves an ephemeral preferred horizontal caret position in the editor session.
- Home and End target the current visual line start and end, not the containing block start and end.
- Shift variants extend the active text-selection endpoint while preserving the selection anchor.
- Non-text scientific blocks such as `EquationBlock`, `TableBlock`, `PlotBlock`, `DiagramBlock`, `FigureBlock`, and `TableOfContentsBlock` participate in plain document navigation as atomic block selections.
- Up, Down, Home, and End inside structured math editing remain deferred.
- `EditorState` validates transient selections against the current document. Text selections require contiguous editable inline blocks and cannot cross atomic non-text blocks.
- Nested selections validate against their owning block: equations use math positions, tables use cell-local offsets against the resolved table view, plots and diagrams use semantic targets, and figure captions use caption-local logical offsets.
- `Ctrl+A` selects the current editing scope rather than the whole document: contiguous prose/heading run, equation root, current table cell, or figure caption. Atomic block, plot-editing, and diagram-editing selections remain unchanged.
- Inline cross-references are atomic source nodes. They contribute one logical caret unit even when their rendered labels are derived from current document numbering.

## Current Cross-Reference Slice

- Cross-references are first-class semantic inline nodes, not plain rendered labels.
- A reference stores `CrossReferenceTargetKind` plus stable target ID; displayed labels are derived from the current document.
- The initial supported target kinds are Figure, Table, Equation, and Section.
- `FigureBlock` already owns a stable ID; headings, tables, and equations may now carry optional stable IDs when they should be reference targets.
- Unresolved references remain in the document and render/export as `[Missing reference]`.
- Inline layout renders resolved labels through the normal text pipeline while preserving the reference as one logical source character for editing.
- The editor offers Insert > Cross Reference using a target picker instead of raw ID entry.
- Native Scholar clipboard preserves inline references inside selected text when possible; the OS clipboard receives resolved readable text.
- Markdown export writes resolved readable labels only. Semantic Markdown reference round-trip remains deferred.

## Current Document Structure And Navigation Slice

- Document structure is derived from the flat ordered block list; there is no authoritative `Section` AST.
- Headings may carry stable IDs when they are navigation/reference targets.
- Section numbers are derived display state and are not stored in heading content or metadata.
- Skipped heading levels use deterministic zero placeholders, for example `1.0.1`.
- Heading rendering prefixes the derived section number as display-only text; the prefix is not editable heading content.
- Section cross-reference labels use the same structure resolver and display hierarchical labels such as `Section 2.1`.
- `TableOfContentsBlock` is semantic and stores no entries; TOC layout/plain text derive entries from current headings.
- The editor outline is transient UI state derived from the same structure resolver.
- TOC and outline navigation resolve stable heading IDs to current block positions and do not create undo history.
- Pasting a copied heading remaps duplicate stable IDs. Pasted TOC blocks remain semantic and derive entries from the destination document.

## Current Dataset Slice

- Scholar documents can own immutable `ScientificDataset` resources separately from their ordered block list.
- Dataset IDs are stable document-local identifiers. Column IDs are stable within a dataset and are separate from editable display names.
- Row IDs are optional and reserved for future workflows; M23 row editing remains ordered/index-based.
- Dataset cells store numeric decimal values, text values, or explicit missing values. Units, formulas, uncertainty, and spreadsheet semantics remain deferred.
- Dataset-backed tables use `DatasetTableBinding(datasetId, columnIds)` and are resolved as current table views during layout and export. Empty `columnIds` means all current dataset columns.
- Dataset-backed plot series use `DatasetPlotBinding(datasetId, xColumnId, yColumnId)` and are resolved into current XY points during layout.
- Dataset-backed views do not own copied data. Editing a dataset propagates to every bound table and plot view through resolver output.
- Missing datasets and missing selected columns produce deterministic broken table states; missing dataset/column plot bindings resolve to empty series data rather than invented points.
- Dataset-backed plot resolution skips rows where either X or Y is missing or nonnumeric.
- Whole dataset native clipboard uses a `DatasetClipboardPayload` plus a readable TSV fallback. Pasting a duplicate dataset remaps the dataset ID instead of overwriting existing data.
- Markdown and plain-text export flatten dataset-backed tables as current resolved snapshots. Scholar does not yet define Markdown dataset syntax.

## Current Validation Slice

- Scholar provides a pure Java `DocumentValidator` for structural document diagnostics.
- Validation is read-only and never mutates, normalizes, repairs, renders, or launches Minecraft.
- Diagnostics distinguish structural `ERROR`s from valid degraded-state `WARNING`s.
- Stable IDs are unique within their own namespaces: figures, headings/sections, tables, equations, datasets, dataset columns, and diagram elements.
- Broken cross-references and missing dataset bindings remain valid degraded state and are reported as warnings.
- Validation is intended for trust boundaries such as import, load, save, and explicit diagnostics rather than being embedded as renderer behavior.

## Current Input And Focus Slice

- `EditorSession.focusOwner()` derives one semantic input owner from the current `EditorState.selection()`.
- The current focus owners are document text, atomic block, equation, table, plot, diagram, and figure caption.
- Minecraft screen dispatch prioritizes modal popups, context menus, menu/toolbar popups, nested editors, document editing, and then screen fallback.
- Higher-priority UI layers consume events for isolation so one physical input event cannot mutate multiple editor domains.
- Entering and leaving nested editing modes is transient selection/focus movement and does not create undo history.
- Escape unwinds one active interaction layer at a time.
- `Ctrl+A` selects the active editing scope rather than the whole document.
- Menu/context actions continue to route through `EditorAction` enable guards.

## Current Editor Foundation v1

- M24G closes the current editor-foundation pass without adding a new feature surface.
- The accepted foundation is documented in `M24_EDITOR_FOUNDATION_V1.md`.
- The foundation regression suite now includes canonical mixed-document coverage across prose, math, authored and dataset-backed tables, authored and dataset-backed plots, diagrams, electrical/mechanical vocabularies, figures, captions, cross-references, TOC, datasets, validation, context actions, focus, clipboard, and history.
- M24G classifies current invariants as model-enforced, validator-enforced, editor-state-enforced, history-enforced, interaction-enforced, or documented-only in `M24G_FOUNDATION_AUDIT.md`.
- No M24G ADRs were added because this milestone consolidates M24A-M24F decisions rather than making new architecture decisions.

## Non-Goals For The Current Milestone

- M24G is the current implementation slice. It adds final foundation-level regression coverage, documentation, and manual QA fixtures only; document-wide multi-object selection, repair workflows, persistence migration, validation UI, schema serialization, and automatic fixes remain deferred.
- M23 adds reusable dataset resources and first dataset-backed table/plot views only; units, formulas, spreadsheet semantics, dataset persistence format design, dataset manager UI, dynamic data sources, and dataset-backed Markdown syntax remain deferred.
- Electrical simulation, PCB/breadboard/perfboard physical layout, arbitrary-angle rotation/mirroring, user-defined symbols, netlist import/export, and automatic net inference remain outside the initial M18 scope.
- Node resizing, port creation/deletion/repositioning, snapping, multi-selection, and partial element clipboard remain deferred unless later diagram-domain evidence requires them.
- No function evaluation, authored series styling, advanced chart types, or dynamic plot sources in the current plot slice.
- No row or column resizing.
- No multi-cell table selection.
- No spreadsheet semantics.
- No table alignment, captions, or multiline cell content.
- No networking.
- No gameplay systems.
- No physics systems.
- No public extension API.

## Unresolved Design Decisions

- Serialization format for the internal document model.
- Supported Markdown subset.
- Mathematical notation subset and rendering approach.
- Rendering strategy inside Minecraft.
- Editing model and user interaction design.
- Package boundaries for future rendering, editing, and API code.
- Whether and when to split the repository into multiple Gradle modules.


### Mechanical diagram status

Mechanical diagram authoring now includes M19A primitives, M19B semantic dimensions/callouts, and
M19C semantic geometric relationships. Mechanical constraints remain technical-drawing semantics;
they do not imply physical simulation.
