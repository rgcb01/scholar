# 0079 - Math Editing Uses Virtual Sequence Adapters For Non-Sequence Structural Slots

Status: Accepted

## Context

Many math expressions are single tokens or structural slots containing one non-sequence expression. Editing needs before and after positions around those expressions without forcing the AST to wrap everything in visible sequence nodes.

## Decision

The math editor interprets non-sequence roots and structural slots as virtual one-child sequences for caret movement and editing. The AST is materialized as `MathSequence` only when an edit actually requires multiple children or preserves an existing sequence.

## Alternatives Considered

- Normalize every editable expression to a real `MathSequence`. This would add structural churn unrelated to user edits.
- Special-case all non-sequence roots and slots in every editing operation. This would duplicate traversal behavior.

## Consequences

Editing can use one sequence-oriented algorithm while preserving existing simple AST shapes where possible. Rendering and layout do not need to show synthetic sequence wrappers.
