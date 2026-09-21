# M25A Identity And Reference Policy

Status: ROM-9 architecture design document. Not yet an accepted ADR. No production classes are implemented by this document.

Authority notice: corrected ROM-9 is accepted supporting rationale. [M25A Transfer Architecture Contract](M25A_TRANSFER_ARCHITECTURE_CONTRACT.md) governs M25B, resolves the deferred proof/insertion questions, and supersedes earlier permission for missing-resource degraded insertion. The accepted external CrossReference-to-Text policy is retained.

## 1. Executive Decision

Scholar transfer identity must be namespace-aware, destination-aware, and conservative.

Core decisions:

- `DocumentFragment` preserves source IDs exactly.
- `DocumentTransfer` owns all destination collision handling, ID allocation, ID remapping, binding rewrites, and CrossReference rewrites.
- Document-global copied content identities are remapped when duplication would collide.
- Same-document copied dataset-backed views reuse existing dataset resources.
- Cross-document dataset resources are not reused merely because the destination has the same textual ID or equal contents.
- Repeated cross-document paste materializes a fresh remapped dataset whenever the source dataset ID is already occupied and no explicit accepted provenance proves identity.
- References whose targets travel with the fragment are rewritten to the target's destination identity.
- References whose targets do not travel are never heuristically retargeted.
- Cross-document external references degrade to non-reference inline text unless their target travels with the fragment.
- Cross-document external references must not accidentally bind to unrelated destination targets.
- Diagram-local IDs remain local to the copied diagram value and are not document-transfer identities.
- Redo restores committed IDs from history snapshots and never reruns transfer allocation.

This policy solves current M25A transfer risks without introducing persistence provenance, generic graph identity, plugin IDs, or a second reference resolver.

## 2. Identity Namespace Inventory

| Identity | Model type | Field | Scope | Uniqueness rule | Validator / resolver | Referenced externally? | Transfer remap? |
|---|---|---|---|---|---|---:|---:|
| Heading ID | `Heading` | `id()` optional | document-global section namespace | unique among headings | `DocumentValidator.validateHeading`; `CrossReferenceResolver` SECTION targets | yes, `CrossReference(SECTION, id)` | yes for copied content |
| Equation ID | `EquationBlock` | `id()` optional | document-global equation namespace | unique among equations | `DocumentValidator.validateBlocks`; `CrossReferenceResolver` EQUATION targets | yes | yes for copied content |
| Table ID | `TableBlock` | `id()` optional | document-global table namespace | unique among tables | `DocumentValidator.validateTable`; `CrossReferenceResolver` TABLE targets | yes | yes for copied content |
| Figure ID | `FigureBlock` | `id()` required | document-global figure namespace | unique among figures | `DocumentValidator.validateFigure`; `CrossReferenceResolver` FIGURE targets | yes | yes for copied content |
| Dataset ID | `ScientificDataset` | `id()` required | document-global resource namespace | unique among document datasets | `Document` constructor; `DocumentValidator.validateDatasets`; `DatasetRegistry` | yes, dataset bindings | yes for resource materialization |
| Dataset column ID | `DatasetColumn` | `id()` required | dataset-local | unique inside one dataset | `ScientificDataset` constructor; `DocumentValidator.validateDatasets` | yes, dataset table/plot bindings with dataset ID | normally no; follows dataset |
| Dataset row ID | `DatasetRow` | `id()` optional | dataset-local value metadata | no current uniqueness validation | none found | no current binding | no |
| Plot identity | `PlotBlock` / `PlotDefinition` | none | none | n/a | n/a | no direct CrossReference target today | no |
| Diagram element ID | `DiagramElement` | `id()` | diagram-local | unique inside `DiagramDefinition` | `DiagramDefinition` constructor; `DocumentValidator.validateDiagram` | referenced by diagram connections and mechanical elements | no document-level remap |
| Diagram port/terminal ID | `DiagramPort` / electrical terminal definitions | `id()` | element-local | unique within an element's ports | `DocumentValidator.validateDuplicatePorts`; diagram constructors validate endpoints | referenced by `DiagramEndpoint` with element ID | no document-level remap |
| Diagram connection endpoints | `DiagramEndpoint` | `elementId`, `portId` | diagram-local | must resolve inside same diagram | `DiagramDefinition.requireEndpoint`; `DocumentValidator.validateEndpoint` | internal only | no document-level remap |
| Mechanical constraint target IDs | `MechanicalConstraint` | `subjectId`, `peerId` | diagram-local | may warn if missing | `DocumentValidator.validateMechanicalReferences` | internal only | no document-level remap |
| Mechanical part target ID | `MechanicalPartReference` | `targetId` | diagram-local | may warn if missing | `DocumentValidator.validateMechanicalReferences` | internal only | no document-level remap |
| Table of contents identity | `TableOfContentsBlock` | none | none | n/a | derived from headings | no | no |

