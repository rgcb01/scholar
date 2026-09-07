# ADR 148 - Whole Table Plain Clipboard Fallback Uses TSV

## Status

Accepted

## Context

The OS clipboard adapter exposes text. A whole table copied from Scholar should still be useful when pasted into spreadsheets and ordinary text tools.

## Decision

Whole-table Copy and Cut write a TSV fallback with tab-separated cells, LF-separated rows, no trailing tab, and no trailing newline.

## Alternatives Considered

- Markdown pipe tables as the default clipboard text.
- CSV as the default clipboard text.
- A Scholar-specific encoded text envelope.

## Consequences

External paste is readable and spreadsheet-friendly. Header semantics and inline formatting are intentionally lost in the fallback, while the native sidecar preserves them inside Scholar.
