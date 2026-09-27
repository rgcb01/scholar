package dev.rgcb.scholar.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** Compact application/document status region; it owns no command semantics. */
public final class ApplicationHeaderWidget {
    public static final int HEIGHT = 22;

    public void render(GuiGraphics graphics, Font font, ShellRect bounds, String documentName, boolean dirty) {
        ScholarShellRenderer.drawRaisedPanel(graphics, bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                ScholarShellStyle.HEADER_BACKGROUND);
        ScholarIcons.SCHOLAR.render(graphics, bounds.x() + 7, bounds.y() + 7, 1, ScholarShellStyle.SCIENTIFIC_ACCENT);
        graphics.drawString(font, "Scholar", bounds.x() + 19, bounds.y() + 7, ScholarShellStyle.TEXT_PRIMARY, false);
        var titleWidth = Math.max(24, bounds.width() / 2);
        var clippedTitle = font.plainSubstrByWidth(documentName, titleWidth);
        graphics.drawCenteredString(font, clippedTitle, bounds.x() + bounds.width() / 2, bounds.y() + 7, ScholarShellStyle.TEXT_PRIMARY);
        var status = ScholarText.get(dirty ? "scholar.status.unsaved" : "scholar.status.saved");
        var statusX = bounds.right() - 8 - font.width(status);
        graphics.fill(statusX - 8, bounds.y() + 9, statusX - 4, bounds.y() + 13,
                dirty ? ScholarShellStyle.SCIENTIFIC_ACCENT : ScholarShellStyle.SCIENTIFIC_ACCENT_MUTED);
        graphics.drawString(font, status, bounds.right() - 8 - font.width(status), bounds.y() + 7,
                dirty ? ScholarShellStyle.TEXT_WARNING : ScholarShellStyle.TEXT_SECONDARY, false);
    }
}
