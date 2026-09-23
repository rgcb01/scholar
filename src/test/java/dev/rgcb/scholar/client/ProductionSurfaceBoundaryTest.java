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

    @Test void productionHomeEditorAndTemperatureDifferenceAuthoringRemainReachable() throws Exception {
        assertTrue(Files.exists(MAIN_CLIENT.resolve("screen/ScholarHomeScreen.java")));
        assertTrue(Files.exists(MAIN_CLIENT.resolve("screen/ScholarEditorScreen.java")));
        assertTrue(Files.exists(MAIN_CLIENT.resolve("ui/ScholarRibbonModel.java")));
        var ribbon = Files.readString(MAIN_CLIENT.resolve("ui/ScholarRibbonModel.java"));
        assertTrue(ribbon.contains("INSERT_QUANTITY_CELSIUS_DIFFERENCE"));
        assertTrue(ribbon.contains("DATA_COLUMN_UNIT_CELSIUS_DIFFERENCE"));
        assertTrue(ribbon.contains("PLOT_Y_UNIT_KELVIN_DIFFERENCE"));
    }

    @Test void productionRibbonAuthorsAndEditsScientificComputationsWithoutDevCommands() throws Exception {
        var ribbon = Files.readString(MAIN_CLIENT.resolve("ui/ScholarRibbonModel.java"));
        var screen = Files.readString(MAIN_CLIENT.resolve("screen/ScholarEditorScreen.java"));
        var dialog = Files.readString(MAIN_CLIENT.resolve("screen/ScholarComputationDialog.java"));
        var actions = Files.readString(Path.of("src/main/java/dev/rgcb/scholar/editor/BuiltInEditorActions.java"));
        for (var id : new String[] { "INSERT_VARIABLE", "INSERT_COMPUTED_RESULT", "EDIT_VARIABLE", "EDIT_COMPUTED_RESULT" }) {
            assertTrue(ribbon.contains(id));
            assertTrue(screen.contains(id));
            assertTrue(actions.contains(id));
        }
        assertTrue(screen.contains("new ScholarComputationDialog(this, session, kind)"));
        assertTrue(dialog.contains("session.insertVariable("));
        assertTrue(dialog.contains("session.insertComputedResult("));
        assertTrue(dialog.contains("session.editVariable("));
        assertTrue(dialog.contains("session.editComputedResult("));
        assertFalse(dialog.contains("scholar_dev_"));
    }

    @Test void productionDataRibbonOpensAnalysisAuthoringAndFitOverlay() throws Exception {
        var ribbon = Files.readString(MAIN_CLIENT.resolve("ui/ScholarRibbonModel.java"));
        var screen = Files.readString(MAIN_CLIENT.resolve("screen/ScholarEditorScreen.java"));
        var dialog = Files.readString(MAIN_CLIENT.resolve("screen/ScholarAnalysisDialog.java"));
        assertTrue(ribbon.contains("DATA_INSERT_ANALYSIS"));
        assertTrue(ribbon.contains("DATA_EDIT_ANALYSIS"));
        assertTrue(ribbon.contains("DATA_ADD_FIT_OVERLAY"));
        assertTrue(screen.contains("new ScholarAnalysisDialog(this, session, kind)"));
        assertTrue(dialog.contains("session.insertAnalysis("));
        assertTrue(dialog.contains("session.editAnalysis("));
        assertTrue(dialog.contains("session.addFitOverlay("));
        assertFalse(dialog.contains("scholar_dev_"));
    }
}
