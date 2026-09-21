package dev.rgcb.scholar.client.ui;

import java.util.Objects;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;

/** Renders widgets after Scholar's opaque surface without invoking Minecraft's blurred menu background. */
public final class ScholarScreenRendering {
    private ScholarScreenRendering() { }

    public static void renderWidgets(
            Iterable<? extends Renderable> renderables,
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        Objects.requireNonNull(renderables, "renderables");
        Objects.requireNonNull(graphics, "graphics");
        for (var renderable : renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
        }
    }
}
