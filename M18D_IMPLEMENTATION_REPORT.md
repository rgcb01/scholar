# M18D Implementation Report — Electrical Authoring

## Status

Implemented. Full Gradle and manual in-game QA are still required before M18D is closed.

## Scope Delivered

M18D turns the M18C schematic vocabulary into an editable in-document electrical authoring workflow while preserving the M17 Diagram foundation as the single selection, drag, connection, and history model.

### Explicit component insertion

`Diagram` now exposes explicit insertion commands for the complete approved first vocabulary:

- Resistor
- Capacitor
- DC voltage source
- Ground
- Diode
- LED
- SPST switch

Insertion is implemented by the new pure-Java `ElectricalDiagramEditor`. New components receive deterministic diagram-local element IDs, deterministic convenience reference designators, `DEG_0` orientation, an empty value annotation, kind-specific canonical logical bounds, and deterministic bounded placement. Insertion itself is one global `EditorHistory` transaction.

### Electrical annotation editing

Selecting an `ElectricalComponent` and pressing Enter opens a dedicated two-field popup with `Reference` and `Value`. Tab switches fields; Enter applies; Escape cancels. Applying replaces both authored strings together as one immutable history edit. Unchanged values are a no-op.

The annotations remain presentation strings only. M18D does not parse values such as `10 kΩ` or `5 V` into executable quantities.

### Generic drag reuse

`DiagramElement` now exposes immutable `withBounds(DiagramBounds)`. `DiagramEditor.moveElement(...)` therefore moves any diagram-domain element through the same M17 pure-Java logical movement path rather than special-casing `DiagramNode`.

`DiagramNode` and `ElectricalComponent` both implement this contract. Electrical components now inherit the existing M17 drag behavior:

- grab offset is preserved;
- drag preview does not mutate semantic history;
- cancel restores the original document without a history entry;
- commit is one Undo step;
- movement clamps the complete authored bounds inside the logical canvas;
- wires reroute from the derived terminal positions as the component moves.

### Quarter-turn rotation

Diagram actions now expose clockwise and counterclockwise rotation for a selected electrical component. Rotation:

- advances/rewinds `ElectricalOrientation` by 90 degrees;
- swaps authored width/height when orientation parity changes;
- preserves the old center whenever canvas bounds allow it;
- clamps the final component inside the canvas;
- preserves component ID, kind, reference designator, value, semantic terminal IDs, and all existing `DiagramConnection` endpoints.

The M18C symbol layout and M17 router derive the resulting visible symbol/terminal/wire geometry.

### Explicit wiring

No new electrical wire state was added. Electrical terminals remain `DiagramPortTarget` values and use the M17 connection-draft workflow unchanged:

1. select a terminal;
2. `Start Connection`;
3. select another terminal;
4. Enter or `Finish Connection`.

The resulting wire remains a semantic `DiagramConnection` between stable `DiagramEndpoint(elementId, portId)` references.

### Delete component

`Delete Component` and the Delete key remove a selected `ElectricalComponent` together with every incident connection in one valid immutable edit. One Undo restores the component and all removed wires.

## Main Production Changes

- `diagram/DiagramElement.java` — adds domain-neutral immutable `withBounds(...)` contract.
- `diagram/DiagramNode.java` — implements `withBounds(...)`.
- `electrical/ElectricalComponent.java` — implements covariant `withBounds(...)`.
- `editor/DiagramEditor.java` — generic element movement instead of `DiagramNode`-only movement.
- `electrical/editor/ElectricalComponentDraft.java` — two-field annotation draft.
- `electrical/editor/ElectricalDiagramEditor.java` — pure-Java insert/rotate/edit/delete operations.
- `editor/EditorSession.java` — electrical authoring operations integrated with global history and existing drag/connection state.
- `editor/EditorActionId.java` / `BuiltInEditorActions.java` — explicit insert/rotate/delete actions.
- `client/editor/ScholarEditorController.java` — electrical annotation bridge.
- `client/screen/ScholarEditorScreen.java` — Diagram menu entries, Delete dispatch, and two-field electrical component popup.

## Architecture Decisions

Added accepted ADRs:

- ADR 0206 — `DiagramElement` provides immutable bounds replacement for domain dragging.
- ADR 0207 — electrical insertion is explicit by kind with deterministic convenience identifiers.
- ADR 0208 — quarter-turn authoring preserves terminal references.
- ADR 0209 — electrical reference and value edit as one history transaction.

`ELECTRICAL_DIAGRAM_SPEC.md`, `PROJECT_SPEC.md`, `ROADMAP.md`, and the ADR index were updated to reflect M18D.

## Tests Added / Updated

M18D adds 21 ordinary JUnit tests across:

- `ElectricalDiagramAuthoringTest` — 14 tests for deterministic insertion, reference namespaces, bounded placement, CW/CCW rotation, edge clamping, stable terminal IDs/wire endpoints, annotation replacement, deletion, and generic electrical drag.
- `ElectricalAuthoringSessionTest` — 7 tests for menu exposure, global Undo/Redo integration, annotation transaction behavior, rotation with existing wires, deletion with incident wires, drag preview/commit, and reuse of the M17 connection draft.

Existing structural-action and electrical regression tests were updated where M18D intentionally changes the old M18C “electrical elements are not draggable” boundary.

Expected full Gradle execution count: **874 tests** (872 ordinary `@Test` methods plus one two-invocation parameterized test).

## Validation Performed In This Environment

The environment cannot resolve `services.gradle.org`, so `./gradlew test` cannot download Gradle 9.2.1. A real Gradle run was attempted and failed with `UnknownHostException: services.gradle.org`; no Gradle success is claimed here.

Local validation performed with Java 21:

- fresh pure-Java production compilation: passed;
- client sources affected by this slice compiled against API-shape Minecraft stubs: passed;
- all 70 test source classes compiled against a local JUnit-compatible API stub: passed;
- all **872 ordinary test methods** executed through a reflection harness: **872 passed / 0 failed**;
- the existing parameterized Markdown test executed for both declared values: **2 passed / 0 failed**;
- combined local execution equivalent count: **874 / 874 passed**;
- core boundary scan for `net.minecraft`, `net.neoforged`, and `com.mojang` imports in core diagram/electrical/editor/document/layout/math/plot/table/typography/clipboard/markdown packages: clean.

The local harness is strong regression evidence but is not a substitute for the repository's real Gradle/JUnit/NeoForge build on the user's machine.

## Manual QA Requested

Run the real Gradle tests/build first. In `/scholar_dev_editor`, enter a diagram and verify insertion of all seven electrical kinds, dragging, clockwise/counterclockwise rotation, Enter-based reference/value editing, terminal-to-terminal connection creation, Delete Component, and Undo/Redo across each operation. Confirm that rotating or dragging already-wired components keeps wires attached to the same semantic terminals and that components remain inside the logical canvas.

## Deferred Beyond M18D

- junction/net abstraction and fan-out hardening (M18E);
- electrical clipboard/plain-text regression (M18F);
- final responsive/edge-case hardening (M18G);
- mirroring, arbitrary-angle rotation, resize handles, snapping/multi-select;
- simulation, typed electrical quantities, SPICE/netlists, PCB/perfboard editing, or physical Minecraft component models.
