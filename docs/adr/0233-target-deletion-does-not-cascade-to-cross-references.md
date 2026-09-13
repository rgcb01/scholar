# ADR 0233: Target Deletion Does Not Cascade To Cross References

## Status

Accepted

## Context

Scholar editing uses immutable document snapshots and ordinary undo/redo. A target deletion may affect references elsewhere in the document through resolution, but it does not structurally edit those references.

## Decision

Deleting a referenced target changes only the selected target block. Existing references remain semantic inline nodes and become broken until the target exists again.

## Alternatives Considered

- Delete all references to the removed target in the same history transaction.
- Rewrite references to `[Missing reference]` text.
- Prevent deleting referenced targets.

## Consequences

Target deletion is local and predictable. Undo restores both the target block and resolved labels without special reference-repair history logic.
