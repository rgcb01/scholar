package dev.rgcb.scholar.api.document;

import java.util.List;
import java.util.Optional;

/** Document library operations. IDs are opaque: obtain them from create/list, not by guessing. */
public interface ScholarDocuments {
    record Id(String value) { public Id { if (value == null || value.isBlank()) throw new IllegalArgumentException("Blank document ID"); } }
    record Summary(Id id, String name, long createdAtEpochMillis, long modifiedAtEpochMillis, boolean dirty) {}

    /** Lists persisted documents without opening or changing them. Client thread only. */
    List<Summary> list();
    /** Creates and opens a blank Scholar document. Client thread only. */
    ScholarDocument create();
    /** Opens a listed document; repeated opens share the active editor workspace. Client thread only. */
    Optional<ScholarDocument> open(Id id);
}
