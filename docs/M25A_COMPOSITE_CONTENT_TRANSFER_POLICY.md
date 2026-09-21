# M25A Composite Content Transfer Policy

Status: ROM-10 architecture proposal for review. Documentation only; not M25B implementation.

Authority notice: the composite policy has been accepted as M25A input. [M25A Transfer Architecture Contract](M25A_TRANSFER_ARCHITECTURE_CONTRACT.md) is the final authority for M25B. It resolves the questions recorded below and supersedes missing-resource degraded insertion and implicit incompatible-rich-payload text fallback. This file remains supporting rationale.

Accepted inputs: ROM-6 current-transfer audit, ROM-7 fragment/transfer contract, ROM-8 resource closure, and corrected ROM-9 identity/reference policy. Where ROM-8 tentatively permits repeated cross-document resource reuse, accepted ROM-9 governs: no reuse without proven identity; no provenance cache or content deduplication.

## 1. Scope And Executive Decision

A document transfer root carries its owned semantic content intact. Resources referenced by that content travel through minimal typed resource closure. References to other document content do not enlarge that closure. Extraction preserves source IDs; insertion applies the accepted namespace-aware identity and reference policies.

Document-level Equation, Table, Plot, Diagram, Figure, and TOC remain atomic. Dedicated nested editing is a separate scope: supported math ranges remain math payloads, and current table-cell clipboard remains text. No new caption clipboard, diagram-subgraph clipboard, plot-series clipboard, cell-range selection, or multi-object selection is introduced by this policy.

Whole Figure means wrapper, caption, and owned Plot/Diagram. Whole Diagram means its authored canvas, elements, relationships, and workspace aspect ratio. Whole dataset means its columns, ordered rows, values, and metadata. Neither transfer nor clipboard performs implicit wrap, unwrap, flatten, or snapshot conversion.

This contract defines composition for a supplied ordered block set, including mixed families; it does not claim that today's UI can select that set. New selection modes are outside M25B unless separately approved.

## 2. Existing-Model Audit And Evidence

Paths below are relative to `src/main/java/dev/rgcb/scholar/` unless stated otherwise.

| Production evidence | Current model/behavior | Policy consequence |
|---|---|---|
| `document/Document.java` | Immutable block list and document-owned dataset list; dataset IDs unique in constructor. | Datasets belong in fragment resources, not inserted block lists. |
| `document/Paragraph.java`, `Heading.java`, `InlineContent.java`, `Text.java` | Ordered inline nodes; marks live on Text; Heading level and optional ID are semantic. | Preserve run segmentation, marks, order, level, and any existing identity. |
| `editor/InlineContentEditor.split/slice` | Text is sliced at logical character boundaries; CrossReference is an atomic one-unit inline node. | Never extract half a reference label; preserve current logical offsets. |
| `document/CrossReference.java`, `CrossReferenceResolver.resolve/targets` | Kind plus raw target ID; existing resolver indexes top-level SECTION/EQUATION/TABLE/FIGURE targets. | No external binding state exists; accepted cross-document degradation is Text. |
| `document/EquationBlock.java`, `math/editor/MathExpressionEditor.extractSelection/pasteFragment` | Equation has optional ID; local extraction returns MathSequence, including partial tokens and whole structural atoms; unsupported cross-slot ranges return empty. | Equation block and local math fragment remain distinct transfer units. |
| `document/TableBlock.java`, `TableRow.java`, `TableCellContent.java` | Nonempty rectangular rows, 0/1 header row, optional table ID/binding; cells own InlineContent. | Whole authored table preserves semantic cells, including programmatically supplied references. |
| `editor/TableCellTextSelection.java`, `TableEditor.copy/paste` | One cell coordinate plus directional text offsets; local copy is String; paste normalizes line breaks. | No rectangular cell-range payload exists. |
| `data/DatasetTableBinding.java` | Dataset ID plus ordered selected column IDs; empty list means all columns. | Carry binding exactly and include the whole referenced dataset. |
| `document/PlotBlock.java`, `plot/PlotDefinition.java`, `PlotSeries.java` | No plot stable ID; authored axes/series/points/options; per-series optional dataset binding. | Do not invent plot identity; preserve mixed authored/bound series. |
| `data/DatasetPlotBinding.java` | Dataset ID and x/y column IDs per series. | Discover every series dependency, potentially several datasets. |
| `document/FigureBlock.java` | Required Figure ID, InlineContent caption, and content constrained to PlotBlock or DiagramBlock. | Retain existing ownership and supported-family restriction. |
| `document/DiagramBlock.java`, `diagram/DiagramDefinition.java` | No diagram-global ID; local elements, connections, title/canvas; authored workspace aspect ratio. Definition checks element IDs, bounds, and endpoints. | Whole-block transfer preserves local identities and authored geometry. |
| `electrical/ElectricalComponent.java`, `ElectricalJunction.java` | Component kind/orientation/designator/value; ports derived from catalog; junction label and fixed terminal IDs. | Carry semantic component/junction data; recompute terminal geometry and nets. |
| `mechanical/Mechanical*.java` | Primitives, dimensions, constraints, symbols, annotations, part references are DiagramElements. Constraints/part references point to diagram-local element IDs. | Keep relationships inside owner; dimensions/annotations are not assumed to attach to another element. |
| `data/ScientificDataset.java`, `DatasetColumn.java`, `DatasetRow.java` | Dataset ID; local column IDs; ordered rows with optional row IDs; immutable values. Row-ID uniqueness is not currently enforced. | Preserve all fields; do not invent global row IDs or row-ID remapping. |
| `document/TableOfContentsBlock.java` | Empty semantic marker, no ID or stored entries. | Transfer marker; destination structure supplies entries. |
| `editor/EditorSession.copyForClipboard/cutForClipboard/pasteFromClipboard` | Shared dispatch: whole blocks, math, cell text, inline references. Caption/diagram/plot editing clipboard is disabled. | Atomic owner transfer is not triggered by nested selection. |

