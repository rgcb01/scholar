# ADR 153 - Markdown Table Parsing Rejects Ragged Rows

## Status

Accepted

## Context

`TableBlock` requires rectangular rows. Markdown input can contain ragged or malformed pipe rows, and silently padding or truncating them would invent data.

## Decision

Markdown table parsing rejects malformed table syntax with diagnostics rather than constructing partial tables. Separator rows and body rows must match the header column count exactly.

## Alternatives Considered

- Pad missing cells.
- Truncate extra cells.
- End the table before a ragged row and parse that row separately.

## Consequences

The parser preserves table invariants and avoids guessing user intent. Malformed input remains recoverable as paragraph text with diagnostics.
