package dev.rgcb.scholar.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ProductionSurfaceBoundaryTest {
    private static final Path MAIN_CLIENT = Path.of("src/main/java/dev/rgcb/scholar/client");
    private static final Path TEST_CLIENT = Path.of("src/test/java/dev/rgcb/scholar/client");

    @Test void productionRegistersOneScholarApplicationCommand() throws Exception {
        var commands = Files.readString(MAIN_CLIENT.resolve("ProductionClientCommands.java"));
        var client = Files.readString(MAIN_CLIENT.resolve("ScholarClient.java"));

        assertTrue(commands.contains("Commands.literal(\"scholar\")"));
        assertTrue(commands.contains("ScholarHomeScreen.create()"));
        assertTrue(client.contains("ProductionClientCommands.register(event)"));
        assertFalse(client.contains("DevelopmentClientCommands"));
    }

    @Test void productionSourcesExposeNoDevelopmentCommandsOrScreens() throws Exception {
        try (var files = Files.walk(Path.of("src/main/java"))) {
            for (var source : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                var text = Files.readString(source);
                assertFalse(text.contains("scholar_dev_"), source + " exposes a development command");
                assertFalse(text.contains("createDevelopmentScreen"), source + " exposes a development screen factory");
            }
        }

        assertFalse(Files.exists(MAIN_CLIENT.resolve("DevelopmentClientCommands.java")));
        assertFalse(Files.exists(MAIN_CLIENT.resolve("screen/ScholarDocumentScreen.java")));
    }

    @Test void visualQaFixturesRemainTestsInsteadOfRuntimeCommands() {
        assertFalse(Files.exists(MAIN_CLIENT.resolve("DevelopmentDocument.java")));
        assertFalse(Files.exists(MAIN_CLIENT.resolve("DevelopmentStressDocument.java")));
        assertTrue(Files.exists(TEST_CLIENT.resolve("DevelopmentDocument.java")));
        assertTrue(Files.exists(TEST_CLIENT.resolve("DevelopmentStressDocument.java")));
    }

    @Test void productionHomeAndEditorRemainReachable() {
        assertTrue(Files.exists(MAIN_CLIENT.resolve("screen/ScholarHomeScreen.java")));
        assertTrue(Files.exists(MAIN_CLIENT.resolve("screen/ScholarEditorScreen.java")));
        assertTrue(Files.exists(MAIN_CLIENT.resolve("ui/ScholarRibbonModel.java")));
    }
}
