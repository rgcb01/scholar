# 0076 - Equation Editing Is A Distinct EditorSelection Mode

Status: Accepted

## Context

Scholar already distinguishes text selections from structural block selections. Editing inside an `EquationBlock` needs to target a math caret while still knowing which document block owns the equation.

## Decision

Equation editing is represented as `EquationEditingSelection`, an `EditorSelection` variant carrying the equation block index and an internal `MathSelection`.

## Alternatives Considered

- Extend `BlockSelection` with optional math focus. This would make object selection and internal editing ambiguous.
- Keep equation caret state only in the screen. This would prevent undo, redo, actions, and tests from observing the actual editor mode.

## Consequences

Document actions can explicitly disable or route behavior while equation content is focused. `EditorState` must validate that equation editing selections point at `EquationBlock` instances.