Same textual stable ID may be reused across target kinds. Current tests explicitly validate that `Heading("shared")`, `TableBlock("shared")`, `EquationBlock("shared")`, and `FigureBlock("shared")` can coexist.

## 3. Identity Scope Classification

Document-global semantic identities:

- `SECTION` / heading ID;
- `EQUATION` / equation ID;
- `TABLE` / table ID;
- `FIGURE` / figure ID;
- `DATASET` / dataset ID.

Resource-local identities:

- dataset column IDs, scoped by dataset ID;
- dataset row IDs, currently optional metadata and not binding targets.

Composite-local identities:

- diagram element IDs;
- diagram port/terminal IDs;
- diagram connection endpoints;
- mechanical constraint and part-reference target IDs.

Transient/non-transfer identities:

- selection positions;
- layout source ranges;
- rendered geometry;
- UI focus, hover, menu, drag, and clipboard snapshot state;
- figure/table/equation/section display numbers.

Only document-global semantic identities participate in `DocumentTransfer` identity maps for M25A. Resource-local and composite-local IDs remain embedded in their owning semantic value unless a future feature gives them document-level meaning.

## 4. StableIdentityKey Model

The minimal identity key is:

```java
record StableIdentityKey(StableIdentityKind kind, String id) {}
```

Current `StableIdentityKind` values should conceptually include:

- `SECTION`
- `EQUATION`
- `TABLE`
- `FIGURE`
- `DATASET`

`kind + id` is enough for document-global transfer identities.

Parent scope is required only for local identities, and local identities should not be promoted into the primary document transfer map in M25A:

- dataset columns are identified conceptually as `(datasetId, columnId)` but normally do not remap independently;
- diagram elements are identified conceptually inside one `DiagramDefinition`, not the document;
- diagram ports are identified conceptually as `(diagram element, portId)`.

If a future operation needs to transfer partial dataset schemas or diagram subgraphs, it must define a local identity map in that subsystem rather than overloading the document-global identity map.

## 5. Source ID Preservation

`DocumentFragment` preserves source semantic IDs exactly.

Fragment construction must not:

- generate replacement IDs;
- inspect destination IDs;
- normalize IDs beyond constructors that already normalized the source model;
- rewrite CrossReferences;
- rewrite dataset bindings;
- rewrite diagram internals.

Why:

- one immutable fragment may be materialized into many destinations;
- destination collisions are not knowable at extraction time;
- source IDs are required to compute coherent remaps;
- reference rewriting depends on target remap decisions;
- redo must restore committed destination state, not mutate the source fragment.

Malformed duplicate source IDs:

- if the source model could not be constructed, there is no fragment;
- if the source document is constructable but validator-invalid, fragment construction may carry the content with diagnostics;
- transfer should not silently repair source duplicates before destination materialization;
- structurally impossible states remain validation errors, separate from transfer.

## 6. Destination Collision Policy

Document-global content IDs:

- Destination does not contain source ID: preserve source ID.
- Destination contains same ID because this is the same source target in the same document and the operation is not duplicating that target as content: reuse only where explicitly allowed, such as dataset resource reuse.
- Destination contains same ID for copied content being duplicated: allocate fresh destination ID.
- Destination contains same ID but unrelated content/resource: allocate fresh destination ID.
- Destination contains equivalent content under another ID: do not dedupe by content; allocate/preserve based on ID collision only.

Dataset resources:

- Same-document paste of views referencing an existing dataset: reuse existing dataset ID.
- Cross-document paste where destination lacks dataset ID: add resource with source ID.
- Cross-document paste where destination already has dataset ID: allocate fresh dataset ID unless explicit accepted provenance proves the destination resource is the same source identity.
- Cross-document same ID plus equal value is not sufficient proof.
- Same value under different ID is not deduped.

