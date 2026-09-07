# M18B Visual Polish Test Fix

## Failure observed

`ElectricalDiagramLayoutTest.electricalAnnotationsPreferGuttersThatStayClearOfTerminalLeads()` failed at the assertion requiring the capacitor value label to be placed in the right gutter.

## Root cause

The production layout was behaving as designed. The regression test used a 256 px document width, where the capacitor had only 36 px of free space between its right edge and the canvas boundary. The text `100 nF` is 36 px wide under the fixed test measurer and the preferred gutter requires an additional 4 px (`ELECTRICAL_LABEL_GAP`). Therefore the preferred right-side placement physically cannot fit at that test width, so the layout correctly falls back to another candidate side.

The test nevertheless asserted that the right-side placement must always be used. Its premise was contradictory to the bounded fallback policy introduced by the visual-polish pass.

## Fix

The preferred-gutter regression now lays out the fixture at 320 px, where the preferred left/right and top/bottom gutters all fit. A comment documents why this test deliberately needs enough room to test *preference* rather than constrained fallback behavior.

No production code was changed.

## Validation

- Changed test source compiles against the project core and JUnit-compatible stubs.
- Direct assertion smoke test reproducing all assertions in the regression at 320 px: PASS.
- Full Gradle cannot run in this sandbox because `services.gradle.org` is not resolvable.

Expected project baseline remains **835 tests**.
