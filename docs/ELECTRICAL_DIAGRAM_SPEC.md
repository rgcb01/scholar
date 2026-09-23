# Electrical Diagram Specification

This document captures the approved M18A architecture for Scholar's electrical-diagram domain. M18 builds on the completed M17 Scientific Diagram Foundation; it does not replace that foundation with a second editor or a second document block.

## Implementation Status

M18A-M18H.1 are complete and manually accepted. The established electrical stack includes the bounded symbol vocabulary, quarter-turn terminal placement, authoring, explicit semantic junctions, derived connectivity/nets, named-net annotations, independent workspace scaling with transient zoom/pan, and lossless whole-diagram clipboard with a readable one-way plain-text fallback.

M18H presentation polish made electrical terminal exits compact and adaptive so routed wires do not create visible out-and-back spikes beyond terminal connection points. Logical anchors, stable terminal IDs, hit targets, snapping, connection endpoints, and derived net semantics remain unchanged.

## Scope

M18 introduces semantic electrical components and standard schematic-symbol presentation inside the existing `DiagramBlock(DiagramDefinition)` model.

The first electrical slice is deliberately about **authoring and communicating circuit diagrams**, not circuit solving or physical electronics simulation.

M18 will provide:

- domain-specific electrical component elements inside `DiagramDefinition`;
- stable semantic terminals compatible with M17 port-to-port connections;
- standard schematic symbols derived in pure Java;
- quarter-turn component orientation;
- reference designators and human-readable value annotations;
- insertion, selection, dragging, rotation, connection, deletion, undo/redo, and whole-diagram clipboard through the existing M17 editor/history architecture;
- a small explicit first component vocabulary.

M18 does **not** make Scholar a SPICE engine, PCB editor, breadboard/perfboard simulator, or electronics rule checker.

## Architectural Boundary

The document-level container remains:

```text
Document
└─ DiagramBlock
   └─ DiagramDefinition
      ├─ DiagramNode
      ├─ ElectricalComponent
      └─ DiagramConnection
```

M18 does not introduce `ElectricalDiagramBlock`. A scientific diagram may contain generic M17 nodes and electrical components in the same logical canvas if an educational document needs both.

The concrete M18 element is conceptually:

```text
ElectricalComponent
├─ DiagramElementId id
├─ DiagramBounds bounds
├─ ElectricalComponentKind kind
├─ ElectricalOrientation orientation
├─ String referenceDesignator
└─ String valueLabel
```

`ElectricalComponent` implements `DiagramElement` and therefore participates in the same layout, selection, drag, connection, clipboard, and history infrastructure as M17 elements.

## Component Kinds

The first M18 vocabulary is intentionally bounded. The planned vocabulary is:

- `RESISTOR`
- `CAPACITOR`
- `DC_VOLTAGE_SOURCE`
- `GROUND`
- `DIODE`
- `LED`
- `SWITCH_SPST`

M18B proved the architecture with resistor, capacitor, DC voltage source, and ground. M18C completes the approved basic vocabulary with diode, LED, and SPST switch while preserving the same semantic component/terminal/symbol pipeline.

Inductors, transistors, MOSFETs, op-amps, transformers, relays, IC packages, sensors, motors, and arbitrary user-defined symbols are deferred. The terminal model must not assume exactly two terminals, so these can be added later without replacing the component abstraction.

## Stable Terminal Schema

Electrical connectivity continues to use M17 `DiagramEndpoint(elementId, portId)` references.

Electrical components do not author an arbitrary list of perimeter ports. Instead, each `ElectricalComponentKind` owns a stable terminal schema in domain code. Example conceptual terminal IDs:

```text
RESISTOR          a, b
CAPACITOR         a, b
DC_VOLTAGE_SOURCE positive, negative
GROUND            ground
DIODE             anode, cathode
LED               anode, cathode
SWITCH_SPST       a, b
```

Terminal IDs are part of the persistent semantic contract for that component kind. Visual placement may evolve, but an authored connection to `resistor-1/a` must continue to mean the same terminal.

`ElectricalComponent.ports()` is derived from the component kind, orientation, and bounds. This preserves the existing M17 connection-validation contract without duplicating rotatable terminal placement data in each component instance.

A component catalog may expose domain metadata such as:

```text
ElectricalComponentDefinition
├─ kind
├─ defaultReferencePrefix
└─ terminalDefinitions
   ├─ stable port id
   ├─ semantic terminal role
   └─ base normalized perimeter placement
```