Diagram-local IDs:

- Whole-diagram transfer preserves local IDs inside the copied diagram.
- Document-level transfer does not check diagram element/port IDs against other diagrams.

## 7. ID Allocation Policy

Destination ID allocation must be:

- namespace-aware;
- deterministic for a given destination state and allocator policy;
- collision-free within the relevant namespace;
- independent of UI/client/rendering;
- independent of native persistence;
- testable without Minecraft;
- recorded in semantic output before the editor commits history.

`DocumentTransfer` should receive or own an `IdentityAllocator` through `TransferContext`.

The allocator may initially use the current suffixing convention (`id`, `id-2`, `id-3`, ...), but that convention should move out of `EditorSession`.

Redo must not call the allocator. Redo restores the previously committed `EditorState`.

## 8. IdentityRemap Semantics

`IdentityRemap` should record decisions, not only string substitutions.

Conceptual model:

```java
record IdentityRemap(List<IdentityDecision> decisions) {}

record IdentityDecision(
        StableIdentityKey source,
        StableIdentityKey destination,
        IdentityDisposition disposition
) {}

enum IdentityDisposition {
    PRESERVED,
    REUSED_EXISTING,
    REMAPPED_NEW
}
```

Unchanged IDs should appear explicitly when they matter for rewrite logic.

Examples:

- copied heading `SECTION:motion` inserted into empty destination: `PRESERVED`.
- copied heading `SECTION:motion` pasted into document already containing `motion`: `REMAPPED_NEW` to `motion-2`.
- same-document dataset-backed table referencing dataset `DATASET:projectile`: `REUSED_EXISTING`.
- external CrossReference degraded to text to avoid collision: no identity decision is emitted because no live reference remains.

The final implementation may store maps internally for lookup, but diagnostics and tests should be able to distinguish preserved, reused, remapped, and degraded decisions.

## 9. Same-Document Content Identity Behavior

When document content is duplicated in the same document, copied document-global content IDs must be fresh if preserving them would create duplicates:

- Heading ID remaps.
- Equation ID remaps.
- Table ID remaps.
- Figure ID remaps.

Reason:

- duplicated semantic objects are new document content;
- sharing a content ID would make CrossReferences ambiguous and violates validation;
- current tests already expect suffix remaps for headings, tables, and figures.

If a block lacks an optional ID, transfer does not need to create one.

## 10. Same-Document Resource Identity Behavior

Same-document resource handling differs from copied content identity.

Dataset-backed views duplicated inside the same document should keep referencing the same existing dataset resource:

- the source dataset already belongs to the destination document;
- the view is a live view over document-owned data;
- duplicating the view should not duplicate the dataset;
- editing the dataset should update all views.

This is the content/resource distinction from ROM-8:

- copied content identity duplicates;
- referenced resource identity reuses when the resource is already in the same destination document.

## 11. Repeated Paste - Content

Repeated paste of the same source fragment into the same destination must produce fresh content IDs as the destination changes.

Example:

- source fragment contains `Heading("motion")`;
- paste 1 into destination already containing `motion`: pasted heading becomes `motion-2`;
- paste 2 of the same immutable fragment into the updated destination: pasted heading becomes `motion-3`;
- paste 3 becomes `motion-4`.

The fragment always remains `motion`.

No paste count belongs in the fragment. The destination state and allocator determine each materialization.

## 12. Repeated Paste - Resources

ROM-9 chooses the minimal safe resource rule:

Without explicit accepted provenance, repeated cross-document paste does not automatically reuse a previously materialized dataset.

Compare options:

- Always duplicate resource on each paste: safe, possibly large.
- Reuse by destination textual ID: unsafe; same ID can be unrelated.
- Reuse by content equality: unsafe; equal content can be accidental and future metadata may differ.
- Reuse through explicit provenance: potentially good, but not accepted and would affect persistence/API.
- Reuse only when destination identity is provably the same source identity: recommended, but current proof exists only for same-document transfer.

Therefore:

- Same-document repeated paste reuses existing source dataset.
- Cross-document paste into destination with unused dataset ID may add the dataset with source ID.
- Repeating that cross-document paste sees the ID now occupied and must allocate a fresh dataset ID unless a future accepted provenance mechanism proves identity.
- If destination dataset was edited after prior import, that also forces a fresh remapped dataset.

