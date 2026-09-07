package dev.rgcb.scholar.editor;

public record BlockStyle(int headingLevel) {
    public BlockStyle {
        if (headingLevel < 0 || headingLevel > 6) {
            throw new IllegalArgumentException("headingLevel must be 0 for paragraph or 1 through 6 for heading.");
        }
    }

    public static BlockStyle paragraph() {
        return new BlockStyle(0);
    }

    public static BlockStyle heading(int level) {
        return new BlockStyle(level);
    }

    public boolean isParagraph() {
        return headingLevel == 0;
    }

    public boolean isHeading() {
        return headingLevel > 0;
    }

    public String displayName() {
        return isParagraph() ? "Paragraph" : "Heading " + headingLevel;
    }
}
