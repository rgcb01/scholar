# Post-M24 Architecture Audit

Status: planning document only. No production implementation was performed.

Companion documents:

- `docs/POST_M24_ARCHITECTURE_MAP.md`
- `docs/POST_M24_TECHNICAL_DEBT.md`
- `docs/POST_M24_ROADMAP_PROPOSAL.md`

## A. Executive Assessment

Scholar is in a strong post-M24 position. It has a real semantic document model, a disciplined Minecraft boundary, a broad editor foundation, deterministic history, meaningful validation, and enough scientific block families to reveal architectural patterns.

The project does not need a rewrite.

The project also should not continue by simply adding feature milestones. The main risk is now architectural multiplication: every new block type or scientific object currently tends to require edits in validation, selection validation, layout, rendering, clipboard, paste/remap, action enablement, tests, and development fixtures.

The next foundation should therefore not be another feature. It should be document transfer and identity closure: a precise answer for how selected document content, resources, references, datasets, figures, and stable IDs move together during clipboard, paste, import, and eventually persistence.

## B. Current Architecture Map

The current architecture is layered as:

1. Semantic document model: `document`, `math`, `table`, `plot`, `diagram`, `data`, `figure`.
2. Derived semantic structures: document structure, cross-reference resolution, dataset-backed views.
3. Validation: `DocumentValidator`, `EditorSelectionValidator`.
4. Editor state: `EditorState` plus selection variants.
5. Mutation/session layer: `DocumentEditor`, `EditorSession`.
6. History: `EditorHistory` snapshots.
7. Clipboard/interchange: `ScholarClipboardService` plus specialized payloads.
8. Nested editors: equation, table, plot, diagram, figure caption.
9. Layout: `DocumentLayoutEngine`.
10. Rendering: Minecraft renderer under `client`.
11. Client integration: screens, commands, Minecraft clipboard bridge.
12. Transient UI state: menus, hover/focus, toolbar state.
13. Persistence/import/export: Markdown subset only; no native persistence yet.
14. Extension/API boundary: not explicit yet.

See `docs/POST_M24_ARCHITECTURE_MAP.md` for the fuller dependency map.

## C. What Is Architecturally Strong

- Core packages remain independent of Minecraft and NeoForge.
- The immutable AST is still the canonical document state.
- Derived structures are not stored as mutable document truth.
- Stable IDs now exist for the major referenceable structures.
- Selection and caret validation are centralized enough to prevent many invalid editor states.
- History has a clear user-facing transaction rule.
- Nested editors are represented semantically rather than through screen-only state.
- Dataset resource/view separation is a strong foundation.
- Tests exercise a broad behavioral surface rather than only happy-path rendering.

## D. What Is Architecturally Weak

- `EditorSession` is becoming a large coordinator for unrelated block-specific behavior.
- Clipboard payloads are specialized by block family and do not yet model a selected subdocument plus resource closure.
- Stable ID remapping is scattered across paste/import-like paths.
- Figure composition is currently restricted to plot/diagram content, which will not scale cleanly to images/media.
- The public Java surface is much larger than the supported API surface.
- Layout and validation are intentionally centralized around current built-in block types.
- Native persistence has not yet defined schema, metadata, versioning, migration, or degraded-load behavior.

## E. Hidden Coupling / Patchwork Risks

Current pressure points:

- Repeated block type checks across validation, layout, selection validation, clipboard, actions, rendering, and paste.
- Several implicit concepts exist without explicit contracts: editable block, atomic block, nested editor host, dataset-backed view, referenceable object, transferable object.
- Copy/paste currently knows too much about each block family.
- Resource and ID closure is not yet a first-class operation.
- Markdown remains partial but could be accidentally treated as a save format.

This is manageable today. It becomes expensive if images, citations, footnotes, code blocks, imported datasets, and third-party integrations are added before the transfer/persistence foundations are clarified.

## F. Document Model Assessment

The document model is prepared for the next phase if native persistence is not frozen yet.

Current strengths:

- `Document -> BlockNode -> InlineNode` remains understandable.
- Datasets are document-level resources, not embedded table implementation details.
- Cross references use stable target IDs and are resolved as derived state.
- Equations, tables, plots, diagrams, and figures are semantic blocks.
- TOC/outline data is derived rather than stored.

Concerns before persistence:

