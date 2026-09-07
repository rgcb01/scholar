# Scientific Diagram Foundation Specification

This document captures the approved M17A architecture for Scholar's first semantic scientific-diagram foundation. M17 is infrastructure for later discipline-specific diagram vocabularies; it is not itself an electrical or mechanical simulator/editor.

## Scope

M17 introduces an immutable semantic `DiagramBlock(DiagramDefinition)` whose contents can be laid out, rendered, selected, edited, moved, connected, copied, and hardened without making Minecraft rendering the source of truth.

The first implementation remains deliberately small:

- One generic rectangular node element.
- Edge/perimeter ports.
- Port-to-port connections.
- Optional plain-string labels for diagram, nodes, ports, and connections.
- Local logical coordinates independent from screen pixels.
- Responsive layout into the Scholar document surface.
- Deterministic derived connection routing.
- Atomic document-level selection plus a dedicated internal diagram-editing mode.
- Pure-Java layout and hit testing.
- Global Scholar undo/redo rather than a diagram-specific history system.

M17 does not define resistor, capacitor, battery, gear, spring, force-vector, or other discipline-specific semantics. Those belong to later milestones built on this foundation.

## Semantic Model

The intended first model is conceptually:

```text
DiagramBlock
└─ DiagramDefinition
   ├─ title: String
   ├─ canvas: DiagramCanvas
   ├─ elements: List<DiagramElement>
   │  └─ DiagramNode
   │     ├─ id: DiagramElementId
   │     ├─ bounds: DiagramBounds
   │     ├─ label: String
   │     └─ ports: List<DiagramPort>
   │        ├─ id: DiagramPortId
   │        ├─ label: String
   │        └─ placement: DiagramPortPlacement
   └─ connections: List<DiagramConnection>
      ├─ source: DiagramEndpoint
      ├─ target: DiagramEndpoint
      └─ label: String
```

The exact Java declarations are an implementation detail of M17B, but the semantic responsibilities above are approved by M17A.

### DiagramDefinition

`DiagramDefinition` is the semantic source of truth for one diagram. It owns the diagram-local canvas, elements, and connections. Presentation geometry such as routed polylines, screen coordinates, hover bounds, selection outlines, and Minecraft colors are not stored here.

A blank title is valid.

### DiagramElement

`DiagramElement` is the diagram-level semantic element abstraction. M17 initially provides only a generic rectangular `DiagramNode`. This gives the foundation real implementation evidence without pretending that every future scientific symbol is just a generic box.

Future M18/M19 element families may add richer element types while reusing the same canvas, connectivity, layout/editor boundaries, and history concepts.

### DiagramNode

The first `DiagramNode` owns:

- a diagram-scoped stable element ID;
- local logical bounds;
- an optional plain-string label;
- zero or more perimeter ports.

Node overlap is not a semantic-model error. Avoidance, snapping, spacing, or overlap warnings are editor/layout policies rather than AST validity rules.

### DiagramPort

A port is a semantic connection point owned by one element. The initial placement model is perimeter-based:

```text
DiagramPortPlacement(side, offset)
```

where `side` is one of `LEFT`, `RIGHT`, `TOP`, or `BOTTOM`, and `offset` is a finite normalized value in `[0, 1]` measured along that side.

This is sufficient for the first generic node foundation and maps naturally onto later electrical pins. Arbitrary interior anchors are deferred until a concrete later domain requires them.

### DiagramConnection

A connection stores semantic endpoint references, not authored line segments:

```text
DiagramEndpoint(elementId, portId)
```

Connections are undirected in the first M17 slice. Directional arrows, bus semantics, wire classes, mechanical joints, constraints, and domain-specific connectivity rules are deferred.

An optional plain-string connection label is semantic content. Its visual placement is derived by layout.

## Diagram-Scoped Identity

