package dev.rgcb.scholar.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** Compact application/document status region; it owns no command semantics. */
public final class ApplicationHeaderWidget {
    public static final int HEIGHT = 22;

    public void render(GuiGraphics graphics, Font font, ShellRect bounds, String documentName, boolean dirty) {
        graphics.fill(bounds.x(), bounds.y(), bounds.right(), bounds.bottom(), 0xFF292E35);
        graphics.fill(bounds.x(), bounds.y(), bounds.right(), bounds.y() + 1, ScholarShellStyle.HIGHLIGHT);
        graphics.fill(bounds.x(), bounds.bottom() - 1, bounds.right(), bounds.bottom(), ScholarShellStyle.DEEP_SHADOW);
        ScholarIcons.SCHOLAR.render(graphics, bounds.x() + 7, bounds.y() + 7, 1, 0xFF73C4E2);
        graphics.drawString(font, "Scholar", bounds.x() + 19, bounds.y() + 7, 0xFFFFFFFF, false);
        var titleWidth = Math.max(24, bounds.width() / 2);
        var clippedTitle = font.plainSubstrByWidth(documentName, titleWidth);
        graphics.drawCenteredString(font, clippedTitle, bounds.x() + bounds.width() / 2, bounds.y() + 7, 0xFFF2F5F8);
        var status = dirty ? "Unsaved" : "Saved";
        graphics.drawString(font, status, bounds.right() - 8 - font.width(status), bounds.y() + 7,
                dirty ? 0xFFFFD27A : 0xFFAAB4BF, false);
    }
}
