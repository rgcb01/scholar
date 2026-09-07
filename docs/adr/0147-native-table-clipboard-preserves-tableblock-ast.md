# ADR 147 - Native Table Clipboard Preserves TableBlock AST

## Status

Accepted

## Context

Whole-table clipboard operations need to preserve table structure that plain text cannot represent, including header metadata, empty cells, authored inline marks, Unicode, and cell segmentation.

## Decision

Whole selected tables use a native `TableClipboardPayload` containing the copied `TableBlock` AST through the existing `ScholarClipboardPayload` sidecar mechanism.

## Alternatives Considered

- Reconstruct tables from TSV on paste.
- Store layout or editor selection state in the clipboard payload.
- Wait for a generic mixed-document fragment clipboard.

## Consequences

Scholar-to-Scholar table paste in the same client process is lossless. The payload remains semantic content only and contains no Minecraft UI, layout, caret, or viewport state.
