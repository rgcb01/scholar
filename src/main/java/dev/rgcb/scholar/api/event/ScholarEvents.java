package dev.rgcb.scholar.api.event;

import dev.rgcb.scholar.api.document.ScholarDocuments;
import java.util.function.Consumer;

/** Semantic lifecycle notifications on the client thread; not editor gesture events. */
public interface ScholarEvents {
    enum Kind { DOCUMENT_OPENED, DOCUMENT_CHANGED, DOCUMENT_SAVED, DOCUMENT_CLOSED }
    record Event(Kind kind, ScholarDocuments.Id documentId) {}
    /** The returned subscription removes this listener when closed. */
    AutoCloseable subscribe(Consumer<Event> listener);
}
