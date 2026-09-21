# M28 Productization Technical Audit

Status: technical audit complete; no implementation performed.

This report audits the Foundation V2 working tree as it exists after M27. It does not
accept an M28 implementation plan, change behavior, or redefine the V1 persistence
format. The repository had uncommitted M25-M27 work before this audit; that work is
treated as the implementation baseline and is not attributed to M28.

## A. Executive Summary

Scholar has a strong semantic core, explicit immutable models, pure layout for the
scientific surface, validated transfer and persistence boundaries, and unusually broad
behavioral coverage. It is not in need of a rewrite or a generic node/plugin framework.

The remaining debt is concentrated rather than systemic. One existing clipboard path
loses the plain-text representation of whole equation blocks. Dataset-backed plot and
figure fallbacks also serialize unresolved plot values. The Minecraft editor screen and
`EditorSession` have become very large coordination surfaces, development commands and
fixtures ship without a release gate, physical shortcut matching is duplicated outside
action metadata, and several authoring commands still create milestone demo content.

The smallest coherent M28 should fix those concrete productization defects, establish a
small reusable host-modal/input boundary, and clean the development/release boundary. It
should not introduce a universal dispatch registry, redesign the AST, change V1, or
generalize Figure before a real media feature is designed.

### Finding Counts

| Classification | Count |
| --- | ---: |
| FIX NOW - BLOCKING | 1 |
| FIX NOW - IMPORTANT | 6 |
| FIX NOW - MINOR | 3 |
| KEEP | 14 |
| DEFER | 11 |
| VISUAL QA | 8 |

### Five Highest-Value Findings

- Whole `EquationBlock` copy has an empty OS plain-text fallback.
- Dataset-backed plots and figures export authored empty point lists instead of their
  resolved dataset view.
- `ScholarEditorScreen` is a 3,194-line modal/input/render coordinator with repeated
  popup geometry and event handling; one more UI-heavy feature will compound it.
- `scholar_dev_viewer` and `scholar_dev_editor` plus large fixtures ship in main source
  and are registered on every client with no development gate.
- Action shortcut labels are metadata, but actual key matching is independently hardcoded
  in `ScholarEditorScreen`, allowing menu hints and behavior to diverge.

## B. Repository Areas Inspected

The audit covered all 374 public production types and the package families under
`document`, `math`, `data`, `plot`, `diagram`, `electrical`, `mechanical`, `validation`,
`editor`, `transfer`, `clipboard`, `persistence`, `markdown`, `layout`, `typography`, and
`client`. It also inspected Gradle configuration, resources, `.gitignore`, 132 test source
files, current test XML timing, M24-M27 reports/contracts, the post-M24 audits, project
specification, roadmap, milestone history, and relevant ADRs.

Production size is concentrated in four coordinators:

| Type | Lines | Observation |
| --- | ---: | --- |
| `ScholarEditorScreen` | 3,194 | host rendering, input priority, modal state, menu/action assembly, file lifecycle |
| `EditorSession` | 2,842 | semantic orchestration for every nested editor and document resource |
| `MathExpressionEditor` | 1,756 | intentionally specialized structural math transformation engine |
| `DocumentEditor` | 1,222 | document text/structural mutations and insertion defaults |

## C. Hardcoded-Value Inventory

| Area and evidence | Classification | Finding |
| --- | --- | --- |
| `ScholarTypography.defaultProfile()` | A: legitimate policy profile | Spacing, readable width, page margin and role colors have one owner. KEEP K2. |
| `PlotLayoutEngine` and `DiagramLayoutEngine` named constants | A: legitimate local constants | Geometry values are close to the algorithms they control and are tested. KEEP K1. |
| `ElectricalSymbolLibrary` normalized coordinates | F: compatibility/domain requirement | These are symbol definitions, not arbitrary UI magic. KEEP K5. |
| `DocumentJsonCodec.MAX_CHARACTERS`, `MAX_DEPTH`, `FileDocumentStorage.MAX_BYTES` | F: format/safety | Bounded parsing and storage are deliberate V1 safety limits. KEEP K4. |
| `EditorHistory.DEFAULT_CAPACITY = 100` | UX/performance default | Explicit and injectable; no evidence currently justifies a setting framework. KEEP K10. |
| `DiagramViewport.MIN_ZOOM/MAX_ZOOM` | UX safety | One authority owns clamping and validation. KEEP K11. |
| `ScholarEditorScreen` popup widths/heights and repeated field/button offsets | B/D | Five custom popup families repeat a modal layout policy. FIX NOW I3. |
| `ToolbarWidget.keyPressed(256)` | C | Raw GLFW Escape value is owned by a widget while peers use GLFW names. FIX NOW M3. |
| `SCROLL_STEP = 24` in viewer and editor | B | Duplicated shell policy is harmless today but should have one owner. FIX NOW M2 with scroll cleanup. |
| Plot, diagram and dataset sample values in production insertion methods | D | Milestone fixtures became normal authoring defaults. FIX NOW I5. |

No evidence supports centralizing every padding or scientific geometry number into one
theme object. That would reduce locality and create abstraction debt.

## D. User-Visible String Inventory

Action labels and tooltips primarily live in `BuiltInEditorActions`, which is the right
semantic owner. File action labels are created separately in `ScholarEditorScreen`.
Dialog titles, labels, validation hints and buttons are embedded throughout
`ScholarEditorScreen` and `ScholarFileDialog`; persistence and validation diagnostics are
also English literals at their source.

This is internally consistent enough for a pre-localized project, but it is not ready for
localization. Localization is DEFER D6: first reduce modal duplication and define whether
Minecraft `Component` keys belong only in client code. M28 should avoid a broad string
catalog migration. It should remove obvious development-facing product labels such as
`Scholar Development Editor` only when the dev/release entry point is separated.

## E. Visual Constant Ownership

`ScholarShellStyle` coherently owns shell colors. `ToolbarLayout`, `ContextMenuLayout`,
`MenuBarWidget`, and `ScholarShellLayout` own most chrome dimensions. Scientific text
roles and document spacing belong to `ScholarTypography`. Plot, math, table and diagram
geometry are correctly owned by their layout engines.

