# 0099 - Plain-Text Math Import Does Not Create Structural Math Nodes

## Status

Accepted

## Context

Structural math nodes such as fractions, scripts, roots, and groups carry editor behavior that cannot be safely inferred from plain text in the first importer slice.

## Decision

External plain-text math import creates only `MathSequence`, `MathNumber`, `MathIdentifier`, `MathOperator`, and `MathSymbol`.

## Alternatives Considered

- Infer `MathFraction`, `MathGroup`, scripts, or roots from common text syntax.
- Create partially supported structural nodes and rely on later editor work.

## Consequences

Paste is conservative and render-safe. Rich structural paste remains the responsibility of Scholar-native clipboard payloads and explicit authoring tools.
