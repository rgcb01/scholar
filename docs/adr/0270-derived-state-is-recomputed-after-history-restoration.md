# ADR 0270 - Derived State Is Recomputed After History Restoration

## Status

Accepted

## Context

Scholar derives section numbers, figure numbers, TOC entries, cross-reference labels, dataset-backed table/plot views, electrical nets, and layout geometry from semantic document state.

## Decision

History stores source semantic `Document` snapshots and editor selections. Derived state is recomputed after undo/redo rather than stored as authoritative history content.

## Alternatives Considered

- Store derived caches in history snapshots.
- Mutate references or TOC blocks during undo/redo to preserve labels.

## Consequences

History restoration stays source-of-truth oriented and avoids stale labels. Derived resolvers must remain deterministic for restored documents.
