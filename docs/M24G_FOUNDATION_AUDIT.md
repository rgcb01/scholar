# M24G Foundation Audit

Status: implemented; automated validation complete.

M24G closes the current editor-foundation pass. It does not add a new document feature. Its purpose is to verify that the existing prose, math, table, plot, diagram, figure, cross-reference, structure, dataset, validation, focus, clipboard, and history slices continue to work as one editor surface.

## Invariant Classification

### Model-Enforced Invariants

- Documents contain immutable block and dataset collections.
- Heading levels, table dimensions, figure IDs, dataset IDs, dataset columns, diagram element IDs, and math AST value objects validate their local construction rules.
- Dataset-backed views keep semantic bindings rather than copied resolved cells or points.

### Validator-Enforced Invariants

- Stable IDs are unique in their own namespaces.
- Cross-references to missing targets are warnings, not silent repairs.
- Missing dataset bindings and dataset column references are warnings or degraded resolved views, not crashes.
- Atomic scientific blocks remain valid document blocks even when some derived content is unavailable.

### Editor-State-Enforced Invariants

- `EditorState` rejects invalid transient selections.
- Text selections stay inside contiguous editable inline runs and do not cross atomic scientific blocks.
- Nested selections validate against their owning equation, table, plot, diagram, or figure caption.
- Dataset-backed table selections validate against the resolved current table view.

### History-Enforced Invariants

- One semantic edit creates one undo transaction unless it is an intentional no-op or existing typing coalescing case.
- Transient navigation, focus, popups, drag preview setup, and copy do not add undo entries.
- Undo/redo restores the exact document and selection snapshot for current supported systems.

### Interaction-Enforced Invariants

- One input event has one authoritative focus owner.
- Context menus and toolbar/menu popups route through shared `EditorAction` enable guards.
- Structural keyboard and context-menu actions use the same action path where applicable.

### Documented-Only Boundaries

- Repair workflows, validation UI, persistence migration, richer multi-object selection, and document-wide resource management remain deferred.
- Markdown-backed editing, networking, gameplay systems, physics systems, extension APIs, and incremental layout remain outside the editor foundation.

## Audit Result

The current foundation has no intentionally undocumented core editor behavior in the covered areas. Remaining work is feature expansion and dedicated UX polish rather than additional foundation hardening.
