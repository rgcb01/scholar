# 0119 - MathScript Owns Base And Optional Script Slots

## Status

Accepted

## Context

Script notation needs to represent `x^2`, `x_i`, and `x_i^2` without storing script markers as loose sibling tokens.

## Decision

`MathScript` owns its base expression and optional subscript and superscript expressions. At least one script slot must be present.

## Alternatives Considered

- Store `^` and `_` as linear operators.
- Store scripts as decorations on adjacent text.
- Introduce a generic slot container before scripts.

## Consequences

Script structure is explicit, immutable, and clipboard-preservable. Editor logic must manage slot presence and avoid constructing scripts with no slots.
