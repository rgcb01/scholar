# ADR 0213 — Net Labels Annotate Explicit Connected Nets Without Merging By Name

## Status
Accepted for M18E.

## Decision
A junction may carry an optional net label such as `VOUT`. The label names the already-connected net derived from graph connectivity.

Equal labels on disconnected nets do not silently connect them. Conflicting non-empty junction labels inside one connected net are rejected by the resolver.

## Consequences
- Naming never becomes hidden topology inference.
- Explicit wires/junctions remain the source of electrical connectivity in M18E.
- A future explicit global-net-label feature can be designed separately if needed.
