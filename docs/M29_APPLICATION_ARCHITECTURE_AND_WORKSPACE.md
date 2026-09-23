# M29 - Scholar Application Architecture & Workspace

## Status

Complete and manually accepted. This report's implementation notes were written before M30.

## Architecture Audit

Before M29, `/scholar_dev_editor` directly constructed a `DocumentWorkspace` around a fixture and a
hard-coded application directory. `ScholarEditorScreen` owned New/Open/Save/Save As behavior and
`ScholarFileDialog` listed raw persistence keys. `EditorSession` correctly owned semantic editing,
history, selection, and its runtime transfer token, but there was no production application entry
point, document-library identity, library metadata, Home screen, rename, or multi-document owner.

The M26 `DocumentStorage`/`FileDocumentStorage` boundary and V1 codec were already suitable as the
semantic persistence layer. M29 therefore wraps them instead of changing or duplicating the format.
The existing process clipboard remains global while each opened workspace creates a fresh
`EditorSession`, preserving M25 same-document proof and cross-document transfer behavior.

## Application Architecture

The new pure-Java `dev.rgcb.scholar.application` layer has four responsibilities:

1. `ScholarApplication` coordinates library-level create, list, and open operations.
2. `ScholarDocumentRepository` maps application IDs to persisted documents and metadata.
3. `ApplicationDocumentWorkspace` owns one open document's descriptor, clean snapshot, and fresh
   editor session.
4. `DocumentPreview` derives lightweight Home-card text without persisting layout or UI state.

`EditorSession` remains the semantic editing authority. `DocumentWorkspace` remains as a legacy
development-tool adapter so `/scholar_dev_editor` keeps its regression-fixture workflow. Both expose
the narrow `EditorDocumentWorkspace` lifecycle surface consumed by the editor shell.

## Identity And Storage

`ScholarDocumentId` is an immutable application-library identity. It is unrelated to heading,
equation, table, figure, dataset, or diagram IDs inside the scientific AST. Save As creates a new
application ID while preserving the semantic document exactly. Rename changes only library metadata.

Production storage is:

```text
<game directory>/scholar/
  workspace.json
  documents/
    <ScholarDocumentId>.scholar.json
```

`documents/` continues to use M26 `FileDocumentStorage`, including its bounded codec, path checks,
temporary writes, and atomic replacement policy. `workspace.json` is a versioned, atomically replaced
application index containing names, timestamps, and disposable previews. A missing, malformed, or
partially malformed index is ignored; valid document files are discovered and exposed with recovered
descriptors. The index is never required to decode a Scholar document.

Existing M26 files in `scholar/documents` remain discoverable. User-visible names are metadata and
never become paths. New documents use `Untitled`, `Untitled 2`, and so on.

## Home And Lifecycle

The production `/scholar` command always registers and opens `ScholarHomeScreen`. Home provides a
Minecraft-native header, New Document, a paged card list, lightweight excerpts, modified timestamps,
an empty state, explicit errors, and Open. It does not instantiate or fully lay out every document per
frame. Opaque Scholar application screens render their widgets directly after the Scholar surface;
they do not invoke Minecraft's blurred menu-background pass after drawing their own content.

The production lifecycle is:

```text
/scholar -> Home -> create/open -> fresh workspace/session -> edit -> save/rename/save-as -> Home
```

New immediately persists a neutral one-paragraph document and opens a clean session with a valid
caret. Open loads through M26, validates through the existing editor entry path, and creates fresh
selection, history, focus, and runtime transfer identity. Save updates the same application identity.
Save As creates a distinct one. Rename leaves the AST untouched. Close and Open/Home return to Home.

Dirty navigation uses the existing Save / Discard / Cancel modal. Save, Save As, and Rename produce
zero editor-history entries. A failed semantic save leaves the workspace baseline and history
unchanged. If semantic data was saved but disposable metadata could not be updated, the operation
succeeds with a warning because the document remains recoverable by scanning storage.

