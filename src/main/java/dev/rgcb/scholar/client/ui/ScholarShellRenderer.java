package dev.rgcb.scholar.client.ui;

import net.minecraft.client.gui.GuiGraphics;

public final class ScholarShellRenderer {
    private ScholarShellRenderer() {
    }

    public static void drawRaisedPanel(GuiGraphics graphics, int x, int y, int width, int height, int fill) {
        graphics.fill(x, y, x + width, y + height, ScholarShellStyle.OUTER_EDGE);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, fill);
        graphics.fill(x + 1, y + 1, x + width - 1, y + 2, ScholarShellStyle.HIGHLIGHT);
        graphics.fill(x + 1, y + 1, x + 2, y + height - 1, ScholarShellStyle.MID_HIGHLIGHT);
        graphics.fill(x + 1, y + height - 2, x + width - 1, y + height - 1, ScholarShellStyle.SHADOW);
        graphics.fill(x + width - 2, y + 1, x + width - 1, y + height - 1, ScholarShellStyle.DEEP_SHADOW);
    }

    public static void drawInsetPanel(GuiGraphics graphics, int x, int y, int width, int height, int fill) {
        graphics.fill(x, y, x + width, y + height, ScholarShellStyle.OUTER_EDGE);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, fill);
        graphics.fill(x + 1, y + 1, x + width - 1, y + 2, ScholarShellStyle.DEEP_SHADOW);
        graphics.fill(x + 1, y + 1, x + 2, y + height - 1, ScholarShellStyle.SHADOW);
        graphics.fill(x + 1, y + height - 2, x + width - 1, y + height - 1, ScholarShellStyle.HIGHLIGHT);
        graphics.fill(x + width - 2, y + 1, x + width - 1, y + height - 1, ScholarShellStyle.MID_HIGHLIGHT);
    }

    public static void drawVerticalSeparator(GuiGraphics graphics, int x, int y, int height) {
        graphics.fill(x, y + 2, x + 1, y + height - 2, ScholarShellStyle.SEPARATOR_DARK);
        graphics.fill(x + 1, y + 2, x + 2, y + height - 2, ScholarShellStyle.SEPARATOR_LIGHT);
    }

    public static void drawHorizontalSeparator(GuiGraphics graphics, int x, int y, int width) {
        graphics.fill(x, y, x + width, y + 1, ScholarShellStyle.SEPARATOR_DARK);
        graphics.fill(x, y + 1, x + width, y + 2, ScholarShellStyle.SEPARATOR_LIGHT);
    }
}
