package dev.rgcb.scholar.editor;

import dev.rgcb.scholar.document.InlineContent;
import java.util.Objects;

record InlineContentSplit(InlineContent left, InlineContent right) {
    InlineContentSplit {
        left = Objects.requireNonNull(left, "left");
        right = Objects.requireNonNull(right, "right");
    }
}
