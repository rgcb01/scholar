# 0046 Enter Produces Structural Blocks Not Inline Newlines

Status: Accepted

## Context

Scholar documents model paragraphs and headings as block nodes. Pressing Enter in an editor could either insert a newline character inside text or transform the block list.

## Decision

Enter is a structural editing operation. It splits or transforms editable text blocks in the immutable Document AST and never inserts `\n` into `Text`.

## Alternatives Considered

- Store newline characters inside `Text`.
- Add soft line break inline nodes now.
- Rebuild edited content through Markdown parsing.

## Consequences

The AST stays aligned with document structure and Markdown serialization remains straightforward. Soft line breaks and richer block semantics remain future work.
