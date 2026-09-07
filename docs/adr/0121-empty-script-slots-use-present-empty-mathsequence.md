# 0121 - Empty Script Slots Use Present Empty MathSequence

## Status

Accepted

## Context

During authoring, a user can create `x^[]` or `x_[]` before entering content. The AST must distinguish an absent slot from an editable empty slot.

## Decision

A present empty script slot is represented as `Optional.of(MathSequence(List.of()))`. An absent slot remains `Optional.empty()`.

## Alternatives Considered

- Use null for absent or empty slots.
- Store placeholder characters.
- Add a dedicated empty slot AST node.

## Consequences

Script slot lifecycle matches fraction and root empty-slot behavior. Rendering, caret geometry, and hit testing must provide non-zero geometry for present empty slots.
