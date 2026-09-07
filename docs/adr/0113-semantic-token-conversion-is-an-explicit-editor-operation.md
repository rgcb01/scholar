# 0113 - Semantic Token Conversion Is An Explicit Editor Operation

## Status

Accepted

## Context

Scholar needs a first workflow for creating semantic tokens from ordinary typed math without adding hidden parser behavior or client-side AST mutation.

## Decision

Semantic token authoring is implemented as an explicit editor operation that converts an eligible same-sequence math range into one `MathNamedOperator` or `MathText` node. The core editor validates eligibility, extracts textual content, and performs immutable range replacement.

## Alternatives Considered

- Put conversion logic directly in Minecraft widgets. This would violate the client/core boundary.
- Add a separate tree-rewrite system. Existing same-sequence replacement already handles this shape of mutation.

## Consequences

Conversion is testable in pure Java, participates in normal history, and keeps UI widgets thin. Unsupported structural selections remain disabled rather than flattened.
