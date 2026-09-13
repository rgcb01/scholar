# ADR 0231: Cross Reference Labels Are Derived From Current Document

## Status

Accepted

## Context

Figures, tables, equations, and sections need readable labels, but their numbers are presentation derived from current document order.

## Decision

Resolve `CrossReference` display text from the current `Document` with `CrossReferenceResolver`. The initial labels are `Figure N`, `Table N`, `Equation N`, and `Section N`.

## Alternatives Considered

- Persist numbers on targets. This would duplicate derived state and require synchronization.
- Resolve in Minecraft rendering only. This would make UI code responsible for semantic document behavior.
- Use heading level numbering. Full section numbering is deferred.

## Consequences

References update automatically after insert/delete/reorder operations. Core Java code owns the semantic resolution and client rendering stays thin.
