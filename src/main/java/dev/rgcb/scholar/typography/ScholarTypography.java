package dev.rgcb.scholar.typography;

import dev.rgcb.scholar.document.TextMark;
import dev.rgcb.scholar.layout.TextStyle;
import dev.rgcb.scholar.layout.TextCaretMetrics;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record ScholarTypography(
        Map<TypographyRole, TypographyRoleStyle> roleStyles,
        int paragraphSpacingAfter,
        int headingSpacingBefore,
        int headingSpacingAfterBase,
        int equationSpacingBefore,
        int equationSpacingAfter,
        int minPageMargin,
        int maxReadableContentWidth,
        int mathRuleThickness
) {
    public static final float SCRIPT_SCALE = 0.75f;
    public static final float SCRIPT_OFFSET = 3.0f;
    private static final int DOCUMENT_FONT_EM = 11;
    private static final int DOCUMENT_LEADING = 1;
    // Source Sans 3's visible ascender begins above Minecraft's 7-unit glyph baseline.
    private static final float DOCUMENT_CARET_ASCENDER_OFFSET = 4.0f;
    private static final float DOCUMENT_CARET_INK_HEIGHT = 10.0f;
    private static final int PARAGRAPH_COLOR = 0xFF241F1A;
    private static final int H1_COLOR = 0xFF1C3440;
    private static final int H2_COLOR = 0xFF384C36;
    private static final int HEADING_COLOR = 0xFF3B3329;

    public ScholarTypography {
        roleStyles = Map.copyOf(roleStyles);
        for (var role : TypographyRole.values()) {
            if (!roleStyles.containsKey(role)) {
                throw new IllegalArgumentException("Missing typography style for role: " + role);
            }
        }
        if (paragraphSpacingAfter < 0 || headingSpacingBefore < 0 || headingSpacingAfterBase < 0
                || equationSpacingBefore < 0 || equationSpacingAfter < 0 || minPageMargin < 0
                || maxReadableContentWidth <= 0 || mathRuleThickness <= 0) {
            throw new IllegalArgumentException("Typography spacing and sizing values must be valid.");
        }
    }

    public static ScholarTypography defaultProfile() {
        var styles = new EnumMap<TypographyRole, TypographyRoleStyle>(TypographyRole.class);
        styles.put(TypographyRole.BODY, new TypographyRoleStyle(false, false, 0, PARAGRAPH_COLOR));
        styles.put(TypographyRole.HEADING_1, new TypographyRoleStyle(true, false, 2, H1_COLOR));
        styles.put(TypographyRole.HEADING_2, new TypographyRoleStyle(true, false, 1, H2_COLOR));
        styles.put(TypographyRole.HEADING_3, new TypographyRoleStyle(true, false, 0, HEADING_COLOR));
        styles.put(TypographyRole.HEADING_4, new TypographyRoleStyle(false, false, 0, HEADING_COLOR));
        styles.put(TypographyRole.HEADING_5, new TypographyRoleStyle(false, true, 0, HEADING_COLOR));
        styles.put(TypographyRole.HEADING_6, new TypographyRoleStyle(false, true, -1, HEADING_COLOR));
        styles.put(TypographyRole.MATH, new TypographyRoleStyle(false, false, 0, PARAGRAPH_COLOR));
        return new ScholarTypography(styles, 10, 6, 8, 6, 10, 24, 360, 1);
    }

    public static TypographyRole headingRole(int level) {
        return switch (level) {
            case 1 -> TypographyRole.HEADING_1;
            case 2 -> TypographyRole.HEADING_2;
            case 3 -> TypographyRole.HEADING_3;
            case 4 -> TypographyRole.HEADING_4;
            case 5 -> TypographyRole.HEADING_5;
            case 6 -> TypographyRole.HEADING_6;
            default -> throw new IllegalArgumentException("Heading level must be between 1 and 6.");
        };
    }

    public ResolvedTypographyStyle resolve(TypographyRole role, Set<TextMark> marks) {
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(marks, "marks");
        var roleStyle = roleStyles.get(role);
        return new ResolvedTypographyStyle(
                role,
                marks,
                roleStyle.bold() || marks.contains(TextMark.BOLD),
                roleStyle.italic() || marks.contains(TextMark.ITALIC),
                roleStyle.lineHeightAdjustment(),
                roleStyle.color());
    }

    public int headingSpacingBefore(int level, boolean firstBlock) {
        if (firstBlock) {
            return 0;
        }
        return Math.max(2, headingSpacingBefore + (6 - level));
    }

    public int headingSpacingAfter(int level) {
        return headingSpacingAfterBase + Math.max(0, 3 - level);
    }

    public float textTopInset(TextStyle style) {
        return SCRIPT_OFFSET * style.format().fontSizeHalfPoints().orElse(20) / 20.0f;
    }

    public TextCaretMetrics documentCaretMetrics(TextStyle style, int lineHeight) {
        var scale = style.format().fontSizeHalfPoints().orElse(20) / 20.0f;
        var top = Math.round(textTopInset(style) - DOCUMENT_CARET_ASCENDER_OFFSET * scale);
        var height = Math.max(1, Math.min(lineHeight - top, Math.round(DOCUMENT_CARET_INK_HEIGHT * scale)));
        return new TextCaretMetrics(top, height);
    }

    public int documentGlyphEm(int fontLineHeight) {
        return Math.max(DOCUMENT_FONT_EM, fontLineHeight);
    }

    public int documentLineHeight(TextStyle style, int fontLineHeight, int roleAdjustment) {
        var scale = style.format().fontSizeHalfPoints().orElse(20) / 20.0f;
        var em = documentGlyphEm(fontLineHeight) + roleAdjustment;
        var offset = SCRIPT_OFFSET * scale;
        var normalExtent = em * scale;
        var subscriptExtent = offset + em * scale * SCRIPT_SCALE;
        return Math.max(1, (int) Math.ceil(offset + Math.max(normalExtent, subscriptExtent)
                + DOCUMENT_LEADING * scale));
    }
}
