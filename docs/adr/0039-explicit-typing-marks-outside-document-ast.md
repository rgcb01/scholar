# 0039 Explicit Typing Marks Outside Document AST

Status: Accepted

## Context

When the caret is collapsed, Bold or Italic has no selected range to mutate. Scholar still needs future typed text to carry the user's requested semantic marks.

## Decision

Store explicit typing marks in editor state as `Optional<Set<TextMark>>`. The document AST remains unchanged until text is inserted.

## Alternatives Considered

- Insert empty marked text nodes into the document.
- Store typing marks only in the Minecraft screen/controller.
- Always infer formatting from adjacent text and ignore explicit caret toggles.

## Consequences

The document remains semantic content only. Typing marks are Minecraft-independent, testable, and undo/history-aware without introducing empty AST artifacts.
