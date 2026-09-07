# 0050 Typing Mark Carryover Across Structural Splits

Status: Accepted

## Context

Enter creates a new insertion context. The editor already distinguishes inferred caret marks from explicit typing marks, and that distinction matters after a structural split.

## Decision

Before Enter, the editor session captures the effective typing marks. After the structural edit, the resulting state stores those marks as explicit typing marks, including an explicit empty set.

## Alternatives Considered

- Clear typing marks after Enter.
- Re-infer marks from the new neighboring text.
- Store inline formatting state in the Document AST.

## Consequences

Typing after Enter continues the user's formatting intent. Empty explicit marks can intentionally override misleading neighboring affinity, while the Document AST remains free of editor state.