- There is no document metadata/version hook.
- Figure content is hard-coded to plot/diagram.
- Stable ID strategy exists but remapping policy is not centralized.
- Plot blocks do not have the same direct stable block identity shape as every referenceable structure.
- There is no accepted policy for unknown future nodes or degraded loading.
- There is no native schema boundary separate from Java record structure.

Recommendation: harden transfer/identity and persistence schema before adding more semantic object families.

## G. Editor Architecture Assessment

`DocumentEditor` is still a reasonable mutation boundary. `EditorSession` is the riskier component.

Keep:

- `EditorState` as the semantic editor snapshot.
- `DocumentEditor` as the core mutation API.
- One logical edit equals one history transaction.
- Nested selection variants as the editor mode model.
- Client input dispatch through shared actions.

Harden:

- Extract transfer/remap/clipboard concerns out of `EditorSession`.
- Make block-specific action support more discoverable.
- Keep selection validation centralized.

Do not introduce a broad third-party block capability system yet. The need is real, but the next step should be narrower: transfer capabilities for built-in document content.

## H. Clipboard/Fragment Assessment

Current clipboard architecture is good enough for current features but is the clearest near-term growth risk.

What it currently represents:

- OS clipboard text fallback.
- Process-local structured sidecars.
- Specialized payloads for inline content, document blocks, math, tables, plots, diagrams, figures, and datasets.

What is lossless:

- Many in-process Scholar-to-Scholar payloads preserve structure.

What is not yet consistently modeled:

- Multi-block selected content plus all required resources.
- Cross-reference target preservation/remapping.
- Dataset-backed views plus the dataset resources they depend on.
- Figure content and nested references as a closure.

Do we need a general `DocumentFragment` abstraction?

Yes, but narrowly. Scholar needs a document transfer abstraction, not a universal graph engine. The problem it should solve is: "given selected content, what semantic nodes, resources, IDs, references, and remap rules move together?"

Option A, continue specialized payloads:

- Lowest short-term cost.
- High future patchwork risk.

Option B, introduce `DocumentFragment`/`DocumentTransfer`:

- Recommended.
- Solves clipboard/import/paste identity closure.
- Must stay scoped to transfer, not become a second document model.

Option C, generic graph/subdocument transfer model:

- Too heavy now.
- Keep future-compatible, but do not build it yet.

## I. Persistence Readiness

Scholar is not ready to freeze a native document format.

Must decide before native persistence:

- Document metadata and schema version location.
- Stable type tags independent of Java class names.
- ID namespace and remapping rules.
- Resource ownership and closure rules.
- Embedded versus linked resource strategy.
- Unknown-node/degraded-load behavior.
- Migration policy.
- Validation-on-load policy.
- Deterministic serialization order.
- Security constraints: no Java object serialization or reflection-loaded clipboard/file payloads.
- Markdown relationship: Markdown remains import/export, not native persistence.

Native persistence should be preceded by a design milestone and then a minimal implementation milestone.

## J. Layout/Rendering Readiness

Current layout/rendering is adequate for the existing editor and viewer.

Near-term safe:

- Text, headings, equations, tables, plots, diagrams, figures, captions, scrolling.
- Minecraft boundary remains clean.

M27 hardening candidates:

- Long tables.
- Large documents.
- Resizing and scroll visibility.
- Figure/media sizing.
- High-DPI/GUI scale checks.
- Selection geometry after layout changes.

Long-term only:

- Pagination.
- Print/export layout.
- Accessibility model.
- Alternate renderers.
- Incremental layout.

Do not redesign layout now. Harden it after transfer and persistence contracts are clearer.

## K. Validation/Diagnostics/Recovery Assessment

These must remain separate:

- Structural validation: is the document internally well-formed?
- Semantic validation: do references, datasets, bindings, and diagram connections resolve?
- User-facing diagnostics: what should a player see and where?
- Repair: what safe transformation can fix a known issue?
- Migration: how older schemas become newer schemas.
- Degraded loading: how invalid or unknown persisted content remains inspectable.
- Editor invariants: states the editor must never create.

Validation UI should not be next. It becomes more useful after native persistence/import creates real degraded states. Until then, improve validation contracts only when transfer/persistence needs them.

## L. API/Extensibility Assessment

Scholar is not ready for a stable public mod API.

Current state:

- Other mods could technically construct public records/classes.
- That is accidental reachability, not a supported API.
- There is no stability annotation, `api` package, or compatibility policy.

