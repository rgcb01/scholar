# 0052 EquationBlock Is A Text Selection Barrier In First Slice

Status: Accepted

## Context

Equation blocks are present in the document AST but do not yet have text editing or object-selection semantics.

## Decision

For the first multi-block text-selection slice, selections may span only contiguous editable text blocks: `Paragraph` and `Heading`. `EquationBlock` is a hard barrier for keyboard extension, mouse drag, selection geometry, and clipboard extraction.

## Alternatives Considered

- Allow ranges to cross equations but reject later mutations.
- Skip equations during selection.
- Add equation/object selection now.

## Consequences

Selection behavior remains deterministic and avoids pretending equations are editable text. Future equation or block-object selection can be designed separately.
