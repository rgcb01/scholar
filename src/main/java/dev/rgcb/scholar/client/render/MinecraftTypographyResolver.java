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
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class MinecraftTypographyResolver {
    private static final ResourceLocation DOCUMENT_REGULAR = ResourceLocation.fromNamespaceAndPath(Scholar.MOD_ID, "document_regular");
    private static final ResourceLocation DOCUMENT_BOLD = ResourceLocation.fromNamespaceAndPath(Scholar.MOD_ID, "document_bold");
    private static final ResourceLocation DOCUMENT_ITALIC = ResourceLocation.fromNamespaceAndPath(Scholar.MOD_ID, "document_italic");
    private static final ResourceLocation DOCUMENT_BOLD_ITALIC = ResourceLocation.fromNamespaceAndPath(Scholar.MOD_ID, "document_bold_italic");
    private static final ResourceLocation MATH = ResourceLocation.fromNamespaceAndPath(Scholar.MOD_ID, "math");

    private final Font defaultFont;
    private final ScholarTypography typography;

    public MinecraftTypographyResolver(Font defaultFont, ScholarTypography typography) {
        this.defaultFont = Objects.requireNonNull(defaultFont, "defaultFont");
        this.typography = Objects.requireNonNull(typography, "typography");
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
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(resolved, "resolved");
        return Component.literal(text).withStyle(style -> {
            var styled = style;
            styled = styled.withFont(fontIdFor(resolved));
            return styled
                    .withBold(resolved.role() == TypographyRole.MATH && resolved.bold())
                    .withItalic(resolved.role() == TypographyRole.MATH && resolved.italic());
        });
    }

    public ScholarTypography typography() {
        return typography;
    }

    ResourceLocation fontIdFor(ResolvedTypographyStyle resolved) {
        if (resolved.role() == TypographyRole.MATH) {
            return MATH;
        }
        return selectVariant(resolved, DOCUMENT_REGULAR, DOCUMENT_BOLD, DOCUMENT_ITALIC, DOCUMENT_BOLD_ITALIC);
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
