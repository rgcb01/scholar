# M25B-B - Transfer Context + Fragment Extraction

Status: COMPLETE. M25A and M25B-A are accepted. M25B remains IN PROGRESS; M25B-C is not started. Authority: [M25A_TRANSFER_ARCHITECTURE_CONTRACT.md](M25A_TRANSFER_ARCHITECTURE_CONTRACT.md). No clipboard/editor dispatch, destination operation, UI, persistence or public API is introduced.

## Architecture And API

```text
EditorSelection + source Document
  -> EditorFragmentExtractionAdapter.requestFor
  -> FragmentExtractionRequest
  -> FragmentExtractor.extract(source, request, optional runtime token)
  -> ExtractionResult.Success(fragment, sourceMetadata, diagnostics)
     OR ExtractionResult.Failure(diagnostics only)
```

`FragmentExtractor`, `FragmentExtractionRequest` and `ExtractionResult` live in `dev.rgcb.scholar.transfer`. `EditorFragmentExtractionAdapter` lives in `dev.rgcb.scholar.editor`, depends on transfer, and is not called by EditorSession/actions/clipboard. Transfer never depends on editor types.

The source token is supplied explicitly, never inferred or created by extraction. The existing destination TransferContext is unchanged. Source requests contain neither destination state nor an allocator.

## Request Model

| Request | Meaning / checks |
|---|---|
| Blocks(List<Integer>) | Explicit source block indices, unique and in bounds; ordered by source document order. Supports collections without a new UI selection mode. |
| InlineSegments(List<InlineContent>) | Already sliced semantic segments prepared by the trusted editor adapter or explicit core caller; ordered boundaries preserved. No editor positions or heading style in core. |
| Datasets(List<String>) | Explicit unique dataset IDs in request order; complete resource-primary transfer only. |
| Rejected(code, reason) | Adapter failure/unsupported scope carried as an explicit request rejection. |

Request collection constructors defensively copy and reject null entries. Empty/out-of-bounds/duplicate source requests, missing resources and null extraction inputs return Failure. Programmer-invalid value construction (e.g. null list entries) remains a constructor invariant, not a partially valid request.

InlineSegments is intentionally a prepared semantic-value request, not a second range model. The adapter owns source selection validation and exact slicing using existing package-private `InlineContentEditor.slice`/`TextBoundary`. This avoids moving or duplicating tested editing helpers and keeps transfer independent of editor. Core direct callers are trusted producers of exact source slices; this is not a serialized/untrusted import API, nor does core attempt to reconstruct or verify a source range from supplied inline values. Future editor integration should always use the validated adapter.

## Selection Matrix

| Existing selection | Result |
|---|---|
| Noncollapsed TextSelection in adjacent Paragraph/Heading blocks | InlineSegments, exact normalized range; direction remains in the original selection, not fragment. |
| Boundary-only TextSelection | Two empty segments, preserving the structural boundary. Empty interior blocks remain segments. |
| BlockSelection | One complete selected Heading/scientific block/TOC; existing Paragraph BlockSelection is invalid. |
| All visible Heading text via TextSelection | InlineSegments; no SECTION identity/level. |
| Heading via BlockSelection | Whole Heading; SECTION identity retained. |
| Collapsed text caret | UNSUPPORTED_SOURCE_SELECTION. |
| Invalid selection/offset or text range crossing atomic blocks | MALFORMED_FRAGMENT; no fallback normalization. |
| EquationEditingSelection | Unsupported for DocumentFragment; existing MathClipboardPayload/MathSequence extractor stays local. |
| TableEditingSelection | Unsupported for DocumentFragment; existing single-cell plain-text copy stays local. No grid/row/column semantic clipboard is invented. |
| PlotEditingSelection / DiagramEditingSelection | Unsupported; no owner widening or partial graph/series transfer. |
| FigureCaptionSelection | Unsupported. It has range offsets but current clipboard does not support caption copying, and the final M25A contract explicitly freezes that guard. |

BlockSelection stores only one index. Core Blocks requests can supply several supported roots, including sparse ordered collections, without enabling mixed-object UI selection or harvesting atomic blocks through a text range. Caption range representation alone is not authorization to add a clipboard capability.

## Semantic Preservation

All eight block families retain the original immutable semantic values. Whole authored tables retain rows/header count/cells/marks/reference atoms. Bound tables retain canonical stored rows plus binding, not resolved visible cells. Plots retain axes/settings/height/series/authored points/bindings, not generated bound points. Whole equations retain their MathExpression and optional ID; math ranges never become EquationBlock.

Figure retains one wrapper root, caption and supported Plot/Diagram content. Its plot dependencies are discovered through ownership; its content is never added as another top-level root. Caption references remain raw semantic nodes.

Whole Diagram retains its complete owner graph: title/canvas/bounds/ports/endpoints/connections/electrical or mechanical elements and authored workspace ratio. Local IDs remain embedded and unchanged; no graph merge/remapper or inferred nets are produced. TOC remains only its semantic marker, without headings/numbering/entries as dependencies.