The exception is modal geometry in `ScholarEditorScreen`: cancel/apply rectangles,
20-pixel fields, title baselines, popup centering, border rendering and close/open cleanup
are repeated. M28 should extract a small host-modal geometry/input primitive, not a giant
theme or form framework. FIX NOW I3.

## F. Minecraft UI Consistency Findings

- `ScholarFileDialog` correctly uses native Minecraft `Button`, `EditBox`, `ConfirmScreen`
  and `Screen` focus behavior. KEEP K3.
- Menus, toolbar and context menu are custom because Scholar needs desktop-editor style
  action surfaces. Their keyboard navigation and viewport clamping are explicit and
  tested; custom implementation is justified.
- Five in-editor popups manually implement text entry, Tab switching, Backspace, Enter,
  Escape, buttons and field drawing. This duplicates native behavior and each other.
  FIX NOW I3.
- The scientific page deliberately uses non-pixel-art fonts and styling. This is the
  intended shell/content distinction, not inconsistency.

## G. Semantic Type-Dispatch Map

The eight current block families are knowingly dispatched in at least 25 production
files. The highest concentrations are `EditorSession` (24 checks), `DocumentValidator`
(13), `FragmentIdentityIndex` (10), `DocumentLayoutEngine` (10), persistence (8),
selection/context resolution (7 each), transfer materialization (7), and Markdown (7).

| System | Decision | Reason |
| --- | --- | --- |
| V1 codec | KEEP | Explicit closed wire-schema dispatch is required. |
| Layout and renderer | KEEP | Each visual family requires real layout/render behavior. |
| Validator | KEEP | Exhaustive semantic validation is desirable. |
| Transfer identity/materialization | KEEP | Closed built-in transfer policy is intentional after M25. |
| Cross-reference resolver | KEEP | Target-kind ownership is a semantic contract. |
| Markdown/plain text | REVIEW when adding a type | Export support is product policy and currently fragmented. |
| `EditorSession` activation and capability checks | FIX NOW I6, narrowly | Repeated selection/block coordination is accumulating in one class. |
| Figure-content checks | DEFER D4 | Narrow by design; change only with an accepted new content family. |

Adding one block tomorrow inherently requires a model, validation, layout, renderer and
V1 decision. Avoidable changes are duplicated plain-text/Markdown policy and broad
`EditorSession` capability branching. M28 should extract only proven session coordinators,
not install a registry or visitor across every layer.

## H. Extension-Cost Map

| Extension | Cost | Why |
| --- | --- | --- |
| New top-level block | HIGH | Closed persistence, validation, layout, render, transfer/export and editor behavior all require an explicit decision. |
| New inline type | HIGH | Character semantics, atomicity, layout, resolution, transfer, persistence and every export must agree. |
| New math node | MEDIUM | Five predictable closed systems change: layout, editor/navigation, plain text, persistence and tests. |
| New dataset value/type | MEDIUM | Value model, inference, resolver, validation, TSV/JSON and editing must agree. |
| New Figure content | HIGH | Constructor policy plus layout, render, editor routing, selection, transfer/export and persistence know Plot/Diagram. |
| New diagram element | MEDIUM-HIGH | Open semantic element interface exists, but validation, layout, rendering, persistence and domain editor remain exhaustive. |

High cost does not by itself justify a plugin registry. Scholar has a closed built-in model
and an unversioned public surface; explicit changes are safer until an actual extension
contract is approved.

## I. Duplicated Domain Knowledge

- Editable inline blocks are centralized reasonably in `EditableInlineBlock`, but inline
  node length/atomicity is repeated in `InlineContentEditor`, layout, table editing,
  clipboard and exporters. A future inline type must start with an accepted character and
  atomicity contract. DEFER D3 until such a type exists.
- Stable-ID namespace knowledge is duplicated by necessity in validator, reference
  resolver, transfer identity index and V1 codec. `StableIdentityKind` only covers
  transfer-global namespaces. There is no observed divergent namespace today. KEEP K7.
- Dataset ownership is coherent: `Document.datasets()` owns resources; binding/resolver,
  validator, transfer closure and V1 all treat columns as dataset-local. KEEP K13.
- Figure's Plot-or-Diagram rule is repeated across model, validation, layout, renderer,
  selection, editor, transfer and exporters. It is a real future pressure point but is
  currently an explicit accepted restriction. DEFER D4.
- Atomic document selection rules are mostly centralized in selection validation and
  action/session paths. No second definition was found in layout or persistence.

## J. Stable Identity Ownership

Document-global namespaces are Section/Heading, Equation, Table, Figure and Dataset.
Dataset columns and rows are resource-local; diagram elements and ports are
composite-local. `DocumentValidator`, `CrossReferenceResolver`, M25 transfer types and V1
all preserve that distinction.

There is no generic identity registry, and none is needed now. Namespace knowledge is
explicit in `StableIdentityKind`, `StableIdentityKey.targetOf`, validator sets and codec
fields. The main risk is future additions forgetting one location; architectural tests for
namespace agreement would have more value than a runtime plugin registry. DEFER D3.

## K. Stable ID Generation Paths

`StableIdAllocator.firstFree` now owns the suffix algorithm. Production callers are
`TransferPlanner` and two `EditorSession` paths for datasets and figures. Diagram-local
editors allocate their own local IDs, appropriately outside document-global allocation.

Literal bases remain distributed (`projectile-test`, figure-derived captions, component
kind names, node IDs). The algorithm is centralized, but default naming policy is not.
That is acceptable for domain-local diagram IDs; demo dataset and block defaults are FIX
NOW I5. No random/UUID production ID path was found.

## L. Dataset Ownership/Binding Findings

The dataset model is coherent and immutable. Tables reference dataset plus column IDs;
plots reference dataset plus x/y columns. `DatasetTableResolver` and
`DatasetPlotResolver` derive views; validators report missing resource/columns; transfer
copies minimal whole-dataset closure and rewrites bindings; V1 stores resource and
bindings explicitly.

