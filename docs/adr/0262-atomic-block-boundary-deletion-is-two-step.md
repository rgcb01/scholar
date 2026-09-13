# ADR 0262 - Atomic Block Boundary Deletion Is Two Step

## Status

Accepted

## Context

Complex blocks such as equations, tables, plots, diagrams, figures, and TOC blocks can sit next to editable prose. A single Delete or Backspace at a text boundary could either merge text, skip over the object, or delete the object.

## Decision

At a text boundary next to an atomic block, the first Delete or Backspace selects the atomic block. A second explicit Delete or Backspace removes the selected block.

## Alternatives Considered

- Delete the atomic block immediately from the adjacent text caret. This is too easy to trigger accidentally.
- Ignore the key entirely at atomic boundaries. This makes keyboard navigation and deletion feel stuck.

## Consequences

Boundary behavior is visible and deliberate. Complex scientific content is not removed by a surprising one-key hidden mutation.
