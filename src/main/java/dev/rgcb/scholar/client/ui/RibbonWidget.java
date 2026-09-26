package dev.rgcb.scholar.client.ui;

import dev.rgcb.scholar.client.editor.ScholarEditorController;
import dev.rgcb.scholar.editor.ActionSelectionState;
import dev.rgcb.scholar.editor.EditorAction;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

/** Minecraft-native production ribbon backed exclusively by existing EditorActions. */
public final class RibbonWidget {
    public static final int TAB_HEIGHT = 20;
    public static final int COMMAND_HEIGHT = 58;
    private static final int POPUP_ROW_HEIGHT = 18;
    private static final int POPUP_WIDTH = 118;

    private final ScholarEditorController controller;
    private final List<RibbonTabDefinition> tabs;
    private ShellRect tabBounds = new ShellRect(0, 0, 0, 0);
    private ShellRect commandBounds = new ShellRect(0, 0, 0, 0);
    private int activeTab = 1;
    private int scrollOffset;
    private int tabScrollOffset;
    private int tabContentWidth;
    private int viewportHeight = Integer.MAX_VALUE;
    private int pressedCommand = -1;
    private int openDropdown = -1;
    private RibbonLayout.Result layout;
    private List<ShellRect> renderedTabs = List.of();

    public RibbonWidget(ScholarEditorController controller, List<RibbonTabDefinition> tabs) {
        this.controller = Objects.requireNonNull(controller, "controller");
        this.tabs = List.copyOf(Objects.requireNonNull(tabs, "tabs"));
        if (tabs.isEmpty()) throw new IllegalArgumentException("Ribbon requires at least one tab.");
        activeTab = Math.min(activeTab, tabs.size() - 1);
    }

    public void setBounds(ShellRect tabs, ShellRect commands) {
        tabBounds = Objects.requireNonNull(tabs, "tabs");
        commandBounds = Objects.requireNonNull(commands, "commands");
        recomputeLayout();
    }

    public void setViewportHeight(int viewportHeight) {
        this.viewportHeight = Math.max(0, viewportHeight);
    }

