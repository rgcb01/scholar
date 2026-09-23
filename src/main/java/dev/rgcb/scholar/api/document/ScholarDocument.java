package dev.rgcb.scholar.api.document;

import dev.rgcb.scholar.api.data.ScholarData;
import java.util.List;
import java.util.function.Consumer;

/** An open document. All mutations are client-thread operations and use Scholar history. */
public interface ScholarDocument {
    ScholarDocuments.Id id();
    String name();
    boolean isDirty();
    List<ScholarData.Dataset> datasets();
    List<ScholarData.Variable> variables();

    /** Stages all operations, validates the resulting document, then commits once. Failure/no-op commits nothing. */
    boolean edit(Consumer<ScholarEdit> operation);
    /** Persists the normal Scholar document format; this does not create a history entry. */
    void save();
}
