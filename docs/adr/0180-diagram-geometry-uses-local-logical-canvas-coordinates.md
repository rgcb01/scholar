# ADR 0180 - Diagram Geometry Uses Local Logical Canvas Coordinates

## Status

Accepted

## Context

Diagram element positions must survive document resize, GUI scale changes, and rendering on different surfaces. Storing Minecraft/document pixels in semantic nodes would make presentation geometry the source of truth and make drag behavior dependent on current screen size.

## Decision

`DiagramDefinition` owns a finite positive logical canvas. Element bounds are authored in local logical coordinates with a top-left origin, X increasing rightward, and Y increasing downward.

Document layout uniformly scales the logical canvas into the available content width while preserving aspect ratio. Layout owns the forward logical-to-document transform and provides the inverse mapping required by editing interactions.

## Alternatives Considered

- Store current document pixels directly in the AST.
- Store all positions normalized to `[0,1]` without an explicit logical canvas.
- Use scientific Y-up coordinates because PlotBlock does.
- Stretch X and Y independently to fill every available rectangle.

## Consequences

Semantic positions remain stable across responsive reflow and can use intuitive drawing-surface coordinates. Diagram and plot coordinate conventions intentionally differ because they represent different semantic spaces. Dragging must map pointer positions back through the layout transform before committing edits.
