# 0029 Selection Geometry Outside AST

Status: Accepted

## Context

Rendering selected text requires concrete rectangles, but those rectangles depend on layout, wrapping, typography, and measurement.

## Decision

Introduce `SelectionGeometryResolver` to derive `SelectionRect` values from a normalized `DocumentRange`, `LaidOutDocument`, and `TextMeasurer`. Selection rectangles are not stored in the document AST.

## Alternatives Considered

- Store selection rectangles in `EditorState`: rejected because geometry becomes stale after layout changes.
- Store geometry on document nodes: rejected because the semantic document model must stay renderer independent.
- Let Minecraft compute selection rectangles directly: rejected because geometry should remain testable outside Minecraft.

## Consequences

Selection rendering is deterministic and layout-driven. Wrapped and cross-inline selections can be tested with ordinary JUnit tests.
