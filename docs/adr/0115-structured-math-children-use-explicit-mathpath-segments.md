# 0115 - Structured Math Children Use Explicit MathPath Segments

## Status

Accepted

## Context

Fractions already use explicit `MathPathSegment` records for numerator and denominator. M13B adds square root editing and needs a way to address the radicand without replacing the working snapshot-local path model.

## Decision

Structured math children use explicit node-specific `MathPathSegment` records. Square root radicands are addressed with `RootRadicand`.

## Alternatives Considered

- Generic slot keys or string identifiers.
- A universal structured slot framework.
- Persistent node IDs.

## Consequences

Paths stay type-oriented, small, and easy to validate. Future structures can add explicit segments when they become editable. A broader slot abstraction remains deferred until repetition proves it is needed.
