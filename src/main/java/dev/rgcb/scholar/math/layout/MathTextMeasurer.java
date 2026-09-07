package dev.rgcb.scholar.math.layout;

public interface MathTextMeasurer {
    MathTextMetrics measureText(String content, MathTextKind kind);

    default int ruleThickness() {
        return 1;
    }
}
