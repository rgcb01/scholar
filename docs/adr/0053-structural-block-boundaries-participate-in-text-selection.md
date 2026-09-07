# 0053 Structural Block Boundaries Participate In Text Selection

Status: Accepted

## Context

Scholar block boundaries are not represented as newline `Text` nodes, but users still need to select and copy boundaries between text blocks.

## Decision

A range from the end of one editable text block to the start of the next is non-empty because it selects the structural boundary. Boundary-only selections receive visible geometry and copy as a newline.

## Alternatives Considered

- Treat boundary-only ranges as empty.
- Insert synthetic newline text into the AST.
- Show no geometry for structural-only selections.

## Consequences

The AST remains clean while text selection can represent document structure. Clipboard and geometry helpers must distinguish selected structural boundaries from selected text characters.
