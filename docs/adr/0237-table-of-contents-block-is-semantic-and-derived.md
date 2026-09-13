# ADR 0237: Table Of Contents Block Is Semantic And Derived

## Status

Accepted

## Context

Scholar needs a table of contents that follows document edits without manual refresh. Persisting generated entries would make the TOC stale.

## Decision

Represent the table of contents as an empty semantic `TableOfContentsBlock`. Layout, rendering, navigation hit regions, and plain-text output derive entries from the current `DocumentStructure`.

## Alternatives Considered

- Store TOC entries inside the block. This requires refresh logic and stale-entry handling.
- Generate TOC as ordinary paragraphs. This loses semantic navigation and update behavior.
- Defer TOC until pagination exists. M22 needs navigation now, and page numbers remain out of scope.

## Consequences

TOC entries update automatically after heading edits and reordering. The block is atomic document structure; it does not own editable text in M22.