Terminal roles are descriptive domain semantics (for example positive/negative or anode/cathode), not simulator pin models.

## Orientation

Electrical symbols initially support only deterministic quarter-turn orientation:

```text
DEG_0
DEG_90
DEG_180
DEG_270
```

`DEG_0` is the component kind's canonical authored orientation. Rotation transforms both symbol geometry and derived terminal placement around the component bounds.

Connections reference stable terminal IDs, not screen locations, so rotating a component reroutes existing wires without rewriting endpoints.

Arbitrary-angle rotation and mirroring are deferred.

Reference designators and value labels remain upright relative to the document for readability; they do not rotate with the symbol strokes.

## Component Bounds

Electrical components use `DiagramBounds` so M17 drag, canvas validation, coordinate transform, and responsive reflow remain reusable.

The first editor creates canonical logical sizes per component kind. No resize handles are exposed in M18. Symbol geometry is normalized into the authored bounds so future document versions can preserve non-default sizes without storing pixels.

For quarter-turn rotation, the editor may swap the canonical width/height when required while keeping the component fully inside the logical canvas.

The authored `DiagramBounds` are also the forgiving interaction/terminal footprint; they are intentionally larger than the densest visible part of many schematic symbols. Derived symbol geometry may use a compact central body with longer terminal leads so the diagram reads like a conventional schematic without making ports or drag targets difficult to hit. This distinction is presentation-only and is not stored as extra AST state.

## Symbol Model

The AST stores **electrical meaning**, not drawing commands.

A resistor is stored as:

```text
kind = RESISTOR
orientation = DEG_0
referenceDesignator = "R1"
valueLabel = "10 kΩ"
```

It is not stored as a zig-zag list of pixels or Minecraft line calls.

A pure-Java `ElectricalSymbolLibrary`/layout layer maps the semantic kind into normalized schematic geometry. The initial presentation vocabulary should stay intentionally small and may use only the primitives actually needed by the approved symbols, conceptually:

- line segment;
- polyline;
- circle;
- polygon/filled marker where needed.

This is an internal derived presentation model, not a generic SVG/vector-graphics language and not part of the document AST.

The symbol pipeline is:

```text
ElectricalComponent
        ↓
component definition + orientation
        ↓
normalized symbol geometry
        ↓
DiagramCoordinateTransform
        ↓
LaidOutElectricalComponent
        ↓
Minecraft renderer
```

Minecraft rendering consumes already-positioned geometry. It does not decide terminal identity, component orientation, or electrical semantics.

## Labels And Values

M18 distinguishes two common schematic annotations:

- `referenceDesignator`: `R1`, `C2`, `V1`, `D3`, etc.
- `valueLabel`: `10 kΩ`, `100 nF`, `5 V`, etc.

Both are authored non-null strings and may be empty.

Scholar does not initially parse these labels into executable quantities, normalize SI prefixes, validate Ohm's law, or reject duplicate/mismatched designators. Scholar represents authored scientific knowledge; simulation and electrical correctness checking belong to later systems.

When the editor inserts a component it may generate a convenient deterministic next designator (`R1`, `R2`, ...), but designator uniqueness is not an AST invariant.

M18B visual QA established a deterministic annotation policy in core layout: horizontal two-terminal symbols prefer reference-above/value-below, vertical symbols prefer reference-left/value-right, and ground prefers a side annotation so its top conductor remains clear. M18C visual polish anchors those annotations to a compact centered presentation footprint (currently 62% of the larger interaction bounds) so designators and values stay visually attached to the symbol body instead of the hit box. These are presentation preferences with bounded fallbacks, not authored AST state. Circular source bodies preserve a circular aspect ratio even when their logical bounds are rectangular; terminal leads are derived to meet the circle exactly, and polarity marks remain document-upright under quarter-turn rotation.

## Wiring And Connectivity

The first M18 wiring model deliberately reuses M17 `DiagramConnection`:

```text
Electrical terminal ── DiagramConnection ── Electrical terminal
```

The same side-aware derived orthogonal router remains responsible for visual wire paths. Electrical components expose their terminals through the generic `DiagramElement.ports()` contract, so M17 endpoint validation remains applicable. M18 reuses the generic router but applies a derived electrical endpoint-exit policy. M18C originally reserved a longer fixed straight exit before an elbow. Final M18H polish caps the electrical exit at the generic 8 px maximum and clamps it by the counterpart position: if a terminal faces away from the other endpoint, the artificial exit collapses instead of drawing an out-and-back spike. Electrical/junction routes also discard redundant collinear vertices after routing. These changes are presentation-only; the first and last path points remain the exact laid-out semantic terminal anchors.

