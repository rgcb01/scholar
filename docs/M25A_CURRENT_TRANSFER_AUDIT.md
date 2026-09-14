# M25A Current Transfer Audit

Status: ROM-6 audit document. Architecture planning only.

This document maps the current Scholar transfer and clipboard architecture as implemented after accepted M24 Editor Core Foundation V1. It does not define the future `DocumentFragment` or `DocumentTransfer` design.

## 1. Executive Summary

Scholar currently transfers semantic content through a small shared clipboard shell plus many content-specific branches.

The shared shell is:

- `ClipboardAdapter`: OS/system text clipboard abstraction.
- `ScholarClipboardService`: process-local rich sidecar keyed by exact plain-text clipboard contents.
- `ClipboardCopyResult` / `ClipboardCutResult`: plain text plus optional `ScholarClipboardPayload`.
- `BuiltInEditorActions.copy/cut/paste`: shared command path for shortcuts, menus, and context menus.
- `EditorSession.copyForClipboard/cutForClipboard/pasteFromClipboard`: semantic dispatch point.

The content-specific branches are:

- plain text selections through `PlainTextClipboard`;
- inline cross-reference selections through `InlineContentClipboardPayload`;
- math ranges through `MathClipboardPayload`;
- whole table blocks through `TableClipboardPayload`;
- whole plot blocks through `PlotClipboardPayload`;
- whole diagram blocks through `DiagramClipboardPayload`;
- whole figure blocks through `FigureClipboardPayload`;
- heading/equation/TOC blocks through `DocumentBlockClipboardPayload`;
- dataset resources through `DatasetClipboardPayload`.

The most important current gaps for M25A are:

- there is no single transfer unit for "selected semantic content plus dependencies";
- ID remapping is repeated by namespace in `EditorSession`;
- CrossReferences are copied as raw target IDs and are not rewritten;
- dataset-backed views copy their binding but not their required `ScientificDataset`;
- figures remap only the outer figure ID, not IDs inside contained plot/diagram content;
- diagram-level transfer preserves local IDs wholesale and does not collide with document namespaces, but diagram-internal references are not generalized into a document transfer closure concept.

ROM-6 can close after this audit. ROM-7/8/9/10 must decide the future transfer boundary; this document only maps current behavior.

## 2. Current Transfer Architecture Map

### Shared action path

- `BuiltInEditorActions.copy()` reads `EditorSession.copyForClipboard()`, writes `ClipboardCopyResult.plainText()` to `ClipboardAdapter.setText`, and installs or clears the `ScholarClipboardService` sidecar (`src/main/java/dev/rgcb/scholar/editor/BuiltInEditorActions.java:319`).
- `BuiltInEditorActions.cut()` computes `EditorSession.cutForClipboard()`, writes OS text first, installs/clears sidecar, then calls `EditorSession.applyCut(...)` (`src/main/java/dev/rgcb/scholar/editor/BuiltInEditorActions.java:297`).
- `BuiltInEditorActions.paste()` reads OS text, asks `ScholarClipboardService.matchingPayload(text)`, then calls `EditorSession.canPasteFromClipboard(...)` or `pasteFromClipboard(...)` (`src/main/java/dev/rgcb/scholar/editor/BuiltInEditorActions.java:341`).
- Context menus reuse these actions through `EditorContextActionResolver`, so context menu clipboard behavior shares the same enablement/execution path (`src/main/java/dev/rgcb/scholar/editor/EditorContextActionResolver.java:192`).

### Sidecar matching

- `ScholarClipboardService.install(plainText, payload)` stores one rich payload (`src/main/java/dev/rgcb/scholar/clipboard/ScholarClipboardService.java:9`).
- `matchingPayload(currentText)` returns the sidecar only if `currentText.equals(snapshot.plainText())`; otherwise it clears the sidecar and returns empty (`src/main/java/dev/rgcb/scholar/clipboard/ScholarClipboardService.java:13`).
- This means structured transfer is same-process and text-match-based. External applications receive only plain text.

### Session dispatch

`EditorSession` owns nearly all current transfer dispatch:

- copy dispatch: `copyForClipboard()` (`src/main/java/dev/rgcb/scholar/editor/EditorSession.java:1536`);
- cut dispatch: `cutForClipboard()` (`src/main/java/dev/rgcb/scholar/editor/EditorSession.java:1566`);
- paste enablement: `canPasteFromClipboard(...)` (`src/main/java/dev/rgcb/scholar/editor/EditorSession.java:1621`);
- paste dispatch: `pasteFromClipboard(...)` (`src/main/java/dev/rgcb/scholar/editor/EditorSession.java:1664`);
- block copy: `copySelectedBlockForClipboard()` (`src/main/java/dev/rgcb/scholar/editor/EditorSession.java:2647`);
- block paste branches: `pasteTableFromClipboard`, `pastePlotFromClipboard`, `pasteDiagramFromClipboard`, `pasteFigureFromClipboard`, `pasteDocumentBlockFromClipboard` (`src/main/java/dev/rgcb/scholar/editor/EditorSession.java:2707`, `2738`, `2769`, `2804`, `2935`);
- payload type filtering: `tableFromClipboard`, `plotFromClipboard`, `diagramFromClipboard`, `figureFromClipboard`, `inlineContentFromClipboard`, `documentBlockFromClipboard`, `datasetFromClipboard` (`src/main/java/dev/rgcb/scholar/editor/EditorSession.java:2873`).

