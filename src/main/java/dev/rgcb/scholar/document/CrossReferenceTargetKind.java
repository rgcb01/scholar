package dev.rgcb.scholar.document;

public enum CrossReferenceTargetKind {
    FIGURE("Figure"),
    TABLE("Table"),
    EQUATION("Equation"),
    SECTION("Section");

    private final String displayName;

    CrossReferenceTargetKind(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
