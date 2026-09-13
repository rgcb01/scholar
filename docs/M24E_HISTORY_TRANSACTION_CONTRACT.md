# M24E History Transaction Contract

Status: Accepted / implemented.

M24E makes undo/redo semantics explicit for the current Scholar editor. The governing rule is:

One logical semantic user edit creates at most one logical history transaction.

## History Architecture Inventory

`EditorHistory` owns the undo stack, redo stack, current `EditorState`, and the current typing coalescing group. `EditorSession` is the authoritative mutation boundary and routes document-changing operations through either `history.applyEdit(...)` or `history.applyTyping(...)`.

`EditorState` contains semantic document state plus the current coherent editor selection and optional explicit typing marks. It does not contain menus, popups, hover state, Minecraft widgets, viewport cameras, drag previews, layout geometry, or renderer data.

Persistent semantic state:

- `Document` block list.
- document-owned datasets.
- block ASTs, inline ASTs, math ASTs, table/plot/diagram/figure semantics.
- stable IDs stored in semantic nodes.

Editor selection state:

- text caret/range selection.
- atomic block selection.
- equation, table, plot, diagram, and figure-caption nested selections.
- explicit typing marks when they affect following text input.

Transient UI and interaction state:

- menus and context menus.
- popups and pickers before commit.
- hover and hit-test previews.
- diagram drag interaction.
- incomplete diagram connection source.
- incomplete mechanical constraint source.
- preferred visual caret X.

Viewport state:

- document scroll.
- outline/TOC navigation UI state.
- diagram zoom, pan, and Fit camera state.

Nested editor interaction state:

- entering or exiting a nested editor is selection-only unless it commits semantic content.
- drag begin/preview/cancel are transient.
- drag commit is semantic if geometry changed.

## What Belongs In History

History snapshots store `EditorState`. Undo/redo restores:

- semantic `Document`;
- legal editor selection associated with that document;
- explicit typing marks when present in the snapshot.

This intentionally restores coherent editing position as part of undo/redo. It does not restore shell UI.

## What Must Not Create History Entries

The following update current interaction state or shell state only:

- caret movement;
- selection movement;
- entering or exiting nested editing modes without content changes;
- opening or closing menus;
- opening or closing context menus;
- opening or closing popups before commit;
- right-click targeting;
- scroll;
- zoom;
- pan;
- Fit camera;
- outline navigation;
- TOC navigation;
- hover;
- window resize;
- temporary diagram connection source;
- temporary mechanical constraint source;
- drag preview;
- canceled drag.

## Apply Edit Contract

`history.applyEdit(result)`:

- closes any typing group;
- creates one undo entry only when `result.changed()` is true;
- stores the previous current `EditorState` as the undo snapshot;
- replaces current state with `result.editorState()`;
- clears redo on semantic change;
- returns false for no-op edits without changing history.

`history.applyTyping(result, insertedText)`:

- creates or extends one typing transaction according to current coalescing rules;
- clears redo on semantic typing changes;
- returns false for no-op typing edits.

`history.setCurrent(state)` and `history.replaceCurrent(state)`:

- update the current state without creating undo entries;
- close active typing coalescing;
- do not clear redo.

## Redo Invalidation

Semantic edits after undo clear redo. Selection movement, caret movement, menu opening, context-menu opening, outline navigation, TOC navigation, drag preview, zoom, pan, and other transient actions do not clear redo.

Redo restores stored snapshots. It does not re-run ID generation or editing commands, so generated IDs such as pasted heading/table/figure IDs remain deterministic across redo.

## Typing Policy

Current typing behavior is contiguous insertion coalescing:

- contiguous plain text input at the same text/table/math insertion flow coalesces into one undo step;
- navigation, explicit non-typing edits, and structural edits close the group;
- structural math commands such as fraction/root/script creation are separate transactions;
- figure caption typing currently uses ordinary edit transactions per committed call.

## Composite Edit Boundaries

Composite semantic operations must enter history once:

- delete diagram element plus dependent connections;
- delete mechanical/electrical element plus dependent semantic references;
- wrap plot/diagram in figure;
- unwrap figure;
- edit figure caption through a committed content mutation;
- table row/column operation;
- plot series/point/property operation;
- dataset edit and its derived table/plot view recomputation;
- block copy/cut/paste mutations;
- structural deletion and insertion.

Internal helper calls must not push their own history entries.

## Clipboard Semantics

Copy writes clipboard data but does not create history and does not clear redo.

Cut writes clipboard data first and then applies one semantic delete transaction. If clipboard write fails through `EditorAction`, the document must not be deleted.

Paste applies one semantic insertion/replacement transaction when supported.

## Derived State

Section numbering, figure numbering, TOC entries, outline entries, cross-reference display labels, dataset-backed table views, dataset-backed plot points, electrical nets, and rendered/layout geometry are derived from the restored `Document`. History stores source semantics, not derived caches.

## Validation Oracles

History regression tests should assert after undo/redo:

- `DocumentValidator` has no errors;
- `EditorSelectionValidator` accepts the restored selection;
- degraded warnings, such as missing references or missing dataset bindings, may remain valid and must not be silently repaired.
