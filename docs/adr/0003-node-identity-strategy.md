# ADR 0003 - Node Identity Strategy

## Status

Accepted

## Context

Persistent node identity may eventually help with cursor positions, selections, references, collaboration, document history, or extension data. Adding IDs too early would affect serialization, equality, tests, and transformations before those requirements are understood.

## Decision

The initial semantic document model does not include persistent node IDs. Identity for future editor state should initially be handled outside the semantic model. Persistent IDs may be introduced later if a concrete requirement justifies them.

## Alternatives Considered

- UUIDs on every node.
- Lightweight internal IDs on every node.
- IDs only on selected node types.
- Editor-state-only identities.

## Consequences

The initial model stays small and semantic. Tests and value equality remain simple. Future editor or collaboration work may require paths, transient handles, or an explicit ID strategy, but that decision can be made with better information.
