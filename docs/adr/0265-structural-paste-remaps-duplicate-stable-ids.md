# ADR 0265 - Structural Paste Remaps Duplicate Stable IDs

## Status

Accepted

## Context

Native clipboard payloads preserve semantic block structure, including stable IDs. Pasting a copied target next to its source can otherwise introduce duplicate IDs in namespaces validated by M24A.

## Decision

Structural paste remaps duplicate stable IDs for supported ID-bearing payloads: headings, equations, tables, figures, and datasets. Replacement may keep an ID when replacing the same selected target where that does not create a duplicate.

## Alternatives Considered

- Preserve copied IDs exactly. This breaks document validation on duplicate paste.
- Strip all IDs on paste. This would unnecessarily destroy target identity even when no conflict exists.

## Consequences

Native paste preserves identity when safe and creates deterministic new IDs when needed. Cross-references are not rewritten during paste in M24D.