The general Scholar document AST still does not gain persistent IDs. Diagram connectivity creates a concrete local reference requirement that did not exist for Paragraph, EquationBlock, TableBlock, or PlotBlock.

Therefore M17 introduces IDs only where connectivity requires them:

- `DiagramElementId` is unique within one `DiagramDefinition`.
- `DiagramPortId` is unique within its owning element.
- A `DiagramEndpoint` resolves an element ID plus one of that element's port IDs.

These IDs are semantic reference identity, not general editor identity and not global document identity. They survive immutable edits to the same diagram and lossless whole-diagram clipboard transfer.

Connections do not require their own persistent IDs in the first slice; snapshot-local connection indices are sufficient for editor addressing unless later requirements prove otherwise.

## Model Invariants

The first implementation should enforce at construction boundaries:

- Canvas width and height are finite and strictly positive.
- Element IDs are non-blank and unique within a diagram.
- Port IDs are non-blank and unique within their owning element.
- Node bounds contain finite coordinates and strictly positive width/height.
- Node bounds remain inside the authored canvas.
- Port offsets are finite and in `[0, 1]`.
- Every connection endpoint resolves to exactly one existing element port.
- A connection cannot connect an endpoint to itself.
- Lists are immutable defensive copies.
- Labels are non-null; blank labels are valid.

The model should not reject element overlap, connection crossing, duplicate parallel connections, same-element connections between distinct ports, or visually awkward layouts. Those are not structural corruption.

Deleting an element in the editor must remove its incident connections in the same immutable history transaction so that a valid `DiagramDefinition` is never committed with dangling endpoints.

## Coordinates And Responsive Layout

Diagram semantics use local logical canvas coordinates rather than Minecraft/document pixels.

The initial canvas uses a top-left origin:

```text
(0,0) ─────────────→ +X
  │
  │
  ↓ +Y
```

This is intentionally different from scientific plot Y coordinates. A diagram is a spatial drawing surface, so screen-like Y-down local coordinates minimize authoring and dragging complexity.

`DiagramBounds` values are authored in these logical units. Layout chooses a uniform scale that maps the complete logical canvas into the available Scholar document width while preserving aspect ratio. The resulting block height is derived from the canvas aspect ratio plus any approved title/padding reservation.

Resize/reflow therefore changes laid-out pixels but not semantic node positions.

A bidirectional transform is required:

```text
logical diagram coordinates <-> laid-out document coordinates
```

Forward transform supports rendering. Reverse transform supports dragging and future placement tools.

## Layout Representation

M17 should add a pure-Java `DiagramLayoutEngine` that produces a `LaidOutDiagram` consumed by document layout and Minecraft rendering.

The laid-out representation should contain enough positioned geometry for the renderer and hit tester to avoid recomputing semantics, including conceptually:

- outer diagram/canvas bounds;
- title bounds if present;
- laid-out elements and their labels;
- laid-out port centers/hit bounds;
- routed connection paths;
- connection-label bounds;
- logical/document coordinate transform information or equivalent mapping helpers;
- source block index / semantic source references needed for hit testing.

As with plots, the Minecraft renderer draws positioned geometry. It must not own routing, coordinate transforms, semantic endpoint resolution, or hit-test policy.

## Connection Routing

Connections store only semantic endpoints. Their visible path is derived during layout.

The first routing strategy is deterministic orthogonal routing with short port exit segments and a small number of horizontal/vertical bends. Port side supplies the preferred outward direction.

M17 does **not** require:

- obstacle avoidance;
- path finding;
- author-authored waypoints;
- automatic crossing bridges;
- wire bundling;
- electrical-net merging;
- mechanical constraints.

Crossing another element or connection is allowed in the first slice. The important architectural boundary is that routing remains derived and replaceable later without rewriting the AST.

## Selection And Hit Testing

Outside its embedded editor, a `DiagramBlock` behaves like the other non-text scientific blocks: it is an atomic `BlockSelection` stop during document navigation.

