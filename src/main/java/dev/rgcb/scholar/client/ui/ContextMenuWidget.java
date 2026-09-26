package dev.rgcb.scholar.client.ui;

import dev.rgcb.scholar.client.editor.ScholarEditorController;
import dev.rgcb.scholar.editor.ActionSelectionState;
import dev.rgcb.scholar.editor.ContextMenuEntry;
import dev.rgcb.scholar.editor.ContextMenuEntryKind;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

public final class ContextMenuWidget {
    private static final int LABEL_X = 18;
    private static final int STATE_MARK_X = 9;
    private static final int SHORTCUT_RIGHT_PAD = 10;

    private final ScholarEditorController controller;
    private final List<ContextMenuEntry> entries;
    private final int pointerX;
    private final int pointerY;
    private ShellRect bounds;
    private int viewportWidth = Integer.MAX_VALUE;
    private int viewportHeight = Integer.MAX_VALUE;
    private int scrollRow;
    private int highlightedRow = -1;

    public ContextMenuWidget(ScholarEditorController controller, List<ContextMenuEntry> entries, int pointerX, int pointerY) {
        this.controller = Objects.requireNonNull(controller, "controller");
        this.entries = List.copyOf(Objects.requireNonNull(entries, "entries"));
        this.pointerX = pointerX;
        this.pointerY = pointerY;
        bounds = ContextMenuLayout.compute(pointerX, pointerY, Integer.MAX_VALUE, Integer.MAX_VALUE, entries.size());
    }

    public void render(GuiGraphics graphics, Font font, int width, int height, int mouseX, int mouseY) {
        viewportWidth = Math.max(1, width);
        viewportHeight = Math.max(1, height);
        bounds = ContextMenuLayout.compute(pointerX, pointerY, viewportWidth, viewportHeight, entries.size());
        clampScrollRow();
        highlightedRow = itemIndexAt(mouseX, mouseY);
        ScholarShellRenderer.drawRaisedPanel(graphics, bounds.x(), bounds.y(), bounds.width(), bounds.height(), ScholarShellStyle.PANEL_RECESSED);
        graphics.fill(bounds.x() + 3, bounds.y() + 3, bounds.right() - 3, bounds.bottom() - 3, ScholarShellStyle.PANEL_INSET);

        var visibleRows = visibleRowCount();
        var endRow = Math.min(entries.size(), scrollRow + visibleRows);
        for (var index = scrollRow; index < endRow; index++) {
            var entry = entries.get(index);
            var y = rowY(index - scrollRow);
            if (entry.kind() == ContextMenuEntryKind.SEPARATOR) {
                ScholarShellRenderer.drawHorizontalSeparator(graphics, bounds.x() + 7, y + ContextMenuLayout.ROW_HEIGHT / 2, bounds.width() - 14);
                continue;
            }
            var action = entry.action().orElseThrow();
            var enabled = controller.isEnabled(action);
            if (highlightedRow == index && enabled) {
                renderHoveredRow(graphics, y);
            }
            renderActionState(graphics, y, controller.selectionState(action));
            var color = enabled ? ScholarShellStyle.TEXT : ScholarShellStyle.TEXT_DISABLED;
            graphics.drawString(font, ScholarText.actionLabel(action), bounds.x() + LABEL_X, y + 5, color, false);
            action.shortcut().ifPresent(shortcut -> {
                var shortcutText = shortcut.displayText();
                graphics.drawString(
                        font,
                        shortcutText,
                        bounds.right() - SHORTCUT_RIGHT_PAD - font.width(shortcutText),
                        y + 5,
                        color,
                        false);
            });
        }
        if (scrollRow > 0) {
            graphics.drawString(font, "^", bounds.right() - 12, bounds.y() + 4, ScholarShellStyle.TEXT_DISABLED, false);
        }
        if (scrollRow < maxScrollRow()) {
            graphics.drawString(font, "v", bounds.right() - 12, bounds.bottom() - 12, ScholarShellStyle.TEXT_DISABLED, false);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY) {
        var itemIndex = itemIndexAt(mouseX, mouseY);
        if (itemIndex < 0) {
            return false;
        }
        var entry = entries.get(itemIndex);
        if (entry.kind() == ContextMenuEntryKind.ACTION) {
            var action = entry.action().orElseThrow();
            if (controller.isEnabled(action)) {
                controller.execute(action);
            }
        }
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (!contains(mouseX, mouseY) || scrollY == 0.0) {
            return false;
        }
        var old = scrollRow;
        scrollRow -= (int) Math.signum(scrollY);
        clampScrollRow();
        return scrollRow != old || maxScrollRow() > 0;
    }

    public boolean keyPressed(int keyCode) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DOWN) {
            moveHighlight(1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_UP) {
            moveHighlight(-1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            if (highlightedRow >= 0 && highlightedRow < entries.size()) {
                var entry = entries.get(highlightedRow);
                if (entry.kind() == ContextMenuEntryKind.ACTION) {
                    var action = entry.action().orElseThrow();
                    if (controller.isEnabled(action)) {
                        controller.execute(action);
                    }
                }
            }
            return true;
        }
        return false;
    }

    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= bounds.x() && mouseX < bounds.right() && mouseY >= bounds.y() && mouseY < bounds.bottom();
    }

