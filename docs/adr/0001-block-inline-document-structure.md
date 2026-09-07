# ADR 0001 - Block and Inline Document Structure

## Status

Accepted

## Context

Scholar needs a document model that can represent scientific documents independently from Markdown, Minecraft text components, and rendered pixels. The initial scope only needs documents, headings, paragraphs, and text, but future milestones may add equations, tables, diagrams, rendering, editing, and interchange formats.

## Decision

The document model uses an explicit hierarchy of `Document -> BlockNode -> InlineNode`. A document contains ordered block nodes. Initial block nodes are headings and paragraphs. Blocks that contain text use ordered inline content.

## Alternatives Considered

- A flat list of generic nodes.
- A fully recursive tree where any node can contain any other node.
- Separate structures for each future feature area.

## Consequences

The model can express basic document structure clearly and avoids invalid nesting such as a paragraph inside text. Future block-level elements can be added without treating every feature as styled text. The structure adds a small amount of up-front taxonomy, but it keeps the first implementation understandable and testable.
