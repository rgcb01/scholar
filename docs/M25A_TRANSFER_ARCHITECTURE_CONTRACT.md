# M25A Transfer Architecture Contract

Status: accepted M25A architecture. M25B was subsequently implemented and manually accepted;
the design-time status below is retained as historical context for ROM-11.

This is the authoritative M25A contract for M25B. Earlier M25A issue documents remain evidence and rationale; this contract supersedes their conflicting, tentative, or deferred wording. Architecture completion is not implementation completion, native-format stabilization, or public API publication.

## 1. Source Of Truth And Scope

Authority order: the final accepted clarifications supplied for consolidation, accepted ROM-9 reference correction/composite policies, this consolidated contract, earlier issue rationale. Production code/tests describe the current implementation, not unimplemented M25B behavior.

| Audited source | Role and authority |
|---|---|
| `M25A_CURRENT_TRANSFER_AUDIT.md` (ROM-6) | Current implementation inventory; not the future policy. |
| `M25A_DOCUMENT_FRAGMENT_TRANSFER_CONTRACT.md` (ROM-7) | Fragment versus materialization boundary; subordinate rationale. |
| `M25A_RESOURCE_CLOSURE_POLICY.md` (ROM-8) | Minimal whole-dataset closure rationale; provisional destination/provenance rules superseded below. |
| `M25A_IDENTITY_REFERENCE_POLICY.md` (ROM-9) | Accepted namespace/remap policy and corrected external-reference Text degradation. |
| `M25A_COMPOSITE_CONTENT_TRANSFER_POLICY.md` (ROM-10) | Accepted owned-content rules; remaining consolidation questions resolved here. |
| `M24_EDITOR_FOUNDATION_V1.md` and M24B/C/D/E/F contracts | Frozen selection/action/structural/history/input foundation. |
| `ARCHITECTURE.md` | Current layer map; earlier post-M24 planning audits remain in Git history and do not override accepted contracts. |
| `PROJECT_SPEC.md`, `ROADMAP.md`, `MILESTONE_HISTORY.md` | Project scope and status; updated for architecture completion only. |
| `adr/README.md` and relevant existing ADRs | Decision index and historical scope: 0024/0026/0027/0031/0034; 0053/0054/0056/0059/0060; 0090/0091/0095/0102; 0179/0182/0214/0215; 0225/0229; 0246; 0252-0275. |

Repository inventory found five M25A issue documents before this consolidation; no separate transfer-operation contract, implemented DocumentFragment/DocumentTransfer, native persistence schema, or M25A ADR existed. The ADR index ends at 0275. No new ADR is necessary: the final contract captures these decisions together without duplicating issue rationale or changing public compatibility promises.

Production evidence was checked in `document`, `data`, `math/editor`, `editor`, `clipboard`, `diagram`, `electrical`, `mechanical`, and `validation`. In particular: `EditorSession.copyForClipboard/cutForClipboard/pasteFromClipboard`, `DocumentEditor.insertBlock/replaceRange/insertParagraphBreak`, `InlineContentEditor.slice`, `MathExpressionEditor.extractSelection/pasteFragment`, `DocumentValidator`, `CrossReferenceResolver`, dataset resolvers, and current focused clipboard/history tests.

Scope: semantic document transfer through current supported editor domains and supplied whole-block collections. No new selection modes, features, generic graph framework, Figure families, plugin/resource registries, persistence, or M25B code.

## 2. Contradiction Audit And Resolutions

Each resolution is local to the stated topic. Supporting documents retain their earlier wording with an explicit authority notice.

| Topic | Document A | Document B / final clarification | Conflict? | Resolution and authoritative final rule |
|---|---|---|---|---|---|
| Missing required dataset | ROM-8 sections 12/20; ROM-9 sections 18/27 | Final missing-dataset clarification | YES: earlier degraded insertion was allowed. | Extraction can report an incomplete closure; insertion rejects unless explicit same-document applicability proves reuse. Even an absent destination ID is not permission to insert a broken view. |
| Missing dataset, no destination collision | ROM-10 sections 12/18 | Final missing-dataset clarification | YES: earlier safe-unresolved insertion was allowed. | Same strict rejection rule; no degraded-without-resource paste outcome in M25B. |
| Repeated cross-document datasets | ROM-8 sections 15/17 | ROM-9 sections 12/13 | YES: ROM-8 tentatively allowed prior-import reuse. | Every cross-document materialization adds/remaps its resource; no import cache, ID equality, or content equality reuse. |
| Same-document proof | ROM-8 section 16 suggests inference from matching identity | ROM-9 and final proof clarification | YES / insufficient specification. | Explicit runtime context plus applicability witness; equal semantic IDs never prove document relation. Section 9 freezes the sufficient proof. |
| External CrossReference live/raw ID | ROM-6 documents current raw-ID copy; ROM-7 permits unspecified unresolved states | Corrected ROM-9/final accepted reference rule | Current-versus-future difference, not a production claim. | Internal remap, proven same-document preservation, otherwise Text. No sentinel IDs or resolver change. |
| Partial Heading content | ROM-10 section 4 leaves clipped block segments/insertion shape open | Final partial-prose clarification | Unresolved wording, resolved here. | Inline segment content has no source heading level/ID; only whole block extraction supplies a SECTION target. Section 13 defines receiving shape. |
| Recognized payload in wrong scope | ROM-10 sections 9/18 permit math-to-body text fallback | Final fallback clarification | YES: implicit flattening is too broad. | Recognized incompatible rich payload rejects. Explicit plain-text conversion exists only for current table-cell textual paste, not general math-to-body paste. |
| Fallback display snapshots versus derived-state ban | ROM-9/10 source labels | Final ban on rendered labels in fragment | Apparent wording conflict. | Semantic fragment stores no labels. Separate source text export metadata supplies deterministic accepted reference degradation; no geometry, resolver cache, or active semantic label snapshot. |
| Table clipboard resolved rows | ROM-6 matrix | `EditorSession.copySelectedBlockForClipboard` calls `TableTsvSerializer` on stored block | YES: audit overstates current fallback. | Actual current path exports stored rows; future source-display fallback uses normal resolver without changing rich view semantics. |
| IDs in replacement | ROM-9 collision policy | M24D / ADR 0265 replacement exclusions | NO CONFLICT when candidate survivors are explicit. | Allocate against surviving destination identities plus reservations; an ID removed by an approved replacement can remain unused. |
| Source duplicate IDs | ROM-9 extraction may carry diagnostics | ROM-10 prospective valid-result rule | NO CONFLICT. | Diagnostic extraction may succeed; ambiguous provided identities reject pre-insertion. Never pick first target. |
| Figure composition/local diagram IDs | ROM-7/8/9 | ROM-10/final composite policy | NO CONFLICT. | Whole Figure retains Plot/Diagram owner; local graphs stay opaque. |
| TOC / shared-resource closure | ROM-8/9 | ROM-10/final composite policy | NO CONFLICT. | Transfer TOC marker, not headings; union datasets once by source identity. |
| Cut/move / redo | ROM-7/9 | ROM-10/M24E | NO CONFLICT. | Export plus delete; separate Paste; no move protocol; redo restores exact IDs. |
| Extraction/insertion ownership | ROM-7 | ROM-8/9/10 | NO CONFLICT. | Extraction preserves source; transfer materializes; editor applies/history/selects. |
| Atomic scientific selection | M24B/ADR 0253 | All M25A policies | NO CONFLICT. | No text range crossing atomic barriers; no new mixed-object UI selection. |
| Source degradation versus validation | Existing validator warnings | Final strict resource insertion rule | NO CONFLICT. | A source document may be valid-but-degraded while its selected view is not eligible for paste. Validator permissiveness does not override transfer policy. |

