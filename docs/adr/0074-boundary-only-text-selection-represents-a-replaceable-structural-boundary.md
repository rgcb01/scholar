# 0074 - Boundary-Only Text Selection Represents A Replaceable Structural Boundary

Status: Accepted

## Context

Scholar text selections can include structural boundaries between adjacent editable text blocks. A boundary-only selection contains no characters but still represents an intentional selected boundary.

## Decision

Replacing a boundary-only `TextSelection` with a structural block inserts the block between the two adjacent editable text blocks and leaves both blocks' content and styles unchanged.

## Alternatives Considered

- Treat boundary-only replacement as not applicable because it selects zero characters. That would conflict with boundary-aware structural editing.
- Route boundary-only replacement through text deletion. This risks deleting or converting adjacent blocks unnecessarily.

## Consequences

Users can place equations between existing text blocks by selecting the boundary, while inline formatting can still treat boundary-only selections as not applicable.
