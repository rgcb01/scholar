# 0106 - Named Mathematical Operators Use Explicit Semantic Nodes

## Status

Accepted

## Context

Named mathematical operators such as sin, log, lim, rank, and det are authored differently from adjacent variables and are conventionally typeset differently. Reusing `MathIdentifier` for them would make typography and structured export ambiguous.

## Decision

Scholar represents explicitly authored named mathematical operators with `MathNamedOperator(String name)`. Names are arbitrary authored strings, not a hardcoded keyword enum. The node does not represent function invocation or evaluation semantics.

## Alternatives Considered

- Store named operators as `MathIdentifier`.
- Add a `MathFunctionCall` AST.
- Use a fixed enum of known functions.

## Consequences

Typography and future export can distinguish named operators from variables. Third-party and discipline-specific operators can be represented without modifying Scholar core.
