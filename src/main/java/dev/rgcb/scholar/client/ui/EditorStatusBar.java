package dev.rgcb.scholar.client.ui;

import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.editor.EditorActionId;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

/** Owns status presentation and zoom interaction; document state remains session-owned. */
public final class EditorStatusBar {
    private final Consumer<EditorActionId> actionExecutor;
    private final Consumer<Float> zoomSetter;
    private final Supplier<Float> zoomSupplier;
    private Document countedDocument;
    private int wordCount;
    private boolean sliderDragging;

    public EditorStatusBar(
            Consumer<EditorActionId> actionExecutor,
            Consumer<Float> zoomSetter,
            Supplier<Float> zoomSupplier
    ) {
        this.actionExecutor = Objects.requireNonNull(actionExecutor, "actionExecutor");
        this.zoomSetter = Objects.requireNonNull(zoomSetter, "zoomSetter");
        this.zoomSupplier = Objects.requireNonNull(zoomSupplier, "zoomSupplier");
    }

    public void render(
            GuiGraphics graphics,
            Font font,
            ShellRect bar,
            int screenWidth,
            int mouseX,
            int mouseY,
            Document document,
            int currentPage,
            int pageCount
    ) {
        if (bar.height() < 18) return;
        ScholarShellRenderer.drawRaisedPanel(graphics, bar.x(), bar.y(), bar.width(), bar.height(),
                ScholarShellStyle.PANEL_BACKGROUND);
        var slots = ScholarStatusBarLayout.compute(bar);
        if (pageCount > 0) {
            drawText(graphics, font, slots.page(),
                    ScholarText.get("scholar.status.page", currentPage, pageCount));
        }
        if (countedDocument != document) {
            countedDocument = document;
            wordCount = DocumentStatus.wordCount(document);
        }
        drawText(graphics, font, slots.words(), ScholarText.get("scholar.status.words", wordCount));
        drawIcon(graphics, slots.fitPage(), ScholarIcons.FIT_PAGE, mouseX, mouseY);
        drawIcon(graphics, slots.fitWidth(), ScholarIcons.FIT_WIDTH, mouseX, mouseY);
        drawIcon(graphics, slots.zoomOut(), ScholarIcons.ZOOM_OUT, mouseX, mouseY);
        if (slots.slider().width() > 0) {
            var thumbX = ScholarStatusBarLayout.thumbX(slots.slider(), zoomSupplier.get());
            ScholarShellRenderer.drawSlider(graphics, slots.slider(), thumbX, slots.slider().contains(mouseX, mouseY));
        }
        drawIcon(graphics, slots.zoomIn(), ScholarIcons.ZOOM_IN, mouseX, mouseY);
        drawText(graphics, font, slots.percentage(), Math.round(zoomSupplier.get() * 100) + "%");
        renderTooltip(graphics, font, bar, slots, screenWidth, mouseX, mouseY);
    }

    public boolean mouseClicked(ShellRect bar, double mouseX, double mouseY, int button) {
        if (!bar.contains(mouseX, mouseY)) return false;
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return true;
        var slots = ScholarStatusBarLayout.compute(bar);
        if (slots.fitPage().contains(mouseX, mouseY)) actionExecutor.accept(EditorActionId.VIEW_FIT_PAGE);
        else if (slots.fitWidth().contains(mouseX, mouseY)) actionExecutor.accept(EditorActionId.VIEW_FIT_WIDTH);
        else if (slots.zoomOut().contains(mouseX, mouseY)) actionExecutor.accept(EditorActionId.VIEW_ZOOM_OUT);
        else if (slots.zoomIn().contains(mouseX, mouseY)) actionExecutor.accept(EditorActionId.VIEW_ZOOM_IN);
        else if (slots.slider().contains(mouseX, mouseY)) {
            sliderDragging = true;
            zoomSetter.accept(ScholarStatusBarLayout.zoomAt(slots.slider(), mouseX));
        }
        return true;
    }

    public boolean mouseDragged(ShellRect bar, double mouseX, int button) {
        if (!sliderDragging || button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
        zoomSetter.accept(ScholarStatusBarLayout.zoomAt(ScholarStatusBarLayout.compute(bar).slider(), mouseX));
        return true;
    }

    public boolean mouseReleased(int button) {
        if (!sliderDragging || button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
        sliderDragging = false;
        return true;
    }

    private static void drawText(GuiGraphics graphics, Font font, ShellRect slot, String text) {
        if (slot.width() > 0) {
            graphics.drawString(font, text, slot.x(), slot.y() + 5, ScholarShellStyle.TEXT_PRIMARY, false);
        }
    }

    private static void drawIcon(
            GuiGraphics graphics, ShellRect slot, ScholarIcon icon, int mouseX, int mouseY) {
        if (slot.width() == 0) return;
        ScholarShellRenderer.drawControl(graphics, slot,
                slot.contains(mouseX, mouseY) ? ScholarControlState.HOVERED : ScholarControlState.NORMAL);
        icon.render(graphics, slot.x() + (slot.width() - icon.width()) / 2,
                slot.y() + (slot.height() - icon.height()) / 2, 1, ScholarShellStyle.TEXT_PRIMARY);
    }

    private static void renderTooltip(
            GuiGraphics graphics,
            Font font,
            ShellRect bar,
            ScholarStatusBarLayout slots,
            int screenWidth,
            int mouseX,
            int mouseY
    ) {
        var tooltip = slots.fitPage().contains(mouseX, mouseY) ? ScholarText.get("scholar.action.view_fit_page")
                : slots.fitWidth().contains(mouseX, mouseY) ? ScholarText.get("scholar.action.view_fit_width")
                : slots.zoomOut().contains(mouseX, mouseY) ? ScholarText.get("scholar.action.view_zoom_out")
                : slots.zoomIn().contains(mouseX, mouseY) ? ScholarText.get("scholar.action.view_zoom_in")
                : slots.slider().contains(mouseX, mouseY) ? ScholarText.get("scholar.tooltip.zoom") : null;
        if (tooltip == null) return;
        var width = font.width(tooltip) + 10;
        var x = Math.max(2, Math.min(mouseX, screenWidth - width - 2));
        ScholarShellRenderer.drawTooltipFrame(graphics, new ShellRect(x, bar.y() - 18, width, 16));
        graphics.drawString(font, tooltip, x + 5, bar.y() - 14, ScholarShellStyle.TEXT_PRIMARY, false);
    }
}
