# 0086 - Collapsed Math Ranges Canonicalize To Carets

## Status

Accepted

## Context

A range whose endpoints are equal is semantically identical to a caret and would create duplicate collapsed-selection states.

## Decision

`MathRangeSelection` rejects equal endpoints. Operations that extend a range back to its anchor return `MathCaretSelection`.

## Alternatives Considered

- Permit zero-length ranges.
- Add a collapsed flag to range selections.

## Consequences

Editor code can distinguish active selections from carets by type, keeping command applicability and rendering simpler.
