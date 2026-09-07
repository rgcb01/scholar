# ADR 0026: User Character Boundary Semantics

## Status

Accepted

## Context

Scholar documents may contain Greek, scientific symbols, accented text, and later broader international text. Raw UTF-16 offsets can split surrogate pairs or combining sequences, while full Unicode grapheme segmentation would add dependency and scope.

## Decision

Define editor `characterOffset` as a boundary between user-editable characters, not as UTF-16 code units. The Milestone 5B implementation uses Java's standard `BreakIterator.getCharacterInstance(Locale.ROOT)` behind an editor utility.

This implementation can evolve toward more complete Unicode grapheme segmentation without changing the Document AST or public position concept.

## Alternatives Considered

- Publicly define offsets as UTF-16 indices.
- Publicly define offsets as raw Unicode code-point indices.
- Add a heavyweight Unicode segmentation dependency immediately.

## Consequences

Caret movement and deletion avoid intentionally splitting visible composed characters in the first slice. Java's standard boundary iterator may not match every modern extended grapheme cluster, so the utility boundary remains isolated for future improvement.
