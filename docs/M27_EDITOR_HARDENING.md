# M27 - Editor Hardening & Foundation V2

Status: COMPLETE / ACCEPTED, including user-confirmed final Minecraft manual QA.
M27 is one milestone. The pending wording elsewhere in this report is retained as historical implementation context.
M18-M26, including M26 manual QA, are the user-accepted input baseline.

## M27 File Inventory

Paths below are repository-relative. This is the M27 delta, not the combined uncommitted
M25/M26 working tree. No prior changes are reverted or represented as new M27 work.

New production/development types:
- `src/main/java/dev/rgcb/scholar/client/DevelopmentStressDocument.java` (with Profile enum).
- `src/main/java/dev/rgcb/scholar/editor/ScrollGeometry.java`.

Modified production types under `src/main/java/dev/rgcb/scholar/`:
- `client/ScholarClient.java`.
- `client/render/MinecraftDocumentRenderer.java`.
- `client/screen/ScholarEditorScreen.java`.
- `client/ui/MenuBarWidget.java` and `client/ui/ToolbarWidget.java`.
- `data/DatasetPlotResolver.java`.
- `editor/CaretGeometryResolver.java`, `editor/DocumentHitTester.java`,
  `editor/SelectionGeometryResolver.java`, `editor/VisualLineNavigator.java`.
- `layout/DocumentLayoutEngine.java`, `layout/TableLayoutEngine.java`,
  `layout/LaidOutBlock.java`, `layout/LaidOutText.java`.
- `validation/DocumentDiagnosticCode.java`, `validation/DocumentValidator.java`.

New test files under `src/test/java/dev/rgcb/scholar/`:
- `layout/EditorStressMeasurementTest.java`, `layout/EditorLayoutHardeningTest.java`.
- `editor/FoundationV2HardeningTest.java`, `editor/ScrollGeometryTest.java`.
- `client/ui/MenuInputHardeningTest.java`.

Modified tests: `layout/CrossReferenceLayoutTest.java`, `editor/DocumentHitTesterTest.java`,
`editor/VisualLineNavigatorTest.java`. Documentation: this new report plus updated
`docs/ROADMAP.md`, `docs/PROJECT_SPEC.md`, `docs/MILESTONE_HISTORY.md`.
Total M27 delta: 30 files (18 main, 8 test, 4 documentation).

## Audit And Scope

The audit followed the actual production paths, not just the milestone descriptions:

| Area | Current path and finding | Treatment |
| --- | --- | --- |
| Layout | `DocumentLayoutEngine` builds ordered, complete immutable geometry; `TableLayoutEngine` repeats the text wrapping algorithm. Both used UTF-16 units for token offsets and split boundaries. | Correct both algorithms using the same ROOT character BreakIterator contract as TextBoundary. No AST normalization. |
| Inline references | Display labels expand one semantic character into many glyphs. Hit-test, caret, selection and vertical-navigation algorithms treated these as ordinary text. | Explicit `LaidOutText.atomic`, semantic endpoints only, full-label highlighting and final wrapped-label end affinity. |
| TOC | Entry indentation exceeded very narrow widths; `LaidOutTableOfContentsEntry` rejected negative dimensions. | Bound indentation to width minus one. Derived source targets remain unchanged. |
| Figures | Standalone Plot layout resolved dataset bindings; Figure-contained Plot layout did not. | Delegate both to DatasetPlotResolver before generating geometry. No stored derived points. |
| Rendering | Scientific render helpers already checked visibility; prose still walked off-screen line/run collections. | Skip non-intersecting blocks before dispatch; retain existing fine-grained checks/scissor. |
| Scroll | Existing caret reveal was minimal for ordinary text. Revealing objects taller than the viewport could alternate top/bottom on repeated calls. | Pure ScrollGeometry, stable intersecting tall-object visibility, existing small-caret behavior retained. |
| Input | Screen routes modals first, then popup/chrome and nested selection owners. MenuBarWidget consumed only Escape, so other keys could reach nested handlers before the later menu guard. | Open menus consume all keys, navigate enabled actions and invoke the existing controller once. |
| Chrome | Menu dropdown X followed the title without right-edge bounds; titles could become unreachable at narrow host widths. Toolbar popup render/hit coordinates were repeated and unbounded. | Bounded dropdown origins, horizontally scrollable/clipped title strip, menu arrow navigation, common toolbar origin calculations and bounded tooltips. |
| Resize | Geometry rebuilt, but active diagram/text drag anchors could survive the coordinate-system change. | Cancel uncommitted drag, clear anchors/pan and close transient menus/dropdowns before reflow. Retain valid semantic selection and diagram viewport values. |
| Dataset numeric boundary | BigDecimal may be valid but overflow double. DatasetPlotResolver passed infinity into DataPoint, which rejects it. | Omit unrepresentable points, keep exact dataset values, report UNREPRESENTABLE_PLOT_VALUE warning with dataset/block/series/row context. |
| Semantic gates | EditorState constructs through EditorSelectionValidator; model constructors enforce local shape; M25 insertion and M26 load stage validated candidates. | Preserve these boundaries, no renderer repair or broad exception catch. |
| History | EditorHistory stores full immutable states in capacity-bounded deques (default 100); navigation/transient UI does not create entries. | Measure sharing and exercise 100 heterogeneous transactions, keep snapshot architecture. |
| Persistence/transfer | Separate pure packages; DocumentWorkspace owns saved-value dirty baseline; transfer owns identity/closure decisions. | No pipeline duplication, persistent provenance, or V1 schema edit. Extend integration tests. |

