# ADR 0179 - Diagram Connectivity Uses Diagram-Scoped Reference IDs

## Status

Accepted

## Context

The Scholar document AST intentionally avoids persistent node IDs. Diagram connections introduce a concrete semantic cross-reference requirement: a connection must continue to identify the same element port while unrelated immutable edits reorder or replace other diagram values.

## Decision

M17 introduces scoped semantic IDs only where connectivity requires them. `DiagramElementId` is unique within one `DiagramDefinition`; `DiagramPortId` is unique within its owning element; `DiagramEndpoint` references an element ID plus port ID.

These IDs are diagram-local semantic reference identity, not global document-node identity and not a general editor identity system. Connections do not receive persistent IDs in the first slice; snapshot-local indices remain sufficient unless later requirements justify more.

ADR 0003 remains the default policy for the rest of the document model.

## Alternatives Considered

- Add persistent IDs to every Scholar AST node.
- Address connections only by element/port list indices.
- Re-find endpoint objects by value equality after immutable edits.
- Give every diagram object, including connections and labels, UUID identity immediately.

## Consequences

Connectivity survives ordinary immutable editing without globalizing identity across Scholar. Clipboard and future serialization must preserve diagram-local IDs for a whole diagram. Duplicate IDs become a model invariant violation and must be rejected.
