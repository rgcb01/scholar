# ADR 0015 - MathBox Layout Representation

## Status

Accepted

## Context

Scholar needs to render structured math such as stacked fractions without implementing TeX's complete box, glue, and penalty system. The layout result must still be rich enough for a renderer to draw glyphs and fraction rules.

## Decision

Math layout produces a small immutable `MathBox` tree. A box contains dimensions, positioned child boxes, and minimal drawable primitives. Milestone 4B includes glyph primitives and horizontal rule primitives.

## Alternatives Considered

- Create a specialized laid-out type for every math AST node.
- Flatten all math layout directly into document lines.
- Reproduce a full TeX-style layout engine.

## Consequences

Nested structures such as fractions compose naturally, and renderers can remain simple. The box tree is intentionally small and may grow only when new visual structures require new primitive capabilities.
