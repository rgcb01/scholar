# Scholar Milestone History

This file is a compact checkpoint and acceptance log. M16-M19 began with the imported
`MinecraftSTEMDev_M19F_new.zip` baseline; later milestones were developed in this repository.

Detailed architecture remains in the specs and ADRs. Historical completion reports removed
from the current tree remain available in Git history.

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

Design references: [Project Specification](PROJECT_SPEC.md) and ADRs 0155-0177.

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

Design references: [Diagram Specification](DIAGRAM_SPEC.md) and ADRs 0178-0193.

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

Design references: [Electrical Diagram Specification](ELECTRICAL_DIAGRAM_SPEC.md)
and ADRs 0194-0219.

## M19 - Mechanical Diagrams

Status: complete and manually accepted through M19F.

M19 adds mechanical technical-drawing features on top of the M17 diagram foundation:

- mechanical primitives: line, centerline, rectangle, circle, arc, arrow, and reference point;
- semantic dimensions and callouts;
- generic scrollable dropdown menus for long authoring menus;
- bounded geometric relationships/constraints: horizontal, vertical, coincident, parallel, perpendicular, and concentric;
- mechanical symbols: shaft, gear, bearing, spring, piston, and bolt;
- mechanical labels, notes, leader callouts, and annotation visual polish;
- assembly part references using semantic item balloons linked by stable element IDs;
- generated BOM tables derived from authored part references;
- a corrected M19F Diagram action registration, preserving the shared `EditorAction` dispatch path.

Mechanical constraints reference stable primitive IDs and reconcile a bounded set of
relationships; they are not a general CAD solver. Part balloons reference stable target
element IDs. Generate BOM inserts an ordinary table as one history transaction rather than
persisting a derived diagram cache. See [Diagram Specification](DIAGRAM_SPEC.md) and ADRs 0220-0224.

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

## M23 - Scientific Data And Datasets

Status: complete and manually accepted.

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

Key ADRs:

- ADRs 0241-0246

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

Contract: [M24B Selection And Caret](M24B_SELECTION_CARET_CONTRACT.md).

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

Contract: [M24C Context Menu](M24C_CONTEXT_MENU_CONTRACT.md).

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

Contract: [M24D Structural Editing](M24D_STRUCTURAL_EDITING_CONTRACT.md).

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

Contract: [M24E History Transactions](M24E_HISTORY_TRANSACTION_CONTRACT.md).

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

Contract: [M24F Input And Focus](M24F_INPUT_FOCUS_CONTRACT.md).

Key ADRs:

- ADRs 0271-0275

## M24G - Final Editor Foundation Regression And Documentation

Status: complete and accepted, including final manual Minecraft QA. M24 Editor Core Foundation V1 is fully accepted.

M24G closes the M24 editor-foundation hardening pass:

- canonical mixed-document fixtures cover the current prose, math, table, plot, diagram, figure, cross-reference, TOC, dataset, validation, clipboard, focus, context-menu, and history surface;
- integration tests verify valid selections, zero-error validation, context-menu/delete parity, clipboard preservation, derived-view recomputation, degraded warning stability, atomic boundary policies, full undo/redo replay, and a large-document smoke path;
- a seeded cross-subsystem replay test exercises deterministic operations across current editor domains;
- `/scholar_dev_editor` includes an explicit M24G manual foundation fixture;
- [M24 Editor Foundation V1](M24_EDITOR_FOUNDATION_V1.md) summarizes the accepted foundation contract.

Key ADRs:

- No new ADRs. M24G consolidates and tests M24A-M24F decisions.

## M25A - Document Transfer And Identity Closure Architecture

Status: architecture complete and accepted. M25B is now in progress; its implementation checkpoints are recorded separately below. No production transfer implementation is claimed by M25A itself.

