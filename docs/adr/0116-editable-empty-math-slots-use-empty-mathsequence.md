# 0116 - Editable Empty Math Slots Use Empty MathSequence

## Status

Accepted

## Context

Fraction authoring already uses `MathSequence(List.of())` for empty numerator and denominator slots. Square root authoring needs an editable empty radicand without adding source characters or placeholder nodes.

## Decision

Editable empty math slots are represented as `MathSequence(List.of())`. M13B applies this to empty square root radicands.

## Alternatives Considered

- Fake placeholder AST nodes.
- Whitespace or underscore source nodes.
- Client-only slot state.

## Consequences

The AST remains semantic and immutable. Layout and hit testing must provide minimum geometry for empty slots, but that geometry remains presentation state and does not serialize.
