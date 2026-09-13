# M23 Implementation Report - Scientific Data And Datasets

## Status

Implemented with automated validation complete. Manual Minecraft QA remains pending.

## Implemented

- Added document-owned immutable `ScientificDataset` resources.
- Added stable dataset IDs and stable column IDs.
- Added optional row IDs for future use while keeping M23 row operations index-based.
- Added `DatasetValue` kinds for numeric, text, and missing values.
- Added immutable dataset editing operations for rename, column edit, row edit, and cell edit.
- Added dataset-backed `TableBlock` bindings.
- Added dataset-backed `PlotSeries` bindings.
- Added pure-Java resolvers that materialize current table and plot views from dataset bindings.
- Added deterministic broken binding behavior for missing datasets or columns.
- Added plot resolution policy that skips missing or nonnumeric rows.
- Added native dataset clipboard payload with readable TSV fallback and duplicate-ID remapping on paste.
- Added bounded CSV/TSV import helper for simple tabular datasets.
- Added Data menu actions for creating a sample dataset, inserting a dataset-backed table, and binding a selected plot to the first dataset.
- Updated Markdown and plain-text export to flatten dataset-backed tables as current resolved snapshots.
- Added development fixtures for dataset-backed table and plot views, column subsets, broken bindings, and mixed data.

## Validation

- `.\gradlew.bat test`
- `.\gradlew.bat build`

## Out Of Scope

- Units.
- Formulas.
- Spreadsheet semantics.
- Dataset persistence format design.
- Dataset manager UI.
- Multi-block dependency-aware copy/paste for views and their datasets.
- Dataset-backed Markdown syntax.
- Dynamic datasets or external data connections.
