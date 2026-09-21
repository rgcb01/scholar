# Roadmap

This roadmap is milestone-based and deliberately revisable. It does not plan the full physics simulator or any separate STEM laboratory project.

## Current Implementation Status

The repository has advanced substantially beyond the original early-milestone outline below. The historical milestone descriptions are retained as design history, while current implementation status is summarized here:

- M0-M4 foundations are complete: repository/document core, Markdown interchange, read-only rendering, and scientific math foundation.
- The document Writing Engine v1 is complete through the M14 hardening work, including structured prose editing, selection/history/clipboard, visual-line navigation, equations, and structured math authoring.
- M15 Tables is complete: semantic tables, layout/rendering, cell editing, row/column operations, native clipboard + TSV fallback, and restricted Markdown table interchange.
- M16 Scientific Plots is complete through M16G hardening and final full-Gradle/manual regression validation: semantic static XY plots, auto-range/ticks/transforms, LINE/SCATTER rendering, plot editing, native clipboard, and responsive/extreme-numeric hardening.
- M17 Scientific Diagram Foundation is complete through M17G with full manual regression acceptance: semantic canvas/nodes/ports/connections, deterministic routing, hit testing, embedded editing/dragging, structural editing, native clipboard, and responsive/degenerate-geometry hardening.
- M18A-M18G are complete/manual QA accepted: electrical architecture, static symbols, orientation/hit testing, full authoring, explicit junctions + derived nets, workspace scaling, clipboard/interchange regression, and final hardening.
- M18 is complete/manual QA accepted through M18H.1 final terminal-length polish.
- M19A through M19F are complete/manual QA accepted: primitives, dimensions/callouts, constraints, symbols, annotations, assemblies and part references.
- M20 Figures & Scientific Media is complete/manual QA accepted: semantic figures wrap existing plots/diagrams, derive display numbering from document order, store editable captions, render content plus generated captions, and support native whole-figure clipboard with stable-ID remapping.
- M21 Cross-References is complete/manual QA accepted: semantic references resolve Figure, Table, Equation, and Section targets from stable IDs.
- M22 Document Structure & Navigation is complete/manual QA accepted: headings derive hierarchical section numbers, section references share that numbering, TOC blocks derive entries automatically, and the editor includes an outline panel.
- M23 Scientific Data & Datasets is complete/manual QA accepted: documents own reusable datasets, tables and plots can resolve dataset-backed views, and dataset edits propagate through views.
- M24A Document Model Invariants & Validation Foundation is implemented with automated validation complete: a pure Java validator reports structural errors and degraded-state warnings without mutating documents.
- M24B Selection & Caret Foundation is implemented with automated validation complete: transient editor selections are centrally validated, text selections cannot cross atomic blocks, `Ctrl+A` targets the active editing scope, and mixed-document navigation has deterministic regression coverage.
- M24C Right-Click / Context Menu Foundation is implemented with automated validation complete: context menus reuse `EditorAction`, preserve valid selections, and stay transient Minecraft shell UI.
- M24D Structural Editing Hardening is implemented with automated validation complete: structural edits are documented, ID-sensitive paste/split paths are hardened, and validator-backed sequence/golden/randomized regressions cover current structural operations.
- M24E Undo/Redo & Transaction Hardening is implemented with automated validation complete: history depth is testable, one semantic edit maps to one transaction, transient UI/navigation state does not create undo entries, redo restores stable-ID snapshots deterministically, and validator-backed history regressions cover current editor systems.
- M24F Input, Focus & Interaction Consistency is implemented with automated validation complete: focus ownership, input priority, popup isolation, scoped text input, and transient nested-editor transitions are documented and regression-tested.
- M24G Final Editor Foundation Regression & Documentation is implemented with automated validation complete: the current foundation is covered by canonical mixed-document integration tests, seeded cross-subsystem replay, manual QA fixtures, and foundation v1 documentation.
- M24 Editor Core Foundation V1 is complete and accepted, including final manual Minecraft QA. The per-slice entries above record their implementation checkpoints.
- M25A Document Transfer & Identity Closure Architecture is complete: the authoritative contract is [M25A_TRANSFER_ARCHITECTURE_CONTRACT.md](M25A_TRANSFER_ARCHITECTURE_CONTRACT.md). No transfer implementation is claimed by architecture completion.
- M25B Semantic Document Transfer Implementation is COMPLETE / ACCEPTED, including user-confirmed manual Minecraft QA. Extraction/planning/materialization, atomic validated insertion and semantic Copy/Cut/Paste are integrated. See [M25B_TRANSFER_IMPLEMENTATION.md](M25B_TRANSFER_IMPLEMENTATION.md) for the historical implementation report.
- M26 Document Persistence is COMPLETE / ACCEPTED, including user-confirmed manual Minecraft QA. Explicit Scholar JSON V1 and local editor lifecycle are integrated; its report preserves the historical automated checkpoint.
- M27 Editor Hardening & Foundation V2 is complete and accepted, including final manual Minecraft QA. One milestone covers stress fixtures, Unicode/atomic reference geometry, Figure dataset layout, viewport culling, scroll/input/resize hardening, large persistence/transfer and long-session regression. See [M27_EDITOR_HARDENING.md](M27_EDITOR_HARDENING.md).
- M28 Productization & Visual Polish is complete and manually accepted. It resolves the accepted productization findings, gates development commands, removes demo insertion defaults, unifies shortcut authority, improves contextual menus and visual hierarchy, and preserves V1 persistence and M24-M27 contracts.
- M29 Application Architecture & Workspace is technically complete with final Minecraft manual QA pending: `/scholar` opens a production Home, user documents have independent application identities, M26 persistence is repository-owned, and each open creates an isolated editor session.