### Current Clipboard And Tests

Current rich carriers are `DocumentBlockClipboardPayload`, `InlineContentClipboardPayload`, `MathClipboardPayload`, `TableClipboardPayload`, `PlotClipboardPayload`, `DiagramClipboardPayload`, `FigureClipboardPayload`, and `DatasetClipboardPayload`. `ScholarClipboardService` supplies process-local rich data only while OS text matches. None is a native file format.

Current transfer loses ordinary selected text marks unless the same-block selection includes a CrossReference; multi-block text copy is plain text. Current bound-view payloads omit datasets and do not rewrite bindings. Those are implementation gaps to retire under accepted M25A, not desired semantics.

Focused evidence in `src/test/java/dev/rgcb/scholar/`:

- `editor/FigureEditorTest`: `enteringSelectedFigureEditsContainedPlotAndPreservesCaptionWhenPlotChanges`, `unwrapFigureRestoresContainedBlockAndSupportsUndoRedo`, `pastedFigureKeepsStructureButGetsUniqueStableIdWhenDocumentAlreadyContainsIt`.
- `editor/TableClipboardTest`: `tableClipboardPayloadPreservesExactTableAst`, `tsvSerializerDropsFormattingAndKeepsHeaderTextAndUnicode`.
- `editor/DiagramClipboardTest`: `diagramClipboardPayloadPreservesExactDiagramAstAndReferenceIds`, `diagramEditingModeDoesNotUseWholeDiagramClipboardPayload`.
- `editor/ElectricalDiagramClipboardInterchangeTest`: `nativePayloadPreservesExactElectricalMixedDiagramIncludingWorkspaceAndLocalIds`, `copyingAndPastingWholeDiagramPreservesLocalIdsWithoutGlobalDocumentCollision`.
- `math/editor/MathExpressionEditorTest`: `wholeRootSelectionDeletesCopiesAndPastesAsStructuralAtom`; current extract/paste tests govern supported sequence/slot boundaries.
- `data/DatasetViewIntegrationTest`: `datasetBackedTableClipboardPreservesSemanticReference`, `datasetCopyPasteRemapsDuplicateDatasetId`, `criticalPropagationTableAndPlotUpdateAfterDatasetCellEditAndUndo`.
- `validation/DocumentValidatorTest.skippedHeadingLevelsAndMultipleTableOfContentsBlocksRemainValid`.
- `editor/HistoryTransactionHardeningTest`: `copyIsNotHistoryButCutPasteAndContextDeleteAreSingleTransactions`, `pastedStableIdsAreRestoredByRedoInsteadOfRegenerated`.

These protect current behavior; they do not yet prove future fragment closure or coherent composite reference rewrites.

## 3. Transfer-Root Matrix

Classification: A independent document root; B nested-only semantic content; C resource root/owned resource part; D derived nontransferable content; E context-dependent local transfer root.

