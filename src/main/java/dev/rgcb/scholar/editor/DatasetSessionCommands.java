package dev.rgcb.scholar.editor;

import dev.rgcb.scholar.data.DatasetColumn;
import dev.rgcb.scholar.data.DatasetColumnType;
import dev.rgcb.scholar.data.DatasetPlotBinding;
import dev.rgcb.scholar.data.DatasetRow;
import dev.rgcb.scholar.data.DatasetTableBinding;
import dev.rgcb.scholar.data.DatasetValue;
import dev.rgcb.scholar.data.ScientificDataset;
import dev.rgcb.scholar.document.BlockNode;
import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.PlotBlock;
import dev.rgcb.scholar.document.TableBlock;
import dev.rgcb.scholar.plot.PlotDefinition;
import dev.rgcb.scholar.plot.PlotSeries;
import dev.rgcb.scholar.plot.PlotSeriesKind;
import dev.rgcb.scholar.quantity.QuantitySemantics;
import dev.rgcb.scholar.quantity.UnitExpression;
import dev.rgcb.scholar.plot.AxisDefinition;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;

/** Dataset/document commands sharing the owning session's state and history boundary. */
final class DatasetSessionCommands {
    private final EditorSession session;

    DatasetSessionCommands(EditorSession session) {
        this.session = Objects.requireNonNull(session, "session");
    }

    boolean supportsDocumentAction() {
        var state = session.current();
        return !state.isEquationEditingSelection()
                && !state.isTableEditingSelection()
                && !state.isPlotEditingSelection()
                && !state.isDiagramEditingSelection()
                && !state.isFigureCaptionSelection();
    }

    boolean createDefault() {
        return add(ScientificContentDefaults.dataset(uniqueId("dataset")));
    }

    boolean add(ScientificDataset dataset) {
        Objects.requireNonNull(dataset, "dataset");
        if (!supportsDocumentAction()) return false;
        var datasets = new ArrayList<>(session.current().document().datasets());
        var candidate = dataset;
        var candidateId = candidate.id();
        if (datasets.stream().anyMatch(existing -> existing.id().equals(candidateId))) {
            candidate = candidate.withId(uniqueId(candidate.id()));
        }
        datasets.add(candidate);
        return applyDatasets(datasets, session.current().selection(), session.current().explicitTypingMarks());
    }

    boolean delete(String datasetId) {
        Objects.requireNonNull(datasetId, "datasetId");
        var datasets = session.current().document().datasets().stream()
                .filter(dataset -> !dataset.id().equals(datasetId)).toList();
        if (datasets.size() == session.current().document().datasets().size()) return false;
        return applyDatasets(datasets, session.current().selection(), session.current().explicitTypingMarks());
    }

    Optional<ClipboardCopyResult> copyForClipboard(String datasetId) {
        return session.copyDatasetForCommands(datasetId);
    }

    boolean rename(String datasetId, String displayName) {
        return replace(datasetId, dataset -> dataset.withDisplayName(displayName));
    }

    boolean renameColumn(String datasetId, String columnId, String displayName) {
        return replace(datasetId, dataset -> dataset.withColumnDisplayName(columnId, displayName));
    }

    boolean setColumnUnit(String datasetId, String columnId, Optional<UnitExpression> unit) {
        return setColumnUnit(datasetId, columnId, unit,
                unit.map(QuantitySemantics::defaultFor).orElse(QuantitySemantics.LINEAR));
    }

    boolean setColumnUnit(
            String datasetId, String columnId, Optional<UnitExpression> unit, QuantitySemantics semantics) {
        Objects.requireNonNull(unit, "unit");
        Objects.requireNonNull(semantics, "semantics");
        return replace(datasetId, dataset -> dataset.withColumnUnit(columnId, unit, semantics));
    }

    boolean supportsSetSelectedColumnUnit() {
        var state = session.current();
        if (!state.isTableEditingSelection()) return false;
        var selection = state.tableEditingSelection();
        if (!(state.document().blocks().get(selection.blockIndex()) instanceof TableBlock table)
                || table.datasetBinding().isEmpty()) return false;
        var binding = table.datasetBinding().orElseThrow();
        return state.document().datasets().stream()
                .filter(dataset -> dataset.id().equals(binding.datasetId())).findFirst()
                .flatMap(dataset -> columnId(dataset, binding, selection.selection().cell().columnIndex())
                        .flatMap(dataset::column))
                .filter(column -> column.type() == DatasetColumnType.NUMBER)
                .isPresent();
    }

