# ADR 0024: Immutable Edit Transformation Strategy

## Status

Accepted

## Context

The existing Document AST is immutable. The first editing slice needs typing, Backspace, Delete, caret movement, and re-layout without introducing a broad command framework or mutable editor model.

## Decision

Use a small Minecraft-independent editor core. Editing operations transform an old immutable `Document` into a new immutable `Document` and return updated editor state.

Use `replaceRange` as the core primitive for document-changing edits. Typing, Backspace, and Delete are expressed in terms of that primitive.

## Alternatives Considered

- Direct ad hoc mutation helpers.
- Command objects and operation history from the start.
- Mutable document mirror converted back to immutable AST.

## Consequences

The first editor remains small and testable. Undo/redo can later store document snapshots or operations. Transformations must rebuild affected AST structures rather than mutating them in place.