| Content | Class | Permitted extraction/carrier | Boundary |
|---|---|---|---|
| Paragraph | A | Whole supplied block; selected inline range | No stable paragraph ID. |
| Heading | A/E | Whole selected/supplied block, or inline range | Partial heading text does not copy heading ID/level. |
| Text and marked runs | B/E | Document inline range; preserved within whole owner | No implicit new block wrapper. |
| CrossReference | B/E | Whole atomic inline unit within range/owner | Target not automatically copied. |
| EquationBlock | A | Whole selected block | Expression and optional equation ID travel. |
| Math AST atoms/subtrees | B/E | Extractable MathRangeSelection as MathClipboardPayload | No automatic EquationBlock creation. |
| Authored TableBlock | A | Whole selected block | All rows/cells/header metadata travel. |
| Cell text | E | Current single-cell text selection as String | No owner table copied. |
| Rectangular table-cell range | B | Unsupported selection/transfer | No new rectangular fragment. |
| Dataset-backed TableBlock | A | Whole view plus resource closure | Resolved cells are not transfer source. |
| PlotBlock | A | Whole selected block | No point/series clipboard. |
| Plot series/point | B | Travels through Plot owner only | Editing a point does not enable partial copy. |
| DiagramBlock | A | Whole selected block | Local graph stays inside owner. |
| Diagram element/component/subset | B | Travels through Diagram owner only | Nested target selection is not a clipboard root today. |
| Ports/connections/constraints/callouts | B | Travels through Diagram owner only | No independent relationship transfer. |
| FigureBlock | A | Whole selected block | Wrapper, caption, content stay together. |
| Figure caption | B | Travels with Figure; current caption Copy/Cut/Paste unsupported | No new caption-root capability. |
| Figure content | B | Travels with Figure | Unwrap is separate explicit structural action. |
| ScientificDataset | C | Existing explicit dataset copy as resource-primary fragment | Zero inserted document blocks is valid. |
| Dataset columns/rows/values | C | Travels inside whole dataset | No column slicing or row-only clipboard added. |
| TableOfContentsBlock | A | Semantic marker | No identity/heading closure. |
| TOC entries/outline/numbering/nets/layout | D | Never semantic fragment content | Plain-text display export may include them. |

Independent model roots do not imply new UI commands: whole Paragraph extraction can support supplied content without adding paragraph object selection. No mixed atomic-block UI selection is assumed.

## 4. Atomic/Partial Transfer And Selection Boundaries

| Scope | Atomic owner? | Supported partial transfer | Result/unsupported case |
|---|---|---|---|
| Document scientific block selection | Yes | None at this scope | Whole block and owned content; never current nested target alone. |
| Prose/heading text range | No | Selected inline slices across adjacent editable blocks | Keep run marks and atomic references; block boundary is semantic, not a Text character. |
| Equation editing | Owner stays scoped | Extractable math range | MathSequence; caret Copy disabled; unsupported structural-slot crossing disabled. |
| Table editing | Owner stays scoped | Single-cell text range | Existing plain-text local clipboard; no cell-grid root. |
| Plot editing | Owner stays scoped | None through clipboard | Reject local structured paste/copy; no parent fallback. |
| Diagram editing | Owner stays scoped | None through clipboard | Reject subgraph transfer; no parent fallback. |
| Figure-caption editing | Owner stays scoped | None through clipboard today | Copy/Cut/Paste remains unsupported until separate approval. |

Normalize extraction order without copying selection direction into semantic content. History retains the original directional selection for Undo. Use current code-point/logical offset rules, including one logical unit per CrossReference. A boundary-only prose range carries the selected structural boundary and empty endpoint slices; it must not drag unselected characters. Empty inline content is valid.

For future structured prose extraction, preserve the ordered clipped Paragraph/Heading segments and their intervening boundaries. A clipped segment is not an instruction to replace destination block style or duplicate an entire source heading identity. Its boundary/endpoint role must remain explicit in ROM-11's insertion-shape contract; do not approximate partial ranges as whole-block insertion.

Current DocumentRange cannot span atomic barriers. A supplied mixed block set uses whole roots only; it does not extend that range model or harvest unselected atomic blocks between endpoints.

## 5. Figure Policy

Whole Figure extraction preserves `id`, `caption`, and the complete supported `content` value. Figure is one insertion root, not three roots. No flattening, implicit unwrap, generic block containment, or nested Figure is permitted.

Materialization order:

1. Index all fragment document-global target identities, including outer Figures, before rewriting any caption.
2. Discover datasets through contained Plot series; Diagram content has no document-owned resource today.
3. Decide destination content/resource identities once for the complete fragment.
4. Rewrite Figure ID and caption references using that shared decision set; rewrite contained Plot bindings using the dataset decision set.
5. Rebuild the Figure with the materialized owned content and caption. Preserve contained Diagram local graph unchanged.

References targeting a transferred Figure remap to its outer ID. Caption references to another transferred target remain active; proven same-document external references remain active; unproven/cross-document external references become Text with diagnostics. Contained Plot/Diagram has no document-global ID today.

Editing a contained Plot/Diagram updates the existing Figure owner, as current nested editing does. Copying that editor's target does not extract/unwrap the content. `unwrapSelectedFigure` remains a separate explicit structural operation; after unwrapping, the visual block can be copied normally.

## 6. Table Policy

### Authored Table

Whole transfer preserves row order, column count, headerRowCount, optional table ID, cell boundaries, and every cell's InlineContent. No row/column/cell stable IDs exist. Text segmentation and marks remain exact. Whole-table semantic transfer applies ROM-9 to programmatically supplied cell CrossReferences even though normal cell clipboard is text and does not offer reference authoring.

The model and `DocumentValidator.visitBlockInline` support these references. `TableTsvSerializer` currently accepts Text only and throws for CrossReference cells; that serializer limitation must not define semantic transfer eligibility. Compatible fallback generation needs the existing source resolver, not reference flattening in the rich payload.

