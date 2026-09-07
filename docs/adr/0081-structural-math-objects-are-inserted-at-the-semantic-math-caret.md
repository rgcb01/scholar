# 0081 - Structural Math Objects Are Inserted At The Semantic Math Caret

Status: Accepted

## Context

Math authoring now needs to create nested structures while the user is editing inside an `EquationBlock`. The insertion point must come from semantic math state, not screen coordinates or document-level text positions.

## Decision

Structural math insertion operates at the active `MathPosition` inside `EquationEditingSelection`.

## Alternatives Considered

- Insert structures through document-level `DocumentPosition`. This cannot address nested math slots.
- Insert structures from screen-local callbacks. This would bypass shared action/session behavior.

## Consequences

Math structures can be authored recursively and remain unit-testable in pure Java. UI commands must route through editor actions and `EditorSession`.
