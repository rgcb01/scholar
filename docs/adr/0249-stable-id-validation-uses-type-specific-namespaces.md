# ADR 0249 - Stable ID Validation Uses Type-Specific Namespaces

## Status

Accepted

## Context

Scholar has stable IDs for figures, heading/section targets, tables, equations, datasets, dataset columns, and diagram elements. Cross-references carry both target kind and ID.

## Decision

Stable ID uniqueness is enforced within the relevant namespace, not globally across the whole document. Figure IDs, heading IDs, table IDs, equation IDs, dataset IDs, dataset column IDs, and diagram element IDs each have their own scope.

## Alternatives Considered

- Require one global document-wide ID namespace. This is simpler to validate but creates needless rename pressure.
- Store display numbers as IDs. This would make reorder operations unstable.

## Consequences

Different target kinds may reuse the same textual ID safely because references are type-specific. Validation must check each namespace deliberately.