Single-cell copy/cut/paste retains its existing local plain-text contract. No rich cell-range or table-from-cell conversion is added. Whole-table paste uses document structural insertion/replacement rules; local cell paste does not paste a whole TableBlock.

### Dataset-Backed View

Preserve the TableBlock's canonical stored rows/header fields, optional table ID, and DatasetTableBinding exactly during extraction, plus the whole available dataset in resources. Stored rows are retained model fields; do not replace them with a resolved dataset snapshot or assume they are an authored view fallback.

Preserve selected-column order and the empty-list/all-columns meaning. Same-document insertion reuses the proven existing dataset; cross-document insertion adds/remaps a dataset and rewrites the binding. The visible derived rows/headers recompute through DatasetTableResolver. Source missing resources/columns follow Sections 12 and 18; never silently convert the view to authored cells.

## 7. Plot Policy

Whole authored Plot preserves definition title, axes/ranges/scales, ordered series, names/kinds, authored points, legend/grid settings, and height. There is no Plot ID, series ID, or point ID to remap. Current property/point selection does not support local clipboard roots.

For bound series, preserve canonical series fields and DatasetPlotBinding, including x/y local column IDs. Do not export resolver-produced points as primary content. A plot may mix authored and bound series, and its series may reference different datasets; closure includes each distinct available source dataset once.

Same-document resource reuse and cross-document fresh materialization follow ROM-9. Apply one dataset decision consistently to every consuming series, including Figure-contained plots. No raw ID match or equal contents proves cross-document resource identity.

## 8. Diagram Policy

Whole Diagram is the only currently supported diagram transfer root. Preserve title, logical canvas size, complete ordered element/connection lists, authored bounds/orientations/labels, and DiagramBlock.workspaceAspectRatio. Preserve local element and port IDs exactly. Two separate diagrams may contain identical local IDs.

| Diagram family/content | Whole-root content that travels | Partial clipboard policy | Derived content excluded |
|---|---|---|---|
| Generic node | ID, bounds, label, authored ports | Unsupported | Laid-out node and port coordinates. |
| Electrical component | ID, bounds, kind, orientation, referenceDesignator, valueLabel | Unsupported | Catalog symbol strokes and terminal placement. |
| Electrical junction | ID, bounds, netLabel; semantic junction behavior | Unsupported | Computed terminal geometry/net membership. |
| Ports/terminals | Authored node ports; component/junction terminal definitions through semantic kind | Not independent roots | Render positions. |
| Connections | Both element/port endpoints and label | No edge-only/subset clipboard | Routed wire geometry. |
| Mechanical primitive | ID, bounds, kind, orientation | Unsupported | Render strokes. |
| Mechanical dimension | ID, bounds, kind | Unsupported | measuredValue/displayText recompute from bounds; no assumed primitive target. |
| Mechanical constraint | ID, bounds, kind, subjectId, optional peerId | Unsupported | Derived marker geometry. |
| Mechanical symbol | ID, bounds, kind, orientation | Unsupported | Symbol strokes. |
| Mechanical annotation | ID, bounds, kind, text | Unsupported | No hidden attachment inferred from LEADER/PART_LABEL kind. |
| Mechanical part reference | ID, bounds, targetId, itemNumber, partName, quantity, description | Unsupported | Balloon/leader rendering and derived assembly display. |
| Canvas/workspace | Authored canvas and workspace aspect ratio | Only through owner | Zoom, pan, hover, drag preview, pending connection. |

A whole Diagram includes every connection and relationship already owned by it. Valid electrical endpoints must resolve inside that same diagram. Mechanical missing-target warnings may remain as source-degraded state because current validation classifies them as warnings; preserve/report them, never infer substitute targets.

There is no current multi-element selection clipboard or partial graph normalization/remapper. Therefore M25B must not decide a new policy for endpoint closure, dangling-edge removal, copied constraints, or attached dimensions/annotations. Subgraph extraction/insertion is rejected as unsupported; it must not silently produce a partial invalid graph. A future dedicated diagram-transfer milestone must explicitly define those policies before enabling such copy.

Whole-block transfer neither merges two diagram graphs nor reconstructs local IDs. Normal layout/router/net resolver derives presentation/connectivity after insertion. Diagram internals stay opaque to document-global IdentityRemap.

## 9. Equation/Math Policy

Whole EquationBlock carries the exact MathExpression tree and its optional equation ID. The document-level ID follows EQUATION collision/remap rules; the tree does not acquire document-global identity. Preserve token segmentation, numbers/identifiers/operators/symbols, named operators/math text, nested fractions, roots, scripts, groups, and sequence boundaries as modeled. Do not serialize/reparse or normalize adjacent tokens.

