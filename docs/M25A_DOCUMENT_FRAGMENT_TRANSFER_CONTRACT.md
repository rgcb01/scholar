# M25A Document Fragment And Transfer Contract

Status: ROM-7 architecture design document. Not yet an accepted ADR. No production classes are implemented by this document.

## 1. Executive Decision

Scholar should introduce two separate architectural concepts in M25:

- `DocumentFragment`: an immutable semantic representation of Scholar content extracted from a document.
- `DocumentTransfer`: a pure-Java service boundary that materializes a `DocumentFragment` into a destination document context.

This is a scoped transfer architecture, not a generic graph framework.

The fragment answers: "what semantic content is being transferred?"

The transfer service answers: "how does that content become valid destination document content here?"

The names `DocumentFragment` and `DocumentTransfer` are appropriate for the current codebase because they match the two responsibilities ROM-6 found to be mixed inside `EditorSession`: payload content and destination-specific identity/resource handling. If implementation later finds that `DocumentTransfer` sounds too much like an operation result, `DocumentTransferService` would be an acceptable implementation name, but the conceptual boundary should remain the same.

## 2. Problem Statement Grounded In ROM-6

ROM-6 found that Scholar currently transfers content through a shared clipboard shell plus many content-specific branches:

- `DocumentBlockClipboardPayload`
- `InlineContentClipboardPayload`
- `MathClipboardPayload`
- `TableClipboardPayload`
- `PlotClipboardPayload`
- `DiagramClipboardPayload`
- `FigureClipboardPayload`
- `DatasetClipboardPayload`

The current implementation works, but transfer policy is fragmented:

- block payload selection lives in `EditorSession.copySelectedBlockForClipboard`;
- paste dispatch and eligibility are repeated in `EditorSession.canPasteFromClipboard` and `pasteFromClipboard`;
- stable ID remapping is namespace-specific and local to `EditorSession`;
- CrossReferences are copied as raw target IDs and never rewritten;
- dataset-backed table/plot views preserve bindings but do not carry required datasets;
- figures remap only the outer figure ID;
- diagram internals remain self-contained and opaque to document-level transfer;
- plain text fallback generation is tied to payload-specific copy branches.

The contract here gives ROM-8/9/10 a place to define resources, identity, and composite behavior without implementing them prematurely.

## 3. Terminology

- Fragment: immutable semantic content extracted from a source document or editor-local semantic scope.
- Fragment content: the primary transferred semantic values, such as blocks, inline content, or a dataset resource.
- Transfer resource: document-owned semantic resource needed by fragment content, such as `ScientificDataset`.
- Source identity: stable IDs as they exist inside the source content.
- Destination identity: stable IDs after destination collision handling.
- Identity remap: explicit source-to-destination ID mapping created during transfer.
- Materialization: producing destination-ready semantic content and resource additions from a fragment.
- Carrier: clipboard or other transport mechanism that stores or transmits a fragment.
- Plain-text fallback: external-compatible text representation; not the domain model.
- Degraded result: transfer that succeeds but leaves valid unresolved references or missing optional dependencies according to existing validation policy.

## 4. Responsibility Boundaries

| Responsibility | Owner |
|---|---|
| selecting source range/object | selection/editor layer |
| deciding whether current selection is transferable | selection/editor layer plus fragment factory capability checks |
| extracting semantic blocks | fragment construction |
| extracting inline content | fragment construction for document-inline transfer; editor-local models for nested scopes |
| extracting math subranges | math editor-local transfer model, optionally adapted as fragment content only when crossing document transfer boundary |
| extracting dataset resources | fragment construction |
| discovering dependencies/resources | fragment construction policy, specified by ROM-8 |
| preserving source IDs | fragment representation |
| enumerating stable IDs in content/resources | fragment representation metadata or helper in transfer package |
| detecting destination ID collisions | transfer service |
| allocating destination IDs | transfer service through an explicit destination identity policy |
| remapping IDs | transfer service |
| rewriting CrossReferences | transfer service, policy specified by ROM-9 |
| resource deduplication/materialization | transfer service, policy specified by ROM-8 |
| destination insertion shape | destination editor/document mutation layer |
| selection after paste | editor/session layer |
| history transaction creation | editor/session layer |
| plain-text fallback generation | clipboard/interchange layer using fragment-aware serializers |
| external clipboard serialization | clipboard/interchange layer |
| validation after transfer | validation/resolution layer, invoked by tests/trust boundaries or editor policy |
| degraded-result reporting | transfer result plus validation diagnostics, not exceptions for ordinary degraded states |

