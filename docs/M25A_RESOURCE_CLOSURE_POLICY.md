# M25A Resource Closure Policy

Status: ROM-8 architecture design document. Not yet an accepted ADR. No production classes are implemented by this document.

Authority notice: ROM-8 is accepted supporting rationale. [M25A Transfer Architecture Contract](M25A_TRANSFER_ARCHITECTURE_CONTRACT.md) governs M25B. In particular, provisional repeated cross-document reuse, inferred same-document identity, and missing-resource degraded insertion below are superseded by explicit proof, independent resource materialization, and required-resource rejection.

## 1. Executive Decision

Scholar transfer should use a minimal transferable resource closure:

- a fragment carries the selected semantic content;
- plus only the document-owned resources required to preserve that content's current semantics;
- resources are carried once per typed source identity;
- derived views, layout, transient editor state, and unrelated document state are never part of closure.

For the current repository, the only top-level document-owned transferable resource is `ScientificDataset`. Dataset-backed `TableBlock` and `PlotBlock` therefore require the referenced whole `ScientificDataset` in the fragment closure when the source document contains it.

This is a scoped typed closure policy, not a generic dependency graph or persistence format.

## 2. Definition Of Transfer Resource

A transfer resource is semantic document-owned data that is not itself the selected document content but is required for that content to keep its meaning after transfer.

Current transfer resource:

- `ScientificDataset`: document-owned tabular scientific data referenced by dataset-backed tables and plots.

Current non-resources:

- `BlockNode` values themselves: these are fragment content.
- `Paragraph` and `Heading`: fragment content, not resources.
- `CrossReference` target blocks: semantic references, not dependency resources.
- `TableOfContentsBlock`, outline entries, section numbers, and figure/table/equation numbers: derived content.
- resolved reference labels: derived display text.
- resolved dataset-backed table rows or plot points: derived views.
- layout geometry, rendered glyphs, scroll position, focus, selection, history, menus, and toolbar state: transient or rendered state.
- diagram-local elements, ports, connections, and mechanical internal references: semantic data inside `DiagramBlock`, not document resources.

Future resource families may include embedded media, bibliography entries, external dataset descriptors, or attachments, but ROM-8 does not introduce those systems or any third-party resource registry.

## 3. Resource Ownership Model

`Document` owns datasets as top-level state through `Document(List<BlockNode> blocks, List<ScientificDataset> datasets)`.

`ScientificDataset` is immutable value data:

- stable document-level dataset ID;
- optional display name;
- ordered columns;
- ordered rows;
- unique column IDs within the dataset;
- row width matching the dataset column count.

Multiple blocks may reference the same dataset:

- `TableBlock.datasetBinding()` stores a `DatasetTableBinding(datasetId, columnIds)`.
- `PlotSeries.datasetBinding()` stores a `DatasetPlotBinding(datasetId, xColumnId, yColumnId)`.
- `DatasetRegistry.find(document, datasetId)` resolves by document-owned dataset ID.

A `DocumentFragment` may carry a copy of a source dataset as immutable value data. It must not retain a live pointer back to the source `Document`.

One transferred resource can serve multiple transferred blocks. For example, a transferred table and plot that both reference `projectile-test` should share one dataset resource entry inside the closure.

## 4. Minimal Closure Rule

The closure of a fragment is the union of required document-owned resources directly required by the fragment's semantic content, traversing only accepted ownership boundaries.

Rules:

- Include a source resource only when selected content directly depends on it for semantic meaning.
- Include each required source resource at most once.
- Do not include all resources from the source document.
- Do not traverse `CrossReference` targets as resources.
- Do not include derived state.
- Do not infer resources from plain-text fallback.
- Do not recurse through arbitrary object graphs.

For current Scholar, direct resource discovery is enough. A generic graph walker would add complexity without evidence.

## 5. Dataset-Backed Table Policy

A dataset-backed `TableBlock` requires the referenced whole `ScientificDataset` when the source document contains that dataset.

Reasoning from current code:

- `DatasetTableBinding` references a dataset ID and either all columns or a stable selected column list.
- `DatasetTableResolver.resolve` derives the visible table from the current document dataset.
- `DatasetTableBinding.usesAllColumns()` means an empty column list intentionally represents all current dataset columns.
- Dataset editing mutates the dataset resource, and bound table views update through resolver recomputation.
- Rows and columns form one rectangular dataset value object; row values align with the full column set.

Closure action:

- If the bound dataset exists in the source document, include the entire `ScientificDataset`.
- If the bound dataset is missing, extract the table content but record a missing-required-resource diagnostic.
- Do not slice to selected columns in M25.

## 6. Dataset-Backed Plot Policy

A dataset-backed `PlotBlock` requires each whole `ScientificDataset` referenced by its dataset-backed series.

Reasoning from current code:

- `PlotDefinition` contains a list of `PlotSeries`.
- Each `PlotSeries` may contain a `DatasetPlotBinding(datasetId, xColumnId, yColumnId)`.
- `DatasetPlotResolver.resolve` derives plotted points from the referenced dataset columns and rows.
- Missing datasets or columns produce empty derived points and validation warnings, not a structurally different plot.

Closure action:

- Include every distinct source dataset referenced by plot series bindings.
- If multiple series reference the same dataset, include it once.
- If a referenced dataset is missing, extract the plot content but record a missing-required-resource diagnostic.
- Do not copy only x/y columns.

## 7. Shared Resource Deduplication

Resource closure deduplicates by typed source identity.

For current datasets, the key is:

```text
ResourceKey(DATASET, datasetId)
```

If a fragment contains:

- two tables referencing the same dataset;
- a table and a plot referencing the same dataset;
- a Figure containing a dataset-backed plot and another copied dataset-backed table;

then the closure contains that dataset once.

Deterministic ordering should not affect semantics. If implementation needs a stable order for tests or serialization of sidecar debug data, use first discovery order from deterministic fragment traversal or sort by typed resource key.

## 8. Multi-Block / Composite Closure

For multiple selected semantic nodes, fragment closure is the union of each node's required resource closure.

Examples:

- Two dataset-backed tables referencing the same dataset: one dataset resource.
- Table plus plot referencing the same dataset: one dataset resource.
- Two plots referencing different datasets: both datasets.
- Manual table plus dataset-backed plot: only the plot's dataset.
- Figure containing dataset-backed plot: the plot's dataset is discovered through the figure content.
- Figure containing diagram: no document-owned resource unless future diagram content gains one.
- Figure caption containing CrossReference: no target block enters resource closure.

This union rule is compositional but still typed and bounded.

## 9. Figure Closure Behavior

`FigureBlock(id, content, caption)` remains a semantic block with currently supported content of `PlotBlock` or `DiagramBlock`.

Closure policy:

- The outer figure ID is content identity, not a resource.
- The caption is inline semantic content, not a resource.
- Caption `CrossReference` nodes do not pull targets into resource closure.
- If content is a dataset-backed `PlotBlock`, discover dataset resources through that plot.
- If content is a `DiagramBlock`, keep diagram semantic data inside the figure content and do not flatten diagram internals into resources.

ROM-10 owns exact Figure composite remap behavior. ROM-8 only establishes that resource discovery is compositional through figure-owned content.

## 10. CrossReference / Resource Distinction

`CrossReference` is not resource closure.

If a paragraph is copied with a CrossReference to a figure, the target figure is not automatically copied. The reference remains semantic content and ROM-9 decides whether it is rewritten, preserved, or reported as degraded.

Reason:

- CrossReferences point to other document content, not document-owned resources.
- Automatically dragging target blocks into closure would turn copy of text into hidden block copy.
- Missing reference targets are already valid degraded document states reported as warnings.

Resource closure answers "what document-owned resources must accompany this content." Reference remapping answers "what happens to semantic links after materialization." These are separate policies.

## 11. Required Vs Optional Dependencies

Current dataset dependencies are required for lossless semantic preservation.

