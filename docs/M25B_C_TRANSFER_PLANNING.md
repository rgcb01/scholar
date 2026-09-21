# M25B-C - Resource Closure + Identity Remap Planning

Status: COMPLETE. M25B remains IN PROGRESS. M25B-D is not started.

Authority: [M25A_TRANSFER_ARCHITECTURE_CONTRACT.md](M25A_TRANSFER_ARCHITECTURE_CONTRACT.md).
This checkpoint implements planning, not materialization, insertion or clipboard migration.

## Architecture and production inventory

`TransferPlanner.plan(DocumentFragment, TransferContext)` returns either
`PlanningResult.Success(TransferPlan)` or `PlanningResult.Failure(diagnostics)`.
Failure has no partial plan. Expected incompatibilities are returned as diagnostics;
invalid immutable value construction may still throw argument exceptions.

New transfer values: `IdentityRemapPlan`, `ResourceTransferPlan`,
`ReferenceDispositionPlan`, `TransferPlan`, `PlanningResult`.
New planner: `TransferPlanner`. New shared pure helper: `document.StableIdAllocator`.
Modified types: `TransferContext` and the four collision-allocation loops in
`EditorSession`. EditorSession does not call the planner or change mutation paths.

The transfer package depends on semantic document/dataset models and validation,
not editor, platform, clipboard, layout or rendering packages.

## Destination context and validation foundation

Context captures the original immutable destination, optional runtime token,
source metadata, semantic scope and caller-approved removed root indices.
Scopes are DOCUMENT_CONTENT (default), BLOCKS, INLINE and RESOURCES.
Specific scopes reject other content families. They are not insertion positions.
Removed indices exclude those roots from identity inventory and witness applicability;
the planner never computes a selection or removes a root itself. Their validity is a
caller responsibility; concrete insertion-shape preflight remains M25B-D work.
No dataset removal facts are currently modeled.

Inventory enumerates surviving SECTION, EQUATION, TABLE, FIGURE identities and
destination DATASET identities. Ambiguous surviving identities reject planning.
Source-owned content/resources are checked with DocumentValidator using an isolated
projection, not a mutated destination candidate. Structural errors reject; source
semantic warnings are reported. Ordinary missing CrossReferences are handled by
reference decisions instead of duplicated validation warnings.

TransferPlan construction checks scope, complete identity/resource/reference decisions,
survivor collisions, exact resource snapshot/proof and reference occurrence consistency.
Later insertion must validate the complete destination candidate, check stale context,
and commit atomically. A plan is not permission to bypass those checks.

## Runtime proof and witnesses

Same-document relation requires two present tokens that are the identical opaque
RuntimeDocumentToken instance. Document equality, IDs, hashes and content do not prove it.
Caller token continuity must not alias unrelated live documents.

Individual applicability additionally requires the destination semantic value to be
the exact original immutable witness instance (`==`). Equal reconstructed values,
deleted targets and roots excluded by approved replacement facts do not qualify.
No persistent origin, hidden reuse cache or heuristic matching is introduced.

## Identity allocation and reservation

Source fragment IDs and AST remain unchanged. Unused source identities are preserved;
occupied identities remap within their namespace. The allocator returns the unchanged
base if available, otherwise the first free `base-2`, `base-3`, ... suffix.
All unused traveling source IDs are reserved before collision allocation. Example:
destination FIGURE a; fragment FIGURE a and a-2 => a maps to a-3, a-2 stays a-2.
SECTION x and TABLE x do not collide.

Collision allocation follows semantic root order, carried resource list order and
first discovery order for absent dependencies. Hash iteration never controls allocation.
Legacy trimming/fallback/figure sanitization remains at existing call sites; extracting
only the suffix loop does not change legacy clipboard allocation behavior. New transfer
preserves source bases exactly, as required by M25A.

IdentityRemapPlan maps typed source keys to source/destination/disposition decisions:
PRESERVED, REUSED_EXISTING, REMAPPED_NEW. Unchanged decisions are explicit. Destination
keys are unique. Reservations are operation-local, not stored in a global allocator.

## Resources

ResourceTransferPlan has one decision per required source dataset, retaining its original
immutable semantic value, planned destination DATASET key and disposition:
TRANSFER_AS_NEW or REUSE_EXISTING_SAME_DOCUMENT. Consumers later use the shared identity map.
Bindings are not rewritten now. Unrelated carried resources reject planning.