### Plain text fallback

- ordinary text selections use `PlainTextClipboard`;
- whole tables use TSV from `TableTsvSerializer`;
- whole plots use `PlotPlainTextSerializer`;
- whole diagrams use `DiagramPlainTextSerializer`;
- whole figures use `FigurePlainTextSerializer`;
- document blocks use `DocumentPlainTextSerializer`;
- math fragments use `MathPlainTextSerializer`;
- datasets use `DatasetTsvSerializer`.

## 3. Entry-Point Inventory

### Copy

- User action: Ctrl+C, Edit menu Copy, context menu Copy.
- Handler: `BuiltInEditorActions.copy()`.
- Selection read: `EditorSession.copyForClipboard()`.
- Payload:
  - block selection: type-specific payload from `copySelectedBlockForClipboard()`;
  - equation editing range: `MathClipboardPayload`;
  - table cell selection: plain text only;
  - text selection containing CrossReference in one block: `InlineContentClipboardPayload`;
  - ordinary text selection: plain text only.
- Omitted:
  - plot editing selection, diagram editing selection, figure caption selection currently copy nothing through shared clipboard;
  - ordinary inline formatting is not preserved unless an inline CrossReference triggers `InlineContentClipboardPayload`;
  - multi-block text selections are plain text, not structured inline content.
- Dependencies/resources copied: only if the payload is itself a dataset resource; views do not pull resources.
- History: copy is not history.

### Cut

- User action: Ctrl+X, Edit menu Cut, context menu Cut.
- Handler: `BuiltInEditorActions.cut()`.
- Safety ordering: writes OS text before deleting; if `ClipboardAdapter.setText` fails, no document mutation occurs (`src/main/java/dev/rgcb/scholar/editor/BuiltInEditorActions.java:309`).
- Selection read: `EditorSession.cutForClipboard()`.
- Payload: same families as copy when supported.
- Mutation:
  - selected block: `DocumentEditor.deleteSelectedBlock`;
  - math range: `MathExpressionEditor.deleteSelection`;
  - table cell: `TableEditor.cut`;
  - inline CrossReference/text selection: `DocumentEditor.replaceRange`;
  - ordinary text selection: `PlainTextClipboard.cut`.
- History: one mutation transaction when deletion succeeds.

### Paste

- User action: Ctrl+V, Edit menu Paste, context menu Paste.
- Handler: `BuiltInEditorActions.paste()`.
- Payload precedence: sidecar payload if OS text matches; otherwise plain text.
- Paste dispatch order in `EditorSession.pasteFromClipboard(...)`:
  1. equation editing: native math payload or imported plain text;
  2. table editing: plain text into current cell;
  3. inline content payload;
  4. document block payload;
  5. dataset payload;
  6. table payload;
  7. plot payload;
  8. diagram payload;
  9. figure payload;
  10. ordinary plain-text paste.
- History: one semantic transaction when changed.
- Derived state: not copied; layout, refs, TOC, dataset views recompute from document snapshots.

### Structured paste

Current structured paste means "a recognized `ScholarClipboardPayload` sidecar is present":

- `InlineContentClipboardPayload` inserts exact inline nodes.
- `DocumentBlockClipboardPayload` inserts allowed document blocks after ID remapping where implemented.
- `MathClipboardPayload` pastes a `MathSequence` fragment into equation editing.
- `TableClipboardPayload`, `PlotClipboardPayload`, `DiagramClipboardPayload`, `FigureClipboardPayload`, `DatasetClipboardPayload` dispatch through dedicated branches.

There is no general "structured document fragment" abstraction today.

### Plain-text paste/fallback

- Text editing: `PlainTextClipboard.paste` normalizes CR/LF to spaces and inserts text (`src/main/java/dev/rgcb/scholar/editor/PlainTextClipboard.java:29`).
- Table cell editing: `pasteIntoTableCell` normalizes multiline text to spaces before cell insertion (`src/main/java/dev/rgcb/scholar/editor/EditorSession.java:3175`).
- Equation editing: `mathFragmentFromClipboard` imports external text through `MathPlainTextImporter` if no matching native math payload exists (`src/main/java/dev/rgcb/scholar/editor/EditorSession.java:2861`).
- Plot/diagram/figure/table whole-block fallbacks are not parsed back into blocks without a native sidecar.

### Duplicate

No explicit duplicate action was found in production action IDs or session code. Current "duplicate" behavior is achieved by copying then pasting the same payload, including repeated paste tests.

### Same-document insertion of existing semantic structures

Transfer-like insertion paths include:

- `DocumentEditor.insertBlock(...)` for existing block values;
- `EditorSession.wrapSelectedVisualBlockInFigure(...)`, which moves an existing selected plot/diagram into a new `FigureBlock`;
- `EditorSession.unwrapSelectedFigure(...)`, which replaces a figure with its contained block;
- paste branches that set or insert existing `TableBlock`, `PlotBlock`, `DiagramBlock`, `FigureBlock`, `EquationBlock`, `Heading`, and `TableOfContentsBlock` values.

## 4. Content-Type Transfer Matrix

