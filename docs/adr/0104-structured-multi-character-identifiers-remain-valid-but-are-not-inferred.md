# 0104 - Structured Multi-Character Identifiers Remain Valid But Are Not Inferred

## Status

Accepted

## Context

`MathIdentifier` already supports multi-character names, and existing structured clipboard payloads or programmatic APIs may contain values such as `MathIdentifier("velocity")`. Revising default letter tokenization should not invalidate valid AST data or force normalization.

## Decision

Multi-character `MathIdentifier` values remain valid structured math content. Scholar does not infer them from ordinary keyboard letter entry or external plain-text letter runs. Native structured clipboard paste preserves multi-character identifiers exactly.

## Alternatives Considered

- Prohibit multi-character identifiers.
- Split all multi-character identifiers during paste or editing.
- Normalize adjacent single-letter identifiers into larger identifiers.

## Consequences

The AST remains backward-compatible and expressive for future explicit named quantities, APIs, and structured interchange. Default authoring remains conservative, and Scholar avoids silent normalization that could disrupt paths, selections, history, or future semantics.
