# ADR 0017 - Display Equation Alignment Default

## Status

Accepted

## Context

Display equations need a default horizontal position inside the Scholar document surface. Adding alignment configuration before there are authoring controls would expand the semantic model unnecessarily.

## Decision

Display equations are centered by document layout when they fit within the available content width. `EquationBlock` does not store an alignment property.

## Alternatives Considered

- Left-align all display equations.
- Add alignment options to `EquationBlock`.
- Let the Minecraft renderer decide display alignment.

## Consequences

The first visual equation reads like conventional educational material while the document model remains small. Oversized equations are not solved in Milestone 4B; they keep their natural width and rely on existing clipping as a temporary limitation.
