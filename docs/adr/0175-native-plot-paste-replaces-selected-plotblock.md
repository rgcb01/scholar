# ADR 0175 - Native Plot Paste Replaces Selected PlotBlock

## Status

Accepted

## Context

When a whole PlotBlock is selected, pasting another native plot needs deterministic structural behavior consistent with whole-table replacement semantics.

## Decision

A matching `PlotClipboardPayload` pasted over a selected PlotBlock replaces that block in place as one global history transaction and leaves `BlockSelection` on the replacement. Other atomic block selections are not replaced by native plot paste in this slice.

## Alternatives Considered

- Insert the new plot after the selected plot.
- Replace any selected scientific block regardless of type.
- Enter plot editing automatically after paste.

## Consequences

Whole-plot replacement is predictable and undoable without broadening generic block-replacement semantics.
