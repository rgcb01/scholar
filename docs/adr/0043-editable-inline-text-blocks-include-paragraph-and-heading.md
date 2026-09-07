# 0043 Editable Inline Text Blocks Include Paragraph And Heading

Status: Accepted

## Context

Once a paragraph can become a heading, the resulting block must remain editable through the same editor behavior.

## Decision

For the current editor slice, both `Paragraph` and `Heading` are editable inline text blocks. `EquationBlock` remains unsupported for text editing.

## Alternatives Considered

- Keep headings non-editable after conversion.
- Add a public AST interface for editable inline blocks.
- Duplicate paragraph editing logic for headings.

## Consequences

Editing, selection, clipboard, and inline formatting can share one core helper. The public document AST remains unchanged.
