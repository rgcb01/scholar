# ADR 0188 - Native Diagram Clipboard Preserves DiagramBlock AST

## Status

Accepted

## Context

A DiagramBlock contains semantic structure that cannot be reconstructed losslessly from readable clipboard text: logical canvas dimensions, authored node bounds, diagram-scoped element/port reference IDs, port side/offset placement, connection endpoints, labels, and authored ordering.

## Decision

Whole-diagram Scholar clipboard operations use `DiagramClipboardPayload(DiagramBlock)` as the process-local native payload. The payload stores only the immutable semantic DiagramBlock. It carries no laid-out geometry, routing result, hit-test state, drag interaction, selection, viewport, or Minecraft state.

## Alternatives Considered

- Reconstruct the diagram from the plain-text fallback during paste.
- Store routed/laid-out geometry in the payload.
- Introduce partial element clipboard or a generic graph serialization contract during M17F.

## Consequences

Scholar-to-Scholar copy/paste preserves exact diagram semantics and local reference identity while keeping presentation and editor interaction state outside the AST. Partial node/connection clipboard remains deferred.
