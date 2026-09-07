# 0124 - Structural Math Groups Coexist With Linear Delimiter Symbols

## Status

Accepted

## Context

Scholar needs structural grouped expressions such as `(x + 1)^2`, while ordinary typed delimiters are already valid linear math symbols.

## Decision

Explicit group actions create `MathGroup`. Typed delimiter characters remain linear `MathSymbol` input and do not infer structural groups.

## Alternatives Considered

- Automatically convert typed delimiter pairs into `MathGroup`.
- Treat all delimiters as plain symbols until a later parser exists.
- Parse LaTeX-like group syntax during editing.

## Consequences

Authoring is predictable and avoids accidental structure. Users need explicit group commands for structural behavior. A future parser can still add delimiter inference as a separate approved feature.
