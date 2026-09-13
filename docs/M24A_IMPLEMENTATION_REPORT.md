# M24A Implementation Report - Document Model Invariants And Validation Foundation

Status: implemented with automated validation complete.

M24A adds a pure Java document validation layer for Scholar's current semantic document model. It reports immutable diagnostics without mutating, normalizing, repairing, rendering, or launching Minecraft.

## Validation API

- `DocumentValidator.validate(Document)`
- `DocumentValidationResult`
- `DocumentDiagnostic`
- `DocumentDiagnosticSeverity`
- `DocumentDiagnosticCode`

## Implemented Invariants

- Null document reports an `ERROR`.
- Stable IDs are unique within type-specific namespaces: figures, headings/sections, tables, equations, datasets, dataset columns, and diagram elements.
- Dataset-backed table selected column IDs must not repeat.
- Tables must remain rectangular with at least one row and one column, and header row count must remain within the current supported range.
- Dataset rows must match the dataset column count.
- Figure content must be a supported scientific visual block.
- Diagram elements must remain inside their canvas.
- Diagram element ports must be unique within an element.
- Diagram connections must reference existing elements and ports.

## Degraded Warnings

- Missing cross-reference targets are `WARNING`s.
- Missing dataset bindings are `WARNING`s.
- Missing dataset columns in table or plot bindings are `WARNING`s.
- Missing mechanical constraint or part-reference targets are `WARNING`s.

## Constructor-Enforced Invariants

Several invalid states cannot be built through the public model constructors, including invalid heading levels, duplicate document dataset IDs, duplicate dataset column IDs, ragged tables, invalid figure content, duplicate diagram element IDs, and invalid diagram endpoints. Tests preserve those guarantees.

## Tests

Added `DocumentValidatorTest` covering complex valid documents, duplicate ID namespaces, same textual IDs across different target kinds, broken cross-references, degraded dataset bindings, duplicate dataset table binding columns, skipped heading levels, multiple TOCs, representative electrical and mechanical diagrams, missing mechanical references, duplicate custom ports, constructor-enforced invalid states, deterministic non-mutating validation, null document diagnostics, and immutable validation results.

## Boundary

The validation package imports only Scholar core packages and Java standard library classes. It does not import Minecraft, NeoForge, or Mojang classes.

## Validation

- `.\gradlew.bat test` passed.
