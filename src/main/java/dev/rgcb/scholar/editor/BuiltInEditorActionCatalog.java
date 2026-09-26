package dev.rgcb.scholar.editor;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Single authoritative presentation catalog for Scholar's built-in actions. */
public final class BuiltInEditorActionCatalog {
    private static final Map<EditorActionId, EditorActionDescriptor> DESCRIPTORS = createDescriptors();

    private BuiltInEditorActionCatalog() { }

    public static EditorActionDescriptor require(EditorActionId id) {
        var descriptor = DESCRIPTORS.get(Objects.requireNonNull(id, "id"));
        if (descriptor == null) throw new IllegalArgumentException("Missing built-in action descriptor: " + id);
        return descriptor;
    }

    public static Optional<EditorActionDescriptor> find(EditorActionId id) {
        return Optional.ofNullable(DESCRIPTORS.get(Objects.requireNonNull(id, "id")));
    }

    public static List<EditorActionDescriptor> descriptors() {
        return Arrays.stream(EditorActionId.values()).map(BuiltInEditorActionCatalog::require).toList();
    }

    private static Map<EditorActionId, EditorActionDescriptor> createDescriptors() {
        var result = new EnumMap<EditorActionId, EditorActionDescriptor>(EditorActionId.class);
        for (var id : EditorActionId.values()) {
            var label = englishLabel(id);
            var tooltip = englishTooltip(id, label);
            var tooltipKey = tooltip.equals(label)
                    ? Optional.<String>empty()
                    : Optional.of(key(id) + ".tooltip");
            var previous = result.put(id, new EditorActionDescriptor(id, key(id), tooltipKey, label, tooltip,
                    icon(id), category(id), shortcut(id)));
            if (previous != null) throw new IllegalStateException("Duplicate action descriptor: " + id);
        }
        return Map.copyOf(result);
    }

    private static String key(EditorActionId id) {
        return "scholar.action." + id.name().toLowerCase(java.util.Locale.ROOT);
    }

