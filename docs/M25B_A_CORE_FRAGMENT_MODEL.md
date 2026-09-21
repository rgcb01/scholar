# M25B-A - Core Fragment Model

Status: COMPLETE. M25A architecture is accepted; M25B implementation is IN PROGRESS, not complete. No clipboard/editor integration is included. Authority: [M25A_TRANSFER_ARCHITECTURE_CONTRACT.md](M25A_TRANSFER_ARCHITECTURE_CONTRACT.md), especially sections 4, 8-10, 18-21.

## Implemented Types

All new production sources live in `src/main/java/dev/rgcb/scholar/transfer` and depend only on Java plus the existing document/data model.

| Type | Responsibility |
|---|---|
| `DocumentFragment` | Immutable source content, ordered complete dataset resources, verifiable identity index. |
| `FragmentContent` | Closed nested records `Blocks`, `InlineSegments`, `ResourcePrimary`. |
| `StableIdentityKind` | Exactly SECTION, EQUATION, TABLE, FIGURE, DATASET. |
| `StableIdentityKey` | Namespace plus unchanged nonblank source ID; explicit CrossReference target-kind mapping. |
| `FragmentIdentityIndex` | Immutable provided/referenced membership; internal typed AST enumeration. |
| `RuntimeDocumentToken` | Opaque runtime-only capability with object-identity equality. |
| `SourceTransferMetadata` | Optional token, typed target/dataset witnesses, textual reference exports. |
| `TransferContext` | Destination Document, optional destination token, explicit source companion. |
| `TransferDiagnostic` | Severity/code/message, optional original identity and fragment-relative location; nested Severity/Code enums. |

These are internal evolving contracts, not a published extension API. No existing production types were changed. No generic TransferResult, allocator, IdentityRemap, extraction service or DocumentTransfer engine is added prematurely: concrete stage results follow when the stages exist. No genuinely new architectural choice requires an ADR.

## Fragment Shape

```java
DocumentFragment(
    FragmentContent content,
    List<ScientificDataset> resources,
    FragmentIdentityIndex identities
)
```

The two-argument constructor derives the index. The three-argument constructor verifies a supplied index by exact set equality with enumeration of actual content/resources. The index cannot declare a reference target present merely because it is referenced.

- Blocks has one or more ordered existing BlockNode values. Paragraph, Heading, EquationBlock, TableBlock, PlotBlock, DiagramBlock, FigureBlock and TableOfContentsBlock are supported.
- InlineSegments has one or more ordered existing InlineContent values. Text runs/marks/segmentation and CrossReferences remain unchanged. It carries no source Heading style/ID or selection/path/direction. One empty segment represents a no-op; multiple empty segments preserve structural boundaries.
- ResourcePrimary has one or more DATASET keys selecting complete datasets stored once in resources. Selected keys must exist there. It has no document block root or second dataset copy.
- Whole EquationBlock remains block content. Local MathClipboardPayload(MathSequence) remains unchanged, outside FragmentContent; no local math-to-equation conversion is introduced.

Unknown BlockNode/InlineNode implementations are rejected during fragment index construction, rather than silently asserting supported semantics. This does not introduce block registration, plugins or a universal visitor/graph framework.

## Resources And Identity

Resources are an ordered immutable List of complete ScientificDataset values. The fragment retains all schema/columns/rows/value variants/display metadata/local IDs without slicing, flattening or normalization. Duplicate DATASET source IDs are rejected, including repeated identical instances or different values with the same ID. Distinct dataset IDs remain distinct even if all other contents are equal. No equality/hash/name/schema deduplication occurs.

Bindings in canonical TableBlock rows/settings and PlotBlock series stay unchanged. A table and plot referencing D can coexist with one D entry. Missing dataset dependencies can be represented as referenced but not provided keys, supporting later diagnosed incomplete extraction; this is NOT authorization to insert a broken view. Required-resource accounting/resolution and strict pre-insertion rejection belong to later phases.

Global provided identities come from whole Heading/Equation/Table/Figure IDs and included dataset IDs. Absent optional IDs remain absent. Ambiguous duplicate provided keys are rejected. Same ID spelling in different global namespaces is legal. Referenced keys come from CrossReferences in paragraphs/headings/stored table cells/Figure captions and dataset bindings in tables/plot series/Figure plots.

Owner-local column/optional row identities stay inside their dataset. Diagram element IDs, element-local ports, endpoint pairs, constraints and other internal references stay inside the complete Diagram value. No local identities enter the global index, no plot/diagram global ID is invented, and two separate diagrams may repeat local IDs. No standalone owner-local transfer key type is necessary for this whole-owner scope.

Enumeration reads only supplied values: it does not select/extract ranges, resolve dependencies from a source Document, traverse reference targets, allocate IDs, rewrite bindings, or resolve labels. Index sets express membership, not allocation order; future planners must traverse ordered content/resources rather than Set/Map iteration.

## Runtime Proof Companion

Provenance is deliberately outside DocumentFragment and Document AST. RuntimeDocumentToken.create returns a new opaque capability each time. It contains no UUID, semantic ID, content hash, file path or persisted origin. Equal content/IDs do not imply equal tokens. The trusted live-document owner will later manage token lifetime; this phase creates no global store or editor integration.

