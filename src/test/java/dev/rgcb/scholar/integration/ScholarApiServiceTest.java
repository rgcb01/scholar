package dev.rgcb.scholar.integration;

import static org.junit.jupiter.api.Assertions.*;

import dev.rgcb.scholar.api.ScholarApiException;
import dev.rgcb.scholar.api.data.ScholarData;
import dev.rgcb.scholar.api.quantity.ScholarQuantity;
import dev.rgcb.scholar.application.FileScholarDocumentRepository;
import dev.rgcb.scholar.application.ScholarApplication;
import dev.rgcb.scholar.application.ScholarDocumentId;
import dev.rgcb.scholar.document.DatasetAnalysisBlock;
import dev.rgcb.scholar.document.FigureBlock;
import dev.rgcb.scholar.document.TableBlock;
import dev.rgcb.scholar.document.VariableDefinition;
import dev.rgcb.scholar.persistence.PersistenceResult;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ScholarApiServiceTest {
    @TempDir Path root;

    private ScholarApplication application() { return new ScholarApplication(new FileScholarDocumentRepository(root)); }

    @Test void compoundEditIsOneUndoEntryAndOrdinaryContentPersists() {
        var app = application();
        var api = new ScholarApiService(app, () -> true);
        assertEquals(1, api.majorVersion());
        var handle = api.documents().create();
        var id = handle.id();
        assertTrue(handle.edit(edit -> {
            var dataset = edit.createDataset("Free Fall", List.of(
                    ScholarData.ColumnSpec.number("Time", "s"), ScholarData.ColumnSpec.number("Distance", "m")));
            var columns = edit.columns(dataset);
            edit.appendRows(dataset, List.of(row("0", "0"), row("0.2", "0.20"), row("0.4", "0.79")));
            edit.insertDatasetTable(dataset);
            edit.insertFigurePlot(dataset, columns.get(0).id(), columns.get(1).id(), "Free fall", "Distance over time");
            edit.requestAnalysis(dataset, ScholarData.Analysis.QUADRATIC_FIT,
                    Optional.of(columns.get(0).id()), columns.get(1).id());
            edit.defineVariable("gravity", ScholarQuantity.linear(new BigDecimal("9.81"), "m/s^2"));
        }));
        var workspace = ((PersistenceResult.Success<dev.rgcb.scholar.application.ApplicationDocumentWorkspace>)
                app.openActiveDocument(new ScholarDocumentId(id.value()))).value();
        assertEquals(1, workspace.session().undoDepth());
        assertTrue(handle.isDirty());
        assertEquals(3, handle.datasets().getFirst().rows().size());
        assertTrue(workspace.session().current().document().blocks().stream().anyMatch(FigureBlock.class::isInstance));
        assertTrue(workspace.session().current().document().blocks().stream().anyMatch(TableBlock.class::isInstance));
        assertTrue(workspace.session().current().document().blocks().stream().anyMatch(DatasetAnalysisBlock.class::isInstance));
        assertTrue(workspace.session().current().document().blocks().stream().anyMatch(VariableDefinition.class::isInstance));
        assertTrue(workspace.session().undo());
        assertTrue(handle.datasets().isEmpty());
        assertTrue(workspace.session().redo());
        assertEquals(3, handle.datasets().getFirst().rows().size());
        handle.save();
        assertFalse(handle.isDirty());
        app.closeWorkspace(workspace);
        var reopened = new ScholarApiService(new ScholarApplication(new FileScholarDocumentRepository(root)), () -> true)
                .documents().open(id).orElseThrow();
        assertEquals(3, reopened.datasets().getFirst().rows().size());
        assertEquals(1, reopened.variables().size());
    }

    @Test void failedAndNoOpEditsNeverTouchHistoryOrDirtyState() {
        var app = application();
        var api = new ScholarApiService(app, () -> true);
        var handle = api.documents().create();
        var workspace = ((PersistenceResult.Success<dev.rgcb.scholar.application.ApplicationDocumentWorkspace>)
                app.openActiveDocument(new ScholarDocumentId(handle.id().value()))).value();
        assertFalse(handle.edit(edit -> {}));
        var failure = assertThrows(ScholarApiException.class, () -> handle.edit(edit -> {
            var dataset = edit.createDataset("Test", List.of(ScholarData.ColumnSpec.number("Time", "s")));
            edit.appendRows(dataset, List.of(List.of(ScholarData.Cell.text("wrong"))));
        }));
        assertEquals(ScholarApiException.Code.INVALID_INPUT, failure.code());
        assertEquals(0, workspace.session().undoDepth());
        assertFalse(handle.isDirty());
        assertTrue(handle.datasets().isEmpty());
    }

    @Test void measurementIngestionConvertsUnitsAndRejectsIncompatibleValues() {
        var api = new ScholarApiService(application(), () -> true);
        var handle = api.documents().create();
        handle.edit(edit -> {
            var dataset = edit.createDataset("Lengths", List.of(ScholarData.ColumnSpec.number("Distance", "m")));
            edit.appendMeasurement(dataset, Map.of("column", ScholarQuantity.linear(new BigDecimal("250"), "cm")));
        });
        assertEquals(0, new BigDecimal("2.5").compareTo(handle.datasets().getFirst().rows().getFirst().getFirst().number().orElseThrow()));
        var depth = handle.datasets().getFirst().rows().size();
        assertThrows(ScholarApiException.class, () -> handle.edit(edit ->
                edit.appendMeasurement("dataset", Map.of("column", ScholarQuantity.linear(BigDecimal.ONE, "s")))));
        assertEquals(depth, handle.datasets().getFirst().rows().size());
        assertEquals(0, api.units().convert(new ScholarQuantity(new BigDecimal("20"), "°C",
                ScholarQuantity.Semantics.ABSOLUTE_TEMPERATURE), "K").value().compareTo(new BigDecimal("293.15")));
        assertEquals(0, api.units().convert(new ScholarQuantity(new BigDecimal("5"), "°C",
                ScholarQuantity.Semantics.TEMPERATURE_DIFFERENCE), "K").value().compareTo(new BigDecimal("5")));
    }

    @Test void lifecycleEventsAreSemanticAndThreadGuardRejectsMutation() throws Exception {
        var app = application();
        var api = new ScholarApiService(app, () -> true);
        var events = new ArrayList<dev.rgcb.scholar.api.event.ScholarEvents.Event>();
        try (var subscription = api.events().subscribe(events::add)) {
            var document = api.documents().create();
            assertEquals(1, events.size());
            document.datasets();
            assertEquals(1, events.size());
            document.edit(edit -> edit.defineVariable("x", ScholarQuantity.linear(BigDecimal.ONE, "m")));
            assertEquals(2, events.size());
            var workspace = ((PersistenceResult.Success<dev.rgcb.scholar.application.ApplicationDocumentWorkspace>)
                    app.openActiveDocument(new ScholarDocumentId(document.id().value()))).value();
            api.documents().open(document.id());
            workspace.session().computations();
            assertEquals(2, events.size());
            workspace.session().typeText("sample");
            assertEquals(3, events.size());
            document.save();
            assertEquals(4, events.size());
            app.closeWorkspace(workspace);
            assertEquals(5, events.size());
            var guarded = new ScholarApiService(app, () -> false);
            assertEquals(ScholarApiException.Code.INVALID_THREAD,
                    assertThrows(ScholarApiException.class, () -> guarded.documents().create()).code());
        }
    }

    @Test void bulkRowsUseOneHistoryEntryAndNoOpCellWriteUsesNone() {
        var app = application();
        var api = new ScholarApiService(app, () -> true);
        var handle = api.documents().create();
        handle.edit(edit -> edit.createDataset("Readings", List.of(ScholarData.ColumnSpec.number("Time", "s"))));
        var workspace = ((PersistenceResult.Success<dev.rgcb.scholar.application.ApplicationDocumentWorkspace>)
                app.openActiveDocument(new ScholarDocumentId(handle.id().value()))).value();
        var before = workspace.session().undoDepth();
        assertTrue(handle.edit(edit -> edit.appendRows("dataset", List.of(
                List.of(ScholarData.Cell.number(BigDecimal.ONE)),
                List.of(ScholarData.Cell.missing())))));
        assertEquals(before + 1, workspace.session().undoDepth());
        assertEquals(ScholarData.CellKind.MISSING, handle.datasets().getFirst().rows().get(1).getFirst().kind());
        assertFalse(handle.edit(edit -> edit.setCell("dataset", 0, "column", ScholarData.Cell.number(BigDecimal.ONE))));
        assertEquals(before + 1, workspace.session().undoDepth());
    }

    @Test void activeOpenAndRenameKeepDocumentAndResourceIdentities() {
        var app = application();
        var api = new ScholarApiService(app, () -> true);
        var handle = api.documents().create();
        var docId = handle.id();
        handle.edit(edit -> edit.createDataset("Experiment", List.of(ScholarData.ColumnSpec.number("Time", "s"))));
        var workspace = ((PersistenceResult.Success<dev.rgcb.scholar.application.ApplicationDocumentWorkspace>)
                app.openActiveDocument(new ScholarDocumentId(docId.value()))).value();
        assertSame(workspace, ((PersistenceResult.Success<dev.rgcb.scholar.application.ApplicationDocumentWorkspace>)
                app.openActiveDocument(new ScholarDocumentId(docId.value()))).value());
        assertEquals(1, workspace.session().undoDepth());
        assertEquals(docId, api.documents().open(docId).orElseThrow().id());
        assertInstanceOf(PersistenceResult.Success.class, workspace.rename("Renamed experiment"));
        assertEquals(docId, handle.id());
        assertEquals("dataset", handle.datasets().getFirst().id());
        assertTrue(api.documents().list().stream().anyMatch(summary -> summary.id().equals(docId) && summary.dirty()));
        assertTrue(api.documents().list().getFirst().createdAtEpochMillis() > 0);
    }

    @Test void temperatureColumnRequiresExplicitSemantics() {
        var api = new ScholarApiService(application(), () -> true);
        var handle = api.documents().create();
        assertThrows(ScholarApiException.class, () -> handle.edit(edit -> edit.createDataset("Temperature",
                List.of(ScholarData.ColumnSpec.number("Temperature", "°C")))));
        assertFalse(handle.isDirty());
        assertTrue(handle.edit(edit -> edit.createDataset("Temperature", List.of(new ScholarData.ColumnSpec(
                "Increase", ScholarData.ColumnType.NUMBER, Optional.of("°C"),
                ScholarQuantity.Semantics.TEMPERATURE_DIFFERENCE)))));
        assertEquals(ScholarQuantity.Semantics.TEMPERATURE_DIFFERENCE,
                handle.datasets().getFirst().columns().getFirst().semantics());
    }

    private static List<ScholarData.Cell> row(String time, String distance) {
        return List.of(ScholarData.Cell.number(new BigDecimal(time)), ScholarData.Cell.number(new BigDecimal(distance)));
    }
}
