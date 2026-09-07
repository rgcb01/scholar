package dev.rgcb.scholar.client.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.rgcb.scholar.editor.BuiltInEditorActions;
import dev.rgcb.scholar.editor.EditorAction;
import dev.rgcb.scholar.editor.EditorActionId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MenuDefinitionTest {
    @Test
    void editAndFormatMenusUseSharedActionIdsInOrder() {
        var editActions = BuiltInEditorActions.editMenuActions();
        var insertActions = BuiltInEditorActions.insertMenuActions();
        var formatActions = BuiltInEditorActions.formatMenuActions();
        var menus = List.of(
                new MenuDefinition("Edit", List.of(
                        MenuEntry.action(action(editActions, EditorActionId.UNDO)),
                        MenuEntry.action(action(editActions, EditorActionId.REDO)),
                        MenuEntry.separator(),
                        MenuEntry.action(action(editActions, EditorActionId.CUT)),
                        MenuEntry.action(action(editActions, EditorActionId.COPY)),
                        MenuEntry.action(action(editActions, EditorActionId.PASTE)))),
                new MenuDefinition("Insert", List.of(
                        MenuEntry.action(action(insertActions, EditorActionId.INSERT_EQUATION)),
                        MenuEntry.action(action(insertActions, EditorActionId.MATH_INSERT_FRACTION)),
                        MenuEntry.action(action(insertActions, EditorActionId.MATH_INSERT_ROOT)),
                        MenuEntry.action(action(insertActions, EditorActionId.MATH_INSERT_PARENTHESES_GROUP)),
                        MenuEntry.action(action(insertActions, EditorActionId.MATH_INSERT_BRACKETS_GROUP)),
                        MenuEntry.action(action(insertActions, EditorActionId.MATH_INSERT_BRACES_GROUP)),
                        MenuEntry.action(action(insertActions, EditorActionId.MATH_INSERT_SUPERSCRIPT)),
                        MenuEntry.action(action(insertActions, EditorActionId.MATH_INSERT_SUBSCRIPT)))),
                new MenuDefinition("Format", List.of(
                        MenuEntry.action(action(formatActions, EditorActionId.PARAGRAPH)),
                        MenuEntry.action(action(formatActions, EditorActionId.HEADING_1)),
                        MenuEntry.action(action(formatActions, EditorActionId.HEADING_2)),
                        MenuEntry.action(action(formatActions, EditorActionId.HEADING_3)),
                        MenuEntry.action(action(formatActions, EditorActionId.HEADING_4)),
                        MenuEntry.action(action(formatActions, EditorActionId.HEADING_5)),
                        MenuEntry.action(action(formatActions, EditorActionId.HEADING_6)),
                        MenuEntry.separator(),
                        MenuEntry.action(action(formatActions, EditorActionId.BOLD)),
                        MenuEntry.action(action(formatActions, EditorActionId.ITALIC)))));

        assertEquals(List.of("Edit", "Insert", "Format"), menus.stream().map(MenuDefinition::title).toList());
        assertEquals(EditorActionId.UNDO, menus.get(0).entries().get(0).action().orElseThrow().id());
        assertEquals(MenuEntryKind.SEPARATOR, menus.get(0).entries().get(2).kind());
        assertEquals(EditorActionId.INSERT_EQUATION, menus.get(1).entries().get(0).action().orElseThrow().id());
        assertEquals(EditorActionId.MATH_INSERT_FRACTION, menus.get(1).entries().get(1).action().orElseThrow().id());
        assertEquals(EditorActionId.MATH_INSERT_ROOT, menus.get(1).entries().get(2).action().orElseThrow().id());
        assertEquals(EditorActionId.MATH_INSERT_PARENTHESES_GROUP, menus.get(1).entries().get(3).action().orElseThrow().id());
        assertEquals(EditorActionId.MATH_INSERT_BRACKETS_GROUP, menus.get(1).entries().get(4).action().orElseThrow().id());
        assertEquals(EditorActionId.MATH_INSERT_BRACES_GROUP, menus.get(1).entries().get(5).action().orElseThrow().id());
        assertEquals(EditorActionId.MATH_INSERT_SUPERSCRIPT, menus.get(1).entries().get(6).action().orElseThrow().id());
        assertEquals(EditorActionId.MATH_INSERT_SUBSCRIPT, menus.get(1).entries().get(7).action().orElseThrow().id());
        assertEquals(EditorActionId.PARAGRAPH, menus.get(2).entries().get(0).action().orElseThrow().id());
        assertEquals(EditorActionId.HEADING_6, menus.get(2).entries().get(6).action().orElseThrow().id());
        assertEquals(MenuEntryKind.SEPARATOR, menus.get(2).entries().get(7).kind());
        assertEquals(EditorActionId.BOLD, menus.get(2).entries().get(8).action().orElseThrow().id());
        assertEquals(EditorActionId.ITALIC, menus.get(2).entries().get(9).action().orElseThrow().id());
    }

    @Test
    void rejectsInvalidMenuDefinitionsAndEntries() {
        assertThrows(IllegalArgumentException.class, () -> new MenuDefinition(" ", List.of(MenuEntry.separator())));
        assertThrows(IllegalArgumentException.class, () -> new MenuDefinition("Format", List.of()));
        assertThrows(IllegalArgumentException.class, () -> new MenuEntry(MenuEntryKind.ACTION, Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> new MenuEntry(MenuEntryKind.SEPARATOR, Optional.of(BuiltInEditorActions.bold())));
    }

    private static EditorAction action(List<EditorAction> actions, EditorActionId id) {
        return actions.stream()
                .filter(action -> action.id() == id)
                .findFirst()
                .orElseThrow();
    }
}