The concrete defect is export: `FragmentPlainTextExporter` resolves dataset-backed tables
before TSV serialization, but passes dataset-backed plots directly to
`PlotPlainTextSerializer`. Bound series commonly have no authored points, so external
plain text omits the derived data. The same occurs inside a Figure. FIX NOW I1.

## M. Figure Ownership Findings

`FigureBlock.supportsContent` correctly enforces Plot or Diagram. Figure numbering and
captions are derived/rendered independently, and transfer recursively owns contained
resources. This is internally coherent.

An `ImageFigureContent` would touch at least `FigureBlock`, validator,
`DocumentLayoutEngine`, laid-out figure representation, renderer, editor selection/action
routing, transfer identity/resource traversal, plain-text exporter, Markdown policy and
V1 codec. This is HIGH cost. Do not pre-generalize Figure in M28. DEFER D4 until the media
model answers ownership, persistence and resource questions.

## N. Diagram Findings

Electrical component metadata is centralized in `ElectricalComponentCatalog`, and symbol
geometry in `ElectricalSymbolLibrary`; these are legitimate bounded domain registries.
Mechanical dimensions, primitives, annotations and symbols use explicit enum dispatch in
editor/layout/renderer. This is verbose but semantically appropriate.

Diagram zoom, workspace aspect limits, hit tolerance and insertion dimensions have clear
owners. The same terminal exit length appears as `DiagramConnectionRouter.PORT_EXIT_LENGTH`
and `DiagramLayoutEngine.ELECTRICAL_PORT_EXIT_LENGTH`; this is a small divergence risk but
not currently behaviorally inconsistent. DEFER D2 with diagram polish rather than
centralizing all geometry.

Diagram local identities remain opaque to document transfer and persistence preserves
them as part of the diagram value. KEEP K6.

## O. Math Extensibility Findings

The math AST is intentionally open (`MathExpression` is not sealed), while layout,
editing, plain-text serialization and V1 are closed/exhaustive. Every current node appears
in those systems. Adding a node is predictable but cannot silently work, which is safer
for structural math.

`MathExpressionEditor` is large because it owns structural path algebra and incomplete
authoring slots, not because it mixes Minecraft concerns. A visitor would not remove the
editing complexity. DEFER D5; add a node only with a cross-layer checklist and exhaustive
tests.

## P. EditorAction Findings

M24's action reuse remains intact: menu, toolbar and context menu execute `EditorAction`
instances, and enablement generally lives with actions/session capabilities. No duplicate
semantic mutation implementation was found in context menus.

`ActionShortcut` contains display text only. Actual Ctrl/key matching is independently
hardcoded in `ScholarEditorScreen.keyPressed`; file actions are also constructed in the
screen. A label can therefore claim one shortcut while another key executes it. Replace
this with one client-side binding/matching authority that maps action IDs to host keys,
while keeping semantic actions Minecraft-independent. FIX NOW I4.

## Q. Selection Findings

Seven selection families are explicit and validated by `EditorSelectionValidator`.
`EditorFocusOwner.fromSelection`, `EditorState`, session navigation, context actions and
screen rendering all dispatch on them. Most dispatch is inherent because each nested
editor has distinct geometry and navigation.

The risk is adding another selection mode without a checklist. A generic selection plugin
system is not justified. Keep the closed model and add an architecture test/checklist when
the next nested editor appears. KEEP K8.

## R. Focus/Input Findings

Input priority is coherent in order: modal popup, context menu, menu bar, toolbar popup,
nested editor, then document. M27 tests protect escape unwinding and one-consumer behavior.

The implementation is accumulated in one long `keyPressed`/mouse dispatch surface, with
five repeated modal branches. Extracting modal host behavior is FIX NOW I3; replacing the
accepted focus model is explicitly out of scope.

## S. Layout Constant Findings

Document margins/readable width and paragraph/heading/equation spacing are centralized in
typography. Table cell padding, plot gutters/tick density, diagram padding, math structure
spacing and TOC indent are local to the respective layout algorithms. KEEP K1/K2.

`DocumentLayoutEngine` remains an exhaustive central block layout coordinator. That is a
known scaling point, but Foundation V2 culling removed the immediate rendering problem.
Do not create a layout plugin registry in M28. DEFER D10 until a measured new block or
pagination requirement exposes a concrete boundary.

## T. Render Constant Findings

Shell colors are centralized. Document page, selection, caret, focus and scientific
primitive colors are split between screen and renderer according to ownership. Some focus
colors repeat (`0xFF6B7280`, `0xFF2563EB`) in `ScholarEditorScreen`; a small editor visual
state palette could remove inconsistency, but final values require screenshots. VISUAL QA
V2 rather than an immediate color rewrite.

## U. Typography Findings

Body, Heading 1-6 and Math use `ScholarTypography` and `MinecraftTypographyResolver`.
Tables, plots, diagrams and TOC derive `TextStyle` using body/bold roles, so scientific
content does not directly choose Minecraft fonts. Shell chrome intentionally uses the
Minecraft UI font directly. No improper content-font bypass was found. KEEP K2.

Caption does not have a distinct typography role; it currently renders as paragraph text.
Whether it needs one is visual/product judgment. VISUAL QA V4.

## V. GUI-Scale/Responsive Findings

The editor and viewer derive document width from current window size and readable-width
limits. M27 added resize relayout and viewport clamping. Context menus clamp to viewport;
toolbars clip/scroll their surfaces.

Custom modal widths of 270-360 pixels can exceed narrow windows because centering clamps
Y but not width/X. `ScholarFileDialog` does clamp width. This is part of FIX NOW I3 and
must be verified at narrow width and large GUI scale (VISUAL QA V6).

## W. Scroll Implementation Inventory

- Main editor document: `ScrollGeometry.clamp/reveal` plus `SCROLL_STEP`.
- Read-only viewer: duplicate private clamp and duplicate step.
- Outline: private content clamp in `ScholarEditorScreen`.
- Context menu and menu bar: row-based scrolling, correctly distinct from pixel document
  scrolling.
- Diagram pan/zoom: semantic viewport navigation, not document scrolling.
- File picker: page navigation, not scrolling.