SourceTransferMetadata contains separate immutable typed maps for original BlockNode targets and ScientificDataset witnesses, plus source reference export strings. The split maps are a type-safe implementation of the contract's conceptual typed witness map. Keys must agree with each witness's actual source identity. Reference text keys must belong to a CrossReference target namespace, not DATASET. Witness instances are retained exactly, not cloned/reconstructed: later conservative applicability proof requires the same original immutable value instance.

TransferContext.hasSameDocumentToken compares present capabilities by `==`. Unknown source/destination tokens and different tokens are not proof. Matching tokens prove only the live-document relation. They do NOT prove any target/resource is still applicable; lookup plus exact witness-instance comparison remains deferred. Context stores the destination immutable Document as explicit operation input, never in the fragment or source companion. Allocator and receiving-shape planning will be added with their implementing phases.

No reference exports are generated or consumed here. SourceTransferMetadata.unknown permits conservative direct callers without inventing provenance. Metadata remains operational, outside semantic history and native persistence; it is not a hidden cache.

## Diagnostics

TransferDiagnostic supports INFO/WARNING/ERROR and the accepted degraded/rejected condition codes, plus SOURCE_DISPLAY_UNAVAILABLE. Original typed identity and fragment-relative textual location are optional. Null mandatory fields and blank message/location are rejected. No actual insertion diagnostics, result status machine, semantic repair state or UI is implemented.

The fragment has no fallback String. Source textual reference exports are explicit companion data only; OS text and rich clipboard matching remain transport concerns. Keeping these separate prevents resolved labels from becoming canonical fragment semantics.

## Immutability And Malformed States

All added collection boundaries use List.copyOf/Set.copyOf/Map.copyOf, rejecting null entries and preventing subsequent input mutation. Existing built-in AST records already defensively copy their lists/sets, including inline nodes/marks, math sequence atoms, table rows/cells, plot series/points, diagram elements/ports/connections and dataset columns/rows/values. Those immutable semantic values are safely retained directly; there is no serialization/reparse or deep-copy rewrite. Source immutable replacement does not alter the fragment snapshot.

This guarantee is for supported built-in immutable AST values, not arbitrary mutable implementations of the existing open MathExpression/DiagramElement interfaces. Their broader trust/validation boundary is unchanged; no third-party plugin contract is introduced. Later extraction/materialization must perform accepted structural validation before insertion.

Constructors reject null content/roots/resources/index/context/metadata, empty root categories, blank identity keys, non-DATASET resource-primary keys, missing selected primary resources, duplicate provided identities/resources, contradictory supplied indexes and mismatched witness keys. IDs are not trimmed or normalized by transfer values. Basic constructor checks do not duplicate DocumentValidator: source warnings, full graph validity, resource completeness and destination/selection eligibility remain later checks.

## Ownership And Derived State

Figure is one semantic root, retaining its wrapper ID, caption and currently supported Plot/Diagram content. Traversing contained bindings/reference atoms for index membership does not unwrap the Figure or register another top-level root. Captions preserve raw references. Table/plot canonical stored values are not replaced by resolved dataset views.

TOC remains its zero-field semantic marker, not a snapshot of headings, numbering or labels. Layout, rendered geometry, resolver caches, derived nets, selection/focus/history and Minecraft objects are absent. Authored diagram bounds/workspace aspect ratio and plot settings remain semantic content.

## Legacy Coexistence And Deferred Work

All eight existing clipboard payload families remain unchanged. Whole-block carriers will later adapt to fragment content, inline payloads to InlineSegments, datasets to ResourcePrimary. MathClipboardPayload stays local; single-cell text and external math import stay scoped. ScholarClipboardService and EditorSession neither import nor consume these new types yet.

Deferred: semantic extraction/slicing, resource closure discovery/resolution, source export generation, per-target applicability, allocation/reservations/IdentityRemap, reference degradation/binding rewrite, result materialization, destination structural staging/insertion, validators at stage boundaries, atomic history/clipboard integration and manual Minecraft regression. No M25B-B work, persistence, UI, features or public extension infrastructure is included.

## Validation

Three new focused suites: DocumentFragmentTest (41 tests), TransferContextTest (11), TransferBoundaryTest (2): 54 new tests. They cover every current block family, inline marks/order/reference preservation, local math separation, whole/shared datasets, namespaces/index consistency, local graph ownership, token/metadata invariants, defensive copying, malformed inputs, Figure/TOC and source dependency boundaries. Existing tests are unchanged.

- `gradlew.bat test`: BUILD SUCCESSFUL; 1190 tests, zero failures/errors/skips.
- `gradlew.bat build`: BUILD SUCCESSFUL.
- `git diff --check`: passes.
- Source scan and boundary test: no Minecraft/NeoForge/Mojang/client/editor/clipboard/layout/render dependency in transfer core.

These results validate the value-model-only subphase, not a working transfer pipeline or manual Minecraft acceptance. Existing uncommitted M25A documents are preserved. No commit/push is performed. M25B-A is complete; the next subphase can build on these values when explicitly requested.
