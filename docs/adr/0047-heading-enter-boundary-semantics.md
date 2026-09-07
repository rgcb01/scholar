# 0047 Heading Enter Boundary Semantics

Status: Accepted

## Context

Headings are editable text blocks, but pressing Enter at heading boundaries has different authoring intent than splitting body paragraphs.

## Decision

Enter in the middle of a non-empty heading splits it into two headings with the same level. Enter at the end creates a following empty paragraph. Enter at the start creates an empty paragraph before the heading and places the caret there. Enter in an empty heading replaces it with one empty paragraph.

## Alternatives Considered

- Always split headings into headings.
- Always create a paragraph after a heading.
- Leave empty headings unchanged.

## Consequences

Heading editing matches common authoring expectations while avoiding chains of empty headings. The behavior is intentionally block-style specific and remains separate from inline marks.
