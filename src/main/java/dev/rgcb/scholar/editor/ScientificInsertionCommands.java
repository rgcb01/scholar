package dev.rgcb.scholar.editor;

import dev.rgcb.scholar.analysis.AnalysisKind;
import dev.rgcb.scholar.analysis.DatasetAnalysisEngine;
import dev.rgcb.scholar.compute.ComputationEngine;
import dev.rgcb.scholar.compute.ComputationSnapshot;
import dev.rgcb.scholar.compute.Expression;
import dev.rgcb.scholar.compute.ExpressionBinder;
import dev.rgcb.scholar.compute.ExpressionEvaluator;
import dev.rgcb.scholar.compute.ExpressionParser;
import dev.rgcb.scholar.compute.ScientificValue;
import dev.rgcb.scholar.document.BlockNode;
import dev.rgcb.scholar.document.ComputedResult;
import dev.rgcb.scholar.document.DatasetAnalysisBlock;
import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.FigureBlock;
import dev.rgcb.scholar.document.PlotBlock;
import dev.rgcb.scholar.document.VariableDefinition;
import dev.rgcb.scholar.plot.PlotDefinition;
import dev.rgcb.scholar.plot.PlotSeries;
import dev.rgcb.scholar.quantity.MeasuredQuantity;
import dev.rgcb.scholar.quantity.NumberNotation;
import dev.rgcb.scholar.quantity.UnitExpression;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Variable, computed-result, and dataset-analysis commands for a session. */
final class ScientificInsertionCommands {
    private final EditorSession session;
    private final ComputationEngine computationEngine = new ComputationEngine();
    private final DatasetAnalysisEngine analysisEngine = new DatasetAnalysisEngine();

    ScientificInsertionCommands(EditorSession session) {
        this.session = Objects.requireNonNull(session, "session");
    }

    boolean supportsInsertAnalysis() {
        return supportsInsertComputation() && !session.current().document().datasets().isEmpty();
    }

    boolean supportsEditAnalysis() {
        var state = session.current();
        return state.isBlockSelection()
                && state.document().blocks().get(state.blockSelection().blockIndex()) instanceof DatasetAnalysisBlock;
    }

    boolean insertAnalysis(
            String datasetId,
            AnalysisKind kind,
            Optional<String> xColumnId,
            String yColumnId,
            Optional<UnitExpression> displayUnit,
            NumberNotation notation
    ) {
        if (!supportsInsertAnalysis()) return false;
        var state = session.current();
        var existing = state.document().blocks().stream().filter(DatasetAnalysisBlock.class::isInstance)
                .map(DatasetAnalysisBlock.class::cast).map(DatasetAnalysisBlock::id)
                .collect(java.util.stream.Collectors.toSet());
        var id = dev.rgcb.scholar.document.StableIdAllocator.firstFree("analysis", existing);
        var block = new DatasetAnalysisBlock(
                id, datasetId, kind, xColumnId, yColumnId, displayUnit, notation);
        if (analysisEngine.evaluate(state.document(), block).result().isEmpty()) return false;
        return session.applyCommandEdit(session.commandEditor().insertBlock(state, block));
    }

    boolean editAnalysis(
            String datasetId,
            AnalysisKind kind,
            Optional<String> xColumnId,
            String yColumnId,
            Optional<UnitExpression> displayUnit,
            NumberNotation notation
    ) {
        if (!supportsEditAnalysis()) return false;
        var state = session.current();
        var index = state.blockSelection().blockIndex();
        var original = (DatasetAnalysisBlock) state.document().blocks().get(index);
        var replacement = new DatasetAnalysisBlock(
                original.id(), datasetId, kind, xColumnId, yColumnId, displayUnit, notation);
        if (original.equals(replacement)
                || analysisEngine.evaluate(state.document(), replacement).result().isEmpty()) return false;
        var blocks = new ArrayList<>(state.document().blocks());
        blocks.set(index, replacement);
        return session.applyCommandEdit(new EditResult(
                withCurrentDatasets(blocks), state.selection(), state.explicitTypingMarks(), true));
    }

