# M25B - Semantic Document Transfer Implementation

Status: complete and manually accepted. Earlier automated implementation checkpoints
predated the final Minecraft QA and M26.
Authority: [M25A_TRANSFER_ARCHITECTURE_CONTRACT.md](M25A_TRANSFER_ARCHITECTURE_CONTRACT.md).
A/B/C were implementation checkpoints, not additional roadmap milestones; their full reports
remain in Git history.

## Final Pipeline

```text
Copy/Cut action -> EditorFragmentExtractionAdapter -> FragmentExtractor
  -> DocumentFragment + SourceTransferMetadata -> source plain-text export
  -> successful OS clipboard write -> DocumentFragmentClipboardPayload sidecar

Paste action -> matching carrier -> TransferInserter receiving-context staging
  -> TransferPlanner -> immutable TransferPlan -> TransferMaterializer
  -> immutable MaterializedTransfer -> TransferInserter candidate staging
  -> DocumentValidator + EditorSelectionValidator + stale-state preflight
  -> one EditorHistory.applyEdit -> derived relayout/resolution
```

Keyboard shortcuts, top menu and context menu retain existing BuiltInEditorActions
Copy/Cut/Paste. There is no context-only or shortcut-only transfer implementation.

## Production Inventory

| Component | Layer / responsibility |
|---|---|
| TransferMaterializer | Core: apply frozen plan identity/resource/reference decisions, no allocation or mutation. |
| MaterializedTransfer | Core: immutable content, resource additions, original plan. Package-owned construction. |
| MaterializationResult | Core: successful prepared values or fatal diagnostics without partial output. |
| TransferInserter | Editor: receiving scope, approved survivor facts, pure candidate construction and validation. |
| TransferInsertionResult | Editor: staged EditResult or rejection diagnostics without an applicable partial edit. |
| DocumentFragmentClipboardPayload | Clipboard: runtime fragment/metadata carrier, not persistence or nested-editor content. |
| FragmentPlainTextExporter | Clipboard: source display export using existing resolvers/serializers. |
| LegacyFragmentClipboardAdapter | Clipboard: old carrier shape adapters only; delegates all transfer policy. |
| EditorSession | Authoritative action routing, live-owner token, final single history application. |
| DocumentEditor | Existing structural helpers extended for ordered roots and exact InlineSegments. |
| ClipboardCutResult | Optional captured source EditorState guards against reentrant/stale Cut commit. |
| DevelopmentDocument | Added practical M25 inline, root, dataset and Figure reference fixtures. |

FragmentIdentityIndex.blockIdentity is shared with editor survivor preparation rather than
introducing another editor identity switch. These public Java types remain internal evolving
contracts, not a versioned public mod API. No new ADR or architecture policy is introduced.

## Materialization And Identity

The materializer requires the exact fragment captured by its plan. It performs no allocator
calls, destination collision decisions, resolver calls, source normalization or serialization.
Optional SECTION/EQUATION/TABLE identities and required FIGURE identity follow the shared
typed remap. Dataset additions follow their planned DATASET identities. Absent optional IDs
stay absent. Source fragment and destination snapshot are never mutated.

Plan decisions remain the only destination identity authority: free IDs preserve;
collisions use deterministic first-free suffixes with pre-reservation. Repeated cross-document
paste plans against the changed destination and creates independent resources/content.
No random IDs, source paste counter, content deduplication or hidden provenance cache.

## Dataset Resources And Bindings

TRANSFER_AS_NEW produces one complete ScientificDataset addition with its planned ID.
Columns, ordered rows, values, metadata and owner-local IDs remain exact. Every transferred
Table binding and every Plot series binding, including Figure-owned Plot, uses the same map.
No generated plot points or resolved table rows are substituted for canonical definitions.

REUSE_EXISTING_SAME_DOCUMENT produces no addition. It requires token continuity and exact
applicable original dataset witness; included snapshots must also match that witness instance.
Edited/rebuilt resources do not qualify and available snapshots transfer independently.
Resource-primary imports never reuse, even in the same document. They add only datasets,
retain a valid selection and never synthesize consuming blocks.