The goal is not to move every editing concern into `DocumentTransfer`. The goal is to remove transfer semantics from `EditorSession` while leaving editor mutation, selection, and history in the editor layer.

## 5. DocumentFragment Contract

`DocumentFragment` is the semantic unit of transfer.

It should:

- be immutable;
- contain semantic source content;
- preserve source stable IDs exactly;
- optionally carry transfer resources in a scoped resource set;
- optionally carry source identity/reference metadata useful for transfer;
- be independent of clipboard transport;
- be independent of native persistence;
- be independent of Minecraft/client classes.

It must not contain:

- `EditorState`;
- `EditorSelection`;
- caret position;
- hover/focus/menu/toolbar state;
- layout geometry;
- rendered text/glyph data;
- resolved figure/section/table/equation numbers as authoritative state;
- dataset-backed resolved view snapshots as authoritative state;
- destination-generated IDs;
- destination insertion position;
- history transaction state;
- native file serialization schema data.

### Scope Choice

ROM-7 selects Option B:

DocumentFragment contains semantic content plus scoped transferable resources.

Option A, blocks only, is too small because dataset-backed views and future citations/media need resource closure.

Option C, generic graph/subdocument, is too broad because current Scholar does not need arbitrary graph traversal, plugin nodes, or persistence object graphs.

### Content Forms

The fragment must be able to represent:

- zero or more document blocks;
- inline-only content when document-level inline structure must be preserved;
- dataset-only resource transfer;
- whole FigureBlock;
- whole DiagramBlock as one document block;
- future multi-block structured selections.

The fragment should not force all nested editor transfers into document-level content. Math subranges, table cell text, and diagram sub-elements may remain specialized editor-local transfer models until they cross document structural boundaries.

## 6. DocumentTransfer Contract

`DocumentTransfer` is the destination-aware materialization service.

It should:

- accept a `DocumentFragment` and destination context;
- inspect destination document IDs/resources;
- allocate replacement IDs where required;
- remap stable IDs according to accepted policy;
- rewrite CrossReferences according to accepted policy;
- materialize required resources according to accepted policy;
- report warnings/degraded states;
- return destination-ready semantic content and resource additions.

It must not:

- mutate `Document`;
- mutate `DocumentFragment`;
- apply editor history;
- decide final caret/selection;
- talk to the OS clipboard;
- serialize native files;
- import Markdown;
- depend on Minecraft/client/rendering/UI packages.

The editor layer should apply a `TransferResult` as one semantic edit through existing editor mutation/history pathways.

## 7. Content / Resources Separation

The contract should separate:

- fragment content;
- fragment resources;
- fragment identity/reference metadata.

Recommended conceptual shape:

```java
record DocumentFragment(
    FragmentContent content,
    TransferResourceSet resources,
    FragmentIdentityIndex identities
) {}
```

This does not mean implementation must overbuild metadata. It means resources must have a defined structural location instead of being hidden in ad hoc payloads.

Why resources belong inside `DocumentFragment` rather than a completely separate clipboard side object:

- copying and carrying semantic content should preserve the selected closure as one immutable value;
- dataset-backed views need a place for datasets without turning transfer into persistence;
- future citations/media can be scoped similarly;
- clipboard carriers can store one rich payload without knowing content internals.

What ROM-8 still decides:

- which dependencies are included;
- minimal versus complete closure rules;
- resource deduplication rules;
- behavior for copied views whose resources are absent by policy;
- whether missing resources produce rejection or degraded transfer.

## 8. Identity / Remap Boundary

DocumentFragment preserves source IDs exactly. Fragment construction must not remap IDs.

DocumentTransfer owns destination-aware identity handling.

Rules:

- source IDs remain immutable inside the fragment;
- one fragment may be materialized multiple times;
- repeated materialization into the same destination must produce fresh destination IDs when required by collision policy;
- the fragment itself must not change after materialization;
- remapping must be represented as first-class transfer output;
- generated IDs must be captured in the semantic document state that the editor commits;
- redo must restore the committed snapshot and not rerun remapping.

Recommended conceptual result:

```java
record TransferResult(
    MaterializedContent content,
    TransferResourceAdditions resources,
    IdentityRemap identityRemap,
    List<TransferDiagnostic> diagnostics
) {}
```

