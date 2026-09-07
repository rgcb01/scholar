# M19A Implementation Report — Mechanical Primitives

M19A introduces the first mechanical-domain vocabulary above the completed M17 diagram foundation.

## Implemented

- New semantic `MechanicalPrimitive` element family implementing the open `DiagramElement` contract.
- Primitive kinds: `LINE`, `CENTERLINE`, `RECTANGLE`, `CIRCLE`, `ARC`, `ARROW`, and `REFERENCE_POINT`.
- Pure authored bounds; no M19B dimensions, M19C constraints, simulation state, forces, materials, or mechanical connectivity are introduced.
- Mechanical primitives participate in generic immutable dragging through `DiagramElement.withBounds`.
- Dedicated `MechanicalDiagramEditor` supports add/delete as one history transaction through `EditorSession`.
- Diagram menu commands expose all seven primitive kinds plus explicit mechanical deletion.
- Responsive layout publishes mechanical primitives as their own derived family in `LaidOutDiagram`.
- Minecraft renderer draws all seven primitive kinds inside the existing clipped zoom/pan workspace.
- Hit testing and selection outlines support mechanical primitives.
- Native whole-diagram clipboard remains lossless automatically; plain-text fallback now identifies mechanical primitive kind and bounds.
- Development document includes a `Mechanical Primitives` fixture for manual QA.

## Boundary

M19A is intentionally geometry-only. Dimensions/callouts belong to M19B and geometric relationships/constraints belong to M19C. Mechanical symbols such as gears, bearings, springs, shafts, pistons, and fasteners remain deferred to M19D.

## Validation

The complete Minecraft-independent Java source tree compiles successfully with Java 21 after the M19A changes. Six focused JUnit tests were added for semantic creation, canvas validity, generic movement, deletion, layout publication, and plain-text fallback. The authoritative Gradle suite cannot be executed in this sandbox because the Gradle 9.2.1 wrapper distribution cannot be downloaded from `services.gradle.org`.