- Dataset-backed table with source dataset present: dataset is required closure.
- Dataset-backed plot with source dataset present: dataset is required closure.
- Dataset binding whose source dataset is missing: source document is already degraded; fragment construction may still extract content and record a missing-resource diagnostic.
- Missing dataset column: source document is degraded; if the dataset exists, include the dataset exactly as present and record the missing-column diagnostic through validation/closure diagnostics.

There are no optional or enrichment resources in the current model.

Future resource types may distinguish embedded transferable resources from external references, but ROM-8 does not create that system.

## 12. Missing-Resource / Degraded Source Behavior

Fragment construction should reject only structurally invalid source selections or unconstructable semantic content.

If source content references a missing dataset:

- fragment construction may succeed;
- the fragment records the selected semantic content;
- the resource set lacks the missing resource because none exists in the source document;
- a diagnostic records the missing required resource;
- transfer may produce a degraded but valid destination result.

This matches current `DocumentValidator` behavior where missing datasets and missing columns are warnings, while duplicate binding columns are errors.

Validation and transfer remain separate:

- closure diagnostics explain extraction/materialization conditions;
- `DocumentValidator` reports document validity after transfer.

## 13. Destination Resource Matching

Destination resource matching should be conservative.

Cases:

- Destination has no dataset with the source ID: add dataset, possibly with remapped ID chosen by ROM-9.
- Destination has the same dataset ID and the same semantic resource identity: reuse existing dataset.
- Destination has the same dataset ID but different dataset content/meaning: do not silently bind to it; add a remapped dataset and update transferred bindings.
- Destination contains equal dataset contents under another ID: treat as distinct unless an explicit future deduplication policy says otherwise.
- Destination has overlapping column IDs inside a dataset with a different dataset identity: not a conflict if a new dataset resource is materialized.

For M25, matching should be identity-based and exact within the transfer context, not content-hash-based.

## 14. Resource Equivalence

Two `ScientificDataset` values with equal contents but different IDs are distinct semantic resources.

Rationale:

- Current bindings resolve by dataset ID, not by content.
- Dataset ID is authored semantic identity.
- Content equality can be accidental.
- Content-hash deduplication would make repeated paste and undo/redo harder to reason about.
- Future datasets may gain metadata or external provenance that makes content equality insufficient.

Therefore resource equivalence is identity-based. Exact structural equality can be used only to confirm that a same-ID destination resource is safe to reuse, not to merge different IDs.

## 15. Repeated Paste Semantics

Each materialization of a fragment is conceptually independent.

Recommended policy:

- Same-document repeated paste of a fragment copied from that document may reuse the existing source dataset identity because the destination already owns that exact resource.
- Cross-document paste where the destination lacks the source resource materializes a dataset resource.
- Repeating that cross-document paste should not create another dataset if the previous paste already materialized the same source dataset and it still matches exactly by source identity/materialized content.
- If the destination dataset ID collides with different content, materialize a fresh remapped dataset and rewrite bindings for that paste.

This balances semantic independence with practical avoidance of duplicate large datasets. ROM-9 must define the exact provenance/remap mechanism that lets transfer distinguish "same source resource already imported" from "same text ID but unrelated destination resource."

## 16. Same-Document Transfer Semantics

When copying a dataset-backed table or plot within the same document, the pasted view should continue referencing the existing dataset rather than duplicating it.

Reason:

- Current dataset-backed views are live views over document-owned datasets.
- Same-document duplicate views should remain coordinated with edits to the same dataset.
- Deleting a view does not delete the dataset resource today.

The fragment itself must not carry a live source-document pointer. DocumentTransfer can infer same-document reuse through destination resource matching against source resource identity preserved in the fragment resource key.

## 17. Cross-Document Transfer Semantics

For future cross-document transfer:

- If destination lacks the source dataset identity, materialize the dataset into the destination.
- If destination has the same ID and matching same semantic resource, reuse it.
- If destination has the same ID but different data, allocate a new destination dataset ID and update transferred bindings.
- If the source fragment lacks a required dataset because the source was degraded, transfer may materialize content degraded and report diagnostics.