`IdentityRemap` should be domain-aware. A plain `Map<String, String>` is too weak because the same textual ID may be valid in multiple namespaces. M25B should prefer a typed key such as:

```java
record StableIdentityKey(StableIdentityKind kind, String id) {}
record IdentityRemap(Map<StableIdentityKey, StableIdentityKey> mappings) {}
```

ROM-9 owns exact namespace definitions and allocation algorithms.

## 9. CrossReference Boundary

DocumentFragment stores raw source `CrossReference(kind, targetId)` nodes.

DocumentTransfer owns reference rewriting because rewriting requires destination identity remaps.

The contract must support this future rule:

- if a reference and its target both exist in the transferred closure, transfer can rewrite the reference to the remapped destination target;
- if a reference leaves its target behind, transfer must not invent or guess a new target.

Reference rewriting requires:

- a source identity index for fragment targets;
- an identity remap created during materialization;
- knowledge of which CrossReference target kinds map to which stable identity namespaces.

Unresolved references remain valid semantic content when existing validation policy allows them. Transfer should report degraded references as diagnostics/warnings, not exceptions, unless the selected future policy says the target is mandatory.

No heuristic retargeting is allowed.

ROM-9 owns exact CrossReference rewrite rules.

## 10. Same-Document / Cross-Document Semantics

The same abstraction must work for:

- same-document copy/paste;
- repeated paste;
- future cross-document transfer;
- future document import insertion;
- cut/move workflows.

Same-document transfer is not a special fragment type. It is a destination context where collisions are likely.

Cross-document transfer is also not a special fragment type. It is a destination context with a different document ID/resource set.

Cut is not a distributed move. It remains:

1. construct fragment or editor-local payload;
2. successfully export to clipboard/carrier;
3. remove source content through an editor transaction;
4. paste later like an ordinary copied fragment.

No source-document deletion metadata belongs in `DocumentFragment`.

## 11. Nested-Editor Boundary

Do not force every editor-local transfer through `DocumentFragment`.

Recommended rule:

Use `DocumentFragment` when semantic content crosses document structural boundaries or may carry document-level resources/IDs. Use specialized editor-local transfer models for intra-node editing.

Implications:

- whole EquationBlock transfer can be document fragment content;
- math subrange transfer may remain `MathClipboardPayload(MathSequence)` or an editor-local math fragment unless it is intentionally embedded in a document fragment carrier;
- table cell text remains local plain text unless table-cell structured transfer becomes a requirement;
- whole TableBlock transfer becomes document fragment content;
- diagram sub-element transfer remains diagram-editor-specific until a real requirement exists;
- whole DiagramBlock transfer becomes document fragment content.

This avoids turning `DocumentFragment` into a universal AST for every nested editor.

## 12. Figure / Diagram Boundary

### Figure

Figure remains `FigureBlock(id, content, caption)`.

ROM-7 does not generalize Figure into arbitrary `BlockNode` containment. Transfer should treat whole FigureBlock according to existing semantic ownership:

- outer figure ID is a document-level identity;
- caption is inline semantic content and may contain CrossReferences;
- content remains limited to `PlotBlock` or `DiagramBlock`;
- dependencies of contained content are considered part of the figure's transfer closure only through scoped content/resource rules defined later.

ROM-10 owns exact Figure composite closure behavior.

### Diagram

Whole DiagramBlock is a document-level block.

Diagram internals remain diagram-owned:

- diagram element IDs are diagram-local;
- port IDs are element-local;
- diagram connections and mechanical internal references remain inside the diagram semantic unit;
- document transfer should not flatten diagram internals into document-global identity machinery.

Document transfer may treat DiagramBlock as one semantic content node with internally valid data. It should not rewrite diagram-local IDs unless a future diagram-specific transfer requirement appears.

## 13. Editor / History Boundary

M24 editor contracts remain intact:

- `EditorState` is the editor snapshot;
- `EditorSession` coordinates semantic mutation and history today;
- one logical edit is one transaction;
- transient UI state is not history;
- redo restores snapshots and does not rerun ID generation.

DocumentTransfer must not become an editor mutation engine.

Preferred flow:

1. editor/selection layer identifies source selection;
2. fragment factory builds `DocumentFragment`;
3. clipboard/interchange exports fragment plus plain text;
4. paste reads carrier and obtains fragment;
5. `DocumentTransfer` materializes fragment for destination context;
6. `DocumentEditor`/`EditorSession` applies materialized content and resource additions as one edit;
7. editor computes resulting selection/caret;
8. history stores the resulting `EditorState` snapshot.