Internal editing uses a dedicated `DiagramEditingSelection` rather than overloading text selection. Initial semantic targets should be typed, conceptually covering:

- diagram/title;
- element;
- port;
- connection.

Hit testing is pure Java over `LaidOutDiagram`. When hit regions overlap, the initial priority is:

```text
port > element > connection > canvas
```

This keeps small connection points usable while preventing lines behind nodes from stealing ordinary node clicks.

Hit testing uses a bounded connection-distance tolerance rather than requiring a one-pixel-perfect click on a routed segment.

## Dragging And History

M17 element dragging must preserve Scholar's existing global `EditorHistory` model.

A mouse drag is one logical edit, not hundreds of undo entries. The interaction therefore has three conceptual phases:

```text
begin drag -> transient preview -> commit on release
```

The transient pointer/drag state is editor interaction state outside the semantic AST. On mouse release, the final clamped logical position is committed as one immutable document replacement and one global history transaction.

Canceling a drag restores the pre-drag semantic document without a history entry.

The first drag slice moves elements only. Resize handles, multi-element dragging, alignment guides, snapping, and marquee selection are deferred.

## Structural Editing Direction

After dragging is stable, M17E may expose explicit commands for:

- add generic node;
- delete selected node;
- edit node/port/connection labels;
- create a connection by explicitly choosing a source port then target port;
- delete selected connection.

Connection creation should use ephemeral editor interaction state until both endpoints are known. Incomplete connections do not belong in `DiagramDefinition`.

The first structural editor does not infer connections from proximity and does not silently merge ports or nets.

## Clipboard And Interchange

M17F follows the proven Table/Plot clipboard pattern:

- selected whole `DiagramBlock` -> native process-local `DiagramClipboardPayload` for lossless Scholar-to-Scholar transfer;
- OS clipboard -> deterministic readable plain-text summary;
- native paste may structurally insert/replace a selected DiagramBlock;
- external diagram-like text is not silently inferred back into a diagram.

The first M17 slice has no Markdown diagram syntax, SVG import/export, image import, Mermaid/Graphviz parsing, JSON interchange contract, or partial element clipboard.

## Domain Boundary: M18 And M19

M17 owns reusable diagram mechanics:

```text
canvas
geometry
ports
connectivity
routing
layout
hit testing
selection
dragging
history integration
whole-block clipboard
```

M18 Electrical Diagrams owns electrical meaning and presentation, such as resistors, capacitors, sources, LEDs, grounds, electrical pin semantics, and later any domain-specific validation.

M19 Mechanical Diagrams owns its own mechanical vocabulary and domain rules. M17 must not anticipate those by adding generic fields that only make sense for electrical or mechanical systems.

This boundary intentionally favors adding domain-specific element families later over creating a universal diagram meta-model now.

## M17 Implementation Slices

### M17A - Architecture & Data Model Design

Status: accepted by this specification and ADRs. Documentation only; no production diagram code.

### M17B - Static DiagramBlock + Layout/Render + Atomic Selection

Status: complete and manually validated in Minecraft.

The first semantic model is production code: immutable `DiagramBlock(DiagramDefinition)`, generic rectangular nodes, perimeter ports, validated semantic endpoints, responsive pure-Java canvas layout, reversible coordinate transform, positioned `LaidOutDiagram` geometry, Minecraft rendering, Insert -> Diagram, and atomic selection/navigation/deletion/undo behavior.

### M17C - Ports, Connections, Routing & Hit Testing

Status: complete and manually validated in Minecraft.

M17C hardens diagram connectivity geometry without changing the semantic AST:

