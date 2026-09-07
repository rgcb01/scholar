# 0084 - Slash Remains Linear Division And Does Not Implicitly Create A Structural Fraction

Status: Accepted

## Context

Scholar now supports explicit structural fraction creation, but `/` already has clear linear operator semantics in math typing.

## Decision

Typing `/` continues to create a linear binary division operator. Structural fractions require the explicit Insert Fraction action.

## Alternatives Considered

- Convert `/` into a structural fraction automatically. This would make ordinary linear typing unpredictable.
- Convert linear division after later parsing. Markdown/LaTeX-style parsing is out of scope for this milestone.

## Consequences

Math authoring remains predictable: printable typing creates linear tokens, structural actions create structural objects.
