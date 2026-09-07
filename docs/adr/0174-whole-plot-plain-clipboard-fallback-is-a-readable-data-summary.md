# ADR 0174 - Whole Plot Plain Clipboard Fallback Is A Readable Data Summary

## Status

Accepted

## Context

The OS clipboard remains text-only. A plot does not map cleanly to a single TSV table when it can contain multiple named LINE/SCATTER series and axis/configuration metadata.

## Decision

Whole PlotBlock copy/cut writes a deterministic human-readable plain-text summary containing title, axis labels/range policy, legend/grid state, height, and one TSV-like x/y data section per series. Structural whitespace inside semantic labels is normalized only in this lossy fallback; the native payload remains exact.

## Alternatives Considered

- Use Markdown or a Scholar-specific plot language.
- Use one flattened CSV/TSV table and lose series/configuration context.
- Put binary/image data on the clipboard.

## Consequences

External applications receive readable scientific data without treating the fallback as a native serialization format. No parser is implied by this representation.
