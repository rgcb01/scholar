package dev.rgcb.scholar.integration;

import dev.rgcb.scholar.api.ScholarApi;
import dev.rgcb.scholar.api.ScholarApiException;
import dev.rgcb.scholar.api.data.ScholarData;
import dev.rgcb.scholar.api.document.ScholarDocument;
import dev.rgcb.scholar.api.document.ScholarDocuments;
import dev.rgcb.scholar.api.document.ScholarEdit;
import dev.rgcb.scholar.api.event.ScholarEvents;
import dev.rgcb.scholar.api.quantity.ScholarQuantity;
import dev.rgcb.scholar.api.quantity.ScholarUnits;
import dev.rgcb.scholar.application.ApplicationDocumentWorkspace;
import dev.rgcb.scholar.application.ScholarApplication;
import dev.rgcb.scholar.application.ScholarDocumentId;
import dev.rgcb.scholar.data.DatasetColumn;
import dev.rgcb.scholar.data.DatasetColumnType;
import dev.rgcb.scholar.data.DatasetPlotBinding;
import dev.rgcb.scholar.data.DatasetRow;
import dev.rgcb.scholar.data.DatasetTableBinding;
import dev.rgcb.scholar.data.DatasetValue;
import dev.rgcb.scholar.data.DatasetValueKind;
import dev.rgcb.scholar.data.ScientificDataset;
import dev.rgcb.scholar.document.BlockNode;
import dev.rgcb.scholar.document.DatasetAnalysisBlock;
import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.FigureBlock;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.PlotBlock;
import dev.rgcb.scholar.document.StableIdAllocator;
import dev.rgcb.scholar.document.TableBlock;
import dev.rgcb.scholar.document.Text;
import dev.rgcb.scholar.document.VariableDefinition;
import dev.rgcb.scholar.analysis.AnalysisKind;
import dev.rgcb.scholar.analysis.DatasetAnalysisEngine;
import dev.rgcb.scholar.compute.ScientificValue;
import dev.rgcb.scholar.compute.ExpressionBinder;
import dev.rgcb.scholar.document.ComputedResult;
import dev.rgcb.scholar.persistence.PersistenceResult;
import dev.rgcb.scholar.plot.AxisDefinition;
import dev.rgcb.scholar.plot.PlotDefinition;
import dev.rgcb.scholar.plot.PlotSeries;
import dev.rgcb.scholar.plot.PlotSeriesKind;
import dev.rgcb.scholar.quantity.NumberNotation;
import dev.rgcb.scholar.quantity.Quantity;
import dev.rgcb.scholar.quantity.QuantitySemantics;
import dev.rgcb.scholar.quantity.UnitConverter;
import dev.rgcb.scholar.quantity.UnitParser;
import dev.rgcb.scholar.quantity.UnitRegistry;
import dev.rgcb.scholar.validation.DocumentValidator;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Internal adapter from the stable addon vocabulary to Scholar's authored model. */
public final class ScholarApiService implements ScholarApi {
    private final ScholarApplication application;
    private final BooleanSupplier clientThread;
    private final CopyOnWriteArrayList<Consumer<ScholarEvents.Event>> listeners = new CopyOnWriteArrayList<>();
    private final ScholarDocuments documents = new DocumentsService();
    private final ScholarUnits units = new UnitsService();
    private final ScholarEvents events = listener -> {
        listeners.add(Objects.requireNonNull(listener));
        return () -> listeners.remove(listener);
    };

    public ScholarApiService(ScholarApplication application, BooleanSupplier clientThread) {
        this.application = Objects.requireNonNull(application);
        this.clientThread = Objects.requireNonNull(clientThread);
        application.subscribe(event -> emit(ScholarEvents.Kind.valueOf(event.kind().name()),
                new ScholarDocuments.Id(event.id().value())));
    }

    @Override public ScholarDocuments documents() { return documents; }
    @Override public ScholarUnits units() { return units; }
    @Override public ScholarEvents events() { return events; }

    private void requireClientThread() {
        if (!clientThread.getAsBoolean()) fail(ScholarApiException.Code.INVALID_THREAD, "Scholar document operations require the Minecraft client thread.");
    }

