# ADR 0192: Responsive diagram geometry remains bounded under narrow layout

## Status
Accepted

## Context

Diagram semantics use stable logical canvas coordinates while document layout may become extremely narrow because of window size, GUI scale, or future embedding constraints. Integer rounding can collapse a logically valid node to zero pixels or move its derived rectangle one pixel beyond the laid-out canvas. Edge-facing node and port labels can also be positioned outside the diagram surface even when the label would fit inside it.

## Decision

`DiagramLayoutEngine` clamps only **derived document-space geometry**:

- laid-out node rectangles remain inside the laid-out canvas and retain at least a one-pixel visible extent;
- node and port labels are clamped inside the canvas when their measured width/height can fit there;
- semantic `DiagramBounds` and port placement remain unchanged.

Responsive reflow is therefore presentation-only and never rewrites authored logical coordinates.

## Consequences

Very narrow layouts remain deterministic and usable instead of producing off-canvas one-pixel artifacts. Labels too large to fit the canvas are not silently truncated or rewritten; richer overflow/wrapping policy remains deferred.
