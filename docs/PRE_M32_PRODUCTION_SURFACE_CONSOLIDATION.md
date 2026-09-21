# Pre-M32 Production Surface Consolidation

## Decision

Scholar has one user-facing application entry point: `/scholar`.

That command opens Scholar Home, which owns document creation, discovery, and opening through the production application repository. Documents open in the production editor workspace. Accepted scientific capabilities remain exposed through the editor ribbon and its shared `EditorAction` paths.

## Runtime Boundary

- Production code registers `/scholar` only.
- Development viewers, alternate editor commands, units demo commands, and stress-profile commands are not runtime product surfaces.
- Visual QA documents and stress documents live in test sources and may support automated regression, golden scenarios, persistence coverage, and performance measurements.
- A fixture must not acquire a command or production screen merely to make manual testing convenient.

## Capability Reachability Audit

The production Home and editor expose the accepted M30 and M31 surface:

- application lifecycle: New, Open/Home, Save, Save As, Rename, and Close;
- document structure and typesetting: paragraph and heading styles, semantic typography, paper, orientation, margins, columns, and page breaks;
- scientific content: equations, tables, plots, diagrams, datasets, cross-references, table of contents, and figures;
- scientific units and quantities: quantity insertion, conversion, number presentation, dataset-column units, and plot-axis units;
- electrical and mechanical diagram authoring through the grouped Diagram ribbon.

No accepted capability depends on a removed development command. Tests construct focused fixtures directly and exercise the same semantic/editor APIs used by production actions.

## Non-Goals

This consolidation does not alter the scientific document model, editor behavior, transfer, persistence, history, rendering, or ribbon command semantics. It does not start M32 or introduce a replacement visualizer application.
