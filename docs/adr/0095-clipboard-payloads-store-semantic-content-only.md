# 0095 - Clipboard Payloads Store Semantic Content Only

## Status

Accepted

## Context

Editor paths, selections, layout rectangles, and history state are snapshot-local details rather than copied mathematical content.

## Decision

Structured clipboard payloads store semantic content only. Math payloads store a `MathSequence` fragment and no `MathPath`, block index, selection direction, geometry, IDs, screen state, or history state.

## Alternatives Considered

- Preserve source paths in clipboard payloads.
- Preserve selection direction.
- Store layout geometry to support visual paste.

## Consequences

Pasted content is independent of its source document location and remains safe across document edits within the same process.