This may duplicate large datasets. That cost is preferable to hidden semantic aliasing in M25A.

## 13. Provenance Decision

M25A should not add persistent provenance metadata to `Document`.

Rejected for M25A:

- process-local provenance cache: would make document semantics depend on hidden ephemeral state;
- persistent "importedFrom" fields: would leak future persistence/source-document identity decisions into the document model before M26;
- content-hash provenance: would become content-equality dedupe by another name.

Accepted:

- `TransferContext` may indicate whether source and destination are the same document identity for the current operation.
- `DocumentFragment` may contain source `StableIdentityKey`s, not live source document pointers.
- Cross-document resource reuse requires an explicit future provenance policy before it can happen safely.

Until then, cross-document same-ID collision means remap, not reuse.

## 14. CrossReference Semantics

For each transferred `CrossReference(kind, targetId)`, transfer maps `kind + targetId` to a `StableIdentityKey`.

Case A: target is inside transferred semantic closure.

- The target receives a destination identity.
- The CrossReference rewrites to that destination identity.
- Multiple references to the same copied target rewrite consistently.

Case B: target is outside transferred closure and transfer is same-document with the source target still in the destination.

- The reference may keep its raw target ID.
- Normal `CrossReferenceResolver` will resolve it to the existing target.

Case C: target is outside transferred closure and transfer is cross-document or cannot prove the target is the same source target.

- Transfer must not guess or retarget.
- Transfer must avoid accidental rebinding to unrelated destination content.
- The reference is intentionally degraded to non-reference inline `Text`.
- The inserted text should use the source document's resolved display text when available, or the current missing-reference display text when the source reference was already unresolved.
- The original source target identity is preserved only in transfer diagnostics, not in the inserted semantic document content.

Case D: target ID collides with unrelated destination object.

- Transfer must not leave the raw ID if that would resolve to the unrelated object.
- It must degrade the reference to text and emit a diagnostic.

No heuristic retargeting is allowed.

## 15. Accidental Rebinding Analysis And Solution

Problem:

Source contains:

```text
Paragraph(CrossReference(FIGURE, "fig-1"))
```

The target figure is not copied. Destination already contains an unrelated `FigureBlock("fig-1")`.

If transfer preserves the raw target ID, current `CrossReferenceResolver` resolves the pasted reference to the unrelated destination figure. That silently changes meaning and is unacceptable.

Current model limitation:

- `CrossReference` stores only `kind` and `targetId`.
- `InlineContent` can store `CrossReference` or `Text`.
- `CrossReferenceResolver` resolves by scanning current document targets for matching `kind + targetId`.
- Validation reports unresolved references as warnings.
- There is no current representation for "external unresolved reference that remembers original source target identity without participating in normal resolution."

Therefore M25 cannot preserve an active external reference across documents without either risking accidental rebinding or changing the semantic model.

Options evaluated:

| Option | Result |
|---|---|
| A. Synthetic guaranteed-unresolved target ID | Rejected. It avoids rebinding, but mutates the target ID into a sentinel convention, pollutes the semantic ID space, weakens repair by losing the clean source ID in content, and risks accidental persistence semantics. |
| B. Explicit unresolved/external reference state | Deferred. It preserves source identity best, but requires changing `CrossReference` semantics and resolver behavior. That is too deep for M25A and risks becoming a second reference system if rushed. |
| C. Degrade to non-reference inline `Text` | Accepted for M25. It prevents wrong binding with the current model, is deterministic, does not affect persistence, and keeps the normal resolver unchanged. It is explicitly lossy. |
| D. Reject structured transfer / force plain text | Too restrictive for mixed fragments. It is correct but would discard unrelated structured content only because one external reference cannot be safely preserved. |
| E. Better minimal model-supported solution | None found in the current model that satisfies the constraints without sentinel IDs, provenance, hidden caches, heuristic matching, or resolver changes. |

Chosen solution:

- Determine whether each reference target is internal or external.
- For internal references, rewrite through `IdentityRemap`.
- For same-document external references whose original target remains in the destination, preserve raw ID.
- For cross-document or unproven external references, replace the `CrossReference` inline node with `Text`.
- Use the source document's display text for that reference when available.
- If the source reference was already unresolved, use the same missing-reference display text currently produced by `CrossReferenceResolver`.
- Record a degraded-reference diagnostic containing the original source key and the fact that active reference semantics were dropped.