Use `ScrollGeometry` in the viewer and share the shell wheel increment. Do not force
row-based menu scrolling through the same abstraction. FIX NOW M2.

## X. Popup/Modal Geometry Findings

Context-menu geometry has one pure authority. Toolbar and menu popup bounds are owned by
their widgets. File dialogs use native widgets. In-editor semantic-token, cross-reference,
plot, canvas, diagram-label and electrical-component popups independently compute nearly
identical rectangles and buttons. FIX NOW I3.

## Y. Filesystem/Persistence Hardcoding

`ScholarEditorScreen.createDevelopmentScreen` chooses `<gameDirectory>/scholar/documents`.
`FileDocumentStorage` receives an application-owned directory and safely normalizes names,
rejects symlinks, writes UTF-8 temporary files and uses atomic replacement when available.
`.scholar.json`, 1-64 ASCII-safe names and Windows reserved-name handling are compatibility
and safety policy, not accidental hardcoding. KEEP K4.

The storage root should move out of a development-screen factory when a production entry
point exists, but the path itself is reasonable. It is part of FIX NOW I2, not a V1 change.

## Z. Persistence V1 Coupling

`DocumentJsonCodec` explicitly maps runtime values to V1 labels and rejects unknown fields
and types. Runtime records do not contain Gson annotations or wire discriminators;
`V1WireLabels` isolates enum labels. Internal implementations can evolve as long as the
codec continues constructing equivalent semantic values. KEEP K4.

The codec is one 447-line exhaustive class. Splitting it by wire family could improve
maintainability, but doing so now would add movement without changing extension cost.
DEFER D9 unless V2 or a new large family is approved.

## AA. Transfer Coupling

M25 successfully moved extraction, resource closure, namespace-aware identity planning,
reference disposition and materialization out of `EditorSession`. New document-level
content still needs explicit support in `FragmentIdentityIndex`, `TransferPlanner` when it
contains inline references, and `TransferMaterializer`. That is expected closed semantic
dispatch. KEEP K6.

Legacy payload classes remain only as compatibility inputs in
`LegacyFragmentClipboardAdapter`; current production copy emits fragment payloads. They
add test and public-surface weight but no longer duplicate policy. DEFER D8 until an API
compatibility decision exists.

## AB. Validation Ownership

`DocumentValidator` covers duplicate global IDs, dataset shape/bindings, Figure content,
diagram endpoints/bounds/ports, mechanical targets, plot representability and
CrossReference degradation. Constructors also protect local value invariants. Editor and
transfer validate at trust/commit boundaries.

No major semantic invariant was found solely in rendering. Selection validity remains
correctly separate. Persistence adds schema limits, which are format invariants rather
than document invariants. KEEP K7.

## AC. Default-Value Inventory

| Default | Owner | Classification |
| --- | --- | --- |
| Empty new Paragraph/document | `DocumentWorkspace` | KEEP |
| Empty equation | `DocumentEditor` | KEEP |
| 2x2 empty table | `DocumentEditor`/`TableBlock` | KEEP |
| Sample quadratic plot | `DocumentEditor` | FIX NOW I5 |
| Pre-connected System Diagram | `DocumentEditor` | FIX NOW I5 |
| Projectile Test dataset | `EditorSession` | FIX NOW I5 |
| Plot height 180/minimum 96 | `PlotDefinition` | KEEP |
| Diagram node size and viewport fit | diagram editor/layout | KEEP |
| Untitled filename draft | file dialog | KEEP |

M28 should replace demo scientific data with neutral authoring defaults or an explicit
template choice. It must not silently redefine model defaults without product agreement.

## AD. Explicit-Limit Inventory

| Limit | Reason | Assessment |
| --- | --- | --- |
| History 100 snapshots | UX/memory | KEEP, measure later |
| V1 16 Mi characters / 64 Mi bytes / depth 128 | safety/format | KEEP |
| Filename 64 ASCII-safe characters | filesystem safety/UX | KEEP |
| Math external paste 4,096 chars / 512 tokens | parser safety | KEEP |
| Nice plot ticks max 128 | algorithm safety | KEEP |
| Diagram zoom 0.5-6.0 | UX | KEEP |
| Diagram workspace aspect 0.25-1.5 | UX/prototype | DEFER D2; validate visually before changing |
| Context menu 12 rows | UX | KEEP |
| Cross-reference picker 8 rows | prototype UI | part of FIX NOW I3 |
| File picker rows derived from height, max 8 | UX | KEEP |
| No dataset row/column count limit | performance | DEFER D9; persistence size and M29 measurement are current bounds |

## AE. Dev/Production Boundary

`ScholarClient` unconditionally registers `scholar_dev_viewer` and
`scholar_dev_editor [small|medium|large]`. Both open screens backed by
`DevelopmentDocument`/`DevelopmentStressDocument`, all in main source and therefore the
release jar. Seven test files import these client fixtures, including core persistence and
transfer tests.

No normal production open-document entry point exists; the development command is the
product entry point. Before release or new feature demonstrations, introduce an explicit
development gate/source set and move reusable semantic test fixtures to test support or a
Minecraft-independent fixture module. FIX NOW I2.

## AF. Logging/Debug Findings

No `System.out`, `System.err`, `printStackTrace` or temporary debug overlay was found in
production. Clipboard failures are logged with SLF4J. Storage returns bounded user-facing
diagnostics and intentionally suppresses only temporary-file cleanup failures.

There is no product-level diagnostic surface for failed clipboard/transfer operations;
`EditorSession.transferDiagnostics()` is not rendered. This is real UX debt but should be
designed with diagnostics/recovery, not patched into a status string. DEFER D11.

## AG. TODO/FIXME/HACK Inventory

No production TODO, FIXME, HACK, stub or placeholder branch was found. Comments containing
"temporary" describe file replacement or accepted editor transient state. One plot comment
references an old M16 placeholder but documents why the current geometry differs; it is
not dead code.

## AH. Dead/Legacy-Code Candidates

- Legacy rich clipboard payload records plus `LegacyFragmentClipboardAdapter` are used by
  compatibility tests and accepted by production paste, but production copy no longer
  creates them. DEFER D8 pending compatibility policy.
