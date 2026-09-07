# ADR 0217 — Transient Diagram Viewports Follow Diagrams Across Block-Index Shifts

## Status
Accepted for M18G.

## Decision
Per-diagram zoom/pan remains transient presentation state under ADR 0214, but it is no longer stored as an unqualified raw map from snapshot-local document block index to `DiagramViewport`.

A pure-Java `DiagramViewportStore` reconciles transient cameras across successive committed immutable `Document` snapshots. Unchanged `DiagramBlock` object identity is followed across insertions/deletions that shift block indices. If the diagram itself is semantically replaced in the same slot by an immutable editor operation, the existing camera remains attached to that slot. Deleted diagram cameras are discarded instead of being inherited by a neighboring diagram that shifted into the same numeric index.

Whole-diagram clipboard can intentionally reuse one immutable `DiagramBlock` instance in more than one document position. The registry therefore does not key by record equality or persistent diagram IDs. Ambiguous new duplicates start at Fit rather than sharing transient mutable camera state.

Reconciliation occurs only for committed semantic documents. Drag-preview documents stay outside this mechanism because previews are transient and must not become the reference snapshot for later document-index reconciliation.

## Consequences
- Inserting or deleting text/scientific blocks before a zoomed diagram no longer loses or misassigns its camera merely because its block index changed.
- Deleting a zoomed diagram cannot leak its viewport onto a different diagram that shifts into the vacated slot.
- Semantic edits such as rotate, drag commit, canvas resize, annotation editing, undo, and redo can retain the current diagram camera.
- New/ambiguous whole-diagram duplicates use deterministic Fit state instead of accidentally sharing another diagram's camera.
- No viewport identifiers or camera fields are added to the authored AST, clipboard payload, Markdown/plain text, or global history.
