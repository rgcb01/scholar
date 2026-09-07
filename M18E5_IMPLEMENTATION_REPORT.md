# M18E.5 Implementation Report — Diagram Workspace Scaling

## Scope

Implemented the M18E.5 workspace-scaling slice on top of the repaired/manual-validated M18E project. The slice keeps authored diagram coordinates stable while allowing a larger logical canvas, an independently sized embedded workspace, transient zoom/pan/Fit, and bulk electrical symbol scaling.

## Implemented

### Transient viewport
- Added pure-Java `DiagramViewport(zoom, centerX, centerY)` with bounded zoom.
- `DocumentLayoutEngine` accepts a per-diagram viewport provider while preserving the existing overload as deterministic Fit.
- `DiagramLayoutEngine` now distinguishes the visible workspace rectangle from the transformed logical canvas rectangle.
- Ctrl+wheel and Ctrl+plus/minus zoom the diagram viewport.
- Middle-mouse drag pans.
- Ctrl+0 restores Fit.
- Zoom/pan are not stored in `DiagramBlock`, do not dirty the document, and do not create undo entries.

### Workspace clipping and interaction
- Zoomed/panned schematic geometry is clipped to the internal diagram workspace in the Minecraft renderer.
- Diagram hit testing rejects off-workspace derived geometry.
- Selection overlays use the workspace for the Canvas target and clip non-title targets to the workspace.
- Pointer-to-logical drag conversion continues through the inverse layout transform.

### Logical canvas authoring
- Selecting the empty diagram canvas and pressing Enter opens a Width/Height popup.
- Canvas resize is immutable and globally undoable.
- Existing element sizes are preserved; elements are translated inward if necessary.
- A canvas smaller than any existing element is rejected.

### Variable DiagramBlock height
- `DiagramBlock` now carries authored `workspaceAspectRatio` separately from `DiagramDefinition.canvas`.
- One-argument `DiagramBlock` derives the legacy ratio from the canvas, preserving previous layout by default.
- Diagram menu exposes Workspace Shorter, Workspace Taller, and Reset Workspace Height.

### Global electrical symbol scale
- Diagram menu exposes Scale Symbols Down / Up.
- Scaling rewrites only `ElectricalComponent.bounds()` about each component center.
- Orientation, IDs, reference/value annotations, stable terminal IDs, connections, explicit junctions, and semantic nets are preserved.
- Electrical edits were hardened to preserve the new `DiagramBlock.workspaceAspectRatio` rather than accidentally resetting it.

### M18E integration cleanup
- The Minecraft Diagram menu/action registry now includes the M18E Add Junction and Delete Junction actions that already existed in core actions.

## Architecture records

- ADR 0214 — Diagram Zoom And Pan Are Transient Viewport State
- ADR 0215 — Diagram Workspace Height And Logical Canvas Size Are Authored Separately
- ADR 0216 — Global Electrical Symbol Scale Rewrites Component Bounds

## Tests added/updated

Added pure-Java regression coverage for:
- legacy Fit compatibility;
- variable workspace height;
- zoom and pan transforms;
- off-workspace hit-test clipping;
- logical canvas resize/clamping/rejection;
- workspace height reset;
- bulk electrical symbol scaling and topology preservation.

Added session-level regression coverage confirming that logical-canvas resize, workspace-height edits, and global electrical-symbol scaling are single undoable semantic edits.

Updated the two exact Diagram menu expectation tests for the five new authored scaling commands.

## Validation in this environment

The non-Minecraft production core compiles successfully with Java 21. The Minecraft client/editor sources affected by this slice also compile in a lightweight API-stub compile check, which catches Java syntax/type integration mistakes without claiming a real NeoForge runtime build. A direct M18E.5 smoke program validates zoomed layout, logical canvas resize, and electrical bulk symbol scaling.

A dependency-free local test harness compiled the current test sources and executed all 897 current test invocations successfully (the repaired 885-test baseline plus 12 M18E.5 tests). This is supplementary validation rather than a replacement for Gradle/JUnit.

The real Gradle suite cannot be executed in this sandbox because the Gradle wrapper requires Gradle 9.2.1 from `services.gradle.org` and outbound DNS/network access is unavailable. Run `./gradlew test` on the normal development machine before manual Minecraft QA.

## Manual QA requested

1. Open `Junction + Net`.
2. Ctrl+wheel zoom in/out over the diagram; verify the mouse point stays visually anchored and nothing draws outside the workspace.
3. Middle-drag to pan; verify wires/components/junctions stay selectable only while visible.
4. Ctrl+0 restores Fit.
5. Click empty canvas, press Enter, enlarge Width/Height, Apply; verify authored coordinates remain coherent and undo/redo works.
6. Try a canvas smaller than an existing symbol; verify it is rejected.
7. Use Diagram → Workspace Taller/Shorter/Reset Height and verify document reflow/undo.
8. Use Diagram → Scale Symbols Up/Down and verify wires, junctions, rotation, annotations, and net labels remain connected.
9. Re-test component drag while zoomed and panned.

## Deferred final M18 polish

The user-observed terminal/pin lead-length issue is intentionally not changed here. It is recorded as M18H, the final M18 visual-polish point, and must shorten only rendered terminal lead geometry without changing logical anchors, snapping, hit testing, or net semantics.
