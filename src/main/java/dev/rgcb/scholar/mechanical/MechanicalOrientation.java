package dev.rgcb.scholar.mechanical;

/** Minimal quarter-turn orientation needed by the M19C directional constraints. */
public enum MechanicalOrientation {
    DEG_0,
    DEG_90;

    public MechanicalOrientation perpendicular() {
        return this == DEG_0 ? DEG_90 : DEG_0;
    }
}