## 3. Terminology

| Term | Exact meaning |
|---|---|
| DocumentFragment | Immutable source-side semantic document content and scoped resources; no destination IDs. |
| Transfer root | Explicit selected/supplied semantic unit that defines owned content to extract. |
| Fragment content | Whole ordered blocks, document-inline segments, or resource-primary selection. Not a second AST. |
| Fragment resource | Document-owned required semantic data; currently ScientificDataset only. |
| Resource closure | Union of required resources through supported ownership boundaries, once per typed source identity. |
| Source context | Extraction-time source Document/selection interpretation and runtime proof/export context; not retained as a live Document pointer. |
| Destination context | Current destination Document, allowed receiving shape, and caller-provided runtime proof facts. |
| Transfer context | Pure operation input for materialization: destination, allocator, explicit proof, source text metadata. |
| Same-document transfer | Runtime context proves source/destination share one live semantic document; individual target applicability must also be established. |
| Cross-document transfer | Different or unproven live-document relation; includes restart/cross-process/unknown origin. |
| Semantic payload | Recognized Scholar carrier of a fragment or supported editor-local semantic value. |
| Plain-text fallback | Interoperable OS text; intentionally lossy and separate from fragment semantics. |
| Internal reference | Typed CrossReference target is provided by whole transferred semantic content. |
| External reference | Target is not provided by transferred content; does not cause target/resource discovery. |
| Identity remap | One typed source-to-destination decision per provided document-global identity. |
| Owner-local identity | Identity meaningful only inside its resource/composite, such as dataset column or diagram element. |
| Degradation | Accepted successful semantic loss, explicitly diagnosed; primarily external reference to Text. |
| Rejection | No destination mutation/history; operation reports unsupported, invalid, or incomplete semantics. |
| Transfer diagnostic | Operational code/severity/context explaining extraction, remap, degradation, or rejection; not inserted AST state. |

Copy means extract/export without mutation. Cut means successful export then source deletion. Paste means prepare/apply at a destination. Neither means distributed move or guaranteed ID preservation. External reference and external clipboard text are different concepts.

## 4. Final Fragment Shape And Carrier Boundary

Conceptual contracts only; no Java type is added by M25A:

```java
record DocumentFragment(
    FragmentContent content,
    List<ScientificDataset> resources,
    FragmentIdentityIndex identities
) {}

sealed interface FragmentContent {
    record Blocks(List<BlockNode> roots) implements FragmentContent {}
    record InlineSegments(List<InlineContent> segments) implements FragmentContent {}
    record ResourcePrimary(Set<StableIdentityKey> selectedResources) implements FragmentContent {}
}

record FragmentIdentityIndex(
    Set<StableIdentityKey> provided,
    Set<StableIdentityKey> referenced
) {}

record ExtractionResult(
    DocumentFragment fragment,
    SourceTransferMetadata sourceMetadata,
    List<TransferDiagnostic> diagnostics
) {}
```

Fields are immutable/defensively copied. Use closed internal families; runtime names are implementation details, responsibilities are frozen. ResourcePrimary selects datasets already stored once in resources, not a second embedded dataset copy. It permits zero blocks but requires at least one selected resource. Blocks requires at least one root for an actionable transfer. InlineSegments requires at least one segment; one empty segment is a no-op, whereas two empty segments represent a selected structural boundary.

Source IDs and raw semantic reference kind/ID stay exactly in AST content. Identity indexes must agree with that content, not assert extra target inclusion. Inline segments carry no block style, heading level/ID, source positions, or selection direction. Consecutive segments imply exactly one selected block boundary; empty segments preserve boundary-only selections. No inline newline encoding or global text offsets.

Required source resources unavailable at extraction are reported through diagnostics/reference dependency inspection; no stub dataset. A diagnosed incomplete fragment is not automatically insertable.

The fragment MUST NOT contain screen/widgets/Minecraft objects, selection, focus, viewport, history, live source Document, resolver caches, rendered labels, TOC entries/numbering, destination remaps, persisted origin, or synthetic unresolved IDs. Authored geometry and settings already in Plot/Diagram AST are semantic and do travel.

SourceTransferMetadata is an explicit, immutable, nonpersistent companion: runtime document token, applicability witnesses, and source textual reference exports keyed by typed original target. No source Document pointer, hidden lookup cache, file UUID, path, or history stack. Clipboard carrier owns this companion; it is never part of inserted Document or native persistence. Textual export labels are permitted here only to supply accepted degradation/fallback, not as canonical resolved reference state.

## 5. Responsibility Boundaries

| Owner | Responsibilities | Prohibited responsibilities |
|---|---|---|
| Selection/editor adapter | Interpret existing selection; identify roots/ranges; check target scope; stage structural insertion/deletion; final selection/history. | Per-family resource/remap/reference policy; implicit new selection mode. |
| Fragment extraction | Exact semantic slices/owned roots, source identity index, typed dependency discovery, source text exports/diagnostics. | Destination collisions, new IDs, source mutation. |
| Fragment value | Immutable source semantics/resources and verifiable indexes. | Resolve labels, operate clipboard, choose destination placement. |
| DocumentTransfer | Destination-aware identities/resources, reference/binding rewrite, materialized output and diagnostics. | Mutate editor/Document, run history, select/caret, query OS clipboard. |
| DocumentEditor/EditorSession | Apply one prepared edit using structural contracts; validate complete candidate and selection; commit once. | Second transfer implementation or resolver. |
| Validators/resolvers | Existing structural/semantic checks and normal derived resolution. | Transfer resource-reuse policy, heuristic repairs. |
| Clipboard/interchange | OS text, rich sidecar and explicit companion lifetime; fallback generation; transport matching. | Define semantic identity from text equality, persist transfer format. |