Reviewed collaborators include ScholarDocumentScreen, ScholarEditorScreen,
ScholarFileDialog, EditorSession, EditorState, DocumentEditor, DocumentHitTester,
CaretGeometryResolver, SelectionGeometryResolver, VisualLineNavigator, menu/toolbar/
context geometry, Dataset resolvers, Figure layout, EditorHistory and current
Foundation V1/semantic transfer/persistence regression suites.

The accepted M24 foundation contracts remain intact. Core tests cannot prove native
widget visuals, real clipboard integration, GPU frame time or host GUI-scale behavior.
Those are specifically manual QA, not claims implied by a green build.

## Deterministic Fixtures

`client/DevelopmentStressDocument` is a development utility, not a public authoring API.
It scales the existing complete persistence fixture, shares immutable semantic values,
assigns unique repeated referenceable owner IDs, and retains one document resource list.
It is not a new transfer implementation. References may target the first fixture cycle;
diagram-local identities remain local. Data-heavy rows repeat deterministic source samples,
with unique dataset-local row IDs; they are stress data, not simulated scientific results.

| Profile | Blocks | Dataset rows | Encoded UTF-16 characters |
| --- | ---: | ---: | ---: |
| SMALL | 62 | 6 | 64573 |
| MEDIUM | 160 | 6 | 149256 |
| LARGE | 1000 | 6 | 988534 |
| DATA_HEAVY | 120 | 4000 | 1696165 |
| DIAGRAM_HEAVY | 180 | 6 | 145376 |
| MIXED_STRESS | 240 | 6 | 239336 |

All profiles validate with zero ERROR. Mixed profiles include the current prose, marked
text, hierarchy, equations, authored/bound tables, plots, Figures, diagrams, references
and TOC. Diagram-heavy concentrates existing diagram families and prose. Warning-level
missing references are allowed, not silently repaired.

Commands: `/scholar_dev_editor` (SMALL), or append `small`, `medium`, `large`,
`data_heavy`, `diagram_heavy`, `mixed_stress`. No additional command family is needed.

## Measurement Method

`EditorStressMeasurementTest` logs development measurements to JUnit output, never runtime
production logs. Positive width, row count, semantic equality and ordering are correctness
assertions; wall-clock values are not CI assertions. Each timed operation has one untimed
warmup and three samples, reported as median. Fixed CPU text metrics are six units per
code point and 10/14 line height; math metrics are likewise synthetic. Width is 480.
These are small diagnostic samples, not a rigorous isolated benchmark or Minecraft FPS.
JIT, GC, test order, filesystem and OneDrive make timings variable.

The initial table below was recorded BEFORE production fixes. Later values come from the
successful full-suite run. Fixture semantics and serialized character counts are unchanged;
reference run grouping and Unicode geometry corrections differ. Do not attribute all
differences to optimization: the only deliberate performance optimization is block culling.

| Profile | Initial layout ms | Later layout ms | Initial encode/decode ms | Later encode/decode ms |
| --- | ---: | ---: | --- | --- |
| SMALL | 5.108 | 1.341 | 6.980 / 5.559 | 1.590 / 1.566 |
| MEDIUM | 10.024 | 4.200 | 10.548 / 8.210 | 5.816 / 4.252 |
| LARGE | 52.179 | 56.437 | 47.110 / 45.310 | 60.278 / 48.029 |
| DATA_HEAVY | 26.450 | 12.222 | 26.351 / 13.956 | 28.375 / 16.077 |
| DIAGRAM_HEAVY | 2.218 | 1.647 | 2.889 / 2.435 | 2.994 / 1.952 |
| MIXED_STRESS | 6.801 | 2.602 | 6.884 / 4.762 | 4.942 / 3.322 |

