package dev.rgcb.scholar.client.render;

import dev.rgcb.scholar.Scholar;
import dev.rgcb.scholar.document.TextMark;
import dev.rgcb.scholar.layout.TextStyle;
import dev.rgcb.scholar.math.layout.MathTextKind;
import dev.rgcb.scholar.typography.ResolvedTypographyStyle;
import dev.rgcb.scholar.typography.ScholarTypography;
import dev.rgcb.scholar.typography.TypographyRole;
import java.util.Objects;
import java.util.Set;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class MinecraftTypographyResolver {
    private static final ResourceLocation DOCUMENT_REGULAR = ResourceLocation.fromNamespaceAndPath(Scholar.MOD_ID, "document_regular");
    private static final ResourceLocation DOCUMENT_BOLD = ResourceLocation.fromNamespaceAndPath(Scholar.MOD_ID, "document_bold");
    private static final ResourceLocation DOCUMENT_ITALIC = ResourceLocation.fromNamespaceAndPath(Scholar.MOD_ID, "document_italic");
    private static final ResourceLocation DOCUMENT_BOLD_ITALIC = ResourceLocation.fromNamespaceAndPath(Scholar.MOD_ID, "document_bold_italic");
    private static final ResourceLocation MATH = ResourceLocation.fromNamespaceAndPath(Scholar.MOD_ID, "math");
    private static final ResourceLocation DOCUMENT_REGULAR_HI = ResourceLocation.fromNamespaceAndPath(Scholar.MOD_ID, "document_regular_hi");
    private static final ResourceLocation DOCUMENT_BOLD_HI = ResourceLocation.fromNamespaceAndPath(Scholar.MOD_ID, "document_bold_hi");
    private static final ResourceLocation DOCUMENT_ITALIC_HI = ResourceLocation.fromNamespaceAndPath(Scholar.MOD_ID, "document_italic_hi");
    private static final ResourceLocation DOCUMENT_BOLD_ITALIC_HI = ResourceLocation.fromNamespaceAndPath(Scholar.MOD_ID, "document_bold_italic_hi");
    private static final ResourceLocation MATH_HI = ResourceLocation.fromNamespaceAndPath(Scholar.MOD_ID, "math_hi");

    private final Font defaultFont;
    private final ScholarTypography typography;
    private final boolean highResolution;

    public MinecraftTypographyResolver(Font defaultFont, ScholarTypography typography) {
        this(defaultFont, typography, false);
    }

    private MinecraftTypographyResolver(Font defaultFont, ScholarTypography typography, boolean highResolution) {
        this.defaultFont = Objects.requireNonNull(defaultFont, "defaultFont");
        this.typography = Objects.requireNonNull(typography, "typography");
        this.highResolution = highResolution;
    }

    public MinecraftTypographyResolver highResolution() {
        return new MinecraftTypographyResolver(defaultFont, typography, true);
    }

    public void drawDocumentString(GuiGraphics graphics, Component content, float x, float y, int color, float scale) {
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(x, y, 0);
            graphics.pose().scale(scale / (highResolution ? 2.0f : 1.0f), scale / (highResolution ? 2.0f : 1.0f), 1.0f);
            graphics.drawString(defaultFont, content, 0, 0, color, false);
        } finally {
            graphics.pose().popPose();
        }
    }

    public float logicalWidth(Component content) {
        return defaultFont.width(content) / (highResolution ? 2.0f : 1.0f);
    }

    public Font fontFor(TypographyRole role) {
        Objects.requireNonNull(role, "role");
        return defaultFont;
    }

    public ResolvedTypographyStyle resolve(TextStyle style) {
        return typography.resolve(style.role(), style.marks());
    }

    public ResolvedTypographyStyle resolveMath(MathTextKind kind) {
        Objects.requireNonNull(kind, "kind");
        return typography.resolve(TypographyRole.MATH, Set.of());
    }

    public Component component(String text, ResolvedTypographyStyle resolved) {
        return component(text, resolved, null);
    }

    public Component component(String text, ResolvedTypographyStyle resolved, TextStyle textStyle) {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(resolved, "resolved");
        return Component.literal(text).withStyle(style -> {
            var styled = style;
            styled = styled.withFont(fontIdFor(resolved, textStyle));
            return styled
                    .withBold(resolved.role() == TypographyRole.MATH && resolved.bold())
                    .withItalic(resolved.role() == TypographyRole.MATH && resolved.italic())
                    .withUnderlined(textStyle != null && textStyle.marks().contains(TextMark.UNDERLINE));
        });
    }

    public ScholarTypography typography() {
        return typography;
    }

    ResourceLocation fontIdFor(ResolvedTypographyStyle resolved) {
        return fontIdFor(resolved, null);
    }

    ResourceLocation fontIdFor(ResolvedTypographyStyle resolved, TextStyle textStyle) {
        if (resolved.role() == TypographyRole.MATH) {
            return highResolution ? MATH_HI : MATH;
        }
        if (textStyle != null && textStyle.format().fontFamily().orElse(null) == dev.rgcb.scholar.document.ScholarFontFamily.SCIENTIFIC_MATH) {
            return highResolution ? MATH_HI : MATH;
        }
        return highResolution
                ? selectVariant(resolved, DOCUMENT_REGULAR_HI, DOCUMENT_BOLD_HI, DOCUMENT_ITALIC_HI, DOCUMENT_BOLD_ITALIC_HI)
                : selectVariant(resolved, DOCUMENT_REGULAR, DOCUMENT_BOLD, DOCUMENT_ITALIC, DOCUMENT_BOLD_ITALIC);
    }

    private static ResourceLocation selectVariant(
            ResolvedTypographyStyle resolved,
            ResourceLocation regular,
            ResourceLocation bold,
            ResourceLocation italic,
            ResourceLocation boldItalic
    ) {
        if (resolved.bold() && resolved.italic()) {
            return boldItalic;
        }
        if (resolved.bold()) {
            return bold;
        }
        if (resolved.italic()) {
            return italic;
        }
        return regular;
    }
}
