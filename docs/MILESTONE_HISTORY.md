# Scholar Milestone History

This file is a compact checkpoint log for the work imported from `MinecraftSTEMDev_M19F_new.zip`.

Detailed architecture remains in the specs and ADRs. Detailed implementation notes remain in the milestone report files linked below.

## M16 - Scientific Plots

Status: complete through M16G.

M16 introduced native Scholar scientific plots:

- semantic `PlotBlock(PlotDefinition)` document blocks;
- static XY plot data using `PlotSeries` and `DataPoint`;
- finite `double` data values, explicit/automatic axis ranges, and linear axes;
- pure-Java plot auto-range, tick generation, number formatting, and coordinate transforms;
- responsive pure-Java plot layout with labels, legend data, line segments, scatter points, and deterministic series styling;
- Minecraft rendering from laid-out plot geometry rather than rendered image snapshots;
- atomic document selection first, then plot editing selection and explicit plot property/series/point editing;
- native whole-plot clipboard payload plus readable plain-text fallback;
- hardening for extreme finite ranges, responsive layout, clipping, hidden/out-of-range points, and tick label determinism.

Key references:

- `docs/ROADMAP.md`
- `docs/PROJECT_SPEC.md`
- `M16G_HOTFIX_NOTE.md`
- ADRs 0155-0177

## M17 - Scientific Diagram Foundation

Status: complete and manually accepted through M17G.

M17 introduced the reusable, domain-neutral diagram foundation:

- semantic `DiagramBlock(DiagramDefinition)` with logical canvas coordinates;
- diagram-scoped element IDs and element-scoped port IDs;
- generic rectangular `DiagramNode` elements with perimeter ports;
- semantic `DiagramConnection` values between ports, with derived orthogonal routes;
- pure-Java diagram layout, routing, hit testing, and viewport coordinate transforms;
- atomic document selection plus dedicated `DiagramEditingSelection`;
- diagram target traversal, node dragging, structural node/connection editing, and label editing;
- native whole-diagram clipboard payload plus readable structural fallback text;
- responsive and edge-case hardening without adding electrical or mechanical meaning.

Key reports:

- `M17A_ARCHITECTURE_REPORT.md`
- `M17B_IMPLEMENTATION_REPORT.md`
- `M17B_CONNECTION_LABEL_FIX.md`
- `M17C_IMPLEMENTATION_REPORT.md`
- `M17D_IMPLEMENTATION_REPORT.md`
- `M17E_IMPLEMENTATION_REPORT.md`
- `M17F_IMPLEMENTATION_REPORT.md`
- `M17G_IMPLEMENTATION_REPORT.md`

Key specs/ADRs:

- `docs/DIAGRAM_SPEC.md`
- ADRs 0178-0193

## M18 - Electrical Diagrams

Status: complete and manually accepted through M18H.1.

M18 built electrical schematic authoring on top of the M17 diagram foundation:

- `ElectricalComponent` domain elements inside existing `DiagramBlock` values;
- bounded first electrical vocabulary: resistor, capacitor, DC voltage source, ground, diode, LED, and SPST switch;
- stable component terminal schemas and `DiagramEndpoint` reuse for wires;
- quarter-turn orientation that preserves terminal identity and existing connections;
- pure-Java derived schematic symbol geometry separate from the AST;
- insert-by-kind actions, deterministic designators, value labels, rotation, dragging, connection creation, deletion, and undo/redo;
- explicit electrical junction elements and derived semantic nets;
- net labels that annotate explicit connected nets without merging by equal text;
- diagram workspace scaling with authored canvas/workspace size and transient zoom/pan viewport state;
- whole-diagram clipboard/interchange regression for mixed generic/electrical diagrams;
- responsive hardening and final terminal/wire visual polish.

Key reports:

- `M18A_ARCHITECTURE_REPORT.md`
- `M18B_IMPLEMENTATION_REPORT.md`
- `M18B_TEST_FIX_REPORT.md`
- `M18B_VISUAL_POLISH_REPORT.md`
- `M18C_IMPLEMENTATION_REPORT.md`
- `M18C_VISUAL_POLISH_REPORT.md`
- `M18C_LED_POLISH_REPORT.md`
- `M18D_IMPLEMENTATION_REPORT.md`
- `M18E_IMPLEMENTATION_REPORT.md`
- `M18E5_IMPLEMENTATION_REPORT.md`
- `M18F_IMPLEMENTATION_REPORT.md`
- `M18G_IMPLEMENTATION_REPORT.md`
- `M18H_IMPLEMENTATION_REPORT.md`
- `M18H1_IMPLEMENTATION_REPORT.md`

