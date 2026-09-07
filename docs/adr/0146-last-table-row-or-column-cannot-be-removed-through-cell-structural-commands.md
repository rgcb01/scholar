# ADR 146 - Last Table Row Or Column Cannot Be Removed Through Cell Structural Commands

## Status

Accepted

## Context

Deleting the final row or final column would violate `TableBlock` invariants or blur the difference between cell-level structural commands and whole-table deletion.

## Decision

Delete Row is unsupported for one-row tables, and Delete Column is unsupported for one-column tables. These commands do not delete the containing `TableBlock`.

## Alternatives Considered

- Delete the entire table when the last row or column is removed.
- Convert the table to an empty paragraph.
- Permit zero-row or zero-column tables.

## Consequences

Whole-table deletion remains a separate `BlockSelection` behavior. Unsupported structural commands are no-ops and do not create undo history.
