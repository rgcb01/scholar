# M18H Implementation Report — Final Electrical Visual Polish

## Status

Implemented on top of the user-accepted M18G project. M18H is the final M18 closeout slice and awaits the user's authoritative Gradle and in-game visual QA before M18 as a whole is marked complete.

Starting accepted baseline: **924 test invocations / 0 failures**.
M18H adds **5** ordinary JUnit regressions, so the expected full Gradle count is **929**.

## Root Cause From Manual QA

The visible "terminal sticking past the connection" artifact did not come from terminal IDs, logical anchors, or electrical-net semantics. M18C's routing polish reserved a fixed **14 px** straight exit at each electrical endpoint. When a port faced away from the actual counterpart, the generic orthogonal route could go outward and then immediately back across the same axis before continuing.

Examples from the `Junction + Net` fixture before M18H included:

- source TOP terminal: terminal -> 14 px upward -> back to terminal -> resistor;
- junction -> capacitor TOP terminal: route passed 14 px above the capacitor terminal and then returned to it;
- diode RIGHT cathode wired toward a ground on its left: route extended 14 px to the right before reversing left.

Those are presentation spikes, not semantic connectivity errors.

## Implementation

### Compact adaptive electrical exits

`DiagramLayoutEngine.ELECTRICAL_PORT_EXIT_LENGTH` is reduced from 14 px to **8 px maximum**.

The actual exit used by one electrical endpoint is now clamped against the counterpart position in that terminal's outward half-plane:

- LEFT only reserves distance when the counterpart is left;
- RIGHT only reserves distance when the counterpart is right;
- TOP only reserves distance when the counterpart is above;
- BOTTOM only reserves distance when the counterpart is below;
- otherwise the artificial exit is zero.

Junction endpoints retain their existing zero-exit behavior.

### Electrical route normalization

After the generic M17 router produces a route that touches an electrical component or explicit junction, M18H removes redundant collinear vertices. Straight intermediate points collapse, and collinear out-and-back spikes collapse to the direct segment.

The normalization explicitly preserves:

- exact first/last endpoint coordinates;
- orthogonal routing;
- the two-point contract for coincident semantic endpoints.

No second electrical router was introduced.

## Semantic Boundaries Preserved

M18H does **not** change:

- `ElectricalComponent.bounds()`;
- `DiagramPortPlacement` or oriented terminal anchors;
- stable terminal IDs/roles;
- port hit boxes or component hit testing;
- snapping/connection authoring targets;
- `DiagramConnection` endpoint references;
- junction semantics;
- `ElectricalNetResolver` / named nets;
- clipboard/plain-text behavior;
- global history;
- zoom/pan/workspace state.

## Regression Coverage

`ElectricalTerminalVisualPolishTest` adds five regressions:

1. electrical terminal exit maximum is now the compact 8 px generic maximum rather than 14 px;
2. aligned voltage-source -> resistor routing collapses to one clean segment with no terminal spike;
3. junction -> top-facing capacitor stops exactly at the capacitor terminal and never runs above it;
4. a right-facing diode cathode routed toward a left-side ground never protrudes to the right of its terminal;
5. terminal hit selection and derived semantic net connectivity remain unchanged after the presentation polish.

The earlier `ElectricalConnectionIntegrationTest` was updated to assert exact terminal endpoints, orthogonality, and absence of collinear endpoint backtracking rather than requiring the old fixed 14 px presentation segment.

## Validation In This Environment

- Pure-Java Scholar production core compiled with Java 21: **PASS**.
- Focused M18H + junction + M18G hardening regressions: **20/20 passing**.
- All 77 non-Minecraft test source classes compiled and executed through the same lightweight JUnit-compatible reflection harness: **916/916 passing**.
- The four pure client UI/controller test classes were compiled with only API-shape stubs for the Minecraft-backed menu/toolbar height constants and executed: **13/13 passing**.
- Combined local-equivalent execution count: **929/929 passing**.
- Real Gradle/NeoForge execution remains authoritative on the normal development machine.

## Manual QA Checklist

1. Run `.\\gradlew test` and expect **929 tests / 0 failures**.
2. Open `Junction + Net` and inspect the source -> R1 connection: there must be no small terminal spike on the source side.
3. Inspect junction -> C1: the wire must terminate at C1's top terminal and must not extend above the component connection point before returning.
4. Inspect D1 cathode -> GND: no short line may protrude to the right of D1 before the wire travels left/down toward ground.
5. Rotate resistor/diode/capacitor/source through quarter turns and reconnect representative terminals; wires must still end exactly at the same terminal anchors.
6. Click the same electrical terminals at normal and changed GUI scale; terminal selection/hit regions must feel unchanged.
7. Confirm `VOUT`, junction fan-out, copy/paste, zoom/pan, canvas resize, symbol scale, and undo/redo remain unchanged.

If these visual checks pass, **M18 Electrical Diagrams is complete** and M19 Mechanical Diagrams can begin.