- `ScholarDocumentScreen` is a development viewer only and duplicates editor page setup;
  retain it while visual QA uses it, then decide whether it is a dev tool. FIX NOW I2 owns
  gating, not deletion.
- No pre-M24 mutation engine or pre-M26 storage experiment was found.
- `DocumentPlainTextSerializer` is live through fragment export, not dead; its missing
  Equation handling is FIX NOW B1.

## AI. Duplicate Implementation Candidates

Meaningful duplicates are:

- physical shortcut matching vs shortcut display metadata: FIX NOW I4;
- custom modal geometry/input: FIX NOW I3;
- viewer/editor document scroll clamp and step: FIX NOW M2;
- Figure Plot/Diagram dispatch across layers: DEFER D4, accepted closed model;
- inline Text/CrossReference traversal across editing/export/transfer: DEFER D3 until a
  third inline type proves the right abstraction;
- stable suffix generation: already consolidated in `StableIdAllocator`, KEEP K9;
- dataset and reference lookup: centralized resolvers/registry exist, KEEP K13.

## AJ. Core/Client Boundary

Only `dev.rgcb.scholar.client.*` plus the NeoForge mod entry class import Minecraft,
NeoForge or Mojang classes. Document, editor, layout, transfer and persistence remain pure
Java. Persistence has a boundary test preventing client/editor/layout/transfer/clipboard
imports. KEEP K12.

The main boundary blemish is tests importing `client.DevelopmentDocument`; production core
does not depend on client. Fix under I2 without moving Minecraft into core.

## AK. Test-Driven Production Artifacts

`DevelopmentDocument.createPersistenceFixture` and `DevelopmentStressDocument` serve both
Minecraft QA and seven automated suites. They are useful deterministic fixtures but their
location in production client code makes core tests depend on a shipped dev utility.
FIX NOW I2: preserve the scenarios while separating reusable semantic fixture construction
from release entry points.

No other production API appears to exist solely for tests. Constructors and injectable
history/storage collaborators are legitimate seams.

## AL. Error-Handling Findings

Core semantic APIs generally use constructor exceptions for programmer-invalid values and
result/diagnostic objects for validation, transfer and persistence failures. This is
coherent. Broad catches in validators convert malformed extension values to diagnostics;
clipboard catches log and return failure.

`ScholarFileDialog` displays only the first persistence diagnostic, and editor transfer
diagnostics are invisible. DEFER D11 for a unified user diagnostic surface. Do not expose
raw exception messages.

## AM. Nullability/Sentinel Findings

No synthetic unresolved CrossReference IDs were introduced; external references degrade
to text according to M25. `-1` is used only for transient `diagramPanBlockIndex`, not
semantic state. Optional IDs and missing dataset values use explicit `Optional`/enum
states. `Integer.MAX_VALUE` is used as an internal unclipped viewport sentinel in renderer
helpers; it does not escape semantic state. KEEP K8.

## AN. Mutability Findings

Semantic records defensively copy lists/maps/sets, editor mutations reconstruct immutable
values, transfer snapshots are immutable, and history stores `EditorState` snapshots.
`DiagramViewportStore`, clipboard sidecar, workspace lifecycle and screen popup fields are
appropriately transient mutable state. No semantic mutable collection escape was found.
KEEP K8.

## AO. Source-of-Truth Findings

- Semantic document: `EditorHistory.current().document()`; workspace tracks only saved
  baseline/name. Coherent.
- Selection/focus: `EditorState.selection`; focus derives from selection. Coherent.
- Dataset: document-owned resource; views resolve from it. Coherent.
- IDs: semantic records plus validator; allocator never reserves hidden state. Coherent.
- Diagram viewport: `DiagramViewportStore`, transient and keyed to diagram continuity.
  Coherent.
- Action shortcuts: two sources of truth (display metadata and physical keys). FIX NOW I4.
- Product strings/modal state: scattered in screen and actions. Address modal ownership in
  I3; localization remains D6.

## AP. CitationBlock Impact Analysis

Likely required production changes:

- new model record and constructor invariants: inherently necessary;
- `DocumentValidator`: necessary if it has IDs/resources/inline content;
- `DocumentLayoutEngine`, `LaidOutBlockKind`/laid-out data and renderer: necessary;
- V1 codec: necessary only if persisted in V1; otherwise V1 must reject explicitly;
- `FragmentIdentityIndex`, `TransferMaterializer`, plain-text exporter: necessary for
  transferable semantic content;
- CrossReference target kind/resolver/identity map: necessary only if targetable;
- Markdown serializer/parser: explicit product decision, not automatic;
- `EditorSession`, action ID/action menus/context resolver: necessary only for authoring;
- selection validator/hit testing: mostly generic atomic block behavior should work, but
  activation/edit mode would be new policy;
- tests and dev fixture: necessary.

Cost is HIGH. Most changes are inherent closed-domain decisions. Avoidable duplication is
primarily action/screen integration and export traversal. Do not solve this with arbitrary
block registration.

## AQ. CitationInline/FootnoteReference Impact Analysis

Required changes include `InlineNode`, logical character length/atomicity in
`InlineContentEditor`, editable-block support, document/table layout, hit/caret/selection
geometry, plain text, Markdown, transfer traversal/materialization, validation,
persistence, and reference resolution if active. Figure captions and table cells must
follow the same rule.

Cost is HIGH because inline nodes participate in text coordinates. Before implementation,
freeze whether the new node is one atomic logical character like CrossReference or owns
editable text. A generic visitor alone would not answer that semantic question.

## AR. ImageFigureContent Impact Analysis

The current Figure constructor accepts only PlotBlock/DiagramBlock, and at least 11 systems
know that union. Cost is HIGH. A media feature needs a resource/asset model, intrinsic
dimensions, persistence, missing-asset diagnostics, transfer closure, layout/rendering,
plain-text export and authoring UX. That is a new subsystem, not an M28 refactor. DEFER D4.

## AS. CSV Import Impact Analysis

The dataset model can accept imported immutable columns/rows cleanly, and views,
validation, transfer and persistence already operate on the result. Architecture readiness
is MEDIUM-HIGH.