Dependencies: pure `dev.rgcb.scholar.transfer` -> document/data and required pure validation primitives. Editor -> transfer. Clipboard may carry transfer values. Transfer has no editor/session/history/client/render/layout/Minecraft/persistence dependency. Editor-local math stays in its current subsystem.

## 6. Extraction Pipeline

1. Validate source selection against the source Document with the existing domain rules.
2. Classify whole document roots, document inline range, resource-primary copy, or supported editor-local copy. Do not widen a nested selection to its owner.
3. Normalize content order. Slice Text by current logical/code-point boundaries; preserve marks and atomic CrossReferences.
4. Whole roots carry every owned semantic value, not resolved views. Partial Paragraph/Heading ranges become InlineSegments; no source Heading target identity is provided.
5. Enumerate whole-root provided typed identities and referenced CrossReference/resource keys. Discover datasets under Tables, Plot series, and Figure-owned Plot.
6. Resolve/deduplicate source datasets using Section 8; report unavailable dependencies.
7. Obtain source reference display text through existing CrossReferenceResolver for the companion export map; use `[Missing reference]` when unresolved.
8. Capture explicit runtime document/witness metadata only when available. Freeze the fragment, companion, and diagnostics without retaining source selection or Document.

Unsupported source domains/ranges produce a rejection result. Malformed selection/content cannot produce an actionable fragment. Source warnings can be extracted; duplicate ambiguous provided identities or structurally invalid owned content cannot be inserted. A source invalidity elsewhere does not authorize copying it or repairing it.

Extraction never looks at a destination or changes source IDs/references. One immutable fragment can be materialized repeatedly.

## 7. Final Transfer-Root Matrix

| Content | Can be root? / selection context | Atomic? / partial support | Resource closure | Semantic carrier | Destination restrictions |
|---|---|---|---|---|---|
| Paragraph | Whole supplied block; current text range | Whole root intact; inline slices allowed | None | Blocks / InlineSegments | Body blocks or editable document-inline target. No new paragraph object selection. |
| Heading | Whole BlockSelection or supplied block; text range | Whole intact; partial becomes inline only | None | Blocks / InlineSegments | Whole level/ID only in Blocks; no source Heading from partial range. |
| Inline range | Current TextSelection over adjacent editable blocks | Reference units atomic; structural boundaries retained | None | InlineSegments | Editable document prose; no atomic barrier crossing. |
| CrossReference | Selected atomic inline node or owned node | No partial label extraction | No target closure | InlineSegments or owner Blocks | Internal remap; proven external preserve; otherwise Text. |
| EquationBlock | Whole BlockSelection/supplied block | Atomic at document scope | None | Blocks | Document structural target; never inserted into math AST. |
| MathSequence/subtree/token part | Extractable current MathRangeSelection | Current extractor eligibility only | None | MathClipboardPayload(MathSequence) | Math editor only; no implicit equation/body conversion. |
| Authored TableBlock | Whole BlockSelection/supplied block | Whole structure/cells | None | Blocks | Document structural target. |
| Table cell text | Current single-cell directional text range | Partial text only; no grid range | No dataset closure for displayed text export | Existing local String | Current cell textual destination; no table construction. |
| Dataset-backed Table | Whole BlockSelection/supplied block | Atomic view with binding | Whole dataset | Blocks plus resources | Required closure or explicit same-document applicability; otherwise reject. |
| PlotBlock authored | Whole BlockSelection/supplied block | Atomic; no series/point clipboard | None | Blocks | Document structural target. |
| Dataset-backed Plot | Whole block; bindings per series | Atomic; mixed series preserved | Every distinct whole dataset | Blocks plus resources | Same required-resource rule for every consumer. |
| DiagramBlock, electrical/mechanical/generic | Whole BlockSelection/supplied block | Complete owner graph; no partial clipboard | None currently | Blocks | Body/Figure ownership unchanged; no subgraph merge. |
| Diagram elements/ports/edges/constraints | Nested only in whole owner | Partial clipboard unsupported | Not document resources | Whole Diagram owner only | Editing target is not a transferable root. |
| FigureBlock | Whole BlockSelection/supplied block | Wrapper/caption/visual atomic | Contained Plot dependencies | Blocks plus resources | No unwrap, nested Figure, or arbitrary content family. |
| Figure caption/content alone | Nested only; current clipboard unsupported | No new partial capability | Through whole owner only | Whole Figure owner only | Explicit unwrap remains separate action. |
| TOC | Whole BlockSelection/supplied block | Semantic marker | No heading closure | Blocks | Destination derives entries; multiple TOCs valid. |
| ScientificDataset | Existing explicit dataset copy | Complete resource | Itself once | ResourcePrimary plus resources | Resource import; no BlockNode insertion. |
| Dataset columns/rows/values | Owned parts only | No new partial resource copy | Whole dataset | ResourcePrimary/closure owner | Preserve local identities/value order. |
| Outline/TOC entries/labels/nets/layout | Not semantic transfer roots | Derived | None | Plain display export only | Never inserted as canonical derived state. |

Blocks supports ordered collections of current types for core composition/testing; M25B adds no arbitrary mixed-object selection UI. A TextSelection must not harvest atomic blocks between endpoints. Local Cut requires both extraction and deletion eligibility; extraction alone does not imply mutability.

## 8. Resource Closure Algorithm

Current transferable document-resource family is exactly ScientificDataset. The complete dataset is the ownership unit: include all columns, ordered rows, values, optional row IDs, and model metadata/types. No selected-column slicing, consumers, unrelated datasets, or formulas/units features are invented.

```text
required := ordered distinct DATASET keys
visit roots in content order:
  Table: add binding.datasetId when bound
  Plot: visit series in order; add every binding.datasetId
  Figure: visit supported owned Plot/Diagram content
  Diagram/Equation/Paragraph/Heading/TOC: no resource emitted
  CrossReference: do not traverse target
for each required key in first-discovery order:
  find source dataset
  if found: store immutable whole value once
  otherwise: emit MISSING_REQUIRED_RESOURCE diagnostic
resource-primary copy: include explicitly selected dataset values once
validate duplicate entries/schema shape and freeze resource list
```

Current datasets do not reference other resources, so recursion stops there. Future resource-dependent types require a deliberate typed policy, not a graph walker or plugin registry. First-discovery order is deterministic; semantic results cannot depend on map iteration order.

T -> D and P -> D yields exactly one source D. Figure(Plot -> D) plus Table -> D still yields one D. Several distinct datasets all travel; all referenced consumers use their own typed source key.

Large datasets remain complete. No lazy/chunked resource transfer, unbounded new transport format, or large-resource UI is added; measurement/size-policy work remains later. Reject malformed values at trust boundaries; never deserialize Java objects/classes from external clipboard.

