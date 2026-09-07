# 0082 - New Fractions Begin With Empty MathSequence Numerator And Denominator Slots

Status: Accepted

## Context

A newly inserted fraction is incomplete but must be immediately editable in both numerator and denominator.

## Decision

New structural fractions are created as `MathFraction(new MathSequence(List.of()), new MathSequence(List.of()))`.

## Alternatives Considered

- Store placeholder glyphs in the AST. This would mix editor affordances with semantic content.
- Use `null` slots. This would violate the math model's required-value invariants.

## Consequences

Empty fraction slots are real editable math expressions. Rendering and caret geometry must handle empty sequences without relying on persisted placeholder text.