Same textual ID alone must never silently imply same resource.

## 18. Resource ID-Remap Interaction

If a dataset ID changes during transfer, every transferred binding that references it must change consistently:

- `DatasetTableBinding.datasetId`;
- every `DatasetPlotBinding.datasetId`;
- bindings inside Figure-contained plots;
- any future content type that references the same dataset resource.

Column IDs are currently dataset-local:

- `ScientificDataset` requires unique column IDs only within a dataset.
- `DocumentValidator` checks duplicate dataset column IDs inside each dataset.
- `DatasetTableBinding` and `DatasetPlotBinding` resolve column IDs through their dataset ID.

Therefore remapping a dataset ID is normally enough for current bindings. Column IDs should remain unchanged unless ROM-9 later introduces column-level remapping for a concrete collision case inside a transformed dataset.

## 19. Dataset Column Identity Findings

Current column identity is dataset-local.

Evidence:

- `ScientificDataset` constructor rejects duplicate column IDs within one dataset.
- `DocumentValidator.validateDatasets` reports duplicate column IDs inside a dataset.
- `DatasetTableBinding` stores `datasetId` plus a list of `columnIds`.
- `DatasetPlotBinding` stores `datasetId`, `xColumnId`, and `yColumnId`.
- `DatasetTableResolver` and `DatasetPlotResolver` first find the dataset, then resolve column IDs within it.

Current row IDs are optional and not used by table/plot bindings. They are part of the dataset value, not separate document-level identity.

## 20. Resource Conflict Outcomes

Conceptual outcomes:

- `REUSE_EXISTING`: destination already has the same source resource identity and compatible value.
- `ADD_RESOURCE`: destination lacks the resource and can accept its source ID.
- `ADD_REMAPPED_RESOURCE`: destination has an ID collision with different resource value; transfer adds a copied dataset under a fresh ID and rewrites bindings.
- `REJECT_CONFLICT`: reserved for future resource types or cases where remap cannot preserve semantics.
- `DEGRADED_WITHOUT_RESOURCE`: source fragment references a required resource that was missing from source or intentionally unavailable.

For current datasets, `REUSE_EXISTING`, `ADD_RESOURCE`, `ADD_REMAPPED_RESOURCE`, and `DEGRADED_WITHOUT_RESOURCE` are valid. `REJECT_CONFLICT` should be rare and only used if identity/remap policy cannot produce a coherent destination.

## 21. Closure Algorithm

Minimal conceptual algorithm:

1. Start with primary fragment content.
2. Traverse known document-level semantic content types through accepted ownership boundaries.
3. For each table, inspect `datasetBinding`.
4. For each plot, inspect every series `datasetBinding`.
5. For each figure, inspect its content block and caption inline content.
6. For each diagram, treat diagram internals as contained semantic content and do not emit document resources.
7. Add required resource keys to a typed source resource set.
8. Resolve each resource key from the source `Document`.
9. If found, add the immutable resource value once.
10. If missing, record a resource diagnostic.
11. Stop. Do not follow `CrossReference` targets.

Typed closure functions are sufficient for M25:

- `resourcesForBlock(BlockNode block, SourceDocumentContext source)`;
- `resourcesForTable(TableBlock table, SourceDocumentContext source)`;
- `resourcesForPlot(PlotBlock plot, SourceDocumentContext source)`;
- `resourcesForFigure(FigureBlock figure, SourceDocumentContext source)`.

A generic dependency graph framework is not justified now.

## 22. Performance / Large-Resource Tradeoffs

Whole-dataset closure may copy large datasets.

ROM-8 accepts that tradeoff for correctness because:

- current datasets are embedded immutable document resources;
- column slicing would silently change dataset meaning and row context;
- dataset-backed views are live semantic views, not static snapshots;
- no current large-resource storage, lazy loading, or external data descriptor exists.

M25 should not invent chunking, lazy resource loading, or external resource caches.

Future work may add:

- warning diagnostics for large copied resources;
- user-facing "paste linked vs embedded" choices;
- external dataset descriptors;
- performance measurement in M29.

