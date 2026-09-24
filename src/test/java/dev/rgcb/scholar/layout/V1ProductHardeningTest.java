package dev.rgcb.scholar.layout;

import dev.rgcb.scholar.client.DevelopmentStressDocument;
import dev.rgcb.scholar.client.DevelopmentStressDocument.Profile;
import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.Paragraph;
import dev.rgcb.scholar.document.Text;
import dev.rgcb.scholar.document.VariableDefinition;
import dev.rgcb.scholar.document.ComputedResult;
import dev.rgcb.scholar.document.DatasetAnalysisBlock;
import dev.rgcb.scholar.analysis.AnalysisKind;
import dev.rgcb.scholar.analysis.DatasetAnalysisEngine;
import dev.rgcb.scholar.compute.ExpressionParser;
import dev.rgcb.scholar.compute.ScientificValue;
import dev.rgcb.scholar.quantity.NumberNotation;
import dev.rgcb.scholar.editor.DocumentWorkspace;
import dev.rgcb.scholar.editor.EditorSession;
import dev.rgcb.scholar.editor.EditorHistory;
import dev.rgcb.scholar.interchange.PdfDocumentExporter;
import dev.rgcb.scholar.interchange.CsvDatasetInterchange;
import dev.rgcb.scholar.interchange.MarkdownDocumentExporter;
import dev.rgcb.scholar.persistence.DocumentJsonCodec;
import dev.rgcb.scholar.persistence.FileDocumentStorage;
import dev.rgcb.scholar.persistence.PersistenceResult;
import dev.rgcb.scholar.validation.DocumentValidator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.math.BigDecimal;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import static org.junit.jupiter.api.Assertions.*;

class V1ProductHardeningTest {
    @TempDir Path directory;

    @ParameterizedTest
    @EnumSource(value = Profile.class, names = {"LARGE", "DATA_HEAVY", "DIAGRAM_HEAVY", "MIXED_STRESS"})
    void repeatedSaveLoadOfStressProfilesHasNoSemanticDrift(Profile profile) {
        Document expected = DevelopmentStressDocument.create(profile);
        var storage = new FileDocumentStorage(directory);
        var started = System.nanoTime();
        for (var cycle = 0; cycle < 3; cycle++) {
            assertInstanceOf(PersistenceResult.Success.class, storage.save("cycle", expected));
            PersistenceResult<Document> result = new FileDocumentStorage(directory).load("cycle");
            assertTrue(result instanceof PersistenceResult.Success<Document>);
            var loaded = (PersistenceResult.Success<Document>) result;
            assertEquals(expected, loaded.value());
            assertTrue(DocumentValidator.validate(loaded.value()).isValid());
            expected = loaded.value();
        }
        System.out.printf("M38 persistence %s three_cycles_ms=%.3f%n", profile,
                (System.nanoTime() - started) / 1_000_000.0);
    }

    @Test
    void corruptOpenKeepsCurrentDirtyDocumentAndOriginalFileUntouched() throws Exception {
        var initial = DevelopmentStressDocument.create(Profile.SMALL);
        var storage = new FileDocumentStorage(directory);
        var workspace = new DocumentWorkspace(initial, storage);
        assertInstanceOf(PersistenceResult.Success.class, workspace.saveAs("current"));
        var original = Files.readAllBytes(directory.resolve("current.scholar.json"));
        assertTrue(workspace.session().typeText("unsaved"));
        var dirtyState = workspace.session().current();
        Files.writeString(directory.resolve("broken.scholar.json"), "{\"format\":\"scholar\",");

        assertInstanceOf(PersistenceResult.Failure.class, workspace.open("broken"));
        assertSame(dirtyState, workspace.session().current());
        assertEquals("current", workspace.name().orElseThrow());
        assertTrue(workspace.isDirty());
        assertArrayEquals(original, Files.readAllBytes(directory.resolve("current.scholar.json")));
    }

