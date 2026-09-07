# ADR 0178 - DiagramBlock Stores Semantic DiagramDefinition

## Status

Accepted

## Context

Scholar needs scientific diagrams to remain editable document objects rather than screenshots or renderer-owned drawing commands. M18 electrical and M19 mechanical work also need a reusable document-level diagram boundary without placing discipline-specific fields directly on the document block.

## Decision

`DiagramBlock` owns a non-null immutable `DiagramDefinition`. The definition stores diagram-local semantic content: logical canvas, elements, ports, connections, and plain labels. Routed paths, screen coordinates, selection outlines, Minecraft colors, and hit regions are derived outside the semantic model.

At ordinary document-navigation level, DiagramBlock participates as an atomic `BlockSelection`, consistent with other non-text scientific blocks.

## Alternatives Considered

- Store rendered pixels or drawing commands in the document AST.
- Put all diagram fields directly on `DiagramBlock`.
- Make electrical components the first document-level diagram abstraction.

## Consequences

Diagram semantics remain reusable by layout, editors, clipboard/exporters, and later scientific domains. The document shell stays small, while M18/M19 can evolve above the same diagram container without making rendering the source of truth.