Missing snapshot and missing applicable reuse proof rejects before mutation. No dataset
lookup by equal ID/name/schema, substitute columns, detached binding or plain-text retry.
Missing-column warnings remain valid degraded bindings; they are not repaired or flattened.

## CrossReferences

The materializer consumes one planned occurrence decision at each fragment-local path:

- REMAP_INTERNAL: active CrossReference targets the shared planned identity.
- PRESERVE_EXTERNAL_SAME_DOCUMENT: original active reference remains unchanged.
- DEGRADE_TO_TEXT: unmarked Text contains captured source export or `[Missing reference]`.

Paragraph/Heading content, stored authored/bound Table cells and Figure captions use the
same rules. Surrounding Text instances, segmentation, marks and node order are preserved.
No sentinel ID, active broken link, shadow resolver, guessed target or persistent repair
metadata is inserted. Original degraded target identity remains in operational diagnostics.
Materialization carries planning warnings into successful insertion results; EditorSession
exposes the last semantic-paste diagnostics outside semantic history, without new UI.

## Composite And Nested Boundaries

Figure remains one wrapper with remapped outer ID, transformed caption and supported
Plot/Diagram content. Contained content never escapes into another document root.
Diagram values are retained exactly, including complete generic/electrical/mechanical graphs,
local endpoints/ports/IDs, labels, constraints, annotations, dimensions, positions and workspace
ratio. Candidate validation still checks the complete result. No subgraph merge is added.

EquationBlock remaps only its optional identity and retains the exact MathExpression instance.
Math-local MathClipboardPayload and MathExpressionEditor remain specialized; stacked fractions,
roots/scripts/groups and token selection do not become document blocks. Recognized document
carriers cannot enter math by silently importing their fallback. Native math cannot enter body
through text fallback either.

TOC remains the exact semantic marker. Destination structure/numbering/entries are recomputed;
no source headings, labels or layout snapshots are transferred.

## Structural Staging And Atomic Commit

DocumentEditor now stages whole ordered roots using its existing caret/range/point helpers:
prefix/suffix preservation, heading ownership and fallback behavior remain M24D semantics.
There are no intermediate inserts, repeated user commands or helper history transactions.
One authoring fallback is appended for the final complete root collection where required.

Existing matching Table/Plot/Diagram/Figure single-owner replacement remains supported.
Other scientific single-root payloads do not replace unrelated atomic families. Heading,
Equation and TOC retain insertion-after-selected-block behavior. Supplied multi-root carriers
remain supported without adding mixed-object UI selection.

InlineSegments replaces the normalized editable range in one operation. The destination
left block retains its style/identity; later segments create ID-less Paragraphs; the final
segment receives the surviving suffix. Empty intermediate segments are real boundaries.
Caret lands after the final inserted segment before suffix. One empty segment at a caret
is a no-op even if represented by zero-length Text runs. No adjacent-node normalization.
Whole Paragraph collections end in a valid prose caret; selectable last roots use BlockSelection.

Receiving-context preparation determines exactly removed identities before planning.
It uses an identity-free structural marker to stage the existing receiving shape, not a second
structural editor. Plan survivor facts are rechecked against captured selection during insertion.

Resources and staged content are composed into one complete immutable candidate first.
DocumentValidator must report zero ERROR; EditorSelectionValidator must accept final selection.
Stale EditorState/destination snapshot or invalid candidate returns Failure without mutation,
resource addition, selection change, history entry or retry. Warnings may remain. Reservations
never become committed allocator state. Only a changed validated candidate reaches history.

## Clipboard And Actions

Copy reads existing selections through the adapter, closes scoped source resources, captures
runtime proof/export metadata and produces one fragment carrier. No remap or history entry.
Whole Heading BlockSelection transfers level/SECTION identity; selecting all its text transfers
inline content only. Multi-block text preserves selected boundaries and marks internally.