    List<DatasetAnalysisBlock> availableFitAnalyses() {
        var plot = selectedPlotBlock();
        if (plot.isEmpty()) return List.of();
        return session.current().document().blocks().stream()
                .filter(DatasetAnalysisBlock.class::isInstance)
                .map(DatasetAnalysisBlock.class::cast)
                .filter(analysis -> analysis.kind().isFit())
                .filter(analysis -> plot.orElseThrow().definition().series().stream().anyMatch(series ->
                        series.datasetBinding().filter(binding ->
                                binding.datasetId().equals(analysis.datasetId())
                                        && binding.xColumnId().equals(analysis.xColumnId().orElse(""))
                                        && binding.yColumnId().equals(analysis.yColumnId())).isPresent()))
                .filter(analysis -> plot.orElseThrow().definition().series().stream().noneMatch(series ->
                        series.fitAnalysisId().filter(analysis.id()::equals).isPresent()))
                .toList();
    }

    boolean supportsAddFitOverlay() {
        return !availableFitAnalyses().isEmpty();
    }

    boolean addFitOverlay(String analysisId) {
        if (!supportsAddFitOverlay()) return false;
        var analysis = availableFitAnalyses().stream()
                .filter(value -> value.id().equals(analysisId)).findFirst();
        if (analysis.isEmpty()
                || analysisEngine.evaluate(session.current().document(), analysis.orElseThrow())
                        .result().isEmpty()) return false;
        var index = selectedPlotIndex();
        var blocks = new ArrayList<>(session.current().document().blocks());
        var plot = selectedPlotBlock().orElseThrow();
        if (plot.definition().series().stream()
                .anyMatch(series -> series.fitAnalysisId().filter(analysisId::equals).isPresent())) return false;
        var series = new ArrayList<>(plot.definition().series());
        series.add(PlotSeries.fit("Fit: " + analysisId, analysisId));
        var definition = plot.definition();
        var updated = new PlotBlock(new PlotDefinition(definition.title(), definition.xAxis(),
                definition.yAxis(), series, definition.legendVisible(),
                definition.gridVisible(), definition.height()));
        blocks.set(index, replacePlotContent(blocks.get(index), updated));
        var state = session.current();
        return session.applyCommandEdit(new EditResult(
                withCurrentDatasets(blocks), state.selection(), state.explicitTypingMarks(), true));
    }

    ComputationSnapshot computations() {
        return computationEngine.update(session.current().document());
    }

    boolean supportsInsertComputation() {
        var state = session.current();
        return !state.isEquationEditingSelection() && !state.isTableEditingSelection()
                && !state.isPlotEditingSelection() && !state.isDiagramEditingSelection()
                && !state.isFigureCaptionSelection()
                && session.commandEditor().supportsInsertBlock(state);
    }

    boolean insertVariable(String name, ScientificValue value, Optional<String> label) {
        if (!supportsInsertComputation()) return false;
        var state = session.current();
        var existing = state.document().blocks().stream()
                .filter(VariableDefinition.class::isInstance).map(VariableDefinition.class::cast)
                .map(VariableDefinition::id).collect(java.util.stream.Collectors.toSet());
        var id = dev.rgcb.scholar.document.StableIdAllocator.firstFree("variable-" + name, existing);
        return session.applyCommandEdit(bindNewlyAvailableVariables(
                session.commandEditor().insertBlock(
                        state, new VariableDefinition(id, name, value, label))));
    }

    boolean insertComputedResult(
            String source,
            Optional<String> label,
            Optional<UnitExpression> displayUnit,
            NumberNotation notation
    ) {
        if (!supportsInsertComputation()) return false;
        var expression = new ExpressionParser().parse(source, session.current().document()).expression();
        if (!compatibleDisplay(expression, displayUnit)) return false;
        return session.applyCommandEdit(session.commandEditor().insertBlock(session.current(),
                new ComputedResult(expression, source, label, displayUnit, notation)));
    }

    boolean supportsEditVariable() {
        var state = session.current();
        return state.isBlockSelection()
                && state.document().blocks().get(state.blockSelection().blockIndex()) instanceof VariableDefinition;
    }

    boolean supportsEditComputedResult() {
        var state = session.current();
        return state.isBlockSelection()
                && state.document().blocks().get(state.blockSelection().blockIndex()) instanceof ComputedResult;
    }