Cross-document/unproven resources always transfer as new: unused IDs may be preserved,
colliding IDs allocate fresh suffixes. Equal values/schema/names never cause reuse.
Repeated cross-document materialization reserves against the new destination state;
it does not reuse earlier imports.

The authoritative M25A contract explicitly permits included dependency snapshots to
reuse the original live dataset when token and exact witness applicability hold.
If a snapshot is included, it must be the same witness instance as well. External
dependencies without snapshots also reuse with exact applicable proof. Missing snapshot
plus missing/inapplicable proof produces MISSING_REQUIRED_RESOURCE failure.
If an included witness became stale, the carried snapshot transfers as new instead.
Resource-primary direct import never reuses, even with same-document proof.

Multiple Table/Plot/Figure consumers of one dataset share one disposition and destination
identity. Whole dataset values are retained; column and row IDs remain dataset-local.

## References and occurrence paths

ReferenceDispositionPlan is an ordered immutable list, one decision per source occurrence:
REMAP_INTERNAL, PRESERVE_EXTERNAL_SAME_DOCUMENT or DEGRADE_TO_TEXT.
Internal targets traveling in fragment content use their shared identity remap.
External targets preserve only with explicit token continuity and exact applicable witness.
All other external occurrences plan non-reference Text using captured source export, or
`[Missing reference]` if unavailable. No sentinel IDs or active wrong-target links are created.

Each degraded occurrence emits WARNING EXTERNAL_REFERENCE_DEGRADED containing original
typed target identity and path. Absent source export additionally emits SOURCE_DISPLAY_UNAVAILABLE.
Degradation is successful planning, not failure. Diagnostics remain operational, not AST.
The normal resolver will operate after later materialization; no second resolver exists.

Paths are fragment-local structural strings, not persistent node IDs or global offsets:
`roots[i].content.nodes[j]`, `roots[i].caption.nodes[j]`,
`roots[i].rows[r].cells[c].nodes[j]`, `segments[i].nodes[j]`.
Repeated occurrences of the same immutable reference object still receive distinct paths.
This grammar must be consumed consistently by later materialization.

## Composite and local ownership

Figure remains a FigureBlock with supported Plot/Diagram content. Outer figure identity
and caption references are planned; contained Plot dataset dependencies use the resource plan.
Diagram graph IDs, ports, connectivity, annotations, positions and workspace metadata remain
inside the unchanged semantic value. No global Plot/Diagram namespace is invented.
TOC does not import headings or copy derived numbering. Layout, focus and selection are absent.

## Invariants and deferred work

- Source fragment, source IDs, destination and witnesses are never mutated.
- Collections are defensive immutable copies; built-in AST values are reused safely.
- Same inputs and destination context produce equal semantic decisions.
- New destination state may require different suffixes on repeated planning.
- Every traveling/reused identity and every reference occurrence receives a complete decision.
- Fatal failure contains diagnostics only, with no partial plan or committed reservations.
- No content-equality deduplication, provenance persistence or reference heuristics.
- No destination insertion, dataset creation, AST rewrite, clipboard/action integration or history entry.
- Later materialization must capture generated IDs in semantic state; redo restores snapshots, not allocation.

M25B-D can consume these decisions but must implement insertion shapes, stale-state preflight,
AST materialization and candidate validation before atomic editor application. No new policy
or ADR is introduced here; accepted M25A rules remain authoritative.

## Validation and tests

Added TransferPlannerTest and TransferPlanModelTest: 41 test executions, including a
75-case fixed-seed collision replay within one test. Coverage includes all four content
identity namespaces, independent namespaces, pre-reservation, token absence/mismatch,
exact/stale/deleted witnesses, shared datasets, direct import, repeated cross-document
resources, internal/external references, table/caption/inline occurrence paths, composites,
immutable values, complete plans, diagnostics and success/failure input preservation.
Existing clipboard/history/editor regressions and TransferBoundaryTest remain green.

Validation: `gradlew.bat test` BUILD SUCCESSFUL; 1284 tests, zero failures/errors/skips.
`gradlew.bat build` BUILD SUCCESSFUL. `git diff --check` passes.
No runtime behavior change or manual Minecraft acceptance is claimed by this pure-core slice.
M25A/B-A/B-B working changes are preserved. No commit/push. No remaining C blocker.