    private static String englishLabel(EditorActionId id) {
        return switch (id) {
            case FILE_NEW -> "New";
            case FILE_OPEN -> "Open";
            case FILE_SAVE -> "Save";
            case FILE_SAVE_AS -> "Save As";
            case FILE_RENAME -> "Rename";
            case FILE_CLOSE -> "Close";
            case FILE_IMPORT_CSV -> "Import CSV";
            case FILE_EXPORT_MARKDOWN -> "Export Markdown";
            case FILE_EXPORT_PDF -> "Export PDF";
            case DATA_EXPORT_CSV -> "Export CSV";
            case UNDO -> "Undo";
            case REDO -> "Redo";
            case CUT -> "Cut";
            case COPY -> "Copy";
            case PASTE -> "Paste";
            case DELETE -> "Delete";
            case INSERT_EQUATION -> "Equation";
            case INSERT_VARIABLE -> "Variable";
            case INSERT_COMPUTED_RESULT -> "Computed Result";
            case EDIT_VARIABLE -> "Edit Variable";
            case EDIT_COMPUTED_RESULT -> "Edit Computed Result";
            case INSERT_TABLE -> "Table";
            case INSERT_PLOT -> "Plot";
            case INSERT_DIAGRAM -> "Diagram";
            case INSERT_CROSS_REFERENCE -> "Cross Reference";
            case INSERT_TABLE_OF_CONTENTS -> "Table of Contents";
            case INSERT_PAGE_BREAK -> "Page Break";
            case INSERT_QUANTITY_METRE -> "Length: 1 m";
            case INSERT_QUANTITY_CELSIUS -> "Temperature: 25 °C";
            case INSERT_QUANTITY_CELSIUS_DIFFERENCE -> "Temperature Difference: Δ5 °C";
            case INSERT_QUANTITY_VOLT -> "Voltage: 5 V";
            case INSERT_QUANTITY_MILLIAMPERE -> "Current: 1 mA";
            case INSERT_QUANTITY_KILOOHM -> "Resistance: 1 kΩ";
            case INSERT_QUANTITY_ACCELERATION -> "Acceleration: 9.81 m/s²";
            case QUANTITY_CONVERT_METRE -> "Convert to m";
            case QUANTITY_CONVERT_MILLIMETRE -> "Convert to mm";
            case QUANTITY_CONVERT_CELSIUS -> "Convert to °C";
            case QUANTITY_CONVERT_KELVIN -> "Convert to K";
            case QUANTITY_FORMAT_DECIMAL -> "Decimal";
            case QUANTITY_FORMAT_SCIENTIFIC -> "Scientific";
            case QUANTITY_FORMAT_ENGINEERING -> "Engineering";
            case TOGGLE_OUTLINE -> "Outline";
            case VIEW_ZOOM_OUT -> "Zoom Out";
            case VIEW_ZOOM_IN -> "Zoom In";
            case VIEW_FIT_PAGE -> "Fit Page";
            case VIEW_FIT_WIDTH -> "Fit Width";
            case DATA_NEW_DATASET -> "New Dataset";
            case DATA_INSERT_ANALYSIS -> "Analysis";
            case DATA_EDIT_ANALYSIS -> "Edit Analysis";
            case DATA_ADD_FIT_OVERLAY -> "Add Fit";
            case DATA_INSERT_DATASET_TABLE -> "Dataset Table";
            case DATA_BIND_PLOT_TO_DATASET -> "Bind Plot";
            case DATA_COLUMN_UNIT_NONE -> "Unitless";
            case DATA_COLUMN_UNIT_METRE -> "Metre (m)";
            case DATA_COLUMN_UNIT_SECOND -> "Second (s)";
            case DATA_COLUMN_UNIT_CELSIUS -> "Celsius (°C)";
            case DATA_COLUMN_UNIT_KELVIN -> "Kelvin (K)";
            case DATA_COLUMN_UNIT_CELSIUS_DIFFERENCE -> "Temperature Difference (Δ°C)";
            case DATA_COLUMN_UNIT_KELVIN_DIFFERENCE -> "Temperature Difference (ΔK)";
            case DATA_COLUMN_UNIT_VOLT -> "Volt (V)";
            case DATA_COLUMN_UNIT_AMPERE -> "Ampere (A)";
            case DATA_COLUMN_UNIT_OHM -> "Ohm (Ω)";
            case FIGURE_WRAP_PLOT -> "Wrap Plot";
            case FIGURE_WRAP_DIAGRAM -> "Wrap Diagram";
            case FIGURE_EDIT_CAPTION -> "Edit Caption";
            case FIGURE_UNWRAP -> "Unwrap Figure";
            case MATH_INSERT_FRACTION -> "Fraction";
            case MATH_INSERT_ROOT -> "Root";
            case MATH_INSERT_PARENTHESES_GROUP -> "Parentheses";
            case MATH_INSERT_BRACKETS_GROUP -> "Brackets";
            case MATH_INSERT_BRACES_GROUP -> "Braces";
            case MATH_INSERT_SUPERSCRIPT -> "Superscript";
            case MATH_INSERT_SUBSCRIPT -> "Subscript";
            case MATH_CONVERT_NAMED_OPERATOR -> "Named Operator";
            case MATH_CONVERT_TEXT -> "Math Text";
            case PARAGRAPH -> "Paragraph";
            case HEADING_1 -> "Heading 1";
            case HEADING_2 -> "Heading 2";
            case HEADING_3 -> "Heading 3";
            case HEADING_4 -> "Heading 4";
            case HEADING_5 -> "Heading 5";
            case HEADING_6 -> "Heading 6";
            case BOLD -> "Bold";
            case ITALIC -> "Italic";
            case UNDERLINE -> "Underline";
            case TEXT_SUPERSCRIPT -> "Superscript";
            case TEXT_SUBSCRIPT -> "Subscript";
            case STYLE_BODY -> "Body Text";
            case STYLE_TITLE -> "Title";
            case STYLE_SUBTITLE -> "Subtitle";
            case STYLE_AUTHOR -> "Author";
            case STYLE_AFFILIATION -> "Affiliation";
            case STYLE_ABSTRACT -> "Abstract";
            case STYLE_KEYWORDS -> "Keywords";
            case STYLE_REFERENCE -> "Reference";
            case FONT_SIZE_10 -> "10 pt";
            case FONT_SIZE_12 -> "12 pt";
            case FONT_SIZE_14 -> "14 pt";
            case FONT_SOURCE_SANS -> "Source Sans 3";
            case FONT_SCIENTIFIC_MATH -> "Scientific Math";
            case ALIGN_LEFT -> "Align Left";
            case ALIGN_CENTER -> "Center";
            case ALIGN_RIGHT -> "Align Right";
            case ALIGN_JUSTIFIED -> "Justify";
            case LINE_SPACING_SINGLE -> "Single Spacing";
            case LINE_SPACING_ONE_HALF -> "1.5 Spacing";
            case INDENT_DECREASE -> "Decrease Indent";
            case INDENT_INCREASE -> "Increase Indent";
            case LAYOUT_MARGIN_NORMAL -> "Normal Margins";
            case LAYOUT_MARGIN_NARROW -> "Narrow Margins";
            case LAYOUT_PORTRAIT -> "Portrait";
            case LAYOUT_LANDSCAPE -> "Landscape";
            case LAYOUT_SIZE_LETTER -> "Letter";
            case LAYOUT_SIZE_A4 -> "A4";
            case LAYOUT_SIZE_LEGAL -> "Legal";
            case LAYOUT_ONE_COLUMN -> "One Column";
            case LAYOUT_TWO_COLUMNS -> "Two Columns";
            case TABLE_INSERT_ROW_ABOVE -> "Insert Row Above";
            case TABLE_INSERT_ROW_BELOW -> "Insert Row Below";
            case TABLE_DELETE_ROW -> "Delete Row";
            case TABLE_INSERT_COLUMN_LEFT -> "Insert Column Left";
            case TABLE_INSERT_COLUMN_RIGHT -> "Insert Column Right";
            case TABLE_DELETE_COLUMN -> "Delete Column";
            case PLOT_TOGGLE_GRID -> "Grid";
            case PLOT_TOGGLE_LEGEND -> "Legend";
            case PLOT_ADD_LINE_SERIES -> "Add Line Series";
            case PLOT_ADD_SCATTER_SERIES -> "Add Scatter Series";
            case PLOT_SET_SERIES_LINE -> "Line Series";
            case PLOT_SET_SERIES_SCATTER -> "Scatter Series";
            case PLOT_ADD_POINT -> "Add Point";
            case PLOT_DELETE_POINT -> "Delete Point";
            case PLOT_DELETE_SERIES -> "Delete Series";
            case PLOT_X_UNIT_AUTO -> "Automatic X Unit";
            case PLOT_X_UNIT_SECOND -> "X Axis: s";
            case PLOT_X_UNIT_METRE -> "X Axis: m";
            case PLOT_Y_UNIT_AUTO -> "Automatic Y Unit";
            case PLOT_Y_UNIT_CELSIUS -> "Y Axis: °C";
            case PLOT_Y_UNIT_KELVIN -> "Y Axis: K";
            case PLOT_Y_UNIT_CELSIUS_DIFFERENCE -> "Y Axis: Δ°C";
            case PLOT_Y_UNIT_KELVIN_DIFFERENCE -> "Y Axis: ΔK";
            case PLOT_Y_UNIT_VOLT -> "Y Axis: V";
            case DIAGRAM_ADD_NODE -> "Add Node";
            case DIAGRAM_ADD_RESISTOR -> "Add Resistor";
            case DIAGRAM_ADD_CAPACITOR -> "Add Capacitor";
            case DIAGRAM_ADD_DC_VOLTAGE_SOURCE -> "Add DC Voltage Source";
            case DIAGRAM_ADD_GROUND -> "Add Ground";
            case DIAGRAM_ADD_DIODE -> "Add Diode";
            case DIAGRAM_ADD_LED -> "Add LED";
            case DIAGRAM_ADD_SWITCH_SPST -> "Add SPST Switch";
            case DIAGRAM_ADD_JUNCTION -> "Add Junction";
            case DIAGRAM_ADD_MECHANICAL_LINE -> "Add Mechanical Line";
            case DIAGRAM_ADD_MECHANICAL_CENTERLINE -> "Add Centerline";
            case DIAGRAM_ADD_MECHANICAL_RECTANGLE -> "Add Mechanical Rectangle";
            case DIAGRAM_ADD_MECHANICAL_CIRCLE -> "Add Mechanical Circle";
            case DIAGRAM_ADD_MECHANICAL_ARC -> "Add Mechanical Arc";
            case DIAGRAM_ADD_MECHANICAL_ARROW -> "Add Mechanical Arrow";
            case DIAGRAM_ADD_MECHANICAL_REFERENCE_POINT -> "Add Reference Point";
            case DIAGRAM_ADD_MECHANICAL_PART_REFERENCE -> "Add Part Balloon";
            case DIAGRAM_GENERATE_MECHANICAL_BOM -> "Generate BOM";
            case DIAGRAM_ADD_MECHANICAL_ANNOTATION_PART_LABEL -> "Add Part Label";
            case DIAGRAM_ADD_MECHANICAL_ANNOTATION_NOTE -> "Add Mechanical Note";
            case DIAGRAM_ADD_MECHANICAL_ANNOTATION_LEADER -> "Add Leader Callout";
            case DIAGRAM_ADD_MECHANICAL_SYMBOL_SHAFT -> "Add Shaft Symbol";
            case DIAGRAM_ADD_MECHANICAL_SYMBOL_GEAR -> "Add Gear Symbol";
            case DIAGRAM_ADD_MECHANICAL_SYMBOL_BEARING -> "Add Bearing Symbol";
            case DIAGRAM_ADD_MECHANICAL_SYMBOL_SPRING -> "Add Spring Symbol";
            case DIAGRAM_ADD_MECHANICAL_SYMBOL_PISTON -> "Add Piston Symbol";
            case DIAGRAM_ADD_MECHANICAL_SYMBOL_BOLT -> "Add Bolt Symbol";
            case DIAGRAM_ADD_MECHANICAL_DIMENSION_HORIZONTAL -> "Add Horizontal Dimension";
            case DIAGRAM_ADD_MECHANICAL_DIMENSION_VERTICAL -> "Add Vertical Dimension";
            case DIAGRAM_ADD_MECHANICAL_DIMENSION_ALIGNED -> "Add Aligned Dimension";
            case DIAGRAM_ADD_MECHANICAL_DIMENSION_RADIUS -> "Add Radius Dimension";
            case DIAGRAM_ADD_MECHANICAL_DIMENSION_DIAMETER -> "Add Diameter Dimension";
            case DIAGRAM_ADD_MECHANICAL_DIMENSION_ANGLE -> "Add Angle Dimension";
            case DIAGRAM_ADD_MECHANICAL_CONSTRAINT_HORIZONTAL -> "Constrain Horizontal";
            case DIAGRAM_ADD_MECHANICAL_CONSTRAINT_VERTICAL -> "Constrain Vertical";
            case DIAGRAM_START_MECHANICAL_CONSTRAINT_COINCIDENT -> "Start Coincident";
            case DIAGRAM_START_MECHANICAL_CONSTRAINT_PARALLEL -> "Start Parallel";
            case DIAGRAM_START_MECHANICAL_CONSTRAINT_PERPENDICULAR -> "Start Perpendicular";
            case DIAGRAM_START_MECHANICAL_CONSTRAINT_CONCENTRIC -> "Start Concentric";
            case DIAGRAM_FINISH_MECHANICAL_CONSTRAINT -> "Finish Mechanical Constraint";
            case DIAGRAM_CANCEL_MECHANICAL_CONSTRAINT -> "Cancel Mechanical Constraint";
            case DIAGRAM_DELETE_MECHANICAL_CONSTRAINT -> "Delete Mechanical Constraint";
            case DIAGRAM_DELETE_MECHANICAL_DIMENSION -> "Delete Mechanical Dimension";
            case DIAGRAM_DELETE_MECHANICAL_PART_REFERENCE -> "Delete Part Balloon";
            case DIAGRAM_DELETE_MECHANICAL_ANNOTATION -> "Delete Mechanical Annotation";
            case DIAGRAM_DELETE_MECHANICAL_SYMBOL -> "Delete Mechanical Symbol";
            case DIAGRAM_DELETE_MECHANICAL_PRIMITIVE -> "Delete Mechanical Primitive";
            case DIAGRAM_SCALE_SYMBOLS_DOWN -> "Scale Symbols Down";
            case DIAGRAM_SCALE_SYMBOLS_UP -> "Scale Symbols Up";
            case DIAGRAM_WORKSPACE_SHORTER -> "Workspace Shorter";
            case DIAGRAM_WORKSPACE_TALLER -> "Workspace Taller";
            case DIAGRAM_WORKSPACE_RESET_HEIGHT -> "Reset Workspace Height";
            case DIAGRAM_ROTATE_CLOCKWISE -> "Rotate Clockwise";
            case DIAGRAM_ROTATE_COUNTERCLOCKWISE -> "Rotate Counterclockwise";
            case DIAGRAM_DELETE_ELECTRICAL_COMPONENT -> "Delete Component";
            case DIAGRAM_DELETE_JUNCTION -> "Delete Junction";
            case DIAGRAM_DELETE_NODE -> "Delete Node";
            case DIAGRAM_START_CONNECTION -> "Start Connection";
            case DIAGRAM_FINISH_CONNECTION -> "Finish Connection";
            case DIAGRAM_CANCEL_CONNECTION -> "Cancel Connection";
            case DIAGRAM_DELETE_CONNECTION -> "Delete Connection";
        };
    }

