# ADR 0266 - One Logical Edit One History Transaction

## Status

Accepted

## Context

Scholar now edits text, equations, tables, plots, diagrams, figures, datasets, and structural blocks. Composite operations often call several helpers internally.

## Decision

A single logical semantic user edit creates one undoable history transaction when it changes the document. Internal helper steps do not create their own history entries.

## Alternatives Considered

- Store every helper mutation as a separate undo entry.
- Build a command journal before stabilizing the editor foundation.

## Consequences

Undo/redo matches user intent and composite edits remain understandable. Tests must inspect transaction counts so helper refactors do not accidentally add undo noise.