Nested MathRangeSelection uses `MathExpressionEditor.extractSelection` as the authoritative eligibility boundary and produces canonical MathSequence. Same-token substrings reconstruct the same supported token category; whole structural atoms preserve their entire owned subtree. Sequence/slot-local supported ranges remain copyable. Unsupported partial cross-slot ranges remain unavailable. Complete fraction-interior extraction currently has a special supported case; do not replace the editor's predicate with a simplistic rule that every different path is unsupported. Cut additionally requires the current deletion capability, which may differ from extraction.

Native local math paste inserts/replaces directly at a supported math target, splits tokens when needed, preserves atom boundaries, and places the caret after the fragment in its receiving sequence. It never creates a full EquationBlock automatically. Whole equation document paste does not insert an EquationBlock into another equation's AST.

A MathClipboardPayload at a document text target may use its existing readable text fallback via normal text paste; that is a lossy text operation, not semantic math transfer or EquationBlock construction. Equation insertion is an explicit document action.

## 10. Inline Policy

Semantic document-inline extraction preserves selected Text slices, all BOLD/ITALIC combinations, node order, empty content, and complete atomic CrossReferences. Adjacent equal-mark runs are not automatically merged. Whole-owner extraction applies this to Paragraph/Heading, authored table cells, and Figure captions.

Internal transferred references remain CrossReference and follow the shared remap. Same-document external references stay active only with proven source identity. Unproven/cross-document external references degrade to unmarked Text containing the source resolved display label; source-unresolved references use `[Missing reference]`. Capture the display text at extraction using the source CrossReferenceResolver so later insertion needs no source Document pointer. Display snapshots are fallback/diagnostic information, never active reference identity or canonical derived numbering.

If an externally constructed fragment has no source display snapshot, use `[Missing reference]` and report unavailable source display plus degradation. Do not guess from destination numbering. Original target kind/ID survives only in transfer diagnostics for degraded references.

Plain text exports omit marks and resolve references to display text separately. Rich fragments keep active internal references regardless of fallback loss.

## 11. TOC Policy

TableOfContentsBlock is transferable semantic source content: a marker requesting a derived TOC. Its entries, outline, section numbers, labels, and heading geometry are nontransferable derived data. It has no independent stable ID.

Copy the marker only; do not copy headings. After same- or cross-document insertion, DocumentStructureResolver derives from destination headings. Multiple markers remain valid under M24A. Plain-text TOC export can capture displayed source entries, but that text is not the rich marker payload or a heading closure.

## 12. Dataset/Resource Policy

ScientificDataset is a resource root, never a BlockNode. Direct dataset copy is resource-primary transfer with zero document content roots. It includes ID, optional displayName, complete columns and their model metadata/types, ordered rows, optional row IDs, and all exact DatasetValue variants. DatasetColumn IDs are local; row indexes/order matter and optional row IDs remain local metadata without a newly invented uniqueness rule.

Bound views include the complete referenced dataset, not sliced columns, generated view data, or other consumers. Source resources are immutable snapshots with no live source-document pointer.

Missing source dataset permits extraction with a diagnostic and no resource entry. Missing column permits the existing dataset to travel intact with a diagnostic. Structural invalidity rejects materialization before editor mutation.

Insertion of an already missing dataset binding may remain degraded only if it remains unresolved in the proposed destination. If the raw missing dataset ID would bind to an unproven destination resource, reject the structured materialization before mutation. Do not invent a dataset, rename a missing ID into a sentinel, silently bind, or flatten the view. This is a necessary safety clarification to ROM-8/9's degraded-binding rule, not a new resource-reuse policy. Evaluate the complete prospective result, including resources added by other roots. Offer only an existing explicitly lossy fallback where that target/action already supports it.

Direct dataset-only transfer imports a copied resource through normal dataset identity allocation; it is not a duplicate-view reuse operation. Same-document reuse applies to dependencies of copied views, not to an explicit dataset duplication/import command.

## 13. Shared Resource Closure

For content roots R, closure is the union of available required datasets discovered through known ownership boundaries. Deduplicate by typed source dataset identity before destination remapping. Never include unrelated datasets or follow CrossReferences to target blocks.

Examples:

- T -> D and P -> D: one resource D; one destination resource decision; both consumers follow it.
- Figure F containing P -> D plus Table T -> D: still one D and one destination binding identity.
- Two bound series using different datasets: two resource entries.
- Figure containing Diagram: no current document-resource entry for diagram internals.

Source IDs remain unchanged inside the immutable fragment. Same-document view materialization reuses proven source D. Cross-document materialization adds D when unused or remaps it on collision, even if colliding contents are equal. Repeating cross-document paste creates another resource on collision; no hidden import cache. Within each paste, consumers continue sharing one resource.

## 14. Nested Identity Matrix

All authored identities are preserved during extraction. Insertion decisions below use accepted ROM-9.

