# ADR 0244: Dataset Values Are Numeric Text Or Missing

## Status

Accepted

## Context

M23 needs enough value structure for table display and numeric plotting, but units, formulas, uncertainty, and typed scientific quantities are future work.

## Decision

Dataset cells store one of three value kinds: numeric decimal, text, or missing.

## Alternatives Considered

- Store all cells as strings. Plots would need ad hoc numeric inference every time.
- Add units and formulas immediately. This broadens M23 into spreadsheet/scientific-computation territory.
- Treat missing values as empty text. That loses the distinction between absent data and authored empty text.

## Consequences

Tables can display every value deterministically, while plots can require numeric values. Richer scientific quantity semantics remain deferred.
