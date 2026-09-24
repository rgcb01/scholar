package dev.rgcb.scholar.client.ui;

import dev.rgcb.scholar.editor.EditorAction;
import dev.rgcb.scholar.editor.EditorActionId;
import dev.rgcb.scholar.editor.BuiltInEditorActions;
import java.util.ArrayList;
import java.util.List;

/** Production ribbon metadata; command behavior remains owned by EditorAction. */
public final class ScholarRibbonModel {
    private ScholarRibbonModel() { }

    public static List<RibbonTabDefinition> production(
            List<EditorAction> file, List<EditorAction> edit, List<EditorAction> format,
            List<EditorAction> styles, List<EditorAction> insert, List<EditorAction> data,
            List<EditorAction> table, List<EditorAction> plot, List<EditorAction> figure,
            List<EditorAction> diagram, List<EditorAction> view) {
        return production(file, edit, format, styles, insert, data, table, plot, figure, diagram,
                BuiltInEditorActions.layoutMenuActions(), view);
    }

    public static List<RibbonTabDefinition> production(
            List<EditorAction> file, List<EditorAction> edit, List<EditorAction> format,
            List<EditorAction> styles, List<EditorAction> insert, List<EditorAction> data,
            List<EditorAction> table, List<EditorAction> plot, List<EditorAction> figure,
            List<EditorAction> diagram, List<EditorAction> layout, List<EditorAction> view) {
        var all = new ArrayList<EditorAction>();
        java.util.Arrays.asList(file, edit, format, styles, insert, data, table, plot, figure, diagram, layout, view).forEach(all::addAll);
        var tabs = new ArrayList<RibbonTabDefinition>();
        tabs.addAll(java.util.Arrays.asList(
                tab("File", group("Document", large(all, EditorActionId.FILE_NEW), List.of(
                        command(all, EditorActionId.FILE_OPEN, RibbonCommandSize.MEDIUM, "Open"),
                        command(all, EditorActionId.FILE_SAVE, RibbonCommandSize.MEDIUM, "Save"),
                        command(all, EditorActionId.FILE_SAVE_AS, RibbonCommandSize.MEDIUM, "Save As"))),
                        group("Manage", medium(all, EditorActionId.FILE_RENAME, EditorActionId.FILE_CLOSE)),
                        group("Interchange", medium(all, EditorActionId.FILE_IMPORT_CSV,
                                EditorActionId.FILE_EXPORT_MARKDOWN, EditorActionId.FILE_EXPORT_PDF))),
                tab("Home", group("Clipboard", large(all, EditorActionId.PASTE), small(all,
                        EditorActionId.CUT, EditorActionId.COPY)),
                        group("History", medium(all, EditorActionId.UNDO, EditorActionId.REDO)),
                        new RibbonGroupDefinition("Style", List.of(RibbonCommandPresentation.valueDropdown(
                                find(all, EditorActionId.STYLE_BODY), styleChoices(styles, format), "Style", "Style"))),
                        group("Font", medium(all, EditorActionId.BOLD, EditorActionId.ITALIC, EditorActionId.UNDERLINE,
                                EditorActionId.TEXT_SUBSCRIPT, EditorActionId.TEXT_SUPERSCRIPT),
                                List.of(RibbonCommandPresentation.valueDropdown(find(all, EditorActionId.FONT_SOURCE_SANS),
                                        select(format, EditorActionId.FONT_SOURCE_SANS, EditorActionId.FONT_SCIENTIFIC_MATH), "Font", "Font")),
                                List.of(RibbonCommandPresentation.valueDropdown(find(all, EditorActionId.FONT_SIZE_10),
                                        select(format, EditorActionId.FONT_SIZE_10, EditorActionId.FONT_SIZE_12, EditorActionId.FONT_SIZE_14), "Size", "Size"))),
                        group("Paragraph", medium(all, EditorActionId.ALIGN_LEFT, EditorActionId.ALIGN_CENTER,
                                EditorActionId.ALIGN_RIGHT, EditorActionId.ALIGN_JUSTIFIED),
                                medium(all, EditorActionId.LINE_SPACING_SINGLE, EditorActionId.LINE_SPACING_ONE_HALF,
                                        EditorActionId.INDENT_DECREASE, EditorActionId.INDENT_INCREASE))),
                tab("Insert", group("Structure", large(all, EditorActionId.INSERT_TABLE, EditorActionId.INSERT_EQUATION)),
                        group("Computation", medium(all, EditorActionId.INSERT_VARIABLE, EditorActionId.INSERT_COMPUTED_RESULT,
                                EditorActionId.EDIT_VARIABLE, EditorActionId.EDIT_COMPUTED_RESULT)),
                        group("Scientific", medium(all, EditorActionId.INSERT_PLOT, EditorActionId.INSERT_DIAGRAM),
                                List.of(command(all, EditorActionId.DATA_NEW_DATASET, RibbonCommandSize.MEDIUM, "Dataset"),
                                        palette(insert, "Quantity", "Quantity", ScholarIcons.UNIT,
                                                EditorActionId.INSERT_QUANTITY_METRE,
                                                EditorActionId.INSERT_QUANTITY_CELSIUS,
                                                EditorActionId.INSERT_QUANTITY_CELSIUS_DIFFERENCE,
                                                EditorActionId.INSERT_QUANTITY_VOLT,
                                                EditorActionId.INSERT_QUANTITY_MILLIAMPERE,
                                                EditorActionId.INSERT_QUANTITY_KILOOHM,
                                                EditorActionId.INSERT_QUANTITY_ACCELERATION),
                                        palette(insert, "Convert To", "Convert", ScholarIcons.UNIT,
                                                EditorActionId.QUANTITY_CONVERT_METRE,
                                                EditorActionId.QUANTITY_CONVERT_MILLIMETRE,
                                                EditorActionId.QUANTITY_CONVERT_CELSIUS,
                                                EditorActionId.QUANTITY_CONVERT_KELVIN),
                                        palette(insert, "Number Format", "Format", ScholarIcons.UNIT,
                                                EditorActionId.QUANTITY_FORMAT_DECIMAL,
                                                EditorActionId.QUANTITY_FORMAT_SCIENTIFIC,
                                                EditorActionId.QUANTITY_FORMAT_ENGINEERING))),
                        group("References", List.of(
                                command(all, EditorActionId.INSERT_CROSS_REFERENCE, RibbonCommandSize.MEDIUM, "Reference"),
                                command(all, EditorActionId.INSERT_TABLE_OF_CONTENTS, RibbonCommandSize.MEDIUM, "Contents"),
                                command(all, EditorActionId.INSERT_PAGE_BREAK, RibbonCommandSize.MEDIUM, "Page Break")))),
                tab("Data", group("Dataset",
                        List.of(command(all, EditorActionId.DATA_NEW_DATASET, RibbonCommandSize.LARGE, "Dataset"),
                                command(all, EditorActionId.DATA_INSERT_DATASET_TABLE, RibbonCommandSize.MEDIUM, "Table"),
                                command(all, EditorActionId.DATA_BIND_PLOT_TO_DATASET, RibbonCommandSize.MEDIUM, "Bind Plot"),
                                palette(data, "Column Unit", "Unit", ScholarIcons.UNIT,
                                        EditorActionId.DATA_COLUMN_UNIT_NONE, EditorActionId.DATA_COLUMN_UNIT_METRE,
                                        EditorActionId.DATA_COLUMN_UNIT_SECOND, EditorActionId.DATA_COLUMN_UNIT_CELSIUS,
                                        EditorActionId.DATA_COLUMN_UNIT_KELVIN, EditorActionId.DATA_COLUMN_UNIT_CELSIUS_DIFFERENCE,
                                        EditorActionId.DATA_COLUMN_UNIT_KELVIN_DIFFERENCE, EditorActionId.DATA_COLUMN_UNIT_VOLT,
                                        EditorActionId.DATA_COLUMN_UNIT_AMPERE, EditorActionId.DATA_COLUMN_UNIT_OHM))),
                        group("Interchange", medium(all, EditorActionId.DATA_EXPORT_CSV)),
                        group("Analysis", medium(all, EditorActionId.DATA_INSERT_ANALYSIS,
                                EditorActionId.DATA_EDIT_ANALYSIS, EditorActionId.DATA_ADD_FIT_OVERLAY)),
                        group("Table", compact(table)), group("Plot", compact(plot), List.of(
                                palette(plot, "X Axis Unit", "X Unit", ScholarIcons.UNIT,
                                        EditorActionId.PLOT_X_UNIT_AUTO, EditorActionId.PLOT_X_UNIT_SECOND,
                                        EditorActionId.PLOT_X_UNIT_METRE),
                                palette(plot, "Y Axis Unit", "Y Unit", ScholarIcons.UNIT,
                                        EditorActionId.PLOT_Y_UNIT_AUTO, EditorActionId.PLOT_Y_UNIT_CELSIUS,
                                        EditorActionId.PLOT_Y_UNIT_KELVIN, EditorActionId.PLOT_Y_UNIT_CELSIUS_DIFFERENCE,
                                        EditorActionId.PLOT_Y_UNIT_KELVIN_DIFFERENCE, EditorActionId.PLOT_Y_UNIT_VOLT)))),
                tab("Figure", group("Compose", medium(figure)), group("Insert", large(all,
                        EditorActionId.INSERT_PLOT, EditorActionId.INSERT_DIAGRAM))),
                tab("Diagram", group("Create", large(all, EditorActionId.INSERT_DIAGRAM)),
                        group("Electrical", List.of(
                                palette(diagram, "Components", "Parts", ScholarIcons.ELECTRICAL,
                                        EditorActionId.DIAGRAM_ADD_NODE, EditorActionId.DIAGRAM_ADD_RESISTOR,
                                        EditorActionId.DIAGRAM_ADD_CAPACITOR, EditorActionId.DIAGRAM_ADD_DC_VOLTAGE_SOURCE,
                                        EditorActionId.DIAGRAM_ADD_GROUND, EditorActionId.DIAGRAM_ADD_DIODE,
                                        EditorActionId.DIAGRAM_ADD_LED, EditorActionId.DIAGRAM_ADD_SWITCH_SPST,
                                        EditorActionId.DIAGRAM_ADD_JUNCTION),
                                palette(diagram, "Connections", "Wires", ScholarIcons.ELECTRICAL,
                                        EditorActionId.DIAGRAM_START_CONNECTION, EditorActionId.DIAGRAM_FINISH_CONNECTION,
                                        EditorActionId.DIAGRAM_CANCEL_CONNECTION, EditorActionId.DIAGRAM_DELETE_CONNECTION),
                                palette(diagram, "Arrange", "Arrange", ScholarIcons.ELECTRICAL,
                                        EditorActionId.DIAGRAM_SCALE_SYMBOLS_DOWN, EditorActionId.DIAGRAM_SCALE_SYMBOLS_UP,
                                        EditorActionId.DIAGRAM_ROTATE_CLOCKWISE, EditorActionId.DIAGRAM_ROTATE_COUNTERCLOCKWISE),
                                palette(diagram, "Remove", "Remove", ScholarIcons.ELECTRICAL,
                                        EditorActionId.DIAGRAM_DELETE_ELECTRICAL_COMPONENT,
                                        EditorActionId.DIAGRAM_DELETE_JUNCTION, EditorActionId.DIAGRAM_DELETE_NODE))),
                        group("Mechanical", List.of(
                                palette(diagram, "Geometry", "Geometry", ScholarIcons.MECHANICAL,
                                        EditorActionId.DIAGRAM_ADD_MECHANICAL_LINE,
                                        EditorActionId.DIAGRAM_ADD_MECHANICAL_CENTERLINE,
                                        EditorActionId.DIAGRAM_ADD_MECHANICAL_RECTANGLE,
                                        EditorActionId.DIAGRAM_ADD_MECHANICAL_CIRCLE,
                                        EditorActionId.DIAGRAM_ADD_MECHANICAL_ARC,
                                        EditorActionId.DIAGRAM_ADD_MECHANICAL_ARROW,
                                        EditorActionId.DIAGRAM_ADD_MECHANICAL_REFERENCE_POINT),
                                palette(diagram, "Documentation", "Notes", ScholarIcons.MECHANICAL,
                                        EditorActionId.DIAGRAM_ADD_MECHANICAL_PART_REFERENCE,
                                        EditorActionId.DIAGRAM_GENERATE_MECHANICAL_BOM,
                                        EditorActionId.DIAGRAM_ADD_MECHANICAL_ANNOTATION_PART_LABEL,
                                        EditorActionId.DIAGRAM_ADD_MECHANICAL_ANNOTATION_NOTE,
                                        EditorActionId.DIAGRAM_ADD_MECHANICAL_ANNOTATION_LEADER),
                                palette(diagram, "Symbols", "Symbols", ScholarIcons.MECHANICAL,
                                        EditorActionId.DIAGRAM_ADD_MECHANICAL_SYMBOL_SHAFT,
                                        EditorActionId.DIAGRAM_ADD_MECHANICAL_SYMBOL_GEAR,
                                        EditorActionId.DIAGRAM_ADD_MECHANICAL_SYMBOL_BEARING,
                                        EditorActionId.DIAGRAM_ADD_MECHANICAL_SYMBOL_SPRING,
                                        EditorActionId.DIAGRAM_ADD_MECHANICAL_SYMBOL_PISTON,
                                        EditorActionId.DIAGRAM_ADD_MECHANICAL_SYMBOL_BOLT),
                                palette(diagram, "Dimensions", "Measure", ScholarIcons.MECHANICAL,
                                        EditorActionId.DIAGRAM_ADD_MECHANICAL_DIMENSION_HORIZONTAL,
                                        EditorActionId.DIAGRAM_ADD_MECHANICAL_DIMENSION_VERTICAL,
                                        EditorActionId.DIAGRAM_ADD_MECHANICAL_DIMENSION_ALIGNED,
                                        EditorActionId.DIAGRAM_ADD_MECHANICAL_DIMENSION_RADIUS,
                                        EditorActionId.DIAGRAM_ADD_MECHANICAL_DIMENSION_DIAMETER,
                                        EditorActionId.DIAGRAM_ADD_MECHANICAL_DIMENSION_ANGLE),
                                palette(diagram, "Constraints", "Constrain", ScholarIcons.MECHANICAL,
                                        EditorActionId.DIAGRAM_ADD_MECHANICAL_CONSTRAINT_HORIZONTAL,
                                        EditorActionId.DIAGRAM_ADD_MECHANICAL_CONSTRAINT_VERTICAL,
                                        EditorActionId.DIAGRAM_START_MECHANICAL_CONSTRAINT_COINCIDENT,
                                        EditorActionId.DIAGRAM_START_MECHANICAL_CONSTRAINT_PARALLEL,
                                        EditorActionId.DIAGRAM_START_MECHANICAL_CONSTRAINT_PERPENDICULAR,
                                        EditorActionId.DIAGRAM_START_MECHANICAL_CONSTRAINT_CONCENTRIC,
                                        EditorActionId.DIAGRAM_FINISH_MECHANICAL_CONSTRAINT,
                                        EditorActionId.DIAGRAM_CANCEL_MECHANICAL_CONSTRAINT),
                                palette(diagram, "Remove", "Remove", ScholarIcons.MECHANICAL,
                                        EditorActionId.DIAGRAM_DELETE_MECHANICAL_CONSTRAINT,
                                        EditorActionId.DIAGRAM_DELETE_MECHANICAL_DIMENSION,
                                        EditorActionId.DIAGRAM_DELETE_MECHANICAL_PART_REFERENCE,
                                        EditorActionId.DIAGRAM_DELETE_MECHANICAL_ANNOTATION,
                                        EditorActionId.DIAGRAM_DELETE_MECHANICAL_SYMBOL,
                                        EditorActionId.DIAGRAM_DELETE_MECHANICAL_PRIMITIVE))),
                        group("Workspace", List.of(palette(diagram, "Canvas Height", "Canvas", ScholarIcons.DIAGRAM,
                                EditorActionId.DIAGRAM_WORKSPACE_SHORTER, EditorActionId.DIAGRAM_WORKSPACE_TALLER,
                                EditorActionId.DIAGRAM_WORKSPACE_RESET_HEIGHT)))),
                tab("Layout",
                        group("Page Setup", List.of(
                                RibbonCommandPresentation.dropdown(find(all, EditorActionId.LAYOUT_MARGIN_NORMAL),
                                        select(layout, EditorActionId.LAYOUT_MARGIN_NORMAL, EditorActionId.LAYOUT_MARGIN_NARROW)),
                                RibbonCommandPresentation.dropdown(find(all, EditorActionId.LAYOUT_PORTRAIT),
                                        select(layout, EditorActionId.LAYOUT_PORTRAIT, EditorActionId.LAYOUT_LANDSCAPE)),
                                RibbonCommandPresentation.dropdown(find(all, EditorActionId.LAYOUT_SIZE_LETTER),
                                        select(layout, EditorActionId.LAYOUT_SIZE_LETTER, EditorActionId.LAYOUT_SIZE_A4, EditorActionId.LAYOUT_SIZE_LEGAL)))),
                        group("Columns", medium(all, EditorActionId.LAYOUT_ONE_COLUMN, EditorActionId.LAYOUT_TWO_COLUMNS)),
                        group("Breaks", large(all, EditorActionId.INSERT_PAGE_BREAK)))));
        if (!view.isEmpty()) tabs.add(tab("View", group("Navigation", large(view))));
        return List.copyOf(tabs);
    }

