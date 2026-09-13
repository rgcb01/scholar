# M24E Implementation Report

Status: Implemented with automated validation complete.

M24E hardens undo/redo by documenting the transaction contract and adding regression coverage around history depth, redo branching, no-op behavior, composite edits, stable IDs, derived-state recomputation, drag transactions, clipboard semantics, and validator-backed undo/redo.

## Production Changes

- `EditorHistory` now exposes `undoDepth()` and `redoDepth()` for deterministic tests.
- `EditorSession` forwards `undoDepth()` and `redoDepth()`.
- `/scholar_dev_editor` includes an M24E manual history fixture.

No document model redesign, persistence, clipboard rewrite, or new scientific feature was added.

## Test Coverage

Added `HistoryTransactionHardeningTest` covering:

- semantic edits create exactly one undo entry;
- no-op edits and transient navigation create none;
- redo remains available after navigation-only changes;
- new semantic edits after undo clear redo;
- text, equation, table, plot, diagram, and dataset edits each create one transaction;
- copy creates no history while cut/paste create one transaction;
- context-menu Delete reuses the shared action path;
- figure wrap, caption edit, delete, undo, and redo restore deterministic states;
- table row deletion, plot point deletion, and diagram node deletion with dependent connections are single transactions;
- diagram drag preview/cancel are transient and commit is one transaction;
- dataset edit undo/redo propagates through dataset-backed table and plot resolvers;
- pasted stable IDs are restored by redo instead of regenerated;
- cross-reference display labels are recomputed from restored document structure;
- a mixed golden document sequence undoes and redoes every logical action exactly;
- a seeded randomized history sequence remains valid.

## Validation

`DocumentValidator` and `EditorSelectionValidator` are used in history tests after mutations and history navigation.

Core dependency boundaries remain unchanged: editor/history tests exercise pure Java core behavior, while Minecraft UI remains outside the semantic transaction model.
