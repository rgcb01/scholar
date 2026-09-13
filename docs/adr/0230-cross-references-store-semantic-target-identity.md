# ADR 0230: Cross References Store Semantic Target Identity

## Status

Accepted

## Context

Scholar needs references such as "Figure 1" and "Equation 2" inside ordinary inline document content. Display numbers can change when blocks are inserted, deleted, or reordered, so storing the rendered label would make references stale.

## Decision

Represent references as a first-class inline node, `CrossReference(kind, targetId)`, where `kind` identifies the target domain and `targetId` is the stable semantic target identity.

## Alternatives Considered

- Store the displayed label as text. This is simple but cannot update after renumbering.
- Store a block index. This breaks under document editing and clipboard operations.
- Store a broad link object now. This would overbuild citation, URL, and future link behavior before requirements exist.

## Consequences

References remain stable across document-order renumbering. Inline editing must treat the reference as an atomic node rather than mutable adjacent text.
