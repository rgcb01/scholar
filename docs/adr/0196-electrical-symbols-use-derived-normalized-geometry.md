# ADR 0196 - Electrical Symbols Use Derived Normalized Geometry

## Status

Accepted

## Context

Scholar must preserve electrical meaning as editable document data. Storing resistor zig-zags, circles, pixels, or Minecraft draw calls in the AST would make presentation the source of truth and complicate responsive layout.

## Decision

`ElectricalComponent` stores semantic kind/orientation/annotations and logical bounds. A pure-Java symbol library derives normalized schematic geometry and maps it through the existing diagram coordinate transform into laid-out electrical geometry.

The internal symbol primitive vocabulary stays deliberately small and presentation-only; it is not a general SVG language or public document format.

## Alternatives Considered

- Store symbol drawing primitives directly in the document AST.
- Use raster textures as the semantic component representation.
- Hard-code all electrical drawing only in the Minecraft renderer.

## Consequences

Symbols remain responsive, testable, and renderer-independent. The visual standard can evolve without rewriting authored electrical meaning.
