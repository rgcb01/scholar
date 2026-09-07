# ADR 140 - First Table Editing Slice Supports Only Single Cell Text Selection

## Status

Accepted

## Context

M15C needs usable cell text editing without designing spreadsheet-style or table-structural selection.

## Decision

The first interactive table slice supports caret placement and text selection only within one active cell. Mouse drag is clamped to the active cell.

## Alternatives Considered

- Multi-cell rectangular selection.
- Cross-cell text selection.
- Row, column, and table object selection modes inside table editing.

## Consequences

Cell text editing can ship without table clipboard, TSV, or range algebra. Multi-cell selection remains a dedicated future milestone.
