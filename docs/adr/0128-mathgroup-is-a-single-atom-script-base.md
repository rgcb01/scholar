# 0128 - MathGroup Is A Single Atom Script Base

## Status

Accepted

## Context

Scripts currently attach to one complete base atom. Users need `(x + 1)^2` without introducing raw multi-atom script bases.

## Decision

`MathGroup` is eligible as a single script base. Multi-atom script bases remain rejected unless the user first creates an explicit group.

## Alternatives Considered

- Auto-group multi-atom bases when the user requests a script.
- Allow `MathScript` to own arbitrary sibling ranges.
- Keep grouped expressions ineligible for scripts until a later milestone.

## Consequences

Scripts remain simple and structurally local. Users can author conventional grouped powers explicitly. Future stretch delimiter layout can improve presentation without changing script semantics.
