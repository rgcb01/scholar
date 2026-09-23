package dev.rgcb.scholar.integration;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
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
import dev.rgcb.scholar.validation.DocumentValidator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TemperatureQuantitySemanticsTest {
    private final UnitParser units = new UnitParser();

    @Test void datasetAndPlotConversionsPreserveAbsoluteVersusDifferenceSemantics() {
        var absolute = dataset("absolute", QuantitySemantics.ABSOLUTE_TEMPERATURE, "273.15");
        var difference = dataset("difference", QuantitySemantics.TEMPERATURE_DIFFERENCE, "10");
        var absolutePlot = plot("absolute", QuantitySemantics.ABSOLUTE_TEMPERATURE);
        var differencePlot = plot("difference", QuantitySemantics.TEMPERATURE_DIFFERENCE);
        var document = new Document(List.of(absolutePlot, differencePlot), List.of(absolute, difference));

        var resolver = new DatasetPlotResolver();
        var absoluteResolved = resolver.resolve(document, absolutePlot);
        var differenceResolved = resolver.resolve(document, differencePlot);
        assertEquals(0.0, absoluteResolved.definition().series().getFirst().points().getFirst().y(), 0.0000001);
        assertEquals(10.0, differenceResolved.definition().series().getFirst().points().getFirst().y(), 0.0000001);
        assertEquals("Temperature (°C)", absoluteResolved.definition().yAxis().label());
        assertEquals("Temperature (Δ°C)", differenceResolved.definition().yAxis().label());
        assertTrue(DocumentValidator.validate(document).isValid());

        var mismatched = new Document(List.of(plot("difference", QuantitySemantics.ABSOLUTE_TEMPERATURE)), List.of(difference));
        assertFalse(DocumentValidator.validate(mismatched).isValid());
    }

    @Test void persistenceV2RoundTripsSemanticsAndOldV2DefaultsTemperatureToAbsolute() {
        var source = semanticDocument();
        var codec = new DocumentJsonCodec();
        var encoded = success(codec.encode(source));
        assertTrue(encoded.contains("\"temperature-difference\""));
        assertEquals(source, success(codec.decode(encoded)));

        var oldTree = JsonParser.parseString(encoded);
        removeSemanticFields(oldTree);
        var oldLoaded = success(codec.decode(oldTree.toString()));
        var inline = (QuantityInline) ((Paragraph) oldLoaded.blocks().getFirst()).content().nodes().getFirst();
        assertEquals(QuantitySemantics.ABSOLUTE_TEMPERATURE, inline.value().nominal().semantics());
        assertEquals(QuantitySemantics.ABSOLUTE_TEMPERATURE,
                oldLoaded.datasets().getFirst().columns().get(1).quantitySemantics());
    }

    @Test void transferAndPlainTextKeepTemperatureDifferenceSemantics() {
        var source = semanticDocument();
        var extraction = assertInstanceOf(ExtractionResult.Success.class, new FragmentExtractor().extract(source,
                new FragmentExtractionRequest.Blocks(List.of(0, 1, 2))));
        var destination = new Document(List.of(new Paragraph(new InlineContent(List.of(new Text("target", Set.of()))))));
        var plan = assertInstanceOf(PlanningResult.Success.class, new TransferPlanner().plan(extraction.fragment(),
                new TransferContext(destination, Optional.empty(), extraction.sourceMetadata()))).plan();
        var transfer = assertInstanceOf(MaterializationResult.Success.class,
                new TransferMaterializer().materialize(extraction.fragment(), plan)).transfer();

        var roots = ((FragmentContent.Blocks) transfer.content()).roots();
        var inline = (QuantityInline) ((Paragraph) roots.getFirst()).content().nodes().getFirst();
        var math = (MathQuantity) ((MathSequence) ((EquationBlock) roots.get(1)).expression()).expressions().getFirst();
        assertEquals(QuantitySemantics.TEMPERATURE_DIFFERENCE, inline.value().nominal().semantics());
        assertEquals(QuantitySemantics.TEMPERATURE_DIFFERENCE, math.value().nominal().semantics());
        assertEquals(QuantitySemantics.TEMPERATURE_DIFFERENCE,
                transfer.resourceAdditions().getFirst().columns().get(1).quantitySemantics());
        assertEquals("Δ5 °C", new DocumentPlainTextSerializer().serializeBlock(source, 0, source.blocks().getFirst()));
    }

    @Test void productionEditorOperationsAreSingleUndoableSemanticTransactions() {
        var dataset = dataset("difference", QuantitySemantics.ABSOLUTE_TEMPERATURE, "273.15");
        var paragraph = new Paragraph(new InlineContent(List.of(new Text("Change: ", Set.of()))));
        var table = new TableBlock(new DatasetTableBinding(dataset.id()));
        var session = new EditorSession(new Document(List.of(paragraph, table), List.of(dataset)), 0);
        var caret = new DocumentPosition(0, 8);
        session.setCurrent(new EditorState(session.current().document(), new TextSelection(caret, caret), Optional.empty()));
        var difference = new Quantity("5", units.parseRequired("°C"), QuantitySemantics.TEMPERATURE_DIFFERENCE);
        assertTrue(session.insertQuantity(difference, NumberNotation.DECIMAL));
        assertTrue(session.undo());
        assertEquals(paragraph, session.current().document().blocks().getFirst());
        assertTrue(session.redo());

        session.setCurrent(new EditorState(session.current().document(), new TableEditingSelection(1,
                TableCellTextSelection.caret(new TableCellCoordinate(0, 1), 0)), Optional.empty()));
        assertTrue(session.setSelectedDatasetColumnUnit(Optional.of(units.parseRequired("°C")),
                QuantitySemantics.TEMPERATURE_DIFFERENCE));
        assertEquals(QuantitySemantics.TEMPERATURE_DIFFERENCE,
                session.current().document().datasets().getFirst().columns().get(1).quantitySemantics());
        assertTrue(session.undo());
        assertEquals(QuantitySemantics.ABSOLUTE_TEMPERATURE,
                session.current().document().datasets().getFirst().columns().get(1).quantitySemantics());
    }

    private Document semanticDocument() {
        var difference = new Quantity("5", units.parseRequired("°C"), QuantitySemantics.TEMPERATURE_DIFFERENCE);
        var paragraph = new Paragraph(new InlineContent(List.of(new QuantityInline(difference))));
        var equation = new EquationBlock(new MathSequence(List.of(new MathQuantity(difference))));
        var dataset = dataset("difference", QuantitySemantics.TEMPERATURE_DIFFERENCE, "10");
        return new Document(List.of(paragraph, equation, plot(dataset.id(), QuantitySemantics.TEMPERATURE_DIFFERENCE)),
                List.of(dataset));
    }

    private ScientificDataset dataset(String id, QuantitySemantics semantics, String value) {
        return new ScientificDataset(id, id, List.of(
                new DatasetColumn("time", "Time", DatasetColumnType.NUMBER, Optional.of(units.parseRequired("s"))),
                new DatasetColumn("temperature", "Temperature", DatasetColumnType.NUMBER,
                        Optional.of(units.parseRequired("K")), semantics)),
                List.of(new DatasetRow(List.of(DatasetValue.number("0"), DatasetValue.number(value)))));
    }

    private PlotBlock plot(String datasetId, QuantitySemantics semantics) {
        return new PlotBlock(new PlotDefinition("Thermal", AxisDefinition.linear("Time"),
                AxisDefinition.linear("Temperature").withDisplayUnit(units.parseRequired("°C"), semantics),
                List.of(new PlotSeries("Temperature", PlotSeriesKind.LINE,
                        new DatasetPlotBinding(datasetId, "time", "temperature"))), true, true, 180));
    }

    private static void removeSemanticFields(JsonElement element) {
        if (element.isJsonObject()) {
            var object = element.getAsJsonObject();
            object.remove("semantics");
            object.remove("quantitySemantics");
            object.remove("displayUnitSemantics");
            List.copyOf(object.entrySet()).forEach(entry -> removeSemanticFields(entry.getValue()));
        } else if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(TemperatureQuantitySemanticsTest::removeSemanticFields);
        }
    }

    private static <T> T success(PersistenceResult<T> result) {
        assertInstanceOf(PersistenceResult.Success.class, result, () -> result.diagnostics().toString());
        return ((PersistenceResult.Success<T>) result).value();
    }
}
