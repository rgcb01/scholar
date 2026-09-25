package dev.rgcb.scholar.client.ui;

import dev.rgcb.scholar.editor.EditorActionId;
import dev.rgcb.scholar.editor.BuiltInEditorActionCatalog;

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
        return switch (BuiltInEditorActionCatalog.require(id).icon()) {
            case NEW_DOCUMENT -> NEW_DOCUMENT;
            case OPEN -> OPEN;
            case SAVE -> SAVE;
            case HOME -> HOME;
            case UNDO -> UNDO;
            case REDO -> REDO;
            case CUT -> CUT;
            case COPY -> COPY;
            case PASTE -> PASTE;
            case DELETE -> DELETE;
            case BOLD -> BOLD;
            case ITALIC -> ITALIC;
            case UNDERLINE -> UNDERLINE;
            case SUPERSCRIPT -> SUPERSCRIPT;
            case SUBSCRIPT -> SUBSCRIPT;
            case ALIGN_LEFT -> ALIGN_LEFT;
            case ALIGN_CENTER -> ALIGN_CENTER;
            case ALIGN_RIGHT -> ALIGN_RIGHT;
            case ALIGN_JUSTIFY -> ALIGN_JUSTIFY;
            case LINE_SPACING -> LINE_SPACING;
            case INDENT_DECREASE -> INDENT_DECREASE;
            case INDENT_INCREASE -> INDENT_INCREASE;
            case STYLE -> STYLE;
            case EQUATION -> EQUATION;
            case TABLE -> TABLE;
            case DATASET -> DATASET;
            case UNIT -> UNIT;
            case PLOT -> PLOT;
            case FIGURE -> FIGURE;
            case REFERENCE -> REFERENCE;
            case TOC -> TOC;
            case DIAGRAM -> DIAGRAM;
            case ELECTRICAL -> ELECTRICAL;
            case MECHANICAL -> MECHANICAL;
            case VIEW -> VIEW;
            case OUTLINE -> OUTLINE;
            case ZOOM_OUT -> ZOOM_OUT;
            case ZOOM_IN -> ZOOM_IN;
            case FIT_PAGE -> FIT_PAGE;
            case FIT_WIDTH -> FIT_WIDTH;
        };
    }

    private static ScholarIcon icon(String name, String... rows) {
        return new ScholarIcon(name, java.util.List.of(rows));
    }
}