## 9. Explicit Runtime Same-Document Proof

Freeze the smallest sufficient proof supported by the immutable model: a runtime document capability token plus applicability witnesses. This is explicit per operation, never an inference from ID equality and never a hidden imported-resource cache.

Conceptual companion/context facts:

```text
SourceTransferMetadata:
  optional runtime document token
  immutable witnesses: typed key -> original immutable target/resource value
  source reference textual exports: typed key -> String

TransferContext:
  destination Document
  optional destination runtime document token
  identity allocator
  source metadata supplied explicitly by caller
```

The editor/application owner creates one opaque token for one live semantic document editing lifetime, independent of screen/world/server and immutable snapshot instances. Two different documents receive different tokens even if their ASTs are equal. Reopening/replacing/importing an unrelated document creates a new token; no token is persisted or exported to another process. A shared screen may reuse a token only when it genuinely shares the same live document owner. Screen close loses proof if that live owner is discarded; application sidecar content may survive without proof.

Proof algorithm:

1. Source and destination tokens are present and the exact same capability, supplied by the trusted owner. Otherwise relation is CROSS_DOCUMENT_OR_UNKNOWN.
2. For each external target or reusable dataset, locate the current destination semantic value in the normal typed namespace.
3. Require the current value to be the same immutable object instance as its captured witness, not `equals`, equal IDs, labels, or schema. Retaining the original value across immutable list reconstruction is sufficient proof of unchanged identity. A caller must not alias one token across unrelated documents.
4. If a value was rebuilt/edited/replaced and continuity cannot be proven by this rule, that particular target/resource is unproven. Degrade the external reference; materialize the available dataset snapshot without reuse, or reject if the required dataset is absent from closure.

This intentionally conservative rule proves unchanged identities without maintaining an incarnation ledger, origin field, semantic cache, or a second resolver. It may decline valid continuity after an edit; correctness is preferable to guessed binding. It does not claim every same-document reference/resource survives arbitrary edits. Normal unchanged same-document view Copy/Paste reuses the live dataset, and repeated paste retains that proof if the original resource instance survives.

Witnesses are immutable semantic values only, not pointers to the source Document/session/screen. Fragment remains independent of them. They live explicitly in carrier/context and do not enter Document, EditorState semantic snapshots, or persistence. Core direct callers may omit proof and receive conservative cross-document semantics. Proof/unknown differences are explicit input facts, not optional cache-hit behavior.

Already-unresolved source references have no valid original-target witness and degrade to missing-reference Text. An unrelated object later occupying the same textual ID cannot supply proof. Undo may restore an exact original immutable target value; with the same live document token its witness is again applicable. Redo does not rerun transfer.

No stronger per-identity continuity tracker is required in M25B. Supporting continuity after semantic target edits is deferred rather than improvised through equality or hidden provenance.

## 10. Identity Namespace And Allocation Contract

All source identities remain unchanged during extraction.

| Identity/model | Scope | Destination / collision rule | Same-document / cross-document |
|---|---|---|---|
| Heading optional ID | Document-global SECTION | Preserve unused ID; remap occupied ID | Duplicated whole content normally collides; partial inline carries no ID. |
| Equation optional ID | Document-global EQUATION | Preserve unused; remap occupied | Same content policy in either relation. |
| Table optional ID | Document-global TABLE | Preserve unused; remap occupied | Same content policy; binding handled separately. |
| Figure required ID | Document-global FIGURE | Preserve unused; remap occupied | Outer owner only; caption references follow shared map. |
| ScientificDataset ID | Document-global DATASET | Dependency: proven reuse, otherwise preserve unused/add or remap occupied/add | No cross-document reuse, even same ID/value or previous import. |
| DatasetColumn ID | Dataset-local | Preserve within complete resource | Column ID changes not required for whole-resource copy. |
| DatasetRow optional ID | Dataset-local metadata | Preserve exact/order; no new global uniqueness rule | No current binding targets rows. |
| Plot/series/point identity | Absent | No allocation | Names/indexes are not stable IDs. |
| DiagramBlock/Definition global ID | Absent | No allocation | Separate diagrams can repeat local IDs. |
| DiagramNode/component/junction IDs | Diagram-local element namespace | Preserve inside complete owner; validate locally | No document-global remap. |
| Port/terminal ID | Element-local | Preserve authored definition/catalog IDs | Endpoints remain element+port pairs. |
| DiagramConnection ID | Absent | Preserve endpoints/label; no invented edge ID | No subgraph clipboard/merge. |
| Mechanical primitive/dimension/constraint/symbol/annotation/part-reference element IDs | Diagram-local | Preserve own ID and internal target references | Missing local targets may remain source warnings; invalid structural graph rejects. |
| Component designator/net label/item number | Authored labels/local semantics | Preserve | Not global allocatable keys. |
| Math IDs/TOC IDs | Absent | No allocation | MathPath, numbering, block indices are not identities. |

`StableIdentityKey(kind, id)` is sufficient for SECTION/EQUATION/TABLE/FIGURE/DATASET only. Local identity pairs stay owner-scoped, outside this map. CrossReference kind maps explicitly to SECTION/EQUATION/TABLE/FIGURE; never infer from string spelling.

IdentityRemap contains explicit source/destination typed decisions, including unchanged keys, with PRESERVED, REUSED_EXISTING, or REMAPPED_NEW. External reference degradation is a diagnostic, not an ID decision. Absent optional IDs remain absent.

Allocate deterministically per namespace against the surviving destination plus all plan reservations. To preserve current replacement semantics, an approved replaced root's identity is removed from the occupied set only when structural planning really removes it. Reserve all unused source identities that will be preserved before allocating suffixes, so allocating `h-2` for colliding `h` cannot steal another root's preserved `h-2`. Use current `base`, `base-2`, `base-3`, ... first-free convention in deterministic content/resource traversal order. No fragment paste counter or random/UI/persistence seed.

If two provided source roots claim one typed identity, reject ambiguous remapping. Same spelling across different kinds is permitted. No content-equality deduplication. Explicit dataset-resource Copy/Paste is resource import/duplication, so same-document resource-primary paste adds/remaps rather than eliding the user's import. View dependencies alone qualify for proven reuse.

## 11. Resource Reuse And Repeated Paste Matrix