Key specs/ADRs:

- `docs/ELECTRICAL_DIAGRAM_SPEC.md`
- ADRs 0194-0219

## M19 - Mechanical Diagrams

Status: implemented through M19F, with M19F manual QA still pending according to the imported roadmap.

M19 adds mechanical technical-drawing features on top of the M17 diagram foundation:

- mechanical primitives: line, centerline, rectangle, circle, arc, arrow, and reference point;
- semantic dimensions and callouts;
- generic scrollable dropdown menus for long authoring menus;
- bounded geometric relationships/constraints: horizontal, vertical, coincident, parallel, perpendicular, and concentric;
- mechanical symbols: shaft, gear, bearing, spring, piston, and bolt;
- mechanical labels, notes, leader callouts, and annotation visual polish;
- assembly part references using semantic item balloons linked by stable element IDs;
- generated BOM tables derived from authored part references;
- runtime fix for M19F part-reference/BOM behavior.

Key reports:

- `docs/M19A_IMPLEMENTATION_REPORT.md`
- `M19B_IMPLEMENTATION_REPORT.md`
- `M19B1_IMPLEMENTATION_REPORT.md`
- `M19C_IMPLEMENTATION_REPORT.md`
- `M19D_IMPLEMENTATION_REPORT.md`
- `M19E_IMPLEMENTATION_REPORT.md`
- `M19E1_IMPLEMENTATION_REPORT.md`
- `M19F_IMPLEMENTATION_REPORT.md`
- `M19F_RUNTIME_FIX_REPORT.md`

Key ADRs:

- ADRs 0220-0224

## M20 - Figures & Scientific Media

Status: complete and manually accepted.

M20 adds first-class scientific figures on top of existing plot and diagram content:

- semantic `FigureBlock(id, content, caption)` document blocks;
- stable authored figure IDs separated from derived document-order display numbers;
- figure content limited to existing `PlotBlock` and `DiagramBlock` for this slice;
- semantic editable captions stored as `InlineContent`;
- layout that places visual content above a generated `Figure N.` caption and exposes combined figure bounds;
- Minecraft rendering of figure content plus caption without making rendering the source of truth;
- atomic whole-figure selection with Enter routing into contained plot/diagram editing;
- Figure menu actions for wrapping plots/diagrams, editing captions, and unwrapping figures;
- native whole-figure clipboard payload plus readable plain-text fallback;
- paste-time stable-ID remapping to avoid duplicate figure IDs;
- development fixtures for plot figures, diagram figures, and multiple derived figure numbers.

Key ADRs:

- ADRs 0225-0229

## M21 - Cross-References

Status: complete and manually accepted.

M21 adds first-class semantic inline cross-references:

- `CrossReference(kind, targetId)` as an atomic inline node;
- derived labels for Figure, Table, Equation, and Section targets;
- stable optional target IDs for headings, tables, and equations;
- deterministic `[Missing reference]` rendering for unresolved targets;
- inline layout/rendering through the existing text pipeline while preserving logical one-character source offsets;
- Insert > Cross Reference picker backed by semantic targets from the current document;
- native inline-content clipboard payload for text selections containing references, with resolved plain-text OS fallback;
- Markdown serialization as resolved readable text only, with semantic round-trip deferred;
- development fixtures for resolved and broken references.

Key ADRs:

- ADRs 0230-0234

## M22 - Document Structure & Navigation

Status: complete and manually accepted.

M22 adds a derived document-structure layer on top of the existing flat block document:

- `DocumentStructureResolver`, `DocumentStructure`, `SectionEntry`, and `SectionNumber`;
- deterministic hierarchical section numbering derived from ordered `Heading` blocks;
- explicit zero placeholders for skipped heading levels, such as `1.0.1`;
- section cross-reference labels upgraded from flat labels to hierarchical labels;
- heading layout with derived display-only section-number prefixes;
- semantic `TableOfContentsBlock` whose entries are derived during layout/plain-text output;
- TOC click navigation by stable heading ID;
- editor outline panel derived from the same structure resolver;
- navigation to headings without creating undo history;
- heading and TOC native clipboard payloads, with duplicate heading IDs remapped on paste;
- Markdown export that keeps headings clean and emits a minimal readable TOC placeholder;
- development fixtures for TOC, hierarchical headings, skipped levels, and section references.

Key ADRs:

- ADRs 0235-0240

Key reports:

- `docs/M22_IMPLEMENTATION_REPORT.md`

## M23 - Scientific Data And Datasets

Status: implemented; automated validation complete, manual Minecraft QA pending.

M23 adds first-class reusable scientific datasets:

- document-owned immutable `ScientificDataset` resources;
- stable dataset IDs and column IDs, with optional row IDs reserved for future workflows;
- numeric, text, and missing dataset values;
- dataset-backed `TableBlock` bindings resolved into current table snapshots for layout, plain text, and Markdown export;
- dataset-backed `PlotSeries` bindings resolved into current numeric XY points for layout/rendering;
- deterministic broken binding states for missing datasets or columns;
- missing or nonnumeric rows skipped during dataset-backed plot resolution;
- editor dataset operations for create/rename/edit cells/add-delete rows/add-delete columns;
- native dataset clipboard payload with TSV fallback and duplicate-ID remapping on paste;
- bounded CSV/TSV import helper for simple tabular datasets;
- development fixtures for shared dataset-backed table/plot views, column subsets, broken bindings, and mixed values.

Key reports:

- `docs/M23_IMPLEMENTATION_REPORT.md`

Key ADRs:

- ADRs 0241-0246

## Current Imported Baseline

The imported tree has now been extended through M23. It includes Plot, Diagram, Electrical, Mechanical, Figure, Cross-Reference, Document Structure/Navigation, and Scientific Dataset source/test coverage, updated project specs, roadmap updates, and milestone implementation reports.

Before treating M23 as complete in manual QA, run dataset-backed table/plot and dataset editing checks in Minecraft.

## M24A - Document Model Invariants And Validation Foundation

Status: implemented; automated validation complete.

M24A adds the first central document validation layer:

- pure Java `DocumentValidator.validate(Document)`;
- immutable `DocumentValidationResult` and `DocumentDiagnostic` values;
- diagnostic severities for structural `ERROR`s and degraded-state `WARNING`s;
- namespace-specific stable ID checks for figures, headings/sections, tables, equations, datasets, dataset columns, and diagram elements;
- warnings for broken cross-references and missing dataset bindings/columns;
- diagram checks for duplicate ports, endpoints, canvas bounds, and mechanical semantic references;
- tests for complex valid documents, degraded documents, constructor-enforced invariants, determinism, and immutability.

Key reports:

- `docs/M24A_IMPLEMENTATION_REPORT.md`

Key ADRs:

- ADRs 0247-0251

## M24B - Selection And Caret Foundation

Status: implemented; automated validation complete.

M24B adds the first explicit editor selection/caret contract:

- pure Java `EditorSelectionValidator` for transient editor selection validity;
- `EditorState` now rejects invalid selections at construction time;
- text selections are limited to contiguous editable inline blocks and cannot cross atomic scientific blocks;
- nested equation, table, plot, diagram, and figure-caption selections validate against their owning block;
- dataset-backed table editing selections validate against the resolved table view;
- `Ctrl+A` selects the current editing scope for prose, math, table cells, and figure captions;
- atomic block selections remain unchanged by `Ctrl+A`;
- cross-references remain one logical inline caret unit even though their displayed label is derived;
- `/scholar_dev_editor` includes explicit M24B navigation fixtures;
- deterministic golden and randomized navigation tests verify mixed-document selection validity.

Key reports:

- `docs/M24B_IMPLEMENTATION_REPORT.md`
- `docs/M24B_SELECTION_CARET_CONTRACT.md`

Key ADRs:

- ADRs 0252-0255

## M24C - Right-Click / Context Menu Foundation

Status: implemented; automated validation complete.

M24C adds the first editor-wide right-click context menu foundation:

- pure Java `EditorContextActionResolver` maps semantic selections to relevant existing `EditorAction`s;
- Minecraft-native `ContextMenuWidget` renders bounded, scrollable context menus with disabled rows and shortcut labels;
- right-click inside an active text/table-cell selection preserves the selection;
- right-click outside the active selection targets the clicked semantic object before opening the menu;
- empty document area right-click leaves selection unchanged and offers empty-area actions;
- context menus close incompatible shell state and never bypass action enable guards;
- missing action registrations are skipped without crashing menu construction.

Key reports:

- `docs/M24C_IMPLEMENTATION_REPORT.md`
- `docs/M24C_CONTEXT_MENU_CONTRACT.md`

Key ADRs:

- ADRs 0256-0260

## M24D - Structural Editing Hardening

