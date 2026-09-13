package dev.rgcb.scholar.editor;

import dev.rgcb.scholar.document.DiagramBlock;
import dev.rgcb.scholar.document.EquationBlock;
import dev.rgcb.scholar.document.FigureBlock;
import dev.rgcb.scholar.document.Heading;
import dev.rgcb.scholar.document.PlotBlock;
import dev.rgcb.scholar.document.TableBlock;
import dev.rgcb.scholar.document.TableOfContentsBlock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class EditorContextActionResolver {
    public List<ContextMenuEntry> resolve(EditorState state, Map<EditorActionId, EditorAction> actions) {
        return resolve(state, actions, false);
    }

    public List<ContextMenuEntry> resolveEmptyArea(EditorState state, Map<EditorActionId, EditorAction> actions) {
        return resolve(state, actions, true);
    }

    private List<ContextMenuEntry> resolve(EditorState state, Map<EditorActionId, EditorAction> actions, boolean emptyArea) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(actions, "actions");
        var entries = new ArrayList<ContextMenuEntry>();
        if (emptyArea) {
            addActions(entries, actions, EditorActionId.PASTE);
            return clean(entries);
        }

        if (state.isTableEditingSelection()) {
            addClipboard(entries, actions, state.hasSelection());
            addSeparator(entries);
            addActions(entries, actions, EditorActionId.BOLD, EditorActionId.ITALIC);
            addSeparator(entries);
            addActions(entries, actions,
                    EditorActionId.TABLE_INSERT_ROW_ABOVE,
                    EditorActionId.TABLE_INSERT_ROW_BELOW,
                    EditorActionId.TABLE_DELETE_ROW,
                    EditorActionId.TABLE_INSERT_COLUMN_LEFT,
                    EditorActionId.TABLE_INSERT_COLUMN_RIGHT,
                    EditorActionId.TABLE_DELETE_COLUMN);
            return clean(entries);
        }
        if (state.isEquationEditingSelection()) {
            addClipboard(entries, actions, true);
            addSeparator(entries);
            addActions(entries, actions,
                    EditorActionId.MATH_INSERT_FRACTION,
                    EditorActionId.MATH_INSERT_ROOT,
                    EditorActionId.MATH_INSERT_SUPERSCRIPT,
                    EditorActionId.MATH_INSERT_SUBSCRIPT,
                    EditorActionId.MATH_INSERT_PARENTHESES_GROUP,
                    EditorActionId.MATH_INSERT_BRACKETS_GROUP,
                    EditorActionId.MATH_INSERT_BRACES_GROUP,
                    EditorActionId.MATH_CONVERT_NAMED_OPERATOR,
                    EditorActionId.MATH_CONVERT_TEXT);
            return clean(entries);
        }
        if (state.isPlotEditingSelection()) {
            addActions(entries, actions,
                    EditorActionId.PLOT_TOGGLE_GRID,
                    EditorActionId.PLOT_TOGGLE_LEGEND,
                    EditorActionId.PLOT_ADD_LINE_SERIES,
                    EditorActionId.PLOT_ADD_SCATTER_SERIES,
                    EditorActionId.PLOT_SET_SERIES_LINE,
                    EditorActionId.PLOT_SET_SERIES_SCATTER,
                    EditorActionId.PLOT_ADD_POINT,
                    EditorActionId.PLOT_DELETE_POINT,
                    EditorActionId.PLOT_DELETE_SERIES);
            return clean(entries);
        }
        if (state.isDiagramEditingSelection()) {
            addDiagramActions(entries, actions, state.diagramEditingSelection().target());
            return clean(entries);
        }
        if (state.isFigureCaptionSelection()) {
            addClipboard(entries, actions, true);
            addSeparator(entries);
            addActions(entries, actions, EditorActionId.BOLD, EditorActionId.ITALIC);
            return clean(entries);
        }
        if (state.isTextSelection()) {
            addClipboard(entries, actions, state.hasSelection());
            addSeparator(entries);
            addActions(entries, actions, EditorActionId.BOLD, EditorActionId.ITALIC);
            addSeparator(entries);
            addActions(entries, actions, EditorActionId.INSERT_CROSS_REFERENCE);
            if (state.document().blocks().get(state.active().blockIndex()) instanceof Heading) {
                addSeparator(entries);
                addBlockStyleActions(entries, actions);
            }
            return clean(entries);
        }
        if (state.isBlockSelection()) {
            addBlockActions(entries, actions, state);
        }
        return clean(entries);
    }

    private static void addBlockActions(List<ContextMenuEntry> entries, Map<EditorActionId, EditorAction> actions, EditorState state) {
        var block = state.document().blocks().get(state.blockSelection().blockIndex());
        addActions(entries, actions, EditorActionId.CUT, EditorActionId.COPY, EditorActionId.PASTE, EditorActionId.DELETE);
        if (block instanceof EquationBlock) {
            addSeparator(entries);
            addActions(entries, actions,
                    EditorActionId.MATH_INSERT_FRACTION,
                    EditorActionId.MATH_INSERT_ROOT,
                    EditorActionId.MATH_INSERT_SUPERSCRIPT,
                    EditorActionId.MATH_INSERT_SUBSCRIPT);
        } else if (block instanceof TableBlock) {
            addSeparator(entries);
            addActions(entries, actions,
                    EditorActionId.TABLE_INSERT_ROW_ABOVE,
                    EditorActionId.TABLE_INSERT_ROW_BELOW,
                    EditorActionId.TABLE_DELETE_ROW,
                    EditorActionId.TABLE_INSERT_COLUMN_LEFT,
                    EditorActionId.TABLE_INSERT_COLUMN_RIGHT,
                    EditorActionId.TABLE_DELETE_COLUMN);
        } else if (block instanceof PlotBlock) {
            addSeparator(entries);
            addActions(entries, actions, EditorActionId.FIGURE_WRAP_PLOT);
        } else if (block instanceof DiagramBlock) {
            addSeparator(entries);
            addActions(entries, actions, EditorActionId.FIGURE_WRAP_DIAGRAM);
        } else if (block instanceof FigureBlock) {
            addSeparator(entries);
            addActions(entries, actions, EditorActionId.FIGURE_EDIT_CAPTION, EditorActionId.FIGURE_UNWRAP);
        } else if (block instanceof TableOfContentsBlock) {
            addSeparator(entries);
            addActions(entries, actions, EditorActionId.TOGGLE_OUTLINE);
        }
    }

    private static void addDiagramActions(List<ContextMenuEntry> entries, Map<EditorActionId, EditorAction> actions, DiagramEditTarget target) {
        if (target instanceof DiagramElementTarget) {
            addActions(entries, actions,
                    EditorActionId.DIAGRAM_ROTATE_CLOCKWISE,
                    EditorActionId.DIAGRAM_ROTATE_COUNTERCLOCKWISE,
                    EditorActionId.DIAGRAM_DELETE_ELECTRICAL_COMPONENT,
                    EditorActionId.DIAGRAM_DELETE_JUNCTION,
                    EditorActionId.DIAGRAM_DELETE_MECHANICAL_PRIMITIVE,
                    EditorActionId.DIAGRAM_DELETE_MECHANICAL_DIMENSION,
                    EditorActionId.DIAGRAM_DELETE_MECHANICAL_CONSTRAINT,
                    EditorActionId.DIAGRAM_DELETE_MECHANICAL_SYMBOL,
                    EditorActionId.DIAGRAM_DELETE_MECHANICAL_ANNOTATION,
                    EditorActionId.DIAGRAM_DELETE_MECHANICAL_PART_REFERENCE,
                    EditorActionId.DIAGRAM_DELETE_NODE);
            addSeparator(entries);
            addActions(entries, actions,
                    EditorActionId.DIAGRAM_START_CONNECTION,
                    EditorActionId.DIAGRAM_ADD_MECHANICAL_PART_REFERENCE,
                    EditorActionId.DIAGRAM_GENERATE_MECHANICAL_BOM);
            return;
        }
        if (target instanceof DiagramConnectionTarget || target instanceof DiagramPortTarget) {
            addActions(entries, actions,
                    EditorActionId.DIAGRAM_START_CONNECTION,
                    EditorActionId.DIAGRAM_FINISH_CONNECTION,
                    EditorActionId.DIAGRAM_CANCEL_CONNECTION,
                    EditorActionId.DIAGRAM_DELETE_CONNECTION);
            return;
        }
        addActions(entries, actions,
                EditorActionId.DIAGRAM_ADD_NODE,
                EditorActionId.DIAGRAM_ADD_RESISTOR,
                EditorActionId.DIAGRAM_ADD_CAPACITOR,
                EditorActionId.DIAGRAM_ADD_DC_VOLTAGE_SOURCE,
                EditorActionId.DIAGRAM_ADD_GROUND,
                EditorActionId.DIAGRAM_ADD_JUNCTION);
        addSeparator(entries);
        addActions(entries, actions,
                EditorActionId.DIAGRAM_ADD_MECHANICAL_LINE,
                EditorActionId.DIAGRAM_ADD_MECHANICAL_RECTANGLE,
                EditorActionId.DIAGRAM_ADD_MECHANICAL_CIRCLE,
                EditorActionId.DIAGRAM_ADD_MECHANICAL_DIMENSION_HORIZONTAL,
                EditorActionId.DIAGRAM_ADD_MECHANICAL_DIMENSION_VERTICAL,
                EditorActionId.DIAGRAM_ADD_MECHANICAL_SYMBOL_SHAFT,
                EditorActionId.DIAGRAM_ADD_MECHANICAL_ANNOTATION_NOTE);
        addSeparator(entries);
        addActions(entries, actions,
                EditorActionId.DIAGRAM_SCALE_SYMBOLS_DOWN,
                EditorActionId.DIAGRAM_SCALE_SYMBOLS_UP,
                EditorActionId.DIAGRAM_WORKSPACE_SHORTER,
                EditorActionId.DIAGRAM_WORKSPACE_TALLER,
                EditorActionId.DIAGRAM_WORKSPACE_RESET_HEIGHT);
    }

    private static void addClipboard(List<ContextMenuEntry> entries, Map<EditorActionId, EditorAction> actions, boolean includeCutCopy) {
        if (includeCutCopy) {
            addActions(entries, actions, EditorActionId.CUT, EditorActionId.COPY);
        }
        addActions(entries, actions, EditorActionId.PASTE);
    }

    private static void addBlockStyleActions(List<ContextMenuEntry> entries, Map<EditorActionId, EditorAction> actions) {
        addActions(entries, actions,
                EditorActionId.PARAGRAPH,
                EditorActionId.HEADING_1,
                EditorActionId.HEADING_2,
                EditorActionId.HEADING_3,
                EditorActionId.HEADING_4,
                EditorActionId.HEADING_5,
                EditorActionId.HEADING_6);
    }

    private static void addActions(List<ContextMenuEntry> entries, Map<EditorActionId, EditorAction> actions, EditorActionId... ids) {
        for (var id : ids) {
            action(actions, id).ifPresent(action -> entries.add(ContextMenuEntry.action(action)));
        }
    }

    private static Optional<EditorAction> action(Map<EditorActionId, EditorAction> actions, EditorActionId id) {
        return Optional.ofNullable(actions.get(id));
    }

    private static void addSeparator(List<ContextMenuEntry> entries) {
        entries.add(ContextMenuEntry.separator());
    }

    private static List<ContextMenuEntry> clean(List<ContextMenuEntry> entries) {
        var cleaned = new ArrayList<ContextMenuEntry>();
        var previousSeparator = true;
        for (var entry : entries) {
            if (entry.kind() == ContextMenuEntryKind.SEPARATOR) {
                if (!previousSeparator) {
                    cleaned.add(entry);
                }
                previousSeparator = true;
            } else {
                cleaned.add(entry);
                previousSeparator = false;
            }
        }
        while (!cleaned.isEmpty() && cleaned.get(cleaned.size() - 1).kind() == ContextMenuEntryKind.SEPARATOR) {
            cleaned.remove(cleaned.size() - 1);
        }
        return List.copyOf(cleaned);
    }
}