| Content type | Copy | Cut | Paste | Payload | Stable IDs | Dependency copied? | CrossRefs rewritten? | Plain text fallback | Dedicated branch | Relevant tests |
|---|---:|---:|---:|---|---|---|---|---|---|---|
| Paragraph text | Yes | Yes | Yes | none | no | n/a | no | selected text | `PlainTextClipboard` | `PlainTextClipboardTest`, `EditorActionTest` |
| Heading text selection | Yes | Yes | Yes | none unless cross-ref selected | heading block ID not involved | n/a | no | selected text | `PlainTextClipboard` | `EditorSessionTest`, `DocumentEditorTest` |
| Whole Heading block | Yes | Yes | Yes | `DocumentBlockClipboardPayload` | optional heading ID | n/a | inline refs preserved raw | `DocumentPlainTextSerializer` | `EditorSession.copySelectedBlockForClipboard`, `remapDocumentBlockForPaste` | `DocumentStructureEditorTest.headingBlockPasteRemapsDuplicateStableId` |
| InlineContent with CrossReference | Yes, same-block only | Yes | Yes | `InlineContentClipboardPayload` | raw target IDs in refs | target not copied | no | resolved display text | `copyInlineContentForClipboard` | `CrossReferenceEditingTest` |
| Multi-block text selection | Yes | Yes | Yes | none | no | n/a | resolved to text only | newline-separated text | `PlainTextClipboard` | `PlainTextClipboardTest.cutsMultiBlockSelectionWithPlainTextClipboard` |
| Equation block | Yes | Yes | Yes | `DocumentBlockClipboardPayload` | optional equation ID | n/a | n/a | currently empty from `DocumentPlainTextSerializer` for equation block | block payload branch | `HistoryTransactionHardeningTest.pastedStableIdsAreRestoredByRedoInsteadOfRegenerated` |
| Math range inside equation | Yes for extractable range | Yes | Yes | `MathClipboardPayload(MathSequence)` | no document IDs | n/a | n/a | math plain text | equation editing branch | `EditorActionTest` math clipboard tests |
| Manual TableBlock | Yes | Yes | Yes | `TableClipboardPayload` | optional table ID | n/a | cell refs not supported by TSV; native payload preserves AST | TSV lossy for marks/header nuance | table branch | `TableClipboardTest` |
| Dataset-backed TableBlock | Yes | Yes | Yes | `TableClipboardPayload` | optional table ID and dataset binding ID | no dataset copied | cell refs not rewritten | TSV fallback is resolved table text | table branch | `DatasetViewIntegrationTest.datasetBackedTableClipboardPreservesSemanticReference` |
| PlotBlock manual/static | Yes | Yes | Yes | `PlotClipboardPayload` | no plot block ID | n/a | n/a | readable summary, not parsed | plot branch | `PlotClipboardTest` |
| Dataset-backed PlotBlock | Yes | Yes | Yes | `PlotClipboardPayload` | dataset/column binding IDs inside series | no dataset copied | n/a | readable summary from plot serializer | plot branch | dataset resolver tests, no focused closure test found |
| ScientificDataset | Yes via dataset UI/session method | no generic selection cut | Yes | `DatasetClipboardPayload` | dataset ID and column IDs | it is the resource | n/a | TSV | dataset branch | `DatasetViewIntegrationTest.datasetCopyPasteRemapsDuplicateDatasetId` |
| FigureBlock | Yes | Yes | Yes | `FigureClipboardPayload` | figure ID | contained block copied inside figure | caption refs preserved raw | figure text + contained fallback | figure branch | `FigureEditorTest` |
| DiagramBlock | Yes | Yes | Yes | `DiagramClipboardPayload` | diagram-local element/port IDs | internal graph copied as one block | n/a | descriptive text, not parsed | diagram branch | `DiagramClipboardTest`, `ElectricalDiagramClipboardInterchangeTest` |
| Electrical diagram elements | only as whole DiagramBlock | only as whole DiagramBlock | whole DiagramBlock | `DiagramClipboardPayload` | component/junction IDs and terminal IDs preserved | internal connections copied | n/a | descriptive text | diagram branch | `ElectricalDiagramClipboardInterchangeTest` |
| Mechanical diagram elements | only as whole DiagramBlock | only as whole DiagramBlock | whole DiagramBlock | `DiagramClipboardPayload` | element IDs and internal target IDs preserved | internal references copied | n/a | descriptive text | diagram branch | `DocumentValidatorTest`, diagram tests |
| TableOfContentsBlock | Yes | Yes | Yes | `DocumentBlockClipboardPayload` | no | derived headings not copied | n/a | derived Contents text | document block branch | document structure tests |
| Outline/section numbering | no direct copy | no | recomputed | none | heading IDs only | n/a | n/a | serialized display labels in some fallbacks | resolver, not payload | `DocumentStructureResolverTest`, `DocumentPlainTextSerializerTest` |

## 5. Clipboard Payload Inventory

