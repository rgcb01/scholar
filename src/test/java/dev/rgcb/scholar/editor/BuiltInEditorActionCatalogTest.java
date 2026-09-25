package dev.rgcb.scholar.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Arrays;
import java.util.HashSet;
import org.junit.jupiter.api.Test;

class BuiltInEditorActionCatalogTest {
    @Test void catalogIsTotalUniqueAndDeterministic() {
        var descriptors = BuiltInEditorActionCatalog.descriptors();
        assertEquals(EditorActionId.values().length, descriptors.size());
        assertEquals(EditorActionId.values().length,
                new HashSet<>(descriptors.stream().map(EditorActionDescriptor::id).toList()).size());
        assertEquals(EditorActionId.values().length,
                new HashSet<>(descriptors.stream().map(EditorActionDescriptor::translationKey).toList()).size());
        for (var id : EditorActionId.values()) {
            var descriptor = BuiltInEditorActionCatalog.require(id);
            assertSame(descriptor, BuiltInEditorActionCatalog.require(id));
            assertEquals(id, descriptor.id());
            assertFalse(descriptor.translationKey().isBlank());
            assertFalse(descriptor.englishLabel().isBlank());
            assertFalse(descriptor.englishTooltip().isBlank());
        }
    }

    @Test void builtInActionInstancesUseCatalogMetadata() {
        var actions = new java.util.ArrayList<EditorAction>();
        actions.addAll(BuiltInEditorActions.editMenuActions());
        actions.addAll(BuiltInEditorActions.insertMenuActions());
        actions.addAll(BuiltInEditorActions.formatMenuActions());
        actions.addAll(BuiltInEditorActions.layoutMenuActions());
        actions.addAll(BuiltInEditorActions.tableMenuActions());
        actions.addAll(BuiltInEditorActions.plotMenuActions());
        actions.addAll(BuiltInEditorActions.diagramMenuActions());
        actions.addAll(BuiltInEditorActions.figureMenuActions());
        actions.addAll(BuiltInEditorActions.dataMenuActions());
        actions.addAll(BuiltInEditorActions.viewMenuActions());
        actions.addAll(BuiltInEditorActions.blockStyleActions());

        for (var action : actions) {
            var descriptor = BuiltInEditorActionCatalog.require(action.id());
            assertEquals(descriptor.englishLabel(), action.label(), action.id().name());
            assertEquals(descriptor.englishTooltip(), action.tooltip(), action.id().name());
            assertEquals(descriptor.shortcut(), action.shortcut(), action.id().name());
        }
    }

    @Test void everyShortcutBearingActionUsesCatalogShortcutPolicy() {
        var shortcutIds = Arrays.stream(EditorActionId.values())
                .filter(id -> BuiltInEditorActionCatalog.require(id).shortcut().isPresent()).toList();
        assertEquals(java.util.Set.of(EditorActionId.FILE_NEW, EditorActionId.FILE_OPEN, EditorActionId.FILE_SAVE,
                EditorActionId.FILE_SAVE_AS, EditorActionId.UNDO, EditorActionId.REDO, EditorActionId.CUT,
                EditorActionId.COPY, EditorActionId.PASTE, EditorActionId.DELETE, EditorActionId.BOLD,
                EditorActionId.ITALIC), java.util.Set.copyOf(shortcutIds));
    }
}