    private static String englishTooltip(EditorActionId id, String label) {
        return switch (id) {
            case FILE_NEW -> "Create a new Scholar document";
            case FILE_OPEN -> "Open a Scholar document";
            case FILE_SAVE -> "Save the current Scholar document";
            case FILE_SAVE_AS -> "Save a copy with a different name";
            case FILE_RENAME -> "Rename the current Scholar document";
            case FILE_CLOSE -> "Close the current document";
            case INSERT_CROSS_REFERENCE -> "Insert cross-reference";
            case INSERT_TABLE_OF_CONTENTS -> "Insert table of contents";
            case TOGGLE_OUTLINE -> "Show or hide document outline";
            case DATA_NEW_DATASET -> "Create an empty dataset";
            case DATA_INSERT_DATASET_TABLE -> "Insert dataset-backed table";
            case DATA_BIND_PLOT_TO_DATASET -> "Bind selected plot to dataset";
            case INSERT_QUANTITY_METRE, INSERT_QUANTITY_CELSIUS, INSERT_QUANTITY_CELSIUS_DIFFERENCE,
                    INSERT_QUANTITY_VOLT, INSERT_QUANTITY_MILLIAMPERE, INSERT_QUANTITY_KILOOHM,
                    INSERT_QUANTITY_ACCELERATION -> "Insert a semantic scientific quantity";
            case QUANTITY_CONVERT_METRE, QUANTITY_CONVERT_MILLIMETRE, QUANTITY_CONVERT_CELSIUS,
                    QUANTITY_CONVERT_KELVIN -> "Convert the semantic quantity at the caret";
            case QUANTITY_FORMAT_DECIMAL, QUANTITY_FORMAT_SCIENTIFIC, QUANTITY_FORMAT_ENGINEERING ->
                    "Set quantity number presentation";
            case DATA_COLUMN_UNIT_NONE, DATA_COLUMN_UNIT_METRE, DATA_COLUMN_UNIT_SECOND,
                    DATA_COLUMN_UNIT_CELSIUS, DATA_COLUMN_UNIT_KELVIN, DATA_COLUMN_UNIT_CELSIUS_DIFFERENCE,
                    DATA_COLUMN_UNIT_KELVIN_DIFFERENCE, DATA_COLUMN_UNIT_VOLT, DATA_COLUMN_UNIT_AMPERE,
                    DATA_COLUMN_UNIT_OHM -> "Set the selected numeric dataset column unit";
            case PLOT_X_UNIT_AUTO, PLOT_X_UNIT_SECOND, PLOT_X_UNIT_METRE ->
                    "Set the selected plot X-axis display unit";
            case PLOT_Y_UNIT_AUTO, PLOT_Y_UNIT_CELSIUS, PLOT_Y_UNIT_KELVIN,
                    PLOT_Y_UNIT_CELSIUS_DIFFERENCE, PLOT_Y_UNIT_KELVIN_DIFFERENCE, PLOT_Y_UNIT_VOLT ->
                    "Set the selected plot Y-axis display unit";
            case MATH_INSERT_FRACTION -> "Insert fraction";
            case MATH_INSERT_ROOT -> "Insert square root";
            case MATH_INSERT_PARENTHESES_GROUP -> "Insert parentheses group";
            case MATH_INSERT_BRACKETS_GROUP -> "Insert brackets group";
            case MATH_INSERT_BRACES_GROUP -> "Insert braces group";
            case MATH_INSERT_SUPERSCRIPT -> "Insert superscript";
            case MATH_INSERT_SUBSCRIPT -> "Insert subscript";
            case MATH_CONVERT_NAMED_OPERATOR -> "Convert to Named Operator";
            case MATH_CONVERT_TEXT -> "Convert to Math Text";
            default -> label;
        };
    }