This does not add a second resolver. After insertion, there is no reference for normal `CrossReferenceResolver` to resolve.

Tradeoff:

- The pasted document no longer contains an active `CrossReference` for cross-document external references.
- The original target identity is not preserved in semantic document content.
- It preserves semantic safety by preventing false resolution.
- The source display label survives as ordinary text.
- Later repair may use transfer diagnostics during the paste operation, but those diagnostics are not persistent document semantics.

## 16. Target-Kind To Namespace Mapping

`CrossReferenceTargetKind` maps to identity namespaces as follows:

| CrossReference kind | Target namespace | Target model |
|---|---|---|
| `SECTION` | `StableIdentityKind.SECTION` | `Heading.id()` |
| `EQUATION` | `StableIdentityKind.EQUATION` | `EquationBlock.id()` |
| `TABLE` | `StableIdentityKind.TABLE` | `TableBlock.id()` |
| `FIGURE` | `StableIdentityKind.FIGURE` | `FigureBlock.id()` |

Transfer must use this explicit mapping. It must not infer target namespace from strings.

## 17. Internal Vs External References

Internal transferred reference:

- target identity is provided by fragment content;
- reference is rewritten to the target's destination identity;
- if target remaps, reference remaps.

External reference:

- target identity is not provided by fragment content;
- target block is not pulled into resource closure;
- no heuristic retargeting;
- same-document external references can remain bound to the original document target;
- cross-document external references degrade safely to ordinary text.

Fragment construction can identify internal targets with a source identity index:

- enumerate stable IDs provided by fragment content;
- enumerate stable IDs referenced by inline CrossReferences;
- compare by `StableIdentityKey`.

This does not require a generic graph infrastructure.

## 18. Dataset Binding Remap

Dataset bindings are reference-like but not `CrossReference`.

Transfer must rewrite:

- `DatasetTableBinding.datasetId`;
- each `DatasetPlotBinding.datasetId`;
- bindings inside Figure-contained plots.

Rules:

- dataset `PRESERVED`: binding remains unchanged;
- dataset `REUSED_EXISTING`: binding points at existing destination dataset ID;
- dataset `REMAPPED_NEW`: binding rewrites to new dataset ID;
- dataset missing from source closure: binding remains as degraded unresolved ID unless a future policy explicitly rejects transfer.

Column IDs remain dataset-local and normally unchanged.

If a future policy remaps columns, it must be represented as a resource-local remap, not a document-global identity decision.

## 19. Figure Identity

Figure identity has three parts:

- outer figure ID: document-global `FIGURE` identity;
- caption inline content: may contain CrossReferences;
- contained visual content: currently `PlotBlock` or `DiagramBlock`.

Policy:

- Outer figure ID remaps like other copied document content.
- Caption CrossReferences use the same internal/external reference rules as paragraph and heading content.
- Contained `PlotBlock` currently has no document-global ID.
- Dataset bindings inside contained plots use dataset resource remap.
- Contained `DiagramBlock` internals remain diagram-local.

ROM-10 owns exact Figure composite traversal/materialization details.

## 20. Diagram Identity Boundary

Diagram identities are composite-local.

Current scopes:

- `DiagramElementId`: unique inside a `DiagramDefinition`.
- `DiagramPortId`: unique inside one element's ports.
- `DiagramEndpoint`: internal reference to element + port.
- `MechanicalConstraint.subjectId/peerId`: internal references to diagram elements.
- `MechanicalPartReference.targetId`: internal reference to a diagram element.

Whole `DiagramBlock` transfer treats the diagram as one self-contained semantic value.

Document transfer must not:

- remap diagram elements merely because another diagram has the same element ID;
- compare diagram port IDs across diagrams;
- globalize electrical terminal IDs;
- flatten mechanical references into document-global identity.

Diagram-local remapping is a future diagram-subgraph editing concern, not M25A document transfer.

## 21. Cut Semantics

Cut has no special identity semantics.

Cut is:

1. construct/export the same fragment as copy;
2. write clipboard/carrier successfully;
3. delete source content through editor mutation;
4. paste later according to normal destination transfer policy.

