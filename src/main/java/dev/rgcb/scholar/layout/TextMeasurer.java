package dev.rgcb.scholar.layout;

public interface TextMeasurer {
    int measureWidth(String text, TextStyle style);

    int lineHeight(TextStyle style);
}
