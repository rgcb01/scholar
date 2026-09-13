# ADR 0254 - Control A Selects The Active Editing Scope

## Status

Accepted

## Context

Scholar has multiple editing domains: document prose, math, table cells, figure captions, and scientific object editors. A global select-all would cross boundaries that many operations intentionally do not support.

## Decision

`Ctrl+A` selects the current active editing scope. Text mode selects the contiguous editable inline run containing the active endpoint; equation mode selects the equation root; table mode selects the current cell; figure-caption mode selects the caption; atomic block, plot, and diagram selections remain unchanged.

## Alternatives Considered

- Select the entire document. This would immediately require broad multi-object selection and mutation semantics that are outside M24B.
- Make `Ctrl+A` a no-op everywhere except prose. That would make table, caption, and math editing feel unlike ordinary editors.

## Consequences

Keyboard behavior is useful now while respecting current editing boundaries. A future document-wide select-all can be added as a separate semantic operation.
