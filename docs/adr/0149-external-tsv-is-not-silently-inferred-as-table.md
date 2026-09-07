# ADR 149 - External TSV Is Not Silently Inferred As Table

## Status

Accepted

## Context

Plain text containing tabs or newlines may come from many sources. Automatically turning it into a `TableBlock` would infer document structure from external text.

## Decision

Scholar does not infer a table from external TSV through normal Paste. Native table paste requires a matching `TableClipboardPayload`; otherwise Paste follows the current plain-text behavior for the active context.

## Alternatives Considered

- Infer rectangular TSV as a table during document paste.
- Paste rectangular TSV into a table starting at the active cell.
- Add a TSV parser to normal clipboard paste.

## Consequences

Normal paste remains predictable and avoids accidental structural mutations. Explicit TSV import or Paste as Table can be designed later.
