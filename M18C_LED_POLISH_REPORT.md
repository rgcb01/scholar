# M18C LED Visual Polish

## Scope

This is a deliberately narrow follow-up to M18C visual QA. No editor, routing, hit-box, terminal, or component-size behavior was changed.

## Visual issue

The DEG_90 LED fixture rendered its light-emission arrows too close to the diode body/cathode region. At Minecraft GUI scale the arrows visually merged with the lower-right part of the symbol and could be mistaken for terminal/wire geometry.

## Change

`ElectricalSymbolLibrary.led()` now places all emission-arrow geometry in a dedicated canonical gutter above the DEG_0 diode body. Because symbol geometry rotates with `ElectricalOrientation`, the same policy produces a clear lateral gap to the body for DEG_90 and corresponding clean gutters for the other quarter-turn orientations.

The diode body, cathode bar, terminal leads, component bounds, hit targets, routing, labels, and terminal IDs are unchanged.

## Regression coverage

Two focused regression tests were added:

- `ElectricalSymbolLibraryTest.ledEmissionArrowsStayInDedicatedGutterAwayFromDiodeBody()`
- `ElectricalSymbolLayoutEngineTest.rotatedLedEmissionArrowsKeepVisibleGapFromSymbolBody()`

The layout regression verifies a minimum derived-space gap between the DEG_90 diode body/cathode and every emission-arrow primitive.

## Validation performed in this environment

- Pure-Java Scholar production sources compiled with Java 21.
- Both modified electrical test classes type-checked against lightweight JUnit API stubs.
- A direct Java smoke test laid out the DEG_90 LED and measured a 3 px minimum body-to-arrow gap at the test scale; it passed.
- At the development-document scale, the normalized geometry corresponds to roughly an 8 px visual gap.
- `./gradlew test` was attempted, but the wrapper cannot resolve `services.gradle.org` in this sandbox.

## Expected project test count

The prior M18C polished baseline was approximately 851 tests. This follow-up adds 2 tests, so the expected Gradle run is approximately 853 tests with 0 failures.
