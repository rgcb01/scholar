package dev.rgcb.scholar.mechanical;

/** M19C semantic mechanical relationships. */
public enum MechanicalConstraintKind {
    HORIZONTAL(false),
    VERTICAL(false),
    COINCIDENT(true),
    PARALLEL(true),
    PERPENDICULAR(true),
    CONCENTRIC(true);

    private final boolean binary;

    MechanicalConstraintKind(boolean binary) {
        this.binary = binary;
    }

    public boolean binary() {
        return binary;
    }
}