Cut prepares that same export and an existing M24D deletion, validates it, writes OS text,
installs sidecar, then applies exactly one deletion. Extraction/export/write failure cannot
delete. A captured source-state companion additionally rejects source changes during clipboard
callbacks. View deletion does not remove dependency resources. Clipboard is not undone.

Paste with matching rich carrier always attempts the new semantic pipeline. Rejection never
falls back to OS text. Missing/stale sidecar uses existing supported external text behavior:
body newlines normalize to spaces, math uses its current importer with linear slash semantics.
Table-local textual paste is the sole retained explicit automatic conversion and consumes OS
text under existing single-cell rules. Caption/plot/diagram local clipboard guards remain.

OS fallback uses normal source reference/table resolvers: inline selected boundaries export LF;
whole authored/bound tables export canonical TSV separators with resolved display cells;
tabs/newlines inside cells still reject export. Plot/Diagram/Figure summaries and captions use
existing serializers. Whole Equation retains its existing empty document export. Dataset uses
existing TSV export. No Markdown clipboard language, custom MIME, hidden payload, LaTeX default
or persistence schema is added.

Rich state remains process-local, externally useful text remains on OS clipboard. Exact text
matching still has the accepted identical external recopy ambiguity; it proves no identity.

## Runtime Lifetime And History

Each EditorSession creates one runtime token for its live semantic-document owner lifetime.
It survives immutable edits, selection changes, undo/redo and resizing that retains the session.
Discarding/reopening the owner creates a new token; process sidecar may survive but continuity
is unproven. Direct setCurrent callers must retain the same semantic document ownership rather
than aliasing unrelated documents through one session. No token enters Document/history/files.

Copy: zero entries, redo preserved. Cut: one deletion transaction. Paste: one transaction
including datasets/content/reference transformations. Failed/rejected/no-op paste: zero entries,
redo unchanged. Derived recomputation adds none. Undo restores exact previous directional
selection/document; Redo restores the committed snapshot with identical IDs/resources/bindings
and selection. No planner, allocator, clipboard read or witness reevaluation occurs on Redo.

## Legacy Migration

Removed EditorSession per-family semantic copy/paste/remap branches and their unused serializer
fields. New document Copy installs only DocumentFragmentClipboardPayload. Old DocumentBlock,
InlineContent, Table, Plot, Diagram, Figure and Dataset carrier classes remain compatibility
shapes: LegacyFragmentClipboardAdapter converts them into the common pipeline with unknown
provenance, never independent allocation/reuse/reference rules. Old bound-view carriers without
resources/proof now reject safely rather than borrowing destination resources by textual ID.

Math-local carrier, plain-text convenience APIs, cell String/TSV exporters and external math
import stay intentionally specialized. Dataset creation and Figure wrap operations retain
ordinary non-transfer allocation helpers using the shared StableIdAllocator; these are not
competing clipboard remappers. No production serialization or persistent origin is introduced.

## Automated Acceptance

- TransferMaterializerTest: eight focused cases for identity/reference/binding rewrites,
  exact math/Diagram/TOC preservation, shared additions, reuse, degradation and mismatch failure.
- SemanticTransferIntegrationTest: 37 executions, including twelve whole-root families,
  actual shared actions/sidecar paths, directional/boundary inline ranges, marked Unicode,
  ownership/proof negatives, fresh/reused resources, source export/write failure, reentrant
  Cut, stale staging, zero-ERROR candidate validation, no-op/redo and fixture verification.
- Golden mixed source includes Heading, CrossReferences, Equation, authored/bound Tables,
  shared Plot and Figure Plot, Figure Diagram, generic/electrical/mechanical graphs and TOC.
  Deliberate destination identity collisions remap deterministically; references/resources stay
  coherent, external links degrade safely; undo-all/redo-all restore exact valid states.
