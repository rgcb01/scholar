package dev.rgcb.scholar.client.ui;

import static org.junit.jupiter.api.Assertions.*;

import dev.rgcb.scholar.editor.EditorActionId;
import org.junit.jupiter.api.Test;

class ScholarScreenActionShortcutsTest {
    @Test void lookupIsTotalForFileAndViewportActions() {
        assertEquals("Ctrl+N", ScholarScreenActionShortcuts.forAction(EditorActionId.FILE_NEW)
                .orElseThrow().displayText());
        assertEquals("Ctrl+Shift+S", ScholarScreenActionShortcuts.forAction(EditorActionId.FILE_SAVE_AS)
                .orElseThrow().displayText());
        assertTrue(ScholarScreenActionShortcuts.forAction(EditorActionId.FILE_RENAME).isEmpty());
        assertTrue(ScholarScreenActionShortcuts.forAction(EditorActionId.VIEW_ZOOM_OUT).isEmpty());
        assertTrue(ScholarScreenActionShortcuts.forAction(EditorActionId.VIEW_ZOOM_IN).isEmpty());
        assertTrue(ScholarScreenActionShortcuts.forAction(EditorActionId.VIEW_FIT_PAGE).isEmpty());
        assertTrue(ScholarScreenActionShortcuts.forAction(EditorActionId.VIEW_FIT_WIDTH).isEmpty());
    }
}
