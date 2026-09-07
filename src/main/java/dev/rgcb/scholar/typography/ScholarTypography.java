package dev.rgcb.scholar.typography;

import dev.rgcb.scholar.document.TextMark;
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
        styles.put(TypographyRole.HEADING_2, new TypographyRoleStyle(true, false, 0, H2_COLOR));
        styles.put(TypographyRole.HEADING_3, new TypographyRoleStyle(false, false, 0, HEADING_COLOR));
        styles.put(TypographyRole.HEADING_4, new TypographyRoleStyle(false, false, 0, HEADING_COLOR));
        styles.put(TypographyRole.HEADING_5, new TypographyRoleStyle(false, false, 0, HEADING_COLOR));
        styles.put(TypographyRole.HEADING_6, new TypographyRoleStyle(false, false, 0, HEADING_COLOR));
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
}