| Identity | Scope | Whole document insertion | Local/nested rule |
|---|---|---|---|
| Heading ID | Document-global SECTION | Preserve if unused; allocate on collision | Partial inline slice does not supply heading target identity. |
| Equation ID | Document-global EQUATION | Preserve if unused; allocate on collision | Local math has no equation identity. |
| Table ID | Document-global TABLE | Preserve if unused; allocate on collision | Cell text has no table identity. |
| Figure ID | Document-global FIGURE | Preserve if unused; allocate on collision | Caption/content editing retains existing owner identity. |
| Dataset ID | Document-global DATASET | Dependency: proven same-document reuse; cross-document add/remap | Explicit dataset import/duplication materializes resource, not a view. |
| Dataset column ID | Dataset-local | Preserve with dataset | Bindings retain local column IDs after dataset remap. |
| Optional dataset row ID | Dataset-local metadata | Preserve with dataset | Do not elevate to global map. |
| Plot/series/point ID | Absent | No allocation | Names/indexes are not stable identities. |
| Diagram-level ID | Absent | No allocation | Document block index is not identity. |
| Diagram element ID | Diagram-local | Preserve unchanged | No supported partial graph paste/remap. |
| Port/terminal ID | Element-local | Preserve semantic ID/definition | Endpoint includes element plus port scope. |
| Connection ID | Absent | Preserve endpoints/label | No invented edge identity. |
| Mechanical constraint/part target | Diagram-local reference | Preserve unchanged with graph | Normal local validation; no global remap. |
| Component referenceDesignator/netLabel/itemNumber | Authored labels/local semantics | Preserve | Not document-global identity allocation. |
| Math tokens/subtree IDs | Absent | Preserve tree | MathPath is an address, not transferred identity. |
| TOC/numbering/layout/selection IDs | Derived/transient or absent | Do not transfer | No allocator entries. |

IdentityMap uses SECTION/EQUATION/TABLE/FIGURE/DATASET typed keys, not raw strings. One old target maps to one new target per materialization. Duplicate ambiguous source keys cannot be coherently remapped; reject before insertion rather than choose the first owner.

## 15. External Dependency Matrix

| Dependency | Destination behavior | Reporting |
|---|---|---|
| Existing source dataset required by view | Carry whole closure; reuse/add/remap per identity policy | Resource decision and any source warnings. |
| Missing source dataset | Keep unresolved only when safe; reject prospective accidental binding | Missing resource diagnostic or rejection. |
| Missing source dataset column | Carry existing dataset unchanged; preserve local binding | Missing-column degraded warning; no column guessing. |
| Reference target travels with content | Rewrite to materialized target | Active normal resolution. |
| External reference, proven same semantic document/target | Preserve active original reference | Normal resolution/warning. |
| External reference, cross-document/unproven | Degrade only that inline node to Text | Original source key plus lost-reference diagnostic. |
| Diagram-internal relationship | Stays inside whole owner | Preserve source local warnings; reject structural errors. |
| Derived dependency: TOC, labels, nets, resolved rows/points | Recompute from destination source content | Never resource closure. |
| Unsupported nested payload/target | Reject structured operation; existing explicit text fallback only | Unsupported capability; no implicit wrapper conversion. |

## 16. Same-Document Vs Cross-Document Matrix

| Operation | Proven same semantic document | Cross-document or unknown |
|---|---|---|
| Whole copied identity-bearing content | Usually collision: allocate fresh target ID | Preserve unused ID; allocate if occupied. |
| Copied view dataset dependency | Reuse still-valid source dataset identity | Materialize snapshot; never same-ID/equality reuse. |
| Repeated view paste | New content IDs; shared original resource | New content IDs on collisions; independent resource materialization per paste. |
| Reference and target included together | Shared remap | Shared remap. |
| Reference target left behind | Active only while original target identity still applies | Non-reference Text. |
| TOC | Derive from current destination headings | Derive from destination headings. |
| Whole Figure/Diagram | Retain owner composition/local graph | Same composition/local graph; apply outer/resource/reference policy. |

The editor/application supplies a trustworthy operation-scoped relation in TransferContext. `Document` has no persistent identity/provenance field. SAME_DOCUMENT is valid for an operation whose source/destination are established as the same live semantic document by the caller; equality of snapshots, text, IDs, or dataset contents is insufficient. If clipboard provenance cannot be established, use CROSS_DOCUMENT_OR_UNKNOWN. Do not infer it from a hidden sidecar import cache. ROM-11 must make the caller's proof/lifetime contract explicit; resource/reference semantics must not depend on optional cache hits.

If the original identity was removed/reused in the meantime and continuity cannot be established, treat the external reference as unproven and degrade. Merely finding an ID string again is not proof. Proven same-document already-unresolved references may retain their raw ID only if they cannot be captured by an unrelated prospective target.

## 17. Cut/Move Policy

Cut exports the same fragment/payload as Copy, writes the clipboard successfully, then deletes source content through the current editor transaction. Failed clipboard write does not delete. Dataset dependencies are not deleted merely because their view was cut. Dataset resource cut is not added.

