# 0091 - Math Clipboard Fragments Use MathSequence As Their Canonical Root

## Status

Accepted

## Context

Math selections may contain one atom, multiple atoms, partial token content, or a whole structural node.

## Decision

Every math clipboard payload stores a `MathSequence` fragment as its canonical root.

## Alternatives Considered

- Add a separate `MathFragment` wrapper.
- Store arbitrary `MathExpression` roots directly.

## Consequences

Clipboard insertion can use one sequence-splicing model without introducing a new fragment hierarchy. Empty fragments are treated as no-ops.
