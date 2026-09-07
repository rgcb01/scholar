# ADR 136 - TableBlock Is An Atomic Document Selection Target

## Status

Accepted

## Context

The first table milestone does not include cell editing, but tables must still behave predictably in mixed documents.

## Decision

`TableBlock` participates in plain document navigation, hit testing, selection, deletion, and undo/redo as an atomic `BlockSelection`, matching the existing non-text block behavior.

## Alternatives Considered

- Skip tables during navigation.
- Treat each cell as an editable text block immediately.
- Add a dedicated table editing selection mode now.

## Consequences

Tables can be inserted, selected, rendered, deleted, and restored without introducing table editing architecture. Cell-level caret placement and selection remain future work.
