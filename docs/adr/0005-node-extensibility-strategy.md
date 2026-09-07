# ADR 0005 - Node Extensibility Strategy

## Status

Accepted

## Context

Scholar should eventually be extensible by other modules, mods, and disciplines. Future document elements may include equations, tables, plots, diagrams, or custom educational elements. Java sealed hierarchies would provide exhaustiveness checks but could conflict with third-party extension.

## Decision

The initial node categories use normal public interfaces rather than sealed interfaces. Built-in model values may use Java records where they naturally represent immutable value data.

## Alternatives Considered

- Sealed interfaces for all node categories.
- Abstract base classes.
- Fully generic untyped extension maps.
- Deferring category interfaces entirely.

## Consequences

The model preserves a path toward external node implementations. Internal consumers cannot rely on sealed exhaustiveness and must handle unknown node implementations carefully in future APIs. This is an acceptable tradeoff for a reusable document infrastructure project.