There is no clipboard-level "move preserves ID" rule.

## 22. Undo / Redo Invariants

M24 history rules remain:

- transfer allocation happens before commit;
- committed `EditorState` contains all resulting IDs and rewritten references;
- undo restores the previous snapshot;
- redo restores the committed snapshot;
- redo must not call `DocumentTransfer`;
- redo must not allocate fresh IDs;
- clipboard/sidecar metadata is not history.

Existing tests already protect this pattern for current stable-ID paste behavior.

## 23. Determinism

Given:

- the same source fragment;
- the same destination document state;
- the same identity allocator policy;
- the same same-document/cross-document context;

transfer must produce the same identity decisions.

After a first paste mutates the destination, a second paste sees a different destination state and may produce different destination IDs.

This is deterministic state-dependence, not nondeterminism.

## 24. Validation Interaction

Transfer does not replace validation.

Transfer is responsible for:

- avoiding document-global ID collisions it can detect;
- producing collision-free generated IDs;
- rewriting references/bindings according to accepted policy;
- reporting transfer diagnostics.

`DocumentValidator` remains responsible for:

- duplicate ID errors;
- missing CrossReference targets as warnings;
- missing dataset bindings/columns as warnings;
- invalid dataset/table/diagram structure;
- diagram-local consistency.

After transfer:

- destination IDs must satisfy uniqueness;
- preserved CrossReferences may remain unresolved when the source/destination semantics allow that degraded state;
- cross-document external references that cannot be proven safe are no longer inserted as active CrossReferences;
- missing resources may remain degraded warnings;
- structural invalidity remains a validation error.

## 25. Identity Conflict Matrix

| Identity | Namespace | Copied as | Collision detection | Preserve/remap/reuse | Reference rewrite needed? | Repeated paste | Validation owner |
|---|---|---|---|---|---|---|---|
| Heading ID | document-global `SECTION` | content | destination headings | preserve if unused; remap if duplicate content | yes for internal SECTION refs | fresh remap each materialization on collision | `DocumentValidator`, `CrossReferenceResolver` |
| Equation ID | document-global `EQUATION` | content | destination equations | preserve if unused; remap if duplicate content | yes for internal EQUATION refs | fresh remap each materialization on collision | `DocumentValidator`, `CrossReferenceResolver` |
| Table ID | document-global `TABLE` | content | destination tables | preserve if unused; remap if duplicate content | yes for internal TABLE refs | fresh remap each materialization on collision | `DocumentValidator`, `CrossReferenceResolver` |
| Figure ID | document-global `FIGURE` | content | destination figures | preserve if unused; remap if duplicate content | yes for internal FIGURE refs | fresh remap each materialization on collision | `DocumentValidator`, `CrossReferenceResolver` |
| Dataset ID | document-global `DATASET` | resource | destination datasets | same-doc reuse; cross-doc preserve if unused; remap on collision without provenance | dataset bindings rewrite | same-doc reuse; cross-doc fresh remap on repeated collision | `Document`, `DocumentValidator`, dataset resolvers |
| Dataset column ID | dataset-local | resource-local value | dataset constructor/validator | preserve within dataset | table/plot bindings keep column IDs | unchanged | `ScientificDataset`, `DocumentValidator` |
| Dataset row ID | dataset-local metadata | value | no current uniqueness owner | preserve | none today | unchanged | none currently |
| Diagram element ID | diagram-local | composite-local value | `DiagramDefinition`, `DocumentValidator` | preserve inside whole diagram | diagram-internal only | unchanged | `DiagramDefinition`, `DocumentValidator` |
| Diagram port ID | element-local | composite-local value | `DocumentValidator` | preserve inside element | diagram endpoints only | unchanged | `DocumentValidator` |
| CrossReference target ID | reference to document-global content | inline content or degraded text | target-kind namespace lookup | internal: remap; same-doc external: preserve; cross-doc external: degrade to non-reference `Text` | n/a for degraded text; otherwise follows rule per paste | `CrossReferenceResolver`, `DocumentValidator` |

## 26. CrossReference Behavior Matrix

