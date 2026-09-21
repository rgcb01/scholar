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
import dev.rgcb.scholar.quantity.UnitExpression;
import dev.rgcb.scholar.quantity.UnitParser;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class BuiltInEditorActions {
    private BuiltInEditorActions() {
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
                insertQuantity(EditorActionId.INSERT_QUANTITY_METRE, "Length: 1 m", "m"),
                insertQuantity(EditorActionId.INSERT_QUANTITY_CELSIUS, "Temperature: 25 °C", "°C", "25"),
                insertQuantity(EditorActionId.INSERT_QUANTITY_VOLT, "Voltage: 5 V", "V", "5"),
                insertQuantity(EditorActionId.INSERT_QUANTITY_MILLIAMPERE, "Current: 1 mA", "mA"),
                insertQuantity(EditorActionId.INSERT_QUANTITY_KILOOHM, "Resistance: 1 kΩ", "kΩ"),
                insertQuantity(EditorActionId.INSERT_QUANTITY_ACCELERATION, "Acceleration: 9.81 m/s²", "m/s²", "9.81"),
                convertQuantity(EditorActionId.QUANTITY_CONVERT_METRE, "Convert to m", "m"),
                convertQuantity(EditorActionId.QUANTITY_CONVERT_MILLIMETRE, "Convert to mm", "mm"),
                convertQuantity(EditorActionId.QUANTITY_CONVERT_CELSIUS, "Convert to °C", "°C"),
                convertQuantity(EditorActionId.QUANTITY_CONVERT_KELVIN, "Convert to K", "K"),
                quantityNotation(EditorActionId.QUANTITY_FORMAT_DECIMAL, "Decimal", NumberNotation.DECIMAL),
                quantityNotation(EditorActionId.QUANTITY_FORMAT_SCIENTIFIC, "Scientific", NumberNotation.SCIENTIFIC),
                quantityNotation(EditorActionId.QUANTITY_FORMAT_ENGINEERING, "Engineering", NumberNotation.ENGINEERING));
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
                semanticStyle(EditorActionId.STYLE_BODY, "Body Text", dev.rgcb.scholar.document.SemanticStyle.BODY_TEXT),
                semanticStyle(EditorActionId.STYLE_TITLE, "Title", dev.rgcb.scholar.document.SemanticStyle.TITLE),
                semanticStyle(EditorActionId.STYLE_SUBTITLE, "Subtitle", dev.rgcb.scholar.document.SemanticStyle.SUBTITLE),
                semanticStyle(EditorActionId.STYLE_AUTHOR, "Author", dev.rgcb.scholar.document.SemanticStyle.AUTHOR),
                semanticStyle(EditorActionId.STYLE_AFFILIATION, "Affiliation", dev.rgcb.scholar.document.SemanticStyle.AFFILIATION),
                semanticStyle(EditorActionId.STYLE_ABSTRACT, "Abstract", dev.rgcb.scholar.document.SemanticStyle.ABSTRACT),
                semanticStyle(EditorActionId.STYLE_KEYWORDS, "Keywords", dev.rgcb.scholar.document.SemanticStyle.KEYWORDS),
                semanticStyle(EditorActionId.STYLE_REFERENCE, "Reference", dev.rgcb.scholar.document.SemanticStyle.REFERENCE),
                fontSize(EditorActionId.FONT_SIZE_10, "10 pt", 20), fontSize(EditorActionId.FONT_SIZE_12, "12 pt", 24), fontSize(EditorActionId.FONT_SIZE_14, "14 pt", 28),
                fontFamily(EditorActionId.FONT_SOURCE_SANS, "Source Sans 3", dev.rgcb.scholar.document.ScholarFontFamily.SOURCE_SANS_3),
                fontFamily(EditorActionId.FONT_SCIENTIFIC_MATH, "Scientific Math", dev.rgcb.scholar.document.ScholarFontFamily.SCIENTIFIC_MATH),
                alignment(EditorActionId.ALIGN_LEFT, "Align Left", dev.rgcb.scholar.document.ParagraphAlignment.LEFT),
                alignment(EditorActionId.ALIGN_CENTER, "Center", dev.rgcb.scholar.document.ParagraphAlignment.CENTER),
                alignment(EditorActionId.ALIGN_RIGHT, "Align Right", dev.rgcb.scholar.document.ParagraphAlignment.RIGHT),
                alignment(EditorActionId.ALIGN_JUSTIFIED, "Justify", dev.rgcb.scholar.document.ParagraphAlignment.JUSTIFIED),
                paragraphCommand(EditorActionId.LINE_SPACING_SINGLE, "Single Spacing", s -> s.setParagraphLineSpacing(1000)),
                paragraphCommand(EditorActionId.LINE_SPACING_ONE_HALF, "1.5 Spacing", s -> s.setParagraphLineSpacing(1500)),
                paragraphCommand(EditorActionId.INDENT_DECREASE, "Decrease Indent", s -> s.adjustParagraphLeftIndent(-8)),
                paragraphCommand(EditorActionId.INDENT_INCREASE, "Increase Indent", s -> s.adjustParagraphLeftIndent(8)));
    }

    public static List<EditorAction> layoutMenuActions() {
        return List.of(
                settings(EditorActionId.LAYOUT_MARGIN_NORMAL, "Normal Margins", s -> new dev.rgcb.scholar.document.DocumentSettings(s.template(), s.paper(), s.orientation(), dev.rgcb.scholar.document.PageMargins.normal(), s.columns(), s.decoration())),
                settings(EditorActionId.LAYOUT_MARGIN_NARROW, "Narrow Margins", s -> new dev.rgcb.scholar.document.DocumentSettings(s.template(), s.paper(), s.orientation(), dev.rgcb.scholar.document.PageMargins.narrow(), s.columns(), s.decoration())),
                settings(EditorActionId.LAYOUT_PORTRAIT, "Portrait", s -> new dev.rgcb.scholar.document.DocumentSettings(s.template(), s.paper(), dev.rgcb.scholar.document.PageOrientation.PORTRAIT, s.margins(), s.columns(), s.decoration())),
                settings(EditorActionId.LAYOUT_LANDSCAPE, "Landscape", s -> new dev.rgcb.scholar.document.DocumentSettings(s.template(), s.paper(), dev.rgcb.scholar.document.PageOrientation.LANDSCAPE, s.margins(), s.columns(), s.decoration())),
                settings(EditorActionId.LAYOUT_SIZE_LETTER, "Letter", s -> new dev.rgcb.scholar.document.DocumentSettings(s.template(), dev.rgcb.scholar.document.PaperSize.letter(), s.orientation(), s.margins(), s.columns(), s.decoration())),
                settings(EditorActionId.LAYOUT_SIZE_A4, "A4", s -> new dev.rgcb.scholar.document.DocumentSettings(s.template(), dev.rgcb.scholar.document.PaperSize.a4(), s.orientation(), s.margins(), s.columns(), s.decoration())),
                settings(EditorActionId.LAYOUT_SIZE_LEGAL, "Legal", s -> new dev.rgcb.scholar.document.DocumentSettings(s.template(), dev.rgcb.scholar.document.PaperSize.legal(), s.orientation(), s.margins(), s.columns(), s.decoration())),
                settings(EditorActionId.LAYOUT_ONE_COLUMN, "One Column", s -> s.withColumns(dev.rgcb.scholar.document.ColumnLayout.one())),
                settings(EditorActionId.LAYOUT_TWO_COLUMNS, "Two Columns", s -> s.withColumns(dev.rgcb.scholar.document.ColumnLayout.two())),
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
                plotAxisUnit(EditorActionId.PLOT_X_UNIT_AUTO, "Automatic X Unit", true, Optional.empty()),
                plotAxisUnit(EditorActionId.PLOT_X_UNIT_SECOND, "X Axis: s", true, Optional.of(unit("s"))),
                plotAxisUnit(EditorActionId.PLOT_X_UNIT_METRE, "X Axis: m", true, Optional.of(unit("m"))),
                plotAxisUnit(EditorActionId.PLOT_Y_UNIT_AUTO, "Automatic Y Unit", false, Optional.empty()),
                plotAxisUnit(EditorActionId.PLOT_Y_UNIT_CELSIUS, "Y Axis: °C", false, Optional.of(unit("°C"))),
                plotAxisUnit(EditorActionId.PLOT_Y_UNIT_KELVIN, "Y Axis: K", false, Optional.of(unit("K"))),
                plotAxisUnit(EditorActionId.PLOT_Y_UNIT_VOLT, "Y Axis: V", false, Optional.of(unit("V"))));
    }

    public static List<EditorAction> diagramMenuActions() {
        return List.of(
                addDiagramNode(),
                addElectricalComponent(EditorActionId.DIAGRAM_ADD_RESISTOR, "Add Resistor", ElectricalComponentKind.RESISTOR),
                addElectricalComponent(EditorActionId.DIAGRAM_ADD_CAPACITOR, "Add Capacitor", ElectricalComponentKind.CAPACITOR),
                addElectricalComponent(EditorActionId.DIAGRAM_ADD_DC_VOLTAGE_SOURCE, "Add DC Voltage Source", ElectricalComponentKind.DC_VOLTAGE_SOURCE),
                addElectricalComponent(EditorActionId.DIAGRAM_ADD_GROUND, "Add Ground", ElectricalComponentKind.GROUND),
                addElectricalComponent(EditorActionId.DIAGRAM_ADD_DIODE, "Add Diode", ElectricalComponentKind.DIODE),
                addElectricalComponent(EditorActionId.DIAGRAM_ADD_LED, "Add LED", ElectricalComponentKind.LED),
                addElectricalComponent(EditorActionId.DIAGRAM_ADD_SWITCH_SPST, "Add SPST Switch", ElectricalComponentKind.SWITCH_SPST),
                addElectricalJunction(),
                addMechanicalPrimitive(EditorActionId.DIAGRAM_ADD_MECHANICAL_LINE, "Add Mechanical Line", MechanicalPrimitiveKind.LINE),
                addMechanicalPrimitive(EditorActionId.DIAGRAM_ADD_MECHANICAL_CENTERLINE, "Add Centerline", MechanicalPrimitiveKind.CENTERLINE),
                addMechanicalPrimitive(EditorActionId.DIAGRAM_ADD_MECHANICAL_RECTANGLE, "Add Mechanical Rectangle", MechanicalPrimitiveKind.RECTANGLE),
                addMechanicalPrimitive(EditorActionId.DIAGRAM_ADD_MECHANICAL_CIRCLE, "Add Mechanical Circle", MechanicalPrimitiveKind.CIRCLE),
                addMechanicalPrimitive(EditorActionId.DIAGRAM_ADD_MECHANICAL_ARC, "Add Mechanical Arc", MechanicalPrimitiveKind.ARC),
                addMechanicalPrimitive(EditorActionId.DIAGRAM_ADD_MECHANICAL_ARROW, "Add Mechanical Arrow", MechanicalPrimitiveKind.ARROW),
                addMechanicalPrimitive(EditorActionId.DIAGRAM_ADD_MECHANICAL_REFERENCE_POINT, "Add Reference Point", MechanicalPrimitiveKind.REFERENCE_POINT),
                addMechanicalPartReference(),
                generateMechanicalBom(),
                addMechanicalAnnotation(EditorActionId.DIAGRAM_ADD_MECHANICAL_ANNOTATION_PART_LABEL, "Add Part Label", MechanicalAnnotationKind.PART_LABEL),
                addMechanicalAnnotation(EditorActionId.DIAGRAM_ADD_MECHANICAL_ANNOTATION_NOTE, "Add Mechanical Note", MechanicalAnnotationKind.NOTE),
                addMechanicalAnnotation(EditorActionId.DIAGRAM_ADD_MECHANICAL_ANNOTATION_LEADER, "Add Leader Callout", MechanicalAnnotationKind.LEADER),
                addMechanicalSymbol(EditorActionId.DIAGRAM_ADD_MECHANICAL_SYMBOL_SHAFT, "Add Shaft Symbol", MechanicalSymbolKind.SHAFT),
                addMechanicalSymbol(EditorActionId.DIAGRAM_ADD_MECHANICAL_SYMBOL_GEAR, "Add Gear Symbol", MechanicalSymbolKind.GEAR),
                addMechanicalSymbol(EditorActionId.DIAGRAM_ADD_MECHANICAL_SYMBOL_BEARING, "Add Bearing Symbol", MechanicalSymbolKind.BEARING),
                addMechanicalSymbol(EditorActionId.DIAGRAM_ADD_MECHANICAL_SYMBOL_SPRING, "Add Spring Symbol", MechanicalSymbolKind.SPRING),
                addMechanicalSymbol(EditorActionId.DIAGRAM_ADD_MECHANICAL_SYMBOL_PISTON, "Add Piston Symbol", MechanicalSymbolKind.PISTON),
                addMechanicalSymbol(EditorActionId.DIAGRAM_ADD_MECHANICAL_SYMBOL_BOLT, "Add Bolt Symbol", MechanicalSymbolKind.BOLT),
                addMechanicalDimension(EditorActionId.DIAGRAM_ADD_MECHANICAL_DIMENSION_HORIZONTAL, "Add Horizontal Dimension", MechanicalDimensionKind.HORIZONTAL),
                addMechanicalDimension(EditorActionId.DIAGRAM_ADD_MECHANICAL_DIMENSION_VERTICAL, "Add Vertical Dimension", MechanicalDimensionKind.VERTICAL),
                addMechanicalDimension(EditorActionId.DIAGRAM_ADD_MECHANICAL_DIMENSION_ALIGNED, "Add Aligned Dimension", MechanicalDimensionKind.ALIGNED),
                addMechanicalDimension(EditorActionId.DIAGRAM_ADD_MECHANICAL_DIMENSION_RADIUS, "Add Radius Dimension", MechanicalDimensionKind.RADIUS),
                addMechanicalDimension(EditorActionId.DIAGRAM_ADD_MECHANICAL_DIMENSION_DIAMETER, "Add Diameter Dimension", MechanicalDimensionKind.DIAMETER),
                addMechanicalDimension(EditorActionId.DIAGRAM_ADD_MECHANICAL_DIMENSION_ANGLE, "Add Angle Dimension", MechanicalDimensionKind.ANGLE),
                addMechanicalUnaryConstraint(EditorActionId.DIAGRAM_ADD_MECHANICAL_CONSTRAINT_HORIZONTAL, "Constrain Horizontal", MechanicalConstraintKind.HORIZONTAL),
                addMechanicalUnaryConstraint(EditorActionId.DIAGRAM_ADD_MECHANICAL_CONSTRAINT_VERTICAL, "Constrain Vertical", MechanicalConstraintKind.VERTICAL),
                startMechanicalConstraint(EditorActionId.DIAGRAM_START_MECHANICAL_CONSTRAINT_COINCIDENT, "Start Coincident", MechanicalConstraintKind.COINCIDENT),
                startMechanicalConstraint(EditorActionId.DIAGRAM_START_MECHANICAL_CONSTRAINT_PARALLEL, "Start Parallel", MechanicalConstraintKind.PARALLEL),
                startMechanicalConstraint(EditorActionId.DIAGRAM_START_MECHANICAL_CONSTRAINT_PERPENDICULAR, "Start Perpendicular", MechanicalConstraintKind.PERPENDICULAR),
                startMechanicalConstraint(EditorActionId.DIAGRAM_START_MECHANICAL_CONSTRAINT_CONCENTRIC, "Start Concentric", MechanicalConstraintKind.CONCENTRIC),
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
                datasetColumnUnit(EditorActionId.DATA_COLUMN_UNIT_NONE, "Unitless", Optional.empty()),
                datasetColumnUnit(EditorActionId.DATA_COLUMN_UNIT_METRE, "Metre (m)", Optional.of(unit("m"))),
                datasetColumnUnit(EditorActionId.DATA_COLUMN_UNIT_SECOND, "Second (s)", Optional.of(unit("s"))),
                datasetColumnUnit(EditorActionId.DATA_COLUMN_UNIT_CELSIUS, "Celsius (°C)", Optional.of(unit("°C"))),
                datasetColumnUnit(EditorActionId.DATA_COLUMN_UNIT_KELVIN, "Kelvin (K)", Optional.of(unit("K"))),
                datasetColumnUnit(EditorActionId.DATA_COLUMN_UNIT_VOLT, "Volt (V)", Optional.of(unit("V"))),
                datasetColumnUnit(EditorActionId.DATA_COLUMN_UNIT_AMPERE, "Ampere (A)", Optional.of(unit("A"))),
                datasetColumnUnit(EditorActionId.DATA_COLUMN_UNIT_OHM, "Ohm (Ω)", Optional.of(unit("Ω"))));
    }

    private static EditorAction insertQuantity(EditorActionId id, String label, String unit) {
        return insertQuantity(id, label, unit, "1");
    }

    private static EditorAction insertQuantity(EditorActionId id, String label, String unit, String value) {
        return new SimpleAction(id, label, "Insert a semantic scientific quantity", null,
                context -> context.session().supportsInsertQuantity(),
                context -> context.session().insertQuantity(new Quantity(new BigDecimal(value), unit(unit)), NumberNotation.DECIMAL)
                        ? EditorActionResult.DOCUMENT_CHANGED : EditorActionResult.NONE);
    }

    private static EditorAction datasetColumnUnit(EditorActionId id, String label, Optional<UnitExpression> unit) {
        return new SimpleAction(id, label, "Set the selected numeric dataset column unit", null,
                context -> context.session().supportsSetSelectedDatasetColumnUnit(),
                context -> context.session().setSelectedDatasetColumnUnit(unit)
                        ? EditorActionResult.DOCUMENT_CHANGED : EditorActionResult.NONE);
    }

    private static EditorAction convertQuantity(EditorActionId id, String label, String target) {
        var unit = unit(target);
        return new SimpleAction(id, label, "Convert the semantic quantity at the caret", null,
                context -> context.session().supportsConvertQuantityAtCaret(unit),
                context -> context.session().convertQuantityAtCaret(unit)
                        ? EditorActionResult.DOCUMENT_CHANGED : EditorActionResult.NONE);
    }

    private static EditorAction quantityNotation(EditorActionId id, String label, NumberNotation notation) {
        return new SimpleAction(id, label, "Set quantity number presentation", null,
                context -> context.session().supportsEditQuantityAtCaret(),
                context -> context.session().setQuantityNotationAtCaret(notation)
                        ? EditorActionResult.DOCUMENT_CHANGED : EditorActionResult.NONE);
    }

    private static EditorAction plotAxisUnit(EditorActionId id, String label, boolean xAxis, Optional<UnitExpression> unit) {
        return new SimpleAction(id, label, "Set the selected plot " + (xAxis ? "X" : "Y") + "-axis display unit", null,
                context -> context.session().supportsSetSelectedPlotAxisDisplayUnit(),
                context -> context.session().setSelectedPlotAxisDisplayUnit(xAxis, unit)
                        ? EditorActionResult.DOCUMENT_CHANGED : EditorActionResult.NONE);
    }

    private static UnitExpression unit(String symbol) {
        return new UnitParser().parseRequired(symbol);
    }

    public static EditorAction wrapPlotInFigure() {
        return new SimpleAction(
                EditorActionId.FIGURE_WRAP_PLOT,
                "Wrap Plot",
                null,
                context -> context.session().supportsWrapSelectedPlotInFigure(),
                context -> context.session().wrapSelectedPlotInFigure()
                        ? EditorActionResult.DOCUMENT_CHANGED
                        : EditorActionResult.NONE);
    }

    public static EditorAction wrapDiagramInFigure() {
        return new SimpleAction(
                EditorActionId.FIGURE_WRAP_DIAGRAM,
                "Wrap Diagram",
                null,
                context -> context.session().supportsWrapSelectedDiagramInFigure(),
                context -> context.session().wrapSelectedDiagramInFigure()
                        ? EditorActionResult.DOCUMENT_CHANGED
                        : EditorActionResult.NONE);
    }

    public static EditorAction insertCrossReference() {
        return new SimpleAction(
                EditorActionId.INSERT_CROSS_REFERENCE,
                "Cross Reference",
                "Insert cross-reference",
                null,
                context -> context.session().supportsInsertCrossReference(),
                context -> EditorActionResult.openCrossReferencePopup());
    }

    public static EditorAction insertTableOfContents() {
        return new SimpleAction(
                EditorActionId.INSERT_TABLE_OF_CONTENTS,
                "Table of Contents",
                "Insert table of contents",
                null,
                context -> context.session().supportsInsertTableOfContents(),
                context -> context.session().insertTableOfContents()
                        ? EditorActionResult.DOCUMENT_CHANGED
                        : EditorActionResult.NONE);
    }

    public static EditorAction toggleOutline() {
        return new SimpleAction(
                EditorActionId.TOGGLE_OUTLINE,
                "Outline",
                "Show or hide document outline",
                null,
                context -> true,
                context -> EditorActionResult.requestToggleOutline());
    }

    public static EditorAction newDataset() {
        return new SimpleAction(
                EditorActionId.DATA_NEW_DATASET,
                "New Dataset",
                "Create an empty dataset",
                null,
                context -> context.session().supportsDatasetDocumentAction(),
                context -> context.session().createDefaultDataset()
                        ? EditorActionResult.DOCUMENT_CHANGED
                        : EditorActionResult.NONE);
    }

    public static EditorAction insertDatasetTable() {
        return new SimpleAction(
                EditorActionId.DATA_INSERT_DATASET_TABLE,
                "Dataset Table",
                "Insert dataset-backed table",
                null,
                context -> context.session().supportsInsertDatasetTable(),
                context -> context.session().insertDatasetTableForFirstDataset()
                        ? EditorActionResult.DOCUMENT_CHANGED
                        : EditorActionResult.NONE);
    }

    public static EditorAction bindPlotToDataset() {
        return new SimpleAction(
                EditorActionId.DATA_BIND_PLOT_TO_DATASET,
                "Bind Plot",
                "Bind selected plot to dataset",
                null,
                context -> context.session().supportsBindSelectedPlotToFirstDataset(),
                context -> context.session().bindSelectedPlotToFirstDataset()
                        ? EditorActionResult.DOCUMENT_CHANGED
                        : EditorActionResult.NONE);
    }

    public static EditorAction editFigureCaption() {
        return new SimpleAction(
                EditorActionId.FIGURE_EDIT_CAPTION,
                "Edit Caption",
                null,
                context -> context.session().supportsEditFigureCaption(),
                context -> {
                    context.session().editFigureCaption();
                    return EditorActionResult.NONE;
                });
    }

    public static EditorAction unwrapFigure() {
        return new SimpleAction(
                EditorActionId.FIGURE_UNWRAP,
                "Unwrap Figure",
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
                EditorActionId.UNDO,
                "Undo",
                ActionShortcut.ctrl(ActionShortcut.Key.Z),
                context -> context.session().canUndo(),
                context -> context.session().undo() ? EditorActionResult.DOCUMENT_CHANGED : EditorActionResult.NONE);
    }

    public static EditorAction redo() {
        return new SimpleAction(
                EditorActionId.REDO,
                "Redo",
                ActionShortcut.of(
                        new ActionShortcut.Stroke(ActionShortcut.Key.Y, true, false),
                        new ActionShortcut.Stroke(ActionShortcut.Key.Z, true, true)),
                context -> context.session().canRedo(),
                context -> context.session().redo() ? EditorActionResult.DOCUMENT_CHANGED : EditorActionResult.NONE);
    }

    public static EditorAction cut() {
        return new SimpleAction(
                EditorActionId.CUT,
                "Cut",
                ActionShortcut.ctrl(ActionShortcut.Key.X),
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
                EditorActionId.COPY,
                "Copy",
                ActionShortcut.ctrl(ActionShortcut.Key.C),
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
                EditorActionId.PASTE,
                "Paste",
                ActionShortcut.ctrl(ActionShortcut.Key.V),
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
                EditorActionId.DELETE,
                "Delete",
                ActionShortcut.plain(ActionShortcut.Key.DELETE),
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
                EditorActionId.INSERT_EQUATION,
                "Equation",
                null,
                context -> context.session().supportsInsertEquation(),
                context -> context.session().insertEmptyEquation()
                        ? EditorActionResult.DOCUMENT_CHANGED
                        : EditorActionResult.NONE);
    }

    public static EditorAction insertTable() {
        return new SimpleAction(
                EditorActionId.INSERT_TABLE,
                "Table",
                null,
                context -> context.session().supportsInsertTable(),
                context -> context.session().insertDefaultTable()
                        ? EditorActionResult.DOCUMENT_CHANGED
                        : EditorActionResult.NONE);
    }

    public static EditorAction insertPlot() {
        return new SimpleAction(
                EditorActionId.INSERT_PLOT,
                "Plot",
                null,
                context -> context.session().supportsInsertPlot(),
                context -> context.session().insertDefaultPlot()
                        ? EditorActionResult.DOCUMENT_CHANGED
                        : EditorActionResult.NONE);
    }

    public static EditorAction insertDiagram() {
        return new SimpleAction(
                EditorActionId.INSERT_DIAGRAM,
                "Diagram",
                null,
                context -> context.session().supportsInsertDiagram(),
                context -> context.session().insertDefaultDiagram()
                        ? EditorActionResult.DOCUMENT_CHANGED
                        : EditorActionResult.NONE);
    }

    public static EditorAction insertFraction() {
        return new SimpleAction(
                EditorActionId.MATH_INSERT_FRACTION,
                "Fraction",
                "Insert fraction",
                null,
                context -> context.session().supportsInsertFraction(),
                context -> context.session().insertFraction()
                        ? EditorActionResult.DOCUMENT_CHANGED
                        : EditorActionResult.NONE);
    }

    public static EditorAction insertRoot() {
        return new SimpleAction(
                EditorActionId.MATH_INSERT_ROOT,
                "Root",
                "Insert square root",
                null,
                context -> context.session().supportsInsertRoot(),
                context -> context.session().insertRoot()
                        ? EditorActionResult.DOCUMENT_CHANGED
                        : EditorActionResult.NONE);
    }

    public static EditorAction insertParenthesesGroup() {
        return insertGroup(EditorActionId.MATH_INSERT_PARENTHESES_GROUP, "Parentheses", "Insert parentheses group", MathDelimiter.PARENTHESES);
    }

    public static EditorAction insertBracketsGroup() {
        return insertGroup(EditorActionId.MATH_INSERT_BRACKETS_GROUP, "Brackets", "Insert brackets group", MathDelimiter.BRACKETS);
    }

    public static EditorAction insertBracesGroup() {
        return insertGroup(EditorActionId.MATH_INSERT_BRACES_GROUP, "Braces", "Insert braces group", MathDelimiter.BRACES);
    }

    public static EditorAction insertSuperscript() {
        return insertScriptSlot(EditorActionId.MATH_INSERT_SUPERSCRIPT, "Superscript", "Insert superscript", ScriptSlot.SUPERSCRIPT);
    }

    public static EditorAction insertSubscript() {
        return insertScriptSlot(EditorActionId.MATH_INSERT_SUBSCRIPT, "Subscript", "Insert subscript", ScriptSlot.SUBSCRIPT);
    }

    public static EditorAction convertToNamedOperator() {
        return semanticConversionAction(
                EditorActionId.MATH_CONVERT_NAMED_OPERATOR,
                "Named Operator",
                SemanticMathTokenKind.NAMED_OPERATOR);
    }

    public static EditorAction convertToMathText() {
        return semanticConversionAction(
                EditorActionId.MATH_CONVERT_TEXT,
                "Math Text",
                SemanticMathTokenKind.MATH_TEXT);
    }

    public static EditorAction bold() {
        return formatAction(EditorActionId.BOLD, "Bold", ActionShortcut.ctrl(ActionShortcut.Key.B), TextMark.BOLD);
    }

    public static EditorAction italic() {
        return formatAction(EditorActionId.ITALIC, "Italic", ActionShortcut.ctrl(ActionShortcut.Key.I), TextMark.ITALIC);
    }

    public static EditorAction underline() {
        return formatAction(EditorActionId.UNDERLINE, "Underline", null, TextMark.UNDERLINE);
    }

    public static EditorAction textSuperscript() {
        return formatAction(EditorActionId.TEXT_SUPERSCRIPT, "Superscript", null, TextMark.SUPERSCRIPT);
    }

    public static EditorAction textSubscript() {
        return formatAction(EditorActionId.TEXT_SUBSCRIPT, "Subscript", null, TextMark.SUBSCRIPT);
    }

    public static EditorAction insertPageBreak() {
        return new SimpleAction(EditorActionId.INSERT_PAGE_BREAK, "Page Break", null,
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
                EditorActionId.TABLE_INSERT_ROW_ABOVE,
                "Insert Row Above",
                context -> context.session().supportsInsertTableRow(),
                session -> session.insertTableRowAbove());
    }

    public static EditorAction insertTableRowBelow() {
        return tableAction(
                EditorActionId.TABLE_INSERT_ROW_BELOW,
                "Insert Row Below",
                context -> context.session().supportsInsertTableRow(),
                session -> session.insertTableRowBelow());
    }

    public static EditorAction deleteTableRow() {
        return tableAction(
                EditorActionId.TABLE_DELETE_ROW,
                "Delete Row",
                context -> context.session().supportsDeleteTableRow(),
                session -> session.deleteTableRow());
    }

    public static EditorAction insertTableColumnLeft() {
        return tableAction(
                EditorActionId.TABLE_INSERT_COLUMN_LEFT,
                "Insert Column Left",
                context -> context.session().supportsInsertTableColumn(),
                session -> session.insertTableColumnLeft());
    }

    public static EditorAction insertTableColumnRight() {
        return tableAction(
                EditorActionId.TABLE_INSERT_COLUMN_RIGHT,
                "Insert Column Right",
                context -> context.session().supportsInsertTableColumn(),
                session -> session.insertTableColumnRight());
    }

    public static EditorAction deleteTableColumn() {
        return tableAction(
                EditorActionId.TABLE_DELETE_COLUMN,
                "Delete Column",
                context -> context.session().supportsDeleteTableColumn(),
                session -> session.deleteTableColumn());
    }

    public static EditorAction togglePlotGrid() {
        return new SimpleAction(
                EditorActionId.PLOT_TOGGLE_GRID,
                "Grid",
                null,
                context -> context.session().supportsPlotEditingAction(),
                context -> context.session().togglePlotGrid() ? EditorActionResult.DOCUMENT_CHANGED : EditorActionResult.NONE,
                context -> !context.session().supportsPlotEditingAction()
                        ? ActionSelectionState.NOT_APPLICABLE
                        : context.session().plotGridVisible() ? ActionSelectionState.ON : ActionSelectionState.OFF);
    }

    public static EditorAction togglePlotLegend() {
        return new SimpleAction(
                EditorActionId.PLOT_TOGGLE_LEGEND,
                "Legend",
                null,
                context -> context.session().supportsPlotEditingAction(),
                context -> context.session().togglePlotLegend() ? EditorActionResult.DOCUMENT_CHANGED : EditorActionResult.NONE,
                context -> !context.session().supportsPlotEditingAction()
                        ? ActionSelectionState.NOT_APPLICABLE
                        : context.session().plotLegendVisible() ? ActionSelectionState.ON : ActionSelectionState.OFF);
    }

    public static EditorAction addPlotLineSeries() {
        return plotAction(
                EditorActionId.PLOT_ADD_LINE_SERIES,
                "Add Line Series",
                context -> context.session().supportsPlotEditingAction(),
                session -> session.addPlotSeries(PlotSeriesKind.LINE));
    }

    public static EditorAction addPlotScatterSeries() {
        return plotAction(
                EditorActionId.PLOT_ADD_SCATTER_SERIES,
                "Add Scatter Series",
                context -> context.session().supportsPlotEditingAction(),
                session -> session.addPlotSeries(PlotSeriesKind.SCATTER));
    }

    public static EditorAction setPlotSeriesLine() {
        return new SimpleAction(
                EditorActionId.PLOT_SET_SERIES_LINE,
                "Line Series",
                null,
                context -> context.session().supportsSetPlotSeriesKind(),
                context -> context.session().setPlotSeriesKind(PlotSeriesKind.LINE) ? EditorActionResult.DOCUMENT_CHANGED : EditorActionResult.NONE,
                context -> context.session().selectedPlotSeriesKind()
                        .map(kind -> kind == PlotSeriesKind.LINE ? ActionSelectionState.ON : ActionSelectionState.OFF)
                        .orElse(ActionSelectionState.NOT_APPLICABLE));
    }

    public static EditorAction setPlotSeriesScatter() {
        return new SimpleAction(
                EditorActionId.PLOT_SET_SERIES_SCATTER,
                "Scatter Series",
                null,
                context -> context.session().supportsSetPlotSeriesKind(),
                context -> context.session().setPlotSeriesKind(PlotSeriesKind.SCATTER) ? EditorActionResult.DOCUMENT_CHANGED : EditorActionResult.NONE,
                context -> context.session().selectedPlotSeriesKind()
                        .map(kind -> kind == PlotSeriesKind.SCATTER ? ActionSelectionState.ON : ActionSelectionState.OFF)
                        .orElse(ActionSelectionState.NOT_APPLICABLE));
    }

    public static EditorAction addPlotPoint() {
        return plotAction(
                EditorActionId.PLOT_ADD_POINT,
                "Add Point",
                context -> context.session().supportsAddPlotPoint(),
                EditorSession::addPlotPoint);
    }

    public static EditorAction deletePlotPoint() {
        return plotAction(
                EditorActionId.PLOT_DELETE_POINT,
                "Delete Point",
                context -> context.session().supportsDeletePlotPoint(),
                EditorSession::deletePlotPoint);
    }

    public static EditorAction deletePlotSeries() {
        return plotAction(
                EditorActionId.PLOT_DELETE_SERIES,
                "Delete Series",
                context -> context.session().supportsDeletePlotSeries(),
                EditorSession::deletePlotSeries);
    }

    public static EditorAction addDiagramNode() {
        return diagramAction(
                EditorActionId.DIAGRAM_ADD_NODE,
                "Add Node",
                context -> context.session().supportsDiagramEditingAction(),
                EditorSession::addDiagramNode);
    }

    public static EditorAction deleteDiagramNode() {
        return diagramAction(
                EditorActionId.DIAGRAM_DELETE_NODE,
                "Delete Node",
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

    public static EditorAction addMechanicalPartReference(){return diagramAction(EditorActionId.DIAGRAM_ADD_MECHANICAL_PART_REFERENCE,"Add Part Balloon",context->context.session().supportsAddMechanicalPartReference(),EditorSession::addMechanicalPartReference);}
    public static EditorAction generateMechanicalBom(){return diagramAction(EditorActionId.DIAGRAM_GENERATE_MECHANICAL_BOM,"Generate BOM",context->context.session().supportsGenerateMechanicalBom(),EditorSession::generateMechanicalBom);}
    public static EditorAction deleteMechanicalPartReference(){return diagramAction(EditorActionId.DIAGRAM_DELETE_MECHANICAL_PART_REFERENCE,"Delete Part Balloon",context->context.session().supportsDeleteMechanicalPartReference(),EditorSession::deleteMechanicalPartReference);}

    public static EditorAction addMechanicalAnnotation(EditorActionId id,String label,MechanicalAnnotationKind kind){Objects.requireNonNull(kind);return diagramAction(id,label,context->context.session().supportsDiagramEditingAction(),session->session.addMechanicalAnnotation(kind));}
    public static EditorAction deleteMechanicalAnnotation(){return diagramAction(EditorActionId.DIAGRAM_DELETE_MECHANICAL_ANNOTATION,"Delete Mechanical Annotation",context->context.session().supportsDeleteMechanicalAnnotation(),EditorSession::deleteMechanicalAnnotation);}

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
                EditorActionId.DIAGRAM_FINISH_MECHANICAL_CONSTRAINT,
                "Finish Mechanical Constraint",
                context -> context.session().supportsFinishMechanicalConstraint(),
                EditorSession::finishMechanicalConstraint);
    }

    public static EditorAction cancelMechanicalConstraint() {
        return new SimpleAction(
                EditorActionId.DIAGRAM_CANCEL_MECHANICAL_CONSTRAINT,
                "Cancel Mechanical Constraint",
                null,
                context -> context.session().hasPendingMechanicalConstraint(),
                context -> {
                    context.session().cancelMechanicalConstraint();
                    return EditorActionResult.NONE;
                });
    }

    public static EditorAction deleteMechanicalConstraint() {
        return diagramAction(
                EditorActionId.DIAGRAM_DELETE_MECHANICAL_CONSTRAINT,
                "Delete Mechanical Constraint",
                context -> context.session().supportsDeleteMechanicalConstraint(),
                EditorSession::deleteMechanicalConstraint);
    }

    public static EditorAction deleteMechanicalSymbol() { return diagramAction(EditorActionId.DIAGRAM_DELETE_MECHANICAL_SYMBOL, "Delete Mechanical Symbol", context -> context.session().supportsDeleteMechanicalSymbol(), EditorSession::deleteMechanicalSymbol); }

    public static EditorAction deleteMechanicalDimension() {
        return diagramAction(
                EditorActionId.DIAGRAM_DELETE_MECHANICAL_DIMENSION,
                "Delete Mechanical Dimension",
                context -> context.session().supportsDeleteMechanicalDimension(),
                EditorSession::deleteMechanicalDimension);
    }

    public static EditorAction deleteMechanicalPrimitive() {
        return diagramAction(
                EditorActionId.DIAGRAM_DELETE_MECHANICAL_PRIMITIVE,
                "Delete Mechanical Primitive",
                context -> context.session().supportsDeleteMechanicalPrimitive(),
                EditorSession::deleteMechanicalPrimitive);
    }

    public static EditorAction addElectricalJunction() {
        return diagramAction(
                EditorActionId.DIAGRAM_ADD_JUNCTION,
                "Add Junction",
                context -> context.session().supportsDiagramEditingAction(),
                EditorSession::addElectricalJunction);
    }

    public static EditorAction deleteElectricalJunction() {
        return diagramAction(
                EditorActionId.DIAGRAM_DELETE_JUNCTION,
                "Delete Junction",
                context -> context.session().supportsDeleteElectricalJunction(),
                EditorSession::deleteElectricalJunction);
    }

    public static EditorAction scaleElectricalSymbolsDown() {
        return diagramAction(
                EditorActionId.DIAGRAM_SCALE_SYMBOLS_DOWN,
                "Scale Symbols Down",
                context -> context.session().supportsScaleElectricalSymbols(),
                session -> session.scaleElectricalSymbols(0.90));
    }

    public static EditorAction scaleElectricalSymbolsUp() {
        return diagramAction(
                EditorActionId.DIAGRAM_SCALE_SYMBOLS_UP,
                "Scale Symbols Up",
                context -> context.session().supportsScaleElectricalSymbols(),
                session -> session.scaleElectricalSymbols(1.10));
    }

    public static EditorAction shortenDiagramWorkspace() {
        return diagramAction(
                EditorActionId.DIAGRAM_WORKSPACE_SHORTER,
                "Workspace Shorter",
                context -> context.session().supportsDiagramEditingAction(),
                session -> session.scaleDiagramWorkspaceHeight(0.90));
    }

    public static EditorAction heightenDiagramWorkspace() {
        return diagramAction(
                EditorActionId.DIAGRAM_WORKSPACE_TALLER,
                "Workspace Taller",
                context -> context.session().supportsDiagramEditingAction(),
                session -> session.scaleDiagramWorkspaceHeight(1.10));
    }

    public static EditorAction resetDiagramWorkspaceHeight() {
        return diagramAction(
                EditorActionId.DIAGRAM_WORKSPACE_RESET_HEIGHT,
                "Reset Workspace Height",
                context -> context.session().supportsDiagramEditingAction(),
                EditorSession::resetDiagramWorkspaceHeight);
    }

    public static EditorAction rotateElectricalComponentClockwise() {
        return diagramAction(
                EditorActionId.DIAGRAM_ROTATE_CLOCKWISE,
                "Rotate Clockwise",
                context -> context.session().supportsRotateElectricalComponent(),
                EditorSession::rotateElectricalComponentClockwise);
    }

    public static EditorAction rotateElectricalComponentCounterClockwise() {
        return diagramAction(
                EditorActionId.DIAGRAM_ROTATE_COUNTERCLOCKWISE,
                "Rotate Counterclockwise",
                context -> context.session().supportsRotateElectricalComponent(),
                EditorSession::rotateElectricalComponentCounterClockwise);
    }

    public static EditorAction deleteElectricalComponent() {
        return diagramAction(
                EditorActionId.DIAGRAM_DELETE_ELECTRICAL_COMPONENT,
                "Delete Component",
                context -> context.session().supportsDeleteElectricalComponent(),
                EditorSession::deleteElectricalComponent);
    }

    public static EditorAction startDiagramConnection() {
        return new SimpleAction(
                EditorActionId.DIAGRAM_START_CONNECTION,
                "Start Connection",
                null,
                context -> context.session().supportsBeginDiagramConnection(),
                context -> {
                    context.session().beginDiagramConnection();
                    return EditorActionResult.NONE;
                });
    }

    public static EditorAction finishDiagramConnection() {
        return diagramAction(
                EditorActionId.DIAGRAM_FINISH_CONNECTION,
                "Finish Connection",
                context -> context.session().supportsCompleteDiagramConnection(),
                EditorSession::completeDiagramConnection);
    }

    public static EditorAction cancelDiagramConnection() {
        return new SimpleAction(
                EditorActionId.DIAGRAM_CANCEL_CONNECTION,
                "Cancel Connection",
                null,
                context -> context.session().diagramConnectionInProgress(),
                context -> {
                    context.session().cancelDiagramConnection();
                    return EditorActionResult.NONE;
                });
    }

    public static EditorAction deleteDiagramConnection() {
        return diagramAction(
                EditorActionId.DIAGRAM_DELETE_CONNECTION,
                "Delete Connection",
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
                "Convert to " + label,
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
            this.label = Objects.requireNonNull(label, "label");
            this.tooltip = Objects.requireNonNull(tooltip, "tooltip");
            this.shortcut = shortcut;
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
