# ADR 134 - Table Cells Own InlineContent Based TableCellContent

## Status

Accepted

## Context

The first table slice needs cells that can display document prose without introducing nested block documents or table-specific inline markup.

## Decision

Each `TableCell` owns a `TableCellContent`, and `TableCellContent` wraps `InlineContent`. Empty cells are represented by empty inline content, not placeholder text.

## Alternatives Considered

- Store plain strings directly in cells.
- Store full `Document` instances in cells.
- Store nullable content for empty cells.

## Consequences

Cells reuse existing inline text and mark semantics while staying deliberately small. Multi-block cell content, lists, equations inside cells, and nested documents remain deferred.
