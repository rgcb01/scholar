# M17B Implementation Report — Static DiagramBlock + Layout/Render + Atomic Selection

## Base

Implemented directly on the user-provided `MinecraftSTEMDev_M17A(1).zip`, whose diagram architecture is defined by `docs/DIAGRAM_SPEC.md` and ADRs 0178–0185.

M17A itself was documentation-only. M17B is the first production code for Scholar scientific diagrams.

## Scope implemented

### Semantic diagram model

Added pure-Java immutable model types under `dev.rgcb.scholar.diagram`:

- `DiagramCanvas`
- `DiagramBounds`
- `DiagramElementId`
- `DiagramPortId`
- `DiagramPortSide`
- `DiagramPortPlacement`
- `DiagramPort`
- `DiagramElement`
- `DiagramNode`
- `DiagramEndpoint`
- `DiagramConnection`
- `DiagramDefinition`
- document-level `DiagramBlock`

The model enforces the M17A structural invariants:

- finite positive canvas dimensions;
- non-blank diagram-local element/port IDs;
- unique element IDs in one diagram;
- unique port IDs inside one element;
- finite positive node bounds inside the authored canvas;
- normalized finite perimeter-port offsets in `[0,1]`;
- connection endpoints must resolve to existing element ports;
- an endpoint cannot connect to itself;
- immutable defensive-copy lists.

Node overlap, crossing connections, duplicate parallel connections, and same-element connections between different ports remain valid semantic states as specified by M17A.

### Pure-Java diagram layout

Added `dev.rgcb.scholar.diagram.layout`:

- `DiagramLayoutEngine`
- `DiagramCoordinateTransform`
- `LaidOutDiagram`
- `LaidOutDiagramNode`
- `LaidOutDiagramPort`
- `LaidOutDiagramConnection`
- `LaidOutDiagramPoint`
- `LaidOutDiagramLabel`

The logical diagram canvas is mapped uniformly into the Scholar document width while preserving aspect ratio. Semantic coordinates remain unchanged across reflow.

`DiagramCoordinateTransform` supports both forward and reverse mapping so M17D dragging can reuse the same coordinate boundary rather than introducing a second transform later.

M17B derives a deliberately simple deterministic orthogonal path between resolved ports. This is enough to render a real semantic connection, but M17C still owns routing hardening, port exit segments, connection bounds/clipping, and diagram-specific hit testing.

### Document layout integration

Added `LaidOutBlockKind.DIAGRAM` and an optional `LaidOutDiagram` payload to `LaidOutBlock` while retaining compatibility constructors used by existing code/tests.

`DocumentLayoutEngine` now lays out `DiagramBlock` as a first-class non-text scientific block with normal Scholar block spacing.

### Minecraft rendering

`MinecraftDocumentRenderer` now consumes positioned `LaidOutDiagram` geometry and renders:

- canvas boundary;
- connection paths behind nodes;
- generic rectangular node surfaces/borders;
- port markers;
- diagram title;
- node labels;
- optional port labels;
- optional connection labels.

The renderer does not resolve semantic endpoints, calculate diagram scale, or route connections.

### Atomic document interaction

M17B deliberately does not add internal diagram editing yet. A `DiagramBlock` participates in the existing document interaction model as an atomic `BlockSelection`:

- mouse hit selects the block through generic non-text `DocumentHitTester` behavior;
- Left/Right and visual navigation cross it structurally;
- Delete removes the complete block;
- Undo/Redo restore/remove it through global `EditorHistory`;
- text formatting/block styles remain unavailable on a selected diagram.

### Insert action and development fixture

Added `EditorActionId.INSERT_DIAGRAM` and `BuiltInEditorActions.insertDiagram()`.

`Insert -> Diagram` creates a valid two-node generic diagram with one semantic port-to-port connection and selects the inserted block.

The editable development document now also contains a static M17B visual fixture:

- title: `System Diagram`
- node: `Sensor`
- node: `Processor`
- one derived connection labeled `signal`

### Markdown boundary

`MarkdownSerializer` now explicitly rejects `DiagramBlock` with a diagram-specific unsupported message. M17B does not invent Markdown diagram syntax or infer diagrams from external text.

## Tests added/updated

Added 27 normal JUnit tests:

- `DiagramModelTest`: 11
- `DiagramLayoutEngineTest`: 7
- `DiagramBlockIntegrationTest`: 9

Updated `EditorActionTest` for the new Insert -> Diagram action/order.

Given the user-validated M16G baseline of **704 tests**, the expected Gradle total is approximately:

**731 tests**

## Validation performed in this environment

Full Gradle execution could not start because the sandbox cannot resolve/download `https://services.gradle.org/distributions/gradle-9.2.1-bin.zip`.

Instead, validation performed with Java 21 included:

- all non-client production/core sources compiled successfully;
- `DevelopmentDocument` compiled successfully against the new core;
- all new M17B tests compiled and ran with a local JUnit-compatible runner: **27 / 27 passed**;
- all compiled non-client/core test executions ran through the same runner: **718 passed, 0 failed**;
- modified `MinecraftDocumentRenderer` type-checked against minimal Minecraft/client API stubs;
- diagram core boundary scan found no `net.minecraft`, `net.neoforged`, or `com.mojang` imports under the new diagram core;
- `EditorActionTest` also ran locally with **47 / 47 passed** after the Insert menu update.

The authoritative validation remains the real Gradle suite on the user's machine.

## Manual QA expected

Run:

```powershell
.\gradlew.bat test
.\gradlew.bat build
.\gradlew.bat runClient
```

Then open:

```text
/scholar_dev_editor
```

Expected M17B visual result:

```text
              System Diagram

       ●                         ●
   ┌──────────┐             ┌───────────┐
   │  Sensor  │──── signal ─│ Processor │
   └──────────┘             └───────────┘
```

Exact pixel placement depends on GUI scale/document width, but the logical relationship must remain stable.

Manual checks:

- diagram appears between the surrounding paragraphs/table without overlap;
- title and both node labels are visible;
- two port markers and one connection render;
- connection is behind node surfaces;
- resizing/reflow scales the entire logical canvas uniformly;
- one click selects the whole DiagramBlock with the normal blue object selection;
- Left/Right/Up/Down can navigate across it as one structural object;
- Delete removes it; Undo restores it; Redo removes it again;
- Insert -> Diagram creates another complete diagram;
- existing Plot/Table behavior remains unchanged.

## Explicitly not implemented in M17B

Still deferred to later M17 slices:

- diagram-specific hit testing of nodes/ports/connections;
- hardened port-exit-aware routing and connection bounds/clipping;
- `DiagramEditingSelection`;
- mouse dragging;
- adding/deleting nodes or connections;
- label editing UI;
- native diagram clipboard;
- electrical/mechanical symbols;
- obstacle avoidance, waypoints, arrows, multi-select, resize handles, snap/grid, zoom/pan;
- Markdown/SVG/Mermaid/Graphviz interchange.

## Next milestone

**M17C — Ports, Connections, Routing & Hit Testing**

The semantic and layout boundaries needed for M17C are now present without introducing electrical or mechanical domain assumptions.
