# ADR 0247 - Document Validation Is Pure And Non-Mutating

## Status

Accepted

## Context

Scholar now has many semantic block types and document-owned resources. Validation needs to answer whether a document is structurally safe without changing the document, resolving repairs, launching Minecraft, or depending on layout/rendering.

## Decision

`DocumentValidator` is a pure Java read-only validator. It returns immutable diagnostics and never mutates, normalizes, repairs, or rewrites the `Document`.

## Alternatives Considered

- Repair while validating. This would hide authoring/import defects and complicate undo/history.
- Put validation inside renderers. This would mix trust-boundary checks with presentation.

## Consequences

Validation can be run at import/load/save boundaries and in tests without side effects. Any future repair tool must be explicit and separately documented.