Later structure/hit/reference/selection validation milliseconds:

| Profile | Structure | Hit | Reference resolution | Selection validation |
| --- | ---: | ---: | ---: | ---: |
| SMALL | .003 | .008 | .013 | .005 |
| MEDIUM | .009 | .011 | .039 | .008 |
| LARGE | .021 | .177 | .236 | .005 |
| DATA_HEAVY | .003 | .001 | .022 | .003 |
| DIAGRAM_HEAVY | .006 | .029 | .029 | .004 |
| MIXED_STRESS | .006 | .001 | .031 | .003 |

Scroll arithmetic was below .001 ms per sample, below meaningful precision for practical
interaction claims. It excludes drawing, input dispatch and GPU work. Selection measurement
is validity checking, not native drag latency. Hit uses the midpoint of the complete layout.
Single-reference resolution does not establish the cost of thousands of references.

LARGE edit-plus-full-relayout at blocks 1 / 501 / 999: **44.032 / 33.882 / 41.847 ms**.
This is not evidence for positional incremental layout. Full reflow remains synchronous.
No whole-layout cache, worker, asynchronous mutation, chunking or semantic virtualization
was introduced. Native font layout/drag responsiveness at this scale requires manual QA.

### Demonstrated Culling Improvement

The renderer previously entered every prose block and traversed every text run before
rejecting off-screen glyphs. A test comparator measures the old traversal versus the new
block-bounded traversal against the SAME immutable layout and midpoint viewport height 300.
The renderer uses `LaidOutBlock.intersectsVerticalViewport`, also used by the comparator.
No claim is made that these CPU traversal values are actual render frame times.

| Profile | All runs | Runs in intersecting blocks | Old traversal ms | Bounded traversal ms |
| --- | ---: | ---: | ---: | ---: |
| SMALL | 541 | 10 | .053 | .010 |
| MEDIUM | 1499 | 33 | .182 | .038 |
| LARGE | 10682 | 62 | 1.553 | .014 |
| DATA_HEAVY | 1071 | 45 | .017 | .002 |
| DIAGRAM_HEAVY | 1972 | 102 | .033 | .008 |
| MIXED_STRESS | 2207 | 15 | .059 | .003 |

Boundary intersections are inclusive, empty viewports reject, additions use long arithmetic.
Complete block iteration remains O(blocks); deep off-screen run traversal is removed.
Partially visible large tables still retain existing row/cell rendering checks.

## Correctness And Interaction

- Reflow of all profiles through 480 -> 180 -> 24 -> 480 is complete, ordered,
  nonnegative and equal on return to the original width. Atomic scientific blocks can
  exceed very narrow widths; clipping is preferable to corrupting or flattening notation.
- Prose and table wrapping keep supplementary and combining characters whole and count
  source offsets using logical characters. A glyph cluster wider than the line stays intact.
- References retain a semantic length of one. Source whitespace is a separate non-atomic
  run. Reference labels may wrap visually but hit only start/end; selection covers every
  label piece; end caret and navigation use the final wrapped piece.
- Existing source constructors for LaidOutText remain usable with atomic=false. Atomicity
  is derived layout information only, never a persisted document property.
- Scientific midpoint hits across every fixture/reflow select the correct document owner,
  including Figure wrapper ownership. Existing Figure editor/session tests retain caption
  and nested Plot/Diagram routing. Native precision clicks remain manual QA.
- New ScrollGeometry tests cover clamp after resize/deletion, minimal caret reveal and
  stable repeated visibility requests for tall atomic objects. Diagram pan/zoom stays
  transient. Resize cancels the active uncommitted gesture rather than committing it in
  a different transform.
- Open menus now own all keys before nested input, skip disabled actions/separators,
  accept Up/Down/Left/Right/Enter and close on Escape. Actions execute through the existing
  controller; no duplicate clipboard/edit implementation. Tests use inlined GLFW key
  values without adding native dependencies to the headless runtime.
- Toolbar render and popup hit origins share the same clamping helpers. Resize closes
  transient menus/dropdowns; file dialogs retain their existing native widget rebuilding
  and filename draft. Native file dialogs and centered semantic modals are not simulated
  by the headless tests.
