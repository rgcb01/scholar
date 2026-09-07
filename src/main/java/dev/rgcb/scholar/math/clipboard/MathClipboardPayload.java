package dev.rgcb.scholar.math.clipboard;

import dev.rgcb.scholar.clipboard.ScholarClipboardPayload;
import dev.rgcb.scholar.math.MathSequence;
import java.util.Objects;

public record MathClipboardPayload(MathSequence fragment) implements ScholarClipboardPayload {
    public MathClipboardPayload {
        fragment = Objects.requireNonNull(fragment, "fragment");
    }
}
