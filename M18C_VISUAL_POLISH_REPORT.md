# M18C — Electrical Symbol Visual Polish

## Status

Implemented on top of the user's M18C ZIP after manual QA confirmed that terminal-to-terminal connection creation works but the diode/LED/SPST symbols felt visually oversized and routed elbows could sit too close to electrical terminals.

This follow-up remains part of **M18C**. It does not add M18D authoring features.

Starting expected baseline: **849 test executions / 0 failures**.
This follow-up adds **2 ordinary JUnit `@Test` methods** and expands an existing routing test, so the expected Gradle execution count is approximately **851 tests**.

## Changes

### 1. Compact visible symbol bodies, unchanged semantic footprint

The authored `ElectricalComponent.bounds()` still defines:

- stable logical placement;
- terminal positions;
- forgiving component hit/drag area;
- connection endpoints.

Only derived presentation geometry became more compact. Resistor, capacitor, diode, LED, SPST, ground, and the DC-source body now devote more of the authored footprint to straight terminal leads and less to the central symbol body.

This intentionally separates:

```text
authored interaction / terminal footprint
┌───────────────────────────────┐
│  terminal ─── compact body ─── terminal
└───────────────────────────────┘
```

from the visible body size without introducing any new AST state.

### 2. Compact annotation placement

Reference designators and values now use a centered derived annotation footprint equal to **62%** of the laid-out interaction bounds.

The existing orientation policy is preserved:

- horizontal parts: reference above, value below;
- vertical parts: reference left, value right;
- ground: side annotation.

The difference is that labels track the visible body instead of sitting around the outer hit box.

### 3. Longer electrical terminal exits before elbows

`DiagramConnectionRouter` now has an overload accepting independent source/target exit lengths. Generic M17 routing still uses its original **8 px** default.

`DiagramLayoutEngine` requests **14 px** at electrical endpoints, independently per endpoint. This gives wires a clearer straight run before the first orthogonal elbow while retaining the single generic router and all existing endpoint semantics.

No obstacle avoidance, net inference, junction semantics, or electrical-specific router was added.

## Production Files Changed

- `src/main/java/dev/rgcb/scholar/electrical/symbol/ElectricalSymbolLibrary.java`
- `src/main/java/dev/rgcb/scholar/electrical/layout/ElectricalSymbolLayoutEngine.java`
- `src/main/java/dev/rgcb/scholar/diagram/layout/DiagramConnectionRouter.java`
- `src/main/java/dev/rgcb/scholar/diagram/layout/DiagramLayoutEngine.java`
- `docs/ELECTRICAL_DIAGRAM_SPEC.md`

## Tests

Added/expanded regression coverage for:

- compact diode body while terminal leads still reach the full authored perimeter;
- generic endpoint using the normal 8 px exit while an electrical endpoint receives the 14 px exit in the same routed connection;
- forgiving electrical element hit target remaining the full authored bounds after compacting symbol geometry;
- updated annotation-gutter assertions around the compact presentation footprint.

## Local Validation

Gradle itself was not used in this environment. Validation performed with Java 21:

- all non-client Scholar production sources compiled with `javac --release 21`: **PASS**;
- changed/focused electrical tests compiled against a lightweight JUnit-compatible local harness: **PASS**;
- focused changed symbol/layout/routing/hit tests: **29 passed / 0 failed**;
- generic `DiagramConnectionRouterTest`: **7 passed / 0 failed**;
- broader diagram + electrical + diagram-editor regression harness: **147 passed / 0 failed**;
- `DevelopmentDocument` compiled against the updated core: **PASS**;
- `Electrical Symbols` development fixture layout smoke at 1120 px: **PASS**; compact bodies, full-size ports/hit bounds, and compact annotations remained aligned.

The user's NeoForge Gradle run and in-game rendering remain authoritative.

## Manual QA

Run:

```powershell
.\gradlew.bat test
.\gradlew.bat build
.\gradlew.bat runClient
```

Expected full test count: approximately **851 / 0 failures**.

In `/scholar_dev_editor`, verify:

1. D1, D2, and S1 bodies are visibly much smaller while the straight leads still reach their terminals.
2. `D1` / `1N4148`, `D2` / `LED`, and `S1` / `SPST` sit closer to their symbols.
3. Clicking the generous empty area inside an electrical component's authored footprint still selects the component.
4. Clicking a terminal still selects the terminal before the owning component.
5. A newly created electrical connection runs straight away from each electrical terminal before its first elbow.
6. `Basic DC Circuit` remains readable and connected after the same compact-body policy is applied.
7. Resize / GUI scale still preserves alignment and terminal identity.

If these pass, M18C can be manually closed and M18D Electrical Authoring can begin.