    private static Optional<ActionShortcut> shortcut(EditorActionId id) {
        return switch (id) {
            case FILE_NEW -> Optional.of(ActionShortcut.ctrl(ActionShortcut.Key.N));
            case FILE_OPEN -> Optional.of(ActionShortcut.ctrl(ActionShortcut.Key.O));
            case FILE_SAVE -> Optional.of(ActionShortcut.ctrl(ActionShortcut.Key.S));
            case FILE_SAVE_AS -> Optional.of(ActionShortcut.of(new ActionShortcut.Stroke(ActionShortcut.Key.S, true, true)));
            case UNDO -> Optional.of(ActionShortcut.ctrl(ActionShortcut.Key.Z));
            case REDO -> Optional.of(ActionShortcut.of(new ActionShortcut.Stroke(ActionShortcut.Key.Y, true, false),
                    new ActionShortcut.Stroke(ActionShortcut.Key.Z, true, true)));
            case CUT -> Optional.of(ActionShortcut.ctrl(ActionShortcut.Key.X));
            case COPY -> Optional.of(ActionShortcut.ctrl(ActionShortcut.Key.C));
            case PASTE -> Optional.of(ActionShortcut.ctrl(ActionShortcut.Key.V));
            case DELETE -> Optional.of(ActionShortcut.plain(ActionShortcut.Key.DELETE));
            case BOLD -> Optional.of(ActionShortcut.ctrl(ActionShortcut.Key.B));
            case ITALIC -> Optional.of(ActionShortcut.ctrl(ActionShortcut.Key.I));
            default -> Optional.empty();
        };
    }

