# 0049 Basic Caret Navigation Across Editable Block Boundaries

Status: Accepted

## Context

Scholar's editor positions are block-local, and selection ranges remain single-block for the current slice. Plain caret motion still needs to move naturally between adjacent editable blocks.

## Decision

Plain Left at the start of an editable text block moves to the end of the immediately previous editable text block. Plain Right at the end moves to the start of the immediately next editable text block. Shift selection remains clamped within the current block.

## Alternatives Considered

- Keep plain navigation trapped inside one block.
- Skip over non-editable blocks.
- Implement cross-block selection now.

## Consequences

Basic navigation feels continuous without changing the single-block selection model. Equation and other unsupported blocks remain structural barriers until their editing semantics are designed.
