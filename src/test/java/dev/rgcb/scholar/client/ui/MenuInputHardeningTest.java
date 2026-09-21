package dev.rgcb.scholar.client.ui;

import dev.rgcb.scholar.client.editor.ScholarEditorController;
import dev.rgcb.scholar.document.*;
import dev.rgcb.scholar.editor.*;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MenuInputHardeningTest {
    // GLFW key values; the headless test runtime does not load native libraries.
    private static final int DELETE = 261, DOWN = 264, ENTER = 257, ESCAPE = 256;

    @Test void narrowMenuStripRemainsReachableByWheelAndKeyboardAfterResize() {
        var count = new AtomicInteger();
        var session = new EditorSession(new Document(List.of(new Paragraph(new InlineContent(List.of())))), 0);
        var controller = new ScholarEditorController(session, new ClipboardAdapter() {
            public String getText() { return ""; }
            public boolean setText(String text) { return true; }
        }, () -> { }, () -> { });
        var definitions = java.util.stream.IntStream.range(0, 12).mapToObj(i -> new MenuDefinition("Menu" + i,
                List.of(MenuEntry.action(action(true, count))))).toList();
        var menu = new MenuBarWidget(controller, definitions);
        menu.setViewportSize(320, 180);
        for (var i = 0; i < 30; i++) { assertTrue(menu.mouseScrolled(100, 5, -1)); }
        assertTrue(menu.mouseClicked(300, 5));
        assertTrue(menu.isOpen());
        menu.setViewportSize(800, 600);
        assertTrue(menu.keyPressed(262));
        assertTrue(menu.keyPressed(ENTER));
        assertEquals(1, count.get());
        assertEquals(0, session.undoDepth());
    }
    @Test void openMenuConsumesNestedEditorKeysAndEnterExecutesOnlyEnabledAction() {
        var count = new AtomicInteger();
        var session = new EditorSession(new Document(List.of(new Paragraph(new InlineContent(List.of())))), 0);
        var clipboard = new ClipboardAdapter() {
            public String getText() { return ""; }
            public boolean setText(String text) { return true; }
        };
        var controller = new ScholarEditorController(session, clipboard, () -> { }, () -> { });
        var menu = new MenuBarWidget(controller, List.of(new MenuDefinition("Edit", List.of(
                MenuEntry.action(action(false, count)), MenuEntry.separator(), MenuEntry.action(action(true, count))))));
        menu.setViewportSize(320, 240);
        assertTrue(menu.mouseClicked(8, 5));
        assertTrue(menu.keyPressed(DELETE));
        assertEquals(0, count.get());
        assertTrue(menu.keyPressed(DOWN));
        assertTrue(menu.keyPressed(ENTER));
        assertEquals(1, count.get());
        assertFalse(menu.isOpen());
        assertEquals(0, session.undoDepth());
        assertFalse(menu.keyPressed(ENTER));
        menu.mouseClicked(8, 5);
        assertTrue(menu.keyPressed(ESCAPE));
        assertFalse(menu.isOpen());
    }

    private static EditorAction action(boolean enabled, AtomicInteger count) {
        return new EditorAction() {
            public EditorActionId id() { return EditorActionId.COPY; }
            public String label() { return "Copy"; }
            public Optional<ActionShortcut> shortcut() { return Optional.empty(); }
            public boolean isEnabled(EditorActionContext context) { return enabled; }
            public EditorActionResult execute(EditorActionContext context) { count.incrementAndGet(); return EditorActionResult.NONE; }
        };
    }
}
