# M37 - Authoring UX and Document Workflow

Status: COMPLETE / MANUALLY ACCEPTED. M37 was merged and pushed to `main` at `681548597acb98cd8c0866da5b0fae4885147298`. Historical pending-QA wording below records the implementation checkpoint.

## Initial workflow audit

This audit traces the production Home, editor, ribbon, dialogs, action predicates, and editor/session tests. It is a code-and-test audit; the in-game authoring session below remains the acceptance gate. No new scientific subsystem or document representation is introduced.

| Journey | Existing behavior | Finding / M37 decision |
| --- | --- | --- |
| Home | Document cards, previews, template chooser, guarded Delete, and pagination exist. | Card context menu offered only Delete. Add Open and Rename through the existing application/repository identity path. Escape first dismisses the context menu or template chooser. Keep the accepted card design. |
| Blank document | Application creates a valid blank document and a fresh editor session with a selectable paragraph. | Add a typing/history regression for the first edit. No tutorial text in the document. |
| Text editing | Caret, selection, replacement, Enter, deletion, vertical/Home/End navigation, Undo/Redo, and scoped nested editing use `EditorSession`/controller. | Preserve M24/M27 behavior and command routing. No second editing implementation. |
| Formatting | Home has a style picker, marked text, font/size, alignment, spacing, and indentation. | Existing groups are coherent; do not move every scientific action into Home. |
| Scientific insertion | Insert exposes equation, table, variable, result, quantity, plot, diagram, references, and TOC; Data exposes datasets, analysis, table binding, fit, and CSV export. Figure and Diagram retain their focused tabs. | Keep contextual controls. Analysis fit selection exposed an internal ID; show analysis type and human-readable dataset/X/Y labels instead. |
| Cross-references | Targets already have resolved display labels and descriptions, and `EditorSession` disables insertion when there is no target. | Popup displayed only the first eight targets and mouse wheel could scroll the document behind it. Add paging, arrow/Page Up/Page Down, Enter, and wheel navigation. Stable IDs remain internal. |
| File | Save/Save As/Rename, unsaved Save/Discard/Cancel protection, CSV import/export, Markdown/PDF export, and Home return already exist. Header shows Saved/Unsaved. | Disable Save when an already named document is clean. Keep Save As available. Preserve native persistence and M36 interchange. |
| Dialogs | File rename/save accepts Enter. Analysis and computation have scoped selectors. M36 interchange shows busy/error state. | Preserve form-specific controls; make the interchange filename field retain its draft across paging and accept Enter. Broader visual normalization needs in-game comparison before further change. |
| Context/object editing | Editor context actions reuse canonical commands. Clicking a table cell enters cell editing; plot/diagram targets enter scoped editing from selection; block selection/focus is visible. | Keep one action path and the current two-step object focus where required to prevent accidental edits. |
| Navigation | Scroll, outline, page count, word count, 40%-200% zoom, Fit Page, and Fit Width exist. | View actions remain transient and outside semantic history. Page Up/Page Down caret semantics are intentionally not changed in this milestone. |
| Errors/feedback | Document errors use Scholar dialogs, scientific dialogs have local validation, interchange reports completion/failure, and unsaved work has a blocking prompt. | Retain these local mechanisms; do not introduce a notification framework or use chat for normal document errors. Test user wording manually. |
| Responsiveness/accessibility | Ribbon presentation has full/compact/icon states, palettes, tooltips, and overflow; Home and dialogs constrain widths; scientific document typography remains distinct from Minecraft-native chrome. | Keep accepted M29/M34 presentation. Verify minimum supported GUI scale and long names/labels manually. |

## Final command organization

- **File:** New, Open/Home, Save, Save As, Rename, Close, CSV import, Markdown/PDF export.
- **Home:** Clipboard, Undo/Redo, semantic paragraph styles, font and marks, paragraph alignment/spacing.
- **Insert:** Structural and scientific objects, quantity tools, cross-reference, TOC, page break.
- **Data:** Dataset authoring/binding, CSV export, analysis/fit, table and plot controls.
- **Figure / Diagram:** Existing contextual composition and diagram palettes. Their semantics and icon registry are unchanged.
- **Layout:** Physical page setup, columns, breaks.
- **View:** Outline and zoom/fit controls; these change only view state.

