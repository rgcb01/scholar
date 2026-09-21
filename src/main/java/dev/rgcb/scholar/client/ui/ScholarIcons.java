package dev.rgcb.scholar.client.ui;

import dev.rgcb.scholar.editor.EditorActionId;

/** Closed icon registry for Scholar's built-in application commands. */
public final class ScholarIcons {
    public static final ScholarIcon SCHOLAR = icon("scholar", "   ##   ", "  ####  ", " ##  ## ", "###### ", " ##  ## ", "  ####  ", "   ##   ");
    public static final ScholarIcon DOCUMENT = icon("document", " #####  ", " #   ## ", " #    # ", " # ## # ", " # ## # ", " #    # ", " ###### ");
    public static final ScholarIcon NEW_DOCUMENT = icon("new-document", " ####   ", " #  ##  ", " #   #  ", " ### #  ", " #  ### ", " ###### ", "    #   ");
    public static final ScholarIcon OPEN = icon("open", "        ", " ###    ", "#  #    ", "####### ", "#     # ", " #####  ", "        ");
    public static final ScholarIcon SAVE = icon("save", "######  ", "# ## #  ", "# ## #  ", "#    #  ", "# ## #  ", "# ## #  ", "######  ");
    public static final ScholarIcon UNDO = icon("undo", "  #     ", " ##     ", "#####   ", "  # ##  ", "     #  ", "  ###   ", "        ");
    public static final ScholarIcon REDO = icon("redo", "    #   ", "    ##  ", "  ##### ", " ## #   ", " #      ", "  ###   ", "        ");
    public static final ScholarIcon CUT = icon("cut", "#    #  ", " #  #   ", "  ##    ", "  ##    ", " #  #   ", "#    #  ", "        ");
    public static final ScholarIcon COPY = icon("copy", " ####   ", " #  #   ", " # ###  ", " ### #  ", "   # #  ", "   ###  ", "        ");
    public static final ScholarIcon PASTE = icon("paste", "  ###   ", " ## ##  ", "######  ", "#    #  ", "# ## #  ", "#    #  ", "######  ");
    public static final ScholarIcon BOLD = icon("bold", "#####   ", "#    #  ", "#####   ", "#    #  ", "#    #  ", "#####   ", "        ");
    public static final ScholarIcon ITALIC = icon("italic", " ####   ", "  ##    ", "  ##    ", " ##     ", " ##     ", "####    ", "        ");
    public static final ScholarIcon UNDERLINE = icon("underline", "#    #  ", "#    #  ", "#    #  ", "#    #  ", " ####   ", "        ", "######  ");
    public static final ScholarIcon SUPERSCRIPT = icon("superscript", "#  # ## ", " ##    #", " ##   # ", "#  # ###", "        ", "        ", "        ");
    public static final ScholarIcon SUBSCRIPT = icon("subscript", "#  #    ", " ##     ", " ##     ", "#  #    ", "     ## ", "      # ", "     ###");
    public static final ScholarIcon ALIGN_LEFT = icon("align-left", "######  ", "        ", "####    ", "        ", "######  ", "        ", "####    ");
    public static final ScholarIcon ALIGN_CENTER = icon("align-center", "######  ", "        ", " ####   ", "        ", "######  ", "        ", " ####   ");
    public static final ScholarIcon ALIGN_RIGHT = icon("align-right", "######  ", "        ", "  ####  ", "        ", "######  ", "        ", "  ####  ");
    public static final ScholarIcon ALIGN_JUSTIFY = icon("align-justify", "######  ", "        ", "######  ", "        ", "######  ", "        ", "######  ");
    public static final ScholarIcon LINE_SPACING = icon("line-spacing", "# ##### ", "#       ", "# ##### ", "#       ", "# ##### ", "        ", "        ");
    public static final ScholarIcon INDENT_DECREASE = icon("indent-decrease", "  ##### ", "  ###   ", "# ##### ", "  ###   ", "  ##### ", "        ", "        ");
    public static final ScholarIcon INDENT_INCREASE = icon("indent-increase", "#####   ", "###     ", "##### # ", "###     ", "#####   ", "        ", "        ");
    public static final ScholarIcon STYLE = icon("style", "######  ", "        ", "#####   ", "        ", "###     ", "        ", "######  ");
    public static final ScholarIcon EQUATION = icon("equation", "#    #  ", " #  #   ", "  ##    ", "  ##    ", " #  #   ", "#    #  ", "  ===   ");
    public static final ScholarIcon TABLE = icon("table", "####### ", "# # # # ", "####### ", "# # # # ", "####### ", "# # # # ", "####### ");
    public static final ScholarIcon DATASET = icon("dataset", " #####  ", "#     # ", " #####  ", "#     # ", " #####  ", "#     # ", " #####  ");
    public static final ScholarIcon UNIT = icon("unit", "#     # ", "# ### # ", "## # ## ", "#  #  # ", "# ### # ", "#     # ", "####### ");
    public static final ScholarIcon PLOT = icon("plot", "#       ", "#    ## ", "#   #   ", "# ##    ", "##      ", "#       ", "####### ");
    public static final ScholarIcon FIGURE = icon("figure", "####### ", "#  #  # ", "# ### # ", "##   ## ", "#     # ", "# ### # ", "####### ");
    public static final ScholarIcon REFERENCE = icon("reference", " ## ##  ", "#  #  # ", "   #    ", "  #     ", " #      ", "#  #  # ", " ## ##  ");
    public static final ScholarIcon TOC = icon("toc", "# ##### ", "        ", "# ####  ", "        ", "# ##### ", "        ", "# ###   ");
    public static final ScholarIcon DIAGRAM = icon("diagram", " ##  ## ", " ##  ## ", "  ###   ", "   #    ", "  ###   ", " ##  ## ", " ##  ## ");
    public static final ScholarIcon ELECTRICAL = icon("electrical", "#       ", "# ###   ", "## # ## ", " # # #  ", " ## # ## ", "   ### #", "       #");
    public static final ScholarIcon MECHANICAL = icon("mechanical", "  # #   ", " ###### ", "## ## ##", "#  ##  #", "## ## ##", " ###### ", "  # #   ");
    public static final ScholarIcon VIEW = icon("view", "  ###   ", "##   ## ", "#  #  # ", "# ### # ", "#  #  # ", "##   ## ", "  ###   ");
    public static final ScholarIcon OUTLINE = icon("outline", "# ##### ", "#       ", "# ####  ", "#       ", "# ###   ", "        ", "        ");
    public static final ScholarIcon ZOOM_OUT = icon("zoom-out", "  ###   ", " #   #  ", " #---#  ", " #   #  ", "  ### # ", "     ## ", "      # ");
    public static final ScholarIcon ZOOM_IN = icon("zoom-in", "  ###   ", " # # #  ", " # + #  ", " # # #  ", "  ### # ", "     ## ", "      # ");
    public static final ScholarIcon FIT_PAGE = icon("fit-page", "####### ", "# ### # ", "# # # # ", "# ### # ", "#     # ", "####### ", "        ");
    public static final ScholarIcon FIT_WIDTH = icon("fit-width", "        ", "#     # ", "##   ## ", "####### ", "##   ## ", "#     # ", "        ");
    public static final ScholarIcon DELETE = icon("delete", " #####  ", "  ###   ", " #   #  ", " # # #  ", " #   #  ", " #####  ", "        ");
    public static final ScholarIcon HOME = icon("home", "   #    ", "  ###   ", " #####  ", "##   ## ", " #   #  ", " #####  ", "        ");