| Scenario | Inserted semantic form | Target ID behavior | Diagnostic | Resolver outcome | Lossless/degraded |
|---|---|---|---|---|---|
| Reference + target copied together | `CrossReference` | Rewritten to copied target's destination ID through `IdentityRemap`. | None unless another transfer issue occurs. | Normal resolver resolves the copied target. | Lossless. |
| Reference only, same document, target still exists | `CrossReference` | Original target ID preserved because destination identity is proven to be the same document identity. | None unless the source reference was already unresolved. | Normal resolver resolves the original target. | Lossless if target still exists. |
| Reference only, cross-document, destination target absent | Non-reference `Text` | No active target ID is inserted; source target key is retained only in transfer diagnostics. | External reference degraded; original source key recorded for possible UI repair. | No resolver involvement for the degraded text. | Intentionally degraded. |
| Reference only, cross-document, unrelated same-ID target exists | Non-reference `Text` | No active target ID is inserted, so the unrelated destination target cannot capture the reference. | External reference degraded because target identity is unproven/colliding. | No resolver involvement for the degraded text. | Intentionally degraded. |
| Reference only, destination coincidentally contains semantically similar target | Non-reference `Text` | No active target ID is inserted without explicit provenance. | External reference degraded; no heuristic match attempted. | No resolver involvement for the degraded text. | Intentionally degraded. |
| Multiple references to one copied target | `CrossReference` for each copied reference | All rewrite to the same copied target destination ID. | None unless another transfer issue occurs. | Normal resolver resolves all to the copied target. | Lossless. |
| Copied Figure caption with external reference | Caption inline node becomes non-reference `Text` for that reference. | No active target ID is inserted for the external caption reference. | Caption reference degraded; original source key recorded in diagnostics. | Resolver ignores the text node. | Intentionally degraded for that reference only. |
| Copied Heading/Section reference | If the heading target is copied, `CrossReference`; otherwise cross-document external references become non-reference `Text`. | Internal target IDs remap; external cross-document IDs are not inserted. | None for internal; degraded diagnostic for external. | Normal resolver resolves internal/remapped references only. | Lossless for internal; degraded for external. |
| Source reference already unresolved | Same-document may preserve unresolved `CrossReference`; cross-document inserts non-reference `Text` using source missing-reference display. | Same-document preserves original ID; cross-document inserts no active target ID. | Source reference already unresolved, and cross-document transfer degraded it. | Same-document remains unresolved through normal resolver; cross-document has no resolver involvement. | Already degraded; cross-document remains intentionally degraded. |

## 27. Dataset Resource Reuse Matrix

| Scenario | Expected result |
|---|---|
| Same document, dataset exists unchanged | Reuse existing dataset ID; bindings stay or point to same ID. |
| Cross document, destination ID unused | Add dataset with source ID; bindings preserve source ID. |
| Cross document, same ID + same value | No provenance; allocate fresh dataset ID and rewrite bindings. |
| Cross document, same ID + different value | Allocate fresh dataset ID and rewrite bindings. |
| Cross document, same value + different ID | Do not dedupe; add/preserve based on source ID collision rules. |
| Repeated paste after prior import | If source ID now occupied and no accepted provenance exists, allocate another fresh dataset ID and rewrite bindings. |
| Source dataset missing | Materialize content degraded; bindings remain unresolved; diagnostic emitted. |
| Destination dataset edited after previous import | Treat as ordinary occupied destination ID; allocate fresh ID on subsequent cross-document paste. |

## 28. Pseudocode-Level Contracts

Design sketch only:

```java
enum StableIdentityKind {
    SECTION,
    EQUATION,
    TABLE,
    FIGURE,
    DATASET
}

record StableIdentityKey(StableIdentityKind kind, String id) {}

enum IdentityDisposition {
    PRESERVED,
    REUSED_EXISTING,
    REMAPPED_NEW
}

record IdentityDecision(
        StableIdentityKey source,
        StableIdentityKey destination,
        IdentityDisposition disposition
) {}

record IdentityRemap(List<IdentityDecision> decisions) {
    Optional<StableIdentityKey> destinationFor(StableIdentityKey source) { ... }
}

record TransferIdentityContext(
        Document destination,
        TransferDocumentRelation relation,
        IdentityAllocator allocator
) {}

enum TransferDocumentRelation {
    SAME_DOCUMENT,
    CROSS_DOCUMENT_OR_UNKNOWN
}

record ReferenceRewriteResult(
        InlineNode replacement,
        List<TransferDiagnostic> diagnostics
) {}
```