Selection after paste remains editor-owned because it depends on insertion shape and UX policy, not fragment semantics.

## 14. Failure / Degraded Result Model

Transfer should distinguish:

- success exact: materialized without warnings;
- success degraded: materialized but contains unresolved external references or missing optional resources according to policy;
- rejected source: selection cannot produce a valid fragment;
- rejected destination: fragment cannot be inserted at destination selection;
- unsupported target: destination editor scope cannot consume the fragment;
- resource conflict remapped: conflict resolved by destination policy;
- resource conflict rejected: conflict cannot be resolved by policy;
- identity conflict remapped: ID collision resolved;
- identity conflict rejected: required identity cannot be remapped safely;
- invalid fragment: malformed or internally inconsistent transfer value.

Ordinary degraded semantic states should not use exceptions. Exceptions remain appropriate for programmer errors, nulls, impossible constructor states, or corrupted internal invariants.

Validation and transfer remain separate:

- transfer diagnostics explain what happened during materialization;
- `DocumentValidator` validates the resulting document at trust boundaries;
- validation warnings do not automatically imply transfer failure.

## 15. Immutability / Determinism Invariants

M25 should preserve these invariants:

- `DocumentFragment` is immutable.
- Fragment construction does not mutate the source `Document`.
- Transfer does not mutate the fragment.
- Transfer output is immutable value data.
- Materializing the same fragment into the same destination state with the same identity allocator is deterministic.
- Materializing the same fragment repeatedly into an updated destination may generate new IDs as collisions accumulate.
- Generated destination IDs are stored in committed semantic state.
- Redo restores committed state rather than regenerating transfer output.
- Derived state is never stored in the fragment.
- Transient editor/client state is never stored in the fragment.
- Clipboard text matching is transport-level state, not domain identity.

These invariants match the current immutable document model and M24 history contracts.

## 16. Comparison

### A. Specialized Payloads Only

Pros:

- minimal immediate code change;
- preserves current working behavior;
- avoids designing a new boundary.

Cons:

- keeps transfer policy scattered in `EditorSession`;
- adds more branches for every new content family;
- cannot coherently handle dataset/resource closure;
- cannot coherently remap CrossReferences when target and reference travel together;
- repeats identity allocation by namespace.

Result: not sufficient for M25.

### B. Scoped DocumentFragment + DocumentTransfer

Pros:

- directly addresses ROM-6 pressure points;
- preserves semantic AST as source of truth;
- supports resource closure without persistence leakage;
- provides a destination-aware place for identity and reference remapping;
- keeps editor/history concerns in editor layer;
- does not require plugin architecture or arbitrary graph traversal.

Cons:

- requires careful migration from current payloads;
- needs explicit policies in ROM-8/9/10 before implementation;
- can still become too broad if nested editors are forced into it.

Result: recommended.

### C. Universal Generic Content Graph

Pros:

- could theoretically represent any future object graph.

Cons:

- overfits future plugin/persistence concerns;
- risks becoming a second document model;
- obscures current semantic types;
- would pull diagram internals, datasets, figures, citations, media, and persistence into one abstraction before requirements exist;
- conflicts with the instruction to avoid generic graph frameworks.

Result: reject for M25.

### What Option B Does Not Model

The scoped fragment/transfer design does not model:

- arbitrary third-party blocks;
- generic graph traversal;
- native file serialization;
- Markdown syntax;
- layout/rendering geometry;
- editor commands;
- history journals;
- OS clipboard MIME formats;
- diagram sub-element editing;
- figure as arbitrary block containment.

## 17. Future-Feature Pressure Test

- Image/media blocks: fit as future block content plus media resources; resource closure can extend without graph engine.
- Citations/bibliography: likely need a bibliography resource subsystem; fragment resources can carry citation resources later.
- Footnotes/endnotes: likely document blocks/inline references plus resources or derived numbering; contract can carry references without deciding model now.
- Equation labels: fit current stable ID/reference/remap pattern.
- Hyperlinks: likely inline semantic nodes; no resource closure unless linked assets become managed resources.
- Code blocks: fit as future BlockNode; may not need resources.
- Quantities/units: probably separate semantic math/text subsystem; fragment can carry resulting nodes but should not define quantity semantics.
- Imported/external datasets: need dataset resource policy; fragment resource set is the right structural place.
- Future richer Figure content: can be admitted by Figure/content policy later without making Figure arbitrary now.
- Templates: separate persistence/creation subsystem; fragments may help insert template parts but should not be template format.
- Cross-document copy: supported conceptually by destination-aware transfer.
- Native persistence: separate M26 concern; may reuse semantic traversal ideas but must not reuse clipboard fragment as file format.
- Public mod integration: should wait until transfer and persistence are stable; fragment contract should not promise plugin support now.

