# ADR 0168 - Plot Editing Uses Dedicated PlotEditingSelection

## Status

Accepted

## Context

`PlotBlock` began as an atomic document object. M16E needs internal plot editing without forcing document text positions or table-cell coordinates to address plot properties, series, and points.

## Decision

Scholar enters plot editing through a dedicated `PlotEditingSelection(blockIndex, target)`. Document-level navigation continues to treat `PlotBlock` atomically until the user explicitly enters plot editing, and Escape returns to the whole-block `BlockSelection`.

## Alternatives Considered

- Extend `DocumentPosition` into PlotBlock internals.
- Edit PlotBlock exclusively through detached dialogs without editor selection state.
- Introduce a generic embedded-scientific-block selection framework now.

## Consequences

Plot editing remains explicit, type-safe, and compatible with global history without polluting document text addressing. A generic embedded-editor abstraction remains deferred until more scientific blocks prove the need.
