package dev.rgcb.scholar.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ScholarHomeRecoveryBoundaryTest {
    @Test void homeUsesApplicationCandidateModelAndNeverParsesRecoveryStorage() throws Exception {
        var source = Files.readString(Path.of(
                "src/main/java/dev/rgcb/scholar/client/screen/ScholarHomeScreen.java"));

        assertTrue(source.contains("application.recoveryCandidates()"));
        assertTrue(source.contains("application.recover("));
        assertTrue(source.contains("application.discardRecovery("));
        assertTrue(source.contains("RecoveryCandidate"));
        assertFalse(source.contains("JsonParser"));
        assertFalse(source.contains("Files."));
        assertFalse(source.contains(".recovery.json"));
    }
}
