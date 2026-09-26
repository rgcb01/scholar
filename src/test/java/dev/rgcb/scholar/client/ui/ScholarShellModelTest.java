package dev.rgcb.scholar.client.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.rgcb.scholar.editor.BuiltInEditorActions;
import dev.rgcb.scholar.editor.EditorAction;
import java.util.List;
import org.junit.jupiter.api.Test;

class ScholarShellModelTest {
    @Test void productionTabsAreCompactOrderedAndReferenceExistingActionInstances() {
        var file = List.<EditorAction>of(BuiltInEditorActions.editMenuActions().getFirst());
        var tabs = ScholarShellModel.production(file,
                BuiltInEditorActions.editMenuActions(), BuiltInEditorActions.formatMenuActions(),
                BuiltInEditorActions.insertMenuActions(), BuiltInEditorActions.dataMenuActions(),
                BuiltInEditorActions.tableMenuActions(), BuiltInEditorActions.plotMenuActions(),
                BuiltInEditorActions.figureMenuActions(), BuiltInEditorActions.diagramMenuActions(),
                BuiltInEditorActions.viewMenuActions());

        assertEquals(List.of("scholar.menu.file", "scholar.menu.home", "scholar.menu.insert",
                        "scholar.menu.data", "scholar.menu.figure", "scholar.menu.diagram", "scholar.menu.view"),
                tabs.stream().map(MenuDefinition::title).toList());
        assertEquals(file.getFirst(), tabs.getFirst().entries().getFirst().action().orElseThrow());
    }
}