| Case | Result |
|---|---|
| View dependency; same live document and unchanged dataset witness | REUSED_EXISTING, no addition; binding stays on live dataset. |
| Same document but dataset witness no longer applies; closure contains snapshot | Materialize snapshot with fresh ID on collision; report proof unavailable. |
| Cross-document; source ID unused | Add whole dataset under source ID. |
| Cross-document; same ID plus equal value | Allocate new ID, add, rewrite consumers. |
| Cross-document; same ID plus different value | Allocate new ID, add, rewrite consumers. |
| Same value under different ID | No deduplication; add under source ID if free. |
| Repeated cross-document paste after import | New materialization; collision means fresh resource ID even if prior import is unchanged. |
| Required resource absent and no applicable same-document witness | Reject, whether destination ID is occupied or absent. |
| Direct dataset import in same document | Add/remap copied dataset; not dependency reuse. |

Each materialization is independent. Within one materialization, D -> D2 is one decision and every consumer T/P/Figure Plot uses D2; never D2/D3 per consumer. Whole-resource cost is accepted until performance work, not traded for hidden aliasing.

## 12. Reference And Dataset Binding Algorithms

For every CrossReference in Paragraph/Heading, every authored/stored table cell, and Figure caption:

```text
key := explicit namespace mapping(reference.kind, reference.targetId)
if fragment provides key as a whole transferred target:
  insert CrossReference(kind, identityRemap.destination(key).id)
else if same-document context and original target witness applies:
  insert original CrossReference
else:
  insert unmarked Text(source textual export for key, or "[Missing reference]")
  emit EXTERNAL_REFERENCE_DEGRADED with original kind/id and fragment location
```

All references to one internal target use the same decision. Internal target inclusion comes from the fragment index verified against actual whole content, not label text or resources. No target is pulled into closure by a reference. No sentinel IDs, shadow resolver, heuristics, or persistent external state. Original external target identity remains in diagnostics only after degradation.

Source text comes from existing CrossReferenceResolver at extraction, frozen in the companion. Missing source display uses deterministic `[Missing reference]` with a diagnostic. Text replacement intentionally loses live-reference semantics and source target identity in the resulting Document. It cannot resolve to an unrelated destination object. Existing CrossReferenceResolver remains unchanged and derives labels for the remaining active references.

For every DatasetTableBinding and every series DatasetPlotBinding, including Figure-owned plots:

```text
if applicable same-document source dataset proof permits dependency reuse:
  binding.datasetId := proven current dataset ID
else if whole source dataset is in fragment closure:
  binding.datasetId := shared DATASET identityRemap.destination(source dataset).id
else:
  reject MISSING_REQUIRED_RESOURCE before mutation
preserve local column IDs and ordered table column selection
```

This ordering implements the accepted rule that a carried snapshot may reuse the proven live resource. Every required binding must have one of these outcomes. Missing columns in an available complete resource retain their original local binding and existing source warnings; no substitute columns/values. No dataset-view flattening. Missing source dataset diagnostics may permit Copy, not a broken destination Paste.

## 13. Structural Insertion Shapes And Selection

The editor prepares receiving shape independently of source identity/materialization. It reuses M24D structural contracts; transfer is not a second document editor.

| Fragment/target | Receiving shape and final selection |
|---|---|
| One inline segment, document TextSelection/caret | Replace normalized range with exact InlineContent, left-block ownership; caret after inserted content; no source Heading style/ID imported. |
| Several inline segments, document TextSelection/caret | Preserve selected source boundaries with the algorithm below; caret after last inserted segment, before surviving suffix. |
| Whole block(s), document text caret/range | M24D prefix/suffix preservation and block splice, atomic barriers/minimal-document fallback intact. Final selection targets last inserted root: BlockSelection if atomic/selectable, otherwise legal caret at its inline end. One-root behavior remains existing selection semantics. |
| Matching atomic replacement | Retain current Table/Plot/Diagram/Figure same-family replacement behavior; candidate identities exclude only removed owner. |
| Heading/Equation/TOC payload from BlockSelection | Existing structural insertion after selected block; no new cross-family replacement. |
| ResourcePrimary, permitted dataset action context | Resource additions only; retain still-valid previous editor selection. |
| Math local payload in equation editor | Existing MathExpressionEditor pasteFragment and resulting sequence caret after fragment. |
| Cell textual paste | Existing single-cell text insertion/selection rules; no owner construction. |
| Caption/plot/diagram local clipboard | Current unsupported guards remain; no new nested clipboard mode. |

InlineSegments is inline/range transfer, not whole BlockNode extraction. For n > 1, stage one range edit:

1. Keep destination blocks before normalized start and after normalized end.
2. Slice the destination start prefix and end suffix using existing inline boundary rules. A caret is a zero-length range in one editable block.
3. Rebuild the first destination block from prefix + first transferred segment, retaining the left destination block style/ID.
4. Emit n-1 new ID-less Paragraph blocks for subsequent segments; the final one contains last segment + destination suffix. Intermediate empty segments are real empty paragraphs.
5. Place the caret at the end of last transferred segment before suffix; preserve exactly n-1 selected boundaries. Source Heading levels/IDs never travel in this shape.

This is the minimal structured range receiving shape consistent with destination left ownership and partial Heading rules. It is not replayed Enter commands: Enter's empty-heading conversion would otherwise discard copied boundaries. M25B implements it as a pure structural helper under DocumentEditor, using existing slicing/reconstruction/minimal-document rules and one history application. External plain-text linebreak normalization remains unchanged; this does not implement structural multiline external paste.

Boundary-only extraction is n=2 empty segments; it copies the boundary without adjacent characters. A one-segment empty insertion at a caret is a no-op. No source IDs are allocated for inline segments; references to the source heading remain external unless a whole heading target travels separately in an approved fragment shape.

For ordered whole roots, splice as one collection rather than repeatedly calling user actions. No intermediate fallback paragraphs/transactions; append authoring fallback only for the complete resulting shape as M24D requires. Existing document editor validity and scope remain authoritative. No new UI mixed-object selection or new caption/grid/subgraph ranges.

## 14. Staged Materialization And Atomic Application

1. Recognize semantic payload and check destination scope/approved conversion before text import.
2. Capture current destination state and explicit runtime proof facts; stage structural receiving shape and identify surviving identities.
3. Validate source fragment/indexes/composites and required closure or proven reuse. Reject incomplete semantics.
4. Reserve identities, allocate collision-free namespace-aware decisions, and freeze IdentityRemap.
5. Materialize resource additions once per source key; retain local columns/rows/value structure.
6. Materialize whole roots or inline segments, apply shared dataset bindings and reference rules across owned content.
7. Return immutable materialized content, resource additions, remap, and diagnostics without editor mutation.
8. Editor composes one prospective Document from prepared roots/range content/resources and surviving destination content.
9. DocumentValidator must report zero ERROR; validate deterministic destination EditorSelection using EditorSelectionValidator.
10. Confirm the application still targets the captured destination state. If stale, reject or replan completely from current state; never apply a stale remap.
11. Commit once through M24E history if semantic state changed. Derive layout/views afterward without additional history.

