package dev.rgcb.scholar.client.ui;

import dev.rgcb.scholar.client.editor.ScholarEditorController;
import dev.rgcb.scholar.editor.ActionSelectionState;
import dev.rgcb.scholar.editor.BlockStyleSelectionState;
import dev.rgcb.scholar.editor.EditorAction;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

public final class ToolbarWidget {
    public static final int HEIGHT = 24;

    private static final int TOOLTIP_BACKGROUND = 0xF0100010;
    private static final int TOOLTIP_BORDER = 0xFF6B7280;
    private static final int TOOLTIP_TEXT = 0xFFFFFFFF;
    private static final int POPUP_ROW_HEIGHT = 18;
    private static final int POPUP_WIDTH = ToolbarLayout.BLOCK_STYLE_WIDTH + 28;
    private static final int GROUP_POPUP_WIDTH = 104;
    private static final int CONVERT_POPUP_WIDTH = 126;

    private final ScholarEditorController controller;
    private final List<ToolbarItem> items;
    private final List<EditorAction> blockStyleActions;
    private final List<EditorAction> groupActions;
    private final List<EditorAction> semanticConversionActions;
    private ShellRect bounds = new ShellRect(0, 0, 0, 0);
    private List<ToolbarItemBounds> itemBounds = List.of();
    private int pressedItemIndex = -1;
    private boolean blockStyleOpen;
    private boolean groupOpen;
    private boolean semanticConvertOpen;
    private int viewportHeight = Integer.MAX_VALUE;

    public void setViewportHeight(int height) { viewportHeight = Math.max(0, height); }

    private int popupX(ToolbarItemBounds control, int popupWidth) {
        return Math.max(0, Math.min(bounds.x() + control.x(), bounds.right() - popupWidth));
    }

    private int popupY(ToolbarItemBounds control, int rows) {
        return Math.max(0, Math.min(bounds.y() + control.y() + control.height() + 2,
                viewportHeight - rows * POPUP_ROW_HEIGHT));
    }

    public ToolbarWidget(ScholarEditorController controller, List<ToolbarItem> items) {
        this(controller, items, List.of());
    }

    public ToolbarWidget(ScholarEditorController controller, List<ToolbarItem> items, List<EditorAction> blockStyleActions) {
        this(controller, items, blockStyleActions, List.of());
    }

    public ToolbarWidget(
            ScholarEditorController controller,
            List<ToolbarItem> items,
            List<EditorAction> blockStyleActions,
            List<EditorAction> semanticConversionActions
    ) {
        this(controller, items, blockStyleActions, List.of(), semanticConversionActions);
    }

    public ToolbarWidget(
            ScholarEditorController controller,
            List<ToolbarItem> items,
            List<EditorAction> blockStyleActions,
            List<EditorAction> groupActions,
            List<EditorAction> semanticConversionActions
    ) {
        this.controller = Objects.requireNonNull(controller, "controller");
        this.items = List.copyOf(items);
        this.blockStyleActions = List.copyOf(blockStyleActions);
        this.groupActions = List.copyOf(groupActions);
        this.semanticConversionActions = List.copyOf(semanticConversionActions);
    }

    public void setBounds(ShellRect bounds) {
        this.bounds = Objects.requireNonNull(bounds, "bounds");
    }

    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        ScholarShellRenderer.drawRaisedPanel(graphics, bounds.x(), bounds.y(), bounds.width(), bounds.height(), ScholarShellStyle.PANEL);
        graphics.fill(bounds.x(), bounds.bottom() - 1, bounds.right(), bounds.bottom(), ScholarShellStyle.DEEP_SHADOW);

