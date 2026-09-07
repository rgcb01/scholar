# 0083 - Structural Math Insertion Returns The Exact Inserted Path Rather Than Searching By Equality

Status: Accepted

## Context

Multiple identical empty fractions may exist in the same expression. After insertion, the caret must enter the specific new fraction.

## Decision

Fraction insertion computes the inserted structural path from the insertion position and returns the numerator caret directly in `MathEditResult`.

## Alternatives Considered

- Search the rewritten AST for the inserted fraction by equality. This is ambiguous when identical fractions exist.
- Add persistent node IDs. This would conflict with the current identity strategy.

## Consequences

The resulting caret is deterministic and does not require persistent IDs. Insertion logic must carefully remap paths when virtual sequences materialize.
