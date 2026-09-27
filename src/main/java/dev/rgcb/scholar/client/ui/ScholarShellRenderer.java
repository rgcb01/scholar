package dev.rgcb.scholar.client.ui;

import net.minecraft.client.gui.GuiGraphics;

/** Reusable flat primitives for Scholar's three-level scientific-instrument shell. */
public final class ScholarShellRenderer {
    private ScholarShellRenderer() {
    }

    public static void drawRaisedPanel(GuiGraphics graphics, int x, int y, int width, int height, int fill) {
        drawPanel(graphics, new ShellRect(x, y, width, height), fill, true);
    }

    public static void drawInsetPanel(GuiGraphics graphics, int x, int y, int width, int height, int fill) {
        drawPanel(graphics, new ShellRect(x, y, width, height), fill, false);
    }

    public static void drawPanel(GuiGraphics graphics, ShellRect bounds, int fill, boolean elevated) {
        if (bounds.width() <= 0 || bounds.height() <= 0) return;
        if (elevated) {
            graphics.fill(bounds.x() + 2, bounds.y() + 2, bounds.right() + 2, bounds.bottom() + 2,
                    ScholarShellStyle.PANEL_SHADOW);
        }
        graphics.fill(bounds.x(), bounds.y(), bounds.right(), bounds.bottom(), fill);
        graphics.renderOutline(bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                ScholarShellStyle.STRUCTURAL_BORDER);
    }

    public static void drawVerticalSeparator(GuiGraphics graphics, int x, int y, int height) {
        if (height > 4) graphics.fill(x, y + 2, x + 1, y + height - 2, ScholarShellStyle.SUBTLE_SEPARATOR);
    }

    public static void drawHorizontalSeparator(GuiGraphics graphics, int x, int y, int width) {
        if (width > 0) graphics.fill(x, y, x + width, y + 1, ScholarShellStyle.SUBTLE_SEPARATOR);
    }

    public static void drawScreenBackground(GuiGraphics graphics, int width, int height) {
        graphics.fill(0, 0, width, height, ScholarShellStyle.SHELL_BACKGROUND);
    }

    /** Normal actions inherit their shared panel and intentionally draw no permanent box. */
    public static void drawControl(GuiGraphics graphics, ShellRect bounds, ScholarControlState state) {
        if (bounds.width() <= 0 || bounds.height() <= 0 || state == ScholarControlState.NORMAL) return;
        var fill = switch (state) {
            case HOVERED, FOCUSED -> ScholarShellStyle.HOVER_BACKGROUND;
            case PRESSED -> ScholarShellStyle.PRESSED_BACKGROUND;
            case SELECTED -> ScholarShellStyle.SELECTED_BACKGROUND;
            case MIXED -> ScholarShellStyle.MIXED_BACKGROUND;
            case DISABLED -> ScholarShellStyle.DISABLED_BACKGROUND;
            case NORMAL -> throw new IllegalStateException("Normal controls use the shared panel surface");
        };
        graphics.fill(bounds.x(), bounds.y(), bounds.right(), bounds.bottom(), fill);
        if (state == ScholarControlState.HOVERED || state == ScholarControlState.PRESSED) {
            graphics.renderOutline(bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                    ScholarShellStyle.SUBTLE_SEPARATOR);
        }
        if (state == ScholarControlState.SELECTED || state == ScholarControlState.MIXED) {
            graphics.fill(bounds.x(), bounds.y() + 2, bounds.x() + 2, bounds.bottom() - 2,
                    ScholarShellStyle.ACTIVE_INDICATOR);
            if (state == ScholarControlState.MIXED) {
                graphics.fill(bounds.x() + 4, bounds.bottom() - 3, bounds.right() - 3, bounds.bottom() - 2,
                        ScholarShellStyle.TEXT_SECONDARY);
            }
        }
        if (state == ScholarControlState.FOCUSED) {
            graphics.renderOutline(bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                    ScholarShellStyle.FOCUS_INDICATOR);
        }
    }

