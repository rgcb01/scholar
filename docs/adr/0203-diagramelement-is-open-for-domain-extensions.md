# ADR 0203: DiagramElement Is Open For Domain Extensions

## Status

Accepted

## Context

M17 introduced `DiagramElement` as a sealed interface with only `DiagramNode` permitted because no domain-specific element existed yet. M18 is the first concrete evidence that the diagram foundation must host semantic element families outside the generic diagram package.

Adding every electrical, mechanical, and future scientific element class to a central `permits` list would make the generic diagram model depend directly on all domain packages and turn the sealed hierarchy into an extension bottleneck.

## Decision

`DiagramElement` is now an ordinary interface. Domain elements such as `ElectricalComponent` implement the same small semantic contract: stable element id, logical bounds, and semantic ports.

`DiagramDefinition` continues to enforce diagram-scoped id uniqueness, canvas bounds, and endpoint validity independently of the concrete element family.

## Consequences

- Electrical components can live inside the existing `DiagramBlock(DiagramDefinition)` without a parallel block model.
- Future M19 mechanical elements can implement the same contract without modifying a sealed permits list.
- Concrete layout/render support remains explicit; opening the semantic interface does not imply a public plugin API or automatic rendering of arbitrary third-party element classes.
- Unknown element implementations are still rejected by layout until a deliberate presentation path exists.
