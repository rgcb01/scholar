# ADR 0012 - Math Layout Separation From Document Layout

## Status

Accepted

## Context

Document layout currently handles one-dimensional text flow. Math notation introduces two-dimensional structures such as fractions, scripts, roots, and future matrices. Mixing all math layout behavior into document layout would make the document layout engine harder to test and evolve.

## Decision

Math will eventually use a dedicated math layout subsystem consumed by document layout. Milestone 4A implements only the Math AST and does not implement math layout.

## Alternatives Considered

- Extend `DocumentLayoutEngine` directly for every math node.
- Render math directly from Minecraft code without an intermediate layout.
- Delay the layout boundary decision until math rendering begins.

## Consequences

Math layout can develop with its own metrics and box concepts while document layout remains focused on document flow. A later integration layer will be needed when display equations and inline math are added.