Current clipboard cannot prove a move protocol or reserve identities. Therefore M25B has no special move-preserves-ID mode. Paste uses normal identity decisions. After cut, an old content ID may be unused and therefore preserved by collision policy; this is not a guarantee of move identity. If Undo or another insertion occupies it before paste, remap normally.

References outside the cut material are not rewritten by Cut/Paste. They may become temporarily unresolved; they resolve again only if the original identity legitimately exists after paste. Do not promise automatic reconnection across remaps/documents. Cut and later Paste are separate history transactions; no distributed move tracking.

## 18. Degradation Matrix

| Condition | Semantic insertion/result | Loss/reporting |
|---|---|---|
| Cross-document/unproven external CrossReference | Source label as unmarked Text | Explicitly lossy active-link removal; source target in diagnostic. |
| Source unresolved external reference | Text `[Missing reference]` when unproven; otherwise safe unresolved reference | Source degradation plus transfer diagnostic. |
| Missing source dataset and destination leaves binding unresolved | View retains raw binding; resource absent | Valid degraded view; normal resolver warning/display. |
| Missing source dataset would bind to unrelated destination dataset | Reject entire structured materialization before mutation | No silent semantic change or sentinel IDs. |
| Missing source column | Exact dataset/view binding retained | Existing degraded warning; no invented data. |
| Invalid/ambiguous identity or structurally invalid owned graph | Reject before mutation | Validation/transfer diagnostic; no partial graph insertion. |
| Unsupported partial diagram/table-grid/plot/caption transfer | Reject/disable | No implicit whole-owner extraction. |
| Native math pasted into body | Existing readable text paste where supported | Lossy fallback; no EquationBlock inference. |
| Other unsupported rich payload into nested editor | Reject rich transfer; preserve only existing explicit local text-paste contract | No opportunistic owner conversion. |

Materialization is all-or-nothing for the semantic roots/resources requested. Do not insert some roots and discard others silently. Lossy reference replacement is an accepted valid degraded result, not a partial transaction.

## 19. Plain-Text Fallback And Interchange

| Composite | Existing external representation | Preservation/limitations |
|---|---|---|
| Prose/heading inline | Selected display text; multi-block boundaries as newlines | Loses marks and active references. |
| Whole Heading/TOC | DocumentPlainTextSerializer source section/Contents display | Numbers/entries are exported text only. |
| Whole Equation | Current document serializer returns empty for EquationBlock | Known fallback gap; do not claim current whole-equation math export. |
| Local math | MathPlainTextSerializer readable linear text | Structural fraction versus slash is lossy; native sidecar remains exact. |
| Whole Table | TableTsvSerializer stored rows as TSV | Text only, drops marks; does not itself resolve a bound view or references. |
| Whole Plot | PlotPlainTextSerializer readable summary | Not a lossless/importable plot representation; no resource serialization. |
| Whole Diagram | DiagramPlainTextSerializer readable structural summary | Describes graph/workspace but is not a diagram importer. |
| Whole Figure | FigurePlainTextSerializer source number/caption plus visual summary | No generic Figure parser; source numbering is fallback only. |
| Dataset | DatasetTsvSerializer | Tabular values, not identity/binding round-trip. |

ROM-6's statement that whole-table clipboard always exports resolved bound rows is broader than production: `copySelectedBlockForClipboard` directly calls TableTsvSerializer on the stored block. DocumentPlainTextSerializer and Markdown table export do resolve the view, but they are separate paths. Record this discrepancy here without editing accepted audit documents.

Future fallback adaptation should use existing source resolvers for display where required, with no schema design or rich serialization hidden in text. Matching rich sidecar takes priority in a compatible semantic scope. Stale/missing sidecars retain existing text behavior; incompatible target handling must not create a new structural object from a summary. Fallback changes beyond accepted M25 transfer need explicit ROM-11 scope, especially the empty whole-equation export.

## 20. Fragment And Destination Invariants

- Extraction does not mutate source Document/resources, generate IDs, rewrite references, or inspect destination collisions.
- Whole-root owned content is complete and type-valid; Figure still contains only Plot/Diagram.
- Selected text follows current logical/code-point boundaries; CrossReference remains one atomic node.
- Fragment document-global provided keys are unambiguous within each namespace; diagram-local IDs remain independently scoped.
- Every available required source dataset is included exactly once; no unrelated dataset or CrossReference target is pulled in.
- All consumers of one transferred dataset use the same destination identity decision within a paste.
- Missing source dependencies are explicit diagnostics, not claimed complete closure; prospective insertion must not accidentally bind them elsewhere.
- Rich content is canonical AST data, never laid-out geometry, resolved tables/points, nets, numbering, selection/focus, or viewport state. Authored diagram bounds/canvas/workspace ratio and plot height do travel.
- External reference display snapshots serve the accepted lossy replacement only; they do not replace active internal references.
- Materialization leaves fragment unchanged and is deterministic for identical destination state, relation, and allocator state.
- Validate the prospective full Document before commit: `DocumentValidator` must report zero ERROR. Warnings can represent accepted source degradation. An already-error-invalid destination is not silently repaired by transfer.
- Editor supplies a valid resulting selection checked by `EditorSelectionValidator`; no source MathPath, cell coordinate, or blockIndex is copied as content.
- Content/resource/reference changes commit together in one semantic paste transaction. Redo restores that snapshot and never reruns allocation/materialization.

