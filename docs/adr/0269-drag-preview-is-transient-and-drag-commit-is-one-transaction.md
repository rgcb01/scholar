# ADR 0269 - Drag Preview Is Transient And Drag Commit Is One Transaction

## Status

Accepted

## Context

Diagram dragging may produce many pointer-move previews. Recording each preview would make undo unusable.

## Decision

Drag begin and preview live outside history. Cancel creates no transaction. Commit creates one history transaction if semantic geometry changed.

## Alternatives Considered

- Store every pointer preview in history.
- Mutate the semantic document continuously during drag.

## Consequences

Dragging remains responsive and undoable as one user action. Preview rendering must continue to treat preview documents as transient.
