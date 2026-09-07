# Scholar Milestone History

This file is a compact checkpoint log for the work imported from `MinecraftSTEMDev_M19F_new.zip`.

Detailed architecture remains in the specs and ADRs. Detailed implementation notes remain in the milestone report files linked below.

## M16 - Scientific Plots

Status: complete through M16G.

M16 introduced native Scholar scientific plots:

- semantic `PlotBlock(PlotDefinition)` document blocks;
- static XY plot data using `PlotSeries` and `DataPoint`;
- finite `double` data values, explicit/automatic axis ranges, and linear axes;
- pure-Java plot auto-range, tick generation, number formatting, and coordinate transforms;
- responsive pure-Java plot layout with labels, legend data, line segments, scatter points, and deterministic series styling;
- Minecraft rendering from laid-out plot geometry rather than rendered image snapshots;
- atomic document selection first, then plot editing selection and explicit plot property/series/point editing;
- native whole-plot clipboard payload plus readable plain-text fallback;
- hardening for extreme finite ranges, responsive layout, clipping, hidden/out-of-range points, and tick label determinism.

Key references:

- `docs/ROADMAP.md`
- `docs/PROJECT_SPEC.md`
- `M16G_HOTFIX_NOTE.md`
- ADRs 0155-0177

## M17 - Scientific Diagram Foundation

Status: complete and manually accepted through M17G.

M17 introduced the reusable, domain-neutral diagram foundation:

- semantic `DiagramBlock(DiagramDefinition)` with logical canvas coordinates;
- diagram-scoped element IDs and element-scoped port IDs;
- generic rectangular `DiagramNode` elements with perimeter ports;
- semantic `DiagramConnection` values between ports, with derived orthogonal routes;
- pure-Java diagram layout, routing, hit testing, and viewport coordinate transforms;
- atomic document selection plus dedicated `DiagramEditingSelection`;
- diagram target traversal, node dragging, structural node/connection editing, and label editing;
- native whole-diagram clipboard payload plus readable structural fallback text;
- responsive and edge-case hardening without adding electrical or mechanical meaning.

Key reports:

- `M17A_ARCHITECTURE_REPORT.md`
- `M17B_IMPLEMENTATION_REPORT.md`
- `M17B_CONNECTION_LABEL_FIX.md`
- `M17C_IMPLEMENTATION_REPORT.md`
- `M17D_IMPLEMENTATION_REPORT.md`
- `M17E_IMPLEMENTATION_REPORT.md`
- `M17F_IMPLEMENTATION_REPORT.md`
- `M17G_IMPLEMENTATION_REPORT.md`

Key specs/ADRs:

- `docs/DIAGRAM_SPEC.md`
- ADRs 0178-0193

## M18 - Electrical Diagrams

Status: complete and manually accepted through M18H.1.

M18 built electrical schematic authoring on top of the M17 diagram foundation:

- `ElectricalComponent` domain elements inside existing `DiagramBlock` values;
- bounded first electrical vocabulary: resistor, capacitor, DC voltage source, ground, diode, LED, and SPST switch;
- stable component terminal schemas and `DiagramEndpoint` reuse for wires;
- quarter-turn orientation that preserves terminal identity and existing connections;
- pure-Java derived schematic symbol geometry separate from the AST;
- insert-by-kind actions, deterministic designators, value labels, rotation, dragging, connection creation, deletion, and undo/redo;
- explicit electrical junction elements and derived semantic nets;
- net labels that annotate explicit connected nets without merging by equal text;
- diagram workspace scaling with authored canvas/workspace size and transient zoom/pan viewport state;
- whole-diagram clipboard/interchange regression for mixed generic/electrical diagrams;
- responsive hardening and final terminal/wire visual polish.

Key reports:

- `M18A_ARCHITECTURE_REPORT.md`
- `M18B_IMPLEMENTATION_REPORT.md`
- `M18B_TEST_FIX_REPORT.md`
- `M18B_VISUAL_POLISH_REPORT.md`
- `M18C_IMPLEMENTATION_REPORT.md`
- `M18C_VISUAL_POLISH_REPORT.md`
- `M18C_LED_POLISH_REPORT.md`
- `M18D_IMPLEMENTATION_REPORT.md`
- `M18E_IMPLEMENTATION_REPORT.md`
- `M18E5_IMPLEMENTATION_REPORT.md`
- `M18F_IMPLEMENTATION_REPORT.md`
- `M18G_IMPLEMENTATION_REPORT.md`
- `M18H_IMPLEMENTATION_REPORT.md`
- `M18H1_IMPLEMENTATION_REPORT.md`

Key specs/ADRs:

- `docs/ELECTRICAL_DIAGRAM_SPEC.md`
- ADRs 0194-0219

## M19 - Mechanical Diagrams

Status: implemented through M19F, with M19F manual QA still pending according to the imported roadmap.

M19 adds mechanical technical-drawing features on top of the M17 diagram foundation:

- mechanical primitives: line, centerline, rectangle, circle, arc, arrow, and reference point;
- semantic dimensions and callouts;
- generic scrollable dropdown menus for long authoring menus;
- bounded geometric relationships/constraints: horizontal, vertical, coincident, parallel, perpendicular, and concentric;
- mechanical symbols: shaft, gear, bearing, spring, piston, and bolt;
- mechanical labels, notes, leader callouts, and annotation visual polish;
- assembly part references using semantic item balloons linked by stable element IDs;
- generated BOM tables derived from authored part references;
- runtime fix for M19F part-reference/BOM behavior.

Key reports:

- `docs/M19A_IMPLEMENTATION_REPORT.md`
- `M19B_IMPLEMENTATION_REPORT.md`
- `M19B1_IMPLEMENTATION_REPORT.md`
- `M19C_IMPLEMENTATION_REPORT.md`
- `M19D_IMPLEMENTATION_REPORT.md`
- `M19E_IMPLEMENTATION_REPORT.md`
- `M19E1_IMPLEMENTATION_REPORT.md`
- `M19F_IMPLEMENTATION_REPORT.md`
- `M19F_RUNTIME_FIX_REPORT.md`

Key ADRs:

- ADRs 0220-0224

## Current Imported Baseline

The imported tree represents the project after M19F plus a runtime fix. It includes Plot, Diagram, Electrical, and Mechanical source/test packages, updated project specs, roadmap updates, and milestone implementation reports.

Before treating M19 as complete, run the normal automated validation and complete the pending manual M19F QA noted in `docs/ROADMAP.md`.