## Preview Strategy

M29 intentionally uses a deterministic text preview rather than GPU screenshots. The preview takes
the first non-empty heading as a title, the first non-empty paragraph text as an excerpt, and a block
count. It does not resolve CrossReferences or copy derived labels. Preview corruption cannot affect
document loading, and successful create/save/rename regenerates it.

## Application Shell

The production shell now has a compact application header showing Scholar, document name, and
Saved/Unsaved state. Production command tabs are described by `ScholarShellModel` as File, Home,
Insert, Data, Figure, Diagram, and View. The menu renderer consumes those descriptors, highlights the
open tab, handles narrow-width scrolling, and the existing contextual toolbar remains the command
area below it.

All semantic commands are the existing `EditorAction` instances. Menus, toolbar, context menus, and
shortcuts therefore retain one command authority. File lifecycle actions are thin shell adapters over
the workspace/application boundary and do not mutate semantic content directly. The development
fixture retains its prior detailed menus to avoid weakening regression access.

`ScholarEditorScreen` still coordinates the mature viewport and modal hosts. M29 extracts only the
new application repository/workspace, Home, header, and shell description responsibilities; it does
not rewrite layout, rendering, nested editors, or `EditorSession`.

## Compatibility

- **M25:** Every open creates a new `EditorSession` and runtime token. Copy in A then paste in B is
  cross-document transfer even when internal stable IDs coincide. Clipboard semantics are unchanged.
- **M26:** Semantic files remain Scholar JSON V1 and use the existing codec/storage. Workspace JSON
  is a separate application index, not a second document format.
- **M27:** History snapshots, transaction rules, focus ownership, and input dispatch are unchanged.
- **M28:** `/scholar_dev_editor` and stress profiles remain development-gated. `/scholar` contains no
  fixture content. Existing visual language and document readability are preserved.

## Automated Coverage

Focused coverage includes repository restart, preview derivation, corrupt-index recovery, M26 file
discovery, rename, Save As identity separation, invalid names, identity collision exhaustion, neutral
and collision-safe New, fresh session/history/runtime identity, dirty baselines, failed save behavior,
shell layout/model, legacy workspace regression, and an integrated create/edit/save/reopen/rename/
Save As/cross-document-paste scenario.

The final validation commands are recorded in the implementation report for this task. Manual QA is
still required before M29 is accepted.

Final automated baseline: 1492 tests, zero failures/errors/skips; `gradlew build`,
`TransferBoundaryTest`, `PersistenceBoundaryTest`, `ApplicationBoundaryTest`, and
`git diff --check` pass. Line-ending notices from the preserved Windows working tree are warnings,
not whitespace errors.

## Manual QA

Run `./gradlew runClient`, join a world, then:

1. Run `/scholar`; verify Home opens and contains no QA fixture.
2. With an empty library, verify the empty state and create a document.
3. Verify the neutral blank document, valid caret, document name, and Saved state.
4. Type prose and confirm Unsaved appears.
5. Close; exercise Cancel, Discard, and Save separately.
6. Return Home and verify the document card, excerpt, and modified metadata.
7. Open it; verify content restoration and fresh undo history.
8. Rename it; verify Home reflects the new name and semantic references remain intact.
9. Save As; verify two independent cards exist and edits do not leak between them.
10. Copy structured content in document A, open B, paste, and verify M25 cross-document behavior.
11. Verify Save adds no undo step and undo/redo still affects only semantic edits.
12. Resize/narrow the screen and change GUI scale; verify tabs, header, Home cards, and viewport remain usable.
13. In a development runtime run `/scholar_dev_editor`; verify the M28 fixture remains available.
14. In a production runtime verify development commands are absent while `/scholar` remains available.

## Deferred

Delete, autosave, cloud/network storage, image previews, workspace search/folders, persistent caret or
history, concurrent I/O, metadata migration beyond index V1, and public repository/plugin APIs remain
out of scope. M30 must not begin until manual M29 acceptance.