    public static void drawGroupPanel(GuiGraphics graphics, ShellRect bounds) {
        drawVerticalSeparator(graphics, bounds.right() - 1, bounds.y() + 2, bounds.height() - 4);
    }

    public static void drawTab(GuiGraphics graphics, ShellRect bounds, boolean active, boolean hovered) {
        if (active) {
            graphics.fill(bounds.x(), bounds.y(), bounds.right(), bounds.bottom(), ScholarShellStyle.PANEL_BACKGROUND);
            graphics.fill(bounds.x() + 2, bounds.bottom() - 2, bounds.right() - 2, bounds.bottom(),
                    ScholarShellStyle.ACTIVE_INDICATOR);
        } else if (hovered) {
            graphics.fill(bounds.x(), bounds.y(), bounds.right(), bounds.bottom(), ScholarShellStyle.HOVER_BACKGROUND);
        }
    }

    public static void drawPopupPanel(GuiGraphics graphics, ShellRect bounds) {
        drawPanel(graphics, bounds, ScholarShellStyle.PANEL_ELEVATED_BACKGROUND, true);
    }

    public static void drawDialogFrame(GuiGraphics graphics, ShellRect bounds) {
        drawPanel(graphics, bounds, ScholarShellStyle.PANEL_ELEVATED_BACKGROUND, true);
        drawHorizontalSeparator(graphics, bounds.x() + 6, bounds.y() + 20, bounds.width() - 12);
    }

    public static void drawMenuRow(GuiGraphics graphics, ShellRect bounds, boolean selected) {
        if (selected) drawControl(graphics, bounds, ScholarControlState.HOVERED);
    }

    public static void drawTooltipFrame(GuiGraphics graphics, ShellRect bounds) {
        graphics.fill(bounds.x(), bounds.y(), bounds.right(), bounds.bottom(), ScholarShellStyle.TOOLTIP_BACKGROUND);
        graphics.renderOutline(bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                ScholarShellStyle.STRUCTURAL_BORDER);
    }

    public static void drawSlider(GuiGraphics graphics, ShellRect track, int thumbX, boolean hovered) {
        var trackY = track.y() + track.height() / 2;
        graphics.fill(track.x(), trackY, track.right(), trackY + 1, ScholarShellStyle.STRUCTURAL_BORDER);
        graphics.fill(track.x(), trackY, Math.max(track.x(), thumbX), trackY + 2,
                ScholarShellStyle.SCIENTIFIC_ACCENT_MUTED);
        var thumbColor = hovered ? ScholarShellStyle.FOCUS_INDICATOR : ScholarShellStyle.ACTIVE_INDICATOR;
        graphics.fill(thumbX - 1, track.y() + 3, thumbX + 2, track.bottom() - 3, thumbColor);
    }

    public static void drawScrollbar(
            GuiGraphics graphics, ShellRect bounds, int totalRows, int visibleRows, int firstRow) {
        if (totalRows <= visibleRows || visibleRows <= 0) return;
        var track = new ShellRect(bounds.right() - 4, bounds.y() + 3, 2, Math.max(1, bounds.height() - 6));
        graphics.fill(track.x(), track.y(), track.right(), track.bottom(), ScholarShellStyle.PANEL_RECESSED_BACKGROUND);
        var thumbHeight = Math.max(8, track.height() * visibleRows / totalRows);
        var travel = Math.max(0, track.height() - thumbHeight);
        var maxFirstRow = Math.max(1, totalRows - visibleRows);
        var thumbY = track.y() + travel * Math.max(0, Math.min(firstRow, maxFirstRow)) / maxFirstRow;
        graphics.fill(track.x(), thumbY, track.right(), thumbY + thumbHeight,
                ScholarShellStyle.SCIENTIFIC_ACCENT_MUTED);
    }
}