| Payload | File | Semantic scope | Dependencies embedded? | Stable IDs stored? | Serialization/fallback | Boundary |
|---|---|---|---|---|---|---|
| `ScholarClipboardPayload` | `clipboard/ScholarClipboardPayload.java` | marker only | n/a | n/a | none | process-local sidecar |
| `ScholarClipboardSnapshot` | `clipboard/ScholarClipboardSnapshot.java` | plain text plus payload | payload-defined | payload-defined | none | process-local sidecar |
| `DocumentBlockClipboardPayload` | `clipboard/DocumentBlockClipboardPayload.java` | one `BlockNode` | only nested data in block value | heading/equation/TOC as stored | `DocumentPlainTextSerializer` by caller | internal sidecar |
| `InlineContentClipboardPayload` | `clipboard/InlineContentClipboardPayload.java` | selected `InlineContent` | only inline nodes | raw `CrossReference.targetId` | resolved plain text by caller | internal sidecar |
| `MathClipboardPayload` | `math/clipboard/MathClipboardPayload.java` | `MathSequence` fragment | nested math AST | no document IDs | `MathPlainTextSerializer` | internal sidecar; external text import exists |
| `TableClipboardPayload` | `table/clipboard/TableClipboardPayload.java` | whole `TableBlock` | table binding only, not dataset | table ID, dataset IDs/column IDs if bound | TSV | internal sidecar |
| `PlotClipboardPayload` | `plot/clipboard/PlotClipboardPayload.java` | whole `PlotBlock` | dataset binding only, not dataset | dataset IDs/column IDs if bound | readable plot summary | internal sidecar |
| `DiagramClipboardPayload` | `diagram/clipboard/DiagramClipboardPayload.java` | whole `DiagramBlock` | whole diagram graph | diagram-local element/port IDs | readable diagram summary | internal sidecar |
| `FigureClipboardPayload` | `figure/clipboard/FigureClipboardPayload.java` | whole `FigureBlock` | contained plot/diagram and caption | figure ID plus nested IDs | readable figure summary | internal sidecar |
| `DatasetClipboardPayload` | `data/clipboard/DatasetClipboardPayload.java` | one `ScientificDataset` | dataset is the resource | dataset ID and column IDs | TSV | internal sidecar |

Overlaps:

- `DocumentBlockClipboardPayload` overlaps conceptually with `TableClipboardPayload`, `PlotClipboardPayload`, `DiagramClipboardPayload`, and `FigureClipboardPayload`, but is restricted at paste to Heading, EquationBlock, and TableOfContentsBlock.
- `InlineContentClipboardPayload` exists only for inline selections containing CrossReference; ordinary formatted text selections use lossy plain text.
- `TableClipboardPayload` and `DatasetClipboardPayload` both use TSV fallbacks but have unrelated native payload semantics.

## 6. Stable-ID / Remapping Map

### Namespaces and current behavior

| Namespace | Stored on | Copy behavior | Paste behavior | Collision handling |
|---|---|---|---|---|
| Heading ID | `Heading.id()` | preserved in payload | remapped by `uniqueHeadingId` | checks all heading IDs |
| Equation ID | `EquationBlock.id()` | preserved in payload | remapped by `uniqueEquationId` | checks all equation IDs |
| Table ID | `TableBlock.id()` | preserved in payload | remapped by `uniqueTableId` | checks tables, ignores replaced table index |
| Figure ID | `FigureBlock.id()` | preserved in payload | remapped by `uniqueFigureId` | checks figures, ignores replaced figure index |
| Dataset ID | `ScientificDataset.id()` | preserved in dataset payload | remapped in `addDataset` using `uniqueDatasetId` | checks document datasets |
| Dataset column ID | `DatasetColumn.id()` | preserved | not remapped | scoped inside dataset |
| Dataset binding IDs | `DatasetTableBinding`, `DatasetPlotBinding` | preserved in table/plot payload | not remapped with dataset copy because dataset closure is absent | validator warns on missing dataset/column |
| Diagram element ID | `DiagramElement.id()` | preserved inside whole diagram | not remapped at document level | scoped inside `DiagramDefinition` |
| Diagram port ID | `DiagramPort.id()` | preserved inside element | not remapped | scoped inside element |
| Diagram connection endpoints | `DiagramEndpoint` | preserved | not remapped | must resolve inside same copied diagram |
| Mechanical constraint/part reference target IDs | mechanical elements | preserved | not remapped | validator warns if target missing |

### ID remapping code map

| Source | Responsibility | Namespaces | Callers |
|---|---|---|---|
| `EditorSession.remapDocumentBlockForPaste` | remap heading/equation IDs on document block paste | heading, equation | `pasteDocumentBlockFromClipboard` |
| `EditorSession.remapTableForPaste` | remap table IDs on table paste/replace | table | `pasteTableFromClipboard` |
| `EditorSession.uniqueHeadingId` | unique heading suffixing | heading | `remapDocumentBlockForPaste` |
| `EditorSession.uniqueEquationId` | unique equation suffixing | equation | `remapDocumentBlockForPaste` |
| `EditorSession.uniqueTableId` | unique table suffixing, replacement-aware | table | `remapTableForPaste` |
| `EditorSession.uniqueDatasetId` | unique dataset suffixing | dataset | `createDefaultDataset`, `addDataset` |
| `EditorSession.uniqueFigureId` | sanitizing and unique suffixing, replacement-aware | figure | figure wrapping and paste |
| `DocumentValidator` | detects duplicate namespaces | heading, equation, table, figure, dataset, dataset columns, diagram element/port IDs | validation tests/trust boundaries |
| `DiagramDefinition` constructor | rejects duplicate diagram element IDs and invalid endpoints | diagram-local | model construction |
| `DiagramNode` constructor | rejects duplicate port IDs within element | diagram-local | model construction |

ID remapping is not centralized. It is local to `EditorSession` plus constructors/validators.

## 7. CrossReference Behavior

Current `CrossReference` transfer behavior:

