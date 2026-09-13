# M24D Structural Editing Contract

Status: Accepted and implemented.

M24D defines the current safe behavior for structural document edits. A structural edit must leave a `Document` with no validator errors, a valid `EditorSelection`, no stale nested selection state, and deterministic undoable editor state through the existing history layer.

## Operation Inventory

- Inline edit: prose/table-cell typing, inline mark toggles, cross-reference insertion, figure caption typing, math token edits.
- Block structural edit: equation/table/plot/diagram/TOC insertion, selected atomic-block deletion, selected block cut/paste, text block split/merge.
- Nested structured edit: math fraction/root/script/group edits, table row/column edits, plot series/point edits, diagram node/connection/electrical/mechanical edits.
- Resource edit: dataset create/rename/cell/row/column/delete operations.
- Wrapper transformation: Plot/Diagram to Figure, and Figure unwrap.

## Insert

- At text caret start: insert before the current text block.
- At text caret end: insert after the current text block.
- In the middle of a text block: split the text block and insert between the two resulting text blocks.
- Replacing a text selection with a block removes the selected text range and inserts the block at the selected structural location.
- Inserting at the end of the document appends an empty paragraph fallback after the inserted atomic block so editing can continue.
- Inserting from `BlockSelection` inserts after the selected block.

## Delete

- A selected atomic block is removed by Delete or Backspace.
- Deleting the only block creates one empty paragraph and places the caret at `0:0`.
- Deleting a middle atomic block chooses the previous editable block end when possible.
- Deleting a first atomic block chooses the next editable block start when possible.
- Deleting a last atomic block chooses the previous editable block end when possible.
- Deleting a TOC does not alter headings.
- Deleting dataset-backed table/plot view blocks does not delete dataset resources.

## Split

- Paragraph split produces two paragraphs and preserves inline node order and marks.
- A `CrossReference` remains one atomic inline unit and cannot be split internally.
- Heading at start inserts a paragraph before the heading and keeps the heading ID on the original heading.
- Heading in the middle splits into two headings; the left heading keeps the original stable ID and the new right heading is ID-less.
- Heading at end inserts a paragraph after the heading and keeps the original heading ID.
- Empty heading Enter converts that heading to an empty paragraph.

## Merge

- Paragraph + Paragraph merges inline content into the left paragraph.
- Heading + Paragraph keeps the left heading style and ID.
- Paragraph + Heading keeps the left paragraph style.
- Heading + Heading keeps the left heading level and ID.
- Text merge never skips across atomic blocks.

## Atomic Boundaries

- Backspace at the start of a text block selects the previous atomic block instead of deleting it immediately.
- Delete at the end of a text block selects the next atomic block instead of deleting it immediately.
- Once the atomic block is explicitly selected, Delete or Backspace removes it.

## Wrap / Unwrap

- Wrapping Plot or Diagram creates a `FigureBlock` with a unique figure ID and empty semantic caption.
- The contained Plot/Diagram block is preserved semantically.
- Selection becomes the resulting Figure block.
- Unwrapping a Figure restores its contained Plot/Diagram block at the same block index.
- Selection becomes the restored contained block.

## Nested Structured Edits

- Table row/column edits apply only to authored tables, preserve rectangular shape, and keep a legal cell selection.
- Dataset-backed table view row/column structure commands are disabled/no-op; dataset resource edits are separate.
- Plot edits preserve plot validity and recover to a legal plot target.
- Dataset-backed plot series keep their dataset binding; authored point commands do not mutate dataset resources.
- Diagram edits preserve diagram validity, clear dependent connections/references through existing diagram editors, and recover to a legal diagram target.
- Equation structural edits remain inside `MathExpressionEditor`; whole EquationBlock copy/cut/paste is document structural editing.

## Cut / Paste

- Structural Cut is Copy plus one deletion through the existing action/history path.
- Context-menu Delete uses the shared `DELETE` editor action and the normal `EditorSession.deleteForward()` path.
- Whole Heading, EquationBlock, TableBlock, PlotBlock, DiagramBlock, FigureBlock, and TOC clipboard routes preserve semantic payloads where supported.
- Pasting a duplicate Heading, EquationBlock, TableBlock, Figure, or Dataset remaps the copied stable ID when needed.
- Cross-references are not cascaded when a target is deleted; broken references remain valid degraded state and are warnings.
- Dataset-backed table and plot view deletion does not delete their dataset.

## Minimal Document

`Document` may represent arbitrary valid block lists, but the editor requires a legal selection. If deleting the last selected block would leave no block to select, Scholar creates one empty paragraph as the editor fallback.

## Validation Oracle

M24D regression tests use `DocumentValidator` and `EditorSelectionValidator` after structural mutations. Production code does not run full validation after every keystroke.
