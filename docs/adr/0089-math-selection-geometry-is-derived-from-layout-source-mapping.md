# 0089 - Math Selection Geometry Is Derived From Layout Source Mapping

## Status

Accepted

## Context

Math selection highlighting must reflect logical math positions but render through the Minecraft screen.

## Decision

Math selection geometry is resolved outside the AST from `MathSelection`, `LaidOutMath`, and layout source paths. Token highlights use measured substrings; whole child highlights use the laid-out child bounds, including fraction bars.

## Alternatives Considered

- Store highlight rectangles in math nodes.
- Let the Minecraft screen infer math structure directly from rendered glyphs.

## Consequences

The document and math AST remain rendering-independent, and Minecraft rendering only consumes geometry produced at the editor/layout boundary.
