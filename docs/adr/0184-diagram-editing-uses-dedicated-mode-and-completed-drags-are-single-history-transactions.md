# ADR 0184 - Diagram Editing Uses Dedicated Mode And Completed Drags Are Single History Transactions

## Status

Accepted

## Context

A DiagramBlock must remain an atomic object during ordinary document navigation while still supporting internal selection and mouse manipulation. Continuous mouse movement also creates many pointer updates that should not each become an undo step.

## Decision

Internal diagram editing uses a dedicated `DiagramEditingSelection` with typed semantic targets rather than text selection. Enter from a selected diagram enters internal editing; Escape returns to whole-block `BlockSelection`.

Element dragging uses transient interaction state outside the semantic AST/history during pointer movement. A completed drag commits the final logical element position as one immutable document edit and one global `EditorHistory` transaction. Canceling a drag commits nothing.

The first drag slice moves one element only. Multi-selection, resize handles, snapping, and alignment guides are deferred.

## Alternatives Considered

- Reuse text selections for diagram objects.
- Automatically enter diagram editing whenever document navigation reaches a DiagramBlock.
- Push one history snapshot for every mouse-move event.
- Introduce a separate diagram-specific undo stack.

## Consequences

Diagram interaction follows Scholar's proven embedded-editor pattern and preserves predictable document navigation. Undo reverses a human drag gesture in one step, while transient pointer state remains outside semantic documents and history snapshots.