    public ShellRect bounds() {
        return bounds;
    }

    private void moveHighlight(int direction) {
        if (entries.isEmpty()) {
            highlightedRow = -1;
            return;
        }
        var current = highlightedRow < 0 ? (direction > 0 ? -1 : entries.size()) : highlightedRow;
        for (var step = 0; step < entries.size(); step++) {
            current += direction;
            if (current < 0) {
                current = entries.size() - 1;
            } else if (current >= entries.size()) {
                current = 0;
            }
            if (entries.get(current).kind() == ContextMenuEntryKind.ACTION) {
                highlightedRow = current;
                scrollToHighlight();
                return;
            }
        }
    }

    private int itemIndexAt(double mouseX, double mouseY) {
        if (!contains(mouseX, mouseY)) {
            return -1;
        }
        var visibleRow = ((int) mouseY - bounds.y()) / ContextMenuLayout.ROW_HEIGHT;
        var index = scrollRow + visibleRow;
        if (index < 0 || index >= entries.size()) {
            return -1;
        }
        return entries.get(index).kind() == ContextMenuEntryKind.ACTION ? index : -1;
    }

    private void renderHoveredRow(GuiGraphics graphics, int y) {
        var x = bounds.x() + 4;
        var width = bounds.width() - 8;
        graphics.fill(x, y + 1, x + width, y + ContextMenuLayout.ROW_HEIGHT - 1, ScholarShellStyle.HOVER);
        graphics.fill(x, y + 1, x + width, y + 2, ScholarShellStyle.HOVER_TOP);
        graphics.fill(x, y + ContextMenuLayout.ROW_HEIGHT - 2, x + width, y + ContextMenuLayout.ROW_HEIGHT - 1, ScholarShellStyle.SHADOW);
    }

    private void renderActionState(GuiGraphics graphics, int y, ActionSelectionState state) {
        if (state == ActionSelectionState.ON) {
            graphics.fill(bounds.x() + STATE_MARK_X, y + 5, bounds.x() + STATE_MARK_X + 5, y + 10, ScholarShellStyle.TEXT);
        } else if (state == ActionSelectionState.MIXED) {
            graphics.fill(bounds.x() + STATE_MARK_X, y + 7, bounds.x() + STATE_MARK_X + 5, y + 9, ScholarShellStyle.TEXT_DISABLED);
        }
    }

    private void scrollToHighlight() {
        if (highlightedRow < scrollRow) {
            scrollRow = highlightedRow;
        } else if (highlightedRow >= scrollRow + visibleRowCount()) {
            scrollRow = highlightedRow - visibleRowCount() + 1;
        }
        clampScrollRow();
    }

    private int visibleRowCount() {
        return ContextMenuLayout.visibleRowCount(viewportHeight, entries.size());
    }

    private int maxScrollRow() {
        return Math.max(0, entries.size() - visibleRowCount());
    }

    private void clampScrollRow() {
        scrollRow = Math.max(0, Math.min(scrollRow, maxScrollRow()));
    }

    private int rowY(int row) {
        return bounds.y() + row * ContextMenuLayout.ROW_HEIGHT;
    }
}
