# ADR 0157 - PlotBlock Begins As Atomic Document Selection Target

## Status

Accepted

## Context

EquationBlock and TableBlock established a stable pattern where scientific blocks participate in ordinary document navigation as atomic objects before an internal editor is introduced.

## Decision

M16B treats `PlotBlock` as an atomic `BlockSelection` target. It can be clicked, traversed with document navigation, inserted, deleted, and restored through global history. `PlotEditingSelection` is deferred.

## Alternatives Considered

- Introduce point/series editing in the first PlotBlock implementation.
- Force plot positions into `DocumentPosition`.

## Consequences

Plots integrate with the writing engine immediately without prematurely committing to an internal plot editing model.
