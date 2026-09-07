# 0098 - External Slash Imports As Linear Division

## Status

Accepted

## Context

Scholar already treats typed `/` as a linear division operator. Automatically turning pasted slash text into stacked fractions would make paste semantics differ from typing.

## Decision

External `/` and normalized `÷` import as `MathOperator("/", BINARY)`, never as `MathFraction`.

## Alternatives Considered

- Convert `x/y` to a structural fraction.
- Use heuristics based on parentheses or spacing.

## Consequences

External text paste remains aligned with typing. Structural fractions still round-trip through Scholar-native sidecar clipboard data.