Inline extraction preserves run segmentation, marks, mixed node order, atomic CrossReferences and current Unicode grapheme boundaries. It never resolves/remaps/degrades AST nodes. Only separate companion textual exports use the existing resolver, as required by M25A.

## Dataset Closure

The existing FragmentIdentityIndex typed enumeration is reused via an internal `requiredDatasets` helper, preserving first-discovery order before index sets are frozen. This avoids another independent Table/Plot/Figure dependency switch.

1. Enumerate DATASET references from supplied Table bindings and Plot series, including Figure-owned Plot.
2. Resolve each unique source dataset ID against source Document.datasets.
3. Include the complete immutable dataset once; retain all columns/ordered rows/values/metadata/local IDs.
4. Report every missing required dataset as MISSING_REQUIRED_RESOURCE and fail the entire request.

Table + Plot -> D and Figure(Plot -> D) + Table -> D each carry one D. Multiple datasets remain separate, including equal values with different IDs. Discovery never follows CrossReference targets, TOC headings, dataset consumers, or unrelated resources. Current datasets have no further resource dependencies.

M25A permits diagnosed incomplete extraction; M25B-B deliberately implements the requested strict actionable extractor: required dataset absence yields Failure without a fragment. The M25B-A value model can still represent incomplete membership for other diagnostic callers. This narrower policy does not change accepted pre-insertion rejection or permit substitute resources/destination reuse.

## Source Validation And Identity

Selected owned roots plus discovered resources are validated through the existing DocumentValidator in a temporary semantic projection. Inline segments are wrapped in temporary Paragraphs only for validation, never in the returned fragment. Resource-primary validates resources with zero block roots.

Fatal selected structural errors reject extraction. Unrelated invalid source content does not automatically block otherwise safe extraction. Involved source identities are checked against actual top-level targets: ambiguous duplicate IDs for provided/referenced keys reject rather than picking one target. Ambiguous unrelated IDs are not proof of anything and are not captured.

Projection-only missing CrossReference warnings are excluded because external source targets may legitimately be outside the fragment. Source missing-reference handling instead uses the original source resolver and metadata below. Other selected warnings, such as missing columns or mechanical targets, become SOURCE_VALIDATION_WARNING retaining the validator code/message; no repair occurs. Resource/schema validity and source warnings are distinct from destination eligibility.

The fragment's existing derived/verified identity index records actual whole traveling targets and datasets. Partial headings never provide their SECTION ID. References stay in the referenced set regardless of whether their target travels. Plot/Diagram/local IDs are not invented as global namespaces.

## Metadata And Diagnostics

Success captures the supplied optional token, original immutable target witnesses for involved keys, complete dataset witnesses, and source reference display exports keyed by original typed identities. Witness instances are unchanged, enabling later conservative applicability proof. An external target witness is companion data only, not fragment root/resource closure.

Already missing source references receive `[Missing reference]` export, SOURCE_DISPLAY_UNAVAILABLE warning and no target witness. The active source reference remains unchanged in the fragment; no Text degradation occurs here. Captured text is operational export data, not canonical labels. Unknown token remains unknown even when witnesses exist. No live source Document/editor/history/cache is retained.

Success and Failure are closed immutable result variants. Success cannot contain ERROR diagnostics. Failure requires at least one ERROR and contains neither fragment nor source metadata, preventing partial extraction use. Unsupported nested selections return an explicit error result rather than throwing. Constructor violations/ordinary malformed source requests map to rejection at the extraction boundary where applicable.

## Legacy And Deferred Work

EditorSession, clipboard payload families, sidecar, OS clipboard, action enablement and Copy/Cut/Paste are untouched. Existing math-local extraction and cell text copy remain available through their current APIs; unsupported here means unsupported as a document fragment, not disabled in the legacy editor.

Deferred: destination collision scans/IDs/allocator/remap, per-target proof applicability, reference degradation, dataset reuse/resource insertion, materialization, receiving-shape staging/insertion, selection placement/history commit, clipboard migration, UI and persistence. M25B-C may build closure/remap planning on these requests/results without redesigning source extraction.

## Validation

53 new test executions: FragmentExtractorTest (31), EditorFragmentExtractionAdapterTest (19), ExtractionResultTest (3), with shared test-only ExtractionFixtures. Includes 75 fixed-seed mixed-document extractions. Existing suites remain unchanged, including clipboard/actions/context/history regressions and transfer boundary tests.

- `gradlew.bat test`: BUILD SUCCESSFUL; 1243 tests, zero failures/errors/skips.
- `gradlew.bat build`: BUILD SUCCESSFUL.
- `git diff --check`: passes; repository LF/CRLF notices only.
- Transfer scan/boundary test: no platform/client/editor/clipboard/layout/render dependencies.
- No new destination logic or legacy clipboard integration. No manual Minecraft behavior change is claimed; full M25B still requires manual QA after integration.

Existing uncommitted M25A/M25B-A changes are preserved. No ADR, commit or push is created.