`DatasetTabularImporter` currently chooses tab if the first line contains one, otherwise
comma, then uses regex splitting. It does not support quoted fields, embedded commas,
escaped quotes or multiline records. It is not wired to a file/UI entry point. Treat it as
a bounded simple tabular importer, not production CSV. A real CSV feature should replace
the parser boundary or use a proven parser and then feed the existing dataset model.
DEFER D7 until CSV import is scheduled.

## AT. Productization Risks

- Ungated development commands and fixtures ship in the normal jar (I2).
- Development titles and milestone/demo scientific defaults appear in normal workflows
  (I2/I5).
- Whole-equation external copy can silently produce an empty clipboard string (B1).
- Bound plot external copy can omit displayed data (I1).
- Narrow-window custom dialogs can leave the viewport (I3/V6).
- Transfer/persistence diagnostics have inconsistent user presentation (D11).
- No localization ownership exists yet (D6), acceptable before a localization milestone.

## AU. Test-Suite Findings

The current suite has 132 files and 1,339 direct `@Test` methods; parameterized tests bring
the executed total to 1,456. Seeds are explicit in every randomized test inspected. Current
XML reports total about 5.95 suite-seconds; stress measurement/hardening suites account for
most of that and remain modest.

Strengths: focused model/layout/editor tests, fixed-seed integration, transfer/persistence
boundary tests, and Foundation V2 golden scenarios. Weaknesses: very large
`DocumentEditorTest`, `EditorSessionTest`, `MathExpressionEditorTest` and
`EditorActionTest`; seven suites depend on production client fixtures; legacy clipboard
tests retain old payload concepts; no focused test protects whole-Equation fragment plain
text or resolved bound-plot plain text. Add those regressions with B1/I1. Do not rewrite
the suite.

## AV. Build/Dependency Findings

Direct dependencies are NeoForge/Minecraft via ModDev, Gson 2.10.1 for explicit V1 JSON,
and JUnit 5.11.4 plus launcher at test scope. No duplicate third-party library or test-only
dependency leaks into production. Gson is `implementation` because runtime persistence
uses it. KEEP K12.

Version upgrades are outside M28 audit scope.

## AW. Documentation Inconsistencies

The current user-approved baseline says M27/Foundation V2 is accepted, but
`PROJECT_SPEC.md`, `ROADMAP.md` and `M27_EDITOR_HARDENING.md` still say manual QA pending;
M27 history wording also reflects the pre-acceptance state. FIX NOW M1 as documentation
bookkeeping in the implementation milestone, without rewriting roadmap strategy.

Post-M24 debt entries for persistence and transfer are historical and should remain as
audit context, but each should clearly link to M25/M26 resolution if those documents are
later curated. DEFER documentation consolidation beyond the accepted-status correction.

## AX. Git/Repository Hygiene

`.gitignore` covers `.gradle`, `build`, `run`, `out`, logs and IDE files. No tracked build,
run, crash, screenshot, archive or saved `.scholar.json` artifact was found. Ignored local
build/run directories exist as expected.

The working tree is intentionally large and dirty from uncommitted M25-M27 work. M28 must
add only this report and must not clean, stage or revert that work. KEEP repository ignore
policy.

## AY. Command/Dev-Entrypoint Inventory

| Entrypoint | Class | Classification | Finding |
| --- | --- | --- | --- |
| NeoForge mod constructor | `Scholar` | PRODUCTION | Minimal registration boundary. |
| `scholar_dev_viewer` | `ScholarClient` | DEV ONLY | Ungated; opens `DevelopmentDocument`. FIX NOW I2. |
| `scholar_dev_editor` profiles | `ScholarClient` | DEV ONLY | Ungated; opens stress fixtures. FIX NOW I2. |
| No normal open Scholar command/item/API | n/a | PRODUCT GAP | Product decision needed; do not invent it in M28. |
| Fixture factories | `DevelopmentDocument`, `DevelopmentStressDocument` | DEV/TEST SUPPORT | Split reachability/ownership under I2. |

## AZ. Recommended Visual Screenshot Checklist

Capture 12 screenshots in actual Minecraft:

1. Normal mixed document at default GUI scale: page hierarchy, body/heading rhythm,
   equations, tables and overall density.
2. Empty/new document: empty-state affordance, caret visibility and whether chrome
   overwhelms the page.
3. Long stress document mid-scroll: clipping, culling artifacts, status line and scroll
   continuity.
4. File menu plus disabled/enabled actions: alignment, shortcut columns, hover/pressed
   states and viewport clamping.
5. Context menu near bottom-right: clamping, scrolling, separators and selection retention.
6. Narrow window with a custom modal: off-screen bounds, field/button fit, title clipping
   and focus indication.
7. Equation editing with nested fraction/root/script: math scale, caret, selection and
   focus border.
8. Dataset-backed table editing: headers, cell padding, active cell, missing values and
   narrow-column wrapping.
9. Dataset-backed plot editing: tick collisions, legend, focus/target colors and derived
   points.
10. Electrical and mechanical diagram editing: zoom, selected element/port/connection,
    labels and line contrast.
11. Figure with dataset-backed Plot and caption CrossReference: caption hierarchy,
    numbering and content/caption spacing.
12. Open/Save/Unsaved/Error native dialogs at large GUI scale: truncation, diagnostic
    readability, focus order and button semantics.

## BA. FIX NOW Findings

### B1 - BLOCKING - Whole EquationBlock Plain-Text Copy Is Empty

- Evidence: `FragmentPlainTextExporter.export` delegates unknown block roots to
  `DocumentPlainTextSerializer.serializeBlock`; that serializer handles Heading,
  Paragraph, TOC and Table only, then returns `""`. `EquationBlock` is not special-cased.
- Failure: copying a whole equation block writes no useful OS text, so external paste
  loses visible scientific content even while the rich sidecar remains valid in-process.
- Direction: use the existing `MathPlainTextSerializer` for EquationBlock roots and add
  focused block-copy/export tests, including structured fraction/root/script.

### I1 - IMPORTANT - Bound Plot/Figure Fallback Omits Resolved Dataset Data

