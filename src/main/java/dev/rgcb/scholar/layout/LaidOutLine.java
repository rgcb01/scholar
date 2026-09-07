package dev.rgcb.scholar.layout;

import java.util.List;

public record LaidOutLine(int x, int y, int width, int height, List<LaidOutText> textRuns) {
    public LaidOutLine {
        textRuns = List.copyOf(textRuns);
    }
}
