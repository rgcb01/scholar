# 0112 - Semantic Math Tokens Are Atomic In The Initial Editor Slice

## Status

Accepted

## Context

`MathNamedOperator` and `MathText` are existing AST nodes, but the current math caret model only supports internal character positions for editable number and identifier tokens.

## Decision

In the initial editor slice, semantic math tokens are atomic. Carets can sit before or after them, selection can include them as whole atoms, and adjacent Backspace/Delete removes the whole node.

## Alternatives Considered

- Add internal character carets for semantic tokens immediately. This would broaden the math position model before the editing behavior is proven.
- Decompose semantic tokens during navigation. This would weaken their role as semantic atoms.

## Consequences

Semantic tokens are predictable and fit existing source-path navigation. Editing token content happens through explicit replacement rather than in-place character editing.