Resource and reference materialization can share a typed traversal; their dependency order is fixed: identities first, rewrites second. Ordinary warnings/degradation are results, not exceptions. Allocator reservations remain private to the plan, with no committed counters/IDs on failure. No destination root/resource/selection/history mutation is observable before the validated candidate is committed.

Failure leaves Document/resources/history/selection unchanged and coherent. No half-Figure, orphan dataset, partial diagram rewrite, automatic unrelated repair, or partial transfer of successful roots. Clipboard text already exported by Copy/Cut is external state and is not rolled back by document Undo.

## 15. Composite Materialization Contract

| Composite | What travels / remaps | What derives | Degradation and rejection / nested scope |
|---|---|---|---|
| Figure | Wrapper ID, caption InlineContent, full supported Plot/Diagram; outer FIGURE remap plus bindings/references | Figure number, caption prefix, visual layout | External caption refs -> Text; missing required Plot dataset rejects; no implicit unwrap/caption-only copy. |
| Authored Table | Rows/columns/header count/cells/marks/inline nodes; optional TABLE ID, inline references | Header presentation and layout | Cell refs follow normal policy; invalid shape rejects; local clipboard remains single-cell text. |
| Bound Table | Canonical stored fields plus binding and whole dataset closure; table/dataset IDs | Visible current rows/headers via DatasetTableResolver | No resource/proven reuse rejects; missing source column may remain warning. No authored snapshot conversion. |
| Plot | All definition/axis/settings/height/ordered series/authored points; dataset bindings as modeled | Bound points, ticks/ranges/layout | Required dataset absence rejects; missing source column warning allowed; no series/point clipboard root or new Plot ID. |
| Diagram | Complete title/canvas/elements/connections/bounds/labels/orientations/workspace ratio; preserve local IDs | Port positions, wire routes, nets, measurement/leader display | Structurally invalid graph rejects. Existing missing mechanical target warnings may remain; no graph merge/subgraph clipboard. |
| Equation | Exact MathExpression tree and optional EQUATION ID | Math layout/glyphs | No serialize/reparse/normalization; nested MathSequence stays distinct and insertion requires supported local target. |
| Inline | Exact runs/marks/reference atoms/boundaries; no source block identities | Active reference labels | Unproven external refs -> Text; invalid source/destination range rejects. |
| TOC | Marker only, no ID/headings/entries | Destination sections/outline/TOC entries | Multiple markers allowed; no copied numbering or heading closure. |
| Dataset | Complete ID/displayName/schema/rows/value variants; resource ID decision; local IDs preserved | Consumer views | No content dedupe/slicing; malformed resource rejects; explicit import distinct from dependency reuse. |

Electrical component/junction semantic kinds, designators, labels, orientations, and terminal identities travel through owner; catalog terminal geometry and inferred nets do not. Mechanical dimensions own bounds/kind, not an assumed primitive link. Constraints own subject/peer local IDs; annotations own bounds/kind/text without inferred attachment; part references own target/item/name/quantity/description. All remain inside whole Diagram, including Figure-contained Diagram. M25B adds no graph repair policy.

## 16. Copy, Cut, Paste, And Clipboard Interoperability

Copy: extract immutable semantic content/companion and readable fallback; write OS text, then install rich carrier only on successful export. Zero Document/selection/history mutations; no destination remap, no redo invalidation. Fallback rendering failure fails the export cleanly; no clipboard installation based on a partial representation.

Cut: check extractability and deletion eligibility; prepare the same export as Copy and a valid source deletion using M24D. Successfully export OS fallback/install sidecar before committing one source deletion transaction. Clipboard failure means no deletion. Paste later is separate; resource dependencies are not deleted with views. No resource Cut command or move tracking is added.

Paste dispatch contract:

```text
matching recognized semantic carrier exists:
  if compatible semantic target: plan/validate/apply semantic insertion
  else if explicitly supported textual conversion: use that defined conversion
  else: reject UNSUPPORTED_DESTINATION
  any semantic validation/closure failure: reject, do not retry as text
no recognized semantic carrier (missing/stale sidecar):
  use existing external plain-text insertion/import if supported
```

The sole retained automatic textual destination conversion is existing table-cell plain-text paste: it receives the carrier's OS text under the established cell contract, normalizes linebreaks, and does not interpret rich blocks/math/resources. This is a deliberate cell-scope textual action, not recovery from a failed semantic paste. Other incompatible rich destinations, including native math at document body, reject unless a future explicit conversion action is approved. No new Copy As action is added.

Plain-text-only external clipboard preserves existing prose/cell normalization and external MathPlainTextImporter behavior, including linear slash interpretation. Plot/Diagram/Figure summaries/TSV are not inferred as scientific blocks. Recognized-but-invalid payloads never evade rejection via text import. Rich structure remains process-local; restart/cross-process retains only OS text. Exact OS-text equality is transport matching only, with the existing identical-external-recopy ambiguity; it proves no source identity or resource reuse.

| Fallback | M25B obligation and scope |
|---|---|
| Inline/prose | Source resolved text and selected boundaries as LF; marks/link semantics not encoded. |
| Whole Heading/TOC | Existing source display export; numbering is fallback only. |
| Whole Equation | Preserve existing empty document-serializer fallback in this migration; richer whole-equation export deferred. Native block remains exact. |
| Local math | Existing readable MathPlainTextSerializer, not LaTeX/canonical round-trip. |
| Whole Table | Text-compatible authored TSV remains canonical tabs/LF. For bound-table display and authored cell references, generate source fallback through existing DatasetTableResolver/CrossReferenceResolver. Rich table stays canonical. Preserve existing TSV cell tab/newline rejection rather than invent an escaping schema. |
| Plot/Diagram/Figure | Preserve existing readable summaries/caption display serializers; not resource/file interchange. |
| Dataset | Existing TSV serializer, no native schema/identity encoding. |

Source reference textual metadata is immutable export text kept in carrier/context, not rendered state in the fragment. No new MIME transport, encoded hidden metadata, Java serialization, or Markdown clipboard language. Markdown remains independent partial interchange.

## 17. Degradation Versus Rejection

