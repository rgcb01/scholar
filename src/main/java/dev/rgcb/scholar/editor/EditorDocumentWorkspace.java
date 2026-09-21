package dev.rgcb.scholar.editor;

import dev.rgcb.scholar.persistence.PersistenceResult;
import java.util.Optional;

/** Lifecycle surface consumed by the editor shell; semantic editing remains in EditorSession. */
public interface EditorDocumentWorkspace {
    EditorSession session();
    Optional<String> name();
    boolean isDirty();
    PersistenceResult<String> save();
    PersistenceResult<String> saveAs(String name);

    default PersistenceResult<String> rename(String name) {
        return saveAs(name);
    }
}