- ROM-6 maps current clipboard/transfer entry points, payloads, identity remappers, resource gaps, and tests.
- ROM-7 defines immutable semantic DocumentFragment versus destination-aware DocumentTransfer, separate from editor/history/clipboard/persistence.
- ROM-8 defines minimal whole-dataset resource closure and source-identity deduplication through supported ownership boundaries.
- ROM-9 defines document-global versus owner-local identities, shared namespace-aware remapping, conservative resource reuse, safe external reference Text degradation, and snapshot redo.
- ROM-10 defines whole composite ownership: Figure wrapper/caption/Plot-or-Diagram, complete Diagram graph, exact table/plot/math content, semantic TOC marker, and distinct nested editing scopes.
- ROM-11 consolidates the authoritative contract, explicitly resolves provisional conflicts, freezes runtime same-document proof and partial-prose insertion shape, requires resource-complete/proven-reuse insertion, and defines atomic application plus implementation/test obligations.

Authoritative contract:

- [M25A_TRANSFER_ARCHITECTURE_CONTRACT.md](M25A_TRANSFER_ARCHITECTURE_CONTRACT.md)

Supporting evidence/rationale:

- `M25A_CURRENT_TRANSFER_AUDIT.md`
- `M25A_DOCUMENT_FRAGMENT_TRANSFER_CONTRACT.md`
- `M25A_RESOURCE_CLOSURE_POLICY.md`
- `M25A_IDENTITY_REFERENCE_POLICY.md`
- `M25A_COMPOSITE_CONTENT_TRANSFER_POLICY.md`

No new ADRs: the final consolidated contract captures the architecture without duplicating issue-specific decisions. Production sources and tests remain unchanged during M25A consolidation. M25B requires its own implementation and manual Minecraft QA before acceptance.

## M25B-A - Core Fragment Model

Status: complete. M25B Semantic Document Transfer Implementation remains IN PROGRESS, not complete.

- Added pure Java `dev.rgcb.scholar.transfer` values: closed Blocks/InlineSegments/ResourcePrimary content, DocumentFragment, typed source keys and derived/validated identity indexes.
- Resources preserve complete ScientificDataset values and source identities. Duplicate typed provided identities are rejected; same spelling across namespaces remains distinct. Shared dataset bindings can refer to one resource entry.
- Added opaque runtime document tokens, explicit immutable source target/dataset witnesses and reference export text, and destination context values. Token matching establishes only live-document relation, not per-target applicability.
- Added operational diagnostic values without an engine/result pipeline, persistence metadata, layout state, transient editor state, or semantic reference degradation.
- Existing immutable AST values are retained exactly. Figure remains one owner root; TOC remains a marker; math clipboard stays local and distinct.
- Added 54 focused value-model/boundary tests. Full suite: 1190 tests, zero failures/errors/skips. `gradlew.bat test` and `gradlew.bat build` pass.
- No existing production types, clipboard behavior, editor/history semantics, UI, or tests were migrated. Extraction, closure discovery, remap planning, insertion and clipboard integration are deferred. No new ADR is needed for implementing the accepted contract.

This was a value-model-only checkpoint; [the final M25B report](M25B_TRANSFER_IMPLEMENTATION.md) governs the completed pipeline.

## M25B-B - Transfer Context + Fragment Extraction

Status: complete. M25B remains IN PROGRESS; M25B-C is not started.

- Added pure source FragmentExtractor, semantic FragmentExtractionRequest and explicit immutable Success/Failure extraction results.
- Added an unused editor-side adapter using EditorSelectionValidator and existing InlineContentEditor slicing. Whole BlockSelection stays one block; explicit core requests support ordered collections. Partial Paragraph/Heading and multi-block text ranges produce InlineSegments, preserving marks, reference atoms and empty boundaries.
- Reused identity enumeration for ordered dataset dependency discovery through Table/Plot/Figure ownership. Required complete datasets travel once by source ID; missing required datasets fail without a partial fragment. Resource-primary copies do not pull consumers.
- Captured supplied runtime token, original semantic target/resource witnesses and separate source textual reference exports. No AST references are rewritten/degraded; Figure/Diagram ownership and semantic TOC markers remain intact.
- Scoped existing DocumentValidator checks to selected owned values/resources, with ambiguity rejection for involved source IDs. Warnings can survive without repair; unsupported nested selections are explicit failures, not owner widening.
- Added 53 focused test executions plus a 75-case deterministic mixed extraction replay. Full test suite: 1243 tests, zero failures/errors/skips; test/build pass.
- Existing EditorSession, clipboard/action paths, model types, UI and tests remain unmigrated. Destination ID allocation/remapping/resource reuse/insertion/history/persistence are deferred. No new ADR is required.

