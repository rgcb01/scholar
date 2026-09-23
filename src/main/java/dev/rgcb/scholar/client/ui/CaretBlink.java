package dev.rgcb.scholar.client.ui;

import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.editor.EditorSelection;
import java.util.Objects;

/** Transient caret timing, using Minecraft's 300 ms on/off cadence. */
public final class CaretBlink {
    private static final long HALF_PERIOD_MS = 300;
    private Document lastDocument;
    private EditorSelection lastSelection;
    private long startedAt;
    private boolean wasActive;

    public boolean visible(long nowMillis, boolean active, Document document, EditorSelection selection) {
        if (!active) {
            wasActive = false;
            return false;
        }
        if (!wasActive || lastDocument != document || !Objects.equals(lastSelection, selection)
                || nowMillis < startedAt) {
            startedAt = nowMillis;
            lastDocument = document;
            lastSelection = selection;
        }
        wasActive = true;
        return ((nowMillis - startedAt) / HALF_PERIOD_MS) % 2 == 0;
    }
}
