# M24 Editor Foundation v1

Status: accepted implementation baseline after M24G.

Scholar's editor foundation v1 is the integrated baseline formed by M24A-M24G. It defines how the current editor keeps semantic document data, transient selections, validation, context actions, input focus, and history coherent across mixed scientific content.

## Covered Systems

- Prose and heading editing.
- Inline formatting and inline cross-references.
- Structured math equation editing.
- Authored tables and dataset-backed table views.
- Static and editable plots.
- Generic, electrical, and mechanical diagrams.
- Figures containing plots or diagrams with editable captions.
- Derived section structure, outline, TOC, and cross-reference labels.
- Reusable document datasets.
- Native clipboard payloads plus plain-text fallbacks where implemented.

## Foundation Contracts

- `Document` remains the immutable semantic source of truth.
- `EditorState` is the single transient selection snapshot.
- `EditorSelectionValidator` is the central validity gate for selections.
- `DocumentValidator` reports structural errors and degraded-state warnings without mutating data.
- `EditorSession` owns semantic mutation and history coordination.
- `EditorAction` is the shared path for menu, toolbar, shortcut, and context-menu commands.
- `EditorFocusOwner` derives the active input owner from the current selection.
- Derived views such as TOC entries, cross-reference labels, and dataset-backed table/plot data are recomputed rather than stored as authoritative editor state.

## Current Limits

- Validation diagnostics are not yet exposed through a user-facing repair workflow.
- Persistence/schema migration is still deferred.
- Whole-document multi-object selection is not implemented.
- Dataset editing remains intentionally small.
- Markdown import/export does not cover every semantic block.
- Networking, gameplay systems, physics systems, public extension APIs, and incremental layout remain out of scope.
