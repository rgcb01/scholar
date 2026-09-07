# M18B — Electrical Component Core + First Static Symbols

## Status

Implemented on top of the user-validated M18A/M17G codebase. M18B is **not considered manually closed yet**; full Gradle and in-game visual QA remain authoritative on the user's machine.

Starting validated baseline: **811 tests / 0 failures**.

The initial slice added 22 ordinary JUnit `@Test` methods. The visual-polish follow-up adds 2 regression tests, so the expected full Gradle execution count is **835 tests** if the baseline count remains unchanged.

## Scope Delivered

M18B proves the first production electrical vertical slice inside the existing `DiagramBlock(DiagramDefinition)` architecture.

Implemented:

- `ElectricalComponent implements DiagramElement`.
- First component kinds:
  - `RESISTOR`
  - `CAPACITOR`
  - `DC_VOLTAGE_SOURCE`
  - `GROUND`
- Stable semantic terminal catalog:
  - resistor: `a`, `b`
  - capacitor: `a`, `b`
  - DC source: `positive`, `negative`
  - ground: `ground`
- Descriptive terminal roles separate from simulation semantics.
- Quarter-turn orientation type and derived terminal placement while preserving terminal IDs.
- Authored upright reference designator/value strings.
- Pure-Java normalized symbol presentation model:
  - line
  - polyline
  - circle
- Pure-Java `ElectricalSymbolLibrary` for the first four kinds.
- Pure-Java `ElectricalSymbolLayoutEngine` mapping semantic symbols to positioned document geometry.
- `LaidOutElectricalComponent` and positioned primitive records.
- Existing `DiagramLayoutEngine` now supports mixed generic `DiagramNode` + `ElectricalComponent` diagrams.
- Electrical terminals participate in the same endpoint map and M17 orthogonal connection router.
- Minecraft renderer rasterizes already-positioned schematic geometry; it does not choose electrical semantics or terminal identities.
- Development schematic `Basic DC Circuit` added to `/scholar_dev_editor` with V1, R1, C1, GND, and four authored point-to-point wires.
- Existing diagram plain-text fallback no longer silently drops electrical elements.
- M17 node editing paths were hardened so static electrical elements are not accidentally cast to `DiagramNode` before the dedicated M18 authoring slice.

## Deliberately Deferred

M18B does **not** add:

- electrical insert menus;
- reference/value editing UI;
- electrical component dragging;
- rotation commands;
- electrical component/terminal hit testing;
- electrical deletion commands;
- diode, LED, or SPST switch;
- junctions or net semantics;
- automatic electrical correctness checks;
- simulation/SPICE/netlist execution;
- breadboard/perfboard physical representations;
- arbitrary vector/user-defined symbol systems.

Those remain assigned to M18C-M18G as described in the roadmap.

## Main Production Files Added

### Electrical semantic model

- `src/main/java/dev/rgcb/scholar/electrical/ElectricalComponent.java`
- `src/main/java/dev/rgcb/scholar/electrical/ElectricalComponentKind.java`
- `src/main/java/dev/rgcb/scholar/electrical/ElectricalOrientation.java`
- `src/main/java/dev/rgcb/scholar/electrical/ElectricalTerminalRole.java`
- `src/main/java/dev/rgcb/scholar/electrical/ElectricalTerminalDefinition.java`
- `src/main/java/dev/rgcb/scholar/electrical/ElectricalComponentDefinition.java`
- `src/main/java/dev/rgcb/scholar/electrical/ElectricalComponentCatalog.java`

### Derived symbol geometry

- `src/main/java/dev/rgcb/scholar/electrical/symbol/NormalizedElectricalPoint.java`
- `src/main/java/dev/rgcb/scholar/electrical/symbol/ElectricalSymbolPrimitive.java`
- `src/main/java/dev/rgcb/scholar/electrical/symbol/ElectricalSymbolLine.java`
- `src/main/java/dev/rgcb/scholar/electrical/symbol/ElectricalSymbolPolyline.java`
- `src/main/java/dev/rgcb/scholar/electrical/symbol/ElectricalSymbolCircle.java`
- `src/main/java/dev/rgcb/scholar/electrical/symbol/ElectricalSymbol.java`
- `src/main/java/dev/rgcb/scholar/electrical/symbol/ElectricalSymbolLibrary.java`

### Positioned electrical layout

- `src/main/java/dev/rgcb/scholar/electrical/layout/LaidOutElectricalPrimitive.java`
- `src/main/java/dev/rgcb/scholar/electrical/layout/LaidOutElectricalLine.java`
- `src/main/java/dev/rgcb/scholar/electrical/layout/LaidOutElectricalPolyline.java`
- `src/main/java/dev/rgcb/scholar/electrical/layout/LaidOutElectricalCircle.java`
- `src/main/java/dev/rgcb/scholar/electrical/layout/LaidOutElectricalComponent.java`
- `src/main/java/dev/rgcb/scholar/electrical/layout/ElectricalSymbolLayoutEngine.java`

