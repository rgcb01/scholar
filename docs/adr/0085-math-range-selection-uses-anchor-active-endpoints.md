# 0085 - Math Range Selection Uses Anchor Active Endpoints

## Status

Accepted

## Context

Equation editing needed selection without changing the existing document-level selection model. Text selections already preserve direction with anchor and active endpoints.

## Decision

Math range selection is represented as `MathRangeSelection(anchor, active)` inside `EquationEditingSelection`. Collapsed math selections remain `MathCaretSelection`.

## Alternatives Considered

- Add a separate global document range for math content.
- Store normalized start/end only and lose selection direction.

## Consequences

Math selection direction is preserved for Shift navigation, mouse drag, undo, and redo while equation editing stays isolated from document text selection.