Recommendation:

- Do not support arbitrary third-party block types yet.
- First public API should be narrower: create documents with built-in nodes, open/render Scholar screens, and possibly provide validated builders.
- Extension nodes should wait until persistence, validation, layout, clipboard, and API versioning all have explicit contracts.

## M. Performance Risks

Safe for now:

- Immutable document reconstruction.
- Full editor-state snapshots.
- Full relayout after edits.
- Full validation in tests and selected workflows.

Measure in M29:

- History memory with large datasets/media.
- Layout time for long documents.
- Repeated ID scans.
- TOC/reference/dataset resolver cost.
- Diagram rendering and hit-testing complexity.
- Large table editing.

Likely design change before very large documents:

- Snapshot history for media-heavy documents.
- Layout invalidation if documents become large enough.
- Dataset handling if large datasets are embedded directly.

Do not optimize now.

## N. Test Architecture Assessment

The test suite is a major strength, but count alone is not proof of architectural safety.

Strengths:

- Broad behavior coverage across document, editor, math, tables, plots, diagrams, validation, layout, and client-side logic.
- Good regression coverage for recent editor foundation work.
- Tests protect invariants and user workflows.

Risks:

- Some tests necessarily encode current concrete block/selection structures.
- Large all-feature fixtures can become hard to diagnose.
- Coverage may be high in editor behavior but weaker in future transfer/persistence edge cases.
- Property/invariant tests are still underused relative to the amount of immutable structural data.

Recommendation:

- Keep the suite.
- Add focused fixture builders.
- Add invariant/property-style tests for transfer, ID remapping, validation, and persistence.
- Avoid rewriting tests for style alone.

## O. Prioritized Technical Debt Register

See `docs/POST_M24_TECHNICAL_DEBT.md`.

Highest-priority items:

1. Native persistence schema is not ready to freeze.
2. `EditorSession` is becoming the central block-specific coordinator.
3. Clipboard payloads lack resource/identity closure.
4. Stable ID remapping is scattered.
5. Accidental public API surface is too broad.

## P. Architecture Freeze Map

FREEZE NOW:

- Canonical AST principle.
- Minecraft-independent core boundary.
- Markdown is not native persistence.
- Derived TOC/reference/dataset views are not primary mutable document state.
- One logical edit equals one history transaction.

STABLE BUT EVOLVABLE:

- Document/block/inline model.
- Text marks.
- Math AST.
- Selection model.
- EditorState.
- Snapshot history.
- Validation as a separate subsystem.
- Dataset resource/view separation.

NOT YET STABLE:

- Clipboard/fragment model.
- Stable ID remapping implementation.
- Native persistence.
- Public API boundary.
- Figure/media composition.
- Layout contracts for media/large documents.

EXPERIMENTAL:

- Future third-party node extension.
- Cross-process rich clipboard.
- Advanced export/pagination.
- Large-document optimization.

NEEDS REDESIGN BEFORE PUBLIC API:

- Public package exposure.
- Block capability/registration story.
- Persistence schema and migration.
- Transfer/resource closure.

## Q. Future-Feature Pressure Test

- Images: requires new subsystem or Figure/media generalization.
- Citations/bibliography: requires new subsystem and reference/resource policy.
- Footnotes/endnotes: requires small model extension plus layout/rendering support.
- Code blocks: requires small block extension, but syntax highlighting may be new subsystem.
- Scientific units/quantities: requires new semantic subsystem if more than plain text.
- Reusable variables: requires new document semantic/resource subsystem.
- Appendices: fits current heading/structure model with small extension.
- Document metadata: requires foundational model/persistence addition.
- Hyperlinks: requires small inline extension plus security/UX policy.
- Equation labels: fits current ID/reference pattern with small extension.
- Advanced tables: requires table subsystem growth.
- Imported datasets: requires dataset persistence/resource policy.
- External media: requires resource subsystem.
- Templates: requires persistence/import model.
- Export: requires layout/persistence/schema maturity.
- Other-mod integration: exposes current API boundary problem.

## R. Critique Of Current Tentative M25-M30 Roadmap

The tentative roadmap is broadly sensible but incorrectly underspecified at M25/M26.

- M25 should not be "fragment" as a generic abstraction exercise. It should be document transfer and identity closure.
- M26 should be split into architecture and implementation.
- M28 diagnostics should likely follow native persistence, not precede it.
- M29 performance should remain measurement-led.
- M30 UX stabilization should happen after transfer/persistence/layout contracts settle.

