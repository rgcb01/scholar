# ADR 0253 - Text Selections Cannot Cross Atomic Blocks

## Status

Accepted

## Context

Scholar documents mix editable prose with atomic scientific blocks such as equations, tables, plots, diagrams, figures, and table-of-contents blocks. Treating the full document as one flat text stream would blur source text with non-text object boundaries.

## Decision

`TextSelection` may span only contiguous editable inline blocks. It cannot point inside, start from, end in, or cross an atomic non-text block.

## Alternatives Considered

- Permit text selections to cross atomic blocks and skip those blocks during text operations. That would make selection semantics misleading and inconsistent with deletion/formatting barriers.
- Represent atomic blocks as synthetic text characters. That is deferred until object selection semantics need a richer range model.

## Consequences

Text editing remains source-text based and predictable. Atomic blocks continue to use `BlockSelection` outside their own dedicated editing modes.
