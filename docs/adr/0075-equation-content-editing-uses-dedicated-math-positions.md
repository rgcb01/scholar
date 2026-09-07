# 0075 - Equation Content Editing Uses Dedicated Math Positions

Status: Accepted

## Context

Document text positions are block and character based. Equation content has nested math structure, so reusing `DocumentPosition` would either flatten the math AST too early or make document editing APIs aware of math internals.

## Decision

Equation content editing uses dedicated math positions under `dev.rgcb.scholar.math.editor`. Text document positions remain separate from math positions.

## Alternatives Considered

- Reuse `DocumentPosition` with encoded math offsets. This would blur document and math editing boundaries.
- Use rendered glyph positions as editor positions. This would couple editing semantics to layout.

## Consequences

Math editing can evolve around math structure without changing the document selection model. Bridges are still needed at the editor session and rendering boundary.
