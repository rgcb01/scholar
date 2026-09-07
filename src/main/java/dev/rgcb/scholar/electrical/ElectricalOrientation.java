package dev.rgcb.scholar.electrical;

/** Deterministic quarter-turn orientation for schematic components. */
public enum ElectricalOrientation {
    DEG_0(0),
    DEG_90(1),
    DEG_180(2),
    DEG_270(3);

    private final int quarterTurnsClockwise;

    ElectricalOrientation(int quarterTurnsClockwise) {
        this.quarterTurnsClockwise = quarterTurnsClockwise;
    }

    public int quarterTurnsClockwise() {
        return quarterTurnsClockwise;
    }

    public ElectricalOrientation rotateClockwise() {
        return values()[(ordinal() + 1) % values().length];
    }

    public ElectricalOrientation rotateCounterClockwise() {
        return values()[(ordinal() + values().length - 1) % values().length];
    }
}
