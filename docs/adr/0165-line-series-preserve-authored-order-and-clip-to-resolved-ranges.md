# ADR 0165 - Line Series Preserve Authored Order And Clip To Resolved Ranges

## Status

Accepted

## Context

XY line data may be unsorted, contain duplicate X values, or extend beyond an explicitly authored axis range. Sorting would change authored meaning, while drawing unbounded geometry could escape the plot area.

## Decision

LINE series connect consecutive points strictly in authored list order. Visible segments are clipped against the resolved X/Y range before becoming document-space geometry. Fully outside segments are omitted; crossing segments are retained only for their visible portion.

## Alternatives Considered

- Sort points by X before rendering.
- Clamp every point independently to the plot boundary.
- Rely only on Minecraft scissoring to hide out-of-range lines.

## Consequences

Authored topology is preserved and explicit axis ranges behave as real viewports. Clipping is testable independently of Minecraft and does not alter semantic `PlotSeries` data.
