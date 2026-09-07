# M18C — Orientation + Terminal/Connection Integration + Hit Testing

## Status

Implemented on top of the user-validated M18B visual-polish baseline. M18C is **not manually closed yet**; the user's full Gradle run and in-game QA remain authoritative.

Starting validated baseline: **835 tests / 0 failures**.

This slice adds **14 ordinary JUnit `@Test` methods**. The repository now contains 847 ordinary `@Test` methods plus the existing two-invocation parameterized Markdown test, so the expected Gradle execution count is approximately **849 tests**.

## Scope Delivered

M18C completes the approved basic electrical-symbol/read-selection infrastructure without moving into M18D authoring actions.

Implemented:

- Completed the approved first electrical vocabulary:
  - `RESISTOR`
  - `CAPACITOR`
  - `DC_VOLTAGE_SOURCE`
  - `GROUND`
  - `DIODE`
  - `LED`
  - `SWITCH_SPST`
- Added stable diode/LED terminal semantics:
  - `anode`
  - `cathode`
- Added SPST stable terminals:
  - `a`
  - `b`
- Added descriptive `ANODE` / `CATHODE` terminal roles; no simulator semantics were introduced.
- Added pure-Java normalized symbols for diode, LED, and open SPST switch using the existing line/polyline vocabulary.
- Existing quarter-turn geometry now covers all seven approved basic component kinds.
- Rotated electrical terminals continue to preserve semantic IDs while moving to the derived perimeter side.
- Existing M17 `DiagramConnection` routing is regression-tested against rotated electrical terminals.
- `DiagramHitTester` now handles electrical components and terminals in pure Java.
- Electrical terminals map to the existing generic `DiagramPortTarget`.
- Electrical symbol body, reference designator, and value label map to the existing generic `DiagramElementTarget`.
- Port priority remains above owning-element priority, preserving the wiring interaction model.
- Sparse symbols receive a forgiving authored-bounds fallback so selecting an open switch does not require a pixel-perfect click.
- Diagram editing target outlines in `ScholarEditorScreen` now render for electrical components and electrical terminals.
- Armed connection-source highlighting now also works for electrical terminals.
- Added an `Electrical Symbols` development fixture containing:
  - horizontal diode;
  - quarter-turned LED;
  - 180-degree SPST switch.
- Existing `Basic DC Circuit`, generic system diagram, plot, and table remain in the editable development document for regression/manual QA.

## Deliberately Deferred To M18D+

M18C does **not** add:

- insert-by-electrical-kind menus;
- reference designator editing UI;
- value editing UI;
- rotate clockwise/counter-clockwise commands;
- electrical component dragging;
- electrical component deletion commands;
- automatic designator generation;
- resize handles;
- junction/net semantics;
- electrical-rule checking;
- simulation/SPICE behavior;
- physical breadboard/perfboard representations.

The existing generic M17 connection-draft machinery can now address electrical terminals because they participate in the same target contract, but M18D remains the dedicated electrical-authoring UX slice.

## Main Production Files Changed

### Electrical semantic vocabulary

- `src/main/java/dev/rgcb/scholar/electrical/ElectricalComponentKind.java`
- `src/main/java/dev/rgcb/scholar/electrical/ElectricalTerminalRole.java`
- `src/main/java/dev/rgcb/scholar/electrical/ElectricalComponentCatalog.java`

### Derived symbol geometry

- `src/main/java/dev/rgcb/scholar/electrical/symbol/ElectricalSymbolLibrary.java`

### Pure-Java hit testing

- `src/main/java/dev/rgcb/scholar/editor/DiagramHitTester.java`

### Client interaction overlays / development fixture

- `src/main/java/dev/rgcb/scholar/client/screen/ScholarEditorScreen.java`
- `src/main/java/dev/rgcb/scholar/client/DevelopmentDocument.java`

## Tests Added / Expanded

14 new ordinary JUnit tests across:

- `ElectricalComponentTest`
- `ElectricalSymbolLibraryTest`
- `ElectricalSymbolLayoutEngineTest`
- new `ElectricalConnectionIntegrationTest`
- new `ElectricalDiagramHitTesterTest`

Coverage includes:

- diode/LED stable terminal IDs and roles;
- SPST stable terminal schema and prefix;
- quarter-turn diode terminal identity preservation;
- deterministic diode/LED/SPST symbol geometry;
- rotated LED terminal-lead geometry;
- deterministic 180-degree switch geometry;
- routed connection endpoint resolution into a quarter-turned LED;
- all seven approved basic kinds sharing one mixed generic/electrical diagram;
- electrical terminal hit priority;
- symbol-body hit targeting;
- reference/value annotation hit targeting;
- stable semantic terminal IDs under rotated hit testing.

## Local Validation Performed

The Gradle wrapper cannot download Gradle 9.2.1 in this environment because `services.gradle.org` is not resolvable. `./gradlew test --offline` still attempts the missing wrapper distribution and fails before project compilation.

Validated instead with Java 21:

- Pure-Java production compilation across Scholar non-client code: **PASS**.
- Updated `DevelopmentDocument` compilation against compiled core: **PASS**.
- Electrical + M17 diagram/editor regression harness: **145 passed / 0 failed** across 18 relevant test classes.
- Focused electrical/layout/hit-testing harness: **47 passed / 0 failed**.
- Development-document construction smoke test: **PASS**.
  - `Basic DC Circuit`: 4 electrical components / 4 connections.
  - `Electrical Symbols`: 3 electrical components / 0 connections.
  - `System Diagram`: 2 generic nodes / 1 connection.
- Responsive layout smoke at 320 px for all three diagram fixtures: **PASS**.
- Pure-Java boundary scan over electrical/diagram/editor packages: **PASS**, no Minecraft/NeoForge/Mojang imports.

The client screen changes are intentionally small presentation-overlay extensions; the user's NeoForge Gradle build remains the authoritative client compile.

## Architecture Record

Added:

- ADR 0205 — Electrical Hit Testing Reuses Generic Diagram Targets.

Updated:

- `docs/ELECTRICAL_DIAGRAM_SPEC.md`
- `docs/PROJECT_SPEC.md`
- `docs/ROADMAP.md`
- `docs/adr/README.md`

## Manual QA Target

On the user's machine:

```powershell
.\gradlew.bat test
.\gradlew.bat build
.\gradlew.bat runClient
```

Expected test execution count: approximately **849 tests / 0 failures**.

Open:

```text
/scholar_dev_editor
```

Validate:

1. `Basic DC Circuit` still renders exactly as the accepted M18B polished result.
2. The new `Electrical Symbols` sheet visibly shows a diode, LED, and open SPST switch.
3. Enter diagram-editing mode on an electrical diagram and click a component body/reference/value: the component receives the blue element target outline.
4. Click an electrical terminal: the terminal receives the port target outline instead of the whole component.
5. Tab/Shift+Tab can traverse electrical elements/terminals because they use the existing M17 target list.
6. Start a generic diagram connection from an electrical terminal: the armed source outline appears on that terminal; cancel if desired.
7. Resize the window / change GUI scale and verify all quarter-turn symbols and terminals remain aligned.
8. Regress the generic System Diagram, Plot, Table, and document navigation.

If Gradle and manual QA pass, M18C can be closed and M18D — Electrical Authoring can begin.
