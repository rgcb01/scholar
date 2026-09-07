# ADR 0223: Binary mechanical constraint authoring is transient two-step editor state

## Status
Accepted for M19C.

## Decision
Author binary relationships through a transient source-selection state:

1. choose a source mechanical primitive and relationship kind;
2. choose a peer;
3. finish the relationship.

The pending source/kind is editor interaction state, not document data and not an undoable edit.
Only finishing a valid relationship creates one global history transaction.

## Consequences
- The document never contains half-authored constraints.
- Cancel/Escape is history-neutral.
- The interaction mirrors the existing source/target connection workflow without reusing electrical
  port semantics.
- The long Diagram menu remains usable through the generic M19B.1 scrollable-dropdown behavior.