Status: implemented; automated validation complete.

M24D makes current structural document editing explicit and heavily tested:

- `docs/M24D_STRUCTURAL_EDITING_CONTRACT.md` defines insertion, deletion, split, merge, wrap, unwrap, cut, paste, minimal-document, atomic-boundary, and dataset view/resource semantics;
- heading middle split no longer duplicates stable heading IDs;
- whole selected equations can now be copied/cut/pasted as structural document blocks;
- structural paste remaps duplicate heading, equation, table, figure, and dataset IDs where supported;
- authored table structural edits and dataset-backed table no-op policy are covered by validator-backed tests;
- plot and diagram structural edits are covered by validator-backed tests;
- golden and seeded randomized structural edit sequences assert document and selection validity after every step;
- `/scholar_dev_editor` includes an explicit M24D structural-editing fixture.

Key reports:

- `docs/M24D_IMPLEMENTATION_REPORT.md`
- `docs/M24D_STRUCTURAL_EDITING_CONTRACT.md`

Key ADRs:

- ADRs 0261-0265

## M24E - Undo/Redo And Transaction Hardening

Status: implemented; automated validation complete.

M24E makes Scholar's history model explicit and regression-tested:

- `docs/M24E_HISTORY_TRANSACTION_CONTRACT.md` defines semantic history state, transient UI/navigation state, apply-edit behavior, redo invalidation, typing coalescing, composite edit boundaries, clipboard behavior, derived-state recomputation, and validation oracles;
- `EditorHistory` exposes undo/redo depth for deterministic transaction tests;
- `EditorSession` forwards undo/redo depth without changing mutation authority;
- tests assert that one logical semantic edit creates one transaction, no-op/transient actions create none, navigation after undo does not clear redo, and a new semantic edit after undo does clear redo;
- text, equation, table, plot, diagram, figure, dataset, clipboard, context-menu, stable-ID, reference, golden-sequence, and seeded randomized history regressions are covered;
- `/scholar_dev_editor` includes an explicit M24E manual history fixture.

Key reports:

- `docs/M24E_IMPLEMENTATION_REPORT.md`
- `docs/M24E_HISTORY_TRANSACTION_CONTRACT.md`

Key ADRs:

- ADRs 0266-0270

## M24F - Input, Focus And Interaction Consistency

Status: implemented; automated validation complete.

M24F makes the editor input/focus contract explicit:

- `EditorFocusOwner` names the one semantic owner for the current selection;
- `EditorSession.focusOwner()` exposes that owner to core tests and future UI coordination;
- input dispatch priority is documented as modal popups, context menu, menu/toolbar popup, active nested editor, document editor, then screen fallback;
- Escape unwinds only the highest-priority active interaction layer;
- nested editor entry/exit and target traversal remain selection-only and do not create history;
- tests cover focus-owner derivation, nested entry/exit, scoped text input, active-scope `Ctrl+A`, table Tab traversal, undo/redo focus recovery, transient diagram drag clearing, and a golden input-like sequence;
- `/scholar_dev_editor` includes an explicit M24F manual input/focus fixture.

Key reports:

- `docs/M24F_IMPLEMENTATION_REPORT.md`
- `docs/M24F_INPUT_FOCUS_CONTRACT.md`

Key ADRs:

- ADRs 0271-0275

## M24G - Final Editor Foundation Regression And Documentation

Status: implemented; automated validation complete.

M24G closes the M24 editor-foundation hardening pass:

- canonical mixed-document fixtures cover the current prose, math, table, plot, diagram, figure, cross-reference, TOC, dataset, validation, clipboard, focus, context-menu, and history surface;
- integration tests verify valid selections, zero-error validation, context-menu/delete parity, clipboard preservation, derived-view recomputation, degraded warning stability, atomic boundary policies, full undo/redo replay, and a large-document smoke path;
- a seeded cross-subsystem replay test exercises deterministic operations across current editor domains;
- `/scholar_dev_editor` includes an explicit M24G manual foundation fixture;
- `docs/M24_EDITOR_FOUNDATION_V1.md` and `docs/M24G_FOUNDATION_AUDIT.md` summarize the accepted foundation contract and invariant classification.

Key reports:

- `docs/M24G_IMPLEMENTATION_REPORT.md`
- `docs/M24G_FOUNDATION_AUDIT.md`
- `docs/M24_EDITOR_FOUNDATION_V1.md`

Key ADRs:

- No new ADRs. M24G consolidates and tests M24A-M24F decisions.