    boolean setSelectedColumnUnit(Optional<UnitExpression> unit) {
        return setSelectedColumnUnit(unit,
                unit.map(QuantitySemantics::defaultFor).orElse(QuantitySemantics.LINEAR));
    }

    boolean setSelectedColumnUnit(Optional<UnitExpression> unit, QuantitySemantics semantics) {
        Objects.requireNonNull(unit, "unit");
        if (!supportsSetSelectedColumnUnit()) return false;
        var state = session.current();
        var selection = state.tableEditingSelection();
        var table = (TableBlock) state.document().blocks().get(selection.blockIndex());
        var binding = table.datasetBinding().orElseThrow();
        var dataset = state.document().datasets().stream()
                .filter(candidate -> candidate.id().equals(binding.datasetId())).findFirst().orElseThrow();
        var columnId = columnId(dataset, binding, selection.selection().cell().columnIndex()).orElseThrow();
        return setColumnUnit(dataset.id(), columnId, unit, semantics);
    }

    boolean setPlotAxisUnit(int blockIndex, boolean xAxis, Optional<UnitExpression> unit) {
        return setPlotAxisUnit(blockIndex, xAxis, unit, unit.map(QuantitySemantics::defaultFor));
    }

    boolean setPlotAxisUnit(
            int blockIndex, boolean xAxis, Optional<UnitExpression> unit, Optional<QuantitySemantics> semantics) {
        Objects.requireNonNull(unit, "unit");
        Objects.requireNonNull(semantics, "semantics");
        var state = session.current();
        if (blockIndex < 0 || blockIndex >= state.document().blocks().size()
                || !(state.document().blocks().get(blockIndex) instanceof PlotBlock plot)) return false;
        var definition = plot.definition();
        var source = xAxis ? definition.xAxis() : definition.yAxis();
        var replacement = new AxisDefinition(source.label(), source.explicitRange(), source.scale(), unit, semantics);
        var updatedDefinition = new PlotDefinition(definition.title(),
                xAxis ? replacement : definition.xAxis(),
                xAxis ? definition.yAxis() : replacement,
                definition.series(), definition.legendVisible(), definition.gridVisible(), definition.height());
        var blocks = new ArrayList<BlockNode>(state.document().blocks());
        blocks.set(blockIndex, new PlotBlock(updatedDefinition));
        return session.applyCommandEdit(new EditResult(
                new Document(blocks, state.document().datasets(), state.document().settings()),
                state.selection(), state.explicitTypingMarks(), true));
    }

    boolean supportsSetSelectedPlotAxisUnit() {
        return selectedPlotBlockIndex().isPresent();
    }

    boolean setSelectedPlotAxisUnit(boolean xAxis, Optional<UnitExpression> unit) {
        return selectedPlotBlockIndex().map(index -> setPlotAxisUnit(index, xAxis, unit)).orElse(false);
    }

    boolean setSelectedPlotAxisUnit(
            boolean xAxis, Optional<UnitExpression> unit, Optional<QuantitySemantics> semantics) {
        return selectedPlotBlockIndex()
                .map(index -> setPlotAxisUnit(index, xAxis, unit, semantics)).orElse(false);
    }

    boolean editCell(String datasetId, int rowIndex, String columnId, DatasetValue value) {
        return replace(datasetId, dataset -> dataset.withCell(rowIndex, columnId, value));
    }

    boolean addRow(String datasetId, DatasetRow row) {
        return replace(datasetId, dataset -> dataset.withAddedRow(row));
    }

    boolean deleteRow(String datasetId, int rowIndex) {
        return replace(datasetId, dataset -> dataset.withoutRow(rowIndex));
    }

    boolean addColumn(String datasetId, DatasetColumn column, DatasetValue defaultValue) {
        return replace(datasetId, dataset -> dataset.withAddedColumn(column, defaultValue));
    }

    boolean deleteColumn(String datasetId, String columnId) {
        return replace(datasetId, dataset -> dataset.withoutColumn(columnId));
    }

    boolean supportsInsertTable() {
        return supportsDocumentAction()
                && !session.current().document().datasets().isEmpty()
                && session.commandEditor().supportsInsertBlock(session.current());
    }

