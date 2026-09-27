package dev.rgcb.scholar.client.ui;

import java.util.List;
import java.util.Objects;

/** Applies ordered UI-key precedence before document-level input is considered. */
public final class EditorInputDispatcher {
    @FunctionalInterface
    public interface KeyRoute {
        boolean handle(int keyCode);
    }

    public boolean dispatchKey(int keyCode, List<KeyRoute> routes) {
        Objects.requireNonNull(routes, "routes");
        for (var route : routes) {
            if (Objects.requireNonNull(route, "route").handle(keyCode)) return true;
        }
        return false;
    }
}
