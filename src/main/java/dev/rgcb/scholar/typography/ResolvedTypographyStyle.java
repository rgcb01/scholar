package dev.rgcb.scholar.typography;

import dev.rgcb.scholar.document.TextMark;
import java.util.Set;

public record ResolvedTypographyStyle(
        TypographyRole role,
        Set<TextMark> marks,
        boolean bold,
        boolean italic,
        int lineHeightAdjustment,
        int color
) {
    public ResolvedTypographyStyle {
        marks = Set.copyOf(marks);
    }
}
