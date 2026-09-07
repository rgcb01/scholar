# ADR 0019 - Typography Roles Instead Of Fonts In AST

## Status

Accepted

## Context

The document and math ASTs represent authored content and structure. Font names, resource locations, colors, and sizes are presentation details that may change by platform, resource pack, or future user preference.

## Decision

Scholar uses typography roles such as `BODY`, heading roles, and `MATH` downstream from the AST. Semantic nodes do not store concrete font or rendering choices.

## Alternatives Considered

- Add font/style data to document and math nodes.
- Let renderers infer typography directly from node classes.
- Use concrete font names throughout layout code.

## Consequences

The AST remains stable and semantic. Layout and rendering can evolve through typography profiles. The typography mapping must be maintained as a clear boundary layer.