## Current Forward Milestone Plan

### M25A - Document Transfer & Identity Closure Architecture - Complete

ROM-6 through ROM-11 audit and consolidate scoped semantic fragments, whole-dataset closure, namespace-aware materialization, safe references, composite ownership, explicit runtime same-document proof, atomic insertion, and M24 history/selection integration. The final contract supersedes provisional wording in issue-specific M25A documents.

### M25B - Semantic Document Transfer Implementation - Complete / Accepted

Implemented the internal fragment/transfer contract, typed closure/remap/reference handling, structural staging and rich clipboard integration under [M25A_TRANSFER_ARCHITECTURE_CONTRACT.md](M25A_TRANSFER_ARCHITECTURE_CONTRACT.md). Automated and manual QA are accepted. M25 does not define persistence, new selection modes, Figure generalization, graph framework or public plugin API; M26 supplies a separate native schema.

Historical implementation checkpoints: [M25B_A_CORE_FRAGMENT_MODEL.md](M25B_A_CORE_FRAGMENT_MODEL.md), [M25B_B_FRAGMENT_EXTRACTION.md](M25B_B_FRAGMENT_EXTRACTION.md), [M25B_C_TRANSFER_PLANNING.md](M25B_C_TRANSFER_PLANNING.md).

The externally meaningful milestone is M25B Transfer Implementation, not additional D/E/F/G milestones. The final report [M25B_TRANSFER_IMPLEMENTATION.md](M25B_TRANSFER_IMPLEMENTATION.md) supersedes checkpoint deferral/status wording for current implementation.

### M26 - Document Persistence - Complete / Accepted

One milestone covers the explicit versioned `.scholar.json` schema, pure-JVM codec,
validated semantic reconstruction, safe local file replacement, atomic session opening,
saved-snapshot dirty tracking, native file picker/actions and unsaved Save/Discard/Cancel.
All current built-in document/math/dataset/diagram families round trip without identity
remapping. History, selection, runtime proof, layout and derived state are not persisted.
Automated acceptance: 1425 tests, zero failures/errors/skips; build passes. Manual fixture
and twelve-step procedure: [M26_DOCUMENT_PERSISTENCE.md](M26_DOCUMENT_PERSISTENCE.md).
No M26A/B/C roadmap milestones, cloud/autosave, recovery framework, new scientific content
or public API. Final manual Minecraft acceptance is user-confirmed before M27.

### M27 - Editor Hardening & Foundation V2 - Complete / Accepted

M27 is one milestone, not a layout/rendering/performance/UX sub-milestone chain.
Deterministic six-profile stress fixtures, measured baseline, scoped correctness fixes,
block culling, exclusive menu input, resize gesture cancellation, large file/fragment
round trips and exact 100-transaction history are implemented. Full suite: 1456 passing
tests; build passes. Persistence V1 and M24/M25 semantic contracts remain unchanged.
Final Minecraft QA is user-confirmed. The historical procedure remains in [M27_EDITOR_HARDENING.md](M27_EDITOR_HARDENING.md).

### M28 - Productization & Visual Polish - Technical Complete / Manual QA Pending

M28 resolves the ten accepted FIX NOW findings and the concrete visual QA defects without
changing semantic ASTs, transfer policy, persistence V1, or introducing extension frameworks.
The implementation report and concise manual procedure are in
[M28_PRODUCTIZATION_AND_VISUAL_POLISH.md](M28_PRODUCTIZATION_AND_VISUAL_POLISH.md).
No M29 feature milestone starts before manual acceptance.

### M17 - Scientific Diagram Foundation — Complete

- M17A Architecture & Data Model Design.
- M17B Static DiagramBlock + responsive layout/render + atomic selection.
- M17C Ports, connections, deterministic orthogonal routing, and pure-Java hit testing.
- M17D Dedicated diagram editing mode and one-transaction element dragging.
- M17E Explicit structural node/connection editing and label editing.
- M17F Lossless whole-diagram clipboard plus readable plain-text fallback.
- M17G Hardening, regressions, responsive/edge-case QA, and documentation closeout.

