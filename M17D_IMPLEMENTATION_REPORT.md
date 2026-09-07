# M17D Implementation Report — Diagram Editing Foundation + Dragging

## Status

**Implemented. Awaiting full Gradle + manual Minecraft QA before milestone closure.**

Base: validated M17C project supplied by the user.
Expected full test baseline after this slice: **769 test invocations** (M17C 750 + 19 new M17D tests).

## Scope

M17D adds the first internal editing mode for `DiagramBlock` without introducing M17E structural authoring features. The goal is to make existing diagram elements selectable and draggable while preserving the architectural boundary established in M17A–M17C.

Implemented:

- dedicated `DiagramEditingSelection`;
- deterministic internal target traversal;
- click/hit-target entry into diagram editing;
- element dragging in logical diagram coordinates;
- live transient drag preview;
- deterministic rerouting during preview because routes remain derived from semantic geometry;
- whole-element canvas clamping;
- one completed drag = one global `EditorHistory` edit;
- cancelled drag = no semantic mutation and no history entry;
- Undo/Redo integration;
- visual focus/target outlines in the Minecraft screen;
- documentation/status updates.

Explicitly deferred:

- adding/removing nodes;
- creating/removing connections;
- editing labels;
- moving/resizing ports;
- node resizing;
- connection manipulation;
- Diagram clipboard/interchange;
- obstacle avoidance / advanced routing.

Those remain M17E/M17F/M17G work.

## New production types

### `DiagramEditingSelection`

Represents a diagram-internal editor selection while keeping the `DiagramBlock` atomic at document level.

```text
BlockSelection(DiagramBlock)
        ↓ Enter / second click
DiagramEditingSelection
        ↓
DiagramEditTarget
```

### `DiagramEditor`

Pure-Java immutable editing service responsible for:

- first target selection;
- target validation;
- next/previous target traversal;
- immutable logical movement of `DiagramNode`;
- clamping nodes wholly inside the authored canvas.

It does not know about Minecraft pixels, mouse coordinates, GUI scale, rendering, or history.

### `DiagramEditResult`

Carries the updated immutable `DiagramBlock`, resulting target, and changed flag.

## Target traversal

The deterministic traversal order is:

```text
TITLE
→ element 0
→ element 0 ports
→ element 1
→ element 1 ports
→ ...
→ connections
→ CANVAS
```

`Tab` advances and `Shift+Tab` moves backward. Reaching either boundary clamps rather than wraps.

Only an `ELEMENT` target is draggable in M17D. Ports, connections, title, and canvas can be selected/highlighted now so the selection vocabulary is already usable by later milestones.

## Drag architecture

The drag implementation follows ADR 0184: pointer movement must not flood `EditorHistory`.

### Begin

When the user begins dragging a selected node, `EditorSession` stores only an ephemeral `DiagramDragInteraction` containing:

- block index;
- stable element target;
- pointer-to-node grab offset in logical coordinates.

The grab offset prevents the node from snapping its upper-left corner to the cursor.

### Preview

Each mouse movement:

1. is reverse-mapped through `DiagramCoordinateTransform` from document geometry to logical canvas coordinates;
2. produces a temporary immutable `Document` containing the previewed node position;
3. relayouts only the screen's `LaidOutDocument`;
4. leaves `EditorSession.current().document()` and `EditorHistory` untouched.

Because connections contain semantic endpoint references rather than stored paths, M17C routing automatically derives a new route from the preview document.

### Commit

Mouse release computes the final logical node position and applies exactly one `history.applyEdit(...)` operation.

Therefore:

```text
mouse down
  preview
  preview
  preview
  preview
mouse up
      ↓
1 Undo step
```

### Cancel

`Escape` during an active drag drops the transient interaction and relayouts the committed semantic document. No history entry is created.

## Canvas clamping

`DiagramEditor.moveElement(...)` clamps the node's top-left coordinate to:

```text
X: 0 .. canvas.width  - node.width
Y: 0 .. canvas.height - node.height
```

The complete node therefore remains inside the logical canvas. Authored geometry stays independent of current viewport size or GUI scale.

## Minecraft interaction

