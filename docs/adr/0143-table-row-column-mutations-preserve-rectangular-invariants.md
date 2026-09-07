# ADR 143 - Table Row Column Mutations Preserve Rectangular Invariants

## Status

Accepted

## Context

`TableBlock` requires at least one row, at least one column, and the same column count in every row. Row and column editing must not introduce transient malformed table structures.

## Decision

Table row and column operations rebuild complete immutable `TableBlock` values that already satisfy rectangularity before they are returned to the editor session.

## Alternatives Considered

- Mutate rows or cells in place.
- Allow temporary malformed tables during editing.
- Move table reconstruction into Minecraft UI code.

## Consequences

Core invariants remain centralized in the document model and editor tests can validate structural operations without Minecraft. Inserted cells are canonical empty cells, and existing cells are preserved semantically.
