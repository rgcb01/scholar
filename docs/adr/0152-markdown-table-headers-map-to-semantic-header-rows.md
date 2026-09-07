# ADR 152 - Markdown Table Headers Map To Semantic Header Rows

## Status

Accepted

## Context

`TableBlock` stores header semantics as `headerRowCount`, while authored Bold and Italic marks are independent inline content.

## Decision

A valid Markdown pipe table always parses to `TableBlock(headerRowCount = 1)`. The first row is semantic header content, and the separator row is syntax only. Header cells are not automatically authored as Bold.

## Alternatives Considered

- Convert Markdown header cells into Bold inline content.
- Store the separator row in the document model.
- Treat Markdown tables as headerless by default.

## Consequences

Semantic table presentation and authored formatting remain separate. Markdown round trips can preserve a header row without corrupting user-authored marks.