    private static EditorActionCategory category(EditorActionId id) {
        var name = id.name();
        if (name.startsWith("FILE_")) return EditorActionCategory.FILE;
        if (name.startsWith("DATA_")) return EditorActionCategory.DATA;
        if (name.startsWith("TABLE_")) return EditorActionCategory.TABLE;
        if (name.startsWith("PLOT_")) return EditorActionCategory.PLOT;
        if (name.startsWith("FIGURE_")) return EditorActionCategory.FIGURE;
        if (name.startsWith("DIAGRAM_")) return EditorActionCategory.DIAGRAM;
        if (name.startsWith("LAYOUT_")) return EditorActionCategory.LAYOUT;
        if (name.startsWith("VIEW_") || id == EditorActionId.TOGGLE_OUTLINE) return EditorActionCategory.VIEW;
        if (name.startsWith("STYLE_") || name.startsWith("FONT_") || name.startsWith("ALIGN_")
                || name.startsWith("LINE_SPACING_") || name.startsWith("INDENT_")
                || id == EditorActionId.BOLD || id == EditorActionId.ITALIC || id == EditorActionId.UNDERLINE
                || id == EditorActionId.TEXT_SUPERSCRIPT || id == EditorActionId.TEXT_SUBSCRIPT
                || id == EditorActionId.PARAGRAPH || name.startsWith("HEADING_")) return EditorActionCategory.FORMAT;
        if (name.startsWith("INSERT_") || name.startsWith("MATH_") || name.startsWith("QUANTITY_"))
            return EditorActionCategory.INSERT;
        return EditorActionCategory.EDIT;
    }