- Missing reference/dataset paths keep existing deterministic missing labels/empty plot
  data. Nonfinite converted dataset points now omit geometry and issue one bounded warning
  per affected plot series; BigDecimal values remain exact. No scientific data is invented.
- Diagnostics remain structured (code, block, stable dataset ID, series/row context).
  No repair UI, log flood, renderer mutation, catch(Throwable) or second resolver was added.

## Persistence, Transfer And History

Actual FileDocumentStorage save/load of LARGE: **104.215 / 75.605 ms**; MIXED_STRESS:
**24.927 / 8.749 ms**. These are single end-to-end samples, not warmed medians.
Both compare exact semantic equality, canonical re-encoding and zero validation errors.
The measurements include codec and filesystem work; they are not a storage throughput guarantee.

The larger transfer test extracts 100 mixed roots with one required shared dataset:
**3.603 ms extraction / 5.580 ms paste**, single samples. Each paste is one transaction;
Undo/Redo restores exact states. A second cross-document paste receives another independent
resource closure, preserving the accepted identity policy rather than a hidden dedupe cache.
This is an explicit extractor request, not a newly added multi-object UI selection feature.

The 100-edit session includes prose, authored table text, plot and diagram title edits,
math insertion, paragraph splitting, Figure paste, captions, marks and dataset cells.
Every step validates document and selection; every Undo/Redo compares an exact EditorState.
Navigation before each edit closes typing groups intentionally, so this is 100 transactions.
The retained checkpoint topology was 101 states / 16660 block slots / 197 distinct block
objects. At roughly 4-8 bytes per reference, block slots alone represent about 65-130 KiB,
excluding list headers, snapshots, resources and changed values. This is a sharing estimate,
NOT a heap profile or claim that total memory is bounded by that number.
The capacity remains 100 and untouched AST/resources are shared; no diff history redesign.

DocumentWorkspace saved-value dirty tracking remains semantic: navigation/reflow/copy
do not dirty; edits do; save updates baseline without undo; reload starts fresh history
and runtime ownership. Existing failure/storage/unsaved tests remain enabled. New golden
coverage checks nested changes, transfer, Cut/Undo, Save/reload and later edit Undo/Redo.

## Automated Coverage

New suites (31 executions total):

| Suite | Executions | Protected behavior |
| --- | ---: | --- |
| EditorStressMeasurementTest | 6 | Fixture validity, whole layout, codec equality, diagnostic timings without timing thresholds. |
| EditorLayoutHardeningTest | 14 | Unicode offsets/splitting/cells, atomic hit/caret/highlight, viewport edges/overflow, narrow TOC, Figure bound/degraded Plot, 800 marked nodes, twelve nested Roots, all profile reflow/order/owner hits. |
| FoundationV2HardeningTest | 6 | 100 exact history transactions, golden persisted session, 60 fixed-seed mixed edits (27026), periodic codec/reflow/Undo/Redo, 100-root transfer, two large filesystem round trips. |
| MenuInputHardeningTest | 2 | Exclusive menu keys/one enabled action, narrow title wheel/keyboard reachability after resize. |
| ScrollGeometryTest | 3 | Clamp, minimal reveal, no tall-object oscillation. |

Three existing suites were corrected: CrossReferenceLayoutTest now asserts the precise
one-character atomic reference run rather than combining preceding source whitespace;
DocumentHitTesterTest and VisualLineNavigatorTest use TextBoundary counts for combining
characters rather than out-of-range UTF-16-derived expectations.

The complete M24 context/structural/history/focus, math/fraction/root/script/group,
table/plot/diagram/electrical/mechanical/Figure, M25 transfer and M26 codec/storage/workspace
regressions remain enabled. New stress suites supplement, not replace, these checks.

Golden Foundation V2 core scenario: persisted complete fixture -> load -> section navigation
and reference resolution -> all ten edit families -> Figure transfer -> Cut/Undo -> repeated
reflow -> save -> reopen exact semantic equality -> representative post-load Undo/Redo.
Core reference resolution is tested; native clicking its rendered label is manual QA.
The randomized scenario validates after each of 60 bounded edits and periodically round-trips
codec/history/layout. It is not exhaustive fuzzing or a simulation of the Minecraft event loop.

## Validation