This was an additive extraction checkpoint, not independent manual acceptance.

## M25B-C - Resource Closure + Identity Remap Planning

Status: complete. M25B remains IN PROGRESS; M25B-D is not started.

- Added pure TransferPlanner and immutable identity/resource/reference plans, TransferPlan and PlanningResult.
- Extended context with semantic scopes and caller-approved removed-root survivor facts, retaining original destination snapshot.
- Namespace-aware inventory and deterministic pre-reservation preserve free IDs and suffix colliding IDs without mutating content.
- Dependency datasets reuse only with explicit token and exact applicable witness; resource-primary imports never reuse. Unproven resources transfer as new; missing snapshot/proof fails without partial plan.
- Internal reference occurrences use shared remap; proven external occurrences preserve; others plan Text degradation with warnings and captured export, never sentinel IDs.
- Extracted only existing EditorSession collision loops to a shared pure allocator; legacy normalization and clipboard behavior remain unchanged.
- Added 41 test executions, including a 75-case fixed-seed collision replay. Full suite: 1284 tests, zero failures/errors/skips. Test/build and diff checks pass.
- No AST materialization, insertion, clipboard migration, history integration, UI, persistence or new ADR. No commit/push or manual Minecraft acceptance claimed.

This was a planning checkpoint, not an additional roadmap milestone.

## M25B - Semantic Document Transfer Implementation

Status: COMPLETE with automated acceptance; manual Minecraft QA remains pending. No additional D/E/F/G milestones are introduced. M26 is not started.

- Completed pure plan materialization, shared identity/binding rewrites, exact dataset additions/reuse, safe external-reference Text degradation and Figure/Diagram/Equation/TOC preservation.
- Extended existing DocumentEditor helpers for ordered block collections and InlineSegments, including boundary-only ranges, destination left ownership, valid final selection and one final authoring fallback.
- Added editor receiving-context/stale-state preflight, zero-ERROR DocumentValidator and valid-selection candidate gates before a single history commit.
- Migrated authoritative Copy/Cut/Paste to fragment carriers. Cut writes before deletion and guards reentrant source changes. Rich rejection cannot bypass semantics through text fallback; local math and explicit table-cell text conversion remain specialized.
- Removed duplicated session semantic remappers; retained old carriers as shape-only compatibility adapters and existing external serializers/importers.
- Added 45 test executions: eight materializer cases and 37 integration cases, including twelve root families, golden mixed collision/resource/reference scenario, 50-case fixed-seed full replay, exact Undo/Redo, negative atomicity and development fixture checks. Existing regressions remain enabled; migration assertions reflect the new carrier/safe reference policy.
- Full suite: 1329 tests, zero failures/errors/skips. Test/build and diff checks pass; transfer core boundary passes.
- Added M25 development fixtures and exact manual QA steps. No manual acceptance, M26 work, ADR, commit or push claimed.

Final report: [M25B_TRANSFER_IMPLEMENTATION.md](M25B_TRANSFER_IMPLEMENTATION.md). Historical A/B/C reports describe their checkpoint states; this report governs current implementation status.

## M25 Acceptance Before M26

User confirmed M25 complete/accepted, including final manual Minecraft QA, the 1329-test
baseline, golden transfer and fixed-seed replay, build and core boundary. Earlier pending-QA
wording above records implementation checkpoints, not the current accepted baseline.

## M26 - Document Persistence

