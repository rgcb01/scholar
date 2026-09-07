# 0111 - Semantic Math Tokens Are Authored Explicitly

## Status

Accepted

## Context

Strings such as `sin`, `rank`, `if`, and `where` are ambiguous in ordinary math input. They may be adjacent identifiers, named operators, or descriptive text.

## Decision

Scholar creates `MathNamedOperator` and `MathText` only through explicit semantic authoring actions or native structured clipboard data. Ordinary typing and external plain-text paste do not infer these semantic nodes.

## Alternatives Considered

- Infer common functions or words automatically. This would make ordinary typing unpredictable and require a keyword policy.
- Treat all alphabetic runs as named semantic tokens. This would conflict with adjacent-variable notation such as `xy`.

## Consequences

User intent stays explicit and reversible. Future convenience palettes may still create semantic tokens, but they must remain explicit actions rather than inference.