    private ScholarIcons() { }

    public static ScholarIcon forAction(EditorActionId id) {
        return switch (id) {
            case FILE_NEW -> NEW_DOCUMENT;
            case FILE_OPEN -> OPEN;
            case FILE_SAVE, FILE_SAVE_AS -> SAVE;
            case FILE_CLOSE -> HOME;
            case UNDO -> UNDO;
            case REDO -> REDO;
            case CUT -> CUT;
            case COPY -> COPY;
            case PASTE -> PASTE;
            case DELETE, TABLE_DELETE_ROW, TABLE_DELETE_COLUMN, PLOT_DELETE_POINT, PLOT_DELETE_SERIES,
                    DIAGRAM_DELETE_ELECTRICAL_COMPONENT, DIAGRAM_DELETE_JUNCTION, DIAGRAM_DELETE_NODE,
                    DIAGRAM_DELETE_CONNECTION, DIAGRAM_DELETE_MECHANICAL_CONSTRAINT,
                    DIAGRAM_DELETE_MECHANICAL_DIMENSION, DIAGRAM_DELETE_MECHANICAL_PART_REFERENCE,
                    DIAGRAM_DELETE_MECHANICAL_ANNOTATION, DIAGRAM_DELETE_MECHANICAL_SYMBOL,
                    DIAGRAM_DELETE_MECHANICAL_PRIMITIVE -> DELETE;
            case BOLD -> BOLD;
            case ITALIC -> ITALIC;
            case UNDERLINE -> UNDERLINE;
            case TEXT_SUPERSCRIPT -> SUPERSCRIPT;
            case TEXT_SUBSCRIPT -> SUBSCRIPT;
            case FONT_SIZE_10, FONT_SIZE_12, FONT_SIZE_14, FONT_SOURCE_SANS, FONT_SCIENTIFIC_MATH -> STYLE;
            case PARAGRAPH, HEADING_1, HEADING_2, HEADING_3, HEADING_4, HEADING_5, HEADING_6,
                    STYLE_BODY, STYLE_TITLE, STYLE_SUBTITLE, STYLE_AUTHOR, STYLE_AFFILIATION,
                    STYLE_ABSTRACT, STYLE_KEYWORDS, STYLE_REFERENCE -> STYLE;
            case ALIGN_LEFT -> ALIGN_LEFT;
            case ALIGN_CENTER -> ALIGN_CENTER;
            case ALIGN_RIGHT -> ALIGN_RIGHT;
            case ALIGN_JUSTIFIED -> ALIGN_JUSTIFY;
            case LINE_SPACING_SINGLE, LINE_SPACING_ONE_HALF -> LINE_SPACING;
            case INDENT_DECREASE -> INDENT_DECREASE;
            case INDENT_INCREASE -> INDENT_INCREASE;
            case INSERT_EQUATION, MATH_INSERT_FRACTION, MATH_INSERT_ROOT, MATH_INSERT_PARENTHESES_GROUP,
                    MATH_INSERT_BRACKETS_GROUP, MATH_INSERT_BRACES_GROUP, MATH_INSERT_SUPERSCRIPT,
                    MATH_INSERT_SUBSCRIPT, MATH_CONVERT_NAMED_OPERATOR, MATH_CONVERT_TEXT -> EQUATION;
            case INSERT_TABLE, DATA_INSERT_DATASET_TABLE, TABLE_INSERT_ROW_ABOVE, TABLE_INSERT_ROW_BELOW,
                    TABLE_INSERT_COLUMN_LEFT, TABLE_INSERT_COLUMN_RIGHT -> TABLE;
            case DATA_NEW_DATASET -> DATASET;
            case INSERT_QUANTITY_METRE, INSERT_QUANTITY_CELSIUS, INSERT_QUANTITY_VOLT,
                    INSERT_QUANTITY_MILLIAMPERE, INSERT_QUANTITY_KILOOHM, INSERT_QUANTITY_ACCELERATION,
                    QUANTITY_CONVERT_METRE, QUANTITY_CONVERT_MILLIMETRE, QUANTITY_CONVERT_CELSIUS,
                    QUANTITY_CONVERT_KELVIN, QUANTITY_FORMAT_DECIMAL, QUANTITY_FORMAT_SCIENTIFIC,
                    QUANTITY_FORMAT_ENGINEERING,
                    DATA_COLUMN_UNIT_NONE, DATA_COLUMN_UNIT_METRE, DATA_COLUMN_UNIT_SECOND,
                    DATA_COLUMN_UNIT_CELSIUS, DATA_COLUMN_UNIT_KELVIN, DATA_COLUMN_UNIT_VOLT,
                    DATA_COLUMN_UNIT_AMPERE, DATA_COLUMN_UNIT_OHM,
                    PLOT_X_UNIT_AUTO, PLOT_X_UNIT_SECOND, PLOT_X_UNIT_METRE,
                    PLOT_Y_UNIT_AUTO, PLOT_Y_UNIT_CELSIUS, PLOT_Y_UNIT_KELVIN, PLOT_Y_UNIT_VOLT -> UNIT;
            case INSERT_PLOT, DATA_BIND_PLOT_TO_DATASET, PLOT_TOGGLE_GRID, PLOT_TOGGLE_LEGEND,
                    PLOT_ADD_LINE_SERIES, PLOT_ADD_SCATTER_SERIES, PLOT_SET_SERIES_LINE,
                    PLOT_SET_SERIES_SCATTER, PLOT_ADD_POINT -> PLOT;
            case FIGURE_WRAP_PLOT, FIGURE_WRAP_DIAGRAM, FIGURE_EDIT_CAPTION, FIGURE_UNWRAP -> FIGURE;
            case INSERT_CROSS_REFERENCE -> REFERENCE;
            case INSERT_TABLE_OF_CONTENTS, INSERT_PAGE_BREAK -> TOC;
            case TOGGLE_OUTLINE -> OUTLINE;
            case VIEW_ZOOM_OUT -> ZOOM_OUT;
            case VIEW_ZOOM_IN -> ZOOM_IN;
            case VIEW_FIT_PAGE -> FIT_PAGE;
            case VIEW_FIT_WIDTH -> FIT_WIDTH;
            case LAYOUT_MARGIN_NORMAL, LAYOUT_MARGIN_NARROW, LAYOUT_PORTRAIT, LAYOUT_LANDSCAPE,
                    LAYOUT_SIZE_LETTER, LAYOUT_SIZE_A4, LAYOUT_SIZE_LEGAL,
                    LAYOUT_ONE_COLUMN, LAYOUT_TWO_COLUMNS -> VIEW;
            case FILE_RENAME -> STYLE;
            case DIAGRAM_ADD_NODE, DIAGRAM_ADD_RESISTOR, DIAGRAM_ADD_CAPACITOR,
                    DIAGRAM_ADD_DC_VOLTAGE_SOURCE, DIAGRAM_ADD_GROUND, DIAGRAM_ADD_DIODE,
                    DIAGRAM_ADD_LED, DIAGRAM_ADD_SWITCH_SPST, DIAGRAM_ADD_JUNCTION,
                    DIAGRAM_ROTATE_CLOCKWISE, DIAGRAM_ROTATE_COUNTERCLOCKWISE,
                    DIAGRAM_START_CONNECTION, DIAGRAM_FINISH_CONNECTION, DIAGRAM_CANCEL_CONNECTION -> ELECTRICAL;
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
                    DIAGRAM_WORKSPACE_TALLER, DIAGRAM_WORKSPACE_RESET_HEIGHT -> MECHANICAL;
            default -> DIAGRAM;
        };
    }

    private static ScholarIcon icon(String name, String... rows) {
        return new ScholarIcon(name, java.util.List.of(rows));
    }
}