    @ParameterizedTest
    @EnumSource(value = Profile.class, names = {"LARGE", "DIAGRAM_HEAVY", "MIXED_STRESS"})
    void paginatedStressLayoutKeepsEveryBlockAcrossReflow(Profile profile) {
        var document = DevelopmentStressDocument.create(profile);
        var engine = new DocumentLayoutEngine();
        var started = System.nanoTime();
        var first = engine.layoutPaginated(document, EditorStressMeasurementTest.TEXT, EditorStressMeasurementTest.MATH);
        assertFalse(first.pages().isEmpty());
        assertEquals(document.blocks().size(), first.blocks().size());
        for (var width : List.of(480, 180, 24, 480)) {
            var viewport = engine.layout(document, width, EditorStressMeasurementTest.TEXT, EditorStressMeasurementTest.MATH);
            assertEquals(document.blocks().size(), viewport.blocks().size());
            assertTrue(viewport.height() >= 0);
        }
        assertEquals(first, engine.layoutPaginated(document, EditorStressMeasurementTest.TEXT, EditorStressMeasurementTest.MATH));
        System.out.printf("M38 paginated %s pages=%d layout_reflow_ms=%.3f%n", profile,
                first.pages().size(), (System.nanoTime() - started) / 1_000_000.0);
    }

    @Test
    void mixedScientificPdfExportsAllDerivedPages() throws Exception {
        var document = DevelopmentStressDocument.create(Profile.MIXED_STRESS);
        var layout = new DocumentLayoutEngine().layoutPaginated(document,
                EditorStressMeasurementTest.TEXT, EditorStressMeasurementTest.MATH);
        var started = System.nanoTime();
        var bytes = new PdfDocumentExporter().export(document, layout);
        try (var pdf = Loader.loadPDF(bytes)) {
            assertEquals(layout.pages().size(), pdf.getNumberOfPages());
        }
        System.out.printf("M38 PDF MIXED_STRESS pages=%d bytes=%d export_ms=%.3f%n",
                layout.pages().size(), bytes.length, (System.nanoTime() - started) / 1_000_000.0);
    }

    @Test
    void unicodeScienceTextSurvivesNativeMarkdownPdfAndCsvPaths() throws Exception {
        var text = "Café á é í ó ú ñ ü; α β γ Δ λ μ Ω; ± × − ° ∞ ≈ ≤ ≥";
        var document = new Document(List.of(new Paragraph(new InlineContent(List.of(new Text(text, Set.of()))))));
        var codec = new DocumentJsonCodec();
        var encoded = (PersistenceResult.Success<String>) codec.encode(document);
        assertEquals(document, ((PersistenceResult.Success<Document>) codec.decode(encoded.value())).value());
        assertTrue(new MarkdownDocumentExporter().export(document).contains(text));

        var csv = new CsvDatasetInterchange();
        var preview = csv.preview("Label,Value\n\"" + text + "\",1\n");
        assertEquals(preview.rows(), csv.preview(csv.export(preview.dataset("science", "Science"))).rows());

        var layout = new DocumentLayoutEngine().layoutPaginated(document,
                EditorStressMeasurementTest.TEXT, EditorStressMeasurementTest.MATH);
        try (var pdf = Loader.loadPDF(new PdfDocumentExporter().export(document, layout))) {
            var extracted = new PDFTextStripper().getText(pdf);
            assertTrue(extracted.contains("Café"));
            assertTrue(extracted.contains("Δ"));
        }
    }

