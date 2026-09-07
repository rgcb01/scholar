# 0118 - Square Roots Are Authored Explicitly As Structured Math

## Status

Accepted

## Context

Scholar's math editor avoids silent semantic inference. `MathRoot` already exists in the AST, but external plain text and ordinary typing should remain lexical unless the user chooses a structural action.

## Decision

Square roots are authored through an explicit Root action. Typing or pasting `sqrt(x)` does not create `MathRoot`.

## Alternatives Considered

- Infer `sqrt(...)` from ordinary input.
- Add a LaTeX-like parser.
- Store roots as plain text syntax.

## Consequences

The AST remains the source of truth and user intent stays explicit. Plain-text `sqrt(...)` remains a readable fallback, not a round-trip structural syntax.
