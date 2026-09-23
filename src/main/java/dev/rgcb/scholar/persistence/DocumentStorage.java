package dev.rgcb.scholar.persistence;

import dev.rgcb.scholar.document.Document;
import java.util.List;

public interface DocumentStorage {
    PersistenceResult<String> save(String name, Document document);
    PersistenceResult<Document> load(String name);
    PersistenceResult<List<String>> list();
    PersistenceResult<Boolean> delete(String name);
}