    public int activeTab() { return activeTab; }
    public List<RibbonTabDefinition> tabs() { return tabs; }
    public boolean isPopupOpen() { return openDropdown >= 0; }
    public void closePopup() { openDropdown = -1; pressedCommand = -1; }
    public boolean contains(double x, double y) { return tabBounds.contains(x, y) || commandBounds.contains(x, y); }

    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        renderTabs(graphics, font, mouseX, mouseY);
        ScholarShellRenderer.drawRaisedPanel(graphics, commandBounds.x(), commandBounds.y(),
                commandBounds.width(), commandBounds.height(), ScholarShellStyle.PANEL);
        graphics.enableScissor(commandBounds.x(), commandBounds.y(), commandBounds.right(), commandBounds.bottom());
        try {
            for (var group : layout.groups()) {
                var bounds = translated(group.bounds());
                if (bounds.right() < commandBounds.x() || bounds.x() > commandBounds.right()) continue;
                if (group.groupIndex() > 0) {
                    ScholarShellRenderer.drawVerticalSeparator(graphics, bounds.x(), bounds.y() + 2, 43);
                }
                var label = font.plainSubstrByWidth(ScholarText.get(group.label()), Math.max(8, bounds.width() - 6));
                graphics.drawCenteredString(font, label, bounds.x() + bounds.width() / 2,
                        commandBounds.bottom() - 10, 0xFFABB4C0);
            }
            for (var index = 0; index < layout.commands().size(); index++) {
                renderCommand(graphics, font, index, layout.commands().get(index), mouseX, mouseY);
            }
        } finally {
            graphics.disableScissor();
        }
        if (layout.contentWidth() > commandBounds.width()) renderOverflowHints(graphics);
        renderDropdown(graphics, font, mouseX, mouseY);
    }

    public void renderTooltip(GuiGraphics graphics, Font font, int mouseX, int mouseY, int viewportHeight) {
        if (isPopupOpen()) return;
        var hovered = commandAt(mouseX, mouseY);
        if (hovered < 0) return;
        var command = layout.commands().get(hovered).command();
        var action = presentedAction(command);
        var lines = new ArrayList<String>();
        lines.add(command.label(action, RibbonLabelMode.FULL));
        if (command.palette()) lines.add(ScholarText.get("scholar.tooltip.choose_command"));
        else if (!ScholarText.actionTooltipText(action).equals(ScholarText.actionLabelText(action)))
            lines.add(ScholarText.actionTooltipText(action));
        action.shortcut().ifPresent(shortcut -> lines.add(shortcut.displayText()));
        var width = lines.stream().mapToInt(font::width).max().orElse(40) + 10;
        var height = lines.size() * 11 + 6;
        var x = Math.max(0, Math.min(mouseX + 10, commandBounds.right() - width));
        var y = Math.max(0, Math.min(mouseY + 12, viewportHeight - height));
        graphics.fill(x, y, x + width, y + height, 0xF0100010);
        graphics.renderOutline(x, y, width, height, 0xFF6B7280);
        for (var index = 0; index < lines.size(); index++) {
            graphics.drawString(font, lines.get(index), x + 5, y + 4 + index * 11,
                    index == 0 ? ScholarShellStyle.TEXT : ScholarShellStyle.TEXT_DISABLED, false);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
        var popupChoice = popupChoiceAt(mouseX, mouseY);
        if (popupChoice >= 0) {
            var command = layout.commands().get(openDropdown).command();
            var action = command.choices().get(popupChoice);
            if (controller.isEnabled(action)) controller.execute(action);
            closePopup();
            return true;
        }
        if (openDropdown >= 0 && !contains(mouseX, mouseY)) {
            closePopup();
            return true;
        }
        for (var index = 0; index < renderedTabs.size(); index++) {
            if (renderedTabs.get(index).contains(mouseX, mouseY)) {
                activeTab = index;
                scrollOffset = 0;
                closePopup();
                recomputeLayout();
                return true;
            }
        }
        if (!commandBounds.contains(mouseX, mouseY)) return false;
        pressedCommand = commandAt(mouseX, mouseY);
        return true;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT || pressedCommand < 0) return false;
        var released = commandAt(mouseX, mouseY);
        var index = pressedCommand;
        pressedCommand = -1;
        if (released != index) return true;
        var command = layout.commands().get(index).command();
        if (command.dropdown()) {
            openDropdown = openDropdown == index ? -1 : index;
        } else if (controller.isEnabled(command.action())) {
            controller.execute(command.action());
        }
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (tabBounds.contains(mouseX, mouseY) && tabContentWidth > tabBounds.width()) {
            var max = Math.max(0, tabContentWidth - tabBounds.width() + 8);
            tabScrollOffset = Math.max(0, Math.min(max, tabScrollOffset - (int) Math.signum(scrollY) * 34));
            return true;
        }
        if (!commandBounds.contains(mouseX, mouseY) || layout.contentWidth() <= commandBounds.width()) return false;
        var max = Math.max(0, layout.contentWidth() - commandBounds.width() + 8);
        scrollOffset = Math.max(0, Math.min(max, scrollOffset - (int) Math.signum(scrollY) * 34));
        return true;
    }

    public static VisualState visualState(boolean enabled, ActionSelectionState selected, boolean hovered, boolean pressed) {
        if (!enabled) return VisualState.DISABLED;
        if (pressed) return VisualState.PRESSED;
        if (selected == ActionSelectionState.ON || selected == ActionSelectionState.MIXED) return VisualState.ACTIVE;
        if (hovered) return VisualState.HOVERED;
        return VisualState.NORMAL;
    }

    private void renderTabs(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        graphics.fill(tabBounds.x(), tabBounds.y(), tabBounds.right(), tabBounds.bottom(), ScholarShellStyle.PANEL_RECESSED);
        var result = new ArrayList<ShellRect>();
        var x = tabBounds.x() + 5 - tabScrollOffset;
        graphics.enableScissor(tabBounds.x(), tabBounds.y(), tabBounds.right(), tabBounds.bottom());
        try {
            for (var index = 0; index < tabs.size(); index++) {
                var tab = tabs.get(index);
                var width = font.width(ScholarText.get(tab.label())) + 18;
                var bounds = new ShellRect(x, tabBounds.y() + 1, width, tabBounds.height() - 1);
                result.add(bounds);
                var hovered = bounds.contains(mouseX, mouseY);
                if (index == activeTab) {
                    graphics.fill(bounds.x(), bounds.y(), bounds.right(), bounds.bottom(), ScholarShellStyle.PANEL);
                    graphics.fill(bounds.x() + 2, bounds.bottom() - 2, bounds.right() - 2, bounds.bottom(), 0xFF65B5D2);
                } else if (hovered) {
                    graphics.fill(bounds.x(), bounds.y(), bounds.right(), bounds.bottom(), ScholarShellStyle.HOVER);
                }
                graphics.drawCenteredString(font, ScholarText.get(tab.label()), bounds.x() + bounds.width() / 2, bounds.y() + 6,
                        ScholarShellStyle.TEXT);
                x += width + 2;
            }
        } finally {
            graphics.disableScissor();
        }
        tabContentWidth = x - tabBounds.x() + tabScrollOffset;
        renderedTabs = List.copyOf(result);
    }

    private void renderCommand(GuiGraphics graphics, Font font, int index, RibbonLayout.CommandBounds entry,
                               int mouseX, int mouseY) {
        var bounds = translated(entry.bounds());
        if (bounds.right() < commandBounds.x() || bounds.x() > commandBounds.right()) return;
        var action = presentedAction(entry.command());
        var enabled = entry.command().dropdown()
                ? entry.command().choices().stream().anyMatch(controller::isEnabled)
                : controller.isEnabled(action);
        var hovered = bounds.contains(mouseX, mouseY);
        var state = visualState(enabled, controller.selectionState(action), hovered, pressedCommand == index && hovered);
        renderSurface(graphics, bounds, state);
        var iconScale = entry.command().size() == RibbonCommandSize.LARGE ? 2 : 1;
        var icon = entry.command().icon();
        var labelVisible = entry.labelMode() != RibbonLabelMode.ICON_ONLY
                && entry.command().size() != RibbonCommandSize.SMALL;
        var iconX = entry.command().size() == RibbonCommandSize.LARGE
                ? bounds.x() + (bounds.width() - icon.width() * iconScale) / 2
                : bounds.x() + 5;
        var iconY = entry.command().size() == RibbonCommandSize.LARGE ? bounds.y() + 5 : bounds.y() + 6;
        icon.render(graphics, iconX, iconY, iconScale,
                enabled ? ScholarShellStyle.TEXT : ScholarShellStyle.TEXT_DISABLED);
        if (labelVisible) {
            if (entry.command().size() == RibbonCommandSize.LARGE) {
                var label = font.plainSubstrByWidth(entry.command().label(action, entry.labelMode()), bounds.width() - 4);
                graphics.drawCenteredString(font, label, bounds.x() + bounds.width() / 2, bounds.bottom() - 11,
                        enabled ? ScholarShellStyle.TEXT : ScholarShellStyle.TEXT_DISABLED);
            } else {
                var reserved = entry.command().dropdown() ? 12 : 5;
                var label = font.plainSubstrByWidth(entry.command().label(action, entry.labelMode()),
                        bounds.width() - 20 - reserved);
                graphics.drawString(font, label, bounds.x() + 17, bounds.y() + 6,
                        enabled ? ScholarShellStyle.TEXT : ScholarShellStyle.TEXT_DISABLED, false);
                if (entry.command().dropdown()) drawDropdownChevron(graphics, bounds.right() - 8, bounds.y() + 8,
                        enabled ? ScholarShellStyle.TEXT : ScholarShellStyle.TEXT_DISABLED);
            }
        }
    }

    private void renderSurface(GuiGraphics graphics, ShellRect bounds, VisualState state) {
        switch (state) {
            case NORMAL -> { }
            case HOVERED -> ScholarShellRenderer.drawRaisedPanel(graphics, bounds.x(), bounds.y(), bounds.width(), bounds.height(), ScholarShellStyle.HOVER);
            case PRESSED, ACTIVE -> ScholarShellRenderer.drawInsetPanel(graphics, bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                    state == VisualState.ACTIVE ? ScholarShellStyle.SELECTED : ScholarShellStyle.PRESSED);
            case DISABLED -> graphics.fill(bounds.x(), bounds.y(), bounds.right(), bounds.bottom(), 0x2220242A);
        }
    }

    private void renderDropdown(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        if (openDropdown < 0 || openDropdown >= layout.commands().size()) return;
        var commandBounds = translated(layout.commands().get(openDropdown).bounds());
        var choices = layout.commands().get(openDropdown).command().choices();
        var popupWidth = popupWidth(choices);
        var x = Math.max(0, Math.min(commandBounds.x(), this.commandBounds.right() - popupWidth));
        var height = choices.size() * POPUP_ROW_HEIGHT + 4;
        var y = popupY(height);
        ScholarShellRenderer.drawRaisedPanel(graphics, x, y, popupWidth, height, ScholarShellStyle.PANEL_RECESSED);
        for (var index = 0; index < choices.size(); index++) {
            var action = choices.get(index);
            var rowY = y + 2 + index * POPUP_ROW_HEIGHT;
            var enabled = controller.isEnabled(action);
            if (mouseX >= x + 2 && mouseX < x + popupWidth - 2 && mouseY >= rowY && mouseY < rowY + POPUP_ROW_HEIGHT) {
                graphics.fill(x + 2, rowY, x + popupWidth - 2, rowY + POPUP_ROW_HEIGHT, ScholarShellStyle.HOVER);
            }
            if (controller.selectionState(action) == ActionSelectionState.ON) {
                graphics.fill(x + 4, rowY + 5, x + 8, rowY + 13, 0xFF65B5D2);
            }
            graphics.drawString(font, ScholarText.actionLabel(action), x + 12, rowY + 5,
                    enabled ? ScholarShellStyle.TEXT : ScholarShellStyle.TEXT_DISABLED, false);
        }
    }

    private int popupChoiceAt(double mouseX, double mouseY) {
        if (openDropdown < 0 || openDropdown >= layout.commands().size()) return -1;
        var commandBounds = translated(layout.commands().get(openDropdown).bounds());
        var choices = layout.commands().get(openDropdown).command().choices();
        var count = choices.size();
        var popupWidth = popupWidth(choices);
        var x = Math.max(0, Math.min(commandBounds.x(), this.commandBounds.right() - popupWidth));
        var y = popupY(count * POPUP_ROW_HEIGHT + 4) + 2;
        if (mouseX < x || mouseX >= x + popupWidth || mouseY < y || mouseY >= y + count * POPUP_ROW_HEIGHT) return -1;
        return Math.min(count - 1, (int) (mouseY - y) / POPUP_ROW_HEIGHT);
    }

    private int commandAt(double mouseX, double mouseY) {
        if (!commandBounds.contains(mouseX, mouseY)) return -1;
        for (var index = 0; index < layout.commands().size(); index++) {
            if (translated(layout.commands().get(index).bounds()).contains(mouseX, mouseY)) return index;
        }
        return -1;
    }

    private ShellRect translated(ShellRect local) {
        return new ShellRect(commandBounds.x() + local.x() - scrollOffset,
                commandBounds.y() + local.y(), local.width(), local.height());
    }

    private void recomputeLayout() {
        layout = RibbonLayout.compute(tabs.get(activeTab), commandBounds.width());
        scrollOffset = Math.min(scrollOffset, Math.max(0, layout.contentWidth() - commandBounds.width() + 8));
    }

    private EditorAction presentedAction(RibbonCommandPresentation command) {
        if (!command.dropdown() || command.palette()) return command.action();
        return command.choices().stream()
                .filter(choice -> {
                    var state = controller.selectionState(choice);
                    return state == ActionSelectionState.ON || state == ActionSelectionState.MIXED;
                })
                .findFirst().orElse(command.action());
    }

    private void renderOverflowHints(GuiGraphics graphics) {
        if (scrollOffset > 0) graphics.fill(commandBounds.x(), commandBounds.y() + 3,
                commandBounds.x() + 2, commandBounds.bottom() - 11, 0xFF65B5D2);
        if (scrollOffset + commandBounds.width() < layout.contentWidth()) graphics.fill(commandBounds.right() - 2,
                commandBounds.y() + 3, commandBounds.right(), commandBounds.bottom() - 11, 0xFF65B5D2);
    }

    private static void drawDropdownChevron(GuiGraphics graphics, int x, int y, int color) {
        graphics.fill(x, y, x + 5, y + 1, color);
        graphics.fill(x + 1, y + 1, x + 4, y + 2, color);
        graphics.fill(x + 2, y + 2, x + 3, y + 3, color);
    }

    private int popupY(int popupHeight) {
        return Math.max(0, Math.min(commandBounds.bottom() - 1, viewportHeight - popupHeight));
    }

    private static int popupWidth(List<EditorAction> choices) {
        var labelWidth = choices.stream().mapToInt(action -> RibbonLayout.estimatedTextWidth(
                ScholarText.actionLabelText(action))).max().orElse(0);
        return Math.max(POPUP_WIDTH, Math.min(220, labelWidth + 24));
    }

    public enum VisualState { NORMAL, HOVERED, PRESSED, ACTIVE, DISABLED }
}