    private void emit(ScholarEvents.Kind kind, ScholarDocuments.Id id) {
        var event = new ScholarEvents.Event(kind, id);
        for (var listener : listeners) listener.accept(event);
    }

    private static void fail(ScholarApiException.Code code, String message) {
        throw new ScholarApiException(code, message);
    }

    private static <T> T value(PersistenceResult<T> result) {
        if (result instanceof PersistenceResult.Success<T> success) return success.value();
        var diagnostic = result.diagnostics().getFirst();
        var code = switch (diagnostic.code()) {
            case INVALID_NAME, INVALID_VALUE -> ScholarApiException.Code.INVALID_INPUT;
            case VALIDATION_FAILURE -> ScholarApiException.Code.VALIDATION_FAILED;
            default -> ScholarApiException.Code.IO_FAILURE;
        };
        fail(code, diagnostic.message());
        throw new AssertionError();
    }

    private static ScholarDocuments.Id id(ApplicationDocumentWorkspace workspace) {
        return new ScholarDocuments.Id(workspace.id().value());
    }

    private final class DocumentsService implements ScholarDocuments {
        @Override public List<Summary> list() {
            requireClientThread();
            return value(application.documents()).stream()
                    .map(descriptor -> new Summary(new Id(descriptor.id().value()), descriptor.displayName(),
                            descriptor.createdAtEpochMillis(), descriptor.modifiedAtEpochMillis(),
                            application.isDirtyOpen(descriptor.id()))).toList();
        }

        @Override public ScholarDocument create() {
            requireClientThread();
            var workspace = value(application.createDocument());
            return new DocumentHandle(workspace);
        }

        @Override public Optional<ScholarDocument> open(Id documentId) {
            requireClientThread();
            if (documentId == null) fail(ScholarApiException.Code.INVALID_INPUT, "Document ID is required.");
            var listed = value(application.documents());
            if (listed.stream().noneMatch(descriptor -> descriptor.id().value().equals(documentId.value()))) return Optional.empty();
            var workspace = value(application.openActiveDocument(new ScholarDocumentId(documentId.value())));
            return Optional.of(new DocumentHandle(workspace));
        }
    }

    private final class DocumentHandle implements ScholarDocument {
        private final ApplicationDocumentWorkspace workspace;
        private DocumentHandle(ApplicationDocumentWorkspace workspace) { this.workspace = workspace; }

        private void requireOpen() {
            requireClientThread();
            if (!application.isOpen(workspace)) fail(ScholarApiException.Code.NOT_FOUND, "Document workspace is closed.");
        }

        @Override public ScholarDocuments.Id id() { requireOpen(); return ScholarApiService.id(workspace); }
        @Override public String name() { requireOpen(); return workspace.displayName(); }
        @Override public boolean isDirty() { requireOpen(); return workspace.isDirty(); }
        @Override public List<ScholarData.Dataset> datasets() {
            requireOpen();
            return workspace.session().current().document().datasets().stream().map(ScholarApiService::snapshot).toList();
        }
        @Override public List<ScholarData.Variable> variables() {
            requireOpen();
            return workspace.session().current().document().blocks().stream()
                    .filter(VariableDefinition.class::isInstance).map(VariableDefinition.class::cast)
                    .map(variable -> new ScholarData.Variable(variable.id(), variable.name(), fromScientific(variable.value()))).toList();
        }

        @Override public boolean edit(Consumer<ScholarEdit> operation) {
            requireOpen();
            if (operation == null) fail(ScholarApiException.Code.INVALID_INPUT, "Edit callback is required.");
            var staged = new StagedEdit(workspace.session().current().document());
            try {
                operation.accept(staged);
                var replacement = staged.document();
                if (replacement.equals(workspace.session().current().document())) return false;
                var validation = DocumentValidator.validate(replacement);
                if (!validation.isValid()) fail(ScholarApiException.Code.VALIDATION_FAILED, validation.diagnostics().getFirst().message());
                var changed = workspace.session().applyExternalDocumentEdit(replacement);
                return changed;
            } catch (ScholarApiException exception) {
                throw exception;
            } catch (IllegalArgumentException | NullPointerException exception) {
                throw new ScholarApiException(ScholarApiException.Code.INVALID_INPUT, exception.getMessage());
            }
        }

