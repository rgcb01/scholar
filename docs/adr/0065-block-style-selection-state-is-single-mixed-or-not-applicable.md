# 0065 - Block Style Selection State Is Single Mixed Or Not Applicable

Status: Accepted

## Context

The toolbar and Format menu need to represent block styles across selections that may touch several blocks.

## Decision

Block style query returns NOT_APPLICABLE, SINGLE(BlockStyle), or MIXED. MIXED is a state of the selection, not a fake BlockStyle value.

## Alternatives Considered

- Encode Mixed as a special BlockStyle. This would pollute the semantic style model.
- Have UI code traverse blocks directly. This would duplicate editor semantics in client code.

## Consequences

The toolbar can display Mixed for multi-style selections, while dropdown/menu rows only show concrete Paragraph or Heading choices.