Status: technically COMPLETE after automated acceptance; manual Minecraft QA ready/pending.
This is one milestone, not an M26A/B/C chain. M27 is not started.

- Audited all current semantic block/inline/math/dataset/plot/diagram families, existing
  serializers and M24/M25 ownership/history contracts; no suitable complete native codec existed.
- Added explicit Scholar JSON V1 schema mapping and version dispatch, generic structured
  results/diagnostics, strict bounded parsing and zero-ERROR DocumentValidator save/load gates.
- Preserved IDs, marks, token segmentation, BigDecimal values/scale, shared root datasets,
  bindings, nested Figure content and complete generic/electrical/mechanical diagram semantics.
- Excluded runtime tokens, history, selection, layout, derived numbering/labels/views and UI.
- Added safe application-directory `.scholar.json` storage, filename/path validation,
  temporary flushed writes, atomic replacement/documented fallback and failure-safe loading.
- Added DocumentWorkspace saved-value dirty baselines, fresh session/history/provenance,
  valid default selection, minimal New, native File actions/picker, overwrite confirmation
  and explicit unsaved Save/Discard/Cancel; Save/Save As add no history.
- Added 96 test executions: codec golden/independent wire fixture/all math/malformed cases,
  75-case fixed-seed round trips, canonical output, storage/failure/fallback/traversal tests,
  workspace dirty/load/nested editing and loaded M25 transfer/provenance regression.
- Full suite: 1425 tests, zero failures/errors/skips. Test/build pass; persistence core
  boundary passes. Complex development fixture and twelve-step manual QA procedure exist.
- No M27, cloud/network/autosave, repair framework, plugin schema, commit or push.

Report: [M26_DOCUMENT_PERSISTENCE.md](M26_DOCUMENT_PERSISTENCE.md). Automated acceptance
does not claim manual Minecraft acceptance or client-startup QA.

## M26 Acceptance Before M27

User confirmed M26 COMPLETE / ACCEPTED, including final manual Minecraft QA and the
1425-test/build/persistence baseline. Earlier pending-QA wording records implementation
history rather than the current accepted baseline.

## M27 - Editor Hardening & Foundation V2

Status: automated hardening implemented; final manual Minecraft acceptance pending.
One milestone, no M27A/B/C roadmap split and no following scientific feature work.

- Added six deterministic mixed/large/data/diagram stress profiles and development command selectors.
- Recorded synthetic CPU baseline before production fixes; measured layout, local edit/reflow,
  reference/structure/hit/selection/scroll, codec, actual filesystem and larger transfer behavior.
- Fixed Unicode logical offsets and cluster wrapping in prose/tables, atomic reference hit/caret/
  selection/navigation geometry, very narrow TOC extents and dataset-bound Figure Plot layout.
- Added measured block-level viewport culling; retained synchronous full layout and snapshot history.
- Hardened tall-object visibility, exclusive menu input/keyboard activation, overflow title access,
  popup/tooltip origins and transient gesture/menu cancellation on resize.
- Preserve exact unrepresentable dataset decimals; omit nonfinite plot points and emit bounded warnings.
- Added 31 executions, including all-profile reflow/owner hits, 100 exact mixed history transactions,
  golden persisted nested session, fixed-seed mixed editing, 100-root transfer and large file round trips.
- Full suite: 1456 tests, zero failures/errors/skips; build passes. V1 schema and core boundaries unchanged.
- Fifteen-step Minecraft QA is ready, not performed or accepted automatically. No commit/push.

Report: [M27_EDITOR_HARDENING.md](M27_EDITOR_HARDENING.md).

## M27 Acceptance Before M28

User confirmed M27 COMPLETE / ACCEPTED, including final Minecraft manual QA. Earlier
pending-QA wording records the automated implementation checkpoint, not current status.

## M28 - Productization & Visual Polish

Status: technically complete; final Minecraft visual QA pending.

- Resolved whole-equation and dataset-backed Plot/Figure plain-text fallback gaps.
- Gated development commands with NeoForge's production environment flag while preserving
  the visual and stress fixtures for development QA.
