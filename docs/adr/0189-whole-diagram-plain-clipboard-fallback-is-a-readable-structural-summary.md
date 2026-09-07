# ADR 0189 - Whole Diagram Plain Clipboard Fallback Is A Readable Structural Summary

## Status

Accepted

## Context

The operating-system clipboard currently exposes plain text, while a diagram is a structured graph rather than a natural tabular value.

## Decision

Whole DiagramBlock copy/cut writes a deterministic human-readable summary containing the diagram title, logical canvas size, each node in authored order with its local ID and bounds, each port with local ID/side/normalized offset/label, and each connection as `element/port -> element/port` with its label. Structural whitespace inside authored strings is normalized only in this lossy fallback.

## Alternatives Considered

- Emit JSON as a public interchange contract.
- Emit Mermaid/Graphviz syntax.
- Emit only labels and omit semantic reference IDs/geometry.
- Put rendered image data on the clipboard.

## Consequences

External applications receive readable, inspectable structure without treating the text as Scholar's serialization format. Native payload transfer remains the only lossless clipboard representation in M17F.
