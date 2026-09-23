package dev.rgcb.scholar.client.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.editor.ActionSelectionState;
import dev.rgcb.scholar.editor.ActionShortcut;
import dev.rgcb.scholar.editor.BuiltInEditorActions;
import dev.rgcb.scholar.editor.EditorAction;
import dev.rgcb.scholar.editor.EditorActionContext;
import dev.rgcb.scholar.editor.EditorActionId;
import dev.rgcb.scholar.editor.EditorActionResult;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ScholarRibbonPresentationTest {
    @Test void everyBuiltInCommandHasARegisteredPixelIcon() {
        for (var id : EditorActionId.values()) {
            var icon = ScholarIcons.forAction(id);
            assertNotNull(icon, id.name());
            assertTrue(icon.width() > 0 && icon.height() > 0, id.name());
            assertTrue(icon.pixels().stream().anyMatch(row -> row.contains("#")), id.name());
        }
    }

    @Test void supportsLargeMediumSmallAndDropdownPresentations() {
        var action = action(EditorActionId.PASTE, "Paste", ActionShortcut.ctrl(ActionShortcut.Key.V));
        var large = RibbonCommandPresentation.action(action, RibbonCommandSize.LARGE);
        var medium = RibbonCommandPresentation.action(action, RibbonCommandSize.MEDIUM);
        var small = RibbonCommandPresentation.action(action, RibbonCommandSize.SMALL);
        var dropdown = RibbonCommandPresentation.dropdown(action, List.of(action));

        assertEquals(RibbonCommandSize.LARGE, large.size());
        assertEquals(RibbonCommandSize.MEDIUM, medium.size());
        assertEquals(RibbonCommandSize.SMALL, small.size());
        assertTrue(dropdown.dropdown());
        assertTrue(large.tooltipText().contains(action.shortcut().orElseThrow().displayText()));
    }

    @Test void visualStatesRepresentToggleHoverPressAndDisabledWithoutTextColorAlone() {
        assertEquals(RibbonWidget.VisualState.NORMAL, RibbonWidget.visualState(true, ActionSelectionState.OFF, false, false));
        assertEquals(RibbonWidget.VisualState.HOVERED, RibbonWidget.visualState(true, ActionSelectionState.OFF, true, false));
        assertEquals(RibbonWidget.VisualState.PRESSED, RibbonWidget.visualState(true, ActionSelectionState.OFF, true, true));
        assertEquals(RibbonWidget.VisualState.ACTIVE, RibbonWidget.visualState(true, ActionSelectionState.ON, false, false));
        assertEquals(RibbonWidget.VisualState.DISABLED, RibbonWidget.visualState(false, ActionSelectionState.ON, true, true));
    }

    @Test void responsiveLayoutUsesFullThenShortThenIconLabelsWithoutDroppingCommands() {
        var commands = Arrays.stream(EditorActionId.values()).limit(6)
                .map(id -> RibbonCommandPresentation.action(action(id, "Long Command Name", null),
                        RibbonCommandSize.MEDIUM, "Command")).toList();
        var tab = new RibbonTabDefinition("Data", List.of(new RibbonGroupDefinition("Commands", commands)));

        var wide = RibbonLayout.compute(tab, 1200);
        var shortened = RibbonLayout.compute(tab, 260);
        var narrow = RibbonLayout.compute(tab, 80);

        assertEquals(RibbonLabelMode.FULL, wide.labelMode());
        assertEquals(RibbonLabelMode.SHORT, shortened.labelMode());
        assertEquals(RibbonLabelMode.ICON_ONLY, narrow.labelMode());
        assertEquals(commands.size(), narrow.commands().size());
        assertTrue(wide.commands().getFirst().bounds().width()
                >= RibbonLayout.estimatedTextWidth("Long Command Name") + 28);
    }

    @Test void productionModelProvidesIntentionalTabsGroupsAndCommandHierarchy() {
        var tabs = productionTabs(BuiltInEditorActions.viewMenuActions());

        assertEquals(List.of("File", "Home", "Insert", "Data", "Figure", "Diagram", "Layout", "View"),
                tabs.stream().map(RibbonTabDefinition::label).toList());
        var home = tabs.get(1);
        assertEquals(List.of("Clipboard", "History", "Style", "Font", "Paragraph"),
                home.groups().stream().map(RibbonGroupDefinition::label).toList());
        assertTrue(home.groups().stream().flatMap(group -> group.commands().stream())
                .anyMatch(command -> command.size() == RibbonCommandSize.LARGE));
        assertTrue(home.groups().stream().flatMap(group -> group.commands().stream())
                .anyMatch(RibbonCommandPresentation::dropdown));
    }

    @Test void productionNeverExposesAnEmptyViewTab() {
        var withoutView = productionTabs(List.of());
        var withView = productionTabs(BuiltInEditorActions.viewMenuActions());

        assertFalse(withoutView.stream().anyMatch(tab -> tab.label().equals("View")));
        assertTrue(withView.stream().filter(tab -> tab.label().equals("View"))
                .allMatch(tab -> tab.groups().stream().anyMatch(group -> !group.commands().isEmpty())));
        assertTrue(withView.stream().allMatch(tab -> tab.groups().stream().anyMatch(group -> !group.commands().isEmpty())));
    }

    @Test void dataTabGroupsAnalysisCommands() {
        var data = tab(productionTabs(BuiltInEditorActions.viewMenuActions()), "Data");
        var analysis = data.groups().stream().filter(group -> group.label().equals("Analysis")).findFirst().orElseThrow();
        assertEquals(List.of(EditorActionId.DATA_INSERT_ANALYSIS, EditorActionId.DATA_EDIT_ANALYSIS,
                EditorActionId.DATA_ADD_FIT_OVERLAY), analysis.commands().stream().map(command -> command.action().id()).toList());
    }

    @Test void homeUsesReadableValueControlsAndDistinctScientificFormattingIcons() {
        var home = tab(productionTabs(BuiltInEditorActions.viewMenuActions()), "Home");
        var commands = home.groups().stream().flatMap(group -> group.commands().stream()).toList();
        assertTrue(commands.stream().anyMatch(command -> command.label(command.action(), RibbonLabelMode.FULL)
                .startsWith("Style: ")));
        assertTrue(commands.stream().anyMatch(command -> command.label(command.action(), RibbonLabelMode.FULL)
                .startsWith("Font: ")));
        assertTrue(commands.stream().anyMatch(command -> command.label(command.action(), RibbonLabelMode.FULL)
                .startsWith("Size: ")));
        var valueControls = commands.stream().filter(command -> command.valuePrefix() != null).toList();
        assertTrue(valueControls.stream().flatMap(command -> command.choices().stream()
                .map(choice -> command.label(choice, RibbonLabelMode.FULL))).anyMatch("Style: Affiliation"::equals));
        assertTrue(valueControls.stream().flatMap(command -> command.choices().stream()
                .map(choice -> command.label(choice, RibbonLabelMode.FULL))).anyMatch("Font: Scientific Math"::equals));
        assertTrue(valueControls.stream().flatMap(command -> command.choices().stream()
                .map(choice -> command.label(choice, RibbonLabelMode.FULL))).anyMatch("Size: 14 pt"::equals));
        assertEquals(5, Set.of(ScholarIcons.BOLD, ScholarIcons.ITALIC, ScholarIcons.UNDERLINE,
                ScholarIcons.SUBSCRIPT, ScholarIcons.SUPERSCRIPT).size());
        assertEquals(4, Set.of(ScholarIcons.ALIGN_LEFT, ScholarIcons.ALIGN_CENTER,
                ScholarIcons.ALIGN_RIGHT, ScholarIcons.ALIGN_JUSTIFY).size());
        assertNotEquals(ScholarIcons.LINE_SPACING, ScholarIcons.INDENT_DECREASE);
        assertNotEquals(ScholarIcons.INDENT_DECREASE, ScholarIcons.INDENT_INCREASE);
    }

    @Test void viewCommandsHaveDistinctPurposeSpecificIcons() {
        assertEquals(5, Set.of(ScholarIcons.OUTLINE, ScholarIcons.ZOOM_OUT, ScholarIcons.ZOOM_IN,
                ScholarIcons.FIT_PAGE, ScholarIcons.FIT_WIDTH).size());
        assertEquals(ScholarIcons.OUTLINE, ScholarIcons.forAction(EditorActionId.TOGGLE_OUTLINE));
        assertEquals(ScholarIcons.ZOOM_OUT, ScholarIcons.forAction(EditorActionId.VIEW_ZOOM_OUT));
        assertEquals(ScholarIcons.ZOOM_IN, ScholarIcons.forAction(EditorActionId.VIEW_ZOOM_IN));
        assertEquals(ScholarIcons.FIT_PAGE, ScholarIcons.forAction(EditorActionId.VIEW_FIT_PAGE));
        assertEquals(ScholarIcons.FIT_WIDTH, ScholarIcons.forAction(EditorActionId.VIEW_FIT_WIDTH));
    }

    @Test void diagramUsesPurposePalettesAndPreservesEveryActionExactlyOnce() {
        var tabs = productionTabs(BuiltInEditorActions.viewMenuActions());
        var diagram = tabs.stream().filter(tab -> tab.label().equals("Diagram")).findFirst().orElseThrow();

        assertEquals(List.of("Create", "Electrical", "Mechanical", "Workspace"),
                diagram.groups().stream().map(RibbonGroupDefinition::label).toList());
        assertTrue(diagram.groups().get(1).commands().stream().allMatch(RibbonCommandPresentation::palette));
        assertTrue(diagram.groups().get(2).commands().stream().allMatch(RibbonCommandPresentation::palette));
        assertTrue(diagram.groups().get(1).commands().stream().allMatch(command -> command.icon() == ScholarIcons.ELECTRICAL));
        assertTrue(diagram.groups().get(2).commands().stream().allMatch(command -> command.icon() == ScholarIcons.MECHANICAL));

        var paletteIds = diagram.groups().stream().flatMap(group -> group.commands().stream())
                .flatMap(command -> command.choices().stream()).map(EditorAction::id).toList();
        var expected = BuiltInEditorActions.diagramMenuActions().stream().map(EditorAction::id).collect(Collectors.toSet());
        assertEquals(expected, Set.copyOf(paletteIds));
        assertEquals(expected.size(), paletteIds.size());
    }

    @Test void wideFileInsertAndDataLayoutsKeepImportantLabelsInFull() {
        var tabs = productionTabs(BuiltInEditorActions.viewMenuActions());
        for (var tabName : List.of("File", "Insert", "Data")) {
            assertEquals(RibbonLabelMode.FULL,
                    RibbonLayout.compute(tab(tabs, tabName), 1600).labelMode(), tabName);
        }
        assertTrue(labels(tab(tabs, "File")).containsAll(List.of("New", "Open / Home", "Save", "Save As...", "Rename...", "Close")));
        assertTrue(labels(tab(tabs, "Insert")).containsAll(List.of("New Dataset", "Cross Reference", "Table of Contents")));
        assertTrue(labels(tab(tabs, "Data")).contains("New Dataset"));
    }

    @Test void scientificUnitsUseCompactContextualPalettes() {
        var tabs = productionTabs(BuiltInEditorActions.viewMenuActions());
        var insert = tab(tabs, "Insert");
        var data = tab(tabs, "Data");
        var quantity = insert.groups().stream().flatMap(group -> group.commands().stream())
                .filter(command -> "Quantity".equals(command.labelOverride())).findFirst().orElseThrow();
        var columnUnit = data.groups().stream().flatMap(group -> group.commands().stream())
                .filter(command -> "Column Unit".equals(command.labelOverride())).findFirst().orElseThrow();
        var xAxisUnit = data.groups().stream().flatMap(group -> group.commands().stream())
                .filter(command -> "X Axis Unit".equals(command.labelOverride())).findFirst().orElseThrow();
        var yAxisUnit = data.groups().stream().flatMap(group -> group.commands().stream())
                .filter(command -> "Y Axis Unit".equals(command.labelOverride())).findFirst().orElseThrow();
        assertTrue(quantity.palette());
        assertEquals(ScholarIcons.UNIT, quantity.icon());
        assertTrue(quantity.choices().stream().map(EditorAction::id).toList().contains(EditorActionId.INSERT_QUANTITY_ACCELERATION));
        assertTrue(quantity.choices().stream().map(EditorAction::id).toList().contains(EditorActionId.INSERT_QUANTITY_CELSIUS_DIFFERENCE));
        assertTrue(columnUnit.choices().stream().map(EditorAction::id).toList().contains(EditorActionId.DATA_COLUMN_UNIT_CELSIUS));
        assertTrue(columnUnit.choices().stream().map(EditorAction::id).toList().contains(EditorActionId.DATA_COLUMN_UNIT_CELSIUS_DIFFERENCE));
        assertTrue(xAxisUnit.choices().stream().map(EditorAction::id).toList().contains(EditorActionId.PLOT_X_UNIT_SECOND));
        assertTrue(yAxisUnit.choices().stream().map(EditorAction::id).toList().contains(EditorActionId.PLOT_Y_UNIT_KELVIN));
        assertTrue(yAxisUnit.choices().stream().map(EditorAction::id).toList().contains(EditorActionId.PLOT_Y_UNIT_KELVIN_DIFFERENCE));
    }

    private static List<RibbonTabDefinition> productionTabs(List<EditorAction> view) {
        var file = List.of(
                action(EditorActionId.FILE_NEW, "New", null), action(EditorActionId.FILE_OPEN, "Open / Home", null),
                action(EditorActionId.FILE_SAVE, "Save", null), action(EditorActionId.FILE_SAVE_AS, "Save As...", null),
                action(EditorActionId.FILE_RENAME, "Rename...", null), action(EditorActionId.FILE_CLOSE, "Close", null));
        return ScholarRibbonModel.production(file, BuiltInEditorActions.editMenuActions(),
                BuiltInEditorActions.formatMenuActions(), BuiltInEditorActions.blockStyleActions(),
                BuiltInEditorActions.insertMenuActions(), BuiltInEditorActions.dataMenuActions(),
                BuiltInEditorActions.tableMenuActions(), BuiltInEditorActions.plotMenuActions(),
                BuiltInEditorActions.figureMenuActions(), BuiltInEditorActions.diagramMenuActions(), view);
    }

    private static RibbonTabDefinition tab(List<RibbonTabDefinition> tabs, String label) {
        return tabs.stream().filter(tab -> tab.label().equals(label)).findFirst().orElseThrow();
    }

    private static Set<String> labels(RibbonTabDefinition tab) {
        return tab.groups().stream().flatMap(group -> group.commands().stream())
                .map(command -> command.label(command.action(), RibbonLabelMode.FULL)).collect(Collectors.toSet());
    }

    private static EditorAction action(EditorActionId id, String label, ActionShortcut shortcut) {
        return new EditorAction() {
            public EditorActionId id() { return id; }
            public String label() { return label; }
            public Optional<ActionShortcut> shortcut() { return Optional.ofNullable(shortcut); }
            public boolean isEnabled(EditorActionContext context) { return true; }
            public EditorActionResult execute(EditorActionContext context) { return EditorActionResult.NONE; }
        };
    }
}