- `./gradlew.bat test`: SUCCESS; 1456 tests, 0 failures, 0 errors, 0 skipped (129 suites).
- `./gradlew.bat build`: SUCCESS; jar assembled, no additional runtime launch claimed.
- `git diff --check`: checked separately after documentation; no whitespace errors.
- Persistence V1 schema/tags/version/wire labels and independent V1 file fixture unchanged
  during M27. New validation warning is not a serialized enum field.
- Transfer/persistence boundary tests remain green; no Minecraft imports in added ScrollGeometry
  or semantic layout/validation/data changes. Development commands remain client-owned.
- Source fragment/proof/history/persistence semantics unchanged; no runtime proof persistence.
- All previous uncommitted M25/M26 work retained. Nothing staged, committed or pushed.

## Manual Minecraft QA (15 Steps)

Start the development client with `./gradlew.bat runClient` as needed. Client startup is
not QA. These steps still require the user's actual Minecraft acceptance.

1. Open `/scholar_dev_editor`; inspect the mixed document, TOC, captions, glyphs and missing-state labels. Scroll quickly top to bottom and back.
2. Edit wrapped prose at top, middle and bottom. Use arrows, Shift selection, Home/End, Enter and Backspace merge; caret should reveal minimally without repeated recentering.
3. Click a TOC/outline entry and a reference target. Click reference label left/center/right, drag across it and select it; no caret inside its display letters. Delete a target and Undo; navigation should reflect current content.
4. Enter an Equation; edit nested Fraction/Root/Script/Group, move caret, scope Ctrl+A, Copy/Paste math, Undo/Redo, then Escape back without exiting the broader screen.
5. Edit a manual Table, a long Unicode cell, move by Tab/arrows/Enter and use its context actions/clipboard. Check selection after row/column changes.
6. Enter a dataset-backed Table and edit a numeric dataset value. Confirm the shared Plot updates and prohibited structural view actions remain disabled.
7. Edit authored Plot labels/points/series and inspect the dataset-bound Plot inside a Figure. Undo/Redo; viewing or selecting should not dirty a saved document.
8. Enter electrical and mechanical Diagrams. Zoom/pan/Fit, drag, add/delete, wire/annotate/constraint using supported actions, local clipboard and Undo/Redo. Viewport-only changes should create no history/dirty state.
9. Switch Figure wrapper -> caption -> nested content. Copy/Paste a Figure via shortcut and menu, compare caption/content, Cut/Undo and Undo/Redo paste. No ownership leak or duplicate action.
10. Open Edit/Format/context menus and toolbar dropdowns near edges. Test arrows/Enter/Escape/outside click. With a menu open, Delete/typing must not edit the nested owner. Wheel over a narrow menu title strip to reach overflow titles.
11. Resize wide -> narrow -> wide while deep-scrolled and in prose/math/table/diagram. Resize during a diagram drag and with a dropdown open; canceled gesture must not later commit, and popup hitboxes must not remain stale.
12. Save As, cancel/confirm overwrite, Save, edit/Undo/Redo and reopen. Check filename/dirty marker and semantic equality; open Save/Open picker and resize it, including its empty state.
13. Use New/Open/close with unsaved changes; exercise Save/Discard/Cancel. Test invalid filename and a malformed file. Errors must preserve the current document/history and keep a readable route back.
14. Open `/scholar_dev_editor large`; scroll rapidly, edit at top/middle/bottom, navigate, paste a Figure, resize and save/reopen. Note pauses with actual font metrics rather than relying on headless timings.
15. Open `/scholar_dev_editor data_heavy` and `/scholar_dev_editor diagram_heavy`; interact with tables/plots/diagram workspaces, inspect logs, and reject acceptance for corruption, major pauses, off-screen popups or focus leaks.

## Deferred Work And Acceptance

No async mutation, incremental layout, native file V2, persistence migration, full diagnostics
panel, accessibility redesign, public/plugin API, pagination/export or new scientific family.
Full reference target collection per resolution, complete block scans and full layout remain
potential scaling pressures; the present numbers do not justify an architecture rewrite.
Real font/GPU/GUI-scale and native modal event behavior are not established by synthetic
measurements. Unsupported host sizes smaller than a native menu/dialog remain a limitation,
not a promise of fully usable one-pixel chrome. Future genuinely larger datasets require
measurement, not arbitrary resource slicing or hidden provenance-based reuse.

Automated Foundation V2 hardening is ready for manual QA. Manual acceptance is NOT claimed.
If manual QA reveals an interaction/performance blocker, fix it within M27 before accepting
Foundation V2 or starting another scientific feature milestone.