- A CrossReference is copied as semantic inline node `CrossReference(kind, targetId)` when the selected same-block inline content contains one.
- The OS fallback is the resolved display label, e.g. `Figure 1`, produced through `PlainTextClipboard` and `CrossReferenceResolver`.
- The target object is not discovered or included automatically.
- There is no current concept of "internal fragment reference" versus "external document reference".
- If both reference and target are copied through separate operations, there is no coordinated remap.
- If only a reference is pasted into a destination without the target, the inline node remains and resolves as missing/degraded.
- `CrossReferenceResolver.resolve` returns missing resolution when no target is found.
- `DocumentValidator.validateCrossReferences` reports `MISSING_CROSS_REFERENCE_TARGET` as a warning, not an error.
- Paste does not attempt heuristic retargeting.
- Section, figure, table, and equation targets use the same resolver target list, but target discovery differs by block type:
  - figures always expose `figure.id()`;
  - tables/equations/headings expose targets only when their optional IDs are present.

Tests:

- `CrossReferenceEditingTest.copyingTextContainingCrossReferenceKeepsNativePayloadAndResolvedFallback`;
- `CrossReferenceEditingTest.nativeInlineClipboardPastePreservesCrossReferenceTargetId`;
- `CrossReferenceEditingTest.targetDeletionLeavesReferenceBrokenAndUndoRestoresResolution`;
- `DocumentValidatorTest.unresolvedCrossReferencesAreWarningsAndDocumentRemainsValid`.

Gap for M25A: no transfer path currently rewrites a copied reference when its target is copied and remapped with it.

## 8. Dataset / Resource Dependency Behavior

Current dataset ownership:

- `Document` owns `List<ScientificDataset>`.
- dataset-backed tables and plots store stable binding IDs, not embedded resource data.
- `DatasetTableResolver` and `DatasetPlotResolver` derive views from the current document dataset list.

Dataset resource transfer:

- `EditorSession.copyDatasetForClipboard(datasetId)` creates `DatasetClipboardPayload(ScientificDataset)` plus TSV fallback.
- `EditorSession.addDataset` pastes the dataset and remaps only the dataset ID if duplicated.
- Column IDs remain unchanged inside the pasted dataset.

Dataset-backed table transfer:

- A dataset-backed `TableBlock` copy creates `TableClipboardPayload(table)`.
- The payload preserves `DatasetTableBinding(datasetId, columnIds)` but does not include the `ScientificDataset`.
- If pasted into a document with no matching dataset, the table remains semantic but degraded; validator reports missing dataset.
- TSV fallback serializes resolved table values when the source document has the dataset, but TSV is not parsed back into a `TableBlock`.

Dataset-backed plot transfer:

- A dataset-backed `PlotBlock` copy creates `PlotClipboardPayload(plot)`.
- The payload preserves `DatasetPlotBinding(datasetId, xColumnId, yColumnId)` in series.
- The required dataset is not copied.
- If destination lacks the dataset or columns, plot resolution becomes degraded/empty according to resolver/validator behavior.

Resource closure:

- No general resource closure concept exists.
- Existing behavior separates copying a resource (`DatasetClipboardPayload`) from copying a view (`TableClipboardPayload`/`PlotClipboardPayload`).

Tests:

- `DatasetViewIntegrationTest.datasetCopyPasteRemapsDuplicateDatasetId`;
- `DatasetViewIntegrationTest.datasetBackedTableClipboardPreservesSemanticReference`;
- `DocumentValidatorTest.missingDatasetBindingsAreWarningsAndDocumentRemainsValid`;
- `DocumentValidatorTest.missingDatasetColumnsAreWarningsAndDuplicateBindingColumnsAreErrors`.

Gap for M25A: current code cannot represent "copy this dataset-backed view plus the resources it depends on" as one unit.

## 9. Figure / Composite Content Behavior

Current Figure model:

- `FigureBlock(id, content, caption)`;
- content is restricted to `PlotBlock` or `DiagramBlock` (`FigureBlock.supportsContent`);
- caption is `InlineContent`.

Transfer:

- whole figure copy creates `FigureClipboardPayload(figure)`;
- fallback text uses `FigurePlainTextSerializer`, including derived figure number, caption text, and nested plot/diagram fallback;
- paste remaps only the outer `FigureBlock.id()` through `uniqueFigureId`;
- contained plot/diagram content is pasted as-is.

Atomicity:

- figure is atomic as a document block;
- Enter edits contained plot/diagram;
- caption editing has a separate `FigureCaptionSelection`;
- shared clipboard is disabled inside figure caption selection.

Internal content:

- content does not escape figure semantics during whole-figure transfer;
- plot content has no block ID;
- diagram content preserves all diagram-local IDs and internal references.

Places requiring edits for future figure content:

- `FigureBlock.supportsContent`;
- `DocumentValidator.validateFigure` and `validateNestedFigureContent`;
- `DocumentLayoutEngine` figure layout;
- `FigurePlainTextSerializer`;
- `EditorSession.currentPlot/currentDiagram`, `replacePlotContent/replaceDiagramContent`;
- figure paste/copy branches if new content needs ID/resource closure.

Tests:

- `FigureEditorTest.wholeFigureCopyWritesPlainFallbackPayloadAndLeavesStateUnchanged`;
- `FigureEditorTest.pastedFigureKeepsStructureButGetsUniqueStableIdWhenDocumentAlreadyContainsIt`;
- `FigureEditorTest.staleFigureSidecarFallsBackToPlainTextAndDoesNotInferFigure`.

