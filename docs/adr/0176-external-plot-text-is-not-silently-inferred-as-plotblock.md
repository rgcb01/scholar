# ADR 0176 - External Plot Text Is Not Silently Inferred As PlotBlock

## Status

Accepted

## Context

The plain-text plot clipboard fallback resembles structured scientific data, but ordinary clipboard text may coincidentally contain the same labels or tabular sections.

## Decision

Scholar reconstructs PlotBlock only from a matching native `PlotClipboardPayload` in the process-local sidecar. Plain external text is never silently inferred as a plot. Changing the OS clipboard text invalidates the native sidecar using the existing exact-text match rule.

## Alternatives Considered

- Parse any matching plot-summary text automatically.
- Infer plots from arbitrary TSV/CSV-like content.
- Add a plot import parser as part of clipboard paste.

## Consequences

Paste remains explicit and predictable. Future CSV/TSV/import workflows can be designed as separate user-invoked interchange features.