    @Test
    void mixedStressWithVariablesComputationAndAnalysisSurvivesFileAndLayout() {
        var base = DevelopmentStressDocument.create(Profile.MIXED_STRESS);
        var variable = new VariableDefinition("m38-variable", "testmass", new ScientificValue.Scalar(new BigDecimal("2.5")));
        var blocks = new ArrayList<>(base.blocks());
        blocks.add(variable);
        var withVariable = new Document(blocks, base.datasets(), base.settings());
        blocks.add(new ComputedResult(new ExpressionParser().parse("testmass*2", withVariable).expression(), "testmass*2"));
        var dataset = base.datasets().getFirst();
        var numericColumn = dataset.columns().stream()
                .filter(column -> column.type() == dev.rgcb.scholar.data.DatasetColumnType.NUMBER)
                .findFirst().orElseThrow();
        blocks.add(new DatasetAnalysisBlock("m38-analysis", dataset.id(), AnalysisKind.DESCRIPTIVE,
                Optional.empty(), numericColumn.id(), Optional.empty(), NumberNotation.DECIMAL));
        var source = new Document(blocks, base.datasets(), base.settings());
        assertTrue(DocumentValidator.validate(source).isValid());
        var storage = new FileDocumentStorage(directory);
        assertInstanceOf(PersistenceResult.Success.class, storage.save("modern", source));
        var result = storage.load("modern");
        assertTrue(result instanceof PersistenceResult.Success<Document>);
        var restored = ((PersistenceResult.Success<Document>) result).value();
        assertEquals(source, restored);
        var layout = new DocumentLayoutEngine().layoutPaginated(restored,
                EditorStressMeasurementTest.TEXT, EditorStressMeasurementTest.MATH);
        assertEquals(restored.blocks().size(), layout.blocks().size());
        assertTrue(layout.blocks().stream().anyMatch(block -> block.kind() == LaidOutBlockKind.ANALYSIS));
    }

    @Test
    void fiveHundredEditsKeepBoundedHistoryAndExactRedoState() {
        var source = DevelopmentStressDocument.create(Profile.SMALL);
        var firstParagraph = java.util.stream.IntStream.range(0, source.blocks().size())
                .filter(index -> source.blocks().get(index) instanceof Paragraph).findFirst().orElseThrow();
        var session = new EditorSession(source, firstParagraph);
        for (var step = 0; step < 500; step++) {
            assertTrue(session.typeText("x"));
            session.setCurrent(session.current());
            if (step % 50 == 0) assertTrue(DocumentValidator.validate(session.current().document()).isValid());
        }
        var finalState = session.current();
        assertEquals(EditorHistory.DEFAULT_CAPACITY, session.undoDepth());
        for (var step = 0; step < EditorHistory.DEFAULT_CAPACITY; step++) assertTrue(session.undo());
        assertFalse(session.canUndo());
        for (var step = 0; step < EditorHistory.DEFAULT_CAPACITY; step++) assertTrue(session.redo());
        assertEquals(finalState, session.current());
        assertTrue(DocumentValidator.validate(session.current().document()).isValid());
    }

    @Test
    void dataHeavyAnalysisAndCsvRoundTripRemainUsable() {
        var document = DevelopmentStressDocument.create(Profile.DATA_HEAVY);
        var dataset = document.datasets().getFirst();
        var column = dataset.columns().stream()
                .filter(value -> value.type() == dev.rgcb.scholar.data.DatasetColumnType.NUMBER)
                .findFirst().orElseThrow();
        var analysis = new DatasetAnalysisBlock("m38-heavy-analysis", dataset.id(), AnalysisKind.DESCRIPTIVE,
                Optional.empty(), column.id(), Optional.empty(), NumberNotation.DECIMAL);
        var started = System.nanoTime();
        var outcome = new DatasetAnalysisEngine().evaluate(document, analysis);
        assertTrue(outcome.result().isPresent(), () -> outcome.diagnostics().toString());
        var analysisMs = (System.nanoTime() - started) / 1_000_000.0;

        var interchange = new CsvDatasetInterchange();
        started = System.nanoTime();
        var exported = interchange.export(dataset);
        var imported = interchange.preview(exported);
        assertEquals(dataset.rows().size(), imported.rows().size());
        for (var index = 0; index < dataset.rows().size(); index++) {
            assertEquals(dataset.rows().get(index).values(), imported.rows().get(index).values());
        }
        System.out.printf("M38 data_heavy rows=%d analysis_ms=%.3f csv_roundtrip_ms=%.3f%n",
                dataset.rows().size(), analysisMs, (System.nanoTime() - started) / 1_000_000.0);
    }
}
