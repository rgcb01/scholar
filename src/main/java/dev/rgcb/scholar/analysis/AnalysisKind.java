package dev.rgcb.scholar.analysis;

public enum AnalysisKind {
    DESCRIPTIVE(0), LINEAR_REGRESSION(1), QUADRATIC_FIT(2), CUBIC_FIT(3);

    private final int degree;
    AnalysisKind(int degree) { this.degree = degree; }
    public int degree() { return degree; }
    public boolean isFit() { return degree > 0; }
}
