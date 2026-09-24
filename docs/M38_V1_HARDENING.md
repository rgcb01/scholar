# M38 - V1 Product Hardening

Status: COMPLETE / MANUALLY ACCEPTED. M38 stress QA passed in Minecraft; the manual-pending wording below is retained as historical test procedure. M37 was manually accepted, committed as `e02189b`, merged as `681548597acb98cd8c0866da5b0fae4885147298`, and `main` matched `origin/main` with a clean tree before this branch was created.

## Feature freeze and method

Until 1.0.0, M38 admits only verified bug, performance, compatibility, recovery, accessibility, lifecycle and release-blocker fixes. No new document families, formats, scientific computations, addon capabilities or major workflows. The audit follows M24-M37 contracts and uses the M27 `DevelopmentStressDocument` profiles and existing M27 stress tests, not another fixture framework. Headless tests use synthetic text metrics and cannot establish native GPU frame time or real Minecraft visual quality.

## Test matrix

| Area | Automated evidence / result | Remaining manual or limitation |
| --- | --- | --- |
| Large document | M27 and `V1ProductHardeningTest`: 1000 blocks, 305 derived pages, complete reflow and three exact file cycles | Scroll, edit, select and export PDF of a comparably large authored document in `/scholar` |
| Data-heavy | 4000 rows, three exact file cycles; M38 evaluates descriptive analysis and round-trips CSV for a 2000-row dataset from that profile, comparing every cell | Plot/analysis responsiveness and CSV import/export of a substantial real file |
| Diagram-heavy | 180 blocks, 40 pages, deterministic reflow and three file cycles; diagram-specific regression suites | Zoom, hit-test, labels, copy/paste and PDF visual inspection in game |
| Mixed scientific | 240 blocks, 67 pages, exact cycles and PDF export/reopen page count. M38 also appends M32 variable/computed content and M33 analysis to the M27 mixed fixture, then checks validation, file round-trip and paginated layout | Authoring across M31-M33 blocks and multi-column visual pass |
| Long editing/history | M27 `FoundationV2HardeningTest` runs 100 mixed transactions and seeded 60-step edits. M38 runs 500 separately grouped typing edits, verifies history remains capped at 100, then undoes/redoes all retained snapshots exactly | Extended in-game session and dirty-close protection |
| Save/load/recovery | M26 storage tests inject write/move failures; M38 cycles four stress profiles three times and proves corrupt open preserves dirty state and old bytes | Filesystem denial or power-loss behavior cannot be proved by JVM tests |
| Schema compatibility | `DocumentJsonCodecTest` independently authored V1 and V2 decoding, unknown version and malformed semantic cases; M30-M34 serialization tests | Preserve representative real older user documents for manual reopen |
| Transfer/clipboard | M25/M27 fragment, repeated-paste, resource and ID tests; `EditorActionTest` verifies failed clipboard write does not apply cut | Real Windows clipboard Access denied and cross-document paste in Minecraft |
| Unicode | M38 integrated JSON, Markdown, CSV and PDF test with accents, Greek and math symbols; M27 caret/wrap Unicode tests | Actual font fallback, non-Latin scripts and cursor geometry in GUI |
| Window/GUI/zoom | Shell size matrix; view-transform GUI scales 1-4 and zoom 40-200%; M27 narrow reflow 480/180/24/480 | Small/fullscreen, Minecraft GUI-scale and controls/dropdowns in game |
| Rendering/GPU | Renderer culls off-screen blocks; PDF output has expected pages | GPU state, buffers, reload, frame time and open/close cycles require live game |
| Import/export | M38 bounded CSV preview, mixed PDF export, Unicode interchange; M36 malformed CSV and overwrite tests | Visual PDF/CSV review and real large-file handling |
| Public API/addon | M35 API tests and `ScholarApiBoundaryTest`; `build` compiles separate reference-addon source set | Launch `runReferenceClient`, run its command, inspect persisted Free Fall Measurements |
| Production surface | `ProductionSurfaceBoundaryTest`; source registers `/scholar` through `ProductionClientCommands`; stress fixture is test-only | Verify Home/editor contain no dev UI during live QA |

## Defects and fixes

**CSV preview could read an unbounded file into memory.** `InterchangeFiles.readUtf8` used `Files.readString` before the parser could reject anything. A large or malicious `.csv` could allocate the entire file and then many parser objects on a background thread. The M38 fix caps imported UTF-8 input at 16 MiB using a bounded stream read, rejects oversized files with a user-facing `IOException`, and rejects malformed UTF-8 rather than silently replacing characters. The import dialog already reports async preview failure without mutating the document. `ScholarInterchangeServiceTest` covers oversized and malformed files. This is a practical V1 limit, not a data-platform promise.

No other production behavior has been changed in this checkpoint. Test-only stress coverage was added for cycling, corrupt-open preservation, paginated reflow, mixed PDF, Unicode, M32/M33 integration, and long history; existing zoom/shell tests were extended. An artificial 80x10 screen has no usable document area because chrome exceeds the viewport; the test explicitly checks zero workspace rather than claiming such a resolution is usable.

## Measurements and scaling

