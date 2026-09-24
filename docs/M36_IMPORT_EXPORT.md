# M36 - Import, Export and Interchange

Status: technical implementation awaiting manual `/scholar` QA. This document does not mark M36 accepted.

## Architecture audit

The M23 `ScientificDataset` owns ordered columns and rows, with optional M31 `UnitExpression` and `QuantitySemantics` on numeric columns. `EditorSession` owns semantic history. M26 `.scholar.json` remains the only native editable format. M30/M34's Minecraft-independent `DocumentLayoutEngine` and `LaidOutDocument` own wrapping, page assignment, column positions, math boxes, table cells, plot geometry, figure placement, and diagram geometry. `MinecraftDocumentRenderer` paints those values through `GuiGraphics` and cannot run as a PDF renderer without the client GPU. M35's addon API does not require export methods for V1; no public API was changed.

The existing `DatasetTabularImporter` is a deliberately small clipboard importer using delimiter splitting and does not implement quoted CSV. The existing `MarkdownSerializer` is a v0.1 interchange serializer that rejects plots, figures, and diagrams. M36 adds separate application-owned export paths instead of changing clipboard behavior or making Markdown canonical.

## Service boundaries

- `CsvRecords` parses and writes UTF-8 CSV records, including quoted commas, escaped quotes, CRLF/LF, BOM, and quoted empty text.
- `CsvDatasetInterchange` provides an immutable preview, conservative per-column inference, M31 unit parsing, thermal semantics, and CSV export.
- `ScholarInterchangeService` validates a candidate document before asking `EditorSession` to add a dataset in one history transaction. It delegates each export format.
- `MarkdownDocumentExporter` emits semantic text. It resolves references through Scholar's existing resolver and does not expose stable IDs.
- `PdfDocumentExporter` paints an existing paginated `LaidOutDocument` into physical PDF pages. It does not wrap lines or paginate independently, and receives no editor zoom, caret, selection, or UI chrome.
- `InterchangeFiles` keeps external file I/O apart from native persistence. It writes to a temporary sibling and replaces confirmed destinations atomically where supported. An unconfirmed existing destination is rejected.
- `ScholarInterchangeDialog` is client-only. It lists files/datasets, previews CSV, confirms imports and overwrites, and shows export progress/errors. Format logic stays outside the screen.

## CSV policy

The first row is a required nonempty header. Every data row must have the same number of fields; malformed quoting or row width rejects the import before mutation. All nonempty values in a column must parse with locale-independent `BigDecimal` for `NUMBER`; otherwise the column is `TEXT`. An unquoted empty field is `MISSING`; a quoted empty field is empty `TEXT`. An all-empty column is inferred as `NUMBER` because CSV has no schema. Import never imports stable IDs: dataset ID uses the existing first-free suffix convention and columns are locally numbered in order.

Headers matching `Name [unit]` use the M31 unit parser. A valid thermodynamic temperature unit without `Δ` is absolute; `Δ°C` and `ΔK` indicate a difference. Invalid/ambiguous bracketed units remain part of the plain column name with a preview warning. Unit-bearing columns that infer as text lose the unit with a warning. The writer emits the same bracket convention and M31 symbol formatter. Numbers use `BigDecimal.toString()` and are not localized. CSV cannot preserve arbitrary formatting, Scholar IDs, formulas, or a schema for a completely empty text column.

Confirming the preview calls one `EditorSession.addDataset` operation after normal document validation. Cancel, parse failure, or validation failure does not mutate history or mark the document dirty. Undo removes the dataset; redo restores the same IDs. The existing Data controls can insert a view of the imported dataset afterward.

## Markdown policy

Markdown is readable interchange, not page reproduction. Headings, marked text, quantities, resolved cross-references, equations in readable `$$` text, tables, TOC text, variables, computed results, and analyses are emitted. Dataset-backed tables are resolved through the existing table resolver. Page and layout section breaks are omitted. Plots and diagrams have deterministic text placeholders containing their titles; Figure captions remain readable and numbered. No PNG asset pipeline is claimed in V1. Use PDF when the visual content matters. Missing references retain the existing resolver's visible missing-reference label; internal IDs are not printed.

