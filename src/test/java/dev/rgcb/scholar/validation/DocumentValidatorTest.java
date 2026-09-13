package dev.rgcb.scholar.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.data.DatasetColumn;
import dev.rgcb.scholar.data.DatasetColumnType;
import dev.rgcb.scholar.data.DatasetPlotBinding;
import dev.rgcb.scholar.data.DatasetRow;
import dev.rgcb.scholar.data.DatasetTableBinding;
import dev.rgcb.scholar.data.DatasetValue;
import dev.rgcb.scholar.data.ScientificDataset;
import dev.rgcb.scholar.diagram.DiagramBounds;
import dev.rgcb.scholar.diagram.DiagramCanvas;
import dev.rgcb.scholar.diagram.DiagramConnection;
import dev.rgcb.scholar.diagram.DiagramDefinition;
import dev.rgcb.scholar.diagram.DiagramElement;
import dev.rgcb.scholar.diagram.DiagramElementId;
import dev.rgcb.scholar.diagram.DiagramEndpoint;
import dev.rgcb.scholar.diagram.DiagramNode;
import dev.rgcb.scholar.diagram.DiagramPort;
import dev.rgcb.scholar.diagram.DiagramPortId;
import dev.rgcb.scholar.diagram.DiagramPortPlacement;
import dev.rgcb.scholar.diagram.DiagramPortSide;
import dev.rgcb.scholar.document.BlockNode;
import dev.rgcb.scholar.document.CrossReference;
import dev.rgcb.scholar.document.CrossReferenceTargetKind;
import dev.rgcb.scholar.document.DiagramBlock;
import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.EquationBlock;
import dev.rgcb.scholar.document.FigureBlock;
import dev.rgcb.scholar.document.Heading;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.InlineNode;
import dev.rgcb.scholar.document.Paragraph;
import dev.rgcb.scholar.document.PlotBlock;
import dev.rgcb.scholar.document.TableBlock;
import dev.rgcb.scholar.document.TableCell;
import dev.rgcb.scholar.document.TableCellContent;
import dev.rgcb.scholar.document.TableOfContentsBlock;
import dev.rgcb.scholar.document.TableRow;
import dev.rgcb.scholar.document.Text;
import dev.rgcb.scholar.electrical.ElectricalComponent;
import dev.rgcb.scholar.electrical.ElectricalComponentKind;
import dev.rgcb.scholar.electrical.ElectricalJunction;
import dev.rgcb.scholar.electrical.ElectricalOrientation;
import dev.rgcb.scholar.math.MathFraction;
import dev.rgcb.scholar.math.MathIdentifier;
import dev.rgcb.scholar.math.MathNumber;
import dev.rgcb.scholar.math.MathSequence;
import dev.rgcb.scholar.mechanical.MechanicalConstraint;
import dev.rgcb.scholar.mechanical.MechanicalConstraintKind;
import dev.rgcb.scholar.mechanical.MechanicalPartReference;
import dev.rgcb.scholar.mechanical.MechanicalPrimitive;
import dev.rgcb.scholar.mechanical.MechanicalPrimitiveKind;
import dev.rgcb.scholar.plot.AxisDefinition;
import dev.rgcb.scholar.plot.DataPoint;
import dev.rgcb.scholar.plot.PlotDefinition;
import dev.rgcb.scholar.plot.PlotSeries;
import dev.rgcb.scholar.plot.PlotSeriesKind;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DocumentValidatorTest {
    @Test
    void acceptsComplexDocumentWithCurrentBlockTypes() {
        var document = new Document(List.of(
                new Heading("motion", 1, inline("Motion")),
                paragraph("See ", ref(CrossReferenceTargetKind.SECTION, "motion"), "."),
                new TableOfContentsBlock(),
                new TableBlock("measurements", List.of(row(cell("Time"), cell("Height")), row(cell("0"), cell("0"))), 1),
                new EquationBlock("velocity", new MathFraction(seq(new MathIdentifier("dx")), seq(new MathIdentifier("dt")))),
                new TableBlock(new DatasetTableBinding("projectile", List.of("time", "height"))),
                plotView("projectile"),
                new FigureBlock("figure-plot", plot(), inline("Projected motion")),
                new FigureBlock("figure-diagram", electricalDiagram(), inline("Circuit"))),
                List.of(projectileDataset()));

        var result = DocumentValidator.validate(document);

        assertTrue(result.isValid());
        assertEquals(List.of(), result.diagnostics());
    }

    @Test
    void duplicateStableIdsAreErrorsWithinEachNamespace() {
        var document = new Document(List.of(
                new Heading("same-heading", 1, inline("A")),
                new Heading("same-heading", 2, inline("B")),
                new TableBlock("same-table", List.of(row(cell("A"))), 1),
                new TableBlock("same-table", List.of(row(cell("B"))), 1),
                new EquationBlock("same-equation", new MathIdentifier("x")),
                new EquationBlock("same-equation", new MathIdentifier("y")),
                new FigureBlock("same-figure", plot(), inline("A")),
                new FigureBlock("same-figure", plot(), inline("B"))));

        var codes = DocumentValidator.validate(document).errors().stream()
                .map(DocumentDiagnostic::code)
                .toList();

        assertTrue(codes.contains(DocumentDiagnosticCode.DUPLICATE_HEADING_ID));
        assertTrue(codes.contains(DocumentDiagnosticCode.DUPLICATE_TABLE_ID));
        assertTrue(codes.contains(DocumentDiagnosticCode.DUPLICATE_EQUATION_ID));
        assertTrue(codes.contains(DocumentDiagnosticCode.DUPLICATE_FIGURE_ID));
    }

    @Test
    void sameTextualStableIdMayBeReusedAcrossTargetKinds() {
        var document = new Document(List.of(
                new Heading("shared", 1, inline("Shared")),
                new TableBlock("shared", List.of(row(cell("A"))), 1),
                new EquationBlock("shared", new MathIdentifier("x")),
                new FigureBlock("shared", plot(), inline("Shared"))));

        var result = DocumentValidator.validate(document);

        assertTrue(result.isValid());
        assertEquals(List.of(), result.diagnostics());
    }

    @Test
    void unresolvedCrossReferencesAreWarningsAndDocumentRemainsValid() {
        var document = new Document(List.of(
                paragraph("See ", ref(CrossReferenceTargetKind.FIGURE, "missing-figure"), "."),
                new FigureBlock("present", plot(), inline("Caption links to ", ref(CrossReferenceTargetKind.SECTION, "missing-section")))));

        var result = DocumentValidator.validate(document);

        assertTrue(result.isValid());
        assertEquals(2, result.warnings().size());
        assertTrue(result.warnings().stream().allMatch(diagnostic -> diagnostic.code() == DocumentDiagnosticCode.MISSING_CROSS_REFERENCE_TARGET));
    }

    @Test
    void missingDatasetBindingsAreWarningsAndDocumentRemainsValid() {
        var document = new Document(List.of(
                new TableBlock(new DatasetTableBinding("missing", List.of("x"))),
                plotView("missing")));

        var result = DocumentValidator.validate(document);

        assertTrue(result.isValid());
        assertEquals(2, result.warnings().stream()
                .filter(diagnostic -> diagnostic.code() == DocumentDiagnosticCode.MISSING_DATASET)
                .count());
    }

    @Test
    void missingDatasetColumnsAreWarningsAndDuplicateBindingColumnsAreErrors() {
        var document = new Document(List.of(
                new TableBlock(new DatasetTableBinding("projectile", List.of("time", "height", "height", "missing"))),
                new PlotBlock(PlotDefinition.of(
                        "Broken plot",
                        AxisDefinition.linear("x"),
                        AxisDefinition.linear("y"),
                        List.of(new PlotSeries("Height", PlotSeriesKind.LINE, new DatasetPlotBinding("projectile", "time", "missing")))))),
                List.of(projectileDataset()));

        var result = DocumentValidator.validate(document);

        assertFalse(result.isValid());
        assertEquals(1, result.errors().stream()
                .filter(diagnostic -> diagnostic.code() == DocumentDiagnosticCode.DUPLICATE_DATASET_BINDING_COLUMN_ID)
                .count());
        assertEquals(2, result.warnings().stream()
                .filter(diagnostic -> diagnostic.code() == DocumentDiagnosticCode.MISSING_DATASET_COLUMN)
                .count());
    }

    @Test
    void skippedHeadingLevelsAndMultipleTableOfContentsBlocksRemainValid() {
        var document = new Document(List.of(
                new TableOfContentsBlock(),
                new Heading("top", 1, inline("Top")),
                new Heading("deep", 3, inline("Deep")),
                new TableOfContentsBlock()));

        var result = DocumentValidator.validate(document);

        assertTrue(result.isValid());
        assertEquals(List.of(), result.diagnostics());
    }

    @Test
    void validatesWarningsForMechanicalReferencesToMissingTargets() {
        var missing = new DiagramElementId("missing");
        var primitive = new MechanicalPrimitive(id("line"), bounds(10, 10, 40, 10), MechanicalPrimitiveKind.LINE);
        var constraint = MechanicalConstraint.unary(id("constraint"), bounds(70, 10, 8, 8), MechanicalConstraintKind.HORIZONTAL, missing);
        var partRef = new MechanicalPartReference(id("part-ref"), bounds(10, 60, 20, 20), missing, 1, "Bracket", 1, "");
        var diagram = new DiagramBlock(new DiagramDefinition("Mechanical", canvas(), List.of(primitive, constraint, partRef), List.of()));
        var result = DocumentValidator.validate(new Document(List.of(diagram)));

        assertTrue(result.isValid());
        assertTrue(result.warnings().stream().anyMatch(diagnostic -> diagnostic.code() == DocumentDiagnosticCode.MISSING_MECHANICAL_CONSTRAINT_TARGET));
        assertTrue(result.warnings().stream().anyMatch(diagnostic -> diagnostic.code() == DocumentDiagnosticCode.MISSING_MECHANICAL_PART_REFERENCE_TARGET));
    }

    @Test
    void acceptsRepresentativeElectricalAndMechanicalDiagrams() {
        var resistor = new ElectricalComponent(id("r1"), bounds(20, 20, 60, 24), ElectricalComponentKind.RESISTOR, ElectricalOrientation.DEG_0, "R1", "10k");
        var junction = new ElectricalJunction(id("j1"), bounds(120, 26, 8, 8), "Vout");
        var electrical = new DiagramBlock(new DiagramDefinition(
                "Electrical",
                canvas(),
                List.of(resistor, junction),
                List.of(new DiagramConnection(endpoint(resistor, "b"), endpoint(junction, "left"), ""))));

        var line = new MechanicalPrimitive(id("line"), bounds(20, 80, 80, 12), MechanicalPrimitiveKind.LINE);
        var constraint = MechanicalConstraint.unary(id("constraint"), bounds(110, 80, 8, 8), MechanicalConstraintKind.HORIZONTAL, line.id());
        var partRef = new MechanicalPartReference(id("part-ref"), bounds(20, 120, 24, 24), line.id(), 1, "Beam", 1, "");
        var mechanical = new DiagramBlock(new DiagramDefinition("Mechanical", canvas(), List.of(line, constraint, partRef), List.of()));

        var result = DocumentValidator.validate(new Document(List.of(electrical, mechanical)));

        assertTrue(result.isValid());
        assertEquals(List.of(), result.diagnostics());
    }

    @Test
    void detectsDuplicatePortsFromCustomDiagramElements() {
        var port = new DiagramPortId("p");
        var element = new CustomElement(
                id("custom"),
                bounds(10, 10, 20, 20),
                List.of(port(port, DiagramPortSide.LEFT), port(port, DiagramPortSide.RIGHT)));
        var diagram = new DiagramBlock(new DiagramDefinition("Custom", canvas(), List.of(element), List.of()));

        var result = DocumentValidator.validate(new Document(List.of(diagram)));

        assertFalse(result.isValid());
        assertEquals(DocumentDiagnosticCode.DUPLICATE_DIAGRAM_PORT_ID, result.errors().get(0).code());
    }

    @Test
    void constructorEnforcedInvariantsRejectUnconstructableBrokenModels() {
        assertThrows(IllegalArgumentException.class, () -> new Heading(7, inline("Bad")));
        assertThrows(IllegalArgumentException.class, () -> new TableBlock(List.of(row(cell("A")), row(cell("B"), cell("C"))), 0));
        assertThrows(IllegalArgumentException.class, () -> new FigureBlock("bad", paragraph("not visual"), inline("caption")));
        assertThrows(IllegalArgumentException.class, () -> new Document(List.of(), List.of(projectileDataset(), projectileDataset())));
        assertThrows(IllegalArgumentException.class, () -> new ScientificDataset(
                "bad",
                Optional.empty(),
                List.of(column("time"), column("time")),
                List.of()));
        assertThrows(IllegalArgumentException.class, () -> new DiagramDefinition(
                "duplicate",
                canvas(),
                List.of(new DiagramNode(id("n"), bounds(10, 10, 20, 20), "A", List.of()), new DiagramNode(id("n"), bounds(40, 10, 20, 20), "B", List.of())),
                List.of()));
        assertThrows(IllegalArgumentException.class, () -> new DiagramDefinition(
                "bad endpoint",
                canvas(),
                List.of(new DiagramNode(id("n"), bounds(10, 10, 20, 20), "A", List.of(port("p", DiagramPortSide.LEFT)))),
                List.of(new DiagramConnection(new DiagramEndpoint(id("missing"), new DiagramPortId("p")), new DiagramEndpoint(id("n"), new DiagramPortId("p")), ""))));
    }

    @Test
    void validationIsPureDeterministicAndDoesNotMutateDocument() {
        var document = new Document(List.of(
                new Heading("motion", 1, inline("Motion")),
                paragraph("See ", ref(CrossReferenceTargetKind.SECTION, "motion"), ".")),
                List.of(projectileDataset()));

        var beforeBlocks = document.blocks();
        var beforeDatasets = document.datasets();
        var first = DocumentValidator.validate(document);
        var second = DocumentValidator.validate(document);

        assertEquals(first, second);
        assertEquals(beforeBlocks, document.blocks());
        assertEquals(beforeDatasets, document.datasets());
    }

    @Test
    void nullDocumentReturnsValidationErrorInsteadOfThrowing() {
        var result = assertDoesNotThrow(() -> DocumentValidator.validate(null));

        assertFalse(result.isValid());
        assertEquals(DocumentDiagnosticCode.NULL_DOCUMENT, result.errors().get(0).code());
    }

    @Test
    void validationResultAndDiagnosticsAreImmutable() {
        var result = DocumentValidator.validate(new Document(List.of(paragraph("A"))));

        assertThrows(UnsupportedOperationException.class, () -> result.diagnostics().add(DocumentDiagnostic.error(DocumentDiagnosticCode.VALIDATION_FAILURE, "nope")));
        assertInstanceOf(List.class, result.errors());
    }

    private static Paragraph paragraph(String text) {
        return new Paragraph(inline(text));
    }

    private static Paragraph paragraph(InlineNode... nodes) {
        return new Paragraph(new InlineContent(List.of(nodes)));
    }

    private static Paragraph paragraph(String before, InlineNode node, String after) {
        return paragraph(new Text(before, Set.of()), node, new Text(after, Set.of()));
    }

    private static CrossReference ref(CrossReferenceTargetKind kind, String id) {
        return new CrossReference(kind, id);
    }

    private static InlineContent inline(String text) {
        return new InlineContent(List.of(new Text(text, Set.of())));
    }

    private static InlineContent inline(InlineNode... nodes) {
        return new InlineContent(List.of(nodes));
    }

    private static InlineContent inline(String before, InlineNode node) {
        return inline(new Text(before, Set.of()), node);
    }

    private static TableRow row(TableCell... cells) {
        return new TableRow(List.of(cells));
    }

    private static TableCell cell(String text) {
        return new TableCell(new TableCellContent(inline(text)));
    }

    private static PlotBlock plot() {
        return new PlotBlock(PlotDefinition.of(
                "Plot",
                AxisDefinition.linear("x"),
                AxisDefinition.linear("y"),
                List.of(new PlotSeries("Series", PlotSeriesKind.LINE, List.of(new DataPoint(0, 0), new DataPoint(1, 1))))));
    }

    private static PlotBlock plotView(String datasetId) {
        return new PlotBlock(PlotDefinition.of(
                "Dataset plot",
                AxisDefinition.linear("time"),
                AxisDefinition.linear("height"),
                List.of(new PlotSeries("Height", PlotSeriesKind.LINE, new DatasetPlotBinding(datasetId, "time", "height")))));
    }

    private static ScientificDataset projectileDataset() {
        return new ScientificDataset(
                "projectile",
                "Projectile",
                List.of(column("time"), column("height")),
                List.of(
                        new DatasetRow(List.of(DatasetValue.number("0"), DatasetValue.number("0"))),
                        new DatasetRow(List.of(DatasetValue.number("1"), DatasetValue.number("5"))),
                        new DatasetRow(List.of(DatasetValue.number("2"), DatasetValue.text("bad")))));
    }

    private static DatasetColumn column(String id) {
        return new DatasetColumn(id, id, DatasetColumnType.NUMBER);
    }

    private static DiagramBlock electricalDiagram() {
        var resistor = new ElectricalComponent(id("r1"), bounds(20, 20, 60, 24), ElectricalComponentKind.RESISTOR, ElectricalOrientation.DEG_0, "R1", "10k");
        var junction = new ElectricalJunction(id("j1"), bounds(120, 26, 8, 8), "Vout");
        return new DiagramBlock(new DiagramDefinition(
                "Electrical",
                canvas(),
                List.of(resistor, junction),
                List.of(new DiagramConnection(endpoint(resistor, "b"), endpoint(junction, "left"), ""))));
    }

    private static MathSequence seq(dev.rgcb.scholar.math.MathExpression expression) {
        return new MathSequence(List.of(expression));
    }

    private static DiagramCanvas canvas() {
        return new DiagramCanvas(200, 160);
    }

    private static DiagramBounds bounds(double x, double y, double width, double height) {
        return new DiagramBounds(x, y, width, height);
    }

    private static DiagramElementId id(String value) {
        return new DiagramElementId(value);
    }

    private static DiagramPort port(String id, DiagramPortSide side) {
        return port(new DiagramPortId(id), side);
    }

    private static DiagramPort port(DiagramPortId id, DiagramPortSide side) {
        return new DiagramPort(id, "", new DiagramPortPlacement(side, 0.5));
    }

    private static DiagramEndpoint endpoint(DiagramElement element, String portId) {
        return new DiagramEndpoint(element.id(), new DiagramPortId(portId));
    }

    private record CustomElement(DiagramElementId id, DiagramBounds bounds, List<DiagramPort> ports) implements DiagramElement {
        private CustomElement {
            ports = List.copyOf(ports);
        }

        @Override
        public CustomElement withBounds(DiagramBounds bounds) {
            return new CustomElement(id, bounds, ports);
        }
    }
}
