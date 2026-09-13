# ADR 0239: Section References Use The Shared Structure Resolver

## Status

Accepted

## Context

M21 section references originally produced flat labels such as `Section 1`. M22 introduces hierarchical section numbering, so references, TOC, and heading rendering need one source of truth.

## Decision

Resolve `CrossReferenceTargetKind.SECTION` labels through `DocumentStructureResolver`, producing labels such as `Section 2.1`.

## Alternatives Considered

- Keep section references flat. This becomes inconsistent with heading rendering and TOC.
- Recompute section labels inside the reference resolver independently. This duplicates numbering logic.

## Consequences

Section cross-references stay synchronized with heading rendering and TOC. Reordering headings changes labels without changing stored reference nodes.
