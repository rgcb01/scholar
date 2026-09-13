# Post-M24 Technical Debt Register

Status: planning document only. This is not an accepted implementation plan.

Severity scale:

- P0: blocks safe future development in the related area.
- P1: should be resolved before the related subsystem grows.
- P2: real debt but safely deferrable.
- P3: cleanup or polish.

## P0

### Native Persistence Schema Is Not Ready To Freeze

- Subsystem: persistence/document model
- Issue: Scholar has no accepted native schema, document metadata/version boundary, migration policy, unknown-node policy, or resource closure model.
- Evidence: Markdown exists only as a restricted interchange subset; full documents include figures, datasets, diagrams, plots, references, and IDs that Markdown cannot serialize losslessly.
- Consequence if ignored: a native file format could accidentally encode current Java implementation details and become expensive to migrate.
- Recommended treatment: complete persistence architecture before implementing native save files.
- Prerequisite/dependency: document transfer/resource closure decisions.
- Ideal milestone: M26A.
- Requires migration: no if handled before first native format; yes if deferred until after files exist.

## P1

### EditorSession Is Becoming The Central Block-Specific Coordinator

- Subsystem: editor/session
- Issue: `EditorSession` coordinates actions, clipboard, nested editing, paste, relayout, and ID remapping, with many block-specific paths.
- Evidence: `EditorSession.java` is roughly 3000 lines and contains specialized support for tables, plots, diagrams, figures, datasets, equations, inline content, and math clipboard payloads.
- Consequence if ignored: every new scientific object will add more local branches, increasing regression risk and making feature ownership unclear.
- Recommended treatment: extract transfer/clipboard/remap services first; later consider explicit block capabilities if growth continues.
- Prerequisite/dependency: M25 transfer architecture.
- Ideal milestone: M25B.
- Requires migration: internal refactor only.

### Clipboard Payloads Do Not Yet Model Document Resource Closure

- Subsystem: clipboard/interchange
- Issue: clipboard payloads are specialized and do not share a general model for selected content plus required resources and ID relationships.
- Evidence: document block, inline, math, table, plot, diagram, figure, and dataset payloads are handled through separate branches.
- Consequence if ignored: copying dataset-backed views, figures, references, or future media will produce more ad hoc copy/paste policies.
- Recommended treatment: introduce a scoped `DocumentFragment` or `DocumentTransfer` model for clipboard/structural transfer.
- Prerequisite/dependency: stable ID remapping policy.
- Ideal milestone: M25A/M25B.
- Requires migration: no document migration; clipboard internals change.

### Stable ID Remapping Is Scattered Across Paste Paths

- Subsystem: identity/clipboard/editor
- Issue: ID uniqueness and remapping are implemented in multiple local methods.
- Evidence: `EditorSession` contains unique/remap paths for headings, equations, tables, datasets, figures, and block paste.
- Consequence if ignored: cross references, dataset bindings, figure references, and future citations/media can drift or break during paste/import.
- Recommended treatment: centralize ID allocation/remapping and define remap scopes.
- Prerequisite/dependency: document transfer closure.
- Ideal milestone: M25B.
- Requires migration: no, if semantic IDs remain stable.

### Figure Composition Is Too Narrow For Future Scientific Media

- Subsystem: figure/media/document model
- Issue: `FigureBlock` currently accepts only plot or diagram content.
- Evidence: constructor validation rejects all content except `PlotBlock` and `DiagramBlock`.
- Consequence if ignored: images, videos, simulations, and external media will either special-case Figure again or bypass it.
- Recommended treatment: before adding more media, define whether Figure is a captioned wrapper for a bounded set of figure content types or whether media blocks should stand alone.
- Prerequisite/dependency: media/persistence resource decisions.
- Ideal milestone: before new image/media features.
- Requires migration: possibly, if Figure shape changes after persistence.

### Accidental Public API Surface Is Too Broad

- Subsystem: API/extensibility
- Issue: many implementation records/classes are public without an explicit supported API contract.
- Evidence: hundreds of public types exist across core/editor/model packages; there is no `api` package or stability annotation.
- Consequence if ignored: other mods may depend on internals before Scholar can support compatibility.
- Recommended treatment: define public API policy after transfer and persistence design stabilize.
- Prerequisite/dependency: native schema and block capability decisions.
- Ideal milestone: after M26B, before external mod API promotion.
- Requires migration: possible API source compatibility break if delayed too long.

