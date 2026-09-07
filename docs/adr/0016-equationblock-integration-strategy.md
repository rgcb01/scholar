# ADR 0016 - EquationBlock Integration Strategy

## Status

Accepted

## Context

Scholar needs a first way to place display equations inside documents while keeping math semantics, math layout, document layout, and Minecraft rendering separated.

## Decision

Display equations enter the document model as `EquationBlock(MathExpression)`. Document layout recognizes equation blocks, delegates internal equation layout to `MathLayoutEngine`, and places the resulting math box in document block flow.

## Alternatives Considered

- Store equations as paragraph text.
- Add inline math first.
- Put display options, numbering, labels, or captions into the initial block.

## Consequences

The first math integration stays minimal and structural. Future numbering, labels, captions, and inline math can be added later without making the first display equation block carry speculative metadata.
