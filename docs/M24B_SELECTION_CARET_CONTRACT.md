# M24B Selection And Caret Contract

## Status

Accepted and implemented.

## Selection Domains

Scholar maintains separate selection domains instead of one global character stream:

- `TextSelection` addresses contiguous editable inline blocks by `DocumentPosition`.
- `BlockSelection` addresses one atomic document block by block index.
- `EquationEditingSelection` addresses math content inside one `EquationBlock`.
- `TableEditingSelection` addresses text inside one table cell.
- `PlotEditingSelection` and `DiagramEditingSelection` address semantic object-editing targets.
- `FigureCaptionSelection` addresses caption text inside one `FigureBlock`.

## Validity Rules

Every `EditorState` selection must be valid for its current `Document`.

- text selections require editable inline blocks and valid logical character offsets;
- text selections cannot cross atomic non-text blocks;
- block selections require an existing block and are not used for paragraphs;
- nested equation/table/plot/diagram/caption selections must target a compatible owning block;
- dataset-backed table selections validate against the resolved table view;
- figure-contained plots and diagrams may be edited through the owning `FigureBlock`.

## Navigation Contract

Left and Right traverse logical source positions. When they encounter atomic document blocks outside their internal editing mode, those blocks become `BlockSelection`s.

Up and Down use the current laid-out document for visual-line movement. The preferred X coordinate is transient session state and is cleared by non-vertical navigation.

Home and End move to the current visual-line boundary for text and table-cell editing. Structured math, plot editing, diagram editing, and figure caption editing do not invent visual-line behavior in M24B.

Shift navigation preserves the anchor and moves only the active endpoint inside the current valid domain.

## Ctrl+A

`Ctrl+A` selects the active editing scope, not the whole Scholar document:

- text mode selects the contiguous run of editable inline blocks containing the active endpoint;
- equation mode selects the current equation root when it has content;
- table mode selects the current table cell text;
- figure caption mode selects the current caption text;
- atomic block, plot editing, and diagram editing modes remain unchanged.

## Atomic Inline Nodes

`CrossReference` is an atomic inline node. It contributes one logical text unit for caret movement and selection even though its displayed label is derived from document state.

Derived labels, table-of-contents text, figure numbering, reference numbering, and dataset-backed rendered cells are not treated as editable source text unless the active selection domain explicitly owns that source.

## Recovery

`EditorSelectionValidator.normalizedFallback` keeps valid selections unchanged and can choose the first legal selection for stale selections after document replacement. It does not mutate the document.