## 10. Diagram Transfer Behavior

### Document-level diagram transfer

- whole selected diagrams copy as `DiagramClipboardPayload(DiagramBlock)`;
- OS fallback is `DiagramPlainTextSerializer`;
- paste inserts or replaces only another selected `DiagramBlock`;
- diagram local IDs are preserved wholesale;
- document-level paste does not remap diagram element or port IDs because they are scoped inside the diagram;
- no external diagram-looking text is parsed back into a diagram.

### Internal diagram structure

Generic diagrams:

- `DiagramDefinition` owns title, canvas, elements, and connections.
- `DiagramElementId` values are diagram-scoped.
- `DiagramPortId` values are element-scoped.
- `DiagramConnection` stores endpoint references by element ID and port ID.

Electrical diagrams:

- electrical components implement `DiagramElement`;
- terminal schemas own stable terminal IDs;
- connections reuse generic `DiagramConnection`;
- whole-diagram transfer preserves electrical IDs, terminal IDs, junctions, net labels, orientation, values, workspace aspect ratio.

Mechanical diagrams:

- mechanical primitives/symbols/annotations/dimensions/constraints/part references implement `DiagramElement`;
- `MechanicalConstraint` and `MechanicalPartReference` store internal target IDs;
- whole-diagram transfer preserves those internal references.

Diagram editor clipboard:

- no element-level or sub-diagram clipboard payload was found.
- diagram-specific editors generate IDs for newly authored elements but do not provide separate clipboard transfer.

Tests:

- `DiagramClipboardTest.diagramClipboardPayloadPreservesExactDiagramAstAndReferenceIds`;
- `DiagramClipboardTest.nativeDiagramPasteAtTextCaretSplitsParagraphAndSelectsInsertedDiagram`;
- `DiagramClipboardTest.nativeDiagramPasteReplacesSelectedDiagramAndSupportsUndoRedo`;
- `ElectricalDiagramClipboardInterchangeTest.nativePayloadPreservesExactElectricalMixedDiagramIncludingWorkspaceAndLocalIds`;
- `ElectricalDiagramClipboardInterchangeTest.copyingAndPastingWholeDiagramPreservesLocalIdsWithoutGlobalDocumentCollision`;
- `ElectricalDiagramClipboardInterchangeTest.externalElectricalLookingTextRemainsPlainTextAndNeverInfersDiagramAst`.

Useful current abstraction:

- `DiagramDefinition` is already a self-contained graph-like semantic unit.

Gap for M25A:

- document-level transfer treats the whole diagram as opaque payload; there is no common way to describe its internal ID closure alongside datasets, figures, and cross references.

## 11. Derived-State Findings

Current transfer paths generally copy semantic source data, not derived state.

Semantic copied:

- `BlockNode` values;
- `InlineContent`;
- `MathSequence` fragments;
- `ScientificDataset`;
- `DiagramDefinition`;
- `FigureBlock` content/caption.

Derived or fallback-only data:

- figure numbers are derived and used only in fallback text;
- section numbers and TOC entries are derived and used only in display/fallback;
- CrossReference display labels are derived and used as plain text fallback;
- dataset-backed tables/plots resolve values for rendering and some fallback serialization, but native payloads keep bindings;
- layout geometry is not copied;
- diagram routes/nets/layout are not copied as semantic transfer data;
- transient focus, menus, viewport cameras, and drag previews are not semantic transfer payloads.

No path was found that stores rendered/layout state into a semantic clipboard payload.

## 12. Undo / History Findings

Current transaction rules are consistent:

- copy creates no history entry;
- cut creates one transaction after clipboard write succeeds;
- paste creates one transaction when semantic state changes;
- block replacement paste creates one transaction;
- dataset paste creates one transaction;
- math paste/cut creates one transaction through structural math edit application;
- table cell paste creates one transaction;
- redo restores snapshot state and therefore reuses generated IDs rather than regenerating them.

Important tests:

- `HistoryTransactionHardeningTest.copyIsNotHistoryButCutPasteAndContextDeleteAreSingleTransactions`;
- `HistoryTransactionHardeningTest.pastedStableIdsAreRestoredByRedoInsteadOfRegenerated`;
- `TableClipboardTest.wholeTableCutWritesClipboardBeforeDeletingAndUndoRedoRestoresSelection`;
- `PlotClipboardTest.wholePlotCutWritesClipboardBeforeDeletingAndUndoRedoRestoresPlotSelection`;
- `DiagramClipboardTest.wholeDiagramCutWritesClipboardBeforeDeletingAndUndoRedoRestoresDiagramSelection`;
- `FigureEditorTest.pastedFigureKeepsStructureButGetsUniqueStableIdWhenDocumentAlreadyContainsIt`.

Constraint for M25B: generated IDs must remain part of snapshots; redo must not rerun remap algorithms.

## 13. Type-Specific Branch Analysis

### Legitimate semantic dispatch

- `DocumentValidator` must understand current built-in semantics.
- `DocumentLayoutEngine` must lay out each block family.
- `CrossReferenceResolver` must enumerate referenceable target kinds.
- `DiagramPlainTextSerializer` must describe concrete diagram element families.

### Repeated transfer-specific knowledge

