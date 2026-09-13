# ADR 0235: Document Structure Is Derived From Flat Heading Blocks

## Status

Accepted

## Context

Scholar documents are stored as an ordered flat list of blocks. M22 needs section hierarchy for labels, navigation, and document outline without introducing an authoritative `Section` AST that would duplicate block order.

## Decision

Derive `DocumentStructure` from the current ordered `Heading` blocks. The resolver produces `SectionEntry` values with block index, heading level, optional stable heading ID, derived number, and title preview.

## Alternatives Considered

- Add a persistent `Section` tree to the document AST. This would duplicate the existing block order and create synchronization problems.
- Let each UI feature derive its own structure. This risks inconsistent section numbers and navigation targets.

## Consequences

The document AST remains flat and small. Structure-sensitive features must call the shared resolver after document changes instead of caching section state.