- Fixed-seed integration replay: 50 full extraction/planning/materialization/insertion/history
  scenarios; validation, bindings and exact Undo/Redo remain correct. Prior seeded planning and
  extraction suites remain enabled.
- Existing migration tests retain behavioral assertions; only carrier-type expectations and
  the formerly unsafe no-provenance raw-reference expectation were updated. No test was disabled.
- `gradlew.bat test`: BUILD SUCCESSFUL; 1329 tests, zero failures/errors/skips (45 added since C).
- `gradlew.bat build`: BUILD SUCCESSFUL.
- `git diff --check`: passes, with repository LF/CRLF conversion notices only.
- TransferBoundaryTest and source scan: no platform/editor/clipboard/layout/render/client imports
  in planning/materialization core. Editor staging owns selection/history boundaries.

## Manual Minecraft QA Procedure

Manual acceptance remains pending. Client startup is not QA. Use `/scholar_dev_editor`:

1. In M25 Semantic Transfer, select the marked inline paragraph and Copy/Paste into the
   destination paragraph. Check segmentation visually, marks, Unicode and active same-session reference.
2. Select across the two boundary paragraphs; paste a partial range and a boundary-only range.
   Check separate blocks, left style ownership and caret before surviving suffix. Undo/Redo once.
3. Select Whole Heading as a block through existing object/context targeting; Copy/Paste.
   Check a new numbered heading. Repeat with only its text selected: no new Heading should appear.
4. Copy the M25 Equation as a whole block and paste at prose. Enter it and separately Copy/Paste
   a stacked fraction locally. Try that native math payload at body: Paste should reject.
5. Copy/Paste the authored Table. Check cells/header/marks and one-step Undo/Redo.
6. Copy/Paste the M25 bound Table and bound Plot in the same editor; edit the Projectile Test
   dataset via Data actions. Original/pasted views should share the unchanged resource identity.
7. Copy/Paste the generic Diagram and the electrical Figure. Check complete connectivity,
   local topology, labels and authored workspace shape; do not expect sub-element clipboard.
8. Copy the Figure captioned Self/external as a whole owner and paste at prose. It remains
   a Figure; Self targets its newly remapped Figure; external stays active within this session.
9. Copy that original Figure again. Close the editor and reopen `/scholar_dev_editor`, then
   Paste without recopying. Self stays active to the copied Figure; external becomes ordinary
   label Text instead of binding the reopened unrelated same-ID Figure. Dataset imports independently.
10. Copy/Paste TOC. Add/remove a heading and check both TOCs derive destination structure.
11. Repeat representative Copy/Cut/Paste through Edit menu, Ctrl+C/X/V and right-click actions.
    Use block selection rather than entering a nested editor when testing whole objects.
12. Cut a Figure, Undo once and Redo once. Check exact wrapper/caption/content restored and
    clipboard still available. Paste resources plus views, then one Undo and Redo: all move together.
13. Paste text copied from an external app into body and a table cell; body multiline text stays
    normalized, cell textual conversion stays local. Check no rich invalid payload is silently flattened.
14. Check mouse/focus/selection geometry, relayout, scroll and commands at two GUI scales/window sizes.
    Inspect logs for exceptions; report any runtime defect separately from automated acceptance.

Multi-root mixed-object UI selection was not added. Golden/shared-resource collection transfer is
automatically covered through supplied semantic carrier fixtures; manual sharing uses existing
single-owner copy and same-session resource reuse. No clipboard failure popup or diagnostics UI.

## Limitations And Next Gate

No remaining automated M25B blocker. Manual QA/acceptance is still outstanding. Large datasets
and eager action-enablement planning remain later measurement concerns, not optimization work here.
Stronger identity continuity after target edits, richer exports, diagnostics/repair UX, external
resources, schemas and public API compatibility remain deferred. M26 must design its native
format independently; runtime transfer is not its serialization schema. M26 is not started.

All prior uncommitted M25A/A/B/C documentation and code are preserved; no commit/push performed.
