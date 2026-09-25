package dev.rgcb.scholar.client.ui;

import dev.rgcb.scholar.client.editor.ScholarEditorController;
import dev.rgcb.scholar.editor.ActionSelectionState;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

public final class MenuBarWidget {
    public static final int HEIGHT = 20;

    private static final int TITLE_X = 4;
    private static final int TITLE_GAP = 3;
    private static final int TITLE_Y = 3;
    private static final int ROW_HEIGHT = 18;
    private static final int MENU_WIDTH = 174;
    private static final int MENU_Y = HEIGHT;
    private static final int LABEL_X = 18;
    private static final int STATE_MARK_X = 9;
    private static final int SHORTCUT_RIGHT_PAD = 10;

    private final ScholarEditorController controller;
    private final List<MenuDefinition> menus;
    private int openMenuIndex = -1;
    private int viewportHeight = Integer.MAX_VALUE;
    private int viewportWidth = Integer.MAX_VALUE;
    private int keyboardEntry = -1;
    private int titleScroll;
    private int scrollRow;

    public MenuBarWidget(ScholarEditorController controller, List<MenuDefinition> menus) {
        this.controller = Objects.requireNonNull(controller, "controller");
        this.menus = List.copyOf(menus);
    }

    public void render(GuiGraphics graphics, Font font, int width, int height, int mouseX, int mouseY) {
        setViewportSize(width, height);
        ScholarShellRenderer.drawRaisedPanel(graphics, 0, 0, width, HEIGHT, ScholarShellStyle.PANEL);
        graphics.fill(0, HEIGHT - 2, width, HEIGHT - 1, ScholarShellStyle.SHADOW);
        graphics.fill(0, HEIGHT - 1, width, HEIGHT, ScholarShellStyle.DEEP_SHADOW);

        graphics.enableScissor(0, 0, width, HEIGHT);
        try {
            for (var index = 0; index < menus.size(); index++) {
                renderTitle(graphics, font, index, mouseX, mouseY);
            }
        } finally {
            graphics.disableScissor();
        }

        if (openMenuIndex >= 0) {
            renderDropdown(graphics, font, mouseX, mouseY);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY) {
        var titleIndex = titleIndexAt(mouseX, mouseY);
        if (titleIndex >= 0) {
            var next = openMenuIndex == titleIndex ? -1 : titleIndex;
            if (next != openMenuIndex) {
                scrollRow = 0;
                keyboardEntry = -1;
            }
            openMenuIndex = next;
            return true;
        }

        if (openMenuIndex < 0) {
            return false;
        }

        var itemIndex = itemIndexAt(mouseX, mouseY);
        if (itemIndex >= 0) {
            var entry = openMenu().entries().get(itemIndex);
            if (entry.kind() == MenuEntryKind.ACTION) {
                var action = entry.action().orElseThrow();
                if (controller.isEnabled(action)) {
                    controller.execute(action);
                    openMenuIndex = -1;
                }
            }
            return true;
        }

        openMenuIndex = -1;
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (mouseX >= 0 && mouseX < viewportWidth && mouseY >= 0 && mouseY < HEIGHT && scrollY != 0.0) {
            var old = titleScroll;
            titleScroll = Math.max(0, Math.min(maxTitleScroll(), titleScroll - (int) Math.signum(scrollY) * 24));
            return old != titleScroll || maxTitleScroll() > 0;
        }
        if (openMenuIndex < 0 || scrollY == 0.0) {
            return false;
        }
        var menuX = menuX(openMenuIndex);
        var height = visibleDropdownHeight();
        if (mouseX < menuX || mouseX >= menuX + MENU_WIDTH
                || mouseY < MENU_Y || mouseY >= MENU_Y + height) {
            return false;
        }
        var old = scrollRow;
        scrollRow -= (int) Math.signum(scrollY);
        clampScrollRow();
        return scrollRow != old || maxScrollRow() > 0;
    }

    public boolean keyPressed(int keyCode) {
        if (openMenuIndex < 0) { return false; }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && openMenuIndex >= 0) {
            close();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_RIGHT) {
            openMenuIndex = Math.floorMod(openMenuIndex + (keyCode == GLFW.GLFW_KEY_RIGHT ? 1 : -1), menus.size());
            keyboardEntry = -1;
            scrollRow = 0;
            var x = titleX(openMenuIndex);
            if (x < 0) { titleScroll += x; }
            else if (x + titleWidth(openMenu()) > viewportWidth) { titleScroll += x + titleWidth(openMenu()) - viewportWidth; }
            titleScroll = Math.max(0, Math.min(titleScroll, maxTitleScroll()));
        } else if (keyCode == GLFW.GLFW_KEY_DOWN || keyCode == GLFW.GLFW_KEY_UP) {
            selectKeyboardEntry(keyCode == GLFW.GLFW_KEY_DOWN ? 1 : -1);
        } else if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            if (keyboardEntry < 0) { selectKeyboardEntry(1); }
            if (keyboardEntry >= 0) {
                var action = openMenu().entries().get(keyboardEntry).action().orElseThrow();
                if (controller.isEnabled(action)) {
                    close();
                    controller.execute(action);
                }
            }
        }
        return true;
    }

    public void setViewportSize(int width, int height) {
        viewportWidth = Math.max(1, width);
        viewportHeight = Math.max(MENU_Y + ROW_HEIGHT, height);
        titleScroll = Math.max(0, Math.min(titleScroll, maxTitleScroll()));
        clampScrollRow();
    }

    private void selectKeyboardEntry(int direction) {
        var entries = openMenu().entries();
        var index = keyboardEntry < 0 ? (direction > 0 ? -1 : 0) : keyboardEntry;
        for (var i = 0; i < entries.size(); i++) {
            index = Math.floorMod(index + direction, entries.size());
            var entry = entries.get(index);
            if (entry.action().isPresent() && controller.isEnabled(entry.action().orElseThrow())) {
                keyboardEntry = index;
                if (index < scrollRow) { scrollRow = index; }
                if (index >= scrollRow + visibleRowCount()) { scrollRow = index - visibleRowCount() + 1; }
                clampScrollRow();
                return;
            }
        }
    }

    public boolean isOpen() {
        return openMenuIndex >= 0;
    }

    public void close() {
        openMenuIndex = -1;
        scrollRow = 0;
        keyboardEntry = -1;
    }

    private void renderDropdown(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        var height = visibleDropdownHeight();
        var menuX = menuX(openMenuIndex);
        ScholarShellRenderer.drawRaisedPanel(graphics, menuX, MENU_Y, MENU_WIDTH, height, ScholarShellStyle.PANEL_RECESSED);
        graphics.fill(menuX + 3, MENU_Y + 3, menuX + MENU_WIDTH - 3, MENU_Y + height - 3, ScholarShellStyle.PANEL_INSET);

        var visibleRows = visibleRowCount();
        var endRow = Math.min(openMenu().entries().size(), scrollRow + visibleRows);
        for (var index = scrollRow; index < endRow; index++) {
            var entry = openMenu().entries().get(index);
            var y = rowY(index - scrollRow);
            if (entry.kind() == MenuEntryKind.SEPARATOR) {
                renderSeparator(graphics, menuX, y + ROW_HEIGHT / 2);
                continue;
            }

            var action = entry.action().orElseThrow();
            var enabled = controller.isEnabled(action);
            if ((itemIndexAt(mouseX, mouseY) == index || keyboardEntry == index) && enabled) {
                renderHoveredRow(graphics, menuX, y);
            }
            renderActionState(graphics, menuX, y, controller.selectionState(action));
            var color = enabled ? ScholarShellStyle.TEXT : ScholarShellStyle.TEXT_DISABLED;
            graphics.drawString(font, ScholarText.actionLabel(action), menuX + LABEL_X, y + 5, color, false);
            action.shortcut().ifPresent(shortcut -> {
                var shortcutText = shortcut.displayText();
                graphics.drawString(
                        font,
                        shortcutText,
                        menuX + MENU_WIDTH - SHORTCUT_RIGHT_PAD - font.width(shortcutText),
                        y + 5,
                        color,
                        false);
            });
        }

        // Small Minecraft-native affordances make overflow discoverable without
        // introducing a second scrollbar widget into the menu chrome.
        if (scrollRow > 0) {
            graphics.drawString(font, "▲", menuX + MENU_WIDTH - 12, MENU_Y + 4, ScholarShellStyle.TEXT_DISABLED, false);
        }
        if (scrollRow < maxScrollRow()) {
            graphics.drawString(font, "▼", menuX + MENU_WIDTH - 12, MENU_Y + height - 12, ScholarShellStyle.TEXT_DISABLED, false);
        }
    }

    private void renderTitle(GuiGraphics graphics, Font font, int index, int mouseX, int mouseY) {
        var x = titleX(index);
        var titleWidth = titleWidth(menus.get(index));
        var hovered = isInsideTitle(index, mouseX, mouseY);
        if (openMenuIndex == index) {
            ScholarShellRenderer.drawInsetPanel(graphics, x, TITLE_Y, titleWidth, HEIGHT - TITLE_Y - 3, ScholarShellStyle.PRESSED);
        } else if (hovered) {
            ScholarShellRenderer.drawRaisedPanel(graphics, x, TITLE_Y, titleWidth, HEIGHT - TITLE_Y - 3, ScholarShellStyle.PANEL_RAISED);
        }
        var textColor = openMenuIndex == index ? 0xFFFFFFFF : ScholarShellStyle.TEXT;
        graphics.drawString(font, ScholarText.component(menus.get(index).title()), x + 8, TITLE_Y + 3, textColor, false);
    }

    private static void renderHoveredRow(GuiGraphics graphics, int menuX, int y) {
        var x = menuX + 4;
        var width = MENU_WIDTH - 8;
        graphics.fill(x, y + 1, x + width, y + ROW_HEIGHT - 1, ScholarShellStyle.HOVER);
        graphics.fill(x, y + 1, x + width, y + 2, ScholarShellStyle.HOVER_TOP);
        graphics.fill(x, y + ROW_HEIGHT - 2, x + width, y + ROW_HEIGHT - 1, ScholarShellStyle.SHADOW);
    }

    private static void renderActionState(GuiGraphics graphics, int menuX, int y, ActionSelectionState state) {
        if (state == ActionSelectionState.ON) {
            graphics.fill(menuX + STATE_MARK_X, y + 5, menuX + STATE_MARK_X + 5, y + 10, ScholarShellStyle.TEXT);
        } else if (state == ActionSelectionState.MIXED) {
            graphics.fill(menuX + STATE_MARK_X, y + 7, menuX + STATE_MARK_X + 5, y + 9, ScholarShellStyle.TEXT_DISABLED);
        }
    }

    private static void renderSeparator(GuiGraphics graphics, int menuX, int y) {
        ScholarShellRenderer.drawHorizontalSeparator(graphics, menuX + 7, y, MENU_WIDTH - 14);
    }

    private int titleIndexAt(double mouseX, double mouseY) {
        for (var index = 0; index < menus.size(); index++) {
            if (isInsideTitle(index, mouseX, mouseY)) {
                return index;
            }
        }
        return -1;
    }

    private boolean isInsideTitle(int index, double mouseX, double mouseY) {
        var x = titleX(index);
        return mouseX >= x && mouseX < x + titleWidth(menus.get(index))
                && mouseY >= TITLE_Y && mouseY < HEIGHT - 2;
    }

    private int itemIndexAt(double mouseX, double mouseY) {
        if (openMenuIndex < 0) {
            return -1;
        }
        var menuX = menuX(openMenuIndex);
        var height = visibleDropdownHeight();
        if (mouseX < menuX || mouseX >= menuX + MENU_WIDTH || mouseY < MENU_Y || mouseY >= MENU_Y + height) {
            return -1;
        }
        var visibleRow = ((int) mouseY - MENU_Y) / ROW_HEIGHT;
        var index = scrollRow + visibleRow;
        if (index < 0 || index >= openMenu().entries().size()) {
            return -1;
        }
        return openMenu().entries().get(index).kind() == MenuEntryKind.ACTION ? index : -1;
    }

    private int visibleRowCount() {
        var available = Math.max(ROW_HEIGHT, viewportHeight - MENU_Y - 4);
        return Math.max(1, Math.min(openMenu().entries().size(), available / ROW_HEIGHT));
    }

    private int visibleDropdownHeight() {
        return visibleRowCount() * ROW_HEIGHT;
    }

    private int maxScrollRow() {
        return Math.max(0, openMenu().entries().size() - visibleRowCount());
    }

    private void clampScrollRow() {
        if (openMenuIndex < 0) {
            scrollRow = 0;
            return;
        }
        scrollRow = Math.max(0, Math.min(scrollRow, maxScrollRow()));
    }

    private static int rowY(int row) {
        return MENU_Y + row * ROW_HEIGHT;
    }

    private MenuDefinition openMenu() {
        return menus.get(openMenuIndex);
    }

    private int menuX(int index) {
        return Math.max(0, Math.min(titleX(index), viewportWidth - MENU_WIDTH));
    }

    private int titleX(int index) {
        var x = TITLE_X - titleScroll;
        for (var current = 0; current < index; current++) {
            x += titleWidth(menus.get(current)) + TITLE_GAP;
        }
        return x;
    }

    private int maxTitleScroll() {
        return Math.max(0, TITLE_X + menus.stream().mapToInt(menu -> titleWidth(menu) + TITLE_GAP).sum() - viewportWidth);
    }

    private static int titleWidth(MenuDefinition menu) {
        return Math.max(42, ScholarTranslations.get(menu.title()).length() * 6 + 16);
    }

    private static int dropdownHeight(MenuDefinition menu) {
        return menu.entries().size() * ROW_HEIGHT;
    }
}