`ReferenceRewriteResult.replacement` may be a `CrossReference` for internal or proven same-document references, or a `Text` node for intentionally degraded cross-document external references.

No type here is a persistence ID, plugin API, graph identity, or sentinel ID convention.

## 29. Future Pressure Test

- Image/media resources: fit as future resource identities; no need to change document-global content keys.
- Bibliography/citations: likely need `BIBLIOGRAPHY_ENTRY` or citation resource keys later; namespace-aware model survives.
- Footnotes/endnotes: could become document-global target kind later; add a kind deliberately.
- Equation labels: already fit `EQUATION`.
- External datasets: require a resource identity policy, but no content-hash dedupe or hidden cache.
- Templates: transfer can materialize template content through same allocator rules.
- Document import: same as cross-document transfer; remap destination collisions.
- Copy across two open Scholar documents: works as cross-document; no persistent origin required.
- Public API later: explicit `StableIdentityKind` avoids raw string ambiguity.

The model survives these without a universal graph engine.

## 30. Do-Not-Patch Rules

- Do not add another namespace-specific remapper in `EditorSession`.
- Do not use raw `Map<String, String>` for transfer identity.
- Do not heuristic-retarget CrossReferences.
- Do not preserve cross-document external raw IDs when they would bind to unrelated destination targets.
- Do not invent synthetic/sentinel CrossReference target IDs as a transfer escape hatch.
- Do not reuse datasets by same textual ID without semantic proof.
- Do not dedupe resources by content equality.
- Do not use hidden process-local provenance to change document semantics.
- Do not introduce persistence/document-origin IDs for M25A transfer.
- Do not globalize diagram-local IDs.
- Do not regenerate IDs during redo.
- Do not copy derived display numbers as identities.
- Do not create a second CrossReference resolver.

## 31. Decisions Deferred To ROM-10

ROM-10 must decide:

- how Figure traversal feeds identity indexing and resource closure;
- exact rewrite order for Figure outer ID, caption references, and contained plot dataset bindings;
- whether Figure-contained diagram diagnostics need composite transfer diagnostics;
- whether whole `DiagramBlock` remains the only document-level diagram transfer unit;
- whether future Figure content types require additional identity/resource hooks.

## 32. ROM-7/8 Clarifications

ROM-7 does not need to be edited.

ROM-9 clarifies:

- `StableIdentityKey(kind, id)` is only for document-global transfer identities in M25A.
- `IdentityRemap` should include disposition/provenance of the decision, not only old-to-new strings.
- External cross-document CrossReferences cannot safely keep raw target IDs when a destination target could capture them.
- The current inline/reference model cannot represent an active unresolved external reference while preserving the original source target identity, so M25A uses explicit text degradation instead of sentinel IDs.

ROM-8 does not need to be edited.

ROM-9 clarifies:

- no persistent provenance is added for dataset reuse in M25A;
- repeated cross-document paste remaps dataset resources on ID collision;
- same-ID same-value cross-document datasets are not automatically reused.

ROM-11 can consolidate these accepted clarifications into the implementation plan.

## 33. Open Questions

1. Should a future semantic model add an explicit unresolved/external CrossReference state that preserves source target identity without participating in normal resolution?
2. Should transfer diagnostics expose the original external reference target to UI repair flows in M25B or later?
3. What exact source display string should be used when degraded cross-document references have no source resolver context available?
4. Should dataset-only copy be represented as fragment content or resource-primary transfer in M25B?
5. Should optional dataset row IDs get uniqueness rules before native persistence?

None of these block the ROM-9 policy.

## 34. Recommendation Whether ROM-9 Can Close

ROM-9 can close after review if the team accepts:

- document-global namespace-aware identity keys;
- source ID preservation in fragments;
- destination-aware remap in transfer;
- content identity duplication vs resource identity reuse distinction;
- no cross-document resource reuse without explicit accepted provenance;
- safe text degradation of unproven external CrossReferences to prevent accidental rebinding without sentinel IDs;
- dataset binding rewrites through dataset identity decisions;
- diagram-local identity opacity;
- redo never rerunning transfer allocation.

This gives ROM-10 a bounded Figure/Diagram composite policy problem and gives M25B enough identity semantics to avoid another round of local remap patches.
