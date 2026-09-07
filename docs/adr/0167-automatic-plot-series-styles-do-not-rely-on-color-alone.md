# ADR 0167 - Automatic Plot Series Styles Do Not Rely On Color Alone

## Status

Accepted

## Context

M16D introduces multiple visible series. A color-only palette is insufficient for accessibility and produces ambiguous plots when color reproduction is poor.

## Decision

Series receive deterministic derived style slots during layout. LINE series cycle line patterns and SCATTER series cycle marker shapes; the Minecraft renderer additionally maps the same slot to a deterministic color palette. These automatic styles are not authored `PlotDefinition` data.

## Alternatives Considered

- Distinguish series by color only.
- Add authored RGB colors and full style configuration immediately.
- Introduce a generic scientific styling framework shared with future diagrams.

## Consequences

Multiple plot series have a non-color visual distinction from the first rendered slice. Full authored plot styling and generic diagram styling remain deferred until editing/product requirements justify them.
