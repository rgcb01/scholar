# M33 - Scientific Data Analysis

Status: complete and manually accepted. The manual QA procedure below remains useful for regression checks.

## Scope and ownership

M33 is a general Scholar document capability over `ScientificDataset`. It does not depend on Minecraft experiments, sensors, or an addon API. `/scholar` is the only production entry point. M32 scalar expressions do not acquire dataset functions.

`DatasetAnalysisBlock` is authored semantic content: stable analysis ID, dataset ID, stable column IDs, analysis kind, optional Y display unit, and number notation. It never stores statistics, coefficients, R-squared, generated curve samples, layout, or cache entries. `DatasetAnalysisEngine` recomputes these derived values from the immutable dataset; its bounded cache is keyed by dataset object identity and analysis definition, so replacing one dataset invalidates only results depending on that dataset. Recalculation creates no history edit and does not dirty the document.

## Supported calculations

- Descriptive statistics: valid count, missing count, min, max, arithmetic mean, median, sample variance (denominator n-1), and sample standard deviation. A single valid observation has no sample variance/SD and produces a diagnostic. Zero observations produce no numerical result.
- Ordinary least squares: line, quadratic, and cubic only. Rows with either X or Y missing are omitted as pairs. At least degree+1 pairs and nonconstant X are required; a singular system produces a diagnostic.
- Fits use DECIMAL128 `BigDecimal`, center and scale X before solving a small normal-equation system with partial pivoting, and derive authored-X polynomial coefficients. Coefficient units come from M31 unit factors as Y/X^n. Internal values are not rounded for display. Presentation alone rounds to six significant digits. R-squared uses 1-SSE/SST (constant Y uses an explicit perfect/imperfect result).
- Fitted plot curves are 65 deterministic transient samples over the observed paired X domain. No generated rows or points are stored in the document.

## Units and temperature

All analysis binds by dataset and column IDs, not names or positions. The Y display unit must be dimensionally compatible and is converted with M31 quantity conversion. Mean/min/max/median of absolute temperatures retain absolute-temperature semantics; SD is a temperature difference and variance is linear squared temperature. V1 rejects regression with an absolute-temperature X or Y axis rather than applying affine offsets as if linear. Temperature-difference axes remain eligible.

## Diagnostics and dependencies

Missing datasets, missing or text columns, incompatible display units, insufficient/constant data, and singular fits do not fabricate numerical results. The analysis block remains authored content and displays its current diagnostic. `DocumentValidator` checks lightweight dependency/configuration and fit-link issues without recomputing statistics during every unrelated edit; numerical diagnostics come from the derived analysis engine. Dataset/column renames retain links because IDs do not change. Deleting a dependency yields a visible unavailable state.

## Plot and editor

`PlotSeries.fitAnalysisId` is an authored reference to a document analysis. It cannot also contain authored points or a dataset binding. The plot resolver evaluates the analysis and samples the fit into a transient series. The Data ribbon exposes Insert Analysis, Edit Analysis, and Add Fit. The analysis dialog offers dataset, numeric X/Y column, analysis kind, notation, optional display unit, and a live preview. Editing preselects authored choices. Add Fit lists only regressions matching a selected dataset-backed plot series by dataset and X/Y column IDs. Insert, edit, and overlay addition each use one editor history transaction; deleting the selected block/series uses the existing deletion path. Undo/redo restore authored IDs and links without rerunning ID allocation.

## Transfer and persistence

Fragment extraction includes the required whole dataset in resource closure. The analysis ID is a document-global transfer identity, while column IDs remain dataset-local. Transferring an analysis remaps its dataset binding according to the resource plan; transferring a plot fit remaps its analysis ID if that analysis travels. A fit-only cross-document paste without a proven analysis witness is rejected atomically. Same-document external fit links require exact runtime-token and source-object witness proof. Matching names or coincidentally equal IDs never bind dependencies. Plain-text/Markdown export is intentionally lossy and omits internal IDs.

V2 JSON is extended additively with a `dataset-analysis` block and optional plot-series `fitAnalysisId`. Older V1/V2 documents without these fields still load. Only authored definitions and links are persisted; derived results are recomputed after load. M33 does not change the native file format version.

## Deliberate limits

No arbitrary-degree or nonlinear fitting, scripting, hypothesis tests, uncertainty propagation, external data acquisition, or experiment-specific interpretation. Very large datasets are not streamed; M33 uses bounded cached results and 65 display samples but still scans source rows to compute a changed analysis. The display unit field uses M31's structured parser; the normal fit overlay workflow requires a dataset-backed plot with matching stable column identities.

## Manual QA

In `/scholar`, create Free Fall Data with numeric Time (s) and Distance (m) columns and rows `(0,0)`, `(0.2,0.20)`, `(0.4,0.79)`, `(0.6,1.77)`, `(0.8,3.14)`, `(1,4.91)`. Insert descriptive statistics for Distance from Data > Analysis. Create a dataset-backed scatter plot with Time/Distance. Insert quadratic fit using those columns, select the plot, and choose Data > Add Fit. Verify a visible curve and sensible coefficient units. Change the final distance, confirm the block and curve update, undo, save/reopen, and rename both columns. The analysis and curve must remain linked by IDs. A fit-only cross-document structured paste must reject when its analysis is absent.
