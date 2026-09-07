# 0087 - Math Range Replacement Is Local To One Sequence

## Status

Accepted

## Context

Milestone 12D needed deletion and typed replacement of selected math content without introducing half-structure ownership rules for fractions, scripts, roots, or groups.

## Decision

The first range replacement implementation rewrites ranges whose normalized endpoints resolve to the same virtual or real sequence. Whole child selections remove or replace complete atoms, including fractions. Partial token selections split or rewrite token content at logical text boundaries.

## Alternatives Considered

- Implement arbitrary cross-structure replacement immediately.
- Flatten math to text and rebuild it.

## Consequences

Common authoring selections work with atomic history. Cross-fraction internal ranges remain intentionally non-mutating until structural range ownership is designed.