No pressure test forces a universal graph engine.

## 18. Pseudocode-Level Proposed Contracts

These are design sketches only.

```java
package dev.rgcb.scholar.transfer;

public record DocumentFragment(
    FragmentContent content,
    TransferResourceSet resources,
    FragmentIdentityIndex identities
) {}
```

Responsibility: immutable source-side semantic transfer value.

Ownership layer: core transfer package.

Forbidden fields: editor selection, layout, rendering, clipboard text, destination IDs.

```java
public sealed interface FragmentContent {
    record Blocks(List<BlockNode> blocks) implements FragmentContent {}
    record Inline(InlineContent content) implements FragmentContent {}
    record DatasetResource(ScientificDataset dataset) implements FragmentContent {}
}
```

Responsibility: distinguish document-level transfer shapes without generic graph modeling.

Note: sealed interface is pseudocode. The implementation can use ordinary interfaces if project style prefers avoiding sealed hierarchies.

```java
public record TransferResourceSet(
    List<ScientificDataset> datasets
) {}
```

Responsibility: scoped transferable resources. Future resource families may be added deliberately.

Forbidden fields: resolved table snapshots, layout caches, file-format metadata.

```java
public record FragmentIdentityIndex(
    Set<StableIdentityKey> providedTargets,
    Set<StableIdentityKey> referencedTargets
) {}
```

Responsibility: identify source-side identities needed for remap decisions.

ROM-9 owns exact key kinds.

```java
public record TransferContext(
    Document destination,
    TransferTarget target
) {}
```

Responsibility: destination state needed for remap/materialization.

`TransferTarget` should describe insertion/replacement capability at a semantic level only. It should not store client UI state.

```java
public interface DocumentTransfer {
    TransferResult materialize(DocumentFragment fragment, TransferContext context);
}
```

Responsibility: destination-aware materialization.

```java
public record TransferResult(
    MaterializedContent content,
    TransferResourceAdditions resources,
    IdentityRemap identityRemap,
    List<TransferDiagnostic> diagnostics
) {}
```

Responsibility: immutable output the editor can apply atomically.

```java
public record IdentityRemap(
    Map<StableIdentityKey, StableIdentityKey> mappings
) {}
```

Responsibility: first-class record of source-to-destination identity changes.

```java
public record TransferDiagnostic(
    TransferDiagnosticSeverity severity,
    TransferDiagnosticCode code,
    String message
) {}
```

Responsibility: communicate transfer warnings/rejections without conflating them with validation diagnostics.

The smallest coherent implementation may start with fewer fields if ROM-8/9 accept narrower policies, but the boundary should remain.

## 19. Package / Layer Recommendation

Future implementation should live in a pure core package:

`dev.rgcb.scholar.transfer`

Allowed dependencies:

- `dev.rgcb.scholar.document`
- `dev.rgcb.scholar.data`
- `dev.rgcb.scholar.math` only if document-level math block/fragment support requires it
- validation result primitives only if kept pure and not editor-specific

Avoid dependencies on:

- `client`
- Minecraft/NeoForge/Mojang packages
- render/layout packages
- screen/widgets
- OS clipboard adapter
- native persistence format packages

Dependency direction:

- editor may depend on transfer;
- clipboard sidecar may carry a fragment;
- transfer must not depend on editor/session/history/client.

## 20. Migration Classification Of Current Payloads

