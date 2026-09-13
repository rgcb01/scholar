# ADR 0229: Native figure clipboard remaps stable IDs on paste

## Status
Accepted for M20.

## Context
Scholar's native clipboard sidecar preserves structured table, plot, diagram, and math payloads. Figure copy/paste must preserve content and caption while avoiding duplicate stable IDs.

## Decision
Copy selected figures using `FigureClipboardPayload(FigureBlock)` plus a readable plain-text fallback. On native figure paste, preserve figure content and caption exactly, but assign a unique stable ID when the pasted ID already exists outside the replacement target.

## Alternatives Considered
- Preserve copied IDs even when duplicates result.
- Always generate a new ID on paste.
- Serialize figure structure only through the OS clipboard text.

## Consequences
Scholar-to-Scholar paste is lossless for figure structure while maintaining document-local ID uniqueness. External applications receive readable text, not hidden structured metadata.