## PDF policy

PDFBox 3.0.8 is a focused vector backend, bundled as NeoForge jar-in-jar with FontBox and pdfbox-io. Minecraft 1.21.1 provides commons-logging 1.2; test runtime declares it explicitly. The PDF page MediaBox comes from M30 micrometre paper dimensions and orientation, not Minecraft window size. Its coordinate transform maps Scholar logical page coordinates to PDF points. The painter uses laid-out runs, table cells, math primitives, plot series, diagram elements, figure content/captions, headers, footers, and page numbers. Run-width fitting reconciles font-backend advances without changing Scholar's line breaks or pagination. Plot series use the existing palette and pattern/marker categories; diagrams use vector strokes.

Source Sans 3 regular/italic/bold/bold-italic and Noto Sans Math are embedded from Scholar's already-bundled font resources. Their OFL licenses are recorded under `docs/licenses/fonts/`. A fallback to Noto Sans Math is used when Source Sans cannot encode a run. PDF export is driven by immutable document/layout snapshots on a background task; a failed render never replaces the existing destination.

The PDF painter is output-specific drawing, not a second layout engine. GPU effects and every Minecraft pixel are not promised to match exactly. Dense electrical labels can still sit close to wires in the PDF, and mechanical symbol/constraint styling and visual line-dash appearance require manual comparison; page structure and semantic geometry stay authoritative in `LaidOutDocument`. Paginated layout preparation currently runs on the client thread before the background PDF writer starts, so very large documents need a responsiveness check during manual QA.

## Production workflow

`/scholar` File ribbon: Import CSV, Export Markdown, Export PDF. Data ribbon: Export CSV. Files live in `<Minecraft game directory>/scholar/interchange/` on every platform. This avoids a platform-specific chooser or path hacks. Import lists `.csv` files with paging. Preview shows row count, paged column names/types/units, sample rows, and warnings. The dataset name can be changed. Export chooses a filename, lists datasets when appropriate, confirms replacement, performs work off the render thread, and reports success or failure. No file is overwritten silently.

Development fixture: [`examples/import/free-fall.csv`](../examples/import/free-fall.csv). Copy it to the game's `scholar/interchange/` directory before importing. Import source files are read, never modified.

## Verification and manual QA

Automated coverage includes CSV malformed input, BOM/quotes/newlines, numeric and text inference, missing versus empty text, thermal unit round-trip, one import transaction with undo/redo, safe overwrite, Markdown references/equations, physical PDF size, headers/footers, Unicode, and the M34 readability sample with table, equations, plot, Figure caption, and multi-page output. `build/m36-readability.pdf` and `build/m36-diagrams.pdf` are generated test artifacts for visual inspection, not shipped fixtures.

1. Launch the normal client and open `/scholar`; open/create a document.
2. Put `examples/import/free-fall.csv` in `<game directory>/scholar/interchange/`. File > Import CSV; inspect the columns, thermal semantics, row count, sample and warnings, then confirm. Undo and redo; save and reopen. Insert a dataset table from the Data controls to inspect cells.
3. Data > Export CSV. Open the file externally and verify headers, values, missing cells, and quotes. Re-import and compare values, order, units, and semantics.
4. Open the M34 Readability Sample through the production Home flow. File > Export Markdown. Inspect headings, table, equations, references, Figure caption, and the declared visual placeholders. Confirm there are no internal IDs.
5. File > Export PDF. Open the PDF externally and compare page breaks, columns, typography, equations, table, plot, Figure caption, references, and any header/footer/page number against Scholar. Repeat at another editor zoom; physical page size and content should not change.
   In a diagram-bearing document, also compare dense electrical labels, wire continuity, mechanical dimensions, and symbols at readable zoom.
6. Export to an existing filename and deny confirmation; verify it remains unchanged. Then confirm overwrite. Try a malformed CSV; verify no dataset or dirty-state change.

M36 must remain open until those client checks are reported. Do not merge this branch on technical tests alone.