- Evidence: `FragmentPlainTextExporter` resolves tables but passes PlotBlock directly to
  `PlotPlainTextSerializer`; `FigurePlainTextSerializer` does the same for contained Plot.
- Failure: dataset-backed series can display points while exporting only the `x/y` header.
- Direction: resolve dataset-backed plot views from the source document before fallback
  serialization, preserving degraded missing-resource diagnostics.

### I2 - IMPORTANT - Development Runtime Is Ungated and Test Fixtures Cross Boundaries

- Evidence: `ScholarClient.registerClientCommands` always registers both dev commands;
  `DevelopmentDocument` is 815 lines in main; seven tests import client fixtures.
- Failure: release jars expose internal QA surfaces and future core tests become coupled to
  Minecraft-client package organization.
- Direction: add an explicit dev gate/source boundary; retain deterministic fixtures but
  move reusable semantic fixture construction to test support or a pure fixture package.

### I3 - IMPORTANT - Editor Screen Owns Repeated Modal/Input Infrastructure

- Evidence: `ScholarEditorScreen` has 3,194 lines and separate state, key branches,
  geometry, rendering and close logic for semantic token, CrossReference, plot, diagram
  canvas, diagram label and electrical component popups.
- Failure: citation/media/import UI would add another parallel form and can diverge in
  focus, viewport clamping and keyboard semantics.
- Direction: extract a small client-only modal/form host or reusable field/button geometry
  and event contract. Do not move semantic actions into widgets.

### I4 - IMPORTANT - Shortcut Display and Physical Matching Have Two Authorities

- Evidence: `ActionShortcut` stores only `displayText`; Ctrl/key matching is a separate
  chain in `ScholarEditorScreen.keyPressed`.
- Failure: a shortcut label can change without behavior, platform modifiers cannot be
  expressed coherently, and new actions require unrelated screen edits.
- Direction: define client-side key binding descriptors/matching keyed by action ID while
  keeping `EditorAction` core independent of GLFW.

### I5 - IMPORTANT - Normal Authoring Commands Insert Demo Scientific Content

- Evidence: `DocumentEditor.insertDefaultPlot` creates Sample Plot with quadratic points;
  `insertDefaultDiagram` creates connected Node A/B; `EditorSession.defaultDataset`
  creates Projectile Test measurements.
- Failure: new user documents silently acquire milestone examples and fixture-specific
  names/IDs instead of intentional blank/template content.
- Direction: approve neutral defaults or explicit templates and keep demos in dev fixtures.

### I6 - IMPORTANT - EditorSession Remains the New Feature Coordination Hotspot

- Evidence: 2,842 lines, 24 block checks, all nested editor routing, datasets, figures,
  navigation and clipboard integration; M25 transfer was extracted but authoring
  capabilities continue to accumulate here.
- Failure: a new authored family requires session methods, supports methods, action wiring
  and screen forwarding, increasing inconsistent guards.
- Direction: extract only cohesive existing coordinators (dataset operations and nested
  editor activation/capabilities are candidates) behind package-internal services. Do not
  replace the session or add a capability/plugin registry.

### M1 - MINOR - Accepted M27 Status Is Stale in Authoritative Docs

- Evidence: project spec/roadmap/M27 report still say manual QA pending.
- Failure: later milestones reason from an incorrect foundation state.
- Direction: update status lines/history only after this audit is accepted.

### M2 - MINOR - Viewer Retains Duplicate Document Scroll Arithmetic

- Evidence: `ScholarDocumentScreen.clampScroll` duplicates `ScrollGeometry.clamp`; editor
  and viewer each define step 24.
- Failure: a future scroll fix can land in one surface only.
- Direction: reuse pure `ScrollGeometry` and one shell wheel-step policy; leave menu row
  scrolling separate.

### M3 - MINOR - Toolbar Uses Raw Escape Key Code

- Evidence: `ToolbarWidget.keyPressed` compares `keyCode == 256` while other widgets use
  `GLFW.GLFW_KEY_ESCAPE`.
- Failure: obscures host dependency and invites inconsistent key handling.
- Direction: use named host key constants as part of I4.

## BB. KEEP Findings

1. K1: local scientific layout constants in the owning algorithm.
2. K2: `ScholarTypography` as document typography/spacing authority.
3. K3: `ScholarShellStyle` plus native file dialog widgets.
4. K4: explicit, bounded, deterministic V1 codec/storage.
5. K5: electrical catalog and normalized symbol library as domain registries.
6. K6: closed transfer dispatch and diagram-local identity opacity.
7. K7: exhaustive validator and explicit identity namespaces.
8. K8: immutable AST, explicit selection families and transient UI state separation.
9. K9: centralized first-free stable ID suffix allocation.
10. K10: injectable 100-snapshot history default.
11. K11: centralized diagram viewport zoom bounds.
12. K12: pure Java core/client boundary and minimal dependencies.
13. K13: document-owned datasets with resolver-based derived views.
14. K14: fixed-seed randomized and golden Foundation V2 tests.

## BC. DEFER Findings

1. D1: reduce/label the 374-type accidental public Java surface before a public mod API,
   not during M28 cleanup.
2. D2: consolidate minor diagram geometry/limit policy when diagram work resumes.
3. D3: generic semantic dispatch/visitor/registry; no evidence supports one now.
4. D4: Figure media generalization until image/resource semantics are designed.
5. D5: math visitor or AST sealing until a new math node proves a concrete problem.
6. D6: localization infrastructure after modal/string ownership is cleaner.
7. D7: production CSV parsing/UI until CSV import is scheduled.
8. D8: legacy clipboard payload removal until compatibility policy is explicit.
9. D9: codec splitting, snapshot history and large-dataset optimization until measured.
10. D10: pagination/layout registry/incremental layout until an actual requirement.
11. D11: unified user diagnostics/recovery surface as a cohesive later UX milestone.

## BD. VISUAL QA Findings

