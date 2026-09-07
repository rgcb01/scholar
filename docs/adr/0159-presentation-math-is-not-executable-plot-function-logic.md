# ADR 0159 - Presentation Math Is Not Executable Plot Function Logic

## Status

Accepted

## Context

Scholar already has a presentation-oriented Math AST. Future function plots will need bounded computation, which has different safety and semantic requirements from typesetting.

## Decision

The existing `MathExpression` hierarchy is not treated as executable function logic. M16B stores static XY data only. A future function-plot evaluator must use a separately approved computational expression model, with presentation math connected only deliberately.

## Alternatives Considered

- Evaluate the existing Math AST directly.
- Store arbitrary textual code in PlotBlock and execute it.

## Consequences

Typesetting remains separated from computation, preventing accidental evaluation semantics from leaking into the presentation model.