## 21. M24 Dependencies And Ownership

M24A supplies structural/semantic validation and distinguishes warning degradation from errors. M24B supplies atomic document selection and scoped nested selection; this policy adds no selection variants. M24C keeps menus, shortcuts, and context actions on the same action path. M24D owns structural insertion/deletion and existing selection results. M24E commits one logical edit as one snapshot transaction, with transient UI excluded. M24F/G input/focus and regression guarantees remain intact.

Fragment construction owns typed semantic extraction and resource discovery. DocumentTransfer prepares destination content, resources, remap, and diagnostics. It is pure Java and does not mutate EditorState. DocumentEditor/EditorSession applies prepared content atomically, chooses final valid selection, and creates history. Clipboard service/carriers handle OS text and rich transport, independently of persistence.

## 22. M25B Implementation Obligations

### M25B Must Implement After Consolidated Approval

- Whole-root composition and resource-primary dataset transfer within approved existing action/selection scopes.
- Exact source semantic content preservation, including marks, local math payloads, and complete Figure/Diagram ownership.
- Typed minimal dataset closure across tables, plot series, and Figure-contained plots; one resource per source identity per materialization.
- Shared namespace-aware content/resource allocation and coherent internal-reference/binding rewrite.
- Accepted external-reference Text degradation throughout all inline owners, including table cells and captions, with source label snapshots and diagnostics.
- Conservative unknown-document handling; same-document reuse/preserved references only under an explicit caller proof contract.
- Rejection of unsafe missing-resource capture and structurally invalid prospective results before mutation.
- Preserve current nested-editor payload boundaries, text fallbacks, supported structural insertion shapes, and action applicability.
- Atomic editor application, valid selection, deterministic Undo/Redo, clipboard-write-before-Cut ordering.
- Focused tests for composite roots, Figure ownership, shared resources, repeated cross-document paste, reference safety, local IDs, source degradation, and one-transaction history.

### M25B Must Not Invent

- New Figure families/arbitrary containment, implicit wrap/unwrap, authored snapshots of bound views, or math-to-equation conversion.
- Diagram subgraph/cell-grid/series-point/caption clipboard modes or mixed-object UI selection.
- Dataset-per-view duplication, column slicing, content-hash deduplication, or imported-resource reuse based on a cache.
- Sentinel IDs, second resolvers, heuristic targets, document/file UUIDs, persistent provenance, native serialization, plugin/resource registries, or a graph framework.
- A separate namespace remapper or dataset-copy branch in EditorSession for each root family.
- Automatic rewrites of references outside the transferred content, move tracking, or identity allocation during Redo.

ROM-11 consolidates insertion-shape and capability scope into implementation planning. This issue does not begin that work or authorize M25B.

## 23. Clarifications And Remaining Questions

The following require architecture review/consolidation, not implementation guesses:

1. Same-document proof: ROM-9 provides the relation but not its carrier lifetime/continuity protocol. ROM-11 must specify how callers prove same live document and still-applicable identity across clipboard delays. Unknown defaults to cross-document. No hidden semantic cache is permitted.
2. Missing-resource safety: approve rejecting structured materialization when an already missing source binding would capture an unrelated prospective destination dataset. ROM-8/9 permit degraded bindings only when they actually remain unresolved; raw IDs cannot guarantee that by themselves.
3. Structured partial prose: consolidate endpoint/boundary representation and insertion shape before M25B migrates multi-block marked text. Atomic mixed-root composition here does not solve partial selection insertion or authorize new selection modes.
4. Fallback scope: current table/reference and whole-equation fallback gaps must be explicitly included or deferred by ROM-11; semantic policy must not be inferred from a lossy serializer limitation.

Resolved here: dataset-only is resource-primary; caption/visual-only copy is not added; source display snapshots use existing source resolver or deterministic missing-reference text; whole diagrams remain local opaque values; Cut has no guaranteed move-ID protocol.

## 24. Recommendation Whether ROM-10 Can Close

Ready for architecture review. Composition rules are defined without introducing production types or new selection capabilities. Close after accepting the missing-resource safety clarification and confirming that the proof, partial-prose insertion shape, and fallback-scope gates above are recorded for ROM-11 rather than left to M25B implementation.

No production implementation, tests, accepted ADR, official roadmap, or milestone history was changed by this proposal.
