# 0105 - Ordinary Greek Variable Letters Are MathIdentifiers

## Status

Accepted

## Context

Scholar needs to represent conventional mathematical notation without treating every non-Latin glyph as a special symbol. Greek letters such as theta, lambda, alpha, and beta are commonly authored as variables.

## Decision

Ordinary Greek variable letters are represented as `MathIdentifier` values. Existing `MathSymbolKind.GREEK` remains for compatibility during the transitional symbol taxonomy period, but new ordinary variable authoring should prefer identifiers.

## Alternatives Considered

- Treat every Greek letter as `MathSymbol`.
- Create a separate Greek-variable node.
- Infer meaning from surrounding notation.

## Consequences

Greek variables behave consistently with Latin variables and future script attachment. A later symbol-taxonomy cleanup can narrow or deprecate `MathSymbolKind.GREEK` without blocking current authoring.
