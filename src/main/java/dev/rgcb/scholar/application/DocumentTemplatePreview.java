package dev.rgcb.scholar.application;

/** Existing Home-card preview variants, separate from document semantics. */
public enum DocumentTemplatePreview {
    SINGLE_COLUMN(false),
    TWO_COLUMN(true);

    private final boolean twoColumns;

    DocumentTemplatePreview(boolean twoColumns) {
        this.twoColumns = twoColumns;
    }

    public boolean twoColumns() {
        return twoColumns;
    }
}