M17 deliberately contains no electrical or mechanical domain vocabulary. The completed foundation is now consumed by M18/M19.

### M18 - Electrical Diagrams

- M18A Architecture & Symbol Model Design - accepted/documented.
- M18B Electrical component core, stable terminal schemas, first static symbols, and visual polish: resistor, capacitor, DC voltage source, and ground — complete/manual QA accepted.
- M18C Quarter-turn orientation, terminal/connection integration, mixed-element layout, electrical hit testing, remaining approved basic symbols, and visual polish — complete/manual QA accepted.
- M18D Electrical authoring: insert-by-kind, designator/value editing, rotate, drag, wire creation, deletion, and global undo/redo — complete/manual QA accepted.
- M18E Explicit junction authoring, derived semantic electrical nets, branch connectivity, named-net annotations, and no geometry-based net inference — complete/manual QA accepted.
- M18E.5 Diagram workspace scaling: larger/resizable logical canvas, viewport zoom/pan/Fit, global symbol scale, and variable DiagramBlock height — complete/manual QA accepted.
- M18F Whole-diagram clipboard/plain-text regression for electrical elements and mixed diagrams — complete/manual QA accepted.
- M18G Responsive/edge-case hardening, regressions, documentation, and final manual QA — complete/manual QA accepted.
- M18H/H.1 Final electrical visual polish: compact adaptive electrical terminal exits plus shorter rendered component legs while preserving logical anchors, snapping, hit testing, connection endpoints, and net semantics — complete/manual QA accepted.

M18 models schematic diagrams only. Physical Minecraft components on breadboards/perforated boards remain a separate future STEM Lab representation.

### M19 - Mechanical Diagrams

- M19A Mechanical primitives: line, centerline, rectangle, circle, arc, arrow, and reference point — complete/manual QA accepted.
- M19B Dimensions and callouts — complete/manual QA accepted.
- M19B.1 Generic scrollable dropdown menus for long authoring menus — complete/manual QA accepted.
- M19C Geometric constraints/relationships: horizontal, vertical, coincident, parallel, perpendicular, and concentric — complete/manual QA accepted.
- M19D Mechanical symbols: shaft, gear, bearing, spring, piston, and bolt — complete/manual QA accepted.
- M19E/E.1 Mechanical labels, notes, leader callouts, and annotation visual polish — complete/manual QA accepted.
- M19F Mechanical Assembly & Part References: semantic item balloons linked by stable element IDs plus generated BOM tables — implemented/manual QA pending.
- M19G Hardening — planned.
- M19H Visual polish — planned.

M19 remains schematic/technical-drawing authoring rather than physical simulation. M19C uses a bounded deterministic relationship reconciler, not a general CAD solver.

### M20 - Figures & Scientific Media

- M20A FigureBlock foundation: stable figure IDs, derived display numbering, plot/diagram content, semantic captions, layout/rendering, selection/editing hooks, native clipboard, fixtures, docs, and ADRs — complete/manual QA accepted.
- Image-backed figures, rich media import/export, figure cross-references, figure Markdown syntax, and document-wide media asset management remain deferred.

## Milestone 0 - Repository Foundation

Goal: Establish a professional, maintainable NeoForge project foundation.

Non-goals: No Scholar document features, gameplay systems, UI, Markdown, math, networking, or speculative APIs.

Main deliverables:

- Clean NeoForge 1.21.1 project.
- Minimal mod metadata and entry point.
- Initial repository documentation.
- Basic repository hygiene.
- Initial testing strategy.

Exit criteria:

- `.\gradlew.bat build` succeeds.
- Documentation captures current scope without freezing unresolved decisions.
- The repository remains single-purpose and free of premature feature code.

## Milestone 1 - Document Core

Goal: Create a minimal Minecraft-independent document model.

Non-goals: No Markdown parser, no Minecraft rendering, no WYSIWYG editor, no math rendering.

Main deliverables:

- Initial immutable document model types.
- Basic validation rules for model invariants.
- Unit tests for core document behavior.

Exit criteria:

- Core document logic is testable without launching Minecraft.
- The model can represent a small structured document with text and simple hierarchy.
- Accepted ADRs capture the initial document structure, mutability, identity, text formatting, and extensibility decisions.

## Milestone 2 - Minimal Markdown Interchange

Goal: Support a small Markdown subset for import and export.

Non-goals: No full CommonMark implementation, no HTML passthrough, no math extension, no editor.

Main deliverables:

- Explicit supported Markdown subset.
- Parser from supported Markdown into the document model.
- Serializer from the document model back to supported Markdown.
- Round-trip and edge-case unit tests.
- Recoverable diagnostics for recognizable unsupported or malformed syntax.

