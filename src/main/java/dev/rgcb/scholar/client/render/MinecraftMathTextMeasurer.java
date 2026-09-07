package dev.rgcb.scholar.client.render;

import dev.rgcb.scholar.math.layout.MathTextKind;
import dev.rgcb.scholar.math.layout.MathTextMeasurer;
import dev.rgcb.scholar.math.layout.MathTextMetrics;
import dev.rgcb.scholar.typography.ScholarTypography;
import net.minecraft.client.gui.Font;

public final class MinecraftMathTextMeasurer implements MathTextMeasurer {
    private static final int DEFAULT_DESCENT = 2;

    private final MinecraftTypographyResolver typographyResolver;

    public MinecraftMathTextMeasurer(Font font) {
        this(new MinecraftTypographyResolver(font, ScholarTypography.defaultProfile()));
    }

    public MinecraftMathTextMeasurer(MinecraftTypographyResolver typographyResolver) {
        this.typographyResolver = typographyResolver;
    }

    @Override
    public MathTextMetrics measureText(String content, MathTextKind kind) {
        var resolved = typographyResolver.resolveMath(kind);
        var font = typographyResolver.fontFor(resolved.role());
        return new MathTextMetrics(font.width(typographyResolver.component(content, resolved)), Math.max(0, font.lineHeight - DEFAULT_DESCENT), DEFAULT_DESCENT);
    }

    @Override
    public int ruleThickness() {
        return typographyResolver.typography().mathRuleThickness();
    }
}
