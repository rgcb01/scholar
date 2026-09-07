# 0063 - Block Style Commands Target Every Touched Editable Block

Status: Accepted

## Context

Block style changes operate on structural blocks, not individual text characters.

## Decision

Paragraph and Heading commands apply to every editable block touched by the logical range, including partial endpoint blocks and empty editable blocks.

## Alternatives Considered

- Style only blocks containing selected characters. This would make boundary selections unable to style the participating blocks.
- Introduce a separate block-selection abstraction now. This is unnecessary for the current editor slice.

## Consequences

Multi-block style commands can convert mixed Paragraph and Heading blocks in one history transaction while preserving each block's InlineContent.