- Replaced normal Plot, Diagram, and Dataset demo data with minimal valid authoring defaults.
- Unified action shortcut display and GLFW matching through one platform-neutral descriptor.
- Extracted viewport-safe modal geometry, shared document scrolling policy, and contextual
  diagram-menu filtering without replacing the screen or EditorSession architecture.
- Removed fixture numbering duplication, strengthened H3-H6 hierarchy, compacted TOC indent,
  corrected the complex Root fixture and restored relation spacing after MathGroup.
- Preserved table/plot/Figure visual language and existing clipping/wrapping contracts;
  improved deterministic mechanical insertion placement.
- Added focused productization, shell, visual-fixture, math/layout and integrated
  persistence/transfer/history regressions.

Report: [M28_PRODUCTIZATION_AND_VISUAL_POLISH.md](M28_PRODUCTIZATION_AND_VISUAL_POLISH.md).
No M29 work, commit, or push was included in the M28 implementation checkpoint.

## M28 Acceptance Before M29

M28 was manually accepted after the final electrical-label, mechanical-dimension-label and equation-caret visual QA pass.

## M29 - Application Architecture & Workspace

Status: technically complete; final Minecraft manual QA pending.

- Added a production `/scholar` Home and a multi-document repository/workspace layer.
- Separated stable application document identity and recoverable workspace metadata from the semantic AST.
- Reused M26 Scholar JSON V1 documents under `scholar/documents` and added a disposable `workspace.json` index.
- Added neutral collision-safe New, Open, Save, Save As, Rename, Close/Home and Save/Discard/Cancel flows.
- Added lightweight content-derived previews and recency ordering without per-frame document loading/layout.
- Added a compact production header and data-driven File/Home/Insert/Data/Figure/Diagram/View shell over existing actions.
- Preserved fresh history/runtime tokens per open, M25 cross-document transfer, M26 persistence, M27 transaction/input and M28 development gating.

Report: [M29_APPLICATION_ARCHITECTURE_AND_WORKSPACE.md](M29_APPLICATION_ARCHITECTURE_AND_WORKSPACE.md).
M30 was not started at this checkpoint.

## M29 Acceptance Before M30

M29 was manually accepted after the final Home/ribbon application-shell QA and responsive
Diagram/View/File/Insert/Data polish pass.

## M30 - Scientific Document Typesetting

Status: technically complete; final Minecraft manual QA pending.

- Added physical page settings in integer micrometres, Letter/A4/Legal/custom paper, portrait/
  landscape orientation, margins, one/two-column policy, minimal headers/footers/page numbers,
  semantic PageBreak, and Figure/Table column/full-page spans.
- Added derived paginated pages/columns, automatic paragraph continuation, atomic scientific-block
  placement, full-width scientific content, paginated TOC hit geometry, and clipped page-sheet rendering.
- Added controlled font families/sizes, underline/superscript/subscript, paragraph alignment,
  justification, spacing and indentation, layered semantic styles, and Figure Caption style use.
- Added Blank and IEEE-style Scientific Paper templates plus Home template selection; extended Home,
  Layout, and View ribbon actions without changing command authority.
- Added transient zoom/Fit controls that do not mutate layout, history, transfer, or persistence.
- Evolved native persistence to canonical V2 while retaining strict V1 decoding/default migration.
- Extended M25 materialization and editor reconstruction to preserve M30 semantic formatting while
  keeping destination document settings authoritative.
- Added focused M30 integration coverage and a deterministic save/reopen/layout/transfer/undo golden
  scenario. Full checkpoint suite: 1,525 tests, zero failures/errors/skips.

Report: [M30_SCIENTIFIC_DOCUMENT_TYPESETTING.md](M30_SCIENTIFIC_DOCUMENT_TYPESETTING.md).
No commit/push or manual Minecraft acceptance is claimed by this implementation checkpoint.

## M30 Acceptance Before M31

