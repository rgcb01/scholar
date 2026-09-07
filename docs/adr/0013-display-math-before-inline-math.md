# ADR 0013 - Display Math Before Inline Math

## Status

Accepted

## Context

Scholar will eventually need both display equations and inline math. Inline math requires careful baseline integration with surrounding text, while display equations can prove the Math AST and two-dimensional layout path with fewer text-flow complications.

## Decision

The first document integration target for Scholar Math will be display/block equations. Inline math will follow after baseline and layout behavior are better understood.

## Alternatives Considered

- Implement inline and display math together.
- Implement inline math first because it appears in ordinary prose.
- Keep math completely separate from documents indefinitely.

## Consequences

The first visual math slice can focus on clear scientific notation such as a structural fraction. Inline math remains deferred until the project has a stronger baseline and measurement strategy.
