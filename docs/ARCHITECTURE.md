# Scholar Architecture

This is the current implementation map, not a public API promise. The semantic document is the source of truth; Minecraft UI, layout, caches, and file storage are separate consumers or owners.

## Boundaries

| Layer | Current responsibility |
| --- | --- |
| Application/workspace | `/scholar` Home, document identity and metadata, repository, open workspaces, save/discard lifecycle. Application document IDs are not AST IDs. |
| Semantic document | Immutable `Document`, blocks, inline content, math AST, document settings, and document-owned datasets. Authored references use stable IDs. |
| Validation | `DocumentValidator` checks structural validity and reports degraded semantic dependencies without mutating content. |
| Editing/history | `EditorSession` and `DocumentEditor` apply semantic changes; `EditorState` snapshots give one logical edit one history transaction. Focus, selection movement, menus, scroll, and zoom are transient. |
| Scientific semantics | M31 quantities and unit registry, M32 authored variable/computation expressions, and M33 authored dataset analysis definitions and plot links. Computed values, statistics, and fit samples are derived. |
| Transfer | M25 fragment extraction, resource closure, destination-aware remapping, and atomic editor insertion. Clipboard carriers and plain-text fallbacks are not persistence files. |
| Persistence | M26 Scholar JSON codec and safe local storage. Current writes are V2 with supported V1 reads. It persists authored semantics and IDs, not runtime proofs, history, caches, layout, or view state. |
| Layout/typesetting | Pure-Java layout derives logical pages, columns, line/atomic-block geometry and hit targets from semantic content and text measurement. M30 page settings are semantic; assigned pages are derived. |
| Client/rendering | Minecraft screens, ribbon, dialogs, font resources, `DocumentViewTransform`, and renderers display laid-out content. Zoom/scroll/fit and the status bar belong here, not in `Document`. |

Core model, editor, scientific and transfer logic avoid Minecraft types where practical. Client screens may call into those layers; the core should not depend on screens, widgets, or `GuiGraphics`. `DocumentViewTransform` maps laid-out document coordinates to GUI coordinates for rendering, clipping, caret/selection, and hit testing. GUI scale and zoom do not alter the semantic document.

## Identity And Derived State

Referenceable headings, equations, tables, figures, datasets, variables, and analyses have scoped stable identities. Dataset columns are dataset-local; diagram internals stay within diagram ownership. Copy/paste can allocate destination identities and rewrite carried links, whereas save/load preserves authored IDs. Names, visual labels, and positions are not substitutes for identity.

Cross-reference labels, TOC/outline, dataset-backed views, electrical nets, computed results, analysis values, fitted curves, page assignment, word count, and render geometry are recomputed from authored state. An unresolved dependency is reported as degraded content rather than silently rebound by a matching display name. Undo/redo restores semantic snapshots; it does not rerun ID allocation.

## Product Boundary

`/scholar` is the production entry. The M34 Readability Sample is a deliberate production template. Development fixtures live in tests, not as production commands. Scholar core is not an experiment addon, and an arbitrary third-party node/plugin contract has not yet been published. M35 may define a public addon API only after review; current implementation packages are not stable extension points.

## Detailed Contracts

- [Project Specification](PROJECT_SPEC.md) and [Roadmap](ROADMAP.md)
- [M25 transfer contract](M25A_TRANSFER_ARCHITECTURE_CONTRACT.md) and [M25 implementation](M25B_TRANSFER_IMPLEMENTATION.md)
- [M26 persistence](M26_DOCUMENT_PERSISTENCE.md), [M30 typesetting](M30_SCIENTIFIC_DOCUMENT_TYPESETTING.md)
- [M32 computation](M32_SCIENTIFIC_COMPUTATION.md), [M33 analysis](M33_SCIENTIFIC_DATA_ANALYSIS.md), [M34 rendering](M34_HIGH_FIDELITY_RENDERING.md)

Historical milestone reports retain their checkpoint status; the [Milestone History](MILESTONE_HISTORY.md) records current acceptance.