Those are not part of ROM-8.

## 23. Future External-Resource Pressure Test

The policy can extend to future external datasets by distinguishing:

- embedded transferable resources;
- external reference resources;
- cached-but-not-owned data.

For now, `ScientificDataset` is embedded and transferable. Future external datasets should not change the current embedded dataset policy. They may require a new resource type and a policy for whether transfer carries a descriptor, a cache, or only an unresolved external reference.

## 24. Future Resource-Type Pressure Test

Potential future resource families:

- image/media assets;
- bibliography entries;
- citation databases;
- attachments;
- external dataset descriptors;
- templates.

`TransferResourceSet` can remain typed and internal without becoming a plugin registry. It can add deliberate resource families as Scholar adds owned resources. Arbitrary third-party resource registration remains out of scope.

## 25. Pseudocode Contracts

These are design sketches only.

```java
package dev.rgcb.scholar.transfer;

public record ResourceKey(ResourceKind kind, String id) {}

public enum ResourceKind {
    DATASET
}

public interface TransferResource {
    ResourceKey key();
}

public record DatasetTransferResource(
        ScientificDataset dataset
) implements TransferResource {
    public ResourceKey key() {
        return new ResourceKey(ResourceKind.DATASET, dataset.id());
    }
}

public record TransferResourceSet(
        Map<ResourceKey, TransferResource> resources
) {}

public record ResourceClosure(
        TransferResourceSet resources,
        List<ResourceDiagnostic> diagnostics
) {}

public record ResourceDiagnostic(
        ResourceDiagnosticSeverity severity,
        ResourceDiagnosticCode code,
        ResourceKey key,
        String message
) {}
```

This shape is intentionally closed and small. It is not a plugin resource SPI.

## 26. Resource Policy Matrix

| Content | Direct resource dependency? | Required resource type | Closure action | Missing resource behavior | Destination match behavior | Identity/remap requirement | Notes |
|---|---:|---|---|---|---|---|---|
| Paragraph | No | none | none | n/a | n/a | none | CrossReferences inside do not pull targets |
| Heading | No | none | none | n/a | n/a | heading ID handled by ROM-9 | Section number is derived |
| CrossReference | No resource | none | preserve node as content | unresolved reference warning | n/a | rewrite only by ROM-9 if target travels | Target block is not resource closure |
| Equation | No | none | none | n/a | n/a | equation ID handled by ROM-9 | Math AST is content |
| Manual Table | No | none | none | n/a | n/a | table ID handled by ROM-9 | Cell inline content is content |
| Dataset-backed Table | Yes | `ScientificDataset` | include whole dataset once | degraded diagnostic; binding preserved | reuse/add/remap dataset | update binding dataset ID if resource remapped | Do not slice columns |
| Static/manual Plot | No | none | none | n/a | n/a | none currently | Points are content |
| Dataset-backed Plot | Yes | `ScientificDataset` per series dataset | include each whole dataset once | degraded diagnostic; points remain derived/empty | reuse/add/remap dataset | update all series binding dataset IDs if remapped | Do not copy resolved points as resource |
| ScientificDataset copied directly | It is resource content | `ScientificDataset` | include dataset as primary resource/content | source missing means no fragment | reuse/add/remap dataset | dataset ID may remap | Column IDs remain dataset-local |
| Figure with Plot | Maybe | plot resources | recurse into figure plot content | same as plot | same as plot | figure ID plus resource remap | Do not generalize Figure |
| Figure with Diagram | No current document resource | none | keep diagram inside figure content | n/a | n/a | figure ID; diagram-local IDs stay internal | Diagram internals are not resources |
| Diagram | No current document resource | none | none | n/a | n/a | diagram-local IDs not document remap | Whole diagram is semantic content |
| TOC | No | none | none | n/a | n/a | none | Entries are derived from headings |

## 27. Invariants