M18 does **not** infer electrical nets from geometry. Crossing wires are not automatically connected. Two lines touching visually are not automatically a junction. Connection semantics come from explicit endpoint references.

M18E adds `ElectricalJunction` as an explicit semantic `DiagramElement`. It exposes four directional authored ports for routing, while `ElectricalNetResolver` treats all four as one electrical node. This supports terminal → junction, junction → junction, and branched terminal connectivity without introducing geometry-based inference.

Electrical nets are derived snapshots rather than stored routed objects. A net is the connected component of semantic endpoints joined by `DiagramConnection` edges plus junction-internal equivalence. Optional junction net labels name an already-connected net; equal labels on disconnected nets do not silently merge topology. Conflicting labels inside one connected net are rejected.

Buses, automatic global-net merging by label, electrical-rule checking, and simulation state remain deferred.

Multiple explicit `DiagramConnection` objects may share the same component terminal if the user authors them; M18 does not initially perform electrical-rule checking on fan-out.

## Selection, Hit Testing, And Editing

M18 reuses the M17 editing architecture rather than creating a parallel electrical editor.

Outside embedded editing, the whole `DiagramBlock` remains an atomic document `BlockSelection`.

Inside `DiagramEditingSelection`, electrical components become typed element targets. M18 should extend the existing element editing path so it no longer assumes every element is `DiagramNode`.

Expected component operations after the static symbol slice is proven:

- insert component by explicit kind;
- select by symbol/bounds/terminal;
- drag using M17 logical coordinates;
- rotate clockwise/counter-clockwise by 90 degrees;
- edit reference designator;
- edit value label;
- start/finish a connection using existing terminal targets;
- delete component and incident connections in one immutable transaction;
- undo/redo through the existing global `EditorHistory`.

A completed drag or rotation is one logical history transaction. No electrical-specific history stack is introduced.

## Hit Testing

Symbol hit testing remains pure Java.

Initial selection may use a combination of:

- terminal hit bounds from the existing port infrastructure;
- component bounds as a robust coarse target;
- derived symbol-stroke distance where needed for sparse symbols such as ground or a voltage source.

Terminal priority remains higher than the enclosing component so wiring stays practical.

No pixel-perfect Minecraft-only hit testing should become the source of truth.

## Clipboard And Interchange

The existing native `DiagramClipboardPayload(DiagramBlock)` is already the correct whole-block lossless boundary. When `ElectricalComponent` becomes a valid `DiagramElement`, native Scholar-to-Scholar diagram clipboard should preserve it automatically as part of the immutable block object.

M18 must update the deterministic plain-text diagram summary so electrical components remain intelligible outside Scholar, for example:

```text
Component: RESISTOR R1 [r1]
Bounds: [10.0, 8.0, 24.0, 10.0]
Orientation: DEG_0
Value: 10 kΩ
Terminal: a [PASSIVE_A, LEFT @ 0.5]
Terminal: b [PASSIVE_B, RIGHT @ 0.5]
```

Electrical terminal lines are descriptive and include the stable terminal id, semantic role, and current oriented perimeter placement. Junctions emit their four explicit ports plus an optional `Net Label:` line. If an authored M18E.5 workspace height differs from the logical-canvas default, the fallback also emits `Workspace Aspect Ratio:` so the external summary does not silently hide that authored presentation choice.

External text is never inferred back into electrical components. Markdown remains unsupported for `DiagramBlock`/electrical diagrams in M18.

Partial component clipboard is deferred unless a later authoring slice proves a concrete need.

## Schematic Versus Physical Electronics

M18 models **schematic diagrams inside Scholar documents**.

It does not model the physical placement of a resistor, LED, IC, or jumper on a breadboard/perforated board. Those are different representations with different facts:

```text
Schematic component
├─ electrical kind
├─ terminals
├─ symbol orientation
└─ diagram position

Physical/perfboard component
├─ package/footprint
├─ physical pin spacing
├─ board holes
├─ 3D/pixel model
└─ world placement
```

The small Minecraft components envisioned for the future STEM Lab (for example a resistor occupying only a few pixels on a perforated board) must therefore not be encoded as M18 schematic symbol geometry.

A later integration may map a semantic component kind between a Scholar schematic and a STEM Lab physical component, but neither representation is the other's AST.

## Simulation Boundary

M18 is not an electrical solver.

Deferred beyond M18:

- voltage/current calculation;
- Ohm/Kirchhoff solving;
- SPICE-style netlists;
- transient/AC analysis;
- component tolerances;
- executable resistance/capacitance values;
- power ratings;
- electrical-rule checking;
- world-state data binding;
- STEM Lab simulation coupling.

If executable electrical quantities are introduced later, they should use a dedicated typed quantity/unit model. Presentation strings such as `"10 kΩ"` must not silently become executable simulator input.

## M18 Slice Plan

### M18A — Architecture & Symbol Model Design

This document and ADRs. No production Java changes.

### M18B — Electrical Component Core + First Static Symbols

Implement `ElectricalComponent`, kind/orientation/catalog contracts, derived terminal ports, pure-Java symbol layout, and an initial static fixture using resistor, capacitor, DC voltage source, and ground. Extend `DiagramLayoutEngine`/`LaidOutDiagram` without duplicating the diagram engine.

No internal electrical editing in the first slice beyond existing whole-diagram behavior.

### M18C — Orientation + Terminal/Connection Integration + Hit Testing

Implemented pending manual closeout: complete quarter-turn derived symbol/terminal placement, wire routing into rotated electrical terminals, mixed generic/electrical diagram layout regression coverage, generic diagram target integration for electrical terminals/components, and pure-Java electrical hit testing. The approved basic vocabulary is now complete with diode, LED, and SPST switch.

Electrical terminal hits map to the existing `DiagramPortTarget`; electrical component body/reference/value hits map to `DiagramElementTarget`. Component hit testing uses derived symbol/annotation geometry plus a forgiving authored-bounds fallback, while ports keep priority over owning elements. No electrical-specific editor selection type is introduced.

### M18D — Electrical Authoring

Implemented pending manual QA. Electrical authoring stays inside the existing `DiagramEditingSelection` and global `EditorHistory` model:

- `Diagram -> Add Resistor/Capacitor/DC Voltage Source/Ground/Diode/LED/SPST Switch` inserts an explicit semantic kind with deterministic local ID, convenience reference designator, `DEG_0` orientation, and an empty authored value.
- Inserted component placement is deterministic and bounded by the logical canvas. Canonical authoring bounds are kind-specific; M18D intentionally adds no resize handles.
- Generic diagram dragging now works for any `DiagramElement` through immutable `withBounds(...)`, so electrical components reuse M17 preview/commit/cancel semantics and still commit a completed drag as one global undo step.
- Quarter-turn clockwise/counterclockwise rotation preserves component identity, semantic terminal IDs, annotations, and existing `DiagramConnection` endpoints. Width/height swap when required, center is preserved when possible, and the result is clamped inside the canvas.
- Enter on a selected electrical component opens a two-field editor for reference designator and value. Applying both fields is one immutable/history transaction.
- Delete Component removes the selected component together with all incident wires in one valid/history-friendly edit.
- Electrical terminals continue to use the existing M17 connection-draft workflow; no electrical-specific wire state is introduced.

M18D does not add mirroring, arbitrary-angle rotation, resizing, automatic component-to-component wiring, net inference, junction semantics, or simulation quantities.

### M18E — Electrical Connectivity Hardening

Harden multi-terminal/fan-out behavior and decide, based on actual authoring evidence, whether M18 needs an explicit free-space junction/net abstraction. Do not infer nets from line crossings.

### M18E.5 — Diagram Workspace Scaling

M18E.5 separates the authored logical diagram canvas from the embedded document workspace and adds scalable navigation/authoring controls:

- `DiagramBlock.workspaceAspectRatio` controls the visible document-workspace height independently from logical canvas dimensions. Legacy blocks default to the logical canvas aspect and therefore retain the previous Fit appearance.
- `DiagramViewport` is transient editor/view state. Fit is zoom 1 centered on the logical canvas; zoom and pan never enter the AST, clipboard payload, net model, or global undo history.
- The pure-Java layout engine maps the logical canvas through the viewport into a stable workspace. Zoomed/panned geometry may extend outside that workspace, but renderer and hit tester clip interaction to the workspace.
- Ctrl+wheel or Ctrl+plus/minus zooms; middle-mouse drag pans; Ctrl+0 restores Fit. Logical pointer conversion still uses the inverse `DiagramCoordinateTransform`, so element dragging remains semantic under every viewport transform.
- Selecting the empty canvas and pressing Enter opens authored logical width/height resize. Existing element sizes remain unchanged and are clamped inward; dimensions too small to contain an existing element are rejected. Canvas resize is one global undoable edit.
- Diagram menu commands make the embedded workspace shorter/taller or reset it to logical-canvas aspect, also through global undo/redo.
- Diagram menu commands scale all electrical component bounds up/down around their centers while preserving IDs, kinds, orientation, annotations, terminal schemas, wires, junctions, and net semantics.