    boolean insertTableForFirstDataset() {
        if (!supportsInsertTable()) return false;
        var dataset = session.current().document().datasets().getFirst();
        return session.applyCommandEdit(session.commandEditor().insertBlock(
                session.current(), new TableBlock(new DatasetTableBinding(dataset.id()))));
    }

    boolean supportsBindSelectedPlot() {
        var state = session.current();
        return state.isBlockSelection()
                && state.document().blocks().get(state.blockSelection().blockIndex()) instanceof PlotBlock
                && state.document().datasets().stream().anyMatch(dataset -> dataset.columns().size() >= 2);
    }

    boolean bindSelectedPlot() {
        if (!supportsBindSelectedPlot()) return false;
        var state = session.current();
        var blockIndex = state.blockSelection().blockIndex();
        var plot = (PlotBlock) state.document().blocks().get(blockIndex);
        var dataset = state.document().datasets().stream()
                .filter(candidate -> candidate.columns().size() >= 2).findFirst().orElseThrow();
        var binding = new DatasetPlotBinding(
                dataset.id(), dataset.columns().get(0).id(), dataset.columns().get(1).id());
        var definition = plot.definition();
        var series = definition.series().isEmpty()
                ? List.of(new PlotSeries(dataset.displayLabel(), PlotSeriesKind.LINE, binding))
                : replaceFirstSeriesBinding(definition.series(), binding);
        var updatedPlot = new PlotBlock(new PlotDefinition(definition.title(), definition.xAxis(),
                definition.yAxis(), series, definition.legendVisible(), definition.gridVisible(), definition.height()));
        var blocks = new ArrayList<BlockNode>(state.document().blocks());
        blocks.set(blockIndex, updatedPlot);
        return session.applyCommandEdit(new EditResult(
                new Document(blocks, state.document().datasets(), state.document().settings()),
                new BlockSelection(blockIndex), Optional.empty(), true));
    }

    private boolean replace(String datasetId, UnaryOperator<ScientificDataset> replacement) {
        Objects.requireNonNull(datasetId, "datasetId");
        Objects.requireNonNull(replacement, "replacement");
        var state = session.current();
        var datasets = new ArrayList<>(state.document().datasets());
        for (var index = 0; index < datasets.size(); index++) {
            if (!datasets.get(index).id().equals(datasetId)) continue;
            var updated = replacement.apply(datasets.get(index));
            if (updated.equals(datasets.get(index))) return false;
            datasets.set(index, updated);
            return applyDatasets(datasets, state.selection(), state.explicitTypingMarks());
        }
        return false;
    }

    private boolean applyDatasets(
            List<ScientificDataset> datasets, EditorSelection selection, Optional<java.util.Set<dev.rgcb.scholar.document.TextMark>> marks) {
        var current = session.current().document();
        return session.applyCommandEdit(new EditResult(
                new Document(current.blocks(), datasets, current.settings()), selection, marks, true));
    }

    private String uniqueId(String baseId) {
        var existing = session.current().document().datasets().stream()
                .map(ScientificDataset::id).collect(java.util.stream.Collectors.toSet());
        return dev.rgcb.scholar.document.StableIdAllocator.firstFree(baseId, existing);
    }

    private Optional<Integer> selectedPlotBlockIndex() {
        var state = session.current();
        if (state.isPlotEditingSelection()) return Optional.of(state.plotEditingSelection().blockIndex());
        if (state.isBlockSelection()
                && state.document().blocks().get(state.blockSelection().blockIndex()) instanceof PlotBlock) {
            return Optional.of(state.blockSelection().blockIndex());
        }
        return Optional.empty();
    }

    private static Optional<String> columnId(
            ScientificDataset dataset, DatasetTableBinding binding, int visibleColumnIndex) {
        if (visibleColumnIndex < 0) return Optional.empty();
        if (binding.usesAllColumns()) {
            return visibleColumnIndex < dataset.columns().size()
                    ? Optional.of(dataset.columns().get(visibleColumnIndex).id()) : Optional.empty();
        }
        return visibleColumnIndex < binding.columnIds().size()
                ? Optional.of(binding.columnIds().get(visibleColumnIndex)) : Optional.empty();
    }

    private static List<PlotSeries> replaceFirstSeriesBinding(
            List<PlotSeries> source, DatasetPlotBinding binding) {
        var updated = new ArrayList<>(source);
        updated.set(0, source.getFirst().withDatasetBinding(binding));
        return List.copyOf(updated);
    }
}
