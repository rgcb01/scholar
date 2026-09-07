# 0042 Block Style Is Semantic Block Type

Status: Accepted

## Context

Scholar now supports changing a text block between paragraph and heading presentation. This must remain semantic document structure rather than visual styling.

## Decision

Paragraph and heading styles are represented as block node semantics: `Paragraph(InlineContent)` and `Heading(level, InlineContent)`. Inline marks such as Bold and Italic remain separate `TextMark` semantics.

## Alternatives Considered

- Represent heading level as a `TextMark`.
- Store visual font size or styling properties in the document AST.
- Keep headings read-only and represent editor heading choices outside the AST.

## Consequences

Markdown heading serialization remains natural. Block style commands transform whole block nodes while preserving inline content.