| Condition | Outcome | Document/history effect |
|---|---|---|
| Unproven external CrossReference, including same textual target collision | Accepted degradation to source-display Text; original key diagnostic | Part of one successful paste transaction. |
| Internal transferred reference | Active remapped reference | Exact transfer semantics; normal resolver derives label. |
| Proven same-document external reference | Preserve active original reference | No target copying. |
| Required dataset absent from closure and no applicability proof | MISSING_REQUIRED_RESOURCE rejection | No change, even if destination lacks that ID. |
| Missing source column in carried/reused available dataset | Preserve source degraded binding; warning | Allowed if prospective Document has zero ERROR; no repair. |
| Structurally invalid Diagram/composite | INVALID_COMPOSITE_GRAPH rejection | No change. |
| Duplicate ambiguous source identities, malformed index/resource entries | MALFORMED_FRAGMENT / INVALID_RESOURCE_CLOSURE rejection | No change. |
| Allocator cannot provide collision-free mapping | IDENTITY_REMAP_FAILURE rejection | No change or leaked committed reservations. |
| Recognized semantic payload unsupported at target | UNSUPPORTED_DESTINATION rejection, except defined cell textual conversion | No silent flattening. |
| Unsupported source selection/cross-slot/nonexistent partial mode | UNSUPPORTED_SOURCE_SELECTION rejection/disabled action | No export/delete. |
| Prospective Document or selection invalid / stale destination | INSERTION_VALIDATION_FAILURE or STALE_DESTINATION rejection | No change. |
| No matching semantic payload; supported external text | Existing plain-text/import insertion | Its existing transaction/selection rules. |
| No semantic change | No-op | Zero transactions; no redo invalidation. |

## 18. Validation, Diagnostics, And Correctness

| Stage | Required checks / owner |
|---|---|
| A Extraction | Source selection/domain boundaries; owned semantic shape; source warnings collected; editor adapter + extractor. |
| B Fragment | Closed content family; nonempty actionable shape; precise segments; identity index consistency; no ambiguous keys; resource uniqueness/schema/required dependency accounting. |
| C Plan | Scope compatibility; explicit proof applicability; complete available resource or proven reuse per binding; collision-free reservations; total identity decisions and coherent rewrites. |
| D Prospective Document | Existing DocumentValidator: zero ERROR. Source-allowed missing columns/mechanical targets or unrelated existing warnings may remain; never auto-repair them. |
| E Selection | Existing EditorSelectionValidator accepts staged final selection before commit. |

A FragmentValidator helper may be implemented internally, but is not a new general validation/repair/resolution subsystem. Ordinary degraded/rejected states use structured results. Exceptions remain for programmer errors/impossible invariants, not an absent resource or incompatible scope.

TransferResult conceptually carries success/rejection status, immutable materialized content/resource additions, IdentityRemap, and diagnostics. Rejection contains no applicable partial insertion. Diagnostic fields: code/severity/message, source typed identity when applicable, fragment-relative semantic location, and operation outcome. Original external reference identity can be exposed to immediate UI/reporting but does not become persistent repair state. No new diagnostics UI required.

Required correctness properties: no accidental reference/dataset capture; no duplicate identities or shared resource entries; locally valid complete graph; no half-applied paste; source/fragment immutable; no UI/history/cache/persistence objects in fragment; no implicit Figure unwrap; no stored TOC numbering; no Copy history; deterministic redo. Existing resolvers alone derive destination labels/views/nets/layout.

## 19. History, Actions, And Foundation Integration

| Operation | Semantic history transactions |
|---|---:|
| Copy / proof/export metadata update | 0 |
| Successful Cut source deletion | 1 |
| Successful changed Paste: resources + roots + references | 1 |
| Failed/rejected/no-op Paste/Cut | 0 |
| Derived resolver/layout recomputation | 0 additional |

M24E stores final semantic Document, valid selection, and appropriate typing-mark state. Transfer is a non-typing edit that closes coalescing only as the existing apply-edit contract requires. Redo restores identical allocated IDs/resource values/selection; it never calls DocumentTransfer, allocators, or clipboard again. Clipboard companion and runtime tokens are outside semantic history.

M24B selection domains/atomic barriers and M24D left ownership, structural placement, heading identity split rules, two-step atomic deletion, and empty-document authoring fallback remain. M24C menu/context/toolbar/shortcut paths share existing Copy/Cut/Paste EditorActions and enablement. Client chooses context; pure transfer decides semantics; editor chooses receiving shape and transaction. M24F authoritative focus dispatch remains unchanged. No UI-specific remapper or parallel mutation path.

## 20. M25B Implementation Map

Names below denote responsibilities, not a required proliferation of public classes.

| Component / likely package | Inputs -> outputs | Prohibited responsibilities |
|---|---|---|
| Fragment value/index, `transfer` | Source semantic values -> immutable DocumentFragment | Destination IDs, UI, persistence. |
| Extractor/typed closure, `transfer` with editor selection adapter | Source values/range description -> fragment/resources/source exports/diagnostics | Destination mutation or collisions; dependency branches in client/session. |
| Runtime proof companion/context, `transfer` plus live-document owner integration | Explicit tokens/immutable witnesses/current destination -> applicability facts | Hidden imported-resource cache, equality/provenance heuristics, Document fields. |
| Identity allocator/remap planner, `transfer` | Surviving destination + fragment provided keys -> reservations/typed decisions | Allocation during Copy/Redo or per-family session remappers. |
| DocumentTransfer materializer, `transfer` | Fragment + context -> content/additions/remap/diagnostics or rejection | Editor/history/clipboard/layout dependency. |
| Structural range/block staging, `editor/DocumentEditor` | Materialized values + current selection -> one candidate Document/selection | Resource policy, alternate resolver, helper history entries. |
| Application coordinator, `editor/EditorSession` | Validated staged EditResult -> one history application | Own resource discovery/remap/reference policy. |
| Fragment carrier/sidecar, `clipboard` | ExtractionResult + OS text -> recognized rich transport | File format, source-identity inference from text match. |
| Existing nested math/cell adapters | Local supported payload + current nested target -> existing scoped edit | Wrap into owning block or add new nested clipboard mode. |
| Fallback exporters | Canonical source plus existing resolvers -> OS String | Change semantic AST/IDs or embed resources/schema. |

Migration: whole-block Table/Plot/Diagram/Figure/DocumentBlock carriers become a fragment carrier or temporary adapters to it; InlineContent carrier becomes InlineSegments; Dataset carrier becomes ResourcePrimary. MathClipboardPayload stays local. Cell String and external math importer stay local. No duplicate permanent policy in legacy adapters.

## 21. M25B Dependency-Aware Implementation Order

1. Internal immutable fragment/content/index/diagnostic result contracts and core invariant tests.
2. Explicit runtime token/witness companion and conservative proof tests; no model/persistence changes.
3. Exact block/inline extraction and typed dataset closure, including source textual exports.
4. Namespace allocator/reservation/remap plan with collision and repeated-materialization tests.
5. Shared resource materialization, internal reference rewrite, external Text degradation, and binding rejection/rewrite.
6. Pure editor structural staging for ordered roots and InlineSegments; prospective Document/selection validation.
7. Atomic apply integration, no-op/failure/Undo/Redo regressions.
8. Clipboard carrier/fallback adaptation and shared action/enablement integration; retire old transfer policy branches.
9. Preserve nested math/cell boundaries and prove rejection of incompatible rich payloads without fallback bypass.
10. Complete cross-system/negative regression matrix and manual Minecraft QA, then close implementation.

