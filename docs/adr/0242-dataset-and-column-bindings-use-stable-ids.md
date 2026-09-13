# ADR 0242: Dataset And Column Bindings Use Stable IDs

## Status

Accepted

## Context

Dataset and column display names are user-editable. Table and plot bindings must survive renaming.

## Decision

Dataset-backed views reference datasets and columns by stable string IDs. Display names are presentation labels and are not used as binding identity.

## Alternatives Considered

- Bind by display name. Renaming would break views.
- Bind by current list index. Reordering or column deletion would make bindings ambiguous.
- Add universal persistent node IDs. M23 only needs IDs for datasets and columns.

## Consequences

Renaming datasets and columns does not break bound views. Duplicate IDs must be rejected or remapped at document/clipboard boundaries.
