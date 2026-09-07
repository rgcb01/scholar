# ADR 0002 - Document Model Mutability Strategy

## Status

Accepted

## Context

Scholar is expected to support future parsing, serialization, rendering, WYSIWYG editing, undo/redo, and extension points. Directly mutable trees are simple at first but make history, validation, and safe transformations harder to reason about.

## Decision

The core document model is immutable value data. Public model objects do not expose setters or mutable internal collections. Incoming collections are defensively copied.

## Alternatives Considered

- A directly mutable tree.
- A hybrid model with mutable nodes and immutable snapshots.
- Delaying the mutability decision.

## Consequences

The model is easier to test, compare, serialize, and use as a stable source of truth. Future editing will need explicit transformation or command APIs rather than direct node mutation. That adds work later, but it keeps edits controlled and makes undo/redo more tractable.