    private static List<EditorAction> styleChoices(List<EditorAction> blockStyles, List<EditorAction> format) {
        var result = new ArrayList<EditorAction>(blockStyles);
        format.stream().filter(action -> action.id().name().startsWith("STYLE_")).forEach(result::add);
        return List.copyOf(result);
    }

    private static List<EditorAction> select(List<EditorAction> actions, EditorActionId... ids) {
        return java.util.Arrays.stream(ids).map(id -> find(actions, id)).toList();
    }

    private static RibbonTabDefinition tab(String label, RibbonGroupDefinition... groups) {
        return new RibbonTabDefinition(label, List.of(groups));
    }

    @SafeVarargs
    private static RibbonGroupDefinition group(String label, List<RibbonCommandPresentation>... commandGroups) {
        var commands = new ArrayList<RibbonCommandPresentation>();
        for (var group : commandGroups) commands.addAll(group);
        return new RibbonGroupDefinition(label, commands);
    }

    private static List<RibbonCommandPresentation> large(List<EditorAction> all, EditorActionId... ids) {
        return presentations(all, RibbonCommandSize.LARGE, ids);
    }

    private static List<RibbonCommandPresentation> large(List<EditorAction> actions) {
        return actions.stream().map(action -> RibbonCommandPresentation.action(action, RibbonCommandSize.LARGE)).toList();
    }

