# Post-M24 Architecture Map

Status: planning document only. No production implementation decisions are accepted by this file.

## Layer Map

Scholar currently has a clear core/client split:

1. Semantic document model
   - Packages: `document`, `math`, `table`, `plot`, `diagram`, `data`, `figure`.
   - Owns immutable AST/value data: blocks, inline content, equations, tables, plots, diagrams, figures, datasets, and cross references.
   - Does not import Minecraft or NeoForge classes.

2. Derived semantic structures
   - Packages/classes: `document.DocumentStructureResolver`, `document.ResolvedCrossReference`, dataset resolvers in `data`.
   - Computes headings, outline/table-of-contents entries, references, and dataset-backed views from the canonical document.
   - These are derived views, not stored as primary document truth.

3. Validation
   - Package: `validation`.
   - `DocumentValidator` validates structural and semantic consistency.
   - `EditorSelectionValidator` validates current editor selection against the current document.

4. Editor state
   - Package: `editor`.
   - `EditorState` owns the current `Document`, current `EditorSelection`, and transient typing mark state.
   - Selection variants include text, block, equation editing, table editing, plot editing, diagram editing, and figure caption editing.

5. Mutation/session layer
   - `DocumentEditor` is the core mutation boundary for many structural and text operations.
   - `EditorSession` coordinates actions, nested editing, clipboard, history, relayout, and view-facing state.
   - This layer is pure Java today, but it is increasingly responsible for block-specific orchestration.

6. History
   - `EditorHistory` stores deterministic snapshots of `EditorState`.
   - Current invariant: one logical edit creates one logical history transaction.
   - Transient UI state is intentionally outside semantic history.

7. Clipboard/interchange
   - Package: `clipboard` plus block-specific payloads in editor-related packages.
   - Uses OS plain text plus process-local structured sidecars.
   - Clipboard payloads are currently specialized by content type.

8. Nested editors
   - Equation, table, plot, diagram, and figure-caption editing are scoped nested modes under `EditorSelection`.
   - Nested editors generally preserve the parent document as canonical and mutate through `EditorSession`/`DocumentEditor` paths.

9. Layout
   - Package: `layout`.
   - `DocumentLayoutEngine` converts semantic document data plus derived resolvers into laid-out document elements.
   - Layout is scroll-oriented and currently centralized by block type.

10. Rendering
    - Core layout is renderer-independent.
    - Minecraft rendering lives under `client`.
    - `MinecraftDocumentRenderer` renders the laid-out representation, while typography and math rendering remain separated by role as much as current features allow.

11. Client/Minecraft integration
    - Package: `client` and top-level `Scholar`.
    - Owns screens, input bridge, key handling, Minecraft clipboard bridge, and commands.
    - Minecraft imports are currently limited to client/top-level integration.

12. Transient UI state
    - Menus, hover/focus state, toolbar state, context menus, and shell behavior live in client/session state rather than in `Document`.

13. Persistence/import/export
    - Markdown parser/serializer exists for a restricted interchange subset.
    - There is no native Scholar persistence format yet.
    - Markdown is not the canonical model and cannot represent full Scholar documents losslessly.

14. Extension/API boundary
    - There is no explicit stable public API boundary yet.
    - Many records/classes are public because Java packages need access today, not because they are ready as long-term mod API.

## Dependency Direction

Intended direction:

`document/math/table/plot/diagram/data/figure`
-> `validation` and derived resolvers
-> `editor`
-> `layout`
-> `client`

Markdown and clipboard sit beside the editor as interchange systems:

`document` <-> `markdown`

`editor` <-> `clipboard`

`client` -> `clipboard`

## Strong Boundaries

- Core packages do not import Minecraft, NeoForge, or Mojang rendering classes.
- The semantic document model remains the source of truth.
- Layout is derived from the semantic model.
- Cross references, document structure, and dataset-backed views are derived rather than stored as mutable document state.
- History is deterministic snapshot state, not UI event replay.

## Convention-Based Coupling

The following boundaries work today mostly by discipline and tests rather than explicit architecture:

- Block type behavior is repeated across validation, selection validation, layout, clipboard, action enablement, paste/remap, and rendering.
- "Editable block", "atomic block", "dataset-backed view", and "nested editor host" are implicit concepts rather than explicit capabilities.
- Stable ID remapping is handled in multiple editor/session paths.
- Clipboard transfer closure is payload-specific and does not yet have a shared document-subgraph abstraction.
- Figure content is currently a special composition of plot or diagram blocks rather than a general media/container contract.
- Markdown interchange is intentionally partial, but nothing in the type system prevents future code from treating it as persistence.

## Unwanted Reverse Dependencies

No major reverse dependency was found from core document/editor packages into Minecraft/client classes.

The more important issue is horizontal coupling inside the pure-Java core/editor layer: new block types require coordinated edits across many pure-Java systems. That is safer than client coupling, but it is the main scaling risk after M24.

