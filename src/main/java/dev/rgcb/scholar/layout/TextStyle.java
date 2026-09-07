package dev.rgcb.scholar.layout;

import dev.rgcb.scholar.document.TextMark;
import dev.rgcb.scholar.typography.ScholarTypography;
import dev.rgcb.scholar.typography.TypographyRole;
import java.util.Set;

public record TextStyle(Set<TextMark> marks, int headingLevel, TypographyRole role) {
    public TextStyle(Set<TextMark> marks, int headingLevel) {
        this(marks, headingLevel, headingLevel == 0 ? TypographyRole.BODY : ScholarTypography.headingRole(headingLevel));
    }

    public TextStyle {
        marks = Set.copyOf(marks);
        if (role == null) {
            throw new NullPointerException("role");
        }
        if (headingLevel < 0 || headingLevel > 6) {
            throw new IllegalArgumentException("headingLevel must be 0 or between 1 and 6.");
        }
        if (headingLevel == 0 && role != TypographyRole.BODY) {
            throw new IllegalArgumentException("Paragraph text style must use BODY role.");
        }
        if (headingLevel > 0 && role != ScholarTypography.headingRole(headingLevel)) {
            throw new IllegalArgumentException("Heading text style role must match heading level.");
        }
    }

    public static TextStyle paragraph() {
        return paragraph(Set.of());
    }

    public static TextStyle paragraph(Set<TextMark> marks) {
        return new TextStyle(marks, 0);
    }

    public static TextStyle heading(int level) {
        return new TextStyle(Set.of(), level);
    }

    public TextStyle withMarks(Set<TextMark> marks) {
        return new TextStyle(marks, headingLevel, role);
    }

    public boolean isHeading() {
        return headingLevel > 0;
    }
}
