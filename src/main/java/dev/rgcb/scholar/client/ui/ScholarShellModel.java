package dev.rgcb.scholar.client.ui;

import dev.rgcb.scholar.editor.EditorAction;
import java.util.ArrayList;
import java.util.List;

/** Data-only production shell description assembled from authoritative actions. */
public final class ScholarShellModel {
    private ScholarShellModel() { }

    public static List<MenuDefinition> production(
            List<EditorAction> file,
            List<EditorAction> edit,
            List<EditorAction> format,
            List<EditorAction> insert,
            List<EditorAction> data,
            List<EditorAction> table,
            List<EditorAction> plot,
            List<EditorAction> figure,
            List<EditorAction> diagram,
            List<EditorAction> view
    ) {
        return List.of(
                menu("File", file),
                grouped("Home", edit, format),
                menu("Insert", insert),
                grouped("Data", data, table, plot),
                menu("Figure", figure),
                menu("Diagram", diagram),
                menu("View", view));
    }

    private static MenuDefinition menu(String title, List<EditorAction> actions) {
        return new MenuDefinition(title, actions.stream().map(MenuEntry::action).toList());
    }

    @SafeVarargs
    private static MenuDefinition grouped(String title, List<EditorAction>... groups) {
        var entries = new ArrayList<MenuEntry>();
        for (var group : groups) {
            if (group.isEmpty()) continue;
            if (!entries.isEmpty()) entries.add(MenuEntry.separator());
            group.stream().map(MenuEntry::action).forEach(entries::add);
        }
        return new MenuDefinition(title, entries);
    }
}