        @Override public void save() {
            requireOpen();
            value(workspace.save());
        }
    }

    private final class UnitsService implements ScholarUnits {
        @Override public List<Unit> builtIns() {
            return UnitRegistry.builtIn().units().stream().map(unit -> new Unit(unit.symbol(), unit.name())).toList();
        }
        @Override public Dimension dimension(String expression) {
            try {
                var dimension = new UnitParser().parseRequired(expression).dimension(UnitRegistry.builtIn());
                return new Dimension(dimension.length(), dimension.mass(), dimension.time(), dimension.electricCurrent(),
                        dimension.temperature(), dimension.amount(), dimension.luminousIntensity());
            } catch (IllegalArgumentException | NullPointerException exception) {
                throw new ScholarApiException(ScholarApiException.Code.INVALID_INPUT, exception.getMessage());
            }
        }
        @Override public ScholarQuantity convert(ScholarQuantity source, String targetUnit) {
            try {
                var converted = new UnitConverter().convert(toQuantity(source), new UnitParser().parseRequired(targetUnit));
                return new ScholarQuantity(converted.value(), converted.unit().displaySymbol(UnitRegistry.builtIn()), source.semantics());
            } catch (IllegalArgumentException | NullPointerException exception) {
                throw new ScholarApiException(ScholarApiException.Code.INVALID_INPUT, exception.getMessage());
            }
        }
    }

    private static Quantity toQuantity(ScholarQuantity value) {
        Objects.requireNonNull(value);
        return new Quantity(value.value(), new UnitParser().parseRequired(value.unit()),
                QuantitySemantics.valueOf(value.semantics().name()));
    }

    private static ScholarQuantity fromScientific(ScientificValue value) {
        if (value instanceof ScientificValue.Scalar scalar) return ScholarQuantity.linear(scalar.value(), "1");
        var quantity = ((ScientificValue.Physical) value).value().nominal();
        return new ScholarQuantity(quantity.value(), quantity.unit().displaySymbol(UnitRegistry.builtIn()),
                ScholarQuantity.Semantics.valueOf(quantity.semantics().name()));
    }

    private static ScholarData.Dataset snapshot(ScientificDataset dataset) {
        return new ScholarData.Dataset(dataset.id(), dataset.displayLabel(), dataset.columns().stream()
                .map(column -> new ScholarData.Column(column.id(), column.displayName(),
                        ScholarData.ColumnType.valueOf(column.type().name()),
                        column.unit().map(unit -> unit.displaySymbol(UnitRegistry.builtIn())),
                        ScholarQuantity.Semantics.valueOf(column.quantitySemantics().name()))).toList(),
                dataset.rows().stream().map(row -> row.values().stream().map(value -> switch (value.kind()) {
                    case NUMBER -> ScholarData.Cell.number(value.number().orElseThrow());
                    case TEXT -> ScholarData.Cell.text(value.text().orElseThrow());
                    case MISSING -> ScholarData.Cell.missing();
                }).toList()).toList());
    }

    private static DatasetValue toCell(ScholarData.Cell cell, DatasetColumn column) {
        Objects.requireNonNull(cell);
        if (cell.kind() != ScholarData.CellKind.MISSING && !cell.kind().name().equals(column.type().name())) {
            fail(ScholarApiException.Code.INVALID_INPUT, "Cell type does not match column " + column.id());
        }
        return switch (cell.kind()) {
            case NUMBER -> DatasetValue.number(cell.number().orElseThrow());
            case TEXT -> DatasetValue.text(cell.text().orElseThrow());
            case MISSING -> DatasetValue.missing();
        };
    }

    private static final class StagedEdit implements ScholarEdit {
        private final ArrayList<BlockNode> blocks;
        private final ArrayList<ScientificDataset> datasets;
        private final Document original;

        private StagedEdit(Document original) {
            this.original = original;
            blocks = new ArrayList<>(original.blocks());
            datasets = new ArrayList<>(original.datasets());
        }

