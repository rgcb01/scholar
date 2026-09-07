# 0067 - Structural Insertions Use Between-Block DocumentInsertionPoint

Status: Accepted

## Context

Object blocks are inserted between document blocks, not inside text offsets.

## Decision

Structural insertion uses DocumentInsertionPoint, where index 0 means before the first block and document size means after the final block.

## Alternatives Considered

- Reuse previous-block-end or next-block-start text positions. This would make structural insertion depend on text-only concepts.

## Consequences

Insertion is explicit, index-based, and reusable for future Table, Plot, and Diagram blocks.
