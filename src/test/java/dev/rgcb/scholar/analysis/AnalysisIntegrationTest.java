package dev.rgcb.scholar.analysis;

import static org.junit.jupiter.api.Assertions.*;

import dev.rgcb.scholar.data.*;
import dev.rgcb.scholar.document.*;
import dev.rgcb.scholar.persistence.DocumentJsonCodec;
import dev.rgcb.scholar.persistence.PersistenceResult;
import dev.rgcb.scholar.plot.*;
import dev.rgcb.scholar.quantity.NumberNotation;
import dev.rgcb.scholar.quantity.UnitParser;
import dev.rgcb.scholar.transfer.*;
import dev.rgcb.scholar.validation.DocumentDiagnosticCode;
import dev.rgcb.scholar.validation.DocumentValidator;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AnalysisIntegrationTest {
    private static final DatasetAnalysisBlock ANALYSIS = new DatasetAnalysisBlock("fit-1", "data", AnalysisKind.QUADRATIC_FIT,
            Optional.of("t"), "d", Optional.empty(), NumberNotation.SCIENTIFIC);

    private static ScientificDataset dataset(String finalDistance) {
        var units = new UnitParser();
        return new ScientificDataset("data", "Motion", List.of(
                new DatasetColumn("t", "Time", DatasetColumnType.NUMBER, Optional.of(units.parseRequired("s"))),
                new DatasetColumn("d", "Distance", DatasetColumnType.NUMBER, Optional.of(units.parseRequired("m")))),
                List.of(row("0", "0"), row("1", "1"), row("2", "4"), row("3", finalDistance)));
    }

    private static DatasetRow row(String x, String y) {
        return new DatasetRow(List.of(DatasetValue.number(x), DatasetValue.number(y)));
    }

    private static PlotBlock plot() {
        return new PlotBlock(PlotDefinition.of("Motion", AxisDefinition.linear("Time"), AxisDefinition.linear("Distance"),
                List.of(new PlotSeries("Data", PlotSeriesKind.SCATTER,
                        new DatasetPlotBinding("data", "t", "d")), PlotSeries.fit("Quadratic", "fit-1"))));
    }

    @Test void authoredOnlyRoundTripAndDerivedCurveUpdatesWithSource() {
        var document = new Document(List.of(ANALYSIS, plot()), List.of(dataset("9")));
        var codec = new DocumentJsonCodec();
        var encoded = codec.encode(document);
        assertInstanceOf(PersistenceResult.Success.class, encoded, () -> encoded.diagnostics().toString());
        var json = (String) ((PersistenceResult.Success<?>) encoded).value();
        assertTrue(json.contains("fitAnalysisId"));
        assertFalse(json.contains("rSquared"));
        var restored = (Document) assertInstanceOf(PersistenceResult.Success.class, codec.decode(json)).value();
        assertEquals(document, restored);
        var resolver = new DatasetPlotResolver();
        var first = resolver.resolve(restored, (PlotBlock) restored.blocks().get(1)).definition().series().get(1).points();
        assertEquals(65, first.size());
        assertEquals(2, restored.blocks().size());
        var changed = new Document(restored.blocks(), List.of(dataset("10")), restored.settings());
        var second = resolver.resolve(changed, (PlotBlock) changed.blocks().get(1)).definition().series().get(1).points();
        assertNotEquals(first, second);
        assertEquals(document.blocks(), changed.blocks());
    }

    @Test void missingAnalysisAndDatasetProduceDiagnosticsWithoutFabrication() {
        var missing = new Document(List.of(ANALYSIS, plot()));
        assertTrue(DocumentValidator.validate(missing).diagnostics().stream()
                .anyMatch(d -> d.code() == DocumentDiagnosticCode.MISSING_ANALYSIS_DEPENDENCY));
        assertTrue(new DatasetPlotResolver().resolve(missing, plot()).definition().series().get(1).points().isEmpty());
    }

    @Test void transferRemapsTravelingAnalysisAndRejectsUnprovenFit() {
        var source = new Document(List.of(ANALYSIS, plot()), List.of(dataset("9")));
        var token = RuntimeDocumentToken.create();
        var extracted = assertInstanceOf(ExtractionResult.Success.class, new FragmentExtractor().extract(source,
                new FragmentExtractionRequest.Blocks(List.of(0, 1)), Optional.of(token)));
        var destination = new Document(List.of(ANALYSIS), List.of(dataset("100")));
        var planning = new TransferPlanner().plan(extracted.fragment(),
                new TransferContext(destination, Optional.of(RuntimeDocumentToken.create()), extracted.sourceMetadata()));
        var plan = assertInstanceOf(PlanningResult.Success.class, planning).plan();
        var materialized = assertInstanceOf(MaterializationResult.Success.class,
                new TransferMaterializer().materialize(extracted.fragment(), plan)).transfer();
        var roots = assertInstanceOf(FragmentContent.Blocks.class, materialized.content()).roots();
        var remapped = (DatasetAnalysisBlock) roots.get(0);
        var fit = (PlotBlock) roots.get(1);
        assertNotEquals(ANALYSIS.id(), remapped.id());
        assertNotEquals(ANALYSIS.datasetId(), remapped.datasetId());
        assertEquals(remapped.id(), fit.definition().series().get(1).fitAnalysisId().orElseThrow());
        assertEquals(remapped.datasetId(), fit.definition().series().get(0).datasetBinding().orElseThrow().datasetId());

        var fitOnly = assertInstanceOf(ExtractionResult.Success.class, new FragmentExtractor().extract(source,
                new FragmentExtractionRequest.Blocks(List.of(1)), Optional.of(token)));
        var rejected = assertInstanceOf(PlanningResult.Failure.class, new TransferPlanner().plan(fitOnly.fragment(),
                new TransferContext(destination, Optional.of(RuntimeDocumentToken.create()), fitOnly.sourceMetadata())));
        assertEquals(TransferDiagnostic.Code.UNRESOLVED_EXTERNAL_ANALYSIS_DEPENDENCY, rejected.diagnostics().getFirst().code());
    }

    @Test void sameDocumentFitOnlyKeepsProvenAnalysisDependency() {
        var source = new Document(List.of(ANALYSIS, plot()), List.of(dataset("9")));
        var token = RuntimeDocumentToken.create();
        var extracted = assertInstanceOf(ExtractionResult.Success.class, new FragmentExtractor().extract(source,
                new FragmentExtractionRequest.Blocks(List.of(1)), Optional.of(token)));
        var plan = assertInstanceOf(PlanningResult.Success.class, new TransferPlanner().plan(extracted.fragment(),
                new TransferContext(source, Optional.of(token), extracted.sourceMetadata()))).plan();
        var copied = assertInstanceOf(MaterializationResult.Success.class,
                new TransferMaterializer().materialize(extracted.fragment(), plan)).transfer();
        var plot = (PlotBlock) ((FragmentContent.Blocks) copied.content()).roots().getFirst();
        assertEquals(ANALYSIS.id(), plot.definition().series().get(1).fitAnalysisId().orElseThrow());
        assertTrue(copied.resourceAdditions().isEmpty());
    }

    @Test void olderV2PlotSeriesWithoutFitFieldStillLoads() {
        var document = new Document(List.of(new PlotBlock(PlotDefinition.of("Data",
                AxisDefinition.linear("X"), AxisDefinition.linear("Y"),
                List.of(new PlotSeries("Points", PlotSeriesKind.SCATTER,
                        new DatasetPlotBinding("data", "t", "d")))))), List.of(dataset("9")));
        var codec = new DocumentJsonCodec();
        var json = (String) assertInstanceOf(PersistenceResult.Success.class, codec.encode(document)).value();
        var root = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
        root.getAsJsonArray("blocks").get(0).getAsJsonObject().getAsJsonObject("definition")
                .getAsJsonArray("series").get(0).getAsJsonObject().remove("fitAnalysisId");
        var legacy = root.toString();
        var decoded = (Document) assertInstanceOf(PersistenceResult.Success.class, codec.decode(legacy)).value();
        assertEquals(document, decoded);
    }

    @Test void fitDisplayUnitDoesNotShiftAutomaticPlotAxisCoordinates() {
        var inCentimetres = new DatasetAnalysisBlock("fit-1", "data", AnalysisKind.QUADRATIC_FIT,
                Optional.of("t"), "d", Optional.of(new UnitParser().parseRequired("cm")), NumberNotation.DECIMAL);
        var document = new Document(List.of(inCentimetres, plot()), List.of(dataset("9")));
        var series = new DatasetPlotResolver().resolve(document, plot()).definition().series();
        var measured = series.get(0).points().getLast();
        var predicted = series.get(1).points().getLast();
        assertEquals(measured.y(), predicted.y(), 0.00001);
    }

    @Test void figureContainedFitUsesSameTravelingAnalysisRemap() {
        var figure = new FigureBlock("figure-1", plot(), new InlineContent(List.of(new Text("Motion", java.util.Set.of()))));
        var source = new Document(List.of(ANALYSIS, figure), List.of(dataset("9")));
        var extracted = assertInstanceOf(ExtractionResult.Success.class, new FragmentExtractor().extract(source,
                new FragmentExtractionRequest.Blocks(List.of(0, 1))));
        var plan = assertInstanceOf(PlanningResult.Success.class, new TransferPlanner().plan(extracted.fragment(),
                new TransferContext(new Document(List.of(ANALYSIS)), Optional.empty(), extracted.sourceMetadata()))).plan();
        var transfer = assertInstanceOf(MaterializationResult.Success.class,
                new TransferMaterializer().materialize(extracted.fragment(), plan)).transfer();
        var blocks = ((FragmentContent.Blocks) transfer.content()).roots();
        var analysis = (DatasetAnalysisBlock) blocks.get(0);
        var copiedFigure = (FigureBlock) blocks.get(1);
        assertEquals(analysis.id(), ((PlotBlock) copiedFigure.content()).definition().series().get(1).fitAnalysisId().orElseThrow());
    }
}
