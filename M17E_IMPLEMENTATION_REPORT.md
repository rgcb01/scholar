# M17E Implementation Report — Structural Diagram Editing

## Scope

M17E extends the manually validated M17D diagram editor with explicit semantic node/connection operations and label editing. The slice deliberately remains generic: no electrical/mechanical symbols, no port authoring UI, no resizing/snapping, and no partial clipboard.

## Implemented

### Label editing

`DiagramEditor` now supports immutable plain-string editing for:

- diagram title;
- node label;
- port label;
- connection label.

`ScholarEditorScreen` exposes the same compact modal editing pattern already proven by Plot editing. While inside `DiagramEditingSelection`, Enter opens the label popup for the current title/node/port/connection target. Canvas has no text field.

Port labels now participate in pure-Java `DiagramHitTester` geometry so clicking a visible port label selects its owning port.

### Generic node creation

`Diagram -> Add Node` creates a valid generic rectangular `DiagramNode` with:

- diagram-local stable ID `node-N`;
- default label `Node N`;
- deterministic in-canvas placement;
- four midpoint perimeter ports: LEFT / RIGHT / TOP / BOTTOM.

The new node becomes the active `DiagramElementTarget` and the operation is one global `EditorHistory` edit.

### Node deletion with endpoint cleanup

`Diagram -> Delete Node` and the Delete key remove the selected generic node. Every incident `DiagramConnection` is removed in the same immutable replacement before the new `DiagramDefinition` is constructed. A committed diagram therefore never contains dangling endpoints.

Undo restores the node and its incident connections together.

### Explicit two-stage connection authoring

Connection creation is intentionally explicit:

1. select a port;
2. `Diagram -> Start Connection`;
3. select a distinct target port;
4. press Enter or `Diagram -> Finish Connection`.

The pending source endpoint lives only in `EditorSession` interaction state. No incomplete `DiagramConnection` is added to the AST and no history entry exists until the second endpoint is known.

The armed source port receives a temporary amber outline. Escape or `Diagram -> Cancel Connection` cancels the draft. Pending connection state is also cleared by leaving diagram editing, beginning a node drag, Undo/Redo, or structural replacement.

New connections are semantic endpoint references only; M17C routing continues to derive their geometry.

### Connection deletion

`Diagram -> Delete Connection` and the Delete key remove the selected semantic connection in one global history transaction. Selection is repaired deterministically to another valid connection/element/title target.

### Actions and UI

A new contextual `Diagram` menu contains:

- Add Node
- Delete Node
- Start Connection
- Finish Connection
- Cancel Connection
- Delete Connection

Actions are enabled only for valid structural targets/states.

## Architectural decisions

Added:

- ADR 0186 — diagram structural edits preserve reference validity in one history transaction.
- ADR 0187 — incomplete diagram connections live only in editor interaction state.

Updated `DIAGRAM_SPEC.md`, `PROJECT_SPEC.md`, and `ROADMAP.md` to record M17D as manually validated and M17E as implemented pending full Gradle/manual validation.

## Files changed

Production:

- `src/main/java/dev/rgcb/scholar/editor/DiagramEditor.java`
- `src/main/java/dev/rgcb/scholar/editor/DiagramHitTester.java`
- `src/main/java/dev/rgcb/scholar/editor/EditorSession.java`
- `src/main/java/dev/rgcb/scholar/editor/EditorActionId.java`
- `src/main/java/dev/rgcb/scholar/editor/BuiltInEditorActions.java`
- `src/main/java/dev/rgcb/scholar/client/editor/ScholarEditorController.java`
- `src/main/java/dev/rgcb/scholar/client/screen/ScholarEditorScreen.java`

Tests:

- `src/test/java/dev/rgcb/scholar/editor/DiagramStructuralEditingTest.java`
- `src/test/java/dev/rgcb/scholar/editor/DiagramStructuralEditingSessionTest.java`
- `src/test/java/dev/rgcb/scholar/editor/DiagramHitTesterTest.java`

Docs:

- `docs/DIAGRAM_SPEC.md`
- `docs/PROJECT_SPEC.md`
- `docs/ROADMAP.md`
- `docs/adr/0186-diagram-structural-edits-preserve-reference-validity-in-one-history-transaction.md`
- `docs/adr/0187-incomplete-diagram-connections-live-only-in-editor-interaction-state.md`

## Test delta and expected Gradle baseline

M17D contained 767 ordinary `@Test` methods plus one parameterized test with two executions, matching the reported 769-test baseline.

M17E adds 19 ordinary `@Test` methods, so the expected Gradle execution count is:

**788 tests**

## Validation performed in this environment

Gradle wrapper execution is still blocked because `services.gradle.org` cannot be resolved from the sandbox.

Alternative validation completed successfully:

- all Minecraft-independent production sources compile with Java 21;
- `ScholarEditorController` compiles against the updated core;
- all production sources, including `ScholarEditorScreen`, type-check with Java 21 against shape-compatible Minecraft/NeoForge stubs;
- architecture boundary check passes: diagram/editor core contains no Minecraft/NeoForge/Mojang imports;
- all 84 diagram-related tests (existing M17 model/layout/routing/hit-testing/editing tests plus new M17E tests) executed through a JUnit-compatible local runner: **84 passed / 0 failed**;
- direct M17E smoke test passed for generic node creation, ephemeral connection draft, connection completion, label editing, Undo, and incident-connection cleanup.

## Manual QA requested

Run on the user's machine:

```powershell
.\gradlew.bat test
.\gradlew.bat build
.\gradlew.bat runClient
```

Expected test result: **788 tests / 0 failures**.

In `/scholar_dev_editor`:

1. enter the diagram editor;
2. select Sensor/Processor/title/connection and press Enter; confirm label popup edits and Undo/Redo;
3. select a port, press Enter, assign a visible port label, then confirm clicking that label reselects the port;
4. use `Diagram -> Add Node`; confirm a generic `Node N` appears with four ports and can still be dragged;
5. select the new node and Delete or `Diagram -> Delete Node`; Undo/Redo it;
6. select a source port and choose `Diagram -> Start Connection`; confirm the source gets the amber outline and the document does not otherwise change;
7. select a different target port and press Enter (or choose Finish Connection); confirm the new routed connection appears and one Undo removes it;
8. start another connection and press Escape; confirm only the draft is canceled and diagram editing remains active;
9. select a connection and Delete; confirm one Undo restores it;
10. delete a node that has one or more connections; confirm all incident connections disappear together and one Undo restores the entire structure.

## Deferred beyond M17E

- adding/deleting/repositioning ports;
- node resizing;
- snapping/alignment guides;
- multi-selection/marquee/grouping;
- obstacle-avoiding routing or authored waypoints;
- arrows/directionality;
- electrical/mechanical domain symbols;
- partial node/connection clipboard;
- whole-diagram clipboard (M17F).