## P2

### Validation Is Centralized But Not Extensible

- Subsystem: validation
- Issue: `DocumentValidator` and `EditorSelectionValidator` know concrete built-in node types.
- Evidence: validation uses type checks for headings, equations, tables, plots, diagrams, figures, datasets, and selections.
- Consequence if ignored: acceptable for built-in nodes, but incompatible with third-party block extensions.
- Recommended treatment: keep as-is until extension API work; do not generalize prematurely.
- Prerequisite/dependency: decision on third-party block support.
- Ideal milestone: API stabilization phase.
- Requires migration: internal.

### Layout Engine Is Centralized And Scroll-Only

- Subsystem: layout/rendering
- Issue: `DocumentLayoutEngine` handles each block type directly and assumes a scroll-oriented document surface.
- Evidence: one central layout class computes layout for headings, text, equations, tables, plots, diagrams, figures, and TOC.
- Consequence if ignored: pagination/export, long tables, media, and accessibility scaling will be harder later.
- Recommended treatment: defer redesign; harden after persistence and transfer are stable.
- Prerequisite/dependency: media/resource and document format decisions.
- Ideal milestone: M28.
- Requires migration: no semantic migration.

### Snapshot History May Become Heavy With Large Documents

- Subsystem: history/performance
- Issue: `EditorHistory` stores full `EditorState` snapshots.
- Evidence: current history model is intentionally snapshot-based and deterministic.
- Consequence if ignored: memory pressure when documents include large datasets/media or very large block counts.
- Recommended treatment: keep now; measure before optimizing; consider structural sharing/delta only with data.
- Prerequisite/dependency: performance instrumentation.
- Ideal milestone: M29.
- Requires migration: internal.

### Markdown Is Not A Full Fidelity Scholar Format

- Subsystem: interchange/persistence
- Issue: Markdown serializer cannot represent many block/resource types and intentionally resolves some semantics to plain text.
- Evidence: plot, diagram, and figure serialization are unsupported; cross references serialize as resolved display text.
- Consequence if ignored: risk that Markdown is misused as native persistence.
- Recommended treatment: document this boundary wherever persistence is designed.
- Prerequisite/dependency: M26A.
- Ideal milestone: M26A.
- Requires migration: no.

### Repeated Block Type Switches Will Scale Poorly

- Subsystem: cross-cutting model/editor/layout
- Issue: adding a new block type requires edits in multiple systems.
- Evidence: repeated type checks across validation, layout, selection validation, clipboard, actions, and rendering.
- Consequence if ignored: feature work becomes patchwork.
- Recommended treatment: introduce explicit capability descriptors only when the next 2-3 block families demand it; do not invent a full plugin system now.
- Prerequisite/dependency: M25 transfer work.
- Ideal milestone: M28 or API stabilization, unless new media forces it sooner.
- Requires migration: internal.

## P3

### Built-In Action Registry Is Growing Large

- Subsystem: actions/UI
- Issue: `BuiltInEditorActions` contains many concrete action definitions, especially for diagrams.
- Evidence: diagram, table, plot, document, and formatting actions are all registered centrally.
- Consequence if ignored: discoverability degrades, but behavior remains correct.
- Recommended treatment: split by action domain when editing the action system again.
- Prerequisite/dependency: none.
- Ideal milestone: editor UX stabilization.
- Requires migration: no.

### Development Fixtures Are Heavy

- Subsystem: tests/dev documents
- Issue: large all-feature development documents are useful but can hide focused fixture intent.
- Evidence: development fixtures cover many current systems at once.
- Consequence if ignored: regression tests may become brittle and hard to diagnose.
- Recommended treatment: keep broad fixtures, add smaller named fixtures for transfer, persistence, and layout tests.
- Prerequisite/dependency: none.
- Ideal milestone: as part of M25/M26 tests.
- Requires migration: no.

### Public Concrete Types Need Stability Labels

- Subsystem: API/docs
- Issue: public classes are not labeled as stable/internal/experimental.
- Evidence: no explicit API boundary or annotations exist.
- Consequence if ignored: user/modder expectations may form around unstable internals.
- Recommended treatment: add API documentation policy before external release.
- Prerequisite/dependency: API phase.
- Ideal milestone: API stabilization.
- Requires migration: no document migration; possible source compatibility work.

