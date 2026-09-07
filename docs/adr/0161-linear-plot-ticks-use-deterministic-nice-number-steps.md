# ADR 0161 - Linear Plot Ticks Use Deterministic Nice Number Steps

## Status

Accepted

## Context

Automatically generated tick marks must remain readable and deterministic without exposing floating-point noise.

## Decision

Scholar generates linear-axis ticks in pure Java using 1, 2, and 5 multiplied by powers of ten. Tick labels are deterministically formatted from the selected step, with bounded scientific notation for extreme magnitudes.

## Alternatives Considered

- Even pixel subdivisions with arbitrary numeric values.
- Tick generation in the Minecraft renderer.
- Store generated ticks in PlotBlock AST.

## Consequences

Ticks are reproducible, testable outside Minecraft, and remain presentation/layout output rather than authored semantic state.
