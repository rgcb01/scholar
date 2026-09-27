package dev.rgcb.scholar.client.ui;

import dev.rgcb.scholar.editor.ActionSelectionState;

/** Shared interaction states for controls on Scholar's instrument surfaces. */
public enum ScholarControlState {
    NORMAL,
    HOVERED,
    PRESSED,
    SELECTED,
    MIXED,
    FOCUSED,
    DISABLED;

    public static ScholarControlState resolve(
            boolean enabled, ActionSelectionState selection, boolean hovered, boolean pressed) {
        return resolve(enabled, selection, hovered, pressed, false);
    }

    public static ScholarControlState resolve(
            boolean enabled, ActionSelectionState selection, boolean hovered, boolean pressed, boolean focused) {
        if (!enabled) return DISABLED;
        if (pressed) return PRESSED;
        if (selection == ActionSelectionState.ON) return SELECTED;
        if (selection == ActionSelectionState.MIXED) return MIXED;
        if (focused) return FOCUSED;
        return hovered ? HOVERED : NORMAL;
    }

    public boolean hasPersistentIndicator() {
        return this == SELECTED || this == MIXED || this == FOCUSED;
    }

    /** Instrument controls keep their content fixed while the surface changes beneath it. */
    public int contentOffset() {
        return 0;
    }
}
