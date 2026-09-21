package dev.rgcb.scholar.client.render;

import dev.rgcb.scholar.layout.TextMeasurer;
import dev.rgcb.scholar.layout.TextStyle;
import dev.rgcb.scholar.typography.ScholarTypography;
import net.minecraft.client.gui.Font;

public final class MinecraftTextMeasurer implements TextMeasurer {
    private final MinecraftTypographyResolver typographyResolver;

    public MinecraftTextMeasurer(Font font) {
        this(new MinecraftTypographyResolver(font, ScholarTypography.defaultProfile()));
    }

    public MinecraftTextMeasurer(MinecraftTypographyResolver typographyResolver) {
        this.typographyResolver = typographyResolver;
    }

    @Override
    public int measureWidth(String text, TextStyle style) {
        var resolved = typographyResolver.resolve(style);
        return Math.max(0, Math.round(typographyResolver.fontFor(resolved.role()).width(typographyResolver.component(text, resolved, style)) * scale(style)));
    }

    @Override
    public int lineHeight(TextStyle style) {
        var resolved = typographyResolver.resolve(style);
        return Math.max(1, Math.round((typographyResolver.fontFor(resolved.role()).lineHeight + resolved.lineHeightAdjustment()) * scale(style)));
    }

    private static float scale(TextStyle style) {
        var scale = style.format().fontSizeHalfPoints().orElse(20) / 20.0f;
        if (style.marks().contains(dev.rgcb.scholar.document.TextMark.SUPERSCRIPT)
                || style.marks().contains(dev.rgcb.scholar.document.TextMark.SUBSCRIPT)) {
            scale *= 0.75f;
        }
        return scale;
    }
}