Tests accompany each stage; this sequence is not authorization to implement. M25B must use the frozen policies below and report an uncovered case as an architecture gap rather than improvising.

## 22. M25B Test Obligations

| Family | Required cases / invariants |
|---|---|
| Inline | Plain/Bold/Italic/both/mixed marks, exact segmentation, Unicode/code-point slices, reference atomicity, empty content. |
| Partial prose | Heading partial yields inline with no heading ID/level; several segments preserve boundaries; boundary-only/empty interior blocks; destination left ownership; caret before suffix; no atomic crossing. |
| References | Target + references transferred together; multiple references share remap; same-document unchanged witness; unknown/wrong token; unrelated same-ID/equal-value target; changed witness; source missing target; source-export absent; captions/table cells; deterministic Text diagnostics. |
| Roots | Whole Paragraph, Heading, Equation, authored Table, authored/bound Plot, generic/electrical/mechanical Diagram, Figure Plot/Diagram, TOC; ordered supplied collections without new UI selection. |
| Tables | Header/shape/marks/programmatic reference cells; native canonical versus resolved fallback; current local single-cell textual conversion; no grid/import inference. |
| Datasets | One bound view closure; T+P and Figure Plot+T sharing D once; several datasets/series; whole schema/row/value/local-ID preservation; same-document unchanged reuse; unknown/edited proof snapshot materialization; direct import duplication. |
| Resource negatives | Missing dataset rejects even when destination ID absent; missing colliding dataset does not bind; malformed closure/extra duplicate entry; missing-column source warning retained; no heuristic columns or flattening. |
| Identity | Each global namespace, same spelling across kinds, preserve unused/remap collision, reservation against another source's suffix, ambiguous source duplicate rejection, replacement survivor handling, repeated cross-document fresh datasets despite equal value, deterministic allocator. |
| Diagram | Exact complete electrical endpoints/junctions/local IDs and workspace ratio; mechanical constraints/part targets/annotations/dimensions; owner-local repeats across blocks; derived nets not stored; invalid graph rejects; no partial clipboard. |
| Math | Exact local native fraction/root/script/group/token boundaries; internal token split/paste; extractor versus delete applicability; unsupported cross-slot; native math-to-body rejection; no EquationBlock inference; external text behavior unchanged. |
| Figure/TOC | Wrapper retained; contained bindings remap; external caption refs Text; no unwrap; TOC marker derives destination headings; multiple TOCs valid. |
| Copy/Cut/Paste | Copy zero history/redo preserved; clipboard/export failure no delete; Cut one transaction; Paste one including resources/rewrites; rejected/no-op zero; destination/selection/resource state unchanged on failure. |
| History | Undo restores exact directional source selection/content; Redo identical IDs/datasets/result selection without allocation/clipboard/proof reevaluation; derived views recompute. |
| Actions/transport | Shortcut/menu/context parity; disabled unsupported scopes; rich recognized invalid payload never external-import bypass; stale/missing sidecar uses supported text; screen owner token lifecycle; separate documents equal AST still unknown. |
| Validation/architecture | DocumentValidator zero ERROR and valid EditorSelection after successful paste/Undo/Redo; no Minecraft/client/layout/editor imports in transfer; no new persistent origin/cache state. |

Manual Minecraft QA is required for M25B: marked and multi-block prose, boundary-only ranges, native stacked math, authored/bound views and sharing, Figures/captions, diagram topology/workspace, TOC destination derivation, rich-target rejection, clipboard failures where simulated, menus/shortcuts, and Undo/Redo. Client startup is not manual acceptance. M25A itself changes documentation only and requires no new visual QA claim.

## 23. M25B Must Not Invent And Architectural Gates

- No new ID namespaces or global diagram/column/row identities without accepted model work.
- No sentinel unresolved IDs, semantic external-binding field, heuristic reference matching, or second resolver.
- No same-document detection from IDs/AST equality/text; no hidden import/provenance cache or persisted origins.
- No dataset deduplication by equal values/hash/name/schema; no prior-import reuse across documents.
- No missing-resource degraded paste, substitute dataset, column slicing, or automatic bound-view snapshot.
- No Figure unwrap/new containment families, partial diagram graph policy, multi-cell/series/caption clipboard, new selection modes, or move protocol.
- No silent semantic-to-text retry after recognized-payload rejection; only the defined current cell textual conversion.
- No resource insertion/remap outside the paste transaction or ID generation on Redo.
- No source labels/TOC snapshots as canonical semantic fragment state; no clipboard format as M26 native schema.
- No per-type resource/remapper branch added independently to EditorSession when typed transfer owns it.

Before M25B acceptance: implementation must satisfy Section 22 and manual QA. Before native persistence or public API freeze: separate schema/resource compatibility/API design remains required. This architecture freezes internal semantic behavior, not a public Java API, native file version, or universal transfer language.

## 24. Resolved Open Questions And Completion Checklist

Resolved: runtime proof uses explicit live-document token plus conservative immutable-value witnesses; partial prose has InlineSegments with deterministic receiving shape; dataset-only is ResourcePrimary; no cross-document imported-resource reuse; missing resources reject; source text metadata lives beside fragment; recognized invalid rich payload rejects; current cell textual conversion remains; table display fallback uses existing resolvers; whole-equation fallback expansion is deferred; diagnostic UI and edited-target continuity are future capabilities, not implementation blockers.

No unresolved architectural blocker remains for this bounded M25B scope. Unknown contexts and unproven continuity have explicit safe outcomes. Future richer continuity, diagnostics/repair UX, media/external resources, and native serialization require their own later contracts.

- [x] All M25A issue documents and relevant foundation/ADR sources audited.
- [x] Contradictions explicitly recorded and resolved by final accepted clarifications.
- [x] Terminology frozen.
- [x] Fragment shape frozen.
- [x] Extraction frozen.
- [x] Resource closure frozen.
- [x] Identity policy frozen.
- [x] Reference policy frozen.
- [x] Composite policy frozen.
- [x] Explicit same-document proof frozen.
- [x] Degradation/rejection frozen.
- [x] History integration frozen.
- [x] Selection integration frozen.
- [x] Validation integration frozen.
- [x] M25B implementation map/order complete.
- [x] M25B test obligations complete.
- [x] No unresolved blocker remains.

M25A architecture is COMPLETE. M25B is ready to begin when explicitly requested. No production implementation, test modification, commit, or push is performed by this consolidation.