    private static List<RibbonCommandPresentation> medium(List<EditorAction> all, EditorActionId... ids) {
        return presentations(all, RibbonCommandSize.MEDIUM, ids);
    }

    private static List<RibbonCommandPresentation> medium(List<EditorAction> actions) {
        return actions.stream().map(action -> RibbonCommandPresentation.action(action, RibbonCommandSize.MEDIUM)).toList();
    }

    private static List<RibbonCommandPresentation> small(List<EditorAction> all, EditorActionId... ids) {
        return presentations(all, RibbonCommandSize.SMALL, ids);
    }

    private static List<RibbonCommandPresentation> compact(List<EditorAction> actions) {
        return actions.stream().map(action -> RibbonCommandPresentation.action(action, RibbonCommandSize.SMALL)).toList();
    }

    private static List<RibbonCommandPresentation> presentations(
            List<EditorAction> all, RibbonCommandSize size, EditorActionId... ids) {
        return java.util.Arrays.stream(ids).map(id -> RibbonCommandPresentation.action(find(all, id), size)).toList();
    }

    private static RibbonCommandPresentation command(List<EditorAction> all, EditorActionId id,
                                                     RibbonCommandSize size, String shortLabel) {
        return RibbonCommandPresentation.action(find(all, id), size, shortLabel);
    }

    private static RibbonCommandPresentation palette(List<EditorAction> all, String label, String shortLabel,
                                                     ScholarIcon icon, EditorActionId... ids) {
        return RibbonCommandPresentation.palette(label, shortLabel, icon,
                java.util.Arrays.stream(ids).map(id -> find(all, id)).toList());
    }

    private static EditorAction find(List<EditorAction> actions, EditorActionId id) {
        return actions.stream().filter(action -> action.id() == id).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Missing ribbon action " + id));
    }
}
