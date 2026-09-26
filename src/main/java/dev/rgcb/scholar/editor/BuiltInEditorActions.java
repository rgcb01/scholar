package dev.rgcb.scholar.editor;

import dev.rgcb.scholar.document.TextMark;
import dev.rgcb.scholar.electrical.ElectricalComponentKind;
import dev.rgcb.scholar.math.MathDelimiter;
import dev.rgcb.scholar.mechanical.MechanicalPrimitiveKind;
import dev.rgcb.scholar.mechanical.MechanicalSymbolKind;
import dev.rgcb.scholar.mechanical.MechanicalAnnotationKind;
import dev.rgcb.scholar.mechanical.MechanicalDimensionKind;
import dev.rgcb.scholar.mechanical.MechanicalConstraintKind;
import dev.rgcb.scholar.math.editor.ScriptSlot;
import dev.rgcb.scholar.math.editor.SemanticMathTokenKind;
import dev.rgcb.scholar.plot.PlotSeriesKind;
import dev.rgcb.scholar.quantity.NumberNotation;
import dev.rgcb.scholar.quantity.Quantity;
import dev.rgcb.scholar.quantity.QuantitySemantics;
import dev.rgcb.scholar.quantity.UnitExpression;
import dev.rgcb.scholar.quantity.UnitParser;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class BuiltInEditorActions {
    private BuiltInEditorActions() {
    }

    private static String label(EditorActionId id) {
        return BuiltInEditorActionCatalog.require(id).englishLabel();
    }

    private static String tooltip(EditorActionId id) {
        return BuiltInEditorActionCatalog.require(id).englishTooltip();
    }

    private static ActionShortcut shortcut(EditorActionId id) {
        return BuiltInEditorActionCatalog.require(id).shortcut().orElse(null);
    }

    public static List<EditorAction> editMenuActions() {
        return List.of(undo(), redo(), cut(), copy(), paste(), deleteSelection());
    }

    public static List<EditorAction> insertMenuActions() {
        return List.of(
                insertEquation(),
                insertTable(),
                insertPlot(),
                insertDiagram(),
                insertCrossReference(),
                insertTableOfContents(),
                insertPageBreak(),
                insertFraction(),
                insertRoot(),
                insertParenthesesGroup(),
                insertBracketsGroup(),
                insertBracesGroup(),
                insertSuperscript(),
                insertSubscript(),
                convertToNamedOperator(),
                convertToMathText(),
                insertQuantity(EditorActionId.INSERT_QUANTITY_METRE, label(EditorActionId.INSERT_QUANTITY_METRE), "m"),
                insertQuantity(EditorActionId.INSERT_QUANTITY_CELSIUS, label(EditorActionId.INSERT_QUANTITY_CELSIUS), "°C", "25"),
                insertQuantity(EditorActionId.INSERT_QUANTITY_CELSIUS_DIFFERENCE, label(EditorActionId.INSERT_QUANTITY_CELSIUS_DIFFERENCE), "°C", "5",
                        QuantitySemantics.TEMPERATURE_DIFFERENCE),
                insertQuantity(EditorActionId.INSERT_QUANTITY_VOLT, label(EditorActionId.INSERT_QUANTITY_VOLT), "V", "5"),
                insertQuantity(EditorActionId.INSERT_QUANTITY_MILLIAMPERE, label(EditorActionId.INSERT_QUANTITY_MILLIAMPERE), "mA"),
                insertQuantity(EditorActionId.INSERT_QUANTITY_KILOOHM, label(EditorActionId.INSERT_QUANTITY_KILOOHM), "kΩ"),
                insertQuantity(EditorActionId.INSERT_QUANTITY_ACCELERATION, label(EditorActionId.INSERT_QUANTITY_ACCELERATION), "m/s²", "9.81"),
                convertQuantity(EditorActionId.QUANTITY_CONVERT_METRE, label(EditorActionId.QUANTITY_CONVERT_METRE), "m"),
                convertQuantity(EditorActionId.QUANTITY_CONVERT_MILLIMETRE, label(EditorActionId.QUANTITY_CONVERT_MILLIMETRE), "mm"),
                convertQuantity(EditorActionId.QUANTITY_CONVERT_CELSIUS, label(EditorActionId.QUANTITY_CONVERT_CELSIUS), "°C"),
                convertQuantity(EditorActionId.QUANTITY_CONVERT_KELVIN, label(EditorActionId.QUANTITY_CONVERT_KELVIN), "K"),
                quantityNotation(EditorActionId.QUANTITY_FORMAT_DECIMAL, label(EditorActionId.QUANTITY_FORMAT_DECIMAL), NumberNotation.DECIMAL),
                quantityNotation(EditorActionId.QUANTITY_FORMAT_SCIENTIFIC, label(EditorActionId.QUANTITY_FORMAT_SCIENTIFIC), NumberNotation.SCIENTIFIC),
                quantityNotation(EditorActionId.QUANTITY_FORMAT_ENGINEERING, label(EditorActionId.QUANTITY_FORMAT_ENGINEERING), NumberNotation.ENGINEERING),
                computationDialog(EditorActionId.INSERT_VARIABLE, label(EditorActionId.INSERT_VARIABLE), ComputationDialogKind.INSERT_VARIABLE),
                computationDialog(EditorActionId.INSERT_COMPUTED_RESULT, label(EditorActionId.INSERT_COMPUTED_RESULT), ComputationDialogKind.INSERT_RESULT),
                computationDialog(EditorActionId.EDIT_VARIABLE, label(EditorActionId.EDIT_VARIABLE), ComputationDialogKind.EDIT_VARIABLE),
                computationDialog(EditorActionId.EDIT_COMPUTED_RESULT, label(EditorActionId.EDIT_COMPUTED_RESULT), ComputationDialogKind.EDIT_RESULT));
    }

    public static List<EditorAction> formatMenuActions() {
        return List.of(
                paragraph(),
                heading(1),
                heading(2),
                heading(3),
                heading(4),
                heading(5),
                heading(6),
                bold(),
                italic(), underline(), textSuperscript(), textSubscript(),
                semanticStyle(EditorActionId.STYLE_BODY, label(EditorActionId.STYLE_BODY), dev.rgcb.scholar.document.SemanticStyle.BODY_TEXT),
                semanticStyle(EditorActionId.STYLE_TITLE, label(EditorActionId.STYLE_TITLE), dev.rgcb.scholar.document.SemanticStyle.TITLE),
                semanticStyle(EditorActionId.STYLE_SUBTITLE, label(EditorActionId.STYLE_SUBTITLE), dev.rgcb.scholar.document.SemanticStyle.SUBTITLE),
                semanticStyle(EditorActionId.STYLE_AUTHOR, label(EditorActionId.STYLE_AUTHOR), dev.rgcb.scholar.document.SemanticStyle.AUTHOR),
                semanticStyle(EditorActionId.STYLE_AFFILIATION, label(EditorActionId.STYLE_AFFILIATION), dev.rgcb.scholar.document.SemanticStyle.AFFILIATION),
                semanticStyle(EditorActionId.STYLE_ABSTRACT, label(EditorActionId.STYLE_ABSTRACT), dev.rgcb.scholar.document.SemanticStyle.ABSTRACT),
                semanticStyle(EditorActionId.STYLE_KEYWORDS, label(EditorActionId.STYLE_KEYWORDS), dev.rgcb.scholar.document.SemanticStyle.KEYWORDS),
                semanticStyle(EditorActionId.STYLE_REFERENCE, label(EditorActionId.STYLE_REFERENCE), dev.rgcb.scholar.document.SemanticStyle.REFERENCE),
                fontSize(EditorActionId.FONT_SIZE_10, label(EditorActionId.FONT_SIZE_10), 20), fontSize(EditorActionId.FONT_SIZE_12, label(EditorActionId.FONT_SIZE_12), 24), fontSize(EditorActionId.FONT_SIZE_14, label(EditorActionId.FONT_SIZE_14), 28),
                fontFamily(EditorActionId.FONT_SOURCE_SANS, label(EditorActionId.FONT_SOURCE_SANS), dev.rgcb.scholar.document.ScholarFontFamily.SOURCE_SANS_3),
                fontFamily(EditorActionId.FONT_SCIENTIFIC_MATH, label(EditorActionId.FONT_SCIENTIFIC_MATH), dev.rgcb.scholar.document.ScholarFontFamily.SCIENTIFIC_MATH),
                alignment(EditorActionId.ALIGN_LEFT, label(EditorActionId.ALIGN_LEFT), dev.rgcb.scholar.document.ParagraphAlignment.LEFT),
                alignment(EditorActionId.ALIGN_CENTER, label(EditorActionId.ALIGN_CENTER), dev.rgcb.scholar.document.ParagraphAlignment.CENTER),
                alignment(EditorActionId.ALIGN_RIGHT, label(EditorActionId.ALIGN_RIGHT), dev.rgcb.scholar.document.ParagraphAlignment.RIGHT),
                alignment(EditorActionId.ALIGN_JUSTIFIED, label(EditorActionId.ALIGN_JUSTIFIED), dev.rgcb.scholar.document.ParagraphAlignment.JUSTIFIED),
                paragraphCommand(EditorActionId.LINE_SPACING_SINGLE, label(EditorActionId.LINE_SPACING_SINGLE), s -> s.setParagraphLineSpacing(1000)),
                paragraphCommand(EditorActionId.LINE_SPACING_ONE_HALF, label(EditorActionId.LINE_SPACING_ONE_HALF), s -> s.setParagraphLineSpacing(1500)),
                paragraphCommand(EditorActionId.INDENT_DECREASE, label(EditorActionId.INDENT_DECREASE), s -> s.adjustParagraphLeftIndent(-8)),
                paragraphCommand(EditorActionId.INDENT_INCREASE, label(EditorActionId.INDENT_INCREASE), s -> s.adjustParagraphLeftIndent(8)));
    }

    public static List<EditorAction> layoutMenuActions() {
        return List.of(
                settings(EditorActionId.LAYOUT_MARGIN_NORMAL, label(EditorActionId.LAYOUT_MARGIN_NORMAL), s -> new dev.rgcb.scholar.document.DocumentSettings(s.template(), s.paper(), s.orientation(), dev.rgcb.scholar.document.PageMargins.normal(), s.columns(), s.decoration())),
                settings(EditorActionId.LAYOUT_MARGIN_NARROW, label(EditorActionId.LAYOUT_MARGIN_NARROW), s -> new dev.rgcb.scholar.document.DocumentSettings(s.template(), s.paper(), s.orientation(), dev.rgcb.scholar.document.PageMargins.narrow(), s.columns(), s.decoration())),
                settings(EditorActionId.LAYOUT_PORTRAIT, label(EditorActionId.LAYOUT_PORTRAIT), s -> new dev.rgcb.scholar.document.DocumentSettings(s.template(), s.paper(), dev.rgcb.scholar.document.PageOrientation.PORTRAIT, s.margins(), s.columns(), s.decoration())),
                settings(EditorActionId.LAYOUT_LANDSCAPE, label(EditorActionId.LAYOUT_LANDSCAPE), s -> new dev.rgcb.scholar.document.DocumentSettings(s.template(), s.paper(), dev.rgcb.scholar.document.PageOrientation.LANDSCAPE, s.margins(), s.columns(), s.decoration())),
                settings(EditorActionId.LAYOUT_SIZE_LETTER, label(EditorActionId.LAYOUT_SIZE_LETTER), s -> new dev.rgcb.scholar.document.DocumentSettings(s.template(), dev.rgcb.scholar.document.PaperSize.letter(), s.orientation(), s.margins(), s.columns(), s.decoration())),
                settings(EditorActionId.LAYOUT_SIZE_A4, label(EditorActionId.LAYOUT_SIZE_A4), s -> new dev.rgcb.scholar.document.DocumentSettings(s.template(), dev.rgcb.scholar.document.PaperSize.a4(), s.orientation(), s.margins(), s.columns(), s.decoration())),
                settings(EditorActionId.LAYOUT_SIZE_LEGAL, label(EditorActionId.LAYOUT_SIZE_LEGAL), s -> new dev.rgcb.scholar.document.DocumentSettings(s.template(), dev.rgcb.scholar.document.PaperSize.legal(), s.orientation(), s.margins(), s.columns(), s.decoration())),
                settings(EditorActionId.LAYOUT_ONE_COLUMN, label(EditorActionId.LAYOUT_ONE_COLUMN), s -> s.withColumns(dev.rgcb.scholar.document.ColumnLayout.one())),
                settings(EditorActionId.LAYOUT_TWO_COLUMNS, label(EditorActionId.LAYOUT_TWO_COLUMNS), s -> s.withColumns(dev.rgcb.scholar.document.ColumnLayout.two())),
                insertPageBreak());
    }

    public static List<EditorAction> tableMenuActions() {
        return List.of(
                insertTableRowAbove(),
                insertTableRowBelow(),
                deleteTableRow(),
                insertTableColumnLeft(),
                insertTableColumnRight(),
                deleteTableColumn());
    }

    public static List<EditorAction> plotMenuActions() {
        return List.of(
                togglePlotGrid(),
                togglePlotLegend(),
                addPlotLineSeries(),
                addPlotScatterSeries(),
                setPlotSeriesLine(),
                setPlotSeriesScatter(),
                addPlotPoint(),
                deletePlotPoint(),
                deletePlotSeries(),
                plotAxisUnit(EditorActionId.PLOT_X_UNIT_AUTO, label(EditorActionId.PLOT_X_UNIT_AUTO), true, Optional.empty()),
                plotAxisUnit(EditorActionId.PLOT_X_UNIT_SECOND, label(EditorActionId.PLOT_X_UNIT_SECOND), true, Optional.of(unit("s"))),
                plotAxisUnit(EditorActionId.PLOT_X_UNIT_METRE, label(EditorActionId.PLOT_X_UNIT_METRE), true, Optional.of(unit("m"))),
                plotAxisUnit(EditorActionId.PLOT_Y_UNIT_AUTO, label(EditorActionId.PLOT_Y_UNIT_AUTO), false, Optional.empty()),
                plotAxisUnit(EditorActionId.PLOT_Y_UNIT_CELSIUS, label(EditorActionId.PLOT_Y_UNIT_CELSIUS), false, Optional.of(unit("°C"))),
                plotAxisUnit(EditorActionId.PLOT_Y_UNIT_KELVIN, label(EditorActionId.PLOT_Y_UNIT_KELVIN), false, Optional.of(unit("K"))),
                plotAxisUnit(EditorActionId.PLOT_Y_UNIT_CELSIUS_DIFFERENCE, label(EditorActionId.PLOT_Y_UNIT_CELSIUS_DIFFERENCE), false,
                        Optional.of(unit("°C")), Optional.of(QuantitySemantics.TEMPERATURE_DIFFERENCE)),
                plotAxisUnit(EditorActionId.PLOT_Y_UNIT_KELVIN_DIFFERENCE, label(EditorActionId.PLOT_Y_UNIT_KELVIN_DIFFERENCE), false,
                        Optional.of(unit("K")), Optional.of(QuantitySemantics.TEMPERATURE_DIFFERENCE)),
                plotAxisUnit(EditorActionId.PLOT_Y_UNIT_VOLT, label(EditorActionId.PLOT_Y_UNIT_VOLT), false, Optional.of(unit("V"))));
    }

    public static List<EditorAction> diagramMenuActions() {
        return List.of(
                addDiagramNode(),
                addElectricalComponent(EditorActionId.DIAGRAM_ADD_RESISTOR, label(EditorActionId.DIAGRAM_ADD_RESISTOR), ElectricalComponentKind.RESISTOR),
                addElectricalComponent(EditorActionId.DIAGRAM_ADD_CAPACITOR, label(EditorActionId.DIAGRAM_ADD_CAPACITOR), ElectricalComponentKind.CAPACITOR),
                addElectricalComponent(EditorActionId.DIAGRAM_ADD_DC_VOLTAGE_SOURCE, label(EditorActionId.DIAGRAM_ADD_DC_VOLTAGE_SOURCE), ElectricalComponentKind.DC_VOLTAGE_SOURCE),
                addElectricalComponent(EditorActionId.DIAGRAM_ADD_GROUND, label(EditorActionId.DIAGRAM_ADD_GROUND), ElectricalComponentKind.GROUND),
                addElectricalComponent(EditorActionId.DIAGRAM_ADD_DIODE, label(EditorActionId.DIAGRAM_ADD_DIODE), ElectricalComponentKind.DIODE),
                addElectricalComponent(EditorActionId.DIAGRAM_ADD_LED, label(EditorActionId.DIAGRAM_ADD_LED), ElectricalComponentKind.LED),
                addElectricalComponent(EditorActionId.DIAGRAM_ADD_SWITCH_SPST, label(EditorActionId.DIAGRAM_ADD_SWITCH_SPST), ElectricalComponentKind.SWITCH_SPST),
                addElectricalJunction(),
                addMechanicalPrimitive(EditorActionId.DIAGRAM_ADD_MECHANICAL_LINE, label(EditorActionId.DIAGRAM_ADD_MECHANICAL_LINE), MechanicalPrimitiveKind.LINE),
                addMechanicalPrimitive(EditorActionId.DIAGRAM_ADD_MECHANICAL_CENTERLINE, label(EditorActionId.DIAGRAM_ADD_MECHANICAL_CENTERLINE), MechanicalPrimitiveKind.CENTERLINE),
                addMechanicalPrimitive(EditorActionId.DIAGRAM_ADD_MECHANICAL_RECTANGLE, label(EditorActionId.DIAGRAM_ADD_MECHANICAL_RECTANGLE), MechanicalPrimitiveKind.RECTANGLE),
                addMechanicalPrimitive(EditorActionId.DIAGRAM_ADD_MECHANICAL_CIRCLE, label(EditorActionId.DIAGRAM_ADD_MECHANICAL_CIRCLE), MechanicalPrimitiveKind.CIRCLE),
                addMechanicalPrimitive(EditorActionId.DIAGRAM_ADD_MECHANICAL_ARC, label(EditorActionId.DIAGRAM_ADD_MECHANICAL_ARC), MechanicalPrimitiveKind.ARC),
                addMechanicalPrimitive(EditorActionId.DIAGRAM_ADD_MECHANICAL_ARROW, label(EditorActionId.DIAGRAM_ADD_MECHANICAL_ARROW), MechanicalPrimitiveKind.ARROW),
                addMechanicalPrimitive(EditorActionId.DIAGRAM_ADD_MECHANICAL_REFERENCE_POINT, label(EditorActionId.DIAGRAM_ADD_MECHANICAL_REFERENCE_POINT), MechanicalPrimitiveKind.REFERENCE_POINT),
                addMechanicalPartReference(),
                generateMechanicalBom(),
                addMechanicalAnnotation(EditorActionId.DIAGRAM_ADD_MECHANICAL_ANNOTATION_PART_LABEL, label(EditorActionId.DIAGRAM_ADD_MECHANICAL_ANNOTATION_PART_LABEL), MechanicalAnnotationKind.PART_LABEL),
                addMechanicalAnnotation(EditorActionId.DIAGRAM_ADD_MECHANICAL_ANNOTATION_NOTE, label(EditorActionId.DIAGRAM_ADD_MECHANICAL_ANNOTATION_NOTE), MechanicalAnnotationKind.NOTE),
                addMechanicalAnnotation(EditorActionId.DIAGRAM_ADD_MECHANICAL_ANNOTATION_LEADER, label(EditorActionId.DIAGRAM_ADD_MECHANICAL_ANNOTATION_LEADER), MechanicalAnnotationKind.LEADER),
                addMechanicalSymbol(EditorActionId.DIAGRAM_ADD_MECHANICAL_SYMBOL_SHAFT, label(EditorActionId.DIAGRAM_ADD_MECHANICAL_SYMBOL_SHAFT), MechanicalSymbolKind.SHAFT),
                addMechanicalSymbol(EditorActionId.DIAGRAM_ADD_MECHANICAL_SYMBOL_GEAR, label(EditorActionId.DIAGRAM_ADD_MECHANICAL_SYMBOL_GEAR), MechanicalSymbolKind.GEAR),
                addMechanicalSymbol(EditorActionId.DIAGRAM_ADD_MECHANICAL_SYMBOL_BEARING, label(EditorActionId.DIAGRAM_ADD_MECHANICAL_SYMBOL_BEARING), MechanicalSymbolKind.BEARING),
                addMechanicalSymbol(EditorActionId.DIAGRAM_ADD_MECHANICAL_SYMBOL_SPRING, label(EditorActionId.DIAGRAM_ADD_MECHANICAL_SYMBOL_SPRING), MechanicalSymbolKind.SPRING),
                addMechanicalSymbol(EditorActionId.DIAGRAM_ADD_MECHANICAL_SYMBOL_PISTON, label(EditorActionId.DIAGRAM_ADD_MECHANICAL_SYMBOL_PISTON), MechanicalSymbolKind.PISTON),
                addMechanicalSymbol(EditorActionId.DIAGRAM_ADD_MECHANICAL_SYMBOL_BOLT, label(EditorActionId.DIAGRAM_ADD_MECHANICAL_SYMBOL_BOLT), MechanicalSymbolKind.BOLT),
                addMechanicalDimension(EditorActionId.DIAGRAM_ADD_MECHANICAL_DIMENSION_HORIZONTAL, label(EditorActionId.DIAGRAM_ADD_MECHANICAL_DIMENSION_HORIZONTAL), MechanicalDimensionKind.HORIZONTAL),
                addMechanicalDimension(EditorActionId.DIAGRAM_ADD_MECHANICAL_DIMENSION_VERTICAL, label(EditorActionId.DIAGRAM_ADD_MECHANICAL_DIMENSION_VERTICAL), MechanicalDimensionKind.VERTICAL),
                addMechanicalDimension(EditorActionId.DIAGRAM_ADD_MECHANICAL_DIMENSION_ALIGNED, label(EditorActionId.DIAGRAM_ADD_MECHANICAL_DIMENSION_ALIGNED), MechanicalDimensionKind.ALIGNED),
                addMechanicalDimension(EditorActionId.DIAGRAM_ADD_MECHANICAL_DIMENSION_RADIUS, label(EditorActionId.DIAGRAM_ADD_MECHANICAL_DIMENSION_RADIUS), MechanicalDimensionKind.RADIUS),
                addMechanicalDimension(EditorActionId.DIAGRAM_ADD_MECHANICAL_DIMENSION_DIAMETER, label(EditorActionId.DIAGRAM_ADD_MECHANICAL_DIMENSION_DIAMETER), MechanicalDimensionKind.DIAMETER),
                addMechanicalDimension(EditorActionId.DIAGRAM_ADD_MECHANICAL_DIMENSION_ANGLE, label(EditorActionId.DIAGRAM_ADD_MECHANICAL_DIMENSION_ANGLE), MechanicalDimensionKind.ANGLE),
                addMechanicalUnaryConstraint(EditorActionId.DIAGRAM_ADD_MECHANICAL_CONSTRAINT_HORIZONTAL, label(EditorActionId.DIAGRAM_ADD_MECHANICAL_CONSTRAINT_HORIZONTAL), MechanicalConstraintKind.HORIZONTAL),
                addMechanicalUnaryConstraint(EditorActionId.DIAGRAM_ADD_MECHANICAL_CONSTRAINT_VERTICAL, label(EditorActionId.DIAGRAM_ADD_MECHANICAL_CONSTRAINT_VERTICAL), MechanicalConstraintKind.VERTICAL),
                startMechanicalConstraint(EditorActionId.DIAGRAM_START_MECHANICAL_CONSTRAINT_COINCIDENT, label(EditorActionId.DIAGRAM_START_MECHANICAL_CONSTRAINT_COINCIDENT), MechanicalConstraintKind.COINCIDENT),
                startMechanicalConstraint(EditorActionId.DIAGRAM_START_MECHANICAL_CONSTRAINT_PARALLEL, label(EditorActionId.DIAGRAM_START_MECHANICAL_CONSTRAINT_PARALLEL), MechanicalConstraintKind.PARALLEL),
                startMechanicalConstraint(EditorActionId.DIAGRAM_START_MECHANICAL_CONSTRAINT_PERPENDICULAR, label(EditorActionId.DIAGRAM_START_MECHANICAL_CONSTRAINT_PERPENDICULAR), MechanicalConstraintKind.PERPENDICULAR),
                startMechanicalConstraint(EditorActionId.DIAGRAM_START_MECHANICAL_CONSTRAINT_CONCENTRIC, label(EditorActionId.DIAGRAM_START_MECHANICAL_CONSTRAINT_CONCENTRIC), MechanicalConstraintKind.CONCENTRIC),
                finishMechanicalConstraint(),
                cancelMechanicalConstraint(),
                scaleElectricalSymbolsDown(),
                scaleElectricalSymbolsUp(),
                shortenDiagramWorkspace(),
                heightenDiagramWorkspace(),
                resetDiagramWorkspaceHeight(),
                rotateElectricalComponentClockwise(),
                rotateElectricalComponentCounterClockwise(),
                deleteElectricalComponent(),
                deleteElectricalJunction(),
                deleteMechanicalConstraint(),
                deleteMechanicalDimension(),
                deleteMechanicalPartReference(),
                deleteMechanicalAnnotation(),
                deleteMechanicalSymbol(),
                deleteMechanicalPrimitive(),
                deleteDiagramNode(),
                startDiagramConnection(),
                finishDiagramConnection(),
                cancelDiagramConnection(),
                deleteDiagramConnection());
    }

    public static List<EditorAction> figureMenuActions() {
        return List.of(
                wrapPlotInFigure(),
                wrapDiagramInFigure(),
                editFigureCaption(),
                unwrapFigure());
    }

    public static List<EditorAction> viewMenuActions() {
        return List.of(toggleOutline());
    }

    public static List<EditorAction> dataMenuActions() {
        return List.of(newDataset(), insertDatasetTable(), bindPlotToDataset(),
                computationDialog(EditorActionId.DATA_INSERT_ANALYSIS, label(EditorActionId.DATA_INSERT_ANALYSIS), ComputationDialogKind.INSERT_ANALYSIS),
                computationDialog(EditorActionId.DATA_EDIT_ANALYSIS, label(EditorActionId.DATA_EDIT_ANALYSIS), ComputationDialogKind.EDIT_ANALYSIS),
                computationDialog(EditorActionId.DATA_ADD_FIT_OVERLAY, label(EditorActionId.DATA_ADD_FIT_OVERLAY), ComputationDialogKind.ADD_FIT_OVERLAY),
                datasetColumnUnit(EditorActionId.DATA_COLUMN_UNIT_NONE, label(EditorActionId.DATA_COLUMN_UNIT_NONE), Optional.empty()),
                datasetColumnUnit(EditorActionId.DATA_COLUMN_UNIT_METRE, label(EditorActionId.DATA_COLUMN_UNIT_METRE), Optional.of(unit("m"))),
                datasetColumnUnit(EditorActionId.DATA_COLUMN_UNIT_SECOND, label(EditorActionId.DATA_COLUMN_UNIT_SECOND), Optional.of(unit("s"))),
                datasetColumnUnit(EditorActionId.DATA_COLUMN_UNIT_CELSIUS, label(EditorActionId.DATA_COLUMN_UNIT_CELSIUS), Optional.of(unit("°C"))),
                datasetColumnUnit(EditorActionId.DATA_COLUMN_UNIT_KELVIN, label(EditorActionId.DATA_COLUMN_UNIT_KELVIN), Optional.of(unit("K"))),
                datasetColumnUnit(EditorActionId.DATA_COLUMN_UNIT_CELSIUS_DIFFERENCE, label(EditorActionId.DATA_COLUMN_UNIT_CELSIUS_DIFFERENCE),
                        Optional.of(unit("°C")), QuantitySemantics.TEMPERATURE_DIFFERENCE),
                datasetColumnUnit(EditorActionId.DATA_COLUMN_UNIT_KELVIN_DIFFERENCE, label(EditorActionId.DATA_COLUMN_UNIT_KELVIN_DIFFERENCE),
                        Optional.of(unit("K")), QuantitySemantics.TEMPERATURE_DIFFERENCE),
                datasetColumnUnit(EditorActionId.DATA_COLUMN_UNIT_VOLT, label(EditorActionId.DATA_COLUMN_UNIT_VOLT), Optional.of(unit("V"))),
                datasetColumnUnit(EditorActionId.DATA_COLUMN_UNIT_AMPERE, label(EditorActionId.DATA_COLUMN_UNIT_AMPERE), Optional.of(unit("A"))),
                datasetColumnUnit(EditorActionId.DATA_COLUMN_UNIT_OHM, label(EditorActionId.DATA_COLUMN_UNIT_OHM), Optional.of(unit("Ω"))));
    }

    private static EditorAction insertQuantity(EditorActionId id, String label, String unit) {
        return insertQuantity(id, label, unit, "1");
    }

    private static EditorAction insertQuantity(EditorActionId id, String label, String unit, String value) {
        return insertQuantity(id, label, unit, value, QuantitySemantics.defaultFor(unit(unit)));
    }

    private static EditorAction insertQuantity(EditorActionId id, String label, String unit, String value,
                                               QuantitySemantics semantics) {
        return new SimpleAction(id, label, tooltip(id), null,
                context -> context.session().supportsInsertQuantity(),
                context -> context.session().insertQuantity(new Quantity(new BigDecimal(value), unit(unit), semantics), NumberNotation.DECIMAL)
                        ? EditorActionResult.DOCUMENT_CHANGED : EditorActionResult.NONE);
    }

    private static EditorAction datasetColumnUnit(EditorActionId id, String label, Optional<UnitExpression> unit) {
        return datasetColumnUnit(id, label, unit,
                unit.map(QuantitySemantics::defaultFor).orElse(QuantitySemantics.LINEAR));
    }

    private static EditorAction datasetColumnUnit(EditorActionId id, String label, Optional<UnitExpression> unit,
                                                  QuantitySemantics semantics) {
        return new SimpleAction(id, label, tooltip(id), null,
                context -> context.session().supportsSetSelectedDatasetColumnUnit(),
                context -> context.session().setSelectedDatasetColumnUnit(unit, semantics)
                        ? EditorActionResult.DOCUMENT_CHANGED : EditorActionResult.NONE);
    }

    private static EditorAction convertQuantity(EditorActionId id, String label, String target) {
        var unit = unit(target);
        return new SimpleAction(id, label, tooltip(id), null,
                context -> context.session().supportsConvertQuantityAtCaret(unit),
                context -> context.session().convertQuantityAtCaret(unit)
                        ? EditorActionResult.DOCUMENT_CHANGED : EditorActionResult.NONE);
    }

    private static EditorAction quantityNotation(EditorActionId id, String label, NumberNotation notation) {
        return new SimpleAction(id, label, tooltip(id), null,
                context -> context.session().supportsEditQuantityAtCaret(),
                context -> context.session().setQuantityNotationAtCaret(notation)
                        ? EditorActionResult.DOCUMENT_CHANGED : EditorActionResult.NONE);
    }

    private static EditorAction plotAxisUnit(EditorActionId id, String label, boolean xAxis, Optional<UnitExpression> unit) {
        return plotAxisUnit(id, label, xAxis, unit, unit.map(QuantitySemantics::defaultFor));
    }

    private static EditorAction plotAxisUnit(EditorActionId id, String label, boolean xAxis, Optional<UnitExpression> unit,
                                             Optional<QuantitySemantics> semantics) {
        return new SimpleAction(id, label, tooltip(id), null,
                context -> context.session().supportsSetSelectedPlotAxisDisplayUnit(),
                context -> context.session().setSelectedPlotAxisDisplayUnit(xAxis, unit, semantics)
                        ? EditorActionResult.DOCUMENT_CHANGED : EditorActionResult.NONE);
    }

    private static UnitExpression unit(String symbol) {
        return new UnitParser().parseRequired(symbol);
    }

    public static EditorAction wrapPlotInFigure() {
        return new SimpleAction(
                EditorActionId.FIGURE_WRAP_PLOT, label(EditorActionId.FIGURE_WRAP_PLOT),
                null,
                context -> context.session().supportsWrapSelectedPlotInFigure(),
                context -> context.session().wrapSelectedPlotInFigure()
                        ? EditorActionResult.DOCUMENT_CHANGED
                        : EditorActionResult.NONE);
    }

    public static EditorAction wrapDiagramInFigure() {
        return new SimpleAction(
                EditorActionId.FIGURE_WRAP_DIAGRAM, label(EditorActionId.FIGURE_WRAP_DIAGRAM),
                null,
                context -> context.session().supportsWrapSelectedDiagramInFigure(),
                context -> context.session().wrapSelectedDiagramInFigure()
                        ? EditorActionResult.DOCUMENT_CHANGED
                        : EditorActionResult.NONE);
    }

    public static EditorAction insertCrossReference() {
        return new SimpleAction(
                EditorActionId.INSERT_CROSS_REFERENCE, label(EditorActionId.INSERT_CROSS_REFERENCE),
                tooltip(EditorActionId.INSERT_CROSS_REFERENCE),
                null,
                context -> context.session().supportsInsertCrossReference(),
                context -> EditorActionResult.openCrossReferencePopup());
    }

    public static EditorAction insertTableOfContents() {
        return new SimpleAction(
                EditorActionId.INSERT_TABLE_OF_CONTENTS, label(EditorActionId.INSERT_TABLE_OF_CONTENTS),
                tooltip(EditorActionId.INSERT_TABLE_OF_CONTENTS),
                null,
                context -> context.session().supportsInsertTableOfContents(),
                context -> context.session().insertTableOfContents()
                        ? EditorActionResult.DOCUMENT_CHANGED
                        : EditorActionResult.NONE);
    }

    public static EditorAction toggleOutline() {
        return new SimpleAction(
                EditorActionId.TOGGLE_OUTLINE, label(EditorActionId.TOGGLE_OUTLINE),
                tooltip(EditorActionId.TOGGLE_OUTLINE),
                null,
                context -> true,
                context -> EditorActionResult.requestToggleOutline());
    }

    public static EditorAction newDataset() {
        return new SimpleAction(
                EditorActionId.DATA_NEW_DATASET, label(EditorActionId.DATA_NEW_DATASET),
                tooltip(EditorActionId.DATA_NEW_DATASET),
                null,
                context -> context.session().supportsDatasetDocumentAction(),
                context -> context.session().createDefaultDataset()
                        ? EditorActionResult.DOCUMENT_CHANGED
                        : EditorActionResult.NONE);
    }

    public static EditorAction insertDatasetTable() {
        return new SimpleAction(
                EditorActionId.DATA_INSERT_DATASET_TABLE, label(EditorActionId.DATA_INSERT_DATASET_TABLE),
                tooltip(EditorActionId.DATA_INSERT_DATASET_TABLE),
                null,
                context -> context.session().supportsInsertDatasetTable(),
                context -> context.session().insertDatasetTableForFirstDataset()
                        ? EditorActionResult.DOCUMENT_CHANGED
                        : EditorActionResult.NONE);
    }

    public static EditorAction bindPlotToDataset() {
        return new SimpleAction(
                EditorActionId.DATA_BIND_PLOT_TO_DATASET, label(EditorActionId.DATA_BIND_PLOT_TO_DATASET),
                tooltip(EditorActionId.DATA_BIND_PLOT_TO_DATASET),
                null,
                context -> context.session().supportsBindSelectedPlotToFirstDataset(),
                context -> context.session().bindSelectedPlotToFirstDataset()
                        ? EditorActionResult.DOCUMENT_CHANGED
                        : EditorActionResult.NONE);
    }

    public static EditorAction editFigureCaption() {
        return new SimpleAction(
                EditorActionId.FIGURE_EDIT_CAPTION, label(EditorActionId.FIGURE_EDIT_CAPTION),
                null,
                context -> context.session().supportsEditFigureCaption(),
                context -> {
                    context.session().editFigureCaption();
                    return EditorActionResult.NONE;
                });
    }

    public static EditorAction unwrapFigure() {
        return new SimpleAction(
                EditorActionId.FIGURE_UNWRAP, label(EditorActionId.FIGURE_UNWRAP),
                null,
                context -> context.session().supportsUnwrapFigure(),
                context -> context.session().unwrapFigure()
                        ? EditorActionResult.DOCUMENT_CHANGED
                        : EditorActionResult.NONE);
    }

    public static List<EditorAction> blockStyleActions() {
        return List.of(paragraph(), heading(1), heading(2), heading(3), heading(4), heading(5), heading(6));
    }

    public static EditorAction undo() {
        return new SimpleAction(
                EditorActionId.UNDO, label(EditorActionId.UNDO),
                shortcut(EditorActionId.UNDO),
                context -> context.session().canUndo(),
                context -> context.session().undo() ? EditorActionResult.DOCUMENT_CHANGED : EditorActionResult.NONE);
    }

    public static EditorAction redo() {
        return new SimpleAction(
                EditorActionId.REDO, label(EditorActionId.REDO),
                shortcut(EditorActionId.REDO),
                context -> context.session().canRedo(),
                context -> context.session().redo() ? EditorActionResult.DOCUMENT_CHANGED : EditorActionResult.NONE);
    }

    public static EditorAction cut() {
        return new SimpleAction(
                EditorActionId.CUT, label(EditorActionId.CUT),
                shortcut(EditorActionId.CUT),
                context -> context.session().canCutForClipboard(),
                context -> {
                    var cut = context.session().cutForClipboard();
                    if (cut.isEmpty()) {
                        return EditorActionResult.NONE;
                    }
                    var cutResult = cut.get();
                    if (!context.clipboard().setText(cutResult.plainText())) {
                        return EditorActionResult.NONE;
                    }
                    cutResult.payload().ifPresentOrElse(
                            payload -> context.scholarClipboard().install(cutResult.plainText(), payload),
                            context.scholarClipboard()::clear);
                    return context.session().applyCut(cutResult) ? EditorActionResult.DOCUMENT_CHANGED : EditorActionResult.NONE;
                });
    }

    public static EditorAction copy() {
        return new SimpleAction(
                EditorActionId.COPY, label(EditorActionId.COPY),
                shortcut(EditorActionId.COPY),
                context -> context.session().canCopyForClipboard(),
                context -> {
                    var copy = context.session().copyForClipboard();
                    if (copy.isEmpty()) {
                        return EditorActionResult.NONE;
                    }
                    var copyResult = copy.get();
                    if (!context.clipboard().setText(copyResult.plainText())) {
                        return EditorActionResult.NONE;
                    }
                    copyResult.payload().ifPresentOrElse(
                            payload -> context.scholarClipboard().install(copyResult.plainText(), payload),
                            context.scholarClipboard()::clear);
                    return EditorActionResult.NONE;
                });
    }

    public static EditorAction paste() {
        return new SimpleAction(
                EditorActionId.PASTE, label(EditorActionId.PASTE),
                shortcut(EditorActionId.PASTE),
                context -> {
                    var text = context.clipboard().getText();
                    var payload = context.scholarClipboard().matchingPayload(text);
                    return context.session().canPasteFromClipboard(payload, text);
                },
                context -> {
                    var text = context.clipboard().getText();
                    var payload = context.scholarClipboard().matchingPayload(text);
                    return context.session().pasteFromClipboard(payload, text)
                        ? EditorActionResult.DOCUMENT_CHANGED
                        : EditorActionResult.NONE;
                });
    }

    public static EditorAction deleteSelection() {
        return new SimpleAction(
                EditorActionId.DELETE, label(EditorActionId.DELETE),
                shortcut(EditorActionId.DELETE),
                context -> context.session().current().isBlockSelection()
                        || context.session().current().hasSelection()
                        || context.session().current().isTableEditingSelection()
                        || context.session().current().isEquationEditingSelection()
                        || context.session().current().isFigureCaptionSelection()
                        || context.session().current().isPlotEditingSelection()
                        || context.session().current().isDiagramEditingSelection(),
                context -> context.session().deleteForward()
                        ? EditorActionResult.DOCUMENT_CHANGED
                        : EditorActionResult.NONE);
    }

    public static EditorAction insertEquation() {
        return new SimpleAction(
                EditorActionId.INSERT_EQUATION, label(EditorActionId.INSERT_EQUATION),
                null,
                context -> context.session().supportsInsertEquation(),
                context -> context.session().insertEmptyEquation()
                        ? EditorActionResult.DOCUMENT_CHANGED
                        : EditorActionResult.NONE);
    }

    private static EditorAction computationDialog(EditorActionId id, String label, ComputationDialogKind kind) {
        return new SimpleAction(id, label, null,
                context -> switch (kind) {
                    case INSERT_VARIABLE, INSERT_RESULT -> context.session().supportsInsertComputation();
                    case EDIT_VARIABLE -> context.session().supportsEditVariable();
                    case EDIT_RESULT -> context.session().supportsEditComputedResult();
                    case INSERT_ANALYSIS -> context.session().supportsInsertAnalysis();
                    case EDIT_ANALYSIS -> context.session().supportsEditAnalysis();
                    case ADD_FIT_OVERLAY -> context.session().supportsAddFitOverlay();
                }, context -> EditorActionResult.openComputationDialog(kind));
    }

    public static EditorAction insertTable() {
        return new SimpleAction(
                EditorActionId.INSERT_TABLE, label(EditorActionId.INSERT_TABLE),
                null,
                context -> context.session().supportsInsertTable(),
                context -> context.session().insertDefaultTable()
                        ? EditorActionResult.DOCUMENT_CHANGED
                        : EditorActionResult.NONE);
    }

    public static EditorAction insertPlot() {
        return new SimpleAction(
                EditorActionId.INSERT_PLOT, label(EditorActionId.INSERT_PLOT),
                null,
                context -> context.session().supportsInsertPlot(),
                context -> context.session().insertDefaultPlot()
                        ? EditorActionResult.DOCUMENT_CHANGED
                        : EditorActionResult.NONE);
    }

    public static EditorAction insertDiagram() {
        return new SimpleAction(
                EditorActionId.INSERT_DIAGRAM, label(EditorActionId.INSERT_DIAGRAM),
                null,
                context -> context.session().supportsInsertDiagram(),
                context -> context.session().insertDefaultDiagram()
                        ? EditorActionResult.DOCUMENT_CHANGED
                        : EditorActionResult.NONE);
    }

    public static EditorAction insertFraction() {
        return new SimpleAction(
                EditorActionId.MATH_INSERT_FRACTION, label(EditorActionId.MATH_INSERT_FRACTION),
                tooltip(EditorActionId.MATH_INSERT_FRACTION),
                null,
                context -> context.session().supportsInsertFraction(),
                context -> context.session().insertFraction()
                        ? EditorActionResult.DOCUMENT_CHANGED
                        : EditorActionResult.NONE);
    }

    public static EditorAction insertRoot() {
        return new SimpleAction(
                EditorActionId.MATH_INSERT_ROOT, label(EditorActionId.MATH_INSERT_ROOT),
                tooltip(EditorActionId.MATH_INSERT_ROOT),
                null,
                context -> context.session().supportsInsertRoot(),
                context -> context.session().insertRoot()
                        ? EditorActionResult.DOCUMENT_CHANGED
                        : EditorActionResult.NONE);
    }

    public static EditorAction insertParenthesesGroup() {
        return insertGroup(EditorActionId.MATH_INSERT_PARENTHESES_GROUP, label(EditorActionId.MATH_INSERT_PARENTHESES_GROUP), tooltip(EditorActionId.MATH_INSERT_PARENTHESES_GROUP), MathDelimiter.PARENTHESES);
    }

    public static EditorAction insertBracketsGroup() {
        return insertGroup(EditorActionId.MATH_INSERT_BRACKETS_GROUP, label(EditorActionId.MATH_INSERT_BRACKETS_GROUP), tooltip(EditorActionId.MATH_INSERT_BRACKETS_GROUP), MathDelimiter.BRACKETS);
    }

    public static EditorAction insertBracesGroup() {
        return insertGroup(EditorActionId.MATH_INSERT_BRACES_GROUP, label(EditorActionId.MATH_INSERT_BRACES_GROUP), tooltip(EditorActionId.MATH_INSERT_BRACES_GROUP), MathDelimiter.BRACES);
    }

    public static EditorAction insertSuperscript() {
        return insertScriptSlot(EditorActionId.MATH_INSERT_SUPERSCRIPT, label(EditorActionId.MATH_INSERT_SUPERSCRIPT), tooltip(EditorActionId.MATH_INSERT_SUPERSCRIPT), ScriptSlot.SUPERSCRIPT);
    }

    public static EditorAction insertSubscript() {
        return insertScriptSlot(EditorActionId.MATH_INSERT_SUBSCRIPT, label(EditorActionId.MATH_INSERT_SUBSCRIPT), tooltip(EditorActionId.MATH_INSERT_SUBSCRIPT), ScriptSlot.SUBSCRIPT);
    }

    public static EditorAction convertToNamedOperator() {
        return semanticConversionAction(
                EditorActionId.MATH_CONVERT_NAMED_OPERATOR, label(EditorActionId.MATH_CONVERT_NAMED_OPERATOR),
                SemanticMathTokenKind.NAMED_OPERATOR);
    }

    public static EditorAction convertToMathText() {
        return semanticConversionAction(
                EditorActionId.MATH_CONVERT_TEXT, label(EditorActionId.MATH_CONVERT_TEXT),
                SemanticMathTokenKind.MATH_TEXT);
    }

    public static EditorAction bold() {
        return formatAction(EditorActionId.BOLD, label(EditorActionId.BOLD), shortcut(EditorActionId.BOLD), TextMark.BOLD);
    }

    public static EditorAction italic() {
        return formatAction(EditorActionId.ITALIC, label(EditorActionId.ITALIC), shortcut(EditorActionId.ITALIC), TextMark.ITALIC);
    }

    public static EditorAction underline() {
        return formatAction(EditorActionId.UNDERLINE, label(EditorActionId.UNDERLINE), null, TextMark.UNDERLINE);
    }

    public static EditorAction textSuperscript() {
        return formatAction(EditorActionId.TEXT_SUPERSCRIPT, label(EditorActionId.TEXT_SUPERSCRIPT), null, TextMark.SUPERSCRIPT);
    }

    public static EditorAction textSubscript() {
        return formatAction(EditorActionId.TEXT_SUBSCRIPT, label(EditorActionId.TEXT_SUBSCRIPT), null, TextMark.SUBSCRIPT);
    }

    public static EditorAction insertPageBreak() {
        return new SimpleAction(EditorActionId.INSERT_PAGE_BREAK, label(EditorActionId.INSERT_PAGE_BREAK), null,
                context -> context.session().current().isTextSelection() || context.session().current().isBlockSelection(),
                context -> context.session().insertPageBreak() ? EditorActionResult.DOCUMENT_CHANGED : EditorActionResult.NONE);
    }

    private static EditorAction semanticStyle(EditorActionId id, String label, dev.rgcb.scholar.document.SemanticStyle style) {
        return new SimpleAction(id, label, null,
                context -> context.session().current().isTextSelection()
                        && context.session().current().document().blocks().get(context.session().current().active().blockIndex()) instanceof dev.rgcb.scholar.document.Paragraph,
                context -> context.session().setParagraphStyle(style) ? EditorActionResult.DOCUMENT_CHANGED : EditorActionResult.NONE);
    }

    private static EditorAction fontSize(EditorActionId id, String label, int halfPoints) {
        return new SimpleAction(id, label, null,
                context -> context.session().supportsInlineFormatting() && context.session().current().hasSelection(),
                context -> context.session().setTextFormat(new dev.rgcb.scholar.document.TextFormat(java.util.Optional.empty(), java.util.Optional.of(halfPoints)))
                        ? EditorActionResult.DOCUMENT_CHANGED : EditorActionResult.NONE);
    }

    private static EditorAction fontFamily(EditorActionId id, String label, dev.rgcb.scholar.document.ScholarFontFamily family) {
        return new SimpleAction(id, label, null,
                context -> context.session().supportsInlineFormatting() && context.session().current().hasSelection(),
                context -> context.session().setTextFormat(new dev.rgcb.scholar.document.TextFormat(java.util.Optional.of(family), java.util.Optional.empty()))
                        ? EditorActionResult.DOCUMENT_CHANGED : EditorActionResult.NONE);
    }

    private static EditorAction alignment(EditorActionId id, String label, dev.rgcb.scholar.document.ParagraphAlignment alignment) {
        return new SimpleAction(id, label, null,
                context -> context.session().current().isTextSelection(),
                context -> context.session().setParagraphAlignment(alignment) ? EditorActionResult.DOCUMENT_CHANGED : EditorActionResult.NONE);
    }

    private static EditorAction paragraphCommand(EditorActionId id, String label, java.util.function.Predicate<EditorSession> command) {
        return new SimpleAction(id, label, null, context -> context.session().current().isTextSelection(),
                context -> command.test(context.session()) ? EditorActionResult.DOCUMENT_CHANGED : EditorActionResult.NONE);
    }

    private static EditorAction settings(EditorActionId id, String label,
                                         java.util.function.UnaryOperator<dev.rgcb.scholar.document.DocumentSettings> update) {
        return new SimpleAction(id, label, null, context -> true,
                context -> context.session().updateDocumentSettings(update) ? EditorActionResult.DOCUMENT_CHANGED : EditorActionResult.NONE);
    }

    public static EditorAction insertTableRowAbove() {
        return tableAction(
                EditorActionId.TABLE_INSERT_ROW_ABOVE, label(EditorActionId.TABLE_INSERT_ROW_ABOVE),
                context -> context.session().supportsInsertTableRow(),
                session -> session.insertTableRowAbove());
    }

    public static EditorAction insertTableRowBelow() {
        return tableAction(
                EditorActionId.TABLE_INSERT_ROW_BELOW, label(EditorActionId.TABLE_INSERT_ROW_BELOW),
                context -> context.session().supportsInsertTableRow(),
                session -> session.insertTableRowBelow());
    }

    public static EditorAction deleteTableRow() {
        return tableAction(
                EditorActionId.TABLE_DELETE_ROW, label(EditorActionId.TABLE_DELETE_ROW),
                context -> context.session().supportsDeleteTableRow(),
                session -> session.deleteTableRow());
    }

    public static EditorAction insertTableColumnLeft() {
        return tableAction(
                EditorActionId.TABLE_INSERT_COLUMN_LEFT, label(EditorActionId.TABLE_INSERT_COLUMN_LEFT),
                context -> context.session().supportsInsertTableColumn(),
                session -> session.insertTableColumnLeft());
    }

    public static EditorAction insertTableColumnRight() {
        return tableAction(
                EditorActionId.TABLE_INSERT_COLUMN_RIGHT, label(EditorActionId.TABLE_INSERT_COLUMN_RIGHT),
                context -> context.session().supportsInsertTableColumn(),
                session -> session.insertTableColumnRight());
    }

    public static EditorAction deleteTableColumn() {
        return tableAction(
                EditorActionId.TABLE_DELETE_COLUMN, label(EditorActionId.TABLE_DELETE_COLUMN),
                context -> context.session().supportsDeleteTableColumn(),
                session -> session.deleteTableColumn());
    }

    public static EditorAction togglePlotGrid() {
        return new SimpleAction(
                EditorActionId.PLOT_TOGGLE_GRID, label(EditorActionId.PLOT_TOGGLE_GRID),
                null,
                context -> context.session().supportsPlotEditingAction(),
                context -> context.session().togglePlotGrid() ? EditorActionResult.DOCUMENT_CHANGED : EditorActionResult.NONE,
                context -> !context.session().supportsPlotEditingAction()
                        ? ActionSelectionState.NOT_APPLICABLE
                        : context.session().plotGridVisible() ? ActionSelectionState.ON : ActionSelectionState.OFF);
    }

    public static EditorAction togglePlotLegend() {
        return new SimpleAction(
                EditorActionId.PLOT_TOGGLE_LEGEND, label(EditorActionId.PLOT_TOGGLE_LEGEND),
                null,
                context -> context.session().supportsPlotEditingAction(),
                context -> context.session().togglePlotLegend() ? EditorActionResult.DOCUMENT_CHANGED : EditorActionResult.NONE,
                context -> !context.session().supportsPlotEditingAction()
                        ? ActionSelectionState.NOT_APPLICABLE
                        : context.session().plotLegendVisible() ? ActionSelectionState.ON : ActionSelectionState.OFF);
    }

    public static EditorAction addPlotLineSeries() {
        return plotAction(
                EditorActionId.PLOT_ADD_LINE_SERIES, label(EditorActionId.PLOT_ADD_LINE_SERIES),
                context -> context.session().supportsPlotEditingAction(),
                session -> session.addPlotSeries(PlotSeriesKind.LINE));
    }

    public static EditorAction addPlotScatterSeries() {
        return plotAction(
                EditorActionId.PLOT_ADD_SCATTER_SERIES, label(EditorActionId.PLOT_ADD_SCATTER_SERIES),
                context -> context.session().supportsPlotEditingAction(),
                session -> session.addPlotSeries(PlotSeriesKind.SCATTER));
    }

    public static EditorAction setPlotSeriesLine() {
        return new SimpleAction(
                EditorActionId.PLOT_SET_SERIES_LINE, label(EditorActionId.PLOT_SET_SERIES_LINE),
                null,
                context -> context.session().supportsSetPlotSeriesKind(),
                context -> context.session().setPlotSeriesKind(PlotSeriesKind.LINE) ? EditorActionResult.DOCUMENT_CHANGED : EditorActionResult.NONE,
                context -> context.session().selectedPlotSeriesKind()
                        .map(kind -> kind == PlotSeriesKind.LINE ? ActionSelectionState.ON : ActionSelectionState.OFF)
                        .orElse(ActionSelectionState.NOT_APPLICABLE));
    }

    public static EditorAction setPlotSeriesScatter() {
        return new SimpleAction(
                EditorActionId.PLOT_SET_SERIES_SCATTER, label(EditorActionId.PLOT_SET_SERIES_SCATTER),
                null,
                context -> context.session().supportsSetPlotSeriesKind(),
                context -> context.session().setPlotSeriesKind(PlotSeriesKind.SCATTER) ? EditorActionResult.DOCUMENT_CHANGED : EditorActionResult.NONE,
                context -> context.session().selectedPlotSeriesKind()
                        .map(kind -> kind == PlotSeriesKind.SCATTER ? ActionSelectionState.ON : ActionSelectionState.OFF)
                        .orElse(ActionSelectionState.NOT_APPLICABLE));
    }

    public static EditorAction addPlotPoint() {
        return plotAction(
                EditorActionId.PLOT_ADD_POINT, label(EditorActionId.PLOT_ADD_POINT),
                context -> context.session().supportsAddPlotPoint(),
                EditorSession::addPlotPoint);
    }

    public static EditorAction deletePlotPoint() {
        return plotAction(
                EditorActionId.PLOT_DELETE_POINT, label(EditorActionId.PLOT_DELETE_POINT),
                context -> context.session().supportsDeletePlotPoint(),
                EditorSession::deletePlotPoint);
    }

    public static EditorAction deletePlotSeries() {
        return plotAction(
                EditorActionId.PLOT_DELETE_SERIES, label(EditorActionId.PLOT_DELETE_SERIES),
                context -> context.session().supportsDeletePlotSeries(),
                EditorSession::deletePlotSeries);
    }

    public static EditorAction addDiagramNode() {
        return diagramAction(
                EditorActionId.DIAGRAM_ADD_NODE, label(EditorActionId.DIAGRAM_ADD_NODE),
                context -> context.session().supportsDiagramEditingAction(),
                EditorSession::addDiagramNode);
    }

    public static EditorAction deleteDiagramNode() {
        return diagramAction(
                EditorActionId.DIAGRAM_DELETE_NODE, label(EditorActionId.DIAGRAM_DELETE_NODE),
                context -> context.session().supportsDeleteDiagramNode(),
                EditorSession::deleteDiagramNode);
    }

    public static EditorAction addElectricalComponent(
            EditorActionId id,
            String label,
            ElectricalComponentKind kind
    ) {
        Objects.requireNonNull(kind, "kind");
        return diagramAction(
                id,
                label,
                context -> context.session().supportsDiagramEditingAction(),
                session -> session.addElectricalComponent(kind));
    }

    public static EditorAction addMechanicalPrimitive(
            EditorActionId id,
            String label,
            MechanicalPrimitiveKind kind
    ) {
        Objects.requireNonNull(kind, "kind");
        return diagramAction(
                id,
                label,
                context -> context.session().supportsDiagramEditingAction(),
                session -> session.addMechanicalPrimitive(kind));
    }

    public static EditorAction addMechanicalPartReference(){return diagramAction(EditorActionId.DIAGRAM_ADD_MECHANICAL_PART_REFERENCE, label(EditorActionId.DIAGRAM_ADD_MECHANICAL_PART_REFERENCE),context->context.session().supportsAddMechanicalPartReference(),EditorSession::addMechanicalPartReference);}
    public static EditorAction generateMechanicalBom(){return diagramAction(EditorActionId.DIAGRAM_GENERATE_MECHANICAL_BOM, label(EditorActionId.DIAGRAM_GENERATE_MECHANICAL_BOM),context->context.session().supportsGenerateMechanicalBom(),EditorSession::generateMechanicalBom);}
    public static EditorAction deleteMechanicalPartReference(){return diagramAction(EditorActionId.DIAGRAM_DELETE_MECHANICAL_PART_REFERENCE, label(EditorActionId.DIAGRAM_DELETE_MECHANICAL_PART_REFERENCE),context->context.session().supportsDeleteMechanicalPartReference(),EditorSession::deleteMechanicalPartReference);}

    public static EditorAction addMechanicalAnnotation(EditorActionId id,String label,MechanicalAnnotationKind kind){Objects.requireNonNull(kind);return diagramAction(id,label,context->context.session().supportsDiagramEditingAction(),session->session.addMechanicalAnnotation(kind));}
    public static EditorAction deleteMechanicalAnnotation(){return diagramAction(EditorActionId.DIAGRAM_DELETE_MECHANICAL_ANNOTATION, label(EditorActionId.DIAGRAM_DELETE_MECHANICAL_ANNOTATION),context->context.session().supportsDeleteMechanicalAnnotation(),EditorSession::deleteMechanicalAnnotation);}

    public static EditorAction addMechanicalSymbol(EditorActionId id, String label, MechanicalSymbolKind kind) {
        Objects.requireNonNull(kind); return diagramAction(id,label,context -> context.session().supportsDiagramEditingAction(),session -> session.addMechanicalSymbol(kind));
    }

    public static EditorAction addMechanicalDimension(
            EditorActionId id, String label, MechanicalDimensionKind kind
    ) {
        Objects.requireNonNull(kind, "kind");
        return diagramAction(
                id, label,
                context -> context.session().supportsDiagramEditingAction(),
                session -> session.addMechanicalDimension(kind));
    }

    public static EditorAction addMechanicalUnaryConstraint(
            EditorActionId id,
            String label,
            MechanicalConstraintKind kind
    ) {
        Objects.requireNonNull(kind, "kind");
        return diagramAction(
                id,
                label,
                context -> context.session().supportsAddMechanicalUnaryConstraint(kind),
                session -> session.addMechanicalUnaryConstraint(kind));
    }

    public static EditorAction startMechanicalConstraint(
            EditorActionId id,
            String label,
            MechanicalConstraintKind kind
    ) {
        Objects.requireNonNull(kind, "kind");
        return new SimpleAction(
                id,
                label,
                null,
                context -> context.session().supportsStartMechanicalConstraint(kind),
                context -> {
                    context.session().startMechanicalConstraint(kind);
                    return EditorActionResult.NONE;
                });
    }

    public static EditorAction finishMechanicalConstraint() {
        return diagramAction(
                EditorActionId.DIAGRAM_FINISH_MECHANICAL_CONSTRAINT, label(EditorActionId.DIAGRAM_FINISH_MECHANICAL_CONSTRAINT),
                context -> context.session().supportsFinishMechanicalConstraint(),
                EditorSession::finishMechanicalConstraint);
    }

    public static EditorAction cancelMechanicalConstraint() {
        return new SimpleAction(
                EditorActionId.DIAGRAM_CANCEL_MECHANICAL_CONSTRAINT, label(EditorActionId.DIAGRAM_CANCEL_MECHANICAL_CONSTRAINT),
                null,
                context -> context.session().hasPendingMechanicalConstraint(),
                context -> {
                    context.session().cancelMechanicalConstraint();
                    return EditorActionResult.NONE;
                });
    }

    public static EditorAction deleteMechanicalConstraint() {
        return diagramAction(
                EditorActionId.DIAGRAM_DELETE_MECHANICAL_CONSTRAINT, label(EditorActionId.DIAGRAM_DELETE_MECHANICAL_CONSTRAINT),
                context -> context.session().supportsDeleteMechanicalConstraint(),
                EditorSession::deleteMechanicalConstraint);
    }

    public static EditorAction deleteMechanicalSymbol() { return diagramAction(EditorActionId.DIAGRAM_DELETE_MECHANICAL_SYMBOL, label(EditorActionId.DIAGRAM_DELETE_MECHANICAL_SYMBOL), context -> context.session().supportsDeleteMechanicalSymbol(), EditorSession::deleteMechanicalSymbol); }

    public static EditorAction deleteMechanicalDimension() {
        return diagramAction(
                EditorActionId.DIAGRAM_DELETE_MECHANICAL_DIMENSION, label(EditorActionId.DIAGRAM_DELETE_MECHANICAL_DIMENSION),
                context -> context.session().supportsDeleteMechanicalDimension(),
                EditorSession::deleteMechanicalDimension);
    }

    public static EditorAction deleteMechanicalPrimitive() {
        return diagramAction(
                EditorActionId.DIAGRAM_DELETE_MECHANICAL_PRIMITIVE, label(EditorActionId.DIAGRAM_DELETE_MECHANICAL_PRIMITIVE),
                context -> context.session().supportsDeleteMechanicalPrimitive(),
                EditorSession::deleteMechanicalPrimitive);
    }

    public static EditorAction addElectricalJunction() {
        return diagramAction(
                EditorActionId.DIAGRAM_ADD_JUNCTION, label(EditorActionId.DIAGRAM_ADD_JUNCTION),
                context -> context.session().supportsDiagramEditingAction(),
                EditorSession::addElectricalJunction);
    }

    public static EditorAction deleteElectricalJunction() {
        return diagramAction(
                EditorActionId.DIAGRAM_DELETE_JUNCTION, label(EditorActionId.DIAGRAM_DELETE_JUNCTION),
                context -> context.session().supportsDeleteElectricalJunction(),
                EditorSession::deleteElectricalJunction);
    }

    public static EditorAction scaleElectricalSymbolsDown() {
        return diagramAction(
                EditorActionId.DIAGRAM_SCALE_SYMBOLS_DOWN, label(EditorActionId.DIAGRAM_SCALE_SYMBOLS_DOWN),
                context -> context.session().supportsScaleElectricalSymbols(),
                session -> session.scaleElectricalSymbols(0.90));
    }

    public static EditorAction scaleElectricalSymbolsUp() {
        return diagramAction(
                EditorActionId.DIAGRAM_SCALE_SYMBOLS_UP, label(EditorActionId.DIAGRAM_SCALE_SYMBOLS_UP),
                context -> context.session().supportsScaleElectricalSymbols(),
                session -> session.scaleElectricalSymbols(1.10));
    }

    public static EditorAction shortenDiagramWorkspace() {
        return diagramAction(
                EditorActionId.DIAGRAM_WORKSPACE_SHORTER, label(EditorActionId.DIAGRAM_WORKSPACE_SHORTER),
                context -> context.session().supportsDiagramEditingAction(),
                session -> session.scaleDiagramWorkspaceHeight(0.90));
    }

    public static EditorAction heightenDiagramWorkspace() {
        return diagramAction(
                EditorActionId.DIAGRAM_WORKSPACE_TALLER, label(EditorActionId.DIAGRAM_WORKSPACE_TALLER),
                context -> context.session().supportsDiagramEditingAction(),
                session -> session.scaleDiagramWorkspaceHeight(1.10));
    }

    public static EditorAction resetDiagramWorkspaceHeight() {
        return diagramAction(
                EditorActionId.DIAGRAM_WORKSPACE_RESET_HEIGHT, label(EditorActionId.DIAGRAM_WORKSPACE_RESET_HEIGHT),
                context -> context.session().supportsDiagramEditingAction(),
                EditorSession::resetDiagramWorkspaceHeight);
    }

    public static EditorAction rotateElectricalComponentClockwise() {
        return diagramAction(
                EditorActionId.DIAGRAM_ROTATE_CLOCKWISE, label(EditorActionId.DIAGRAM_ROTATE_CLOCKWISE),
                context -> context.session().supportsRotateElectricalComponent(),
                EditorSession::rotateElectricalComponentClockwise);
    }

    public static EditorAction rotateElectricalComponentCounterClockwise() {
        return diagramAction(
                EditorActionId.DIAGRAM_ROTATE_COUNTERCLOCKWISE, label(EditorActionId.DIAGRAM_ROTATE_COUNTERCLOCKWISE),
                context -> context.session().supportsRotateElectricalComponent(),
                EditorSession::rotateElectricalComponentCounterClockwise);
    }

    public static EditorAction deleteElectricalComponent() {
        return diagramAction(
                EditorActionId.DIAGRAM_DELETE_ELECTRICAL_COMPONENT, label(EditorActionId.DIAGRAM_DELETE_ELECTRICAL_COMPONENT),
                context -> context.session().supportsDeleteElectricalComponent(),
                EditorSession::deleteElectricalComponent);
    }

    public static EditorAction startDiagramConnection() {
        return new SimpleAction(
                EditorActionId.DIAGRAM_START_CONNECTION, label(EditorActionId.DIAGRAM_START_CONNECTION),
                null,
                context -> context.session().supportsBeginDiagramConnection(),
                context -> {
                    context.session().beginDiagramConnection();
                    return EditorActionResult.NONE;
                });
    }

    public static EditorAction finishDiagramConnection() {
        return diagramAction(
                EditorActionId.DIAGRAM_FINISH_CONNECTION, label(EditorActionId.DIAGRAM_FINISH_CONNECTION),
                context -> context.session().supportsCompleteDiagramConnection(),
                EditorSession::completeDiagramConnection);
    }

    public static EditorAction cancelDiagramConnection() {
        return new SimpleAction(
                EditorActionId.DIAGRAM_CANCEL_CONNECTION, label(EditorActionId.DIAGRAM_CANCEL_CONNECTION),
                null,
                context -> context.session().diagramConnectionInProgress(),
                context -> {
                    context.session().cancelDiagramConnection();
                    return EditorActionResult.NONE;
                });
    }

    public static EditorAction deleteDiagramConnection() {
        return diagramAction(
                EditorActionId.DIAGRAM_DELETE_CONNECTION, label(EditorActionId.DIAGRAM_DELETE_CONNECTION),
                context -> context.session().supportsDeleteDiagramConnection(),
                EditorSession::deleteDiagramConnection);
    }

    public static EditorAction paragraph() {
        return blockStyleAction(EditorActionId.PARAGRAPH, BlockStyle.paragraph());
    }

    public static EditorAction heading(int level) {
        return blockStyleAction(headingActionId(level), BlockStyle.heading(level));
    }

    private static EditorAction blockStyleAction(EditorActionId id, BlockStyle style) {
        return new SimpleAction(
                id,
                style.displayName(),
                null,
                context -> context.session().supportsBlockStyle(),
                context -> context.session().setBlockStyle(style)
                        ? EditorActionResult.DOCUMENT_CHANGED
                        : EditorActionResult.NONE,
                context -> {
                    var currentStyle = context.session().blockStyleSelectionState();
                    if (currentStyle.kind() == BlockStyleSelectionState.Kind.NOT_APPLICABLE) {
                        return ActionSelectionState.NOT_APPLICABLE;
                    }
                    if (currentStyle.kind() == BlockStyleSelectionState.Kind.MIXED) {
                        return ActionSelectionState.OFF;
                    }
                    return currentStyle.style().orElseThrow().equals(style) ? ActionSelectionState.ON : ActionSelectionState.OFF;
                });
    }

    private static EditorActionId headingActionId(int level) {
        return switch (level) {
            case 1 -> EditorActionId.HEADING_1;
            case 2 -> EditorActionId.HEADING_2;
            case 3 -> EditorActionId.HEADING_3;
            case 4 -> EditorActionId.HEADING_4;
            case 5 -> EditorActionId.HEADING_5;
            case 6 -> EditorActionId.HEADING_6;
            default -> throw new IllegalArgumentException("Heading level must be between 1 and 6.");
        };
    }

    private static EditorAction formatAction(EditorActionId id, String label, ActionShortcut shortcut, TextMark mark) {
        return new SimpleAction(
                id,
                label,
                shortcut,
                context -> context.session().supportsInlineFormatting(),
                context -> {
                    var hadSelection = hasActiveTextSelection(context.session());
                    var changed = context.session().toggleMark(mark);
                    return changed && hadSelection ? EditorActionResult.DOCUMENT_CHANGED : EditorActionResult.NONE;
                },
                context -> {
                    if (!context.session().supportsInlineFormatting()) {
                        return ActionSelectionState.NOT_APPLICABLE;
                    }
                    return switch (context.session().formattingState(mark)) {
                        case NOT_APPLICABLE -> ActionSelectionState.NOT_APPLICABLE;
                        case OFF -> ActionSelectionState.OFF;
                        case ON -> ActionSelectionState.ON;
                        case MIXED -> ActionSelectionState.MIXED;
                    };
                });
    }

    private static EditorAction tableAction(EditorActionId id, String label, ActionEnabled enabled, TableActionExecution execution) {
        return new SimpleAction(
                id,
                label,
                null,
                enabled,
                context -> execution.execute(context.session())
                        ? EditorActionResult.DOCUMENT_CHANGED
                        : EditorActionResult.NONE);
    }

    private static EditorAction plotAction(EditorActionId id, String label, ActionEnabled enabled, PlotActionExecution execution) {
        return new SimpleAction(
                id,
                label,
                null,
                enabled,
                context -> execution.execute(context.session())
                        ? EditorActionResult.DOCUMENT_CHANGED
                        : EditorActionResult.NONE);
    }

    private static EditorAction diagramAction(EditorActionId id, String label, ActionEnabled enabled, DiagramActionExecution execution) {
        return new SimpleAction(
                id,
                label,
                null,
                enabled,
                context -> execution.execute(context.session())
                        ? EditorActionResult.DOCUMENT_CHANGED
                        : EditorActionResult.NONE);
    }

    private static boolean hasActiveTextSelection(EditorSession session) {
        if (session.current().hasSelection()) {
            return true;
        }
        return session.current().isTableEditingSelection()
                && !session.current().tableEditingSelection().selection().isCaret();
    }

    private static EditorAction semanticConversionAction(EditorActionId id, String label, SemanticMathTokenKind kind) {
        return new SimpleAction(
                id,
                label,
                tooltip(id),
                null,
                context -> context.session().supportsSemanticTokenConversion(kind),
                context -> EditorActionResult.semanticTokenPopup(kind));
    }

    private static EditorAction insertScriptSlot(EditorActionId id, String label, String tooltip, ScriptSlot slot) {
        return new SimpleAction(
                id,
                label,
                tooltip,
                null,
                context -> context.session().supportsInsertScriptSlot(slot),
                context -> context.session().insertScriptSlot(slot)
                        ? EditorActionResult.DOCUMENT_CHANGED
                        : EditorActionResult.NONE);
    }

    private static EditorAction insertGroup(EditorActionId id, String label, String tooltip, MathDelimiter delimiter) {
        return new SimpleAction(
                id,
                label,
                tooltip,
                null,
                context -> context.session().supportsInsertGroup(delimiter),
                context -> context.session().insertGroup(delimiter)
                        ? EditorActionResult.DOCUMENT_CHANGED
                        : EditorActionResult.NONE);
    }

    private static final class SimpleAction implements EditorAction {
        private final EditorActionId id;
        private final String label;
        private final String tooltip;
        private final ActionShortcut shortcut;
        private final ActionEnabled enabled;
        private final ActionExecution execution;
        private final ActionSelection selection;

        private SimpleAction(
                EditorActionId id,
                String label,
                ActionShortcut shortcut,
                ActionEnabled enabled,
                ActionExecution execution
        ) {
            this(id, label, label, shortcut, enabled, execution, context -> ActionSelectionState.NOT_APPLICABLE);
        }

        private SimpleAction(
                EditorActionId id,
                String label,
                String tooltip,
                ActionShortcut shortcut,
                ActionEnabled enabled,
                ActionExecution execution
        ) {
            this(id, label, tooltip, shortcut, enabled, execution, context -> ActionSelectionState.NOT_APPLICABLE);
        }

        private SimpleAction(
                EditorActionId id,
                String label,
                ActionShortcut shortcut,
                ActionEnabled enabled,
                ActionExecution execution,
                ActionSelection selection
        ) {
            this(id, label, label, shortcut, enabled, execution, selection);
        }

        private SimpleAction(
                EditorActionId id,
                String label,
                String tooltip,
                ActionShortcut shortcut,
                ActionEnabled enabled,
                ActionExecution execution,
                ActionSelection selection
        ) {
            this.id = Objects.requireNonNull(id, "id");
            var descriptor = BuiltInEditorActionCatalog.require(id);
            this.label = descriptor.englishLabel();
            this.tooltip = descriptor.englishTooltip();
            this.shortcut = descriptor.shortcut().orElse(null);
            this.enabled = Objects.requireNonNull(enabled, "enabled");
            this.execution = Objects.requireNonNull(execution, "execution");
            this.selection = Objects.requireNonNull(selection, "selection");
        }

        @Override
        public EditorActionId id() {
            return id;
        }

        @Override
        public String label() {
            return label;
        }

        @Override
        public String tooltip() {
            return tooltip;
        }

        @Override
        public Optional<ActionShortcut> shortcut() {
            return Optional.ofNullable(shortcut);
        }

        @Override
        public boolean isEnabled(EditorActionContext context) {
            return enabled.isEnabled(context);
        }

        @Override
        public ActionSelectionState selectionState(EditorActionContext context) {
            return selection.selectionState(context);
        }

        @Override
        public EditorActionResult execute(EditorActionContext context) {
            if (!isEnabled(context)) {
                return EditorActionResult.NONE;
            }
            return execution.execute(context);
        }
    }

    @FunctionalInterface
    private interface ActionEnabled {
        boolean isEnabled(EditorActionContext context);
    }

    @FunctionalInterface
    private interface ActionExecution {
        EditorActionResult execute(EditorActionContext context);
    }

    @FunctionalInterface
    private interface TableActionExecution {
        boolean execute(EditorSession session);
    }

    @FunctionalInterface
    private interface PlotActionExecution {
        boolean execute(EditorSession session);
    }

    @FunctionalInterface
    private interface DiagramActionExecution {
        boolean execute(EditorSession session);
    }

    @FunctionalInterface
    private interface ActionSelection {
        ActionSelectionState selectionState(EditorActionContext context);
    }
}
