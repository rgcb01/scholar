package dev.rgcb.scholar.client.screen;

import dev.rgcb.scholar.client.DevelopmentDocument;
import dev.rgcb.scholar.client.render.MinecraftDocumentRenderer;
import dev.rgcb.scholar.client.render.MinecraftMathTextMeasurer;
import dev.rgcb.scholar.client.render.MinecraftTextMeasurer;
import dev.rgcb.scholar.client.render.MinecraftTypographyResolver;
import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.layout.DocumentLayoutEngine;
import dev.rgcb.scholar.layout.LaidOutDocument;
import dev.rgcb.scholar.typography.ScholarTypography;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ScholarDocumentScreen extends Screen {
    private static final int BACKGROUND_COLOR = 0xFF202020;
    private static final int SCROLL_STEP = 24;

    private final Document document;
    private final ScholarTypography typography = ScholarTypography.defaultProfile();
    private final DocumentLayoutEngine layoutEngine = new DocumentLayoutEngine(typography);
    private LaidOutDocument laidOutDocument;
    private int viewportX;
    private int viewportY;
    private int viewportWidth;
    private int viewportHeight;
    private int scrollOffset;

    private ScholarDocumentScreen(Document document) {
        super(Component.literal("Scholar Development Viewer"));
        this.document = document;
    }

    public static ScholarDocumentScreen createDevelopmentScreen() {
        return new ScholarDocumentScreen(DevelopmentDocument.create());
    }

    @Override
    protected void init() {
        relayout();
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        super.resize(minecraft, width, height);
        relayout();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, BACKGROUND_COLOR);
        if (laidOutDocument != null) {
            new MinecraftDocumentRenderer(new MinecraftTypographyResolver(font, typography)).render(
                    graphics,
                    laidOutDocument,
                    viewportX,
                    viewportY,
                    viewportWidth,
                    viewportHeight,
                    scrollOffset);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (laidOutDocument == null || !isInsideViewport(mouseX, mouseY)) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        scrollOffset = clampScroll(scrollOffset - (int) Math.signum(scrollY) * SCROLL_STEP);
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void relayout() {
        if (font == null) {
            return;
        }

        var horizontalMargin = Math.max(typography.minPageMargin(), width / 12);
        viewportWidth = Math.min(typography.maxReadableContentWidth(), Math.max(80, width - horizontalMargin * 2));
        viewportHeight = Math.max(60, height - typography.minPageMargin() * 2);
        viewportX = (width - viewportWidth) / 2;
        viewportY = typography.minPageMargin();
        var typographyResolver = new MinecraftTypographyResolver(font, typography);
        laidOutDocument = layoutEngine.layout(
                document,
                viewportWidth,
                new MinecraftTextMeasurer(typographyResolver),
                new MinecraftMathTextMeasurer(typographyResolver));
        scrollOffset = clampScroll(scrollOffset);
    }

    private int clampScroll(int value) {
        var maxScroll = laidOutDocument == null ? 0 : Math.max(0, laidOutDocument.height() - viewportHeight);
        return Math.max(0, Math.min(value, maxScroll));
    }

    private boolean isInsideViewport(double mouseX, double mouseY) {
        return mouseX >= viewportX && mouseX <= viewportX + viewportWidth
                && mouseY >= viewportY && mouseY <= viewportY + viewportHeight;
    }
}