Exit criteria:

- Supported Markdown behavior is documented and tested.
- Unsupported syntax is handled predictably.
- `Document -> serialize -> parse -> equivalent Document` round trips are tested.

## Milestone 3 - Read-Only Document Rendering

Goal: Render a minimal document viewer inside Minecraft.

Non-goals: No editing, no custom authoring tools, no complex layout engine, no networking.

Main deliverables:

- Read-only viewer for the minimal document model.
- Basic text layout and scrolling.
- Separation between model logic and Minecraft rendering code.
- Minecraft-independent layout model and text measurement abstraction.
- Temporary development client command for opening the viewer.
- Hardcoded development document parsed through Scholar Markdown.
- Layout unit tests.

Exit criteria:

- A simple document can be viewed in-game.
- Rendering code does not become the document source of truth.
- Layout behavior is covered by ordinary unit tests where Minecraft is not required.
- Manual visual testing confirms readable wrapping, formatting, clipping, and scroll behavior.

## Milestone 4 - Scientific Math Foundation

Goal: Add a small explicit mathematical notation foundation.

Non-goals: No full TeX engine, no CAS, no automatic solving, no broad math-layout completeness effort.

Main deliverables:

- Approved math notation architecture.
- Minecraft-independent Math AST.
- Structural tests for the Math AST.
- Accepted ADRs for math model, syntax separation, layout separation, and integration order.
- Minecraft-independent math layout for the first display equation slice.
- `EquationBlock` display equation integration.
- Minecraft rendering of laid-out math glyphs and fraction rules.

Exit criteria:

- Basic scientific notation can be represented structurally and rendered as a display equation.
- Unsupported notation boundaries are documented.
- Math core remains independent from Minecraft, NeoForge, Markdown, rendering, and document integration.

## Milestone 5 - Editing Vertical Slice

The first document typography slice is complete. Scholar v0.1 uses provisional document fonts while further typography tuning is deferred.

Goal: Build a basic WYSIWYG editing slice against the document model.

Non-goals: No full word processor, no collaborative editing, no advanced plugin API, no broad Markdown editing mode.

Main deliverables:

- Basic in-game editing workflow.
- Simple text and structure edits applied to the document model.
- Save/load path for edited documents.

Exit criteria:

- A user can create or modify a small document without editing raw Markdown.
- Editing operations preserve model invariants.

## Implemented Foundation Through M24G

Scholar now includes semantic document editing, structured math authoring, tables, plots, diagrams, electrical/mechanical diagram vocabularies, figures, first-class cross-references, derived document structure/navigation, first-class reusable datasets, a pure Java document validation foundation, a central selection/caret validity foundation, the first editor-wide right-click context menu foundation, structural editing hardening for current block/nested/resource operations, an explicit undo/redo transaction contract, an accepted input/focus contract for one authoritative consumer per input event, and final foundation-level regression coverage across the current mixed editor surface. Validation UI, repair workflows, persistence migration, and richer document-wide multi-object selection remain future work.
## Milestone 29 - Application Architecture & Workspace

Status: complete and manually accepted.

- Adds a production `/scholar` Home and multi-document application lifecycle.
- Separates application document identity/metadata from semantic AST stable IDs.
- Wraps M26 V1 storage with a recoverable repository index and lightweight previews.
- Creates a fresh workspace, EditorSession, history and runtime transfer token per open.
- Adds New, Open/Home, Save, Save As, Rename and safe Close with unsaved-change handling.
- Uses a compact data-driven production shell while preserving `EditorAction` command authority.
- Keeps development fixtures behind development-only commands.

Report: [M29_APPLICATION_ARCHITECTURE_AND_WORKSPACE.md](M29_APPLICATION_ARCHITECTURE_AND_WORKSPACE.md).

## Milestone 30 - Scientific Document Typesetting

Status: technically complete; final Minecraft manual QA pending.

- Adds deterministic physical paper, margins, orientation, one/two-column flow, automatic pagination,
  semantic PageBreak, and Figure/Table column/full-page spans.
- Adds controlled scientific typography, paragraph formatting, semantic styles, Blank/IEEE-style
  templates, Home template selection, Layout ribbon commands, and view-only zoom/Fit controls.
- Production editor/viewer render distinct page sheets from paginated layout; zoom does not affect
  wrapping, page count, semantic state, persistence, transfer, or history.
- Scholar JSON V2 stores M30 semantics while retaining strict V1 loading and deterministic defaults.
- M25 transfer preserves authored formatting/breaks/spans but never transfers document page policy
  with ordinary copied blocks.

Report: [M30_SCIENTIFIC_DOCUMENT_TYPESETTING.md](M30_SCIENTIFIC_DOCUMENT_TYPESETTING.md).