See `docs/POST_M24_ROADMAP_PROPOSAL.md`.

## S. Proposed Revised Roadmap

1. M25A - Document Transfer And Identity Closure Architecture.
2. M25B - Document Transfer And Clipboard Unification Implementation.
3. M26A - Native Persistence Architecture.
4. M26B - Native Persistence Minimal Implementation.
5. M27 - Diagnostics And Recovery Foundation.
6. M28 - Layout And Rendering Hardening.
7. M29 - Performance Instrumentation And Large Document Budgets.
8. M30 - Editor UX Stabilization.
9. M31 - Public API Boundary Design.
10. M32 - Scientific Media Expansion.

## T. Architectural Gates

Before native persistence:

- Transfer/resource closure accepted.
- Stable ID namespace and remap policy accepted.
- Document metadata/version policy accepted.
- Unknown-node/degraded-load policy accepted.
- Deterministic serialization policy accepted.

Before public API:

- Native schema at least draft-stable.
- API/internal package boundary defined.
- Supported construction/open/render workflows documented.
- Third-party block support accepted or explicitly deferred.

Before advanced scientific features:

- New feature has a resource, persistence, validation, layout, clipboard, and reference story.
- It does not add another independent stable-ID remapper.

Before performance optimization:

- Realistic fixtures exist.
- Current bottleneck is measured.
- User-facing budget is defined.

Before first stable Scholar document format:

- Migration story exists.
- Unknown/degraded content story exists.
- Security/trust model exists.
- Markdown relationship is explicitly non-native.

## U. Do Not Patch Around This Rules

Stop and reassess architecture if a new change requires:

- Adding the same block-type branch in six unrelated layers.
- Storing derived TOC/reference/dataset state in `Document`.
- Putting client UI state into semantic model classes.
- Bypassing `EditorSession`/`DocumentEditor` for semantic edits.
- Creating another local stable-ID remapper.
- Duplicating clipboard serialization for a new block family.
- Adding Minecraft/NeoForge imports to core packages.
- Special-casing undo/redo outside normal history.
- Creating a second reference-resolution mechanism.
- Treating Markdown as native persistence.
- Copying dataset-backed views without deciding resource closure.
- Adding a new referenceable object without ID namespace/remap rules.
- Adding a public API promise around a class that still participates in internal refactors.

## V. Top 5 Technical Risks

1. Block-specific logic multiplying across editor, validation, layout, clipboard, and rendering.
2. Native persistence freezing accidental Java implementation details.
3. Clipboard/import/paste losing resources, references, or stable-ID relationships.
4. Accidental public API commitments before model and persistence contracts stabilize.
5. Large document/media performance limits hidden by current small fixtures.

## W. Recommended Immediate Next Milestone

M25A - Document Transfer And Identity Closure Architecture.

This should be design-only. It should decide whether the implementation type is called `DocumentFragment`, `DocumentTransfer`, or something else, and it should define exact resource closure and ID remapping behavior before code changes.

## X. What Explicitly Should NOT Be Done Next

- Do not start native persistence implementation.
- Do not add new scientific block families.
- Do not create a public mod API.
- Do not rewrite layout/rendering.
- Do not build validation UI before persistence/import creates real degraded states.
- Do not implement a generic graph transfer engine unless M25A proves it is necessary.
- Do not modify `ROADMAP.md` until this proposal is reviewed.

## Y. Open Architectural Questions Requiring Human Decision

1. Should the next transfer abstraction be named `DocumentFragment`, `DocumentTransfer`, or another term?
2. When copying a dataset-backed view, should the dataset resource be copied by default, linked by ID when available, or prompt later?
3. Should cross references inside copied content preserve links to original targets outside the fragment, degrade to text, or copy target closure when possible?
4. Should `FigureBlock` remain restricted to visual scientific content, or become the general captioned media wrapper?
5. Should future native persistence be JSON-like, NBT-like, or a custom format?
6. Should third-party block types be supported in the first public API, or should Scholar expose only built-in document construction/rendering?
7. What compatibility promise should pre-1.0 Scholar make for saved files?

## Z. Explicit Statement

No production implementation was performed. This audit created planning documents only and did not start M25, implement features, fix code, create ADRs, or modify the official roadmap.

