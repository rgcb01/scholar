package dev.rgcb.scholar.client.ui;

import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.DocumentStructureResolver;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** Owns the optional document-outline presentation and interaction state. */
public final class EditorOutlinePanel {
    private static final int ROW_HEIGHT = 18;
    private final Supplier<Document> documentSupplier;
    private final Consumer<String> navigator;
    private final DocumentStructureResolver structureResolver = new DocumentStructureResolver();
    private boolean open;
    private int scroll;

    public EditorOutlinePanel(Supplier<Document> documentSupplier, Consumer<String> navigator) {
        this.documentSupplier = Objects.requireNonNull(documentSupplier, "documentSupplier");
        this.navigator = Objects.requireNonNull(navigator, "navigator");
    }

    public boolean toggle(int screenWidth, int screenHeight) {
        open = !open;
        scroll = clampScroll(scroll, screenWidth, screenHeight);
        return open;
    }

    public boolean isOpen() {
        return open;
    }

    public boolean contains(double mouseX, double mouseY, int screenWidth, int screenHeight) {
        return open && bounds(screenWidth, screenHeight).contains(mouseX, mouseY);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int screenWidth, int screenHeight) {
        if (!contains(mouseX, mouseY, screenWidth, screenHeight)) return false;
        var bounds = bounds(screenWidth, screenHeight);
        var close = new ShellRect(bounds.right() - 22, bounds.y() + 5, 16, 14);
        if (close.contains(mouseX, mouseY)) {
            open = false;
            return true;
        }
        var y = bounds.y() + 26 - scroll;
        for (var section : sections()) {
            var row = new ShellRect(bounds.x() + 6, y, bounds.width() - 12, ROW_HEIGHT);
            if (row.contains(mouseX, mouseY)) {
                section.id().ifPresent(navigator);
                return true;
            }
            y += ROW_HEIGHT;
        }
        return true;
    }

    public boolean mouseScrolled(
            double mouseX, double mouseY, double scrollY, int screenWidth, int screenHeight) {
        if (!contains(mouseX, mouseY, screenWidth, screenHeight)) return false;
        scroll = clampScroll(
                scroll - (int) Math.signum(scrollY) * ScholarShellLayout.DOCUMENT_SCROLL_STEP,
                screenWidth, screenHeight);
        return true;
    }

    public void render(
            GuiGraphics graphics, Font font, int screenWidth, int screenHeight, int mouseX, int mouseY) {
        if (!open) return;
        var bounds = bounds(screenWidth, screenHeight);
        ScholarShellRenderer.drawRaisedPanel(graphics, bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                ScholarShellStyle.PANEL_BACKGROUND);
        graphics.drawString(font, ScholarText.get("scholar.action.toggle_outline"),
                bounds.x() + 8, bounds.y() + 9, ScholarShellStyle.TEXT_PRIMARY, false);
        var close = new ShellRect(bounds.right() - 22, bounds.y() + 5, 16, 14);
        ScholarShellRenderer.drawControl(graphics, close,
                close.contains(mouseX, mouseY) ? ScholarControlState.HOVERED : ScholarControlState.NORMAL);
        graphics.drawCenteredString(font, "x", close.x() + close.width() / 2, close.y() + 3,
                ScholarShellStyle.TEXT_PRIMARY);

        var contentTop = bounds.y() + 26;
        graphics.fill(bounds.x() + 4, contentTop - 2, bounds.right() - 4, bounds.bottom() - 4,
                ScholarShellStyle.PANEL_RECESSED_BACKGROUND);
        graphics.enableScissor(bounds.x() + 4, contentTop, bounds.right() - 4, bounds.bottom() - 4);
        try {
            var y = contentTop - scroll;
            for (var section : sections()) {
                var row = new ShellRect(bounds.x() + 6, y, bounds.width() - 12, ROW_HEIGHT);
                if (row.bottom() >= contentTop && row.y() <= bounds.bottom() - 4) {
                    if (row.contains(mouseX, mouseY)) {
                        graphics.fill(row.x(), row.y(), row.right(), row.bottom(),
                                ScholarShellStyle.HOVER_BACKGROUND);
                    }
                    var indent = Math.max(0, section.level() - 1) * 10;
                    var label = clipFromEnd(font, section.displayText(), row.width() - indent - 6);
                    graphics.drawString(font, label, row.x() + indent + 3, row.y() + 5,
                            section.id().isPresent()
                                    ? ScholarShellStyle.TEXT_PRIMARY : ScholarShellStyle.TEXT_DISABLED,
                            false);
                }
                y += ROW_HEIGHT;
            }
        } finally {
            graphics.disableScissor();
        }
    }

    private java.util.List<dev.rgcb.scholar.document.SectionEntry> sections() {
        return structureResolver.resolve(documentSupplier.get()).sections();
    }

    private ShellRect bounds(int screenWidth, int screenHeight) {
        var width = Math.min(260, Math.max(180, screenWidth / 3));
        var height = Math.max(90, screenHeight - MenuBarWidget.HEIGHT - 18);
        return new ShellRect(8, MenuBarWidget.HEIGHT + 8, width, height);
    }

    private int clampScroll(int value, int screenWidth, int screenHeight) {
        var visibleHeight = Math.max(1, bounds(screenWidth, screenHeight).height() - 32);
        var contentHeight = sections().size() * ROW_HEIGHT;
        return Math.max(0, Math.min(value, Math.max(0, contentHeight - visibleHeight)));
    }

    private static String clipFromEnd(Font font, String value, int maxWidth) {
        if (maxWidth <= 0) return "";
        if (font.width(value) <= maxWidth) return value;
        var ellipsis = "...";
        var suffix = font.plainSubstrByWidth(
                new StringBuilder(value).reverse().toString(), Math.max(0, maxWidth - font.width(ellipsis)));
        return ellipsis + new StringBuilder(suffix).reverse();
    }
}
