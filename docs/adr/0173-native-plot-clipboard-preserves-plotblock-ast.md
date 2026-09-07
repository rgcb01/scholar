# ADR 0173 - Native Plot Clipboard Preserves PlotBlock AST

## Status

Accepted

## Context

A PlotBlock contains semantic data that cannot be reconstructed losslessly from a plain-text clipboard fallback, including explicit axis ranges, series kinds, legend/grid intent, configured height, authored point order, and future plot semantics.

## Decision

Whole-plot Scholar clipboard operations use `PlotClipboardPayload(PlotBlock)` as the process-local native payload. The payload stores the immutable semantic PlotBlock only and carries no layout, selection, viewport, or Minecraft state.

## Alternatives Considered

- Reconstruct plots from plain text during paste.
- Store rendered plot geometry in the clipboard payload.
- Introduce a generic mixed-document clipboard fragment before one is needed.

## Consequences

Scholar-to-Scholar copy/paste preserves the exact plot AST while the operating-system clipboard can still expose an interoperable plain-text representation. Mixed block fragments remain deferred.
