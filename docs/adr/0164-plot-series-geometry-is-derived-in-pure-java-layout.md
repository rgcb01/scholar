# ADR 0164 - Plot Series Geometry Is Derived In Pure Java Layout

## Status

Accepted

## Context

M16C established semantic XY data, resolved ranges, ticks, and a pure-Java coordinate transform. Rendering LINE and SCATTER data directly from semantic points in the Minecraft renderer would move scientific layout decisions into the client layer.

## Decision

Scholar derives visible series geometry in pure Java before Minecraft rendering. `PlotSeriesLayoutEngine` converts semantic `PlotSeries` values into positioned points and line segments attached to `LaidOutPlot`.

## Alternatives Considered

- Transform PlotSeries data directly in `MinecraftDocumentRenderer`.
- Store screen-space geometry inside `PlotBlock`.
- Introduce a generic diagram primitive system before M17.

## Consequences

Series geometry remains deterministic, unit-testable, and Minecraft-independent. The Minecraft renderer consumes positioned plot geometry only, while generic diagram primitives remain deferred until real M17 requirements exist.
