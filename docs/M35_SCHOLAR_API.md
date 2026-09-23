# M35 Scholar Addon API (V1)

## Philosophy and audit

Scholar remains the owner of documents, scientific semantics, persistence, history, layout and rendering. M35 gives ordinary NeoForge mods a client-side integration surface for existing Scholar content, not a plugin runtime or a sandbox. No arbitrary third-party block registration is supported: a new block family would also need validation, transfer, persistence, layout and rendering contracts.

| Capability | Classification | V1 treatment |
| --- | --- | --- |
| Document identity, summaries, dataset/variable snapshots | Public contract candidate | Opaque document ID and immutable API snapshots |
| Create/open document and semantic edit | Public service candidate | `ScholarApi.get().documents()` and `ScholarDocument.edit` |
| Dataset columns/rows, missing values, measurement ingestion | Public contract and service candidate | Typed API values, validated staged edits |
| Units/quantities | Public contract and service candidate | Built-in parser, dimensions and conversions behind `ScholarUnits` |
| Variables | Public service candidate | Define/update an authored scalar/quantity, retain stable ID |
| Computed blocks/evaluator | Unstable / not ready | Scholar owns expression parsing, bindings and derived results |
| Analysis requests | Public service candidate | Descriptive and polynomial fit requests; Scholar evaluates |
| Dataset-backed tables, plots and plot figures | Public service candidate | Existing semantic blocks only; no render API |
| Electrical/mechanical diagrams | Unstable / not ready | Rich internal graph and editor, no M35 construction API |
| Validation | Internal machinery | Same `DocumentValidator` is applied before commit |
| Persistence lifecycle | Public service candidate | Save an open handle; Scholar owns its file format |
| Application events | Public contract candidate | Open/change/save/close only |
| Editor, clipboard, transfer, rendering, storage, caches | Internal | No public implementation types |

The only supported compatibility namespace is `dev.rgcb.scholar.api` and its `document`, `data`, `quantity` and `event` subpackages. Everything else is implementation, even if a Java class is currently `public` for inter-package use. The API is included in the normal Scholar mod JAR; there is no separate API artifact in V1.

## Getting the API and versioning

`ScholarApi.get()` returns the active client API after Minecraft client initialization. It does not initialize Scholar on a dedicated server. `majorVersion()` and `minorVersion()` currently report **1.0**. Major changes may break signatures/semantics; minor additions preserve existing public contracts. This is independent of the Scholar mod version. A regular NeoForge required dependency on `scholar` is the recommended integration; optional integration must avoid loading API classes when Scholar is absent. Addon registration is unnecessary: NeoForge already identifies the mod. No plugin manager, dynamic JAR loading, marketplace or custom classloader exists.

## Documents and safe mutation

`documents().list()` returns library summaries. IDs are obtained from the list or `create()` and should be treated as opaque; guessed IDs are not a supported discovery mechanism. `open(id)` returns `Optional.empty()` for an absent document and shares an active workspace when one exists. `create()` creates a normal blank Scholar document. `ScholarDocument` exposes read-only dataset and variable snapshots, dirty state, an edit transaction and save. It does not expose `Document`, `EditorSession` or a file path.

An edit callback stages semantic changes against the current document, then validates and commits the replacement through the existing editor history once. No-op returns `false`; invalid input throws `ScholarApiException` and commits nothing. The callback must not retain its `ScholarEdit` object or call `edit` recursively. Generated IDs are returned inside the callback and become durable only if the whole edit succeeds. A successful authored edit is dirty; a query, failure or no-op is not. Undo/redo restores the committed document state and IDs; it does not re-run the callback. API content remains ordinary Scholar content with normal transfer and `.scholar.json` persistence.

```java
var document = ScholarApi.get().documents().create();
document.edit(edit -> {
    var dataset = edit.createDataset("Motion", List.of(
            ScholarData.ColumnSpec.number("Time", "s"),
            ScholarData.ColumnSpec.number("Distance", "m")));
    edit.appendRows(dataset, List.of(List.of(
            ScholarData.Cell.number(new BigDecimal("0.2")),
            ScholarData.Cell.number(new BigDecimal("0.20")))));
    edit.insertDatasetTable(dataset);
});
document.save();
```

## Datasets and measurements

`createDataset` allocates a collision-free dataset ID and dataset-local column IDs. A column has NUMBER or TEXT type, optional M31 unit and explicit quantity semantics. Cells are NUMBER, TEXT or MISSING. Row widths and cell types are checked before committing. `appendRows` accepts a batch and creates one history transaction when called alone within `edit`. `setCell` updates an existing row/column and is a no-op if the value is unchanged.