        private Document document() {
            var draft = new Document(blocks, datasets, original.settings());
            var binder = new ExpressionBinder();
            var bound = new ArrayList<>(blocks);
            for (var index = 0; index < bound.size(); index++) {
                if (bound.get(index) instanceof ComputedResult computed) {
                    var expression = binder.bindAvailable(computed.expression(), draft);
                    if (!expression.equals(computed.expression())) {
                        bound.set(index, computed.withExpression(expression, computed.authoredSource()));
                    }
                }
            }
            return new Document(bound, datasets, original.settings());
        }

        private ScientificDataset dataset(String id) {
            return datasets.stream().filter(value -> value.id().equals(id)).findFirst().orElseThrow(
                    () -> new ScholarApiException(ScholarApiException.Code.NOT_FOUND, "Unknown dataset: " + id));
        }

        private void replaceDataset(ScientificDataset replacement) {
            for (var index = 0; index < datasets.size(); index++) {
                if (datasets.get(index).id().equals(replacement.id())) { datasets.set(index, replacement); return; }
            }
            fail(ScholarApiException.Code.NOT_FOUND, "Unknown dataset: " + replacement.id());
        }

        @Override public String createDataset(String name, List<ScholarData.ColumnSpec> columns) {
            if (columns == null || columns.isEmpty()) fail(ScholarApiException.Code.INVALID_INPUT, "Dataset needs columns.");
            var occupied = datasets.stream().map(ScientificDataset::id).collect(java.util.stream.Collectors.toSet());
            var id = StableIdAllocator.firstFree("dataset", occupied);
            var newColumns = new ArrayList<DatasetColumn>();
            var columnIds = new HashSet<String>();
            for (var spec : columns) {
                var columnId = StableIdAllocator.firstFree("column", columnIds);
                columnIds.add(columnId);
                var unit = spec.unit().map(value -> new UnitParser().parseRequired(value));
                newColumns.add(new DatasetColumn(columnId, spec.name(), DatasetColumnType.valueOf(spec.type().name()),
                        unit, QuantitySemantics.valueOf(spec.semantics().name())));
            }
            datasets.add(new ScientificDataset(id, name, newColumns, List.of()));
            return id;
        }

        @Override public List<ScholarData.Column> columns(String datasetId) {
            return snapshot(dataset(datasetId)).columns();
        }

        @Override public void appendRows(String datasetId, List<List<ScholarData.Cell>> rows) {
            var source = dataset(datasetId);
            if (rows == null) fail(ScholarApiException.Code.INVALID_INPUT, "Rows are required.");
            var appended = new ArrayList<>(source.rows());
            for (var row : rows) {
                if (row == null || row.size() != source.columns().size()) {
                    fail(ScholarApiException.Code.INVALID_INPUT, "Row width differs from dataset columns.");
                }
                var values = new ArrayList<DatasetValue>();
                for (var index = 0; index < row.size(); index++) values.add(toCell(row.get(index), source.columns().get(index)));
                appended.add(new DatasetRow(values));
            }
            if (appended.size() != source.rows().size()) {
                replaceDataset(new ScientificDataset(source.id(), source.displayName(), source.columns(), appended));
            }
        }

        @Override public void appendMeasurement(String datasetId, Map<String, ScholarQuantity> byColumnId) {
            var source = dataset(datasetId);
            Objects.requireNonNull(byColumnId);
            if (byColumnId.keySet().stream().anyMatch(key -> source.column(key).isEmpty())) {
                fail(ScholarApiException.Code.NOT_FOUND, "Measurement contains an unknown column.");
            }
            var row = new ArrayList<ScholarData.Cell>();
            for (var column : source.columns()) {
                var measurement = byColumnId.get(column.id());
                if (measurement == null) { row.add(ScholarData.Cell.missing()); continue; }
                if (column.type() != DatasetColumnType.NUMBER) {
                    fail(ScholarApiException.Code.INVALID_INPUT, "Measurement column must be numeric.");
                }
                var sourceQuantity = toQuantity(measurement);
                var targetUnit = column.unit().orElseGet(() -> new UnitParser().parseRequired("1"));
                if (sourceQuantity.semantics() != column.quantitySemantics()) {
                    fail(ScholarApiException.Code.INVALID_INPUT, "Measurement temperature semantics differ from column.");
                }
                row.add(ScholarData.Cell.number(new UnitConverter().convert(sourceQuantity, targetUnit).value()));
            }
            appendRows(datasetId, List.of(row));
        }

