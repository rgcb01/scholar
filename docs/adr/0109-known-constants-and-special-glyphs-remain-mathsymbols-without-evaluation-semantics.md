# 0109 - Known Constants And Special Glyphs Remain MathSymbols Without Evaluation Semantics

## Status

Accepted

## Context

Symbols such as pi and infinity are known mathematical glyphs, but Scholar's AST represents authored notation rather than evaluated mathematical values. Adding a constant node now would imply more semantic machinery than the editor currently needs.

## Decision

Known constants and special glyphs remain representable as `MathSymbol` values, such as `MathSymbol("π", CONSTANT)` and `MathSymbol("∞", INFINITY)`. Scholar does not add `MathConstant` in this slice.

## Alternatives Considered

- Add `MathConstant`.
- Treat all known constants as identifiers.
- Attach evaluated values to symbols.

## Consequences

Scholar can preserve conventional glyphs without evaluation, constant folding, or CAS behavior. More specialized constant semantics can be designed later if a real requirement appears.