- Resource closure contains no unrelated source resources.
- Each required source resource appears at most once by typed source key.
- All copied dataset-backed bindings whose datasets exist in source have their required dataset in closure.
- Missing source resources are diagnostics, not fabricated resources.
- CrossReference targets are not pulled into resource closure.
- Derived state is not a resource.
- Closure construction does not mutate source content or source resources.
- Fragment resources are immutable semantic value data.
- Resource ordering does not affect semantic result.
- Repeated discovery is deterministic for the same source document and selected content.
- Same-document dataset-backed view paste reuses the existing source dataset resource.
- Destination same-ID/different-resource conflicts are never silently rebound.

## 28. Do-Not-Patch Rules

- Do not add another block-specific "also copy dataset here" branch in `EditorSession`.
- Resource discovery belongs in fragment construction / transfer closure policy.
- Do not copy every dataset in the document as an easy workaround.
- Do not copy resolved dataset-backed table rows or plot points as authoritative closure.
- Do not resolve missing resources by silently binding to a same-text destination ID.
- Do not dedupe semantic resources by content equality unless a future accepted policy explicitly designs that.
- Do not treat CrossReference targets as resources.
- Do not serialize resource closure as the native Scholar file format.
- Do not flatten Figure or Diagram internals into resource registries.
- Do not introduce generic plugin resource registration during M25.
- Do not make column slicing the default dataset closure optimization.

## 29. Decisions Deferred To ROM-9

ROM-9 must decide:

- exact identity namespace keys;
- dataset ID allocation/remap algorithm;
- how same-document source identity is represented in transfer context;
- how repeated cross-document paste recognizes a previously materialized source resource;
- CrossReference rewrite rules;
- whether source-to-destination resource remap is one map or separate maps per namespace;
- exact diagnostics emitted when references or resources degrade after remap;
- whether column IDs ever require explicit remap despite being dataset-local today.

## 30. Decisions Deferred To ROM-10

ROM-10 must decide:

- exact Figure transfer materialization order;
- figure ID remap interaction with contained plot/diagram content;
- whether Figure-contained plot resources are discovered through generic block closure or a figure-specific adapter;
- how Figure caption CrossReferences are rewritten if caption targets travel;
- whether whole `DiagramBlock` remains the only document-level diagram transfer unit;
- how diagram-local warnings surface during whole-figure transfer.

## 31. ROM-7 Clarifications Needed

ROM-7 does not need to be edited.

ROM-8 clarifies these ROM-7 terms:

- `TransferResourceSet` currently means a typed set of document-owned resources, with `ScientificDataset` as the only implemented family.
- Resource closure is minimal and semantic; it does not mean a graph of all referenced document nodes.
- Dataset resource closure is whole-dataset, not column-sliced.
- Same-document materialization may reuse an existing source resource rather than materializing a duplicate.

ROM-11 can consolidate these clarifications into the final M25A implementation plan.

## 32. Open Questions

1. How should transfer context represent source provenance without storing live source document pointers in fragments?
2. Should repeated cross-document paste reuse a previously imported dataset through explicit provenance metadata, or should that be delayed until implementation evidence appears?
3. Should large dataset transfer emit a warning immediately in M25B, or wait for performance instrumentation?
4. Should missing required source resources be represented by transfer diagnostics only, or also by a resource-stub value? ROM-8 recommends diagnostics only.
5. Should dataset-only copy become a first-class `DocumentFragment` content form in M25B, or remain bridged from `DatasetClipboardPayload` during migration?

None of these block the ROM-8 policy.

## 33. Recommendation Whether ROM-8 Can Close

ROM-8 can close after review if the team accepts:

- minimal resource-based closure;
- whole `ScientificDataset` as the current resource ownership unit;
- no column-sliced dataset closure in M25;
- same-document dataset-backed transfers reusing the existing dataset;
- cross-document transfers materializing/remapping required datasets;
- identity-based resource equivalence;
- CrossReferences excluded from resource closure;
- Figure resource closure composed through contained content without flattening Figure or Diagram internals.

This gives ROM-9 a precise identity/remap problem and ROM-10 a bounded composite-content problem without implementing M25B prematurely.
