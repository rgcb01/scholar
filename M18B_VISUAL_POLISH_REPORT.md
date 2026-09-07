# M18B Visual Polish Follow-up

## Trigger

Manual in-game QA showed the first electrical schematic working semantically, but exposed three presentation issues: the DC source leads did not visually meet the circle, the rotated polarity minus was not clearly readable, and V1/C1/GND annotations crowded conductors and each other.

## Changes

- `ElectricalSymbolLayoutEngine` now derives DC-source lead/circle geometry in document space so a circular body remains circular in rectangular bounds and both terminal leads meet the circle exactly.
- DC-source polarity marks are positioned according to semantic terminal direction but remain document-upright under rotation.
- `DiagramLayoutEngine` now uses orientation-aware electrical annotation gutters with deterministic bounded fallback positions.
- Ground reference labels prefer a side gutter instead of the top-terminal conductor.
- Electrical label gap increased to 4 px.
- `ELECTRICAL_DIAGRAM_SPEC.md` documents the derived presentation policy.

## Regression Tests Added

1. Vertical DC source: leads meet the circle and `+`/`−` remain upright.
2. Electrical annotations: V1/C1 use side gutters, R1 uses top/bottom gutters, and GND stays clear of the top lead.

Expected project test count after the follow-up: approximately **835 tests**.

## Local Validation

- Pure-Java production compilation with Java 21: PASS.
- Changed JUnit test classes type-check against minimal API-compatible stubs: PASS.
- Direct smoke assertions for source geometry and label placement: PASS.
- Full Gradle could not run because the sandbox cannot resolve `services.gradle.org`; the user's local Gradle run remains authoritative.
