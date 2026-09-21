package dev.rgcb.scholar.client.ui;

import java.util.List;
import java.util.Objects;
import net.minecraft.client.gui.GuiGraphics;

/** A tiny code-defined pixel icon rendered without font glyphs or platform textures. */
public record ScholarIcon(String name, List<String> pixels) {
    public ScholarIcon {
        name = Objects.requireNonNull(name, "name");
        pixels = List.copyOf(Objects.requireNonNull(pixels, "pixels"));
        if (pixels.isEmpty()) throw new IllegalArgumentException("Icon pixels must not be empty.");
        var width = pixels.stream().mapToInt(String::length).max().orElseThrow();
        pixels = pixels.stream().map(row -> row + " ".repeat(width - row.length())).toList();
    }

    public int width() { return pixels.getFirst().length(); }
    public int height() { return pixels.size(); }

    public void render(GuiGraphics graphics, int x, int y, int scale, int color) {
        for (var row = 0; row < height(); row++) {
            for (var column = 0; column < width(); column++) {
                if (pixels.get(row).charAt(column) != ' ') {
                    graphics.fill(x + column * scale, y + row * scale,
                            x + (column + 1) * scale, y + (row + 1) * scale, color);
                }
            }
        }
    }
}
