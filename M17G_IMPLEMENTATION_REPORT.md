# M17G Implementation Report — Diagram Hardening / Final Regression

## Scope

M17G is the final hardening slice for the generic Scientific Diagram Foundation. It adds no new domain vocabulary or major authoring features. The work focuses on responsive layout, degenerate connection geometry, regression coverage, and documentation closeout before M17 acceptance.

## Bugs / Edge Cases Hardened

### 1. Coincident endpoints could violate the routed-path contract

The semantic model deliberately permits overlapping nodes and distinct ports at the same authored position. If two connected endpoints both resolved to the same canvas-boundary coordinate, the router's duplicate-point removal could produce a path containing only one point. `LaidOutDiagramConnection` requires at least two points, so an otherwise valid semantic diagram could fail during layout.

The router now represents a completely collapsed connection as two equal points: a deterministic zero-length segment. No semantic displacement is invented and existing render/hit-test code keeps the normal path contract.

### 2. Narrow responsive layout could push rounded node geometry outside the canvas

At very small document widths, independent coordinate rounding can collapse node extents or place a minimum one-pixel rectangle beyond the laid-out canvas edge. `DiagramLayoutEngine` now clamps derived node rectangles to the canvas while preserving a minimum visible one-pixel extent.

Authored logical `DiagramBounds` are not changed by layout or reflow.

### 3. Edge labels could leave the diagram surface

Port labels are intentionally positioned outward from their perimeter port. Nodes placed on the canvas edge could therefore push a normal fitting label outside the diagram surface. Node labels could similarly spill beyond an edge when their node became narrow.

Node and port labels are now clamped to the laid-out canvas whenever their measured dimensions fit within that canvas. Oversized labels are not truncated or rewritten.

## Tests

Added `DiagramHardeningTest` with 8 ordinary tests covering:

- coincident boundary endpoints producing a stable zero-length routed segment;
- complete semantic layout of coincident distinct ports;
- node rectangles remaining inside an extremely narrow canvas;
- edge port-label clamping;
- edge node-label clamping;
- empty-diagram layout at minimum document width;
- semantic logical bounds remaining unchanged across responsive reflow;
- deterministic/orthogonal behavior for degenerate routes.

The accepted M17F baseline was 803 Gradle test executions, so the expected M17G full-suite count is **811 tests**.

## Validation Performed In This Environment

- Attempted `./gradlew test --no-daemon`; Gradle wrapper download is still blocked by `UnknownHostException: services.gradle.org`.
- Compiled all pure-Java Scholar main sources with Java 21 (`javac --release 21`), excluding Minecraft/NeoForge client entry points: success.
- Compiled and executed the 8 new hardening tests through a lightweight local JUnit-compatible harness: **8 passed / 0 failed**.
- Compiled and executed all diagram-model/layout/editor test classes available in the repository through the same harness: **107 passed / 0 failed across 11 test classes**.
- The changed production files remain in pure-Java diagram/layout packages and introduce no Minecraft, NeoForge, or Mojang imports.

Full Gradle on the user's machine remains authoritative.

## Documentation / ADRs

Updated:

- `docs/DIAGRAM_SPEC.md`
- `docs/PROJECT_SPEC.md`
- `docs/ROADMAP.md`

Added:

- ADR 0192 — responsive diagram geometry remains bounded under narrow layout;
- ADR 0193 — degenerate diagram connections remain valid zero-length routes.

## Explicit Non-Goals Preserved

M17G does not add node resizing, port creation/deletion/repositioning, snap-to-grid, multi-select, grouping, obstacle-avoiding routing, authored waypoints, electrical/mechanical symbols, partial-element clipboard, SVG/Mermaid/Graphviz interchange, simulation state, or public extension APIs.

## Final Manual QA Before Closing M17

1. Run the full Gradle test suite; expected result: **811 tests / 0 failures**.
2. Build and launch the client, then open `/scholar_dev_editor`.
3. Confirm the existing `Sensor -> Processor` diagram still renders identically at normal width.
4. Make the window narrow and change GUI scale; nodes must remain contained and labels should not disappear outside the diagram surface when they fit.
5. Drag nodes to all four canvas boundaries; connections must reroute and one Undo must restore the completed drag.
6. Create overlapping nodes/ports and connect distinct coincident ports if practical; the diagram must not crash.
7. Regress structural editing: add/delete node, create/delete connection, label editing, Undo/Redo.
8. Regress whole-diagram Copy/Cut/Paste and external plain-text fallback.
9. Regress Equation/Table/Plot rendering/editing around the same document.

M17 should be marked complete only after full Gradle and manual regression pass.
