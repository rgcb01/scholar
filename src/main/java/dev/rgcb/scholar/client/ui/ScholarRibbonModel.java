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
                tab("scholar.menu.file", group("scholar.ribbon.group.document", large(all, EditorActionId.FILE_NEW), List.of(
                        command(all, EditorActionId.FILE_OPEN, RibbonCommandSize.MEDIUM, "scholar.ribbon.short.open"),
                        command(all, EditorActionId.FILE_SAVE, RibbonCommandSize.MEDIUM, "scholar.ribbon.short.save"),
                        command(all, EditorActionId.FILE_SAVE_AS, RibbonCommandSize.MEDIUM, "scholar.ribbon.short.save_as"))),
                        group("scholar.ribbon.group.manage", medium(all, EditorActionId.FILE_RENAME, EditorActionId.FILE_CLOSE)),
                        group("scholar.ribbon.group.interchange", medium(all, EditorActionId.FILE_IMPORT_CSV,
                                EditorActionId.FILE_EXPORT_MARKDOWN, EditorActionId.FILE_EXPORT_PDF))),
                tab("scholar.menu.home", group("scholar.ribbon.group.clipboard", large(all, EditorActionId.PASTE), small(all,
                        EditorActionId.CUT, EditorActionId.COPY)),
                        group("scholar.ribbon.group.history", medium(all, EditorActionId.UNDO, EditorActionId.REDO)),
                        new RibbonGroupDefinition("scholar.ribbon.group.style", List.of(RibbonCommandPresentation.valueDropdown(
                                find(all, EditorActionId.STYLE_BODY), styleChoices(styles, format), "scholar.ribbon.group.style", "scholar.ribbon.group.style"))),
                        group("scholar.ribbon.group.font", medium(all, EditorActionId.BOLD, EditorActionId.ITALIC, EditorActionId.UNDERLINE,
                                EditorActionId.TEXT_SUBSCRIPT, EditorActionId.TEXT_SUPERSCRIPT),
                                List.of(RibbonCommandPresentation.valueDropdown(find(all, EditorActionId.FONT_SOURCE_SANS),
                                        select(format, EditorActionId.FONT_SOURCE_SANS, EditorActionId.FONT_SCIENTIFIC_MATH), "scholar.ribbon.group.font", "scholar.ribbon.group.font")),
                                List.of(RibbonCommandPresentation.valueDropdown(find(all, EditorActionId.FONT_SIZE_10),
                                        select(format, EditorActionId.FONT_SIZE_10, EditorActionId.FONT_SIZE_12, EditorActionId.FONT_SIZE_14), "scholar.ribbon.short.size", "scholar.ribbon.short.size"))),
                        group("scholar.ribbon.group.paragraph", medium(all, EditorActionId.ALIGN_LEFT, EditorActionId.ALIGN_CENTER,
                                EditorActionId.ALIGN_RIGHT, EditorActionId.ALIGN_JUSTIFIED),
                                medium(all, EditorActionId.LINE_SPACING_SINGLE, EditorActionId.LINE_SPACING_ONE_HALF,
                                        EditorActionId.INDENT_DECREASE, EditorActionId.INDENT_INCREASE))),
                tab("scholar.menu.insert", group("scholar.ribbon.group.structure", large(all, EditorActionId.INSERT_TABLE, EditorActionId.INSERT_EQUATION)),
                        group("scholar.ribbon.group.computation", medium(all, EditorActionId.INSERT_VARIABLE, EditorActionId.INSERT_COMPUTED_RESULT,
                                EditorActionId.EDIT_VARIABLE, EditorActionId.EDIT_COMPUTED_RESULT)),
                        group("scholar.ribbon.group.scientific", medium(all, EditorActionId.INSERT_PLOT, EditorActionId.INSERT_DIAGRAM),
                                List.of(command(all, EditorActionId.DATA_NEW_DATASET, RibbonCommandSize.MEDIUM, "scholar.ribbon.group.dataset"),
                                        palette(insert, "scholar.ribbon.palette.quantity", "scholar.ribbon.palette.quantity", ScholarIcons.UNIT,
                                                EditorActionId.INSERT_QUANTITY_METRE,
                                                EditorActionId.INSERT_QUANTITY_CELSIUS,
                                                EditorActionId.INSERT_QUANTITY_CELSIUS_DIFFERENCE,
                                                EditorActionId.INSERT_QUANTITY_VOLT,
                                                EditorActionId.INSERT_QUANTITY_MILLIAMPERE,
                                                EditorActionId.INSERT_QUANTITY_KILOOHM,
                                                EditorActionId.INSERT_QUANTITY_ACCELERATION),
                                        palette(insert, "scholar.ribbon.palette.convert_to", "scholar.ribbon.short.convert", ScholarIcons.UNIT,
                                                EditorActionId.QUANTITY_CONVERT_METRE,
                                                EditorActionId.QUANTITY_CONVERT_MILLIMETRE,
                                                EditorActionId.QUANTITY_CONVERT_CELSIUS,
                                                EditorActionId.QUANTITY_CONVERT_KELVIN),
                                        palette(insert, "scholar.ribbon.palette.number_format", "scholar.ribbon.short.format", ScholarIcons.UNIT,
                                                EditorActionId.QUANTITY_FORMAT_DECIMAL,
                                                EditorActionId.QUANTITY_FORMAT_SCIENTIFIC,
                                                EditorActionId.QUANTITY_FORMAT_ENGINEERING))),
                        group("scholar.ribbon.group.references", List.of(
                                command(all, EditorActionId.INSERT_CROSS_REFERENCE, RibbonCommandSize.MEDIUM, "scholar.ribbon.short.reference"),
                                command(all, EditorActionId.INSERT_TABLE_OF_CONTENTS, RibbonCommandSize.MEDIUM, "scholar.ribbon.short.contents"),
                                command(all, EditorActionId.INSERT_PAGE_BREAK, RibbonCommandSize.MEDIUM, "scholar.ribbon.short.page_break")))),
                tab("scholar.menu.data", group("scholar.ribbon.group.dataset",
                        List.of(command(all, EditorActionId.DATA_NEW_DATASET, RibbonCommandSize.LARGE, "scholar.ribbon.group.dataset"),
                                command(all, EditorActionId.DATA_INSERT_DATASET_TABLE, RibbonCommandSize.MEDIUM, "scholar.ribbon.group.table"),
                                command(all, EditorActionId.DATA_BIND_PLOT_TO_DATASET, RibbonCommandSize.MEDIUM, "scholar.ribbon.short.bind_plot"),
                                palette(data, "scholar.ribbon.palette.column_unit", "scholar.ribbon.short.unit", ScholarIcons.UNIT,
                                        EditorActionId.DATA_COLUMN_UNIT_NONE, EditorActionId.DATA_COLUMN_UNIT_METRE,
                                        EditorActionId.DATA_COLUMN_UNIT_SECOND, EditorActionId.DATA_COLUMN_UNIT_CELSIUS,
                                        EditorActionId.DATA_COLUMN_UNIT_KELVIN, EditorActionId.DATA_COLUMN_UNIT_CELSIUS_DIFFERENCE,
                                        EditorActionId.DATA_COLUMN_UNIT_KELVIN_DIFFERENCE, EditorActionId.DATA_COLUMN_UNIT_VOLT,
                                        EditorActionId.DATA_COLUMN_UNIT_AMPERE, EditorActionId.DATA_COLUMN_UNIT_OHM))),
                        group("scholar.ribbon.group.interchange", medium(all, EditorActionId.DATA_EXPORT_CSV)),
                        group("scholar.ribbon.group.analysis", medium(all, EditorActionId.DATA_INSERT_ANALYSIS,
                                EditorActionId.DATA_EDIT_ANALYSIS, EditorActionId.DATA_ADD_FIT_OVERLAY)),
                        group("scholar.ribbon.group.table", compact(table)), group("scholar.ribbon.group.plot", compact(plot), List.of(
                                palette(plot, "scholar.ribbon.palette.x_axis_unit", "scholar.ribbon.short.x_unit", ScholarIcons.UNIT,
                                        EditorActionId.PLOT_X_UNIT_AUTO, EditorActionId.PLOT_X_UNIT_SECOND,
                                        EditorActionId.PLOT_X_UNIT_METRE),
                                palette(plot, "scholar.ribbon.palette.y_axis_unit", "scholar.ribbon.short.y_unit", ScholarIcons.UNIT,
                                        EditorActionId.PLOT_Y_UNIT_AUTO, EditorActionId.PLOT_Y_UNIT_CELSIUS,
                                        EditorActionId.PLOT_Y_UNIT_KELVIN, EditorActionId.PLOT_Y_UNIT_CELSIUS_DIFFERENCE,
                                        EditorActionId.PLOT_Y_UNIT_KELVIN_DIFFERENCE, EditorActionId.PLOT_Y_UNIT_VOLT)))),
                tab("scholar.menu.figure", group("scholar.ribbon.group.compose", medium(figure)), group("scholar.menu.insert", large(all,
                        EditorActionId.INSERT_PLOT, EditorActionId.INSERT_DIAGRAM))),
                tab("scholar.menu.diagram", group("scholar.ribbon.group.create", large(all, EditorActionId.INSERT_DIAGRAM)),
                        group("scholar.ribbon.group.electrical", List.of(
                                palette(diagram, "scholar.ribbon.palette.components", "scholar.ribbon.short.parts", ScholarIcons.ELECTRICAL,
                                        EditorActionId.DIAGRAM_ADD_NODE, EditorActionId.DIAGRAM_ADD_RESISTOR,
                                        EditorActionId.DIAGRAM_ADD_CAPACITOR, EditorActionId.DIAGRAM_ADD_DC_VOLTAGE_SOURCE,
                                        EditorActionId.DIAGRAM_ADD_GROUND, EditorActionId.DIAGRAM_ADD_DIODE,
                                        EditorActionId.DIAGRAM_ADD_LED, EditorActionId.DIAGRAM_ADD_SWITCH_SPST,
                                        EditorActionId.DIAGRAM_ADD_JUNCTION),
                                palette(diagram, "scholar.ribbon.palette.connections", "scholar.ribbon.short.wires", ScholarIcons.ELECTRICAL,
                                        EditorActionId.DIAGRAM_START_CONNECTION, EditorActionId.DIAGRAM_FINISH_CONNECTION,
                                        EditorActionId.DIAGRAM_CANCEL_CONNECTION, EditorActionId.DIAGRAM_DELETE_CONNECTION),
                                palette(diagram, "scholar.ribbon.palette.arrange", "scholar.ribbon.palette.arrange", ScholarIcons.ELECTRICAL,
                                        EditorActionId.DIAGRAM_SCALE_SYMBOLS_DOWN, EditorActionId.DIAGRAM_SCALE_SYMBOLS_UP,
                                        EditorActionId.DIAGRAM_ROTATE_CLOCKWISE, EditorActionId.DIAGRAM_ROTATE_COUNTERCLOCKWISE),
                                palette(diagram, "scholar.ribbon.palette.remove", "scholar.ribbon.palette.remove", ScholarIcons.ELECTRICAL,
                                        EditorActionId.DIAGRAM_DELETE_ELECTRICAL_COMPONENT,
                                        EditorActionId.DIAGRAM_DELETE_JUNCTION, EditorActionId.DIAGRAM_DELETE_NODE))),
                        group("scholar.ribbon.group.mechanical", List.of(
                                palette(diagram, "scholar.ribbon.palette.geometry", "scholar.ribbon.palette.geometry", ScholarIcons.MECHANICAL,
                                        EditorActionId.DIAGRAM_ADD_MECHANICAL_LINE,
                                        EditorActionId.DIAGRAM_ADD_MECHANICAL_CENTERLINE,
                                        EditorActionId.DIAGRAM_ADD_MECHANICAL_RECTANGLE,
                                        EditorActionId.DIAGRAM_ADD_MECHANICAL_CIRCLE,
                                        EditorActionId.DIAGRAM_ADD_MECHANICAL_ARC,
                                        EditorActionId.DIAGRAM_ADD_MECHANICAL_ARROW,
                                        EditorActionId.DIAGRAM_ADD_MECHANICAL_REFERENCE_POINT),
                                palette(diagram, "scholar.ribbon.palette.documentation", "scholar.ribbon.short.notes", ScholarIcons.MECHANICAL,
                                        EditorActionId.DIAGRAM_ADD_MECHANICAL_PART_REFERENCE,
                                        EditorActionId.DIAGRAM_GENERATE_MECHANICAL_BOM,
                                        EditorActionId.DIAGRAM_ADD_MECHANICAL_ANNOTATION_PART_LABEL,
                                        EditorActionId.DIAGRAM_ADD_MECHANICAL_ANNOTATION_NOTE,
                                        EditorActionId.DIAGRAM_ADD_MECHANICAL_ANNOTATION_LEADER),
                                palette(diagram, "scholar.ribbon.palette.symbols", "scholar.ribbon.palette.symbols", ScholarIcons.MECHANICAL,
                                        EditorActionId.DIAGRAM_ADD_MECHANICAL_SYMBOL_SHAFT,
                                        EditorActionId.DIAGRAM_ADD_MECHANICAL_SYMBOL_GEAR,
                                        EditorActionId.DIAGRAM_ADD_MECHANICAL_SYMBOL_BEARING,
                                        EditorActionId.DIAGRAM_ADD_MECHANICAL_SYMBOL_SPRING,
                                        EditorActionId.DIAGRAM_ADD_MECHANICAL_SYMBOL_PISTON,
                                        EditorActionId.DIAGRAM_ADD_MECHANICAL_SYMBOL_BOLT),
                                palette(diagram, "scholar.ribbon.palette.dimensions", "scholar.ribbon.short.measure", ScholarIcons.MECHANICAL,
                                        EditorActionId.DIAGRAM_ADD_MECHANICAL_DIMENSION_HORIZONTAL,
                                        EditorActionId.DIAGRAM_ADD_MECHANICAL_DIMENSION_VERTICAL,
                                        EditorActionId.DIAGRAM_ADD_MECHANICAL_DIMENSION_ALIGNED,
                                        EditorActionId.DIAGRAM_ADD_MECHANICAL_DIMENSION_RADIUS,
                                        EditorActionId.DIAGRAM_ADD_MECHANICAL_DIMENSION_DIAMETER,
                                        EditorActionId.DIAGRAM_ADD_MECHANICAL_DIMENSION_ANGLE),
                                palette(diagram, "scholar.ribbon.palette.constraints", "scholar.ribbon.short.constrain", ScholarIcons.MECHANICAL,
                                        EditorActionId.DIAGRAM_ADD_MECHANICAL_CONSTRAINT_HORIZONTAL,
                                        EditorActionId.DIAGRAM_ADD_MECHANICAL_CONSTRAINT_VERTICAL,
                                        EditorActionId.DIAGRAM_START_MECHANICAL_CONSTRAINT_COINCIDENT,
                                        EditorActionId.DIAGRAM_START_MECHANICAL_CONSTRAINT_PARALLEL,
                                        EditorActionId.DIAGRAM_START_MECHANICAL_CONSTRAINT_PERPENDICULAR,
                                        EditorActionId.DIAGRAM_START_MECHANICAL_CONSTRAINT_CONCENTRIC,
                                        EditorActionId.DIAGRAM_FINISH_MECHANICAL_CONSTRAINT,
                                        EditorActionId.DIAGRAM_CANCEL_MECHANICAL_CONSTRAINT),
                                palette(diagram, "scholar.ribbon.palette.remove", "scholar.ribbon.palette.remove", ScholarIcons.MECHANICAL,
                                        EditorActionId.DIAGRAM_DELETE_MECHANICAL_CONSTRAINT,
                                        EditorActionId.DIAGRAM_DELETE_MECHANICAL_DIMENSION,
                                        EditorActionId.DIAGRAM_DELETE_MECHANICAL_PART_REFERENCE,
                                        EditorActionId.DIAGRAM_DELETE_MECHANICAL_ANNOTATION,
                                        EditorActionId.DIAGRAM_DELETE_MECHANICAL_SYMBOL,
                                        EditorActionId.DIAGRAM_DELETE_MECHANICAL_PRIMITIVE))),
                        group("scholar.ribbon.group.workspace", List.of(palette(diagram, "scholar.ribbon.palette.canvas_height", "scholar.ribbon.short.canvas", ScholarIcons.DIAGRAM,
                                EditorActionId.DIAGRAM_WORKSPACE_SHORTER, EditorActionId.DIAGRAM_WORKSPACE_TALLER,
                                EditorActionId.DIAGRAM_WORKSPACE_RESET_HEIGHT)))),
                tab("scholar.menu.layout",
                        group("scholar.ribbon.group.page_setup", List.of(
                                RibbonCommandPresentation.dropdown(find(all, EditorActionId.LAYOUT_MARGIN_NORMAL),
                                        select(layout, EditorActionId.LAYOUT_MARGIN_NORMAL, EditorActionId.LAYOUT_MARGIN_NARROW)),
                                RibbonCommandPresentation.dropdown(find(all, EditorActionId.LAYOUT_PORTRAIT),
                                        select(layout, EditorActionId.LAYOUT_PORTRAIT, EditorActionId.LAYOUT_LANDSCAPE)),
                                RibbonCommandPresentation.dropdown(find(all, EditorActionId.LAYOUT_SIZE_LETTER),
                                        select(layout, EditorActionId.LAYOUT_SIZE_LETTER, EditorActionId.LAYOUT_SIZE_A4, EditorActionId.LAYOUT_SIZE_LEGAL)))),
                        group("scholar.ribbon.group.columns", medium(all, EditorActionId.LAYOUT_ONE_COLUMN, EditorActionId.LAYOUT_TWO_COLUMNS)),
                        group("scholar.ribbon.group.breaks", large(all, EditorActionId.INSERT_PAGE_BREAK)))));
        if (!view.isEmpty()) tabs.add(tab("scholar.menu.view", group("scholar.ribbon.group.navigation", large(view))));
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
