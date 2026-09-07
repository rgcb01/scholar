# ADR 0199 - First Electrical Wiring Reuses DiagramConnection Without Net Inference

## Status

Accepted

## Context

M17 already provides validated port-to-port connectivity, derived orthogonal routing, connection hit testing, and explicit connection authoring. Electrical schematics eventually need richer net/junction semantics, but implementing those before point-to-point component authoring is proven would be speculative.

## Decision

The first M18 electrical wiring slice uses existing `DiagramConnection` between derived electrical component terminals. Wire geometry remains derived by the M17 routing pipeline.

M18 does not infer electrical connectivity from line crossings, visual contact, or proximity. Free-space junctions, net IDs, buses, and net merging are deferred to a later evidence-driven design checkpoint.

## Alternatives Considered

- Introduce a complete electrical-net graph before rendering the first component.
- Treat any crossing/touching line as electrically connected.
- Add a second electrical wire type parallel to `DiagramConnection` immediately.

## Consequences

Electrical authoring can reuse the proven connection workflow with minimal new machinery. The first model is intentionally not a SPICE/netlist representation.
