# ADR 0177 - Plot Tick Density Is Layout Responsive

## Status

Accepted

## Context

M16C originally generated the same target number of linear ticks regardless of the available plot-area geometry. This is readable at normal document widths, but narrow responsive plots can place long numeric tick labels on top of one another. Plot tick placement is already a pure-Java layout responsibility.

## Decision

Scholar derives the requested X/Y tick count from the available plot-area dimensions, bounded by the existing deterministic nice-tick maximum. After tick labels are positioned, layout deterministically removes labels/ticks whose rendered label bounds would overlap an already-kept tick label.

Tick density remains a presentation/layout decision. It is not stored in `PlotDefinition`, and it does not change semantic axis ranges or authored data.

## Alternatives Considered

- Keep a fixed tick target and allow labels to overlap on narrow plots.
- Store a user-authored tick count in `PlotDefinition`.
- Move collision handling into the Minecraft renderer.
- Introduce an advanced axis/tick-style model during M16 hardening.

## Consequences

Responsive plots remain readable across narrower document widths without adding authored presentation state or Minecraft dependencies to plot semantics. Extremely constrained plots may show fewer tick/grid positions, while normal-width plots retain the familiar nice-number tick density.
