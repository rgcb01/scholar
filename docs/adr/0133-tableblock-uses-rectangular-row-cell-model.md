# ADR 133 - TableBlock Uses Rectangular Row Cell Model

## Status

Accepted

## Context

Scholar needs a first table representation that is useful for scientific notes but small enough to remain stable before table editing exists.

## Decision

`TableBlock` contains a non-empty list of `TableRow` values, each row contains a non-empty list of `TableCell` values, and all rows must have the same number of cells.

## Alternatives Considered

- Ragged rows with missing implicit cells.
- A flattened cell grid with row and column counts.
- A generic block container abstraction for future grid-like objects.

## Consequences

Static layout can calculate predictable columns without guessing missing cells. Future row and column editing will need explicit operations to preserve rectangularity.