    private static EditorActionIcon icon(EditorActionId id) {
        return switch (id) {
            case FILE_NEW -> EditorActionIcon.NEW_DOCUMENT;
            case FILE_OPEN -> EditorActionIcon.OPEN;
            case FILE_SAVE, FILE_SAVE_AS, FILE_EXPORT_MARKDOWN, FILE_EXPORT_PDF -> EditorActionIcon.SAVE;
            case FILE_CLOSE -> EditorActionIcon.HOME;
            case FILE_RENAME, PARAGRAPH, HEADING_1, HEADING_2, HEADING_3, HEADING_4, HEADING_5, HEADING_6,
                    STYLE_BODY, STYLE_TITLE, STYLE_SUBTITLE, STYLE_AUTHOR, STYLE_AFFILIATION, STYLE_ABSTRACT,
                    STYLE_KEYWORDS, STYLE_REFERENCE, FONT_SIZE_10, FONT_SIZE_12, FONT_SIZE_14,
                    FONT_SOURCE_SANS, FONT_SCIENTIFIC_MATH -> EditorActionIcon.STYLE;
            case UNDO -> EditorActionIcon.UNDO;
            case REDO -> EditorActionIcon.REDO;
            case CUT -> EditorActionIcon.CUT;
            case COPY -> EditorActionIcon.COPY;
            case PASTE -> EditorActionIcon.PASTE;
            case DELETE, TABLE_DELETE_ROW, TABLE_DELETE_COLUMN, PLOT_DELETE_POINT, PLOT_DELETE_SERIES,
                    DIAGRAM_DELETE_ELECTRICAL_COMPONENT, DIAGRAM_DELETE_JUNCTION, DIAGRAM_DELETE_NODE,
                    DIAGRAM_DELETE_CONNECTION, DIAGRAM_DELETE_MECHANICAL_CONSTRAINT,
                    DIAGRAM_DELETE_MECHANICAL_DIMENSION, DIAGRAM_DELETE_MECHANICAL_PART_REFERENCE,
                    DIAGRAM_DELETE_MECHANICAL_ANNOTATION, DIAGRAM_DELETE_MECHANICAL_SYMBOL,
                    DIAGRAM_DELETE_MECHANICAL_PRIMITIVE -> EditorActionIcon.DELETE;
            case BOLD -> EditorActionIcon.BOLD;
            case ITALIC -> EditorActionIcon.ITALIC;
            case UNDERLINE -> EditorActionIcon.UNDERLINE;
            case TEXT_SUPERSCRIPT -> EditorActionIcon.SUPERSCRIPT;
            case TEXT_SUBSCRIPT -> EditorActionIcon.SUBSCRIPT;
            case ALIGN_LEFT -> EditorActionIcon.ALIGN_LEFT;
            case ALIGN_CENTER -> EditorActionIcon.ALIGN_CENTER;
            case ALIGN_RIGHT -> EditorActionIcon.ALIGN_RIGHT;
            case ALIGN_JUSTIFIED -> EditorActionIcon.ALIGN_JUSTIFY;
            case LINE_SPACING_SINGLE, LINE_SPACING_ONE_HALF -> EditorActionIcon.LINE_SPACING;
            case INDENT_DECREASE -> EditorActionIcon.INDENT_DECREASE;
            case INDENT_INCREASE -> EditorActionIcon.INDENT_INCREASE;
            case INSERT_EQUATION, INSERT_COMPUTED_RESULT, EDIT_COMPUTED_RESULT, MATH_INSERT_FRACTION,
                    MATH_INSERT_ROOT, MATH_INSERT_PARENTHESES_GROUP, MATH_INSERT_BRACKETS_GROUP,
                    MATH_INSERT_BRACES_GROUP, MATH_INSERT_SUPERSCRIPT, MATH_INSERT_SUBSCRIPT,
                    MATH_CONVERT_NAMED_OPERATOR, MATH_CONVERT_TEXT -> EditorActionIcon.EQUATION;
            case INSERT_TABLE, DATA_INSERT_DATASET_TABLE, TABLE_INSERT_ROW_ABOVE, TABLE_INSERT_ROW_BELOW,
                    TABLE_INSERT_COLUMN_LEFT, TABLE_INSERT_COLUMN_RIGHT -> EditorActionIcon.TABLE;
            case DATA_NEW_DATASET, DATA_INSERT_ANALYSIS, DATA_EDIT_ANALYSIS, FILE_IMPORT_CSV, DATA_EXPORT_CSV ->
                    EditorActionIcon.DATASET;
            case INSERT_VARIABLE, EDIT_VARIABLE, INSERT_QUANTITY_METRE, INSERT_QUANTITY_CELSIUS,
                    INSERT_QUANTITY_CELSIUS_DIFFERENCE, INSERT_QUANTITY_VOLT, INSERT_QUANTITY_MILLIAMPERE,
                    INSERT_QUANTITY_KILOOHM, INSERT_QUANTITY_ACCELERATION, QUANTITY_CONVERT_METRE,
                    QUANTITY_CONVERT_MILLIMETRE, QUANTITY_CONVERT_CELSIUS, QUANTITY_CONVERT_KELVIN,
                    QUANTITY_FORMAT_DECIMAL, QUANTITY_FORMAT_SCIENTIFIC, QUANTITY_FORMAT_ENGINEERING,
                    DATA_COLUMN_UNIT_NONE, DATA_COLUMN_UNIT_METRE, DATA_COLUMN_UNIT_SECOND,
                    DATA_COLUMN_UNIT_CELSIUS, DATA_COLUMN_UNIT_KELVIN, DATA_COLUMN_UNIT_CELSIUS_DIFFERENCE,
                    DATA_COLUMN_UNIT_KELVIN_DIFFERENCE, DATA_COLUMN_UNIT_VOLT, DATA_COLUMN_UNIT_AMPERE,
                    DATA_COLUMN_UNIT_OHM, PLOT_X_UNIT_AUTO, PLOT_X_UNIT_SECOND, PLOT_X_UNIT_METRE,
                    PLOT_Y_UNIT_AUTO, PLOT_Y_UNIT_CELSIUS, PLOT_Y_UNIT_KELVIN,
                    PLOT_Y_UNIT_CELSIUS_DIFFERENCE, PLOT_Y_UNIT_KELVIN_DIFFERENCE, PLOT_Y_UNIT_VOLT ->
                    EditorActionIcon.UNIT;
            case INSERT_PLOT, DATA_BIND_PLOT_TO_DATASET, DATA_ADD_FIT_OVERLAY, PLOT_TOGGLE_GRID,
                    PLOT_TOGGLE_LEGEND, PLOT_ADD_LINE_SERIES, PLOT_ADD_SCATTER_SERIES,
                    PLOT_SET_SERIES_LINE, PLOT_SET_SERIES_SCATTER, PLOT_ADD_POINT -> EditorActionIcon.PLOT;
            case FIGURE_WRAP_PLOT, FIGURE_WRAP_DIAGRAM, FIGURE_EDIT_CAPTION, FIGURE_UNWRAP -> EditorActionIcon.FIGURE;
            case INSERT_CROSS_REFERENCE -> EditorActionIcon.REFERENCE;
            case INSERT_TABLE_OF_CONTENTS, INSERT_PAGE_BREAK -> EditorActionIcon.TOC;
            case TOGGLE_OUTLINE -> EditorActionIcon.OUTLINE;
            case VIEW_ZOOM_OUT -> EditorActionIcon.ZOOM_OUT;
            case VIEW_ZOOM_IN -> EditorActionIcon.ZOOM_IN;
            case VIEW_FIT_PAGE -> EditorActionIcon.FIT_PAGE;
            case VIEW_FIT_WIDTH -> EditorActionIcon.FIT_WIDTH;
            case LAYOUT_MARGIN_NORMAL, LAYOUT_MARGIN_NARROW, LAYOUT_PORTRAIT, LAYOUT_LANDSCAPE,
                    LAYOUT_SIZE_LETTER, LAYOUT_SIZE_A4, LAYOUT_SIZE_LEGAL, LAYOUT_ONE_COLUMN,
                    LAYOUT_TWO_COLUMNS -> EditorActionIcon.VIEW;
            case DIAGRAM_ADD_NODE, DIAGRAM_ADD_RESISTOR, DIAGRAM_ADD_CAPACITOR,
                    DIAGRAM_ADD_DC_VOLTAGE_SOURCE, DIAGRAM_ADD_GROUND, DIAGRAM_ADD_DIODE,
                    DIAGRAM_ADD_LED, DIAGRAM_ADD_SWITCH_SPST, DIAGRAM_ADD_JUNCTION,
                    DIAGRAM_ROTATE_CLOCKWISE, DIAGRAM_ROTATE_COUNTERCLOCKWISE,
                    DIAGRAM_START_CONNECTION, DIAGRAM_FINISH_CONNECTION, DIAGRAM_CANCEL_CONNECTION ->
                    EditorActionIcon.ELECTRICAL;
            case DIAGRAM_ADD_MECHANICAL_LINE, DIAGRAM_ADD_MECHANICAL_CENTERLINE,
                    DIAGRAM_ADD_MECHANICAL_RECTANGLE, DIAGRAM_ADD_MECHANICAL_CIRCLE,
                    DIAGRAM_ADD_MECHANICAL_ARC, DIAGRAM_ADD_MECHANICAL_ARROW,
                    DIAGRAM_ADD_MECHANICAL_REFERENCE_POINT, DIAGRAM_ADD_MECHANICAL_PART_REFERENCE,
                    DIAGRAM_GENERATE_MECHANICAL_BOM, DIAGRAM_ADD_MECHANICAL_ANNOTATION_PART_LABEL,
                    DIAGRAM_ADD_MECHANICAL_ANNOTATION_NOTE, DIAGRAM_ADD_MECHANICAL_ANNOTATION_LEADER,
                    DIAGRAM_ADD_MECHANICAL_SYMBOL_SHAFT, DIAGRAM_ADD_MECHANICAL_SYMBOL_GEAR,
                    DIAGRAM_ADD_MECHANICAL_SYMBOL_BEARING, DIAGRAM_ADD_MECHANICAL_SYMBOL_SPRING,
                    DIAGRAM_ADD_MECHANICAL_SYMBOL_PISTON, DIAGRAM_ADD_MECHANICAL_SYMBOL_BOLT,
                    DIAGRAM_ADD_MECHANICAL_DIMENSION_HORIZONTAL, DIAGRAM_ADD_MECHANICAL_DIMENSION_VERTICAL,
                    DIAGRAM_ADD_MECHANICAL_DIMENSION_ALIGNED, DIAGRAM_ADD_MECHANICAL_DIMENSION_RADIUS,
                    DIAGRAM_ADD_MECHANICAL_DIMENSION_DIAMETER, DIAGRAM_ADD_MECHANICAL_DIMENSION_ANGLE,
                    DIAGRAM_ADD_MECHANICAL_CONSTRAINT_HORIZONTAL, DIAGRAM_ADD_MECHANICAL_CONSTRAINT_VERTICAL,
                    DIAGRAM_START_MECHANICAL_CONSTRAINT_COINCIDENT, DIAGRAM_START_MECHANICAL_CONSTRAINT_PARALLEL,
                    DIAGRAM_START_MECHANICAL_CONSTRAINT_PERPENDICULAR, DIAGRAM_START_MECHANICAL_CONSTRAINT_CONCENTRIC,
                    DIAGRAM_FINISH_MECHANICAL_CONSTRAINT, DIAGRAM_CANCEL_MECHANICAL_CONSTRAINT,
                    DIAGRAM_SCALE_SYMBOLS_DOWN, DIAGRAM_SCALE_SYMBOLS_UP, DIAGRAM_WORKSPACE_SHORTER,
                    DIAGRAM_WORKSPACE_TALLER, DIAGRAM_WORKSPACE_RESET_HEIGHT -> EditorActionIcon.MECHANICAL;
            default -> EditorActionIcon.DIAGRAM;
        };
    }
}