| Current payload | Classification | Notes |
|---|---|---|
| `DocumentBlockClipboardPayload` | likely replaced by `DocumentFragment` | one-block fragment content with identity index |
| `InlineContentClipboardPayload` | likely adapted to `DocumentFragment` | document-level inline transfer; still may only be produced when native inline structure matters |
| `MathClipboardPayload` | should remain editor-local/specialized initially | math subrange transfer is inside equation editor; whole EquationBlock can be fragment content |
| `TableClipboardPayload` | likely replaced by `DocumentFragment` | whole TableBlock plus possible dataset resources in ROM-8 |
| `PlotClipboardPayload` | likely replaced by `DocumentFragment` | whole PlotBlock plus possible dataset resources |
| `DiagramClipboardPayload` | likely replaced by `DocumentFragment` for whole block | diagram internals remain opaque and diagram-owned |
| `FigureClipboardPayload` | likely replaced by `DocumentFragment` | whole FigureBlock plus nested dependency policy in ROM-10 |
| `DatasetClipboardPayload` | likely adapted to `DocumentFragment` | dataset-only fragment/resource transfer |
| plain text clipboard paths | remain external fallback | not replaced by fragment; generated from fragment where structured copy exists |
| `MathPlainTextImporter` path | remain editor-local/external import | not document fragment unless pasting at document boundary |
| table cell plain text path | remain editor-local | not document fragment until structured cell transfer is required |

M25B should not necessarily delete every old class at once. It may bridge current payloads to fragments during migration if that keeps behavior stable.

## 21. Do-Not-Patch Rules

- Do not add another independent document-level clipboard payload without deciding whether it is a `DocumentFragment`.
- Do not add another independent stable-ID remapper when `DocumentTransfer` can own the namespace.
- Do not treat serialization as transfer.
- Do not make clipboard payload shape the native persistence format.
- Do not copy derived labels, numbering, layout, or resolved dataset-backed view snapshots as authoritative fragment state.
- Do not heuristically repair or retarget CrossReferences.
- Do not elevate diagram-local identities into document-global identity.
- Do not generalize Figure into arbitrary block containment during M25.
- Do not put transfer policy back into `EditorSession` as new type branches.
- Do not build capability registries or plugin frameworks before there is an accepted extension requirement.
- Do not make degraded validation warnings into exceptions unless the transfer policy explicitly rejects that case.
- Do not let OS clipboard text matching define semantic identity.

## 22. Decisions Deferred To ROM-8

ROM-8 must decide resource/dependency closure:

- whether dataset-backed tables copy required datasets;
- whether dataset-backed plots copy required datasets;
- whether closure is minimal, optional, or policy-driven;
- how resources are deduplicated in the destination;
- whether copied resources are remapped, shared, or rejected on conflicts;
- how missing resources are diagnosed;
- future shape for media/citation resources without implementing them.

## 23. Decisions Deferred To ROM-9

ROM-9 must decide identity/reference policy:

- exact stable identity namespaces;
- ID allocation algorithms;
- source-to-destination remap representation;
- CrossReference rewrite rules;
- behavior when reference targets are outside the fragment;
- behavior when both reference and target are inside the fragment;
- replacement semantics for preserving IDs;
- diagnostics for degraded references.

## 24. Decisions Deferred To ROM-10

ROM-10 must decide composite content rules:

- FigureBlock transfer closure;
- how figure captions, contained plot/diagram content, and dependencies interact;
- whether contained diagram IDs remain fully opaque;
- how diagram-local mechanical references are treated;
- whether whole DiagramBlock remains the only diagram transfer unit for document-level transfer;
- what happens if future figure content types are introduced.

## 25. Open Questions

1. Should implementation use `DocumentTransfer` or `DocumentTransferService` as the concrete class/interface name?
2. Should `FragmentContent` include `Math` content in M25B, or should math stay entirely editor-local until document-level use appears?
3. Should inline-only fragments preserve all text marks even when no CrossReference is present, replacing today's lossy plain text path for selected formatted inline content?
4. Should `TransferTarget` describe replacement/insertion shape, or should the editor pass only destination `Document` and perform insertion separately after materialization?
5. Should transfer diagnostics reuse `DocumentDiagnosticSeverity` names or define transfer-specific severities?

None of these questions block accepting the high-level boundary.

## 26. Recommendation Whether ROM-7 Can Close

ROM-7 can close after review if the team accepts:

- `DocumentFragment` as immutable semantic source-side transfer content;
- `DocumentTransfer` as destination-aware materialization/remap service;
- resources as a scoped part of fragment structure, with exact closure deferred to ROM-8;
- identity/remap and CrossReference rewriting deferred to ROM-9;
- Figure/Diagram composite policy deferred to ROM-10;
- nested editor transfer remaining specialized unless document-level transfer needs it.

This contract gives ROM-8 through ROM-10 a stable design frame without starting implementation or overgeneralizing the architecture.

