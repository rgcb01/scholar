# 0123 - MathScript Uses Deterministic Base Subscript Superscript Traversal

## Status

Accepted

## Context

Scripts are two-dimensional visually, but the initial math editor uses horizontal navigation only.

## Decision

Logical traversal order is base, subscript if present, superscript if present, then after the script. Missing slots are skipped.

## Alternatives Considered

- Superscript before subscript.
- Authoring-order traversal.
- Geometry-dependent traversal.
- Require Up/Down navigation first.

## Consequences

Left/Right navigation stays deterministic and testable. Up/Down can be added later without redefining the structural order.
