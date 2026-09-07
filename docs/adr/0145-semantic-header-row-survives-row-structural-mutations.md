# ADR 145 - Semantic Header Row Survives Row Structural Mutations

## Status

Accepted

## Context

Table headers are represented by `headerRowCount`, not by authored inline Bold marks. Row insertion and deletion can change which physical row occupies index 0.

## Decision

For the current 0/1 header model, row operations preserve `headerRowCount`. If a table has one header row, the row at index 0 after the mutation is the semantic header row.

## Alternatives Considered

- Convert header styling into authored cell marks.
- Drop the header when deleting row 0.
- Track header identity separately from row position.

## Consequences

Header presentation remains derived and column operations do not affect header metadata. Deleting the original header promotes the next surviving row to the semantic header position.
