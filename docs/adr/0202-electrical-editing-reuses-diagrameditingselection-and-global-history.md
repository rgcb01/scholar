# ADR 0202 - Electrical Editing Reuses DiagramEditingSelection And Global History

## Status

Accepted

## Context

M17 already solved embedded diagram selection, drag previews, explicit connection drafts, structural edits, and one-transaction undo/redo. A separate electrical selection/history architecture would duplicate difficult interaction logic and create inconsistent behavior between generic and electrical diagrams.

## Decision

M18 extends the existing `DiagramEditingSelection`, hit-testing, editor-session, connection-draft, and global `EditorHistory` paths with typed electrical component operations. Insert, drag, rotate, annotation edit, connection creation, and deletion use immutable document replacement and the same history semantics as M17.

No electrical-specific undo stack or editor mode is introduced.

## Alternatives Considered

- Add `ElectricalDiagramEditingSelection` as a parallel editor stack.
- Mutate electrical components directly outside global history.
- Implement wiring through a separate Minecraft-only interaction controller.

## Consequences

Electrical diagrams feel like a native extension of Scholar rather than a second application embedded inside it. M18 will require targeted polymorphic cleanup where M17 code currently casts every element to `DiagramNode`.