        @Override public void setCell(String datasetId, int rowIndex, String columnId, ScholarData.Cell value) {
            var source = dataset(datasetId);
            var column = source.column(columnId).orElseThrow(() -> new ScholarApiException(
                    ScholarApiException.Code.NOT_FOUND, "Unknown column: " + columnId));
            if (rowIndex < 0 || rowIndex >= source.rows().size()) fail(ScholarApiException.Code.INVALID_INPUT, "Row is out of range.");
            replaceDataset(source.withCell(rowIndex, columnId, toCell(value, column)));
        }

        @Override public String defineVariable(String name, ScholarQuantity value) {
            var quantity = toQuantity(value);
            var scientific = new ScientificValue.Physical(quantity);
            for (var index = 0; index < blocks.size(); index++) {
                if (blocks.get(index) instanceof VariableDefinition variable && variable.name().equals(name)) {
                    blocks.set(index, variable.withValue(scientific));
                    return variable.id();
                }
            }
            var existing = blocks.stream().filter(VariableDefinition.class::isInstance)
                    .map(VariableDefinition.class::cast).map(VariableDefinition::id)
                    .collect(java.util.stream.Collectors.toSet());
            var id = StableIdAllocator.firstFree("variable-" + name, existing);
            blocks.add(new VariableDefinition(id, name, scientific));
            return id;
        }

        @Override public void insertDatasetTable(String datasetId) {
            dataset(datasetId);
            blocks.add(new TableBlock(new DatasetTableBinding(datasetId)));
        }

        private PlotBlock plot(String datasetId, String xColumnId, String yColumnId, String title) {
            var source = dataset(datasetId);
            for (var columnId : List.of(xColumnId, yColumnId)) {
                var column = source.column(columnId).orElseThrow(() -> new ScholarApiException(
                        ScholarApiException.Code.NOT_FOUND, "Unknown plot column: " + columnId));
                if (column.type() != DatasetColumnType.NUMBER) fail(ScholarApiException.Code.INVALID_INPUT, "Plot axes require numeric columns.");
            }
            var series = new PlotSeries(source.displayLabel(), PlotSeriesKind.SCATTER,
                    new DatasetPlotBinding(datasetId, xColumnId, yColumnId));
            var definition = PlotDefinition.of(title, AxisDefinition.linear(source.column(xColumnId).orElseThrow().displayName()),
                    AxisDefinition.linear(source.column(yColumnId).orElseThrow().displayName()), List.of(series));
            return new PlotBlock(definition);
        }

        @Override public void insertDatasetPlot(String datasetId, String xColumnId, String yColumnId, String title) {
            blocks.add(plot(datasetId, xColumnId, yColumnId, title));
        }

        @Override public String insertFigurePlot(String datasetId, String xColumnId, String yColumnId, String title, String caption) {
            var plot = plot(datasetId, xColumnId, yColumnId, title);
            var ids = blocks.stream().filter(FigureBlock.class::isInstance).map(FigureBlock.class::cast)
                    .map(FigureBlock::id).collect(java.util.stream.Collectors.toSet());
            var id = StableIdAllocator.firstFree("figure", ids);
            blocks.add(new FigureBlock(id, plot, new InlineContent(List.of(new Text(caption, Set.of())))));
            return id;
        }

        @Override public String requestAnalysis(String datasetId, ScholarData.Analysis kind, Optional<String> xColumnId, String yColumnId) {
            dataset(datasetId);
            var ids = blocks.stream().filter(DatasetAnalysisBlock.class::isInstance).map(DatasetAnalysisBlock.class::cast)
                    .map(DatasetAnalysisBlock::id).collect(java.util.stream.Collectors.toSet());
            var id = StableIdAllocator.firstFree("analysis", ids);
            var block = new DatasetAnalysisBlock(id, datasetId, AnalysisKind.valueOf(kind.name()), xColumnId,
                    yColumnId, Optional.empty(), NumberNotation.DECIMAL);
            var outcome = new DatasetAnalysisEngine().evaluate(document(), block);
            if (outcome.result().isEmpty()) fail(ScholarApiException.Code.INVALID_INPUT, outcome.diagnostics().getFirst().message());
            blocks.add(block);
            return id;
        }
    }
}
