# 0032 Plain-Text Clipboard First Slice

Status: Accepted

## Context

Scholar needs copy, cut, and paste before rich editing, Markdown clipboard, or custom clipboard formats exist.

## Decision

Implement clipboard transformations as Minecraft-independent plain-text operations over the current single-paragraph selection scope. The Minecraft client layer reads and writes the operating-system clipboard, while the editor core extracts selected text, cuts ranges, and pastes provided strings.

## Alternatives Considered

- Preserve `TextMark`s in the clipboard: deferred until rich clipboard semantics are designed.
- Use Markdown or JSON clipboard formats: rejected for this first slice because clipboard interop should remain simple.
- Put clipboard access in editor core: rejected because system clipboard APIs belong at the client boundary.

## Consequences

Clipboard behavior is testable without Minecraft and interoperates with normal text sources. Formatting of pasted text follows existing insertion affinity rather than clipboard metadata.
