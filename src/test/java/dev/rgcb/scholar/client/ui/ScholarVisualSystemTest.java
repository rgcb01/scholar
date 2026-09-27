package dev.rgcb.scholar.client.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.editor.ActionSelectionState;
import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ScholarVisualSystemTest {
    @Test void controlStateUsesUnambiguousInteractionPrecedence() {
        assertEquals(ScholarControlState.DISABLED,
                ScholarControlState.resolve(false, ActionSelectionState.ON, true, true));
        assertEquals(ScholarControlState.PRESSED,
                ScholarControlState.resolve(true, ActionSelectionState.ON, true, true));
        assertEquals(ScholarControlState.SELECTED,
                ScholarControlState.resolve(true, ActionSelectionState.ON, true, false));
        assertEquals(ScholarControlState.MIXED,
                ScholarControlState.resolve(true, ActionSelectionState.MIXED, true, false));
        assertEquals(ScholarControlState.FOCUSED,
                ScholarControlState.resolve(true, ActionSelectionState.OFF, false, false, true));
        assertEquals(ScholarControlState.HOVERED,
                ScholarControlState.resolve(true, ActionSelectionState.OFF, true, false));
        assertEquals(ScholarControlState.NORMAL,
                ScholarControlState.resolve(true, ActionSelectionState.OFF, false, false));
    }

    @Test void instrumentControlsNeverShiftTheirContent() {
        for (var state : ScholarControlState.values()) {
            assertEquals(0, state.contentOffset(), state.name());
        }
        assertTrue(ScholarControlState.SELECTED.hasPersistentIndicator());
        assertTrue(ScholarControlState.MIXED.hasPersistentIndicator());
        assertTrue(ScholarControlState.FOCUSED.hasPersistentIndicator());
    }

    @Test void themeDefinesTheSharedShellPolicyTokens() {
        var fields = Set.of(ScholarShellStyle.class.getFields()).stream()
                .filter(field -> Modifier.isStatic(field.getModifiers()))
                .map(java.lang.reflect.Field::getName)
                .collect(Collectors.toSet());
        assertTrue(fields.containsAll(Set.of(
                "SHELL_BACKGROUND", "WORKSPACE_BACKGROUND", "HEADER_BACKGROUND",
                "PANEL_BACKGROUND", "PANEL_ELEVATED_BACKGROUND", "PANEL_RECESSED_BACKGROUND",
                "HOVER_BACKGROUND", "PRESSED_BACKGROUND", "SELECTED_BACKGROUND",
                "DISABLED_BACKGROUND", "MIXED_BACKGROUND",
                "TEXT_PRIMARY", "TEXT_SECONDARY", "TEXT_DISABLED",
                "STRUCTURAL_BORDER", "SUBTLE_SEPARATOR", "FOCUS_INDICATOR",
                "SCIENTIFIC_ACCENT", "SCIENTIFIC_ACCENT_MUTED", "ACTIVE_INDICATOR",
                "TOOLTIP_BACKGROUND",
                "BORDER_THICKNESS", "CONTROL_HEIGHT", "CONTROL_GAP", "CONTROL_PADDING",
                "PANEL_PADDING", "GROUP_SPACING", "ICON_PADDING", "TOOLTIP_PADDING")));
        assertEquals(1, ScholarShellStyle.BORDER_THICKNESS);
        assertTrue(ScholarShellStyle.CONTROL_HEIGHT >= 18);
        assertTrue(ScholarShellStyle.TEXT_PRIMARY != ScholarShellStyle.TEXT_DISABLED);
        assertTrue(ScholarShellStyle.SELECTED_BACKGROUND != ScholarShellStyle.HOVER_BACKGROUND);
        assertTrue(ScholarShellStyle.SCIENTIFIC_ACCENT != ScholarShellStyle.PANEL_BACKGROUND);
    }
}