- `EditorSession.copySelectedBlockForClipboard` chooses payload and fallback for each block type.
- `EditorSession.canPasteFromClipboard` repeats payload type order and selection restrictions.
- `EditorSession.pasteFromClipboard` repeats payload type order and delegates to type-specific paste methods.
- Dedicated paste methods repeat block-selection replacement patterns for table, plot, diagram, and figure.

### Repeated identity/remap knowledge

- `remapDocumentBlockForPaste`, `remapTableForPaste`, `uniqueFigureId`, and `addDataset` each handle one namespace locally.
- Figure wrapping also uses `uniqueFigureId`.
- Diagram element ID generation is separate inside generic/electrical/mechanical editors.

### Repeated dependency/resource knowledge

- Dataset-backed table/plot transfer knows only the binding, not closure.
- Figure transfer knows content but does not ask content for dependencies.
- Diagram transfer preserves internal references but does not expose a generic closure.

### Likely future patchwork

The most dangerous repeated decision is "what else must come along with this copied thing?" Today that answer is embedded separately in each payload branch, and often the answer is "nothing."

## 14. Fallback / Interchange Behavior

| Fallback | Lossless? | External-app compatible? | Parsed back? | IDs/dependencies survive? | Tests |
|---|---:|---:|---:|---:|---|
| Plain text selected prose | lossy for marks/native refs | yes | yes as text | no | `PlainTextClipboardTest` |
| Inline CrossReference fallback | lossy resolved label | yes | as text unless sidecar matches | no | `CrossReferenceEditingTest` |
| Math plain text | lossy vs native AST | yes | yes through limited importer | no | `EditorActionTest`, math clipboard/import tests |
| Table TSV | lossy for formatting/header metadata/native table identity | yes | not as TableBlock | no | `TableClipboardTest`, `MarkdownInterchangeTest.markdownTableSerializationDoesNotChangeTsvFallback` |
| Dataset TSV | lossy for some metadata? preserves tabular values | yes | dataset paste only with sidecar | no without sidecar | `DatasetViewIntegrationTest` |
| Plot summary | lossy/descriptive | yes | no | no | `PlotClipboardTest` |
| Diagram summary | lossy/descriptive | yes | no | no | `DiagramClipboardTest`, `ElectricalDiagramClipboardInterchangeTest` |
| Figure summary | lossy/descriptive | yes | no | no | `FigureEditorTest` |
| Markdown | interchange subset, not transfer shell | yes | yes for subset | many block types unsupported | `MarkdownInterchangeTest` |

Precedence:

- if a matching native sidecar exists, structured paste uses it first;
- if the sidecar is stale or missing, paste falls back to text behavior;
- stale sidecars are cleared on text mismatch.

## 15. Test Coverage Map

| Behavior | Test class / method | Invariant |
|---|---|---|
| Plain text copy/cut/paste | `PlainTextClipboardTest.copyWithNoSelectionIsNoOp`, `cutsSelectionAndPreservesCopiedText`, `pastesAtCaretAndOverSelection` | text clipboard behavior and no-op cases |
| Multi-block plain text | `PlainTextClipboardTest.cutsMultiBlockSelectionWithPlainTextClipboard` | multi-block text uses newline fallback |
| Shared action copy/cut/paste | `EditorActionTest.copyWritesClipboardWithoutChangingDocumentOrSelection`, `cutWritesClipboardAndChangesDocumentOnce`, `pasteReadsClipboardAndChangesDocumentWhenNonEmpty` | action path and history behavior |
| Clipboard write failure | `EditorActionTest.failedTextCutDoesNotDeleteSelection`, table/plot/diagram failure tests | cut does not mutate if OS clipboard write fails |
| Math native sidecar | `EditorActionTest.nativeFractionSidecarTakesPrecedenceOverExternalSlashImport`, `copyingAndPastingStructuralFractionThroughActionsKeepsFractionStacked` | native math preserves structure over text import |
| External math import | `EditorActionTest.externalMathPasteKeepsSlashLinearAndTokenizesLettersIndividually` | external text import policy |
| Table whole-block transfer | `TableClipboardTest.wholeTableCopyWritesTsvInstallsPayloadAndLeavesStateUnchanged`, `nativeTablePasteAtTextCaretSplitsParagraphAndSelectsInsertedTable` | native table payload plus TSV |
| Table stale sidecar | `TableClipboardTest.staleSidecarFallsBackToPlainTextPaste` | text mismatch clears payload |
| Plot whole-block transfer | `PlotClipboardTest.wholePlotCopyWritesPlainFallbackInstallsPayloadAndLeavesStateUnchanged` | native plot payload plus fallback |
| Plot no text inference | `PlotClipboardTest.stalePlotSidecarFallsBackToPlainTextAndDoesNotInferPlot` | plot-looking fallback is not parsed |
| Diagram whole-block transfer | `DiagramClipboardTest.diagramClipboardPayloadPreservesExactDiagramAstAndReferenceIds` | diagram local IDs preserved |
| Electrical diagram transfer | `ElectricalDiagramClipboardInterchangeTest.nativePayloadPreservesExactElectricalMixedDiagramIncludingWorkspaceAndLocalIds` | electrical semantics and workspace preserved |
| Figure transfer | `FigureEditorTest.wholeFigureCopyWritesPlainFallbackPayloadAndLeavesStateUnchanged`, `pastedFigureKeepsStructureButGetsUniqueStableIdWhenDocumentAlreadyContainsIt` | figure payload and ID remap |
| Dataset transfer | `DatasetViewIntegrationTest.datasetCopyPasteRemapsDuplicateDatasetId` | dataset ID remap |
| Dataset-backed table view | `DatasetViewIntegrationTest.datasetBackedTableClipboardPreservesSemanticReference` | view keeps semantic dataset binding |
| CrossReference transfer | `CrossReferenceEditingTest.copyingTextContainingCrossReferenceKeepsNativePayloadAndResolvedFallback`, `nativeInlineClipboardPastePreservesCrossReferenceTargetId` | inline ref native payload preserves raw target ID |
| Duplicate ID validation | `DocumentValidatorTest.duplicateStableIdsAreErrorsWithinEachNamespace` | duplicate IDs are validation errors |
| Degraded refs/resources | `DocumentValidatorTest.unresolvedCrossReferencesAreWarningsAndDocumentRemainsValid`, `missingDatasetBindingsAreWarningsAndDocumentRemainsValid` | missing dependencies are warnings |
| History snapshots | `HistoryTransactionHardeningTest.pastedStableIdsAreRestoredByRedoInsteadOfRegenerated` | redo does not rerun ID generation |
| Cross-system regression | `EditorFoundationIntegrationTest.clipboardCrossSystemRegressionPreservesCurrentSupportedPayloadsAndIds` | broad supported payload regression |

