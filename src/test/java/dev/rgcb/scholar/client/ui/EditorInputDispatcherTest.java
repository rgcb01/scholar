package dev.rgcb.scholar.client.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class EditorInputDispatcherTest {
    private final EditorInputDispatcher dispatcher = new EditorInputDispatcher();

    @Test
    void firstHandledRouteOwnsTheKey() {
        var calls = new ArrayList<String>();

        var handled = dispatcher.dispatchKey(13, List.of(
                key -> record(calls, "modal", false),
                key -> record(calls, "context", true),
                key -> record(calls, "document", true)));

        assertTrue(handled);
        assertEquals(List.of("modal", "context"), calls);
    }

    @Test
    void unhandledKeyFallsThroughEveryRoute() {
        var calls = new ArrayList<String>();

        var handled = dispatcher.dispatchKey(27, List.of(
                key -> record(calls, "modal", false),
                key -> record(calls, "nested", false),
                key -> record(calls, "document", false)));

        assertFalse(handled);
        assertEquals(List.of("modal", "nested", "document"), calls);
    }

    private static boolean record(List<String> calls, String route, boolean handled) {
        calls.add(route);
        return handled;
    }
}
