# ADR 0169 - Plot Edit Targets Address Properties Series And Points

## Status

Accepted

## Context

Plot editing needs stable semantic destinations for keyboard traversal, mouse hit testing, and property/data updates while the PlotBlock AST remains immutable and does not use persistent IDs.

## Decision

The first plot editor uses explicit index-based `PlotEditTarget` variants for plot properties, series, and authored data points. Targets are validated against the current immutable PlotBlock before use.

## Alternatives Considered

- Persistent IDs for series and points.
- Raw integer paths without typed target variants.
- Screen-space geometry as editing identity.

## Consequences

The editor can address authored plot structure without adding IDs prematurely. Structural edits must deterministically retarget the selection when series or points are removed.
