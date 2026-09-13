# ADR 0248 - Document Validation Separates Structural Errors From Degraded Warnings

## Status

Accepted

## Context

Some Scholar states are broken but still meaningful, such as unresolved cross-references or dataset-backed views whose source dataset was removed. Other states are structurally unsafe, such as duplicate IDs within a namespace.

## Decision

Validation diagnostics distinguish `ERROR` from `WARNING`. Errors mean the document violates a structural invariant. Warnings mean the document remains valid but may render or export a degraded placeholder.

## Alternatives Considered

- Treat every broken reference as invalid. This would make normal edit/delete workflows too brittle.
- Use only warnings. This would fail to protect stable identity and structure invariants.

## Consequences

Editors and importers can preserve degraded documents while still blocking truly invalid persisted states. UI can later surface warnings without forcing immediate repair.
