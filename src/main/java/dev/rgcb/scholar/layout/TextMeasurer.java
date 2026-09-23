package dev.rgcb.scholar.layout;

public interface TextMeasurer {
    int measureWidth(String text, TextStyle style);

    int lineHeight(TextStyle style);

    default TextCaretMetrics caretMetrics(TextStyle style, int lineHeight) {
        return new TextCaretMetrics(0, Math.max(1, lineHeight));
    }
}