The Insert shortcut to create a dataset and the Data primary dataset command intentionally address different starting points without separate semantic implementations.

## Shortcuts, focus, and Escape

`Ctrl+S` Save, `Ctrl+Z` Undo, `Ctrl+Y` or `Ctrl+Shift+Z` Redo, `Ctrl+C/X/V` clipboard, `Ctrl+A` Select All, `Ctrl+B/I` marks, and `Ctrl+N/O` New/Open use existing action/shortcut authority. They operate only in Scholar. Dialog fields and nested popups receive input before document shortcuts. `Ctrl+Shift+S` remains Save As.

Escape closes the innermost popup, selector, context menu, or dialog first; in the editor it requests leaving and invokes Save/Discard/Cancel when dirty. On Home it now dismisses the card menu or template chooser before leaving Home. The cross-reference picker owns arrows, Page Up/Down, wheel, and Enter while open; scrolling cannot leak to the document behind it.

## Workflow and state decisions

Home rename uses `ScholarApplication.renameDocument`: if a document workspace is active, its descriptor is updated through `workspace.rename`; otherwise the repository renames its metadata directly. The document ID and semantic editor history are unchanged. Home Delete still uses its confirmation screen. Save enablement reflects the dirty state (or an unnamed workspace); successful save returns the header to Saved, while failures remain visible in the Scholar file dialog. Import/export confirmation and overwrite behavior remain M36's responsibility.

The cross-reference picker stores only transient selected-row/page state. It uses the existing `CrossReferenceTarget.pickerLabel()` for display and the target's kind/ID only when invoking the canonical insertion action. Fit choices likewise display names but apply the original analysis ID. No transfer, persistence, resolver, or computation semantics change.

## Deliberate V1 limits

- No second outline or scientific dependency model, general spreadsheet, office-suite parity, or third-party UI framework.
- M34 Readability Sample remains an opt-in calibration document, not default content in new documents.
- Dense dataset and diagram authoring retains existing specialized controls; M37 does not redesign their semantic editors.
- Fit choice labels can be long; the existing selector clipping/scrolling must be checked at narrow GUI scales.
- A full visual normalization of every dialog is deferred until in-game comparison identifies a concrete failure.
- Page Up/Page Down are not repurposed for caret movement without a separate text-navigation contract.

## Verification and manual acceptance

Focused tests cover Home rename with active/closed workspaces, stable identity/history, initial blank-document typing, cross-reference paging and selection, and human-readable fit options. Full suite, build, five boundary suites, and `git diff --check` are required before the manual session. No M35 public API, M26 format, or M25 transfer contract changes are intended.

Run this as one normal user session, not a developer screen:

1. Launch `/scholar`. Create a Blank Document, type immediately, rename it from Home or File, reopen it, and verify the name. Right-click a Home card and verify Open, Rename, Delete; cancel Delete once, then confirm only on a disposable document. Press Escape while the card menu and template chooser are open.
2. Write several paragraphs and headings. Select text, replace it, apply bold/italic, use Ctrl+C/X/V/A, then Undo/Redo. Verify the Home style control and direct caret placement.
3. Insert an equation, a variable and computed result, and a dataset (or import the M36 CSV fixture). Insert a dataset-backed table, analysis, plot with fit, Figure with caption, and an electrical or mechanical diagram. Edit/select/copy/delete one atomic object and verify what is highlighted.
4. Add enough headings/figures to produce more than eight reference targets. Insert a cross-reference using mouse, wheel, keyboard arrows/Page Down and Enter; verify later-page targets remain reachable without typing an ID. Insert a TOC.
5. Change page size, margins, orientation, columns, and a section break. Scroll several pages, use the outline, Fit Page/Fit Width, and zoom from 40% to 200%; verify these view changes do not mark the document unsaved.
6. Save, return Home, reopen, and export PDF. Confirm Save is disabled when the named document is clean and the header says Saved. Make an unsaved edit; try Close/Open/Home and verify Save, Discard, and Cancel each behave correctly. Test import/export dialogs with Enter, Escape, filename paging, overwrite cancellation, and failure feedback.
7. Repeat the ribbon, Home, cross-reference picker, and analysis selector at the smallest supported Minecraft GUI scale. Check labels, focus, tooltip contrast, and absence of overlap.

M37 is not accepted until that production session is reported. Do not merge this branch merely because automated tests pass.
