package dev.rgcb.scholar.typography;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.document.TextMark;
import dev.rgcb.scholar.layout.TextStyle;
import java.util.EnumMap;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ScholarTypographyTest {
    @Test
    void resolvesHeadingLevelsToTypographyRoles() {
        assertEquals(TypographyRole.HEADING_1, TextStyle.heading(1).role());
        assertEquals(TypographyRole.HEADING_2, TextStyle.heading(2).role());
        assertEquals(TypographyRole.HEADING_3, TextStyle.heading(3).role());
        assertEquals(TypographyRole.HEADING_4, TextStyle.heading(4).role());
        assertEquals(TypographyRole.HEADING_5, TextStyle.heading(5).role());
        assertEquals(TypographyRole.HEADING_6, TextStyle.heading(6).role());
    }

    @Test
    void resolvesParagraphTextToBodyRole() {
        assertEquals(TypographyRole.BODY, TextStyle.paragraph().role());
    }

    @Test
    void resolvesMathThroughMathRole() {
        var typography = ScholarTypography.defaultProfile();

        assertEquals(TypographyRole.MATH, typography.resolve(TypographyRole.MATH, Set.of()).role());
    }

    @Test
    void mapsBoldAndItalicMarksWithoutChangingSemanticMarks() {
        var typography = ScholarTypography.defaultProfile();
        var marks = Set.of(TextMark.BOLD, TextMark.ITALIC);

        var resolved = typography.resolve(TypographyRole.BODY, marks);

        assertTrue(resolved.bold());
        assertTrue(resolved.italic());
        assertEquals(marks, resolved.marks());
    }

    @Test
    void defaultProfileIsDeterministicAndImmutable() {
        var first = ScholarTypography.defaultProfile();
        var second = ScholarTypography.defaultProfile();

        assertEquals(first, second);
        assertThrows(UnsupportedOperationException.class, () -> first.roleStyles()
                .put(TypographyRole.BODY, new TypographyRoleStyle(true, false, 0, 0)));
    }

    @Test
    void lowerHeadingRolesHaveAReadableNonColorHierarchy() {
        var typography = ScholarTypography.defaultProfile();

        assertTrue(typography.resolve(TypographyRole.HEADING_3, Set.of()).bold());
        assertTrue(typography.resolve(TypographyRole.HEADING_5, Set.of()).italic());
        assertTrue(typography.resolve(TypographyRole.HEADING_6, Set.of()).lineHeightAdjustment()
                < typography.resolve(TypographyRole.HEADING_5, Set.of()).lineHeightAdjustment());
        assertTrue(typography.headingSpacingBefore(3, false) > typography.headingSpacingBefore(6, false));
    }

    @Test
    void rejectsMissingRoleStyles() {
        var styles = new EnumMap<TypographyRole, TypographyRoleStyle>(TypographyRole.class);
        styles.put(TypographyRole.BODY, new TypographyRoleStyle(false, false, 0, 0));

        assertThrows(IllegalArgumentException.class, () -> new ScholarTypography(styles, 10, 6, 8, 6, 10, 24, 360, 1));
    }

    @Test
    void rejectsTextStyleRoleMismatch() {
        assertThrows(IllegalArgumentException.class, () -> new TextStyle(Set.of(), 0, TypographyRole.HEADING_1));
        assertThrows(IllegalArgumentException.class, () -> new TextStyle(Set.of(), 1, TypographyRole.BODY));
    }
}