M30 was manually accepted after the semantic LayoutSectionBreak and ContentSpan.AUTO
extensions and final layout QA. The checkpoint status above is historical.

## M31 - Scientific Units & Quantities

Status: complete and manually accepted.

- Added a structured M31 unit registry, quantity value/semantics, conversion and number formatting.
- Preserved the distinction between absolute temperature and temperature difference, including
  `°C` versus `Δ°C` and `K` versus `ΔK`.
- Integrated quantities with inline/math presentation, dataset units, plots, transfer, and V2
  persistence without storing derived conversions as independent document truth.
- Final scientific/engineering formatting omits redundant `×10^0`.

The M31 model is documented alongside the [temperature semantics follow-up](PRE_M32_TEMPERATURE_QUANTITY_SEMANTICS.md).

## M32 - Scientific Variables & Computed Content

Status: complete and manually accepted.

- Added stable-ID `VariableDefinition` and authored `ComputedResult` expressions with name-to-ID
  binding, derived BigDecimal evaluation, diagnostics, and snapshot-safe history.
- Preserved quantity temperature semantics and added dependency-aware M25 transfer; unproven
  cross-document dependencies reject rather than silently bind by name or coincidental ID.
- V2 persistence stores authored variables/expressions and IDs, never cached results.
- Production unit authoring uses the M31 registry-backed picker; caret geometry after atomic
  computation blocks was corrected and manually checked.

Report: [M32_SCIENTIFIC_COMPUTATION.md](M32_SCIENTIFIC_COMPUTATION.md).

## M33 - Scientific Data Analysis

Status: complete and manually accepted.

- Added authored `DatasetAnalysisBlock` and stable-ID fit overlays on plot series.
- Derived descriptive statistics and bounded line/quadratic/cubic regression from document-owned
  datasets without persisting results or generated curve samples.
- Integrated analysis dependency diagnostics, editor transactions, resource closure, transfer,
  V2 persistence, and plot rendering. Column links remain ID-based through rename.

Report: [M33_SCIENTIFIC_DATA_ANALYSIS.md](M33_SCIENTIFIC_DATA_ANALYSIS.md).

## M34 - High-Fidelity Document Rendering

Status: complete and manually accepted, including final Minecraft visual QA.

- Improved the document-only text profiles and scientific stroke rendering without changing
  Minecraft shell fonts or semantic layout state.
- Added shared `DocumentViewTransform` geometry for zoom, clipping, mouse coordinates and the
  screen-space caret; corrected selection and cross-column paragraph bounds/hit testing.
- Added transient status-bar page/word-count/zoom controls, a production M34 Readability Sample,
  and guarded Home document deletion. Caret optical metrics and blink were visually refined.
- Automated tests and build passed at the accepted implementation checkpoint; this pre-M35
  consolidation reruns the complete verification and records the current test count.

Report: [M34_HIGH_FIDELITY_RENDERING.md](M34_HIGH_FIDELITY_RENDERING.md).

## M35 - Scholar API & Addon Framework

Status: complete and manually accepted. The separate NeoForge reference addon used the public
Scholar API to create a document, dataset-backed table, Figure plot, and quadratic analysis,
then persisted the result. M35 was merged and pushed to `main` at
`fe459ed50c05d9d5086f457bd57ab497f098555f`. See [M35_SCHOLAR_API.md](M35_SCHOLAR_API.md).

## Next

M36 Import / Export & Interchange is complete and manually accepted. CSV import/export,
Markdown export, and PDF export were verified through `/scholar`; the milestone was merged
and pushed to `main` at `78fc7b7bb8cab0609cf62a5e91bb0505bed267b5`.
See [M36_IMPORT_EXPORT.md](M36_IMPORT_EXPORT.md).

M37 Authoring UX & Document Workflow is in technical development on
`codex/m37-authoring-ux`. It requires a normal production `/scholar` authoring session
before acceptance. See [M37_AUTHORING_UX.md](M37_AUTHORING_UX.md).
