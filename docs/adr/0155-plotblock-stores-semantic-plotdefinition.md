# ADR 0155 - PlotBlock Stores Semantic PlotDefinition

## Status

Accepted

## Context

Scholar plots must remain editable, serializable scientific document objects rather than rendered images. The document shell also needs to stay small while plot-specific semantics evolve.

## Decision

`PlotBlock` owns a non-null immutable `PlotDefinition`. The definition stores semantic plot configuration and static data, while layout and Minecraft rendering remain separate representations.

## Alternatives Considered

- Store rendered pixels or drawing commands directly in `PlotBlock`.
- Place every plot field directly on `PlotBlock`.

## Consequences

Plot semantics remain reusable by future editors/exporters and independent from rendering. Plot-specific evolution can occur inside the plot model without turning the document block into a rendering object.
