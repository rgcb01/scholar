package dev.rgcb.scholar.math.editor;

public enum SemanticMathTokenKind {
    NAMED_OPERATOR("Named Operator"),
    MATH_TEXT("Math Text");

    private final String displayName;

    SemanticMathTokenKind(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