## Main Production Files Updated

- `DiagramElement.java`
  - changed from a sealed single-node hierarchy to the small open semantic interface required by real domain extensions.
- `DiagramLayoutEngine.java`
  - layouts both generic nodes and electrical components;
  - registers derived electrical terminals in the same M17 endpoint map;
  - positions designator/value labels and derived symbol geometry.
- `LaidOutDiagram.java`
  - now carries `electricalComponents` alongside generic nodes and connections.
- `MinecraftDocumentRenderer.java`
  - renders positioned line/polyline/circle schematic geometry;
  - uses integer Bresenham for diagonal resistor strokes and midpoint circle rasterization for the DC source.
- `DiagramEditor.java`
  - prevents pre-M18D generic node text/delete/drag paths from miscasting electrical components.
- `DiagramPlainTextSerializer.java`
  - emits descriptive electrical component information rather than silently omitting those elements.
- `DevelopmentDocument.java`
  - adds `Basic DC Circuit` as the M18B visual fixture.

## Tests Added

24 tests across five classes after the visual-polish follow-up:

- `ElectricalComponentTest`
- `ElectricalSymbolLibraryTest`
- `ElectricalSymbolLayoutEngineTest`
- `ElectricalDiagramLayoutTest`
- `ElectricalDiagramRegressionTest`

Coverage includes:

- stable terminal IDs and semantic roles;
- quarter-turn terminal placement and non-centered offset correctness;
- first symbol vocabulary and deterministic geometry;
- resistor/capacitor/source/ground geometry;
- responsive symbol scaling;
- mixed generic/electrical layout;
- generic-to-electrical routed connections;
- reference/value label layout;
- all four M18B component kinds in one diagram;
- plain-text fallback preservation;
- no accidental generic-node editing casts.

## Local Validation Performed In This Environment

Gradle cannot download Gradle 9.2.1 because this sandbox cannot resolve `services.gradle.org`, so `./gradlew test` cannot be used here as the authoritative run.

Performed instead with Java 21:

- Pure-Java production compilation: **PASS**.
- `DevelopmentDocument` compilation against the compiled core: **PASS**.
- New M18B tests through a local JUnit-compatible harness: **22 / 22 PASS**.
- Existing diagram/editor regression suite through the same harness: **107 / 107 PASS** across 11 existing test classes.
- Minecraft document renderer type-check against API-shaped client stubs: **PASS**.
- Pure-Java boundary scan for electrical/diagram/document/layout packages: **PASS**, no Minecraft/NeoForge/Mojang imports.
- Development fixture smoke layout: **4 electrical components / 4 routed connections**, all symbols and terminal endpoints resolved successfully.

## Architecture Records

Added:

- ADR 0203 — `DiagramElement` is open for domain extensions.
- ADR 0204 — electrical layout keeps derived symbol geometry separate from authored AST.

Updated:

- `docs/ELECTRICAL_DIAGRAM_SPEC.md`
- `docs/PROJECT_SPEC.md`
- `docs/ROADMAP.md`
- `docs/adr/README.md`


## Visual QA Follow-up

The first in-game screenshot exposed presentation issues without invalidating the M18B architecture. The follow-up fixes them in pure-Java layout rather than Minecraft renderer offsets:

- DC voltage-source circles stay circular inside non-square authored bounds.
- Source terminal leads now meet the circle exactly instead of leaving visual gaps.
- `+` and `−` polarity marks remain screen/document-upright under quarter-turn rotation.
- Electrical annotation placement now follows deterministic orientation-aware gutters:
  - horizontal parts: reference above, value below;
  - vertical parts: reference left, value right;
  - ground: side placement to stay clear of the top conductor;
  - bounded deterministic fallbacks apply near canvas edges.
- The label gap increased from 2 px to 4 px to reduce visual crowding.
- Two regression tests cover source lead/polarity geometry and the orientation-aware annotation policy.

Local follow-up validation: pure-Java core compilation **PASS**, changed test classes type-check **PASS**, direct M18B visual-layout smoke assertions **PASS**. Full Gradle remains authoritative on the user's machine.

## Manual QA Target

On the user's machine:

```powershell
.\gradlew.bat test
.\gradlew.bat build
.\gradlew.bat runClient
```

Expected Gradle count: approximately **835 tests / 0 failures**.

Open:

```text
/scholar_dev_editor
```

The first scientific block after the editable paragraph should now be **Basic DC Circuit**. It should visibly contain:

- vertical DC source `V1`, `5 V`;
- horizontal zig-zag resistor `R1`, `10 kΩ`;
- vertical capacitor `C1`, `100 nF`;
- ground symbol `GND`;
- orthogonal wires joining their stable derived terminals.

Also verify:

- resizing the window/GUI scale keeps the schematic responsive;
- the whole diagram still selects atomically as a `DiagramBlock`;
- the existing plot, generic System Diagram, and table still render normally;
- no electrical editing UX is expected yet in M18B.

If Gradle and the visual fixture pass, M18B can be closed and M18C can begin.
