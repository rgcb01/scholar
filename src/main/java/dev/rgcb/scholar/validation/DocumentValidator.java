package dev.rgcb.scholar.validation;

import dev.rgcb.scholar.data.DatasetPlotBinding;
import dev.rgcb.scholar.data.DatasetTableBinding;
import dev.rgcb.scholar.data.ScientificDataset;
import dev.rgcb.scholar.data.DatasetColumnType;
import dev.rgcb.scholar.diagram.DiagramDefinition;
import dev.rgcb.scholar.diagram.DiagramElement;
import dev.rgcb.scholar.diagram.DiagramElementId;
import dev.rgcb.scholar.document.BlockNode;
import dev.rgcb.scholar.document.CrossReference;
import dev.rgcb.scholar.document.CrossReferenceResolver;
import dev.rgcb.scholar.document.DiagramBlock;
import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.EquationBlock;
import dev.rgcb.scholar.document.FigureBlock;
import dev.rgcb.scholar.document.Heading;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.InlineNode;
import dev.rgcb.scholar.document.Paragraph;
import dev.rgcb.scholar.document.PlotBlock;
import dev.rgcb.scholar.document.TableBlock;
import dev.rgcb.scholar.document.TableCell;
import dev.rgcb.scholar.document.Text;
import dev.rgcb.scholar.document.VariableDefinition;
import dev.rgcb.scholar.document.DatasetAnalysisBlock;
import dev.rgcb.scholar.document.QuantityInline;
import dev.rgcb.scholar.quantity.UnitExpression;
import dev.rgcb.scholar.quantity.UnitRegistry;
import dev.rgcb.scholar.math.*;
import dev.rgcb.scholar.mechanical.MechanicalConstraint;
import dev.rgcb.scholar.mechanical.MechanicalPartReference;
import dev.rgcb.scholar.plot.PlotSeries;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class DocumentValidator {
    private final CrossReferenceResolver crossReferenceResolver = new CrossReferenceResolver();
    private final List<DocumentDiagnostic> diagnostics = new ArrayList<>();
    private Document document;
    private Map<String, ScientificDataset> datasetsById;
    private final UnitRegistry unitRegistry = UnitRegistry.builtIn();

    private DocumentValidator() {
    }

    public static DocumentValidationResult validate(Document document) {
        var validator = new DocumentValidator();
        return validator.validateDocument(document);
    }

    private DocumentValidationResult validateDocument(Document candidate) {
        if (candidate == null) {
            diagnostics.add(DocumentDiagnostic.error(DocumentDiagnosticCode.NULL_DOCUMENT, "Document is null."));
            return result();
        }
        document = candidate;
        datasetsById = datasetsById(candidate.datasets());

        guard("document settings", this::validateSettings);
        guard("dataset resources", () -> validateDatasets(candidate.datasets()));
        guard("document blocks", this::validateBlocks);
        guard("cross references", this::validateCrossReferences);

        return result();
    }

    private void validateSettings() {
        var settings = document.settings();
        if (settings.pageWidth() <= 0 || settings.pageHeight() <= 0
                || settings.contentWidth() <= 0 || settings.contentHeight() <= 0) {
            diagnostics.add(DocumentDiagnostic.error(
                    DocumentDiagnosticCode.INVALID_PAGE_CONFIGURATION,
                    "Paper dimensions and margins must leave a positive content area."));
        }
        var totalGap = settings.columns().gap().logicalUnits() * (settings.columns().count() - 1);
        if (settings.contentWidth() - totalGap < settings.columns().count()) {
            diagnostics.add(DocumentDiagnostic.error(
                    DocumentDiagnosticCode.INVALID_PAGE_CONFIGURATION,
                    "Column count and gap must leave a positive width for every column."));
        }
    }

    private void validateDatasets(List<ScientificDataset> datasets) {
        var datasetIds = new HashSet<String>();
        for (var dataset : datasets) {
            if (!datasetIds.add(dataset.id())) {
                diagnostics.add(DocumentDiagnostic.error(
                        DocumentDiagnosticCode.DUPLICATE_DATASET_ID,
                        "Dataset id is duplicated: " + dataset.id()));
            }

            var columnIds = new HashSet<String>();
            for (var column : dataset.columns()) {
                if (!columnIds.add(column.id())) {
                    diagnostics.add(DocumentDiagnostic.error(
                            DocumentDiagnosticCode.DUPLICATE_DATASET_COLUMN_ID,
                            "Dataset column id is duplicated inside dataset " + dataset.id() + ": " + column.id()));
                }
                column.unit().ifPresent(unit -> {
                    validateUnit(unit, dataset.id(), column.id());
                    validateQuantitySemantics(column.quantitySemantics(), unit, dataset.id(), column.id());
                });
            }

            for (var rowIndex = 0; rowIndex < dataset.rows().size(); rowIndex++) {
                var row = dataset.rows().get(rowIndex);
                if (row.values().size() != dataset.columns().size()) {
                    diagnostics.add(DocumentDiagnostic.error(
                            DocumentDiagnosticCode.INVALID_DATASET_ROW_WIDTH,
                            "Dataset row value count does not match column count.",
                            dataset.id(),
                            "dataset row " + rowIndex));
                }
            }
        }
    }

    private void validateBlocks() {
        var figureIds = new HashSet<String>();
        var headingIds = new HashSet<String>();
        var tableIds = new HashSet<String>();
        var equationIds = new HashSet<String>();
        var variableIds = new HashSet<String>();
        var analysisIds = new HashSet<String>();

        for (var blockIndex = 0; blockIndex < document.blocks().size(); blockIndex++) {
            var block = document.blocks().get(blockIndex);
            if (block instanceof Heading heading) {
                validateHeading(blockIndex, heading, headingIds);
            } else if (block instanceof Paragraph paragraph) {
                validateInline(paragraph.content(), blockIndex, "paragraph");
            } else if (block instanceof FigureBlock figure) {
                validateFigure(blockIndex, figure, figureIds);
            } else if (block instanceof TableBlock table) {
                validateTable(blockIndex, table, tableIds);
            } else if (block instanceof EquationBlock equation) {
                if (equation.id().isPresent()) {
                    addDuplicateIfSeen(equationIds, equation.id().orElseThrow(), DocumentDiagnosticCode.DUPLICATE_EQUATION_ID, blockIndex, "equation");
                }
                validateMath(equation.expression(), blockIndex);
            } else if (block instanceof VariableDefinition variable) {
                addDuplicateIfSeen(variableIds, variable.id(), DocumentDiagnosticCode.DUPLICATE_VARIABLE_ID, blockIndex, "variable");
            } else if (block instanceof DatasetAnalysisBlock analysis) {
                addDuplicateIfSeen(analysisIds, analysis.id(), DocumentDiagnosticCode.DUPLICATE_ANALYSIS_ID, blockIndex, "analysis");
                validateAnalysis(blockIndex, analysis);
            } else if (block instanceof PlotBlock plot) {
                validatePlot(blockIndex, plot);
            } else if (block instanceof DiagramBlock diagram) {
                validateDiagram(blockIndex, diagram.definition());
            } else if (block instanceof dev.rgcb.scholar.document.LayoutSectionBreak sectionBreak) {
                var columns = sectionBreak.columnLayout();
                var totalGap = columns.gap().logicalUnits() * (columns.count() - 1);
                if (document.settings().contentWidth() - totalGap < columns.count()) {
                    diagnostics.add(DocumentDiagnostic.error(
                            DocumentDiagnosticCode.INVALID_PAGE_CONFIGURATION,
                            "Layout section columns do not fit inside the document content width.",
                            blockIndex,
                            "layout section break"));
                }
            }
        }
    }

    private void validateHeading(int blockIndex, Heading heading, Set<String> headingIds) {
        if (heading.level() < 1 || heading.level() > 6) {
            diagnostics.add(DocumentDiagnostic.error(
                    DocumentDiagnosticCode.INVALID_HEADING_LEVEL,
                    "Heading level must be between 1 and 6.",
                    blockIndex,
                    "heading"));
        }
        heading.id().ifPresent(id -> addDuplicateIfSeen(headingIds, id, DocumentDiagnosticCode.DUPLICATE_HEADING_ID, blockIndex, "heading"));
        validateInline(heading.content(), blockIndex, "heading");
    }

    private void validateFigure(int blockIndex, FigureBlock figure, Set<String> figureIds) {
        addDuplicateIfSeen(figureIds, figure.id(), DocumentDiagnosticCode.DUPLICATE_FIGURE_ID, blockIndex, "figure");
        if (!FigureBlock.supportsContent(figure.content())) {
            diagnostics.add(DocumentDiagnostic.error(
                    DocumentDiagnosticCode.INVALID_FIGURE_CONTENT,
                    "Figure content must be a supported scientific visual block.",
                    blockIndex,
                    figure.id(),
                    "figure"));
        }
        validateInline(figure.caption(), blockIndex, "figure caption");
        validateNestedFigureContent(blockIndex, figure.content());
    }

    private void validateNestedFigureContent(int blockIndex, BlockNode content) {
        if (content instanceof PlotBlock plot) {
            validatePlot(blockIndex, plot);
        } else if (content instanceof DiagramBlock diagram) {
            validateDiagram(blockIndex, diagram.definition());
        }
    }

    private void validateTable(int blockIndex, TableBlock table, Set<String> tableIds) {
        table.id().ifPresent(id -> addDuplicateIfSeen(tableIds, id, DocumentDiagnosticCode.DUPLICATE_TABLE_ID, blockIndex, "table"));
        validateTableShape(blockIndex, table);
        table.datasetBinding().ifPresent(binding -> validateTableBinding(blockIndex, binding));

        for (var rowIndex = 0; rowIndex < table.rows().size(); rowIndex++) {
            for (var columnIndex = 0; columnIndex < table.rows().get(rowIndex).cells().size(); columnIndex++) {
                validateCell(table.rows().get(rowIndex).cells().get(columnIndex), blockIndex, "table cell " + rowIndex + "," + columnIndex);
            }
        }
    }

    private void validateTableShape(int blockIndex, TableBlock table) {
        if (table.rows().isEmpty()) {
            diagnostics.add(DocumentDiagnostic.error(DocumentDiagnosticCode.INVALID_TABLE_SHAPE, "Table must contain at least one row.", blockIndex, "table"));
            return;
        }
        var columnCount = table.rows().get(0).cells().size();
        if (columnCount == 0) {
            diagnostics.add(DocumentDiagnostic.error(DocumentDiagnosticCode.INVALID_TABLE_SHAPE, "Table must contain at least one column.", blockIndex, "table"));
        }
        if (table.headerRowCount() < 0 || table.headerRowCount() > 1 || table.headerRowCount() > table.rows().size()) {
            diagnostics.add(DocumentDiagnostic.error(DocumentDiagnosticCode.INVALID_TABLE_SHAPE, "Table header row count is outside the supported range.", blockIndex, "table"));
        }
        for (var rowIndex = 0; rowIndex < table.rows().size(); rowIndex++) {
            if (table.rows().get(rowIndex).cells().size() != columnCount) {
                diagnostics.add(DocumentDiagnostic.error(DocumentDiagnosticCode.INVALID_TABLE_SHAPE, "Table rows must have equal cell counts.", blockIndex, "table row " + rowIndex));
            }
        }
    }

    private void validateCell(TableCell cell, int blockIndex, String context) {
        validateInline(cell.content().content(), blockIndex, context);
    }

    private void validateTableBinding(int blockIndex, DatasetTableBinding binding) {
        var dataset = dataset(binding.datasetId(), blockIndex, "table binding");
        var selectedColumnIds = new HashSet<String>();
        for (var columnId : binding.columnIds()) {
            if (!selectedColumnIds.add(columnId)) {
                diagnostics.add(DocumentDiagnostic.error(
                        DocumentDiagnosticCode.DUPLICATE_DATASET_BINDING_COLUMN_ID,
                        "Dataset-backed table repeats selected column id: " + columnId,
                        blockIndex,
                        columnId,
                        "table binding"));
            }
            if (dataset != null && dataset.column(columnId).isEmpty()) {
                diagnostics.add(DocumentDiagnostic.warning(
                        DocumentDiagnosticCode.MISSING_DATASET_COLUMN,
                        "Dataset-backed table references a missing dataset column: " + columnId,
                        blockIndex,
                        columnId,
                        "table binding"));
            }
        }
    }

    private void validateAnalysis(int blockIndex, DatasetAnalysisBlock analysis) {
        var source = datasetsById.get(analysis.datasetId());
        if (source == null) {
            diagnostics.add(DocumentDiagnostic.warning(DocumentDiagnosticCode.MISSING_ANALYSIS_DEPENDENCY,
                    "Analysis dataset is unavailable.", blockIndex, analysis.id(), "analysis"));
            return;
        }
        var y = source.column(analysis.yColumnId());
        var x = analysis.xColumnId().flatMap(source::column);
        if (y.isEmpty() || analysis.kind().isFit() && x.isEmpty()) {
            diagnostics.add(DocumentDiagnostic.warning(DocumentDiagnosticCode.MISSING_ANALYSIS_DEPENDENCY,
                    "Analysis column is unavailable.", blockIndex, analysis.id(), "analysis"));
            return;
        }
        var yColumn = y.orElseThrow();
        if (yColumn.type() != DatasetColumnType.NUMBER
                || x.filter(column -> column.type() != DatasetColumnType.NUMBER).isPresent()
                || analysis.displayUnit().isPresent() && (yColumn.unit().isEmpty()
                || !yColumn.unit().orElseThrow().compatibleWith(analysis.displayUnit().orElseThrow(), unitRegistry))
                || analysis.kind().isFit() && (yColumn.quantitySemantics().isAbsoluteTemperature()
                || x.orElseThrow().quantitySemantics().isAbsoluteTemperature())) {
            diagnostics.add(DocumentDiagnostic.warning(DocumentDiagnosticCode.INVALID_ANALYSIS_RESULT,
                    "Analysis has incompatible numeric or unit configuration.", blockIndex, analysis.id(), "analysis"));
        }
    }

    private void validatePlot(int blockIndex, PlotBlock plot) {
        for (var seriesIndex = 0; seriesIndex < plot.definition().series().size(); seriesIndex++) {
            validatePlotSeriesBinding(blockIndex, seriesIndex, plot.definition().series().get(seriesIndex));
        }
        validateAxisUnits(blockIndex, plot, true);
        validateAxisUnits(blockIndex, plot, false);
    }

    private void validatePlotSeriesBinding(int blockIndex, int seriesIndex, PlotSeries series) {
        series.fitAnalysisId().ifPresent(id -> {
            var analysis = document.blocks().stream().filter(block -> block instanceof DatasetAnalysisBlock value
                    && value.id().equals(id)).map(block -> (DatasetAnalysisBlock) block).findFirst();
            if (analysis.isEmpty()) {
                diagnostics.add(DocumentDiagnostic.warning(DocumentDiagnosticCode.MISSING_ANALYSIS_DEPENDENCY,
                        "Fitted plot series references a missing analysis: " + id, blockIndex, id, "plot fit"));
            } else if (!analysis.orElseThrow().kind().isFit()) {
                diagnostics.add(DocumentDiagnostic.warning(DocumentDiagnosticCode.INVALID_ANALYSIS_RESULT,
                        "Plot fit requires a regression analysis.", blockIndex, id, "plot fit"));
            }
        });
        series.datasetBinding().ifPresent(binding -> {
            var dataset = dataset(binding.datasetId(), blockIndex, "plot series " + seriesIndex);
            if (dataset == null) {
                return;
            }
            validatePlotColumn(blockIndex, binding.xColumnId(), dataset, "plot series " + seriesIndex + " x column");
            validatePlotColumn(blockIndex, binding.yColumnId(), dataset, "plot series " + seriesIndex + " y column");
            var xIndex = dataset.columnIndex(binding.xColumnId());
            var yIndex = dataset.columnIndex(binding.yColumnId());
            if (xIndex >= 0 && yIndex >= 0) {
                for (var rowIndex = 0; rowIndex < dataset.rows().size(); rowIndex++) {
                    var values = dataset.rows().get(rowIndex).values();
                    if (values.get(xIndex).asDouble().filter(v -> !Double.isFinite(v)).isPresent()
                            || values.get(yIndex).asDouble().filter(v -> !Double.isFinite(v)).isPresent()) {
                        diagnostics.add(DocumentDiagnostic.warning(DocumentDiagnosticCode.UNREPRESENTABLE_PLOT_VALUE,
                                "Dataset contains a value outside the finite plot coordinate range; affected points are omitted.",
                                blockIndex, dataset.id(), "plot series " + seriesIndex + " row " + rowIndex));
                        break;
                    }
                }
            }
        });
    }

    private void validatePlotColumn(int blockIndex, String columnId, ScientificDataset dataset, String context) {
        if (dataset.column(columnId).isEmpty()) {
            diagnostics.add(DocumentDiagnostic.warning(
                    DocumentDiagnosticCode.MISSING_DATASET_COLUMN,
                    "Dataset-backed plot references a missing dataset column: " + columnId,
                    blockIndex,
                    columnId,
                    context));
        }
    }

    private void validateDiagram(int blockIndex, DiagramDefinition definition) {
        var elementsById = new HashMap<DiagramElementId, DiagramElement>();
        for (var element : definition.elements()) {
            if (elementsById.putIfAbsent(element.id(), element) != null) {
                diagnostics.add(DocumentDiagnostic.error(
                        DocumentDiagnosticCode.DUPLICATE_DIAGRAM_ELEMENT_ID,
                        "Diagram element id is duplicated: " + element.id().value(),
                        blockIndex,
                        element.id().value(),
                        "diagram"));
            }
            if (isOutsideCanvas(element, definition)) {
                diagnostics.add(DocumentDiagnostic.error(
                        DocumentDiagnosticCode.DIAGRAM_ELEMENT_OUTSIDE_CANVAS,
                        "Diagram element bounds are outside the authored canvas.",
                        blockIndex,
                        element.id().value(),
                        "diagram"));
            }
            validateDuplicatePorts(blockIndex, element);
        }

        for (var connectionIndex = 0; connectionIndex < definition.connections().size(); connectionIndex++) {
            var connection = definition.connections().get(connectionIndex);
            validateEndpoint(blockIndex, elementsById, connection.source().elementId(), connection.source().portId().value(), "connection " + connectionIndex + " source");
            validateEndpoint(blockIndex, elementsById, connection.target().elementId(), connection.target().portId().value(), "connection " + connectionIndex + " target");
        }

        for (var element : definition.elements()) {
            validateMechanicalReferences(blockIndex, elementsById, element);
        }
    }

    private void validateDuplicatePorts(int blockIndex, DiagramElement element) {
        var ids = new HashSet<String>();
        for (var port : element.ports()) {
            if (!ids.add(port.id().value())) {
                diagnostics.add(DocumentDiagnostic.error(
                        DocumentDiagnosticCode.DUPLICATE_DIAGRAM_PORT_ID,
                        "Diagram port id is duplicated within element: " + port.id().value(),
                        blockIndex,
                        element.id().value(),
                        "diagram element ports"));
            }
        }
    }

    private boolean isOutsideCanvas(DiagramElement element, DiagramDefinition definition) {
        var bounds = element.bounds();
        var canvas = definition.canvas();
        var epsilon = 1.0e-9;
        return bounds.x() < -epsilon
                || bounds.y() < -epsilon
                || bounds.right() > canvas.width() + epsilon
                || bounds.bottom() > canvas.height() + epsilon;
    }

    private void validateEndpoint(int blockIndex, Map<DiagramElementId, DiagramElement> elementsById, DiagramElementId elementId, String portId, String context) {
        var element = elementsById.get(elementId);
        if (element == null) {
            diagnostics.add(DocumentDiagnostic.error(
                    DocumentDiagnosticCode.INVALID_DIAGRAM_ENDPOINT,
                    "Diagram endpoint references a missing element: " + elementId.value(),
                    blockIndex,
                    elementId.value(),
                    context));
            return;
        }
        if (element.ports().stream().noneMatch(port -> port.id().value().equals(portId))) {
            diagnostics.add(DocumentDiagnostic.error(
                    DocumentDiagnosticCode.INVALID_DIAGRAM_ENDPOINT,
                    "Diagram endpoint references a missing port: " + elementId.value() + "/" + portId,
                    blockIndex,
                    elementId.value(),
                    context));
        }
    }

    private void validateMechanicalReferences(int blockIndex, Map<DiagramElementId, DiagramElement> elementsById, DiagramElement element) {
        if (element instanceof MechanicalConstraint constraint) {
            warnMissingMechanicalTarget(blockIndex, elementsById, constraint.id(), constraint.subjectId(), DocumentDiagnosticCode.MISSING_MECHANICAL_CONSTRAINT_TARGET, "mechanical constraint subject");
            constraint.peerId().ifPresent(peerId -> warnMissingMechanicalTarget(blockIndex, elementsById, constraint.id(), peerId, DocumentDiagnosticCode.MISSING_MECHANICAL_CONSTRAINT_TARGET, "mechanical constraint peer"));
        } else if (element instanceof MechanicalPartReference reference) {
            warnMissingMechanicalTarget(blockIndex, elementsById, reference.id(), reference.targetId(), DocumentDiagnosticCode.MISSING_MECHANICAL_PART_REFERENCE_TARGET, "mechanical part reference target");
        }
    }

    private void warnMissingMechanicalTarget(
            int blockIndex,
            Map<DiagramElementId, DiagramElement> elementsById,
            DiagramElementId ownerId,
            DiagramElementId targetId,
            DocumentDiagnosticCode code,
            String context
    ) {
        if (!elementsById.containsKey(targetId)) {
            diagnostics.add(DocumentDiagnostic.warning(
                    code,
                    "Diagram element " + ownerId.value() + " references a missing mechanical target: " + targetId.value(),
                    blockIndex,
                    targetId.value(),
                    context));
        }
    }

    private ScientificDataset dataset(String datasetId, int blockIndex, String context) {
        var dataset = datasetsById.get(datasetId);
        if (dataset == null) {
            diagnostics.add(DocumentDiagnostic.warning(
                    DocumentDiagnosticCode.MISSING_DATASET,
                    "Dataset binding references a missing dataset: " + datasetId,
                    blockIndex,
                    datasetId,
                    context));
        }
        return dataset;
    }

    private void validateCrossReferences() {
        for (var blockIndex = 0; blockIndex < document.blocks().size(); blockIndex++) {
            visitBlockInline(document.blocks().get(blockIndex), blockIndex);
        }
    }

    private void visitBlockInline(BlockNode block, int blockIndex) {
        if (block instanceof Paragraph paragraph) {
            validateInlineReferences(paragraph.content(), blockIndex, "paragraph");
        } else if (block instanceof Heading heading) {
            validateInlineReferences(heading.content(), blockIndex, "heading");
        } else if (block instanceof TableBlock table) {
            for (var rowIndex = 0; rowIndex < table.rows().size(); rowIndex++) {
                for (var columnIndex = 0; columnIndex < table.rows().get(rowIndex).cells().size(); columnIndex++) {
                    validateInlineReferences(table.rows().get(rowIndex).cells().get(columnIndex).content().content(), blockIndex, "table cell " + rowIndex + "," + columnIndex);
                }
            }
        } else if (block instanceof FigureBlock figure) {
            validateInlineReferences(figure.caption(), blockIndex, "figure caption");
        }
    }

    private void validateInline(InlineContent content, int blockIndex, String context) {
        for (InlineNode node : content.nodes()) {
            if (!(node instanceof Text) && !(node instanceof CrossReference) && !(node instanceof QuantityInline)) {
                diagnostics.add(DocumentDiagnostic.error(
                        DocumentDiagnosticCode.VALIDATION_FAILURE,
                        "Unsupported inline node type: " + node.getClass().getName(),
                        blockIndex,
                        context));
            }
            if (node instanceof QuantityInline quantity) validateQuantity(quantity.value().nominal(), context);
        }
    }

    private void validateAxisUnits(int blockIndex, PlotBlock plot, boolean xAxis) {
        UnitExpression first = null;
        dev.rgcb.scholar.quantity.QuantitySemantics firstSemantics = null;
        for (var series : plot.definition().series()) {
            if (series.datasetBinding().isEmpty()) continue;
            var binding = series.datasetBinding().orElseThrow();
            var dataset = datasetsById.get(binding.datasetId());
            if (dataset == null) continue;
            var column = dataset.column(xAxis ? binding.xColumnId() : binding.yColumnId());
            if (column.isEmpty() || column.orElseThrow().unit().isEmpty()) continue;
            var unit = column.orElseThrow().unit().orElseThrow();
            var semantics = column.orElseThrow().quantitySemantics();
            if (!validateUnit(unit, "plot axis")) continue;
            if (first == null) { first = unit; firstSemantics = semantics; }
            else if (!first.compatibleWith(unit, unitRegistry) || firstSemantics != semantics) {
                diagnostics.add(DocumentDiagnostic.error(DocumentDiagnosticCode.INCOMPATIBLE_PLOT_UNITS,
                        "Dataset-backed plot series use incompatible " + (xAxis ? "x" : "y") + " dimensions.",
                        blockIndex, "plot axis"));
                return;
            }
        }
        var display = (xAxis ? plot.definition().xAxis() : plot.definition().yAxis()).displayUnit();
        var displaySemantics = (xAxis ? plot.definition().xAxis() : plot.definition().yAxis()).displayUnitSemantics();
        if (display.isPresent() && validateUnit(display.orElseThrow(), "plot display unit")
                && first != null && (!first.compatibleWith(display.orElseThrow(), unitRegistry)
                || displaySemantics.orElseThrow() != firstSemantics)) {
            diagnostics.add(DocumentDiagnostic.error(DocumentDiagnosticCode.INCOMPATIBLE_PLOT_UNITS,
                    "Plot display unit is incompatible with bound dataset columns.", blockIndex, "plot axis"));
        }
    }

    private boolean validateUnit(UnitExpression unit, String... context) {
        try {
            unitRegistry.dimension(unit);
            unitRegistry.scale(unit);
            return true;
        } catch (RuntimeException failure) {
            var message = "Invalid unit expression: " + failure.getMessage();
            diagnostics.add(context.length >= 2
                    ? DocumentDiagnostic.error(DocumentDiagnosticCode.INVALID_UNIT, message, context[0], context[1])
                    : DocumentDiagnostic.error(DocumentDiagnosticCode.INVALID_UNIT, message));
            return false;
        }
    }

    private void validateQuantity(dev.rgcb.scholar.quantity.Quantity quantity, String context) {
        if (validateUnit(quantity.unit(), context)) {
            validateQuantitySemantics(quantity.semantics(), quantity.unit(), context);
        }
    }

    private void validateQuantitySemantics(dev.rgcb.scholar.quantity.QuantitySemantics semantics,
                                           UnitExpression unit, String... context) {
        try {
            semantics.validate(unit);
        } catch (RuntimeException failure) {
            diagnostics.add(context.length >= 2
                    ? DocumentDiagnostic.error(DocumentDiagnosticCode.INVALID_QUANTITY_SEMANTICS,
                            failure.getMessage(), context[0], context[1])
                    : DocumentDiagnostic.error(DocumentDiagnosticCode.INVALID_QUANTITY_SEMANTICS,
                            failure.getMessage()));
        }
    }

    private void validateMath(MathExpression expression, int blockIndex) {
        if (expression instanceof MathQuantity quantity) {
            validateQuantity(quantity.value().nominal(), "equation");
        } else if (expression instanceof MathSequence sequence) {
            sequence.expressions().forEach(child -> validateMath(child, blockIndex));
        } else if (expression instanceof MathFraction fraction) {
            validateMath(fraction.numerator(), blockIndex); validateMath(fraction.denominator(), blockIndex);
        } else if (expression instanceof MathScript script) {
            validateMath(script.base(), blockIndex); script.subscript().ifPresent(child -> validateMath(child, blockIndex));
            script.superscript().ifPresent(child -> validateMath(child, blockIndex));
        } else if (expression instanceof MathRoot root) {
            validateMath(root.radicand(), blockIndex); root.index().ifPresent(child -> validateMath(child, blockIndex));
        } else if (expression instanceof MathGroup group) {
            validateMath(group.content(), blockIndex);
        }
    }

    private void validateInlineReferences(InlineContent content, int blockIndex, String context) {
        for (var node : content.nodes()) {
            if (node instanceof CrossReference reference && !crossReferenceResolver.resolve(document, reference).resolved()) {
                diagnostics.add(DocumentDiagnostic.warning(
                        DocumentDiagnosticCode.MISSING_CROSS_REFERENCE_TARGET,
                        "Cross-reference target is missing: " + reference.kind() + " " + reference.targetId(),
                        blockIndex,
                        reference.targetId(),
                        context));
            }
        }
    }

    private void addDuplicateIfSeen(Set<String> seen, String id, DocumentDiagnosticCode code, int blockIndex, String context) {
        if (!seen.add(id)) {
            diagnostics.add(DocumentDiagnostic.error(code, "Stable id is duplicated in its namespace: " + id, blockIndex, id, context));
        }
    }

    private Map<String, ScientificDataset> datasetsById(List<ScientificDataset> datasets) {
        var map = new HashMap<String, ScientificDataset>();
        for (var dataset : datasets) {
            map.putIfAbsent(dataset.id(), dataset);
        }
        return map;
    }

    private void guard(String context, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException exception) {
            diagnostics.add(DocumentDiagnostic.error(
                    DocumentDiagnosticCode.VALIDATION_FAILURE,
                    "Validation failed while checking " + context + ": " + exception.getMessage()));
        }
    }

    private DocumentValidationResult result() {
        return new DocumentValidationResult(diagnostics);
    }
}