`ScholarEditorScreen` now supports:

- first click from normal document editing: existing atomic `BlockSelection` behavior;
- `Enter` on a selected DiagramBlock: enter internal diagram mode with the first element selected;
- second click inside an already selected diagram: enter internal mode on the hit target;
- clicking an internal target: select/highlight it;
- dragging an element: live semantic preview and rerouting;
- `Tab` / `Shift+Tab`: target traversal;
- `Escape` during drag: cancel preview;
- `Escape` when not dragging: return to `BlockSelection`.

During an active drag, other keyboard commands are consumed until mouse release or cancel so commands cannot leave a stale transient preview visible.

## Files changed

Production:

- `src/main/java/dev/rgcb/scholar/editor/DiagramEditingSelection.java` (new)
- `src/main/java/dev/rgcb/scholar/editor/DiagramEditResult.java` (new)
- `src/main/java/dev/rgcb/scholar/editor/DiagramEditor.java` (new)
- `src/main/java/dev/rgcb/scholar/editor/EditorState.java`
- `src/main/java/dev/rgcb/scholar/editor/EditorSession.java`
- `src/main/java/dev/rgcb/scholar/client/editor/ScholarEditorController.java`
- `src/main/java/dev/rgcb/scholar/client/screen/ScholarEditorScreen.java`

Tests:

- `src/test/java/dev/rgcb/scholar/editor/DiagramEditorTest.java` (10 tests)
- `src/test/java/dev/rgcb/scholar/editor/DiagramEditingSessionTest.java` (9 tests)

Documentation:

- `docs/DIAGRAM_SPEC.md`
- `docs/PROJECT_SPEC.md`
- `docs/ROADMAP.md`

No new ADR was required: M17D implements the drag/history policy already approved in ADR 0184.

## Validation performed in this environment

### Java compilation

Passed:

- all non-client production/core Java sources with Java 21;
- `ScholarEditorController` against the compiled core;
- `ScholarEditorScreen` type/syntax compilation against lightweight API-shape stubs for unavailable Minecraft/NeoForge client classes.

### Architecture boundary

Passed: no `net.minecraft`, `net.neoforged`, or `com.mojang` imports were introduced under the diagram/editor core packages.

### Test execution

A lightweight local JUnit-compatible runner executed the complete repository test source set using Java 21 and API stubs only where client UI constants were required:

```text
769 passed
0 failed
```

This is useful source-level regression validation but is **not** a substitute for the real Gradle/NeoForge test task.

### Real Gradle limitation

`./gradlew test` cannot run in this environment because the wrapper attempts to download Gradle 9.2.1 from `services.gradle.org`, which is unreachable here (`UnknownHostException`).

The user's machine must therefore perform the authoritative Gradle run.

## Manual QA checklist

Run:

```powershell
.\gradlew.bat test
.\gradlew.bat build
.\gradlew.bat runClient
```

Expected test result:

```text
769 tests completed
0 failed
```

Then open `/scholar_dev_editor` and verify:

1. First click on the diagram selects the complete block as before.
2. Press `Enter`, or click a second time inside the selected diagram, to enter diagram editing.
3. A selected node has a blue target outline while the whole diagram retains a subtler focus outline.
4. `Tab` / `Shift+Tab` traverses nodes, ports, connection, title/canvas targets deterministically.
5. Drag `Sensor` or `Processor`; the node follows the mouse without snapping its corner to the cursor.
6. The connection reroutes live while the node moves.
7. Releasing the mouse commits the final position.
8. One `Ctrl+Z` restores the entire drag; one `Ctrl+Y` reapplies it.
9. Dragging beyond a canvas edge keeps the complete node inside the canvas.
10. Begin a drag and press `Escape`; the node returns to its committed position and no Undo entry is created.
11. Press `Escape` again after cancellation to return to atomic `BlockSelection`.
12. Resize the window / change GUI scale after moving a node; logical authored position remains stable.

## Closure criterion

Do **not** mark M17D complete until the real Gradle suite is green and the live Minecraft drag/Undo/cancel behavior is manually confirmed.

After M17D closes, proceed to **M17E — Structural Diagram Editing**.
