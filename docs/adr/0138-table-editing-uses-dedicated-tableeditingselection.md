# ADR 138 - Table Editing Uses Dedicated TableEditingSelection

## Status

Accepted

## Context

Tables are structured document blocks, but table cell text is not addressed by document-level `DocumentPosition` values.

## Decision

Interactive table cell editing uses a dedicated `TableEditingSelection` mode that stores the owning table block index plus a cell-local text selection.

## Alternatives Considered

- Reuse document text selections for cell text.
- Treat each cell as an independent document block.
- Keep tables atomic until a generic embedded editor framework exists.

## Consequences

Table editing state is explicit and undoable through the existing editor history. Future table-specific selection types can be added without overloading document text positions.