        itemBounds = ToolbarLayout.compute(items, bounds.width(), action -> font.width(ScholarText.actionLabel(action)));
        for (var itemBound : itemBounds) {
            var item = items.get(itemBound.itemIndex());
            var x = bounds.x() + itemBound.x();
            var y = bounds.y() + itemBound.y();
            if (item.kind() == ToolbarItemKind.SEPARATOR) {
                ScholarShellRenderer.drawVerticalSeparator(graphics, x + itemBound.width() / 2, y, itemBound.height());
                continue;
            }
            if (item.kind() == ToolbarItemKind.BLOCK_STYLE) {
                renderBlockStyleControl(graphics, font, itemBound, mouseX, mouseY, x, y);
                continue;
            }
            if (item.kind() == ToolbarItemKind.GROUP) {
                renderGroupControl(graphics, font, itemBound, mouseX, mouseY, x, y);
                continue;
            }
            if (item.kind() == ToolbarItemKind.SEMANTIC_CONVERT) {
                renderSemanticConvertControl(graphics, font, itemBound, mouseX, mouseY, x, y);
                continue;
            }
            var action = item.action().orElseThrow();
            var enabled = controller.isEnabled(action);
            var selectionState = controller.selectionState(action);
            var hovered = itemBound.contains(mouseX - bounds.x(), mouseY - bounds.y());
            var pressed = pressedItemIndex == itemBound.itemIndex() && hovered && enabled;
            renderButton(graphics, font, action, enabled, selectionState, hovered, pressed, x, y, itemBound.width(), itemBound.height());
        }
    }

    public void renderPopup(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        if (!blockStyleOpen) {
            renderGroupPopup(graphics, font, mouseX, mouseY);
            renderSemanticConvertPopup(graphics, font, mouseX, mouseY);
            return;
        }
        var control = blockStyleBounds();
        if (control == null) {
            blockStyleOpen = false;
            return;
        }
        var popupX = popupX(control, POPUP_WIDTH);
        var popupY = popupY(control, blockStyleActions.size());
        var popupHeight = blockStyleActions.size() * POPUP_ROW_HEIGHT;
        ScholarShellRenderer.drawRaisedPanel(graphics, popupX, popupY, POPUP_WIDTH, popupHeight, ScholarShellStyle.PANEL_RECESSED);
        graphics.fill(popupX + 3, popupY + 3, popupX + POPUP_WIDTH - 3, popupY + popupHeight - 3, ScholarShellStyle.PANEL_INSET);

        for (var index = 0; index < blockStyleActions.size(); index++) {
            var action = blockStyleActions.get(index);
            var y = popupY + index * POPUP_ROW_HEIGHT;
            var enabled = controller.isEnabled(action);
            var hovered = mouseX >= popupX && mouseX < popupX + POPUP_WIDTH
                    && mouseY >= y && mouseY < y + POPUP_ROW_HEIGHT;
            if (hovered && enabled) {
                graphics.fill(popupX + 4, y + 1, popupX + POPUP_WIDTH - 4, y + POPUP_ROW_HEIGHT - 1, ScholarShellStyle.HOVER);
                graphics.fill(popupX + 4, y + 1, popupX + POPUP_WIDTH - 4, y + 2, ScholarShellStyle.HOVER_TOP);
            }
            renderPopupState(graphics, popupX, y, controller.selectionState(action));
            graphics.drawString(
                    font,
                    ScholarText.actionLabelText(action),
                    popupX + 18,
                    y + 5,
                    enabled ? ScholarShellStyle.TEXT : ScholarShellStyle.TEXT_DISABLED,
                    false);
        }
        renderGroupPopup(graphics, font, mouseX, mouseY);
        renderSemanticConvertPopup(graphics, font, mouseX, mouseY);
    }

    public void renderTooltip(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        var hovered = hoveredAction(mouseX, mouseY);
        if (hovered == null) {
            return;
        }

        var firstLine = hovered.tooltip();
        var secondLine = hovered.shortcut().map(shortcut -> shortcut.displayText()).orElse("");
        var textWidth = secondLine.isEmpty()
                ? font.width(firstLine)
                : Math.max(font.width(firstLine), font.width(secondLine));
        var tooltipWidth = textWidth + 10;
        var tooltipHeight = secondLine.isEmpty() ? 17 : 28;
        var x = Math.max(0, Math.min(mouseX + 10, bounds.right() - tooltipWidth));
        var y = Math.max(0, Math.min(mouseY + 12, viewportHeight - tooltipHeight));
        graphics.fill(x, y, x + tooltipWidth, y + tooltipHeight, TOOLTIP_BACKGROUND);
        graphics.renderOutline(x, y, tooltipWidth, tooltipHeight, TOOLTIP_BORDER);
        graphics.drawString(font, firstLine, x + 5, y + 5, TOOLTIP_TEXT, false);
        if (!secondLine.isEmpty()) {
            graphics.drawString(font, secondLine, x + 5, y + 16, ScholarShellStyle.TEXT_DISABLED, false);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY) {
        if (semanticConvertOpen) {
            var popupItem = semanticConvertPopupIndexAt(mouseX, mouseY);
            if (popupItem >= 0) {
                var action = semanticConversionActions.get(popupItem);
                if (controller.isEnabled(action)) {
                    controller.execute(action);
                    semanticConvertOpen = false;
                }
                return true;
            }
            if (!contains(mouseX, mouseY)) {
                semanticConvertOpen = false;
                return true;
            }
        }
        if (blockStyleOpen) {
            var popupItem = blockStylePopupIndexAt(mouseX, mouseY);
            if (popupItem >= 0) {
                var action = blockStyleActions.get(popupItem);
                if (controller.isEnabled(action)) {
                    controller.execute(action);
                    blockStyleOpen = false;
                }
                return true;
            }
            if (!contains(mouseX, mouseY)) {
                blockStyleOpen = false;
                return true;
            }
        }
        if (groupOpen) {
            var popupItem = groupPopupIndexAt(mouseX, mouseY);
            if (popupItem >= 0) {
                var action = groupActions.get(popupItem);
                if (controller.isEnabled(action)) {
                    controller.execute(action);
                    groupOpen = false;
                }
                return true;
            }
            if (!contains(mouseX, mouseY)) {
                groupOpen = false;
                return true;
            }
        }
        if (!contains(mouseX, mouseY)) {
            return false;
        }
        var blockStyleIndex = blockStyleItemIndexAt(mouseX, mouseY);
        if (blockStyleIndex != -1) {
            if (controller.blockStyleSelectionState().kind() != BlockStyleSelectionState.Kind.NOT_APPLICABLE) {
                blockStyleOpen = !blockStyleOpen;
                groupOpen = false;
                semanticConvertOpen = false;
            }
            return true;
        }
        var groupIndex = groupItemIndexAt(mouseX, mouseY);
        if (groupIndex != -1) {
            if (groupActions.stream().anyMatch(controller::isEnabled)) {
                groupOpen = !groupOpen;
                blockStyleOpen = false;
                semanticConvertOpen = false;
            }
            return true;
        }
        var semanticConvertIndex = semanticConvertItemIndexAt(mouseX, mouseY);
        if (semanticConvertIndex != -1) {
            if (semanticConversionActions.stream().anyMatch(controller::isEnabled)) {
                semanticConvertOpen = !semanticConvertOpen;
                blockStyleOpen = false;
                groupOpen = false;
            }
            return true;
        }
        blockStyleOpen = false;
        groupOpen = false;
        semanticConvertOpen = false;
        pressedItemIndex = actionItemIndexAt(mouseX, mouseY);
        return true;
    }

    public boolean mouseReleased(double mouseX, double mouseY) {
        if (pressedItemIndex == -1) {
            return contains(mouseX, mouseY);
        }

        var releasedItemIndex = actionItemIndexAt(mouseX, mouseY);
        if (releasedItemIndex == pressedItemIndex) {
            var action = items.get(pressedItemIndex).action().orElseThrow();
            if (controller.isEnabled(action)) {
                controller.execute(action);
            }
        }
        pressedItemIndex = -1;
        return true;
    }

    public boolean mouseDragged(double mouseX, double mouseY) {
        return pressedItemIndex != -1 || contains(mouseX, mouseY);
    }

    public boolean keyPressed(int keyCode) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && blockStyleOpen) {
            blockStyleOpen = false;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && groupOpen) {
            groupOpen = false;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && semanticConvertOpen) {
            semanticConvertOpen = false;
            return true;
        }
        return false;
    }

    public boolean isPopupOpen() {
        return blockStyleOpen || groupOpen || semanticConvertOpen;
    }

    public void closePopup() {
        blockStyleOpen = false;
        groupOpen = false;
        semanticConvertOpen = false;
    }

    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= bounds.x() && mouseX < bounds.right()
                && mouseY >= bounds.y() && mouseY < bounds.bottom();
    }

    private void renderButton(
            GuiGraphics graphics,
            Font font,
            EditorAction action,
            boolean enabled,
            ActionSelectionState selectionState,
            boolean hovered,
            boolean pressed,
            int x,
            int y,
            int width,
            int height
    ) {
        if (pressed) {
            ScholarShellRenderer.drawInsetPanel(graphics, x, y, width, height, ScholarShellStyle.PRESSED);
        } else if (enabled && selectionState == ActionSelectionState.ON) {
            ScholarShellRenderer.drawInsetPanel(graphics, x, y, width, height, ScholarShellStyle.SELECTED);
        } else if (enabled && selectionState == ActionSelectionState.MIXED) {
            ScholarShellRenderer.drawRaisedPanel(graphics, x, y, width, height, ScholarShellStyle.MIXED);
            graphics.fill(x + 3, y + height - 4, x + width - 3, y + height - 3, ScholarShellStyle.TEXT_DISABLED);
        } else {
            var fill = hovered && enabled ? ScholarShellStyle.PANEL_RAISED : ScholarShellStyle.PANEL;
            ScholarShellRenderer.drawRaisedPanel(graphics, x, y, width, height, fill);
            if (hovered && enabled) {
                graphics.fill(x + 2, y + 2, x + width - 2, y + 3, ScholarShellStyle.HOVER_TOP);
            }
        }
        var color = enabled ? ScholarShellStyle.TEXT : ScholarShellStyle.TEXT_DISABLED;
        var textX = x + Math.max(4, (width - font.width(ScholarText.actionLabel(action))) / 2);
        var textY = y + 5 + (pressed ? 1 : 0);
        graphics.drawString(font, ScholarText.actionLabel(action), textX, textY, color, false);
    }

    private void renderBlockStyleControl(
            GuiGraphics graphics,
            Font font,
            ToolbarItemBounds itemBound,
            int mouseX,
            int mouseY,
            int x,
            int y
    ) {
        var styleState = controller.blockStyleSelectionState();
        var enabled = styleState.kind() != BlockStyleSelectionState.Kind.NOT_APPLICABLE;
        var hovered = itemBound.contains(mouseX - bounds.x(), mouseY - bounds.y());
        var label = switch (styleState.kind()) {
            case SINGLE -> styleState.style().orElseThrow().displayName();
            case MIXED -> ScholarText.get("scholar.status.mixed");
            case NOT_APPLICABLE -> ScholarText.get("scholar.ribbon.group.style");
        };
        if (blockStyleOpen && enabled) {
            ScholarShellRenderer.drawInsetPanel(graphics, x, y, itemBound.width(), itemBound.height(), ScholarShellStyle.PRESSED);
        } else {
            var fill = hovered && enabled ? ScholarShellStyle.PANEL_RAISED : ScholarShellStyle.PANEL;
            ScholarShellRenderer.drawRaisedPanel(graphics, x, y, itemBound.width(), itemBound.height(), fill);
        }
        graphics.drawString(font, label, x + 6, y + 5, enabled ? ScholarShellStyle.TEXT : ScholarShellStyle.TEXT_DISABLED, false);
        renderTriangle(graphics, x + itemBound.width() - 12, y + 7, enabled ? ScholarShellStyle.TEXT : ScholarShellStyle.TEXT_DISABLED);
    }

    private void renderSemanticConvertControl(
            GuiGraphics graphics,
            Font font,
            ToolbarItemBounds itemBound,
            int mouseX,
            int mouseY,
            int x,
            int y
    ) {
        var enabled = semanticConversionActions.stream().anyMatch(controller::isEnabled);
        var hovered = itemBound.contains(mouseX - bounds.x(), mouseY - bounds.y());
        if (semanticConvertOpen && enabled) {
            ScholarShellRenderer.drawInsetPanel(graphics, x, y, itemBound.width(), itemBound.height(), ScholarShellStyle.PRESSED);
        } else {
            var fill = hovered && enabled ? ScholarShellStyle.PANEL_RAISED : ScholarShellStyle.PANEL;
            ScholarShellRenderer.drawRaisedPanel(graphics, x, y, itemBound.width(), itemBound.height(), fill);
        }
        graphics.drawString(font, ScholarText.get("scholar.ribbon.short.convert"), x + 6, y + 5,
                enabled ? ScholarShellStyle.TEXT : ScholarShellStyle.TEXT_DISABLED, false);
        renderTriangle(graphics, x + itemBound.width() - 12, y + 7, enabled ? ScholarShellStyle.TEXT : ScholarShellStyle.TEXT_DISABLED);
    }

    private void renderGroupControl(
            GuiGraphics graphics,
            Font font,
            ToolbarItemBounds itemBound,
            int mouseX,
            int mouseY,
            int x,
            int y
    ) {
        var enabled = groupActions.stream().anyMatch(controller::isEnabled);
        var hovered = itemBound.contains(mouseX - bounds.x(), mouseY - bounds.y());
        if (groupOpen && enabled) {
            ScholarShellRenderer.drawInsetPanel(graphics, x, y, itemBound.width(), itemBound.height(), ScholarShellStyle.PRESSED);
        } else {
            var fill = hovered && enabled ? ScholarShellStyle.PANEL_RAISED : ScholarShellStyle.PANEL;
            ScholarShellRenderer.drawRaisedPanel(graphics, x, y, itemBound.width(), itemBound.height(), fill);
        }
        graphics.drawString(font, ScholarText.get("scholar.ribbon.short.group"), x + 6, y + 5,
                enabled ? ScholarShellStyle.TEXT : ScholarShellStyle.TEXT_DISABLED, false);
        renderTriangle(graphics, x + itemBound.width() - 12, y + 7, enabled ? ScholarShellStyle.TEXT : ScholarShellStyle.TEXT_DISABLED);
    }

    private void renderGroupPopup(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        if (!groupOpen) {
            return;
        }
        var control = groupBounds();
        if (control == null) {
            groupOpen = false;
            return;
        }
        var popupX = popupX(control, GROUP_POPUP_WIDTH);
        var popupY = popupY(control, groupActions.size());
        var popupHeight = groupActions.size() * POPUP_ROW_HEIGHT;
        ScholarShellRenderer.drawRaisedPanel(graphics, popupX, popupY, GROUP_POPUP_WIDTH, popupHeight, ScholarShellStyle.PANEL_RECESSED);
        graphics.fill(popupX + 3, popupY + 3, popupX + GROUP_POPUP_WIDTH - 3, popupY + popupHeight - 3, ScholarShellStyle.PANEL_INSET);
        for (var index = 0; index < groupActions.size(); index++) {
            var action = groupActions.get(index);
            var y = popupY + index * POPUP_ROW_HEIGHT;
            var enabled = controller.isEnabled(action);
            var hovered = mouseX >= popupX && mouseX < popupX + GROUP_POPUP_WIDTH
                    && mouseY >= y && mouseY < y + POPUP_ROW_HEIGHT;
            if (hovered && enabled) {
                graphics.fill(popupX + 4, y + 1, popupX + GROUP_POPUP_WIDTH - 4, y + POPUP_ROW_HEIGHT - 1, ScholarShellStyle.HOVER);
                graphics.fill(popupX + 4, y + 1, popupX + GROUP_POPUP_WIDTH - 4, y + 2, ScholarShellStyle.HOVER_TOP);
            }
            graphics.drawString(
                    font,
                    ScholarText.actionLabelText(action),
                    popupX + 8,
                    y + 5,
                    enabled ? ScholarShellStyle.TEXT : ScholarShellStyle.TEXT_DISABLED,
                    false);
        }
    }

    private void renderSemanticConvertPopup(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        if (!semanticConvertOpen) {
            return;
        }
        var control = semanticConvertBounds();
        if (control == null) {
            semanticConvertOpen = false;
            return;
        }
        var popupX = popupX(control, CONVERT_POPUP_WIDTH);
        var popupY = popupY(control, semanticConversionActions.size());
        var popupHeight = semanticConversionActions.size() * POPUP_ROW_HEIGHT;
        ScholarShellRenderer.drawRaisedPanel(graphics, popupX, popupY, CONVERT_POPUP_WIDTH, popupHeight, ScholarShellStyle.PANEL_RECESSED);
        graphics.fill(popupX + 3, popupY + 3, popupX + CONVERT_POPUP_WIDTH - 3, popupY + popupHeight - 3, ScholarShellStyle.PANEL_INSET);
        for (var index = 0; index < semanticConversionActions.size(); index++) {
            var action = semanticConversionActions.get(index);
            var y = popupY + index * POPUP_ROW_HEIGHT;
            var enabled = controller.isEnabled(action);
            var hovered = mouseX >= popupX && mouseX < popupX + CONVERT_POPUP_WIDTH
                    && mouseY >= y && mouseY < y + POPUP_ROW_HEIGHT;
            if (hovered && enabled) {
                graphics.fill(popupX + 4, y + 1, popupX + CONVERT_POPUP_WIDTH - 4, y + POPUP_ROW_HEIGHT - 1, ScholarShellStyle.HOVER);
                graphics.fill(popupX + 4, y + 1, popupX + CONVERT_POPUP_WIDTH - 4, y + 2, ScholarShellStyle.HOVER_TOP);
            }
            graphics.drawString(
                    font,
                    ScholarText.actionLabelText(action),
                    popupX + 8,
                    y + 5,
                    enabled ? ScholarShellStyle.TEXT : ScholarShellStyle.TEXT_DISABLED,
                    false);
        }
    }

    private static void renderTriangle(GuiGraphics graphics, int x, int y, int color) {
        graphics.fill(x, y, x + 5, y + 1, color);
        graphics.fill(x + 1, y + 1, x + 4, y + 2, color);
        graphics.fill(x + 2, y + 2, x + 3, y + 3, color);
    }

    private static void renderPopupState(GuiGraphics graphics, int popupX, int y, ActionSelectionState state) {
        if (state == ActionSelectionState.ON) {
            graphics.fill(popupX + 9, y + 5, popupX + 14, y + 10, ScholarShellStyle.TEXT);
        }
    }

    private EditorAction hoveredAction(int mouseX, int mouseY) {
        var itemIndex = actionItemIndexAt(mouseX, mouseY);
        if (itemIndex == -1) {
            return null;
        }
        return items.get(itemIndex).action().orElseThrow();
    }

    private int actionItemIndexAt(double mouseX, double mouseY) {
        var localX = mouseX - bounds.x();
        var localY = mouseY - bounds.y();
        for (var itemBound : itemBounds) {
            if (itemBound.kind() == ToolbarItemKind.ACTION && itemBound.contains(localX, localY)) {
                return itemBound.itemIndex();
            }
        }
        return -1;
    }

    private int blockStyleItemIndexAt(double mouseX, double mouseY) {
        var localX = mouseX - bounds.x();
        var localY = mouseY - bounds.y();
        for (var itemBound : itemBounds) {
            if (itemBound.kind() == ToolbarItemKind.BLOCK_STYLE && itemBound.contains(localX, localY)) {
                return itemBound.itemIndex();
            }
        }
        return -1;
    }

    private int semanticConvertItemIndexAt(double mouseX, double mouseY) {
        var localX = mouseX - bounds.x();
        var localY = mouseY - bounds.y();
        for (var itemBound : itemBounds) {
            if (itemBound.kind() == ToolbarItemKind.SEMANTIC_CONVERT && itemBound.contains(localX, localY)) {
                return itemBound.itemIndex();
            }
        }
        return -1;
    }

    private int groupItemIndexAt(double mouseX, double mouseY) {
        var localX = mouseX - bounds.x();
        var localY = mouseY - bounds.y();
        for (var itemBound : itemBounds) {
            if (itemBound.kind() == ToolbarItemKind.GROUP && itemBound.contains(localX, localY)) {
                return itemBound.itemIndex();
            }
        }
        return -1;
    }

    private int blockStylePopupIndexAt(double mouseX, double mouseY) {
        var control = blockStyleBounds();
        if (control == null) {
            return -1;
        }
        var popupX = popupX(control, POPUP_WIDTH);
        var popupY = popupY(control, blockStyleActions.size());
        if (mouseX < popupX || mouseX >= popupX + POPUP_WIDTH || mouseY < popupY || mouseY >= popupY + blockStyleActions.size() * POPUP_ROW_HEIGHT) {
            return -1;
        }
        var index = ((int) mouseY - popupY) / POPUP_ROW_HEIGHT;
        return index >= 0 && index < blockStyleActions.size() ? index : -1;
    }

    private int semanticConvertPopupIndexAt(double mouseX, double mouseY) {
        var control = semanticConvertBounds();
        if (control == null) {
            return -1;
        }
        var popupX = popupX(control, CONVERT_POPUP_WIDTH);
        var popupY = popupY(control, semanticConversionActions.size());
        if (mouseX < popupX || mouseX >= popupX + CONVERT_POPUP_WIDTH
                || mouseY < popupY || mouseY >= popupY + semanticConversionActions.size() * POPUP_ROW_HEIGHT) {
            return -1;
        }
        var index = ((int) mouseY - popupY) / POPUP_ROW_HEIGHT;
        return index >= 0 && index < semanticConversionActions.size() ? index : -1;
    }

    private int groupPopupIndexAt(double mouseX, double mouseY) {
        var control = groupBounds();
        if (control == null) {
            return -1;
        }
        var popupX = popupX(control, GROUP_POPUP_WIDTH);
        var popupY = popupY(control, groupActions.size());
        if (mouseX < popupX || mouseX >= popupX + GROUP_POPUP_WIDTH
                || mouseY < popupY || mouseY >= popupY + groupActions.size() * POPUP_ROW_HEIGHT) {
            return -1;
        }
        var index = ((int) mouseY - popupY) / POPUP_ROW_HEIGHT;
        return index >= 0 && index < groupActions.size() ? index : -1;
    }

    private ToolbarItemBounds blockStyleBounds() {
        for (var itemBound : itemBounds) {
            if (itemBound.kind() == ToolbarItemKind.BLOCK_STYLE) {
                return itemBound;
            }
        }
        return null;
    }

    private ToolbarItemBounds semanticConvertBounds() {
        for (var itemBound : itemBounds) {
            if (itemBound.kind() == ToolbarItemKind.SEMANTIC_CONVERT) {
                return itemBound;
            }
        }
        return null;
    }

    private ToolbarItemBounds groupBounds() {
        for (var itemBound : itemBounds) {
            if (itemBound.kind() == ToolbarItemKind.GROUP) {
                return itemBound;
            }
        }
        return null;
    }
}
