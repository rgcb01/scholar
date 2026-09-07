package dev.rgcb.scholar.client.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.editor.BuiltInEditorActions;
import dev.rgcb.scholar.editor.EditorAction;
import dev.rgcb.scholar.editor.EditorActionId;
import java.util.List;
import org.junit.jupiter.api.Test;

class ToolbarLayoutTest {
    @Test
    void toolbarItemsContainActionsAndSeparatorInOrder() {
        var actions = toolbarActions();
        var items = items(actions);

        assertEquals(ToolbarItemKind.ACTION, items.get(0).kind());
        assertEquals(EditorActionId.UNDO, items.get(0).action().orElseThrow().id());
        assertEquals(EditorActionId.REDO, items.get(1).action().orElseThrow().id());
        assertEquals(ToolbarItemKind.SEPARATOR, items.get(2).kind());
        assertEquals(ToolbarItemKind.BLOCK_STYLE, items.get(3).kind());
        assertEquals(ToolbarItemKind.SEPARATOR, items.get(4).kind());
        assertEquals(EditorActionId.BOLD, items.get(5).action().orElseThrow().id());
        assertEquals(EditorActionId.ITALIC, items.get(6).action().orElseThrow().id());
    }

    @Test
    void mathToolbarCanExposeFractionRootScriptsAndConvertDropdownAfterUndoRedo() {
        var actions = mathToolbarActions();
        var items = List.of(
                ToolbarItem.action(action(actions, EditorActionId.UNDO)),
                ToolbarItem.action(action(actions, EditorActionId.REDO)),
                ToolbarItem.separator(),
                ToolbarItem.action(action(actions, EditorActionId.MATH_INSERT_FRACTION)),
                ToolbarItem.action(action(actions, EditorActionId.MATH_INSERT_ROOT)),
                ToolbarItem.action(action(actions, EditorActionId.MATH_INSERT_SUPERSCRIPT)),
                ToolbarItem.action(action(actions, EditorActionId.MATH_INSERT_SUBSCRIPT)),
                ToolbarItem.separator(),
                ToolbarItem.semanticConvert());

        assertEquals(EditorActionId.UNDO, items.get(0).action().orElseThrow().id());
        assertEquals(EditorActionId.REDO, items.get(1).action().orElseThrow().id());
        assertEquals(ToolbarItemKind.SEPARATOR, items.get(2).kind());
        assertEquals(EditorActionId.MATH_INSERT_FRACTION, items.get(3).action().orElseThrow().id());
        assertEquals(EditorActionId.MATH_INSERT_ROOT, items.get(4).action().orElseThrow().id());
        assertEquals(EditorActionId.MATH_INSERT_SUPERSCRIPT, items.get(5).action().orElseThrow().id());
        assertEquals(EditorActionId.MATH_INSERT_SUBSCRIPT, items.get(6).action().orElseThrow().id());
        assertEquals(ToolbarItemKind.SEPARATOR, items.get(7).kind());
        assertEquals(ToolbarItemKind.SEMANTIC_CONVERT, items.get(8).kind());
        assertEquals(9, ToolbarLayout.compute(items, 520, action -> 48).size());
    }

    @Test
    void layoutFitsItemsLeftToRight() {
        var bounds = ToolbarLayout.compute(items(toolbarActions()), 400, action -> 24);

        assertEquals(7, bounds.size());
        assertEquals(ToolbarItemKind.ACTION, bounds.get(0).kind());
        assertEquals(ToolbarItemKind.SEPARATOR, bounds.get(2).kind());
        assertEquals(ToolbarItemKind.BLOCK_STYLE, bounds.get(3).kind());
        assertTrue(bounds.get(1).x() > bounds.get(0).x());
        assertTrue(bounds.get(3).x() > bounds.get(2).x());
        assertTrue(bounds.get(5).x() > bounds.get(4).x());
    }

    @Test
    void responsiveLayoutHidesTrailingItemsWhenWidthIsInsufficient() {
        var bounds = ToolbarLayout.compute(items(toolbarActions()), 95, action -> 24);

        assertTrue(bounds.size() < 7);
        assertEquals(ToolbarItemKind.ACTION, bounds.get(0).kind());
        assertEquals(ToolbarItemKind.ACTION, bounds.get(1).kind());
    }

    @Test
    void layoutNeverProducesNegativeBounds() {
        var bounds = ToolbarLayout.compute(items(toolbarActions()), 20, action -> 24);

        assertTrue(bounds.isEmpty());
    }

    @Test
    void rejectsInvalidToolbarItems() {
        assertThrows(IllegalArgumentException.class, () -> new ToolbarItem(ToolbarItemKind.ACTION, java.util.Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> new ToolbarItem(ToolbarItemKind.SEPARATOR, java.util.Optional.of(BuiltInEditorActions.undo())));
        assertThrows(IllegalArgumentException.class, () -> new ToolbarItem(ToolbarItemKind.BLOCK_STYLE, java.util.Optional.of(BuiltInEditorActions.undo())));
        assertThrows(IllegalArgumentException.class, () -> new ToolbarItem(ToolbarItemKind.SEMANTIC_CONVERT, java.util.Optional.of(BuiltInEditorActions.undo())));
    }

    private static List<ToolbarItem> items(List<EditorAction> actions) {
        return List.of(
                ToolbarItem.action(action(actions, EditorActionId.UNDO)),
                ToolbarItem.action(action(actions, EditorActionId.REDO)),
                ToolbarItem.separator(),
                ToolbarItem.blockStyle(),
                ToolbarItem.separator(),
                ToolbarItem.action(action(actions, EditorActionId.BOLD)),
                ToolbarItem.action(action(actions, EditorActionId.ITALIC)));
    }

    private static List<EditorAction> toolbarActions() {
        return List.of(
                BuiltInEditorActions.undo(),
                BuiltInEditorActions.redo(),
                BuiltInEditorActions.bold(),
                BuiltInEditorActions.italic());
    }

    private static List<EditorAction> mathToolbarActions() {
        return List.of(
                BuiltInEditorActions.undo(),
                BuiltInEditorActions.redo(),
                BuiltInEditorActions.insertFraction(),
                BuiltInEditorActions.insertRoot(),
                BuiltInEditorActions.insertParenthesesGroup(),
                BuiltInEditorActions.insertBracketsGroup(),
                BuiltInEditorActions.insertBracesGroup(),
                BuiltInEditorActions.insertSuperscript(),
                BuiltInEditorActions.insertSubscript(),
                BuiltInEditorActions.convertToNamedOperator(),
                BuiltInEditorActions.convertToMathText());
    }

    private static EditorAction action(List<EditorAction> actions, EditorActionId id) {
        return actions.stream()
                .filter(action -> action.id() == id)
                .findFirst()
                .orElseThrow();
    }
}