Focused gaps:

- no focused test proves what happens when a CrossReference and its target are copied together because no such fragment operation exists;
- no focused test proves dataset-backed plot paste into a document without its dataset;
- no test covers resource closure because resource closure is not implemented;
- no explicit duplicate command tests because no duplicate command exists;
- no element-level diagram clipboard tests because element-level diagram clipboard is not implemented.

## 16. Technical Debt / Pressure Points

### P0

No P0 production correctness defect was found that blocks closing ROM-6.

### P1

- Transfer closure is missing: M25A must decide how selected semantic content brings dependencies/resources.
- CrossReference remap semantics are undefined for copied fragments containing both references and targets.
- Dataset-backed view transfer currently preserves bindings but not required datasets.
- Stable ID remapping is repeated and namespace-specific inside `EditorSession`.
- Figure transfer remaps only figure ID and has no generic nested dependency policy.

### P2

- `EditorSession` owns too much transfer dispatch.
- Structured payload classes are parallel and non-compositional.
- Plot/diagram/table/figure paste branches repeat insertion/replacement shape.
- Plain-text fallback serializers are inconsistent by necessity, but they are not discoverable through one transfer policy.

### P3

- Context menu clipboard reuse is good; no immediate work needed.
- Markdown remains correctly separate; only documentation guardrails are needed until persistence.

## 17. Constraints M25A Must Preserve

- Copy must not mutate history.
- Cut must write clipboard text successfully before mutation.
- Cut and paste must remain one semantic transaction.
- Redo must restore generated IDs from snapshots rather than regenerating them.
- Native sidecar must not be required for external-app interoperability.
- Stale sidecar mismatch must continue falling back safely.
- Core transfer logic must remain Minecraft-independent.
- Markdown must remain interchange, not native persistence.
- Derived labels, numbering, dataset views, layout, and rendering state must be recomputed, not copied as authoritative state.
- Diagram-local IDs must remain valid inside copied whole diagrams.
- Broken references and missing dataset bindings currently remain degraded valid states unless constructors reject them.

## 18. Questions ROM-7/8/9/10 Must Answer

1. What is the transfer unit name and scope: `DocumentFragment`, `DocumentTransfer`, or another term?
2. Does a transfer unit contain blocks, inline content, resources, and remap tables, or only selected semantic roots?
3. When copying a dataset-backed table/plot, is the dataset copied, linked, omitted, or policy-dependent?
4. If copied content contains CrossReferences and their targets, are references remapped to pasted target IDs?
5. If copied content references targets outside the copied content, should those remain external, degrade, or be optionally included?
6. Are headings/equations/tables/figures/datasets all remapped by one shared identity service?
7. Should diagram internals remain opaque whole-block content for M25, or expose transfer metadata?
8. How should Figure nested content report dependencies and IDs?
9. Should ordinary formatted inline text get a native inline payload even when it has no CrossReference?
10. How much current plain-text fallback behavior must be treated as user-visible contract?
11. Is same-process sidecar still the only rich clipboard transport for M25B?
12. How should future persistence share or not share transfer closure logic?

## 19. Explicit Non-Findings / Not Currently Implemented

- No general `DocumentFragment` exists.
- No general `DocumentTransfer` exists.
- No generic graph/subdocument transfer framework exists.
- No public plugin block transfer architecture exists.
- No explicit Duplicate action exists.
- No multi-block structured clipboard payload exists for arbitrary selected blocks.
- No element-level diagram clipboard exists.
- No automatic target discovery for CrossReferences exists.
- No dataset/resource closure exists for dataset-backed views.
- No Markdown-backed clipboard transfer exists for full Scholar documents.
- No native persistence transfer reuse exists yet.

## 20. Recommendation Whether ROM-6 Can Be Closed

ROM-6 can be closed after review.

The current implementation is sufficiently mapped for ROM-7 through ROM-11 to begin design work. The audit found no need to fix production behavior during ROM-6. The main output needed by the next issues is clear: M25A must design a transfer closure boundary that replaces scattered payload/remap/resource assumptions without turning into a speculative universal graph framework.