`appendMeasurement` accepts a map from column ID to `ScholarQuantity`. Each supplied numeric value is converted into the target column's unit using M31. Omitted columns become MISSING. This is for typical sensor/experiment observations; use `appendRows` for mixed numeric/text tables. Physical values are never converted by string formatting.

## Units and quantities

`units().builtIns()` lists the existing registry's built-in units, `dimension(expression)` parses an M31 unit expression and returns SI base-dimension exponents, and `convert` converts a quantity. `ScholarQuantity.Semantics` distinguishes LINEAR, ABSOLUTE_TEMPERATURE and TEMPERATURE_DIFFERENCE. Celsius and Kelvin differences ignore affine offsets, while absolute temperatures include them. M35 does not allow registering third-party units or mutating the M31 registry. Unit parsing and incompatible conversion failures are reported as invalid input.

## Variables, computation and analysis

`defineVariable` creates an authored physical quantity variable or updates the value of an existing variable with that name, retaining its stable ID. Use unit `1` for dimensionless quantities. The API does not expose expression ASTs, evaluation caches or arbitrary `ComputedResult` construction. Scholar remains the computation owner. `requestAnalysis` creates an existing authored analysis block only if the current dataset can produce a result. Kinds include descriptive statistics, linear regression and quadratic/cubic fit; derived metrics and samples are not persisted or owned by the addon.

## Plots, figures and diagrams

`insertDatasetTable`, `insertDatasetPlot` and `insertFigurePlot` add ordinary Scholar semantic content using an existing dataset and numeric columns. A Figure wraps the supported Plot content and stores a plain caption. Plot rendering/layout and axis mechanics stay internal. Diagram graph construction is intentionally not public in V1; exposing it now would couple addons to the electrical/mechanical editor and local identity model. This does not affect user-authored diagrams.

## Events and threading

`events().subscribe` returns an `AutoCloseable` subscription for document opened, changed, saved and closed events. The change event follows committed semantic changes, including UI edits and Undo/Redo; it is not emitted for caret movement, rendered frames, derived recomputation or a no-op. Listeners run synchronously on the client thread and should do little work. Listener exceptions are logged and cannot undo an already committed edit. Lifecycle events do not expose transient editor state.

All document/library operations, including reads and save, require the Minecraft client thread. Invalid-thread calls throw `ScholarApiException(INVALID_THREAD)` before mutation. `ScholarUnits` operations are immutable and can be called from other threads. M35 offers no asynchronous mutation queue. Addons must schedule their own client-thread handoff. This API is a compatibility boundary among mods in the same JVM, not a hostile-code security sandbox.

## Errors and ownership

`ScholarApiException.Code` distinguishes INVALID_INPUT, NOT_FOUND, INVALID_THREAD, VALIDATION_FAILED and IO_FAILURE. Ordinary missing documents use `Optional.empty()`. Scholar owns `.scholar.json` writes, semantic IDs, transfer and derived state; addon-created content is not locked to the creating mod. Addon-specific gameplay state remains the addon's responsibility. M35 does not change the native format version or the project license.

## Reference addon and build

`examples/reference-addon` is a separate NeoForge source set and mod, compiled during `check` and intentionally excluded from the production Scholar JAR and normal `runClient`. Its only Scholar imports are under `dev.rgcb.scholar.api`, enforced by `ScholarApiBoundaryTest`. The root build uses Scholar's compiled output as the local API dependency for this example; a separately distributed addon would declare a normal compile dependency on the Scholar mod artifact and a NeoForge runtime dependency.

Run `./gradlew runReferenceClient` (Windows: `.\gradlew.bat runReferenceClient`). In a world, execute `/scholar_reference_demo`, then `/scholar`. The demo creates and saves a Free Fall document with time/distance dataset, rows, dataset-backed table, Figure/Plot and quadratic analysis. Open it, edit it, exercise Undo/Redo, save and reopen, then copy/paste the visual content. The command belongs to the reference addon, not Scholar production. Manual in-game acceptance remains required before M35 can be declared accepted or merged.

## Compatibility limits

V1 does not expose arbitrary blocks, custom units, diagrams, evaluator internals, renderers, clipboard/transfer internals, storage paths, persistence codecs or editor widgets. There is no independent server-side document service. Public API signatures use only JDK and API types; source-level implementation classes outside this namespace are not promised stable. API 1.x can grow additively, but no Scholar 1.0 ecosystem stability is claimed yet.