One local JUnit run (warm JVM, synthetic metrics, OneDrive filesystem): LARGE 1000-block paginated layout plus four viewport reflows and a repeat pagination took about 933 ms and produced 305 pages; DIAGRAM_HEAVY 38 ms/40 pages; MIXED_STRESS 58 ms/67 pages. Three save/load cycles took LARGE 736 ms, DATA_HEAVY 289 ms, DIAGRAM_HEAVY 48 ms, MIXED_STRESS 76 ms. MIXED_STRESS PDF export plus PDFBox reopen took 1159 ms, 149 KB, 67 pages. A 2000-row DATA_HEAVY dataset took about 12 ms for descriptive analysis and 14 ms for CSV export/parse round-trip in a focused run. These are diagnostic observations, **not** CI timing thresholds or Minecraft FPS. The existing M27 median measurements still run in the full suite; large single-viewport layout was about 54 ms in this run. `DocumentLayoutEngine` remains synchronous and complete; very large files can pause the client, so responsiveness is a V1 manual QA risk.

## Persistence, recovery and interchange

`FileDocumentStorage` stages a same-directory temporary file, forces it to disk, and attempts atomic replacement; when atomic move is unsupported it uses a complete-file replacement without a crash-atomic guarantee. Existing fault-injection tests prove pre-replacement write and move failures retain the previous file. Native decode is version-gated (V1/V2), size-bounded, UTF-8-strict and validator-gated; malformed input fails rather than installing a partial document. `DocumentWorkspace.open` changes session/name/saved baseline only on successful load; M38 proves dirty in-memory work survives corrupt open.

CSV preview now has a 16 MiB input cap. CSV remains an interchange format, not a native document backup. Markdown is lossy for Scholar-only content by design. PDF is a presentation export; the mixed document test verifies page count, not pixel-perfect output. Interchange writes use a temporary file; replacement falls back from atomic move if unsupported. The limit and fallback should be visible in release notes.

## Clipboard, cache, rendering and API findings

`MinecraftClipboardAdapter` catches runtime read/write failures and logs them. `BuiltInEditorActions.cut` prepares a candidate edit but applies it only after a successful OS clipboard write; a failure does not partially cut. A failed read is treated as empty text, so paste becomes a no-op rather than corrupting the document. The real Windows lock/Access denied path still needs manual reproduction; no retry loop was added.

`ComputationEngine` retains only the current document's derived results after each update. `DatasetAnalysisEngine` has an identity-keyed cache capped at 32 distinct dataset instances and clears on overflow; it may retain up to 32 dataset values while its owning layout/formatter is alive. Render/layout helpers are screen-owned; `MinecraftDocumentRenderer` has no owned texture/buffer cache. There is no JVM-only proof of GPU lifecycle or progressive native memory use. M38 does not rewrite caches speculatively.

The public API remains unchanged. `ScholarApiBoundaryTest` and API behavior tests cover invalid/no-op edits, history and bulk ingestion; `build` compiles the separately sourced reference addon. Its actual NeoForge load and command result are a manual release gate. Production command registration remains `/scholar`; the M27 stress fixture lives only under `src/test` and is not exposed as a production command. Gradle keeps Minecraft 1.21.1/NeoForge 21.1.x and PDFBox/Gson/JUnit dependencies; no dependency upgrade was made.

## Known V1 limitations and post-V1 candidates

- Native clipboard availability is host-dependent. A failed copy/cut leaves the document unchanged but does not provide an in-editor toast; logs carry the detail.
- CSV larger than 16 MiB is intentionally rejected; streaming CSV ingestion is a post-V1 candidate, not M38 scope.
- Very large documents use synchronous full layout and PDF generation. Incremental/async layout can be considered post-V1 only after native profiling.
- PDF, font fallback, GPU lifecycle and GUI-scale visual quality require manual Minecraft QA. Headless PDF page count is not a visual certification.
- M27 deterministic stress profiles are test-only. For manual stress, use a substantial existing user document or a separate copy of a generated stress file; do not add a production dev command.

## Manual stress QA in `/scholar`

1. Back up an existing large/mixed document, then open the copy in `/scholar`; scroll quickly across beginning/middle/end and inspect equations, figures, tables, references and TOC.
2. Cycle zoom 40, 50, 75, 100, 125, 150, 175 and 200%; resize small/normal/large/fullscreen and change Minecraft GUI scale. Check caret, selection, hit testing, ribbon, status bar, dialogs and dropdown reachability.
3. Edit prose near the start and end; insert/edit a variable, computed result and analysis; copy/paste one Figure or dataset-backed view twice, then undo and redo a long mixed sequence. Check dirty state and stable references.
4. Save, close, return Home, reopen, and repeat several times. Verify content, datasets, diagram geometry, page settings and dirty-close Cancel/Save/Discard behavior.
5. Export a multi-page PDF and inspect pages with prose, math, plots, diagrams and Unicode. Export/import a substantial CSV and confirm row counts, units, quoted/newline fields and failure messaging for malformed input.
6. Reopen a diagram-heavy document, zoom/scroll/select/edit/copy/paste, and inspect labels/wires/dimensions. Repeat document/Home/Scholar open-close and resource reload; watch logs and memory for progressive growth or render-state corruption.
7. Temporarily make the OS clipboard unavailable if practical; Cut must not delete content. Verify Unicode titles, columns, captions and pasted text. Launch the separate reference-addon client once and verify its Free Fall Measurements document persists.

The native stress pass and final verification were accepted before M39 closeout. This report does not claim Scholar 1.0.0 release qualification.