M18E.5 deliberately does not make zoom/pan persistent document state and does not perform the final terminal/pin lead-length polish; that visual closeout remains the last M18 item.

### M18F — Clipboard / Interchange Regression

Complete/manual QA accepted. The existing whole-`DiagramBlock` native clipboard remains the only lossless diagram transfer boundary and now has explicit regression coverage across mixed generic/electrical elements, authored workspace height, explicit junctions, named nets, rotated component terminal schemas, connection endpoints, cut/paste history, and mixed Scholar documents. Local element/terminal ids are intentionally preserved because their identity scope is the copied `DiagramBlock`; separate diagram blocks do not share an element-id namespace.

The deterministic external fallback describes electrical components with kind, reference designator, bounds, orientation, optional value, stable terminal ids, terminal semantic roles, and oriented terminal placement. Junction bounds/ports/net labels and connection endpoints remain readable. A non-default authored `workspaceAspectRatio` is included descriptively. Default workspace ratio is omitted to avoid noise in legacy summaries.

The plain-text representation remains deliberately one-way and lossy: matching-looking external text never creates a `DiagramBlock`, `ElectricalComponent`, `ElectricalJunction`, connection, or net. Scholar only restores native diagram structure when the process-local `DiagramClipboardPayload` still matches the current system clipboard text; a stale sidecar is invalidated and the current clipboard is pasted as ordinary text. No external-text inference or Markdown diagram import is introduced.

### M18G — Final Hardening

Complete/manual QA accepted. M18G deliberately adds no new schematic vocabulary or simulation semantics. It hardens the combined M17/M18 editor boundary with:

- exhaustive electrical-kind/quarter-turn layout checks at canvas edges across tiny through wide responsive document widths;
- minimum/maximum viewport zoom and off-workspace hit-test clipping regressions;
- deterministic valid routing for degenerate/coincident electrical wire geometry;
- edge rotation, extreme global symbol scaling, and logical-canvas shrink checks that preserve explicit connection endpoints and derived connectivity;
- empty and mixed generic/electrical diagrams under narrow reflow without authored-AST mutation;
- chained rotate/scale/canvas-resize/annotation edits with exact global undo/redo restoration;
- a transient `DiagramViewportStore` that rebases per-diagram zoom/pan when immutable document edits shift snapshot-local block indices, preventing a deleted/moved diagram camera from leaking onto a neighboring diagram;
- deterministic insertion-order exposure for derived electrical-net endpoints and endpoint-to-net maps, without making net order authored state.

Viewport reconciliation remains transient presentation state and is intentionally skipped for drag-preview documents. Whole-diagram native clipboard semantics remain those accepted in M18F.

### M18H — Final Electrical Visual Polish

Implemented pending authoritative Gradle/manual QA as the final M18 closeout pass. Manual M18E QA exposed small wire/terminal protrusions caused by the former fixed 14 px electrical exit reservation: when a port faced away from the actual route, the router could travel outward and immediately back through the same line before continuing.

M18H fixes the derived presentation without rewriting any authored component geometry:

- electrical endpoint exit reservation is capped at 8 px instead of 14 px;
- the effective exit length is clamped to the available distance in the port's outward half-plane and becomes zero when the counterpart lies behind the port;
- routed electrical/junction paths remove redundant collinear vertices, collapsing out-and-back endpoint spikes while preserving orthogonal geometry;
- exact first/last route points remain the existing laid-out terminal/junction anchors;
- coincident semantic endpoints retain the two-point degenerate-route contract.

No terminal ids, `DiagramPortPlacement`, component bounds, snapping, port hit bounds, connection endpoint references, clipboard payloads, history state, or `ElectricalNetResolver` semantics change.

## Explicit Non-Goals For M18A

- No production Java changes.
- No electrical simulation.
- No PCB, breadboard, or perfboard physical editor.
- No Minecraft 3D/pixel component models.
- No netlist import/export.
- No arbitrary-angle rotation or mirroring.
- No user-defined symbol language.
- No universal electronics taxonomy.
- No transistor/op-amp/IC modeling in the first vertical slice.
- No automatic wiring from proximity or line crossing.
- No public plugin API yet.
