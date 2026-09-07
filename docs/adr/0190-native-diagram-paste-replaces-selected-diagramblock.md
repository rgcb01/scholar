# ADR 0190 - Native Diagram Paste Replaces Selected DiagramBlock

## Status

Accepted

## Context

Whole-diagram paste needs deterministic behavior when the editor currently has an atomic DiagramBlock selected, and must remain consistent with whole TableBlock and PlotBlock clipboard behavior.

## Decision

A matching `DiagramClipboardPayload` pasted over a selected DiagramBlock replaces that block in place as one global history transaction and leaves `BlockSelection` on the replacement. At ordinary editable text positions the diagram is structurally inserted using the existing block insertion/replacement rules. Other atomic block types are not replaced by native diagram paste.

## Alternatives Considered

- Always insert the copied diagram after the selected diagram.
- Replace any selected scientific block regardless of type.
- Enter DiagramEditingSelection automatically after paste.

## Consequences

Whole-diagram paste is predictable, undoable, and does not broaden generic structural replacement semantics beyond the proven type-matched pattern.
