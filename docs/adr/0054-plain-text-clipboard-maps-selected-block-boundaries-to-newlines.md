# 0054 Plain-Text Clipboard Maps Selected Block Boundaries To Newlines

Status: Accepted

## Context

Multi-block copy needs a plain-text representation for selected Scholar structure without exposing Markdown syntax or rich clipboard data.

## Decision

Plain-text copy joins selected block text with canonical `\n` for each selected structural boundary. Headings copy as plain text, not Markdown. Empty blocks contribute empty lines through their surrounding boundaries.

## Alternatives Considered

- Serialize selected content as Markdown.
- Use platform-specific CRLF in core.
- Omit structural boundaries when no text is selected.

## Consequences

Clipboard output is simple, stable, and editor-core independent of platform conventions. Rich clipboard and multiline paste remain future work.