- `DiagramConnectionRouter` derives deterministic orthogonal routes in pure Java.
- Each route starts from the exact laid-out perimeter port, takes a short outward exit according to `LEFT`/`RIGHT`/`TOP`/`BOTTOM`, and approaches the target through its corresponding outside exit.
- Horizontal/horizontal and vertical/vertical port pairs use a deterministic midpoint trunk; mixed orientations use a deterministic single elbow between exits.
- Exit points and all derived route vertices are constrained to the laid-out canvas. Obstacle avoidance remains deliberately deferred.
- Perimeter ports are mapped directly from authored logical bounds + normalized port offset before integer rounding, avoiding accumulated node-size rounding drift.
- `LaidOutDiagramPort` exposes a bounded hit region, while `LaidOutDiagramConnection` exposes derived path bounds.
- Connection labels use the most useful routed segment and are kept inside the canvas when their measured size permits.
- `DiagramHitTester` operates only on `LaidOutDiagram` geometry and returns typed semantic targets for title/canvas, elements, ports, or connections.
- Overlap priority is `port > element > connection > canvas`; connections use a bounded line-distance tolerance and their labels hit the owning connection.

M17C does not enter diagram editing mode or mutate the document. `DiagramEditingSelection`, second-click entry, keyboard target traversal, and dragging remain M17D.

### M17D - Diagram Editing Foundation

Status: complete and manually validated in Minecraft.

M17D adds a dedicated `DiagramEditingSelection` with typed targets and keeps ordinary document navigation atomic. Enter from a selected `DiagramBlock` enters internal editing on the first element when present; Escape returns to whole-block `BlockSelection`. Tab/Shift+Tab traverse title, elements, ports, connections, and canvas deterministically.

Mouse hit testing from M17C now drives internal selection. A second click on an already-selected diagram enters editing at the clicked semantic target. Generic nodes can be dragged directly. Pointer positions are reverse-mapped through the current `DiagramCoordinateTransform`, preserving authored logical coordinates across GUI scale and responsive document width.

Drag interaction remains transient outside the semantic document/history during pointer movement. Preview documents are laid out only for presentation. Mouse release commits the final clamped logical node position as one immutable global `EditorHistory` transaction; Escape cancels an active drag without a history entry. Node size, label, IDs, ports, and connection endpoints are preserved, while connection routes are naturally re-derived by layout.

M17D still does not add structural creation/deletion, label editing, connection authoring, resizing, snapping, multi-selection, or partial clipboard. Those remain later slices.

### M17E - Structural Editing

Status: implemented; pending full Gradle + manual Minecraft validation.

M17E adds explicit generic node/connection operations and plain-string label editing while preserving endpoint validity and global history behavior:

- `DiagramEditor` can edit the diagram title plus node, port, and connection labels by immutable replacement.
- `Diagram -> Add Node` creates a generic rectangular node with four midpoint perimeter ports and a diagram-local stable ID.
- deleting a selected node removes all incident connections in the same immutable edit, so a committed `DiagramDefinition` never contains dangling endpoints;
- connection authoring is two-stage and explicit: arm a selected source port, select a distinct target port, then finish; the pending source exists only in `EditorSession` interaction state and never enters the AST/history;
- a pending connection can be canceled with Escape or `Diagram -> Cancel Connection`, and is cleared by drag start, leaving diagram editing, Undo/Redo, or structural replacement;
- `Diagram -> Delete Connection` removes the selected semantic connection;
- node/connection structural edits and label edits use the existing global `EditorHistory`, with deterministic valid selection repair after deletion;
- port-label geometry participates in hit testing so authored port labels can be selected directly.

M17E still does not add node resizing, port creation/deletion/repositioning, snapping, multi-selection, obstacle avoidance, arrows/directionality, or domain-specific symbols.

### M17F - Clipboard / Interchange

Implemented whole-diagram clipboard follows the proven Scholar sidecar pattern:

