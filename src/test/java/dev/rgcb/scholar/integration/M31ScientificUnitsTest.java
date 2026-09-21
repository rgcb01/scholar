package dev.rgcb.scholar.integration;

import static org.junit.jupiter.api.Assertions.*;

import dev.rgcb.scholar.client.DevelopmentDocument;
import dev.rgcb.scholar.data.*;
import dev.rgcb.scholar.document.*;
import dev.rgcb.scholar.editor.*;
import dev.rgcb.scholar.math.MathQuantity;
import dev.rgcb.scholar.math.MathSequence;
import dev.rgcb.scholar.persistence.DocumentJsonCodec;
import dev.rgcb.scholar.persistence.PersistenceResult;
import dev.rgcb.scholar.plot.*;
import dev.rgcb.scholar.quantity.*;
import dev.rgcb.scholar.transfer.*;
import dev.rgcb.scholar.validation.DocumentDiagnosticCode;
import dev.rgcb.scholar.validation.DocumentValidator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class M31ScientificUnitsTest {
    private final UnitParser units = new UnitParser();

    @Test void datasetTableAndPlotUseSemanticUnitsWithoutMutatingSourceMetadata() {
        var dataset = thermalDataset();
        var table = new TableBlock(new DatasetTableBinding(dataset.id()));
        var plot = thermalPlot();
        var document = new Document(List.of(table, plot), List.of(dataset));

        var resolvedTable = new DatasetTableResolver().resolve(document, table);
        assertEquals("Time (s)", cellText(resolvedTable, 0, 0));
        assertEquals("Temperature (K)", cellText(resolvedTable, 0, 1));
        assertEquals("Time", dataset.columns().get(0).displayName());

        var resolvedPlot = new DatasetPlotResolver().resolve(document, plot);
        assertEquals("Time (s)", resolvedPlot.definition().xAxis().label());
        assertEquals("Temperature (°C)", resolvedPlot.definition().yAxis().label());
        assertEquals(0.0, resolvedPlot.definition().series().get(0).points().get(0).y(), 0.0000001);
        assertEquals(25.0, resolvedPlot.definition().series().get(0).points().get(1).y(), 0.0000001);
    }

    @Test void validationDetectsIncompatiblePlotDimensionsAndAcceptsUnitlessNumbers() {
        var unitless = new ScientificDataset("plain", "Plain", List.of(
                new DatasetColumn("x", "X", DatasetColumnType.NUMBER),
                new DatasetColumn("y", "Y", DatasetColumnType.NUMBER)), List.of());
        assertTrue(DocumentValidator.validate(new Document(List.of(), List.of(unitless))).isValid());

        var dataset = new ScientificDataset("mixed", "Mixed", List.of(
                numberColumn("time", "Time", "s"), numberColumn("distance", "Distance", "m"),
                numberColumn("temperature", "Temperature", "K")), List.of());
        var plot = new PlotBlock(PlotDefinition.of("Bad", AxisDefinition.linear("X"), AxisDefinition.linear("Y"), List.of(
                new PlotSeries("distance", PlotSeriesKind.LINE, new DatasetPlotBinding("mixed", "time", "distance")),
                new PlotSeries("temperature", PlotSeriesKind.LINE, new DatasetPlotBinding("mixed", "time", "temperature")))));
        var result = DocumentValidator.validate(new Document(List.of(plot), List.of(dataset)));
        assertFalse(result.isValid());
        assertTrue(result.diagnostics().stream().anyMatch(d -> d.code() == DocumentDiagnosticCode.INCOMPATIBLE_PLOT_UNITS));
    }

    @Test void persistenceV2RoundTripsAllM31SemanticState() {
        var source = DevelopmentDocument.createScientificUnitsQa();
        var codec = new DocumentJsonCodec();
        var json = success(codec.encode(source));
        assertEquals(2, com.google.gson.JsonParser.parseString(json).getAsJsonObject().get("version").getAsInt());
        assertTrue(json.contains("\"unit\""));
        assertTrue(json.contains("\"displayUnit\""));
        assertTrue(json.contains("\"quantity\""));
        assertEquals(source, success(codec.decode(json)));
    }

    @Test void transferPreservesQuantitiesDatasetUnitsAndAxisDisplayUnit() {
        var source = DevelopmentDocument.createScientificUnitsQa();
        var tableIndex = indexOf(source, TableBlock.class);
        var plotIndex = indexOf(source, PlotBlock.class);
        var quantityParagraph = source.blocks().get(1);
        var extraction = assertInstanceOf(ExtractionResult.Success.class, new FragmentExtractor().extract(source,
                new FragmentExtractionRequest.Blocks(List.of(1, tableIndex, plotIndex))));
        var destination = new Document(List.of(new Paragraph(new InlineContent(List.of(new Text("target", Set.of()))))));
        var plan = assertInstanceOf(PlanningResult.Success.class, new TransferPlanner().plan(extraction.fragment(),
                new TransferContext(destination, Optional.empty(), extraction.sourceMetadata()))).plan();
        var transfer = assertInstanceOf(MaterializationResult.Success.class,
                new TransferMaterializer().materialize(extraction.fragment(), plan)).transfer();
        var roots = ((FragmentContent.Blocks) transfer.content()).roots();
        assertEquals(quantityParagraph, roots.get(0));
        assertEquals(1, transfer.resourceAdditions().size());
        assertEquals(source.datasets().get(0).columns(), transfer.resourceAdditions().get(0).columns());
        var transferredPlot = (PlotBlock) roots.get(2);
        assertEquals(units.parseRequired("°C"), transferredPlot.definition().yAxis().displayUnit().orElseThrow());
    }

    @Test void editorUnitChangesAndQuantityInsertionAreSingleUndoableTransactions() {
        var dataset = thermalDataset();
        var table = new TableBlock(new DatasetTableBinding(dataset.id()));
        var paragraph = new Paragraph(new InlineContent(List.of(new Text("Value: ", Set.of()))));
        var session = new EditorSession(new Document(List.of(paragraph, table, thermalPlot()), List.of(dataset)), 0);
        var caret = new DocumentPosition(0, 7);
        session.setCurrent(new EditorState(session.current().document(), new TextSelection(caret, caret), Optional.empty()));
        assertTrue(session.insertQuantity(new Quantity("12.5", units.parseRequired("m")), NumberNotation.DECIMAL));
        assertInstanceOf(QuantityInline.class, ((Paragraph) session.current().document().blocks().get(0)).content().nodes().get(1));
        assertTrue(session.undo());
        assertEquals(paragraph, session.current().document().blocks().get(0));
        assertTrue(session.redo());

        assertTrue(session.convertQuantityAtCaret(units.parseRequired("mm")));
        var converted = (QuantityInline) ((Paragraph) session.current().document().blocks().get(0)).content().nodes().get(1);
        assertEquals(0, new java.math.BigDecimal("12500").compareTo(converted.value().nominal().value()));
        assertTrue(session.undo());
        assertTrue(session.setQuantityNotationAtCaret(NumberNotation.SCIENTIFIC));
        assertEquals(NumberNotation.SCIENTIFIC, ((QuantityInline) ((Paragraph) session.current().document().blocks().get(0))
                .content().nodes().get(1)).notation());
        assertTrue(session.undo());

        session.setCurrent(new EditorState(session.current().document(), new TableEditingSelection(1,
                TableCellTextSelection.caret(new TableCellCoordinate(0, 1), 0)), Optional.empty()));
        assertTrue(session.setSelectedDatasetColumnUnit(Optional.of(units.parseRequired("°C"))));
        assertEquals(units.parseRequired("°C"), session.current().document().datasets().get(0).column("temperature").orElseThrow().unit().orElseThrow());
        assertTrue(session.undo());
        assertEquals(units.parseRequired("K"), session.current().document().datasets().get(0).column("temperature").orElseThrow().unit().orElseThrow());

        session.setCurrent(new EditorState(session.current().document(), new BlockSelection(2), Optional.empty()));
        assertTrue(session.setSelectedPlotAxisDisplayUnit(false, Optional.of(units.parseRequired("K"))));
        assertTrue(session.undo());
        assertEquals(units.parseRequired("°C"), ((PlotBlock) session.current().document().blocks().get(2)).definition().yAxis().displayUnit().orElseThrow());
    }

    @Test void focusedFixtureIsValidAndContainsInlineAndMathQuantities() {
        var document = DevelopmentDocument.createScientificUnitsQa();
        assertTrue(DocumentValidator.validate(document).isValid(), () -> DocumentValidator.validate(document).diagnostics().toString());
        assertTrue(document.blocks().stream().filter(Paragraph.class::isInstance).map(Paragraph.class::cast)
                .flatMap(paragraph -> paragraph.content().nodes().stream()).anyMatch(QuantityInline.class::isInstance));
        assertTrue(document.blocks().stream().filter(EquationBlock.class::isInstance).map(EquationBlock.class::cast)
                .map(EquationBlock::expression).filter(MathSequence.class::isInstance).map(MathSequence.class::cast)
                .flatMap(sequence -> sequence.expressions().stream()).anyMatch(MathQuantity.class::isInstance));
    }

    private ScientificDataset thermalDataset() {
        return new ScientificDataset("thermal", "Thermal", List.of(numberColumn("time", "Time", "s"),
                numberColumn("temperature", "Temperature", "K")), List.of(
                new DatasetRow(List.of(DatasetValue.number("0"), DatasetValue.number("273.15"))),
                new DatasetRow(List.of(DatasetValue.number("1"), DatasetValue.number("298.15")))));
    }

    private PlotBlock thermalPlot() {
        return new PlotBlock(new PlotDefinition("Thermal", AxisDefinition.linear("Time"),
                AxisDefinition.linear("Temperature").withDisplayUnit(units.parseRequired("°C")),
                List.of(new PlotSeries("Temperature", PlotSeriesKind.LINE,
                        new DatasetPlotBinding("thermal", "time", "temperature"))), true, true, 180));
    }

    private DatasetColumn numberColumn(String id, String label, String unit) {
        return new DatasetColumn(id, label, DatasetColumnType.NUMBER, Optional.of(units.parseRequired(unit)));
    }

    private static String cellText(TableBlock table, int row, int column) {
        return ((Text) table.rows().get(row).cells().get(column).content().content().nodes().getFirst()).content();
    }

    private static int indexOf(Document document, Class<? extends BlockNode> type) {
        for (var index = 0; index < document.blocks().size(); index++) if (type.isInstance(document.blocks().get(index))) return index;
        throw new AssertionError("Missing " + type.getSimpleName());
    }

    private static <T> T success(PersistenceResult<T> result) {
        assertInstanceOf(PersistenceResult.Success.class, result, () -> result.diagnostics().toString());
        return ((PersistenceResult.Success<T>) result).value();
    }
}
