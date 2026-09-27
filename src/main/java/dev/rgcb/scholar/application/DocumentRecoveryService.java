package dev.rgcb.scholar.application;

import dev.rgcb.scholar.persistence.PersistenceResult;
import java.util.List;

/** Storage boundary for unconfirmed working state. */
public interface DocumentRecoveryService {
    PersistenceResult<RecoveryCandidate> capture(RecoverySnapshot snapshot);
    PersistenceResult<List<RecoveryCandidate>> discover();
    PersistenceResult<RecoverySnapshot> load(RecoveryId id);
    PersistenceResult<Boolean> discard(RecoveryId id);
    PersistenceResult<Boolean> discardForDocument(ScholarDocumentId id);
}