1. V1: shell hierarchy and document/page contrast.
2. V2: selection/focus/hover/disabled palette consistency.
3. V3: plot/table/diagram density and clipping at real font metrics.
4. V4: caption role, spacing and Figure visual hierarchy.
5. V5: equation scale/caret alignment for nested structures.
6. V6: narrow-window and large-GUI-scale modal bounds.
7. V7: menu/context/toolbar scrolling and pointer/keyboard state parity.
8. V8: file/error/unsaved diagnostics and empty/new-document usability.

## BE. Recommended M28 Implementation Scope

Implement one bounded productization milestone:

1. Repair EquationBlock and dataset-backed Plot/Figure plain-text fallback, with focused
   regressions.
2. Gate development commands and separate reusable semantic fixtures from shipped client
   entry points without removing QA capability.
3. Extract a small client-only modal/input geometry primitive and make every current custom
   popup viewport-safe; do not redesign the action/session model.
4. Establish one client key-binding authority for action execution and displayed shortcuts.
5. Replace demo scientific insertion defaults with approved neutral defaults or explicit
   template choices.
6. Extract one or two cohesive `EditorSession` coordinators only where existing methods and
   guards already form a stable cluster.
7. Reuse scroll geometry in the viewer, replace raw key literals, and reconcile accepted
   M27 documentation status.
8. Perform the 12-shot Minecraft visual audit and fix only defects evidenced by it.

This scope closes concrete correctness/product gaps and lowers the cost of the next UI
feature without changing semantic models or accepted persistence/transfer contracts.

## BF. Things M28 Should Explicitly NOT Change

- No Document/Inline/Math AST redesign.
- No universal visitor, block registry, graph framework or plugin API.
- No Figure generalization or image/media implementation.
- No V1 wire labels, schema, extension, filename policy or storage semantics change.
- No transfer identity/resource/reference policy redesign.
- No history replacement, incremental layout or performance rewrite without measurement.
- No typography redesign before screenshot evidence.
- No broad localization implementation.
- No deletion of accepted compatibility paths solely to reduce file count.
- No M29 scientific feature work.

## BG. Open Questions Requiring Human/Product Decision

- Should normal Insert Plot/Diagram create empty authoring structures, or should Scholar
  expose explicit starter templates?
- What is the production entry point for opening Scholar after dev commands are gated?
- Are development commands omitted entirely from release jars or enabled by a config/dev
  environment flag?
- Is cross-platform localization a near-term release requirement?
- Does M29 target citations, media, or another feature family? The answer determines
  whether Figure or inline extension pressure should be addressed next.
- Should plain-text export of a dataset-backed plot include all resolved points or a bounded
  summary for very large datasets?

## M28 Visual QA Fixture

Run `/scholar_dev_editor` to open the deterministic, editable M28 Visual QA document at
the top with a valid collapsed caret. It contains concise instructions followed by
Typography & Prose, Document Structure & Navigation, Equations, Tables, Scientific
Dataset & Plot, Figures, Electrical Diagram, Mechanical Diagram, Long & Edge Content,
and Atomic & Interactive Content sections. All CrossReferences and dataset bindings are
valid. No modal or context menu opens automatically.

The explicit stress commands remain available: `/scholar_dev_editor small`, `medium`,
`large`, `data_heavy`, `diagram_heavy`, and `mixed_stress`. Normal File > New behavior is
unchanged and still creates the ordinary empty document.

### Screenshot-State Mapping

1. **Normal mixed document:** run `/scholar_dev_editor`; capture the top and scroll through
   the prepared typography, equation, table, plot, Figure, and diagram sections.
2. **Empty/new document:** open File > New and confirm the unsaved-work choice when shown;
   capture the resulting empty editable document. Re-run the command to restore the fixture.
3. **Long stress document:** run `/scholar_dev_editor large`, scroll to the middle, and
   capture clipping, status, and scroll continuity.
4. **File menu:** in the M28 fixture, click File and hover an enabled and disabled action;
   capture labels, shortcuts, alignment, and viewport clamping.
5. **Context menu near bottom-right:** scroll until `RIGHT CLICK HERE` is near the lower
   right, then right-click that paragraph and capture the clamped menu.
6. **Narrow window with custom modal:** narrow the game window, click the prepared plot or
   diagram, invoke one of its existing edit/add actions that opens a field modal, and
   capture bounds, focus, title, fields, and buttons.
7. **Equation editing:** click the deep nested equation in section 3, place the caret in
   the fraction/root/script, extend a selection, and capture the focus border and math.
8. **Dataset-backed table editing:** click the Thermal Response table in section 4, select
   the row containing the missing observation, and capture headers, padding, wrapping, and
   active-cell treatment.
9. **Dataset-backed plot editing:** click the Thermal Response plot in section 5 and
   capture ticks, legend, measured/predicted series, focus, and target colors.
10. **Diagram editing:** click the branched electrical schematic in section 7 and one of
    the mechanical drawings in section 8; select an element/terminal or annotation and
    capture zoom, connectivity, labels, constraints, and line contrast.
11. **Figure hierarchy:** capture both prepared Figures in section 6, including the long
    dataset-backed Plot caption with its valid equation CrossReference.
12. **Native file dialogs:** at a large GUI scale open File > Open and File > Save As;
    also exercise the unsaved-work confirmation through File > New. Capture truncation,
    diagnostics, focus order, and button semantics. Use an invalid filename only when an
    error-dialog screenshot is required.

These actions expose the current UI. Visual defects observed during review remain audit
evidence and are not corrected by the fixture.

## Audit Conclusion

M28 technical audit is complete. Scholar Foundation V2 is structurally healthy. One
blocking clipboard fallback defect and six important productization issues should be
resolved before the next scientific feature family. No production implementation, test
change, roadmap acceptance, commit or push was performed by this audit.

## Post-Audit M28 Resolution Status

The original evidence above remains unchanged. M28 implementation resolved B1, I1-I5,
M1-M3. I6 was reclassified with evidence: the accepted fixes justified extracting only
neutral scientific insertion defaults from `EditorSession`; a broad session rewrite remains
deferred. Detailed files, tests, visual decisions, and manual QA are recorded in
`M28_PRODUCTIZATION_AND_VISUAL_POLISH.md`. M28 remains pending final manual Minecraft QA.
