# 0061 - Multi-Block Inline Formatting Queries Selected Characters Only

Status: Accepted

## Context

Scholar selections can span editable block boundaries, but inline formatting applies only to text characters.

## Decision

Bold and italic queries inspect only selected text characters inside the normalized logical range. Structural block boundaries and empty blocks contribute zero inline characters.

## Alternatives Considered

- Treat block boundaries as unformatted text. This would make boundary-only selections report OFF even though no character is selected.
- Query rendered glyph geometry. This would couple editor semantics to layout and wrapping.

## Consequences

Boundary-only selections are not applicable for inline formatting. Empty blocks do not affect ON, OFF, or MIXED unless the whole range contains no selected characters.
