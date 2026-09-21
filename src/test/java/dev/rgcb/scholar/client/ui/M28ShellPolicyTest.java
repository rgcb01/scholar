package dev.rgcb.scholar.client.ui;

import dev.rgcb.scholar.editor.ActionShortcut;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class M28ShellPolicyTest {
    private static final int KEY_Y = 89;
    private static final int KEY_Z = 90;
    private static final int MOD_SHIFT = 0x0001;
    private static final int MOD_CONTROL = 0x0002;

    @Test
    void shortcutDisplayAndMatchingUseTheSameSemanticStrokes() {
        var redo = ActionShortcut.of(
                new ActionShortcut.Stroke(ActionShortcut.Key.Y, true, false),
                new ActionShortcut.Stroke(ActionShortcut.Key.Z, true, true));

        assertEquals("Ctrl+Y", redo.displayText());
        assertTrue(MinecraftShortcutMatcher.matches(redo, KEY_Y, MOD_CONTROL));
        assertTrue(MinecraftShortcutMatcher.matches(redo, KEY_Z, MOD_CONTROL | MOD_SHIFT));
        assertFalse(MinecraftShortcutMatcher.matches(redo, KEY_Z, MOD_CONTROL));
    }

    @Test
    void centeredModalIsClampedInsideNarrowViewport() {
        var rect = ModalGeometry.centered(180, 120, 360, 220, 24);

        assertTrue(rect.x() >= 0);
        assertTrue(rect.y() >= 24);
        assertTrue(rect.right() <= 180);
        assertTrue(rect.bottom() <= 120);
    }

    @Test
    void contextMenuRemainsCompactAndViewportClamped() {
        var rect = ContextMenuLayout.compute(170, 110, 180, 120, 30);

        assertTrue(rect.right() <= 180);
        assertTrue(rect.bottom() <= 120);
        assertTrue(rect.height() <= 120);
        assertTrue(ContextMenuLayout.visibleRowCount(120, 30) < 30);
    }
}
