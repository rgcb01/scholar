# ADR 144 - Table Structural Mutations Preserve Table Editing Mode

## Status

Accepted

## Context

Row and column commands are invoked from an active table cell. Falling back to whole-table selection after each command would interrupt focused table authoring.

## Decision

Successful table structural mutations keep `TableEditingSelection` active and collapse the caret to offset 0 in the deterministic target cell created or retained by the operation.

## Alternatives Considered

- Select the whole table after structural mutation.
- Preserve the previous cell-local text range.
- Move focus to a document-level position outside the table.

## Consequences

Users can type immediately into newly inserted cells. Structural changes are still distinct from text edits in history, and preferred visual caret X is reset.