- `DiagramClipboardPayload(DiagramBlock)` preserves the exact immutable diagram AST, including logical canvas, diagram-scoped element/port IDs, node bounds, port placement, connections, labels, and authored ordering;
- the operating-system clipboard receives a deterministic readable structural summary containing title/canvas, nodes/bounds, ports, and endpoint-based connections;
- native paste at an editable document position uses existing structural block insertion/replacement behavior and selects the inserted DiagramBlock;
- native paste over a selected DiagramBlock replaces it in place as one global undoable history transaction;
- native whole-diagram copy/cut/paste remains disabled while inside `DiagramEditingSelection`; M17F does not introduce partial node/port/connection clipboard;
- the plain-text summary is not a parser contract. External diagram-looking text remains ordinary text, and a changed OS clipboard invalidates the native sidecar through exact-text matching.

M17F still adds no Markdown diagram syntax, JSON interchange contract, SVG/image transfer, Mermaid/Graphviz parser, or partial element clipboard.

### M17G - Hardening

Implemented final foundation hardening focuses on failure-resistant derived geometry rather than new authoring features:

- very narrow responsive layouts clamp derived node rectangles to the laid-out canvas while preserving a minimum visible extent; authored logical bounds are never rewritten by reflow;
- node and port labels are kept inside the diagram canvas when their measured text can fit there, preventing edge nodes from pushing ordinary labels outside the document surface;
- distinct semantic connection endpoints that collapse to the exact same laid-out coordinate now produce a deterministic zero-length segment instead of violating the laid-out connection path contract;
- empty diagrams remain layoutable down to the minimum positive document width;
- targeted regressions cover degenerate connections, narrow geometry, edge labels, empty diagrams, and semantic stability across responsive reflow.

M17G intentionally does not add obstacle avoidance, node resizing, snapping, multi-select, domain symbols, partial clipboard, or a new interchange format. Full Gradle and manual GUI-scale/resize regression remain the acceptance gate before M17 is declared closed.

## Explicitly Deferred

The following are outside M17 unless a later implementation slice proves one is strictly required:

- electrical symbols or circuit simulation;
- mechanical components or physics constraints;
- arbitrary interior/free-form ports;
- curved/spline connections;
- obstacle-avoiding autorouting;
- user-authored waypoints;
- connection arrows/directionality;
- multi-select and marquee selection;
- grouping/nesting;
- layers/z-order authoring;
- resize handles;
- snap-to-grid/alignment guides;
- zoom/pan inside a DiagramBlock;
- rich-text/math labels;
- SVG/image/Mermaid/Graphviz import/export;
- partial element clipboard;
- Markdown syntax;
- networking/collaboration;
- world/server queries during layout;
- a public extension/plugin API.


## Mechanical constraints (M19C)

Mechanical constraints are semantic `DiagramElement` instances and are not encoded as decorative
pixel geometry.

Supported M19C relationships:

- Horizontal
- Vertical
- Coincident
- Parallel
- Perpendicular
- Concentric

Directional mechanical primitives carry a minimal semantic quarter-turn orientation (`DEG_0` or
`DEG_90`). Horizontal/vertical and parallel/perpendicular relationships reconcile that orientation.
Coincident is center-coincident in M19C v1 because primitive endpoint handles are not yet authored
objects. Concentric is restricted to radial primitives.

Binary constraints reference source/peer primitive IDs. Their visible markers are derived at layout
time, and pending two-step authoring state is transient editor state only.

M19C deliberately does not introduce a general-purpose CAD solver, dimensional driving constraints,
tolerances, GD&T, or physical mechanics/simulation.

## Mechanical labels and annotations (M19E)

M19E adds semantic technical annotations to mechanical diagrams. `MechanicalAnnotation` stores an annotation kind and editable text; its visible box/leader geometry is derived at layout/render time.

Supported v1 kinds are `PART_LABEL`, `NOTE`, and `LEADER`. Annotation text uses the existing diagram text-edit popup, participates in selection/hit testing, movement, delete, undo/redo, native clipboard, and plain-text fallback. M19E remains technical drawing authoring and does not introduce CAD manufacturing semantics such as GD&T or tolerances.
