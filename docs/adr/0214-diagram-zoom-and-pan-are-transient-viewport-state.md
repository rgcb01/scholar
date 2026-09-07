# ADR 0214 — Diagram Zoom And Pan Are Transient Viewport State

## Status
Accepted for M18E.5.

## Decision
Diagram zoom and pan are editor/view state, not authored `DiagramDefinition` data and not global `EditorHistory` transactions.

A pure-Java `DiagramViewport` carries zoom plus logical viewport center into diagram layout. `ScholarEditorScreen` owns one transient viewport per diagram block and supplies it to `DocumentLayoutEngine`. Fit is the deterministic default: zoom `1.0` centered on the logical canvas.

The layout engine derives one stable embedded workspace rectangle and a transformed logical canvas that may extend beyond it. Rendering and hit testing are clipped to the workspace. Pointer-to-logical conversion continues through `DiagramCoordinateTransform`, so dragging remains correct under zoom and pan.

## Consequences
- Ctrl+wheel and Ctrl+plus/minus can zoom without dirtying the document.
- Middle-mouse drag can pan without creating undo entries.
- Ctrl+0 restores Fit deterministically.
- Clipboard, Markdown/plain-text interchange, and authored electrical topology do not acquire viewport noise.
- Undo/redo of semantic edits does not unexpectedly walk through camera movements.
