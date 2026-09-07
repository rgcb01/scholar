# 0126 - Structural Math Group Authoring Is Explicit

## Status

Accepted

## Context

Grouping changes the math AST and enables future structural operations such as scripting a multi-atom expression. Silent conversion during typing or paste would make editing behavior harder to reason about.

## Decision

Collapsed group commands insert an empty `MathGroup` and place the caret inside it. Group commands over compatible same-sequence ranges wrap complete sibling atoms and place the caret after the new group.

## Alternatives Considered

- Infer groups from typed delimiters.
- Allow group wrapping across structural slots.
- Allow partial token selections to be wrapped by splitting tokens.

## Consequences

Group creation is one deliberate structural edit with clear undo behavior. Partial-token and cross-slot ranges remain rejected until a broader fragment model exists.
