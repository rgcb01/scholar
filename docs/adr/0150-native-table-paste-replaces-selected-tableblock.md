# ADR 150 - Native Table Paste Replaces Selected TableBlock

## Status

Accepted

## Context

When a whole table is selected and the clipboard contains another native table, inserting a second adjacent table is less direct than replacing the selected object.

## Decision

Native table paste over `BlockSelection` of a `TableBlock` replaces that table in place and leaves `BlockSelection` on the replacement.

## Alternatives Considered

- Insert the clipboard table after the selected table.
- Delete first and then insert using generic block insertion.
- Support all atomic block replacement immediately.

## Consequences

Whole-table replacement is deterministic and one undoable history transaction. Other atomic block-selection paste behavior remains deferred unless a safe generic policy is introduced.