    boolean editVariable(String name, ScientificValue value, Optional<String> label) {
        if (!supportsEditVariable()) return false;
        var state = session.current();
        var index = state.blockSelection().blockIndex();
        var prior = (VariableDefinition) state.document().blocks().get(index);
        var replacement = new VariableDefinition(prior.id(), name, value, label);
        if (prior.equals(replacement)) return false;
        var blocks = new ArrayList<>(state.document().blocks());
        blocks.set(index, replacement);
        return session.applyCommandEdit(bindNewlyAvailableVariables(new EditResult(
                withCurrentDatasets(blocks), state.selection(), state.explicitTypingMarks(), true)));
    }

    boolean editComputedResult(
            String source,
            Optional<String> label,
            Optional<UnitExpression> displayUnit,
            NumberNotation notation
    ) {
        if (!supportsEditComputedResult()) return false;
        var index = session.current().blockSelection().blockIndex();
        var expression = new ExpressionParser().parse(source, session.current().document()).expression();
        if (!compatibleDisplay(expression, displayUnit)) return false;
        return replaceComputationBlock(
                index, new ComputedResult(expression, source, label, displayUnit, notation));
    }

    private boolean compatibleDisplay(Expression expression, Optional<UnitExpression> displayUnit) {
        if (displayUnit.isEmpty()) return true;
        var evaluated = new ExpressionEvaluator().evaluate(expression, session.current().document());
        if (evaluated.value().isEmpty()) return true;
        if (!(evaluated.value().orElseThrow() instanceof ScientificValue.Physical physical)) return false;
        try {
            var target = displayUnit.orElseThrow();
            var converter = new dev.rgcb.scholar.quantity.UnitConverter();
            if (physical.value() instanceof MeasuredQuantity measured) converter.convert(measured, target);
            else converter.convert(physical.value().nominal(), target);
            return true;
        } catch (IllegalArgumentException failure) {
            return false;
        }
    }

    private boolean replaceComputationBlock(int index, BlockNode replacement) {
        var state = session.current();
        var blocks = new ArrayList<>(state.document().blocks());
        if (blocks.get(index).equals(replacement)) return false;
        blocks.set(index, replacement);
        return session.applyCommandEdit(new EditResult(
                withCurrentDatasets(blocks), state.selection(), state.explicitTypingMarks(), true));
    }

    private EditResult bindNewlyAvailableVariables(EditResult edit) {
        if (!edit.changed()) return edit;
        var document = edit.document();
        var blocks = new ArrayList<>(document.blocks());
        var binder = new ExpressionBinder();
        var changed = false;
        for (var index = 0; index < blocks.size(); index++) {
            if (!(blocks.get(index) instanceof ComputedResult computed)) continue;
            var bound = binder.bindAvailable(computed.expression(), document);
            if (!bound.equals(computed.expression())) {
                blocks.set(index, computed.withExpression(bound, computed.authoredSource()));
                changed = true;
            }
        }
        if (!changed) return edit;
        return new EditResult(new Document(blocks, document.datasets(), document.settings()),
                edit.selection(), edit.explicitTypingMarks(), true);
    }

    private int selectedPlotIndex() {
        if (session.current().isPlotEditingSelection()) {
            return session.current().plotEditingSelection().blockIndex();
        }
        if (session.current().isBlockSelection()) return session.current().blockSelection().blockIndex();
        return -1;
    }

    private Optional<PlotBlock> selectedPlotBlock() {
        var index = selectedPlotIndex();
        if (index < 0 || index >= session.current().document().blocks().size()) return Optional.empty();
        var block = session.current().document().blocks().get(index);
        if (block instanceof PlotBlock plot) return Optional.of(plot);
        if (block instanceof FigureBlock figure && figure.content() instanceof PlotBlock plot) {
            return Optional.of(plot);
        }
        return Optional.empty();
    }

    private Document withCurrentDatasets(List<BlockNode> blocks) {
        var current = session.current().document();
        return new Document(blocks, current.datasets(), current.settings());
    }

    private static BlockNode replacePlotContent(BlockNode block, PlotBlock plot) {
        return block instanceof FigureBlock figure ? figure.withContent(plot) : plot;
    }
}
