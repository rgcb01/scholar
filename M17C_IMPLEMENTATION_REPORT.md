# M17C Implementation Report

## Milestone

**M17C — Ports, Connections, Routing & Hit Testing**

Base: user-validated `MinecraftSTEMDev_M17B_FIXED` (M17B complete).

M17C hardens the reusable diagram geometry and reverse-mapping layer. It does not add diagram editing or structural mutation; those remain M17D/M17E.

## Implemented

### 1. Side-aware deterministic orthogonal routing

Added `DiagramConnectionRouter` in pure Java.

Routes now:

- begin at the exact laid-out source port center;
- take a short outward exit according to `LEFT`, `RIGHT`, `TOP`, or `BOTTOM`;
- connect exit points using deterministic orthogonal trunks/elbows;
- approach the target through its own outward-side exit;
- remain constrained to the laid-out diagram canvas;
- preserve derived presentation geometry outside the semantic AST.

The first router deliberately does **not** perform obstacle avoidance, path finding, authored waypoints, crossing bridges, or domain-specific net semantics.

### 2. Hardened perimeter port geometry

Port centers are now mapped from the exact authored logical perimeter coordinate (`DiagramBounds + normalized DiagramPortPlacement`) through `DiagramCoordinateTransform` before integer rounding.

This avoids accumulating rounding from already-rounded node pixel dimensions.

`LaidOutDiagramPort` now also exposes an explicit document-space `hitBounds` rectangle.

### 3. Connection bounds and canvas containment

Added `LaidOutDiagramRect` for small diagram-local laid-out rectangles.

`LaidOutDiagramConnection` now includes derived route bounds. All router vertices are kept within the current laid-out canvas. These bounds are used as a cheap prefilter before segment-distance hit testing.

Connection labels still use the midpoint of the most useful routed segment (longest segment, horizontal wins deterministic ties) and are clamped into the canvas when their measured size permits.

### 4. Pure-Java typed diagram hit testing

Added:

- `DiagramHitTester`
- `DiagramEditTarget`
- `DiagramPropertyTarget` / `DiagramProperty`
- `DiagramElementTarget`
- `DiagramPortTarget`
- `DiagramConnectionTarget`

Hit priority follows the accepted M17 architecture:

```text
port > element > connection > canvas
```

Connection labels map to their owning connection. Routed segments use a bounded 4 px distance tolerance rather than pixel-perfect equality. Clicks outside the title/canvas return no target.

Hit targets carry snapshot-local indices and semantic element/port IDs where relevant, preparing M17D without introducing document-global IDs.

### 5. Development fixture

The `/scholar_dev_editor` System Diagram fixture now offsets `Sensor` and `Processor` vertically so M17C's orthogonal routing is visible during manual QA instead of looking like the old single straight segment.

The diagram is **still atomic at document level**. Internal hit targets are implemented/tested but intentionally not wired into an editing mode until M17D.

## Tests

Added 18 JUnit tests:

- 7 `DiagramConnectionRouterTest`
- 8 `DiagramHitTesterTest`
- 3 additional `DiagramLayoutEngineTest`

Repository count is now:

- 748 ordinary `@Test` methods
- 1 parameterized test with 2 invocations
- **Expected full Gradle executions: 750**

### Validation performed in this environment

- Java 21 compilation of all Minecraft-independent production sources: **PASS**
- Java 21 compilation of updated `DevelopmentDocument`: **PASS**
- Java 21 compilation of all new/changed diagram tests: **PASS**
- Executed 26 routing/layout/hit-testing tests with a local lightweight JUnit-compatible runner: **26 passed / 0 failed**
- Direct smoke test of route bounds, canvas containment, port exit orientation, port hit priority, and connection-label hit mapping: **PASS**
- Core boundary scan for Minecraft/NeoForge/Mojang imports in diagram + new diagram editor geometry: **PASS**

Full Gradle could not run in the sandbox because the wrapper attempts to download Gradle 9.2.1 from `services.gradle.org`, and DNS/network access is unavailable (`UnknownHostException`).

## Files changed / added

Production:

- `src/main/java/dev/rgcb/scholar/diagram/layout/DiagramConnectionRouter.java` (new)
- `src/main/java/dev/rgcb/scholar/diagram/layout/LaidOutDiagramRect.java` (new)
- `src/main/java/dev/rgcb/scholar/diagram/layout/DiagramLayoutEngine.java`
- `src/main/java/dev/rgcb/scholar/diagram/layout/LaidOutDiagramConnection.java`
- `src/main/java/dev/rgcb/scholar/diagram/layout/LaidOutDiagramPort.java`
- `src/main/java/dev/rgcb/scholar/editor/DiagramHitTester.java` (new)
- `src/main/java/dev/rgcb/scholar/editor/DiagramEditTarget.java` (new)
- `src/main/java/dev/rgcb/scholar/editor/DiagramProperty.java` (new)
- `src/main/java/dev/rgcb/scholar/editor/DiagramPropertyTarget.java` (new)
- `src/main/java/dev/rgcb/scholar/editor/DiagramElementTarget.java` (new)
- `src/main/java/dev/rgcb/scholar/editor/DiagramPortTarget.java` (new)
- `src/main/java/dev/rgcb/scholar/editor/DiagramConnectionTarget.java` (new)
- `src/main/java/dev/rgcb/scholar/client/DevelopmentDocument.java`
- `src/main/java/dev/rgcb/scholar/client/render/MinecraftDocumentRenderer.java` (comment/documentation alignment only)

Tests:

- `src/test/java/dev/rgcb/scholar/diagram/layout/DiagramConnectionRouterTest.java` (new)
- `src/test/java/dev/rgcb/scholar/editor/DiagramHitTesterTest.java` (new)
- `src/test/java/dev/rgcb/scholar/diagram/layout/DiagramLayoutEngineTest.java`

Docs:

- `docs/DIAGRAM_SPEC.md`
- `docs/PROJECT_SPEC.md`
- `docs/ROADMAP.md`

## Manual QA requested

Run:

```powershell
.\gradlew.bat test
.\gradlew.bat build
.\gradlew.bat runClient
```

Expected tests: **750, 0 failures**.

Open `/scholar_dev_editor` and inspect the System Diagram. `Sensor` and `Processor` are intentionally vertically offset. Verify that:

1. the route exits the right side of Sensor horizontally;
2. bends orthogonally rather than diagonally;
3. approaches Processor from its left side;
4. `signal` remains fully readable and associated with the route;
5. ports stay exactly on node borders;
6. resizing / GUI scale preserves alignment and orthogonality;
7. the diagram still behaves as one atomic document block for click/delete/undo/redo.

Do **not** expect clicks on nodes/ports/connections to enter internal editing yet. M17C provides the pure-Java hit mapping; M17D will expose it through `DiagramEditingSelection` and dragging.

## Next

After full Gradle and manual visual QA pass:

**M17D — Diagram Editing Foundation**

- `DiagramEditingSelection`
- Enter/Escape mode transition
- mouse target selection using `DiagramHitTester`
- transient drag preview
- reverse logical coordinate mapping
- one completed element drag = one global undo transaction
