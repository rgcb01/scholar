package dev.rgcb.scholar.editor;

import dev.rgcb.scholar.data.DatasetColumn;
import dev.rgcb.scholar.data.DatasetColumnType;
import dev.rgcb.scholar.data.DatasetPlotBinding;
import dev.rgcb.scholar.data.DatasetRow;
import dev.rgcb.scholar.data.DatasetTableBinding;
import dev.rgcb.scholar.data.DatasetValue;
import dev.rgcb.scholar.data.ScientificDataset;
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
import dev.rgcb.scholar.document.TextMark;
import dev.rgcb.scholar.diagram.DiagramBounds;
import dev.rgcb.scholar.diagram.DiagramCanvas;
import dev.rgcb.scholar.diagram.DiagramConnection;
import dev.rgcb.scholar.diagram.DiagramDefinition;
import dev.rgcb.scholar.diagram.DiagramElementId;
import dev.rgcb.scholar.diagram.DiagramEndpoint;
import dev.rgcb.scholar.diagram.DiagramNode;
import dev.rgcb.scholar.diagram.DiagramPort;
import dev.rgcb.scholar.diagram.DiagramPortId;
import dev.rgcb.scholar.diagram.DiagramPortPlacement;
import dev.rgcb.scholar.diagram.DiagramPortSide;
import dev.rgcb.scholar.electrical.ElectricalComponent;
import dev.rgcb.scholar.electrical.ElectricalComponentKind;
import dev.rgcb.scholar.electrical.ElectricalOrientation;
import dev.rgcb.scholar.math.MathFraction;
import dev.rgcb.scholar.math.MathIdentifier;
import dev.rgcb.scholar.math.MathNumber;
import dev.rgcb.scholar.math.MathOperator;
import dev.rgcb.scholar.math.MathOperatorRole;
import dev.rgcb.scholar.math.MathRoot;
import dev.rgcb.scholar.math.MathSequence;
import dev.rgcb.scholar.mechanical.MechanicalAnnotation;
import dev.rgcb.scholar.mechanical.MechanicalAnnotationKind;
import dev.rgcb.scholar.mechanical.MechanicalConstraint;
import dev.rgcb.scholar.mechanical.MechanicalConstraintKind;
import dev.rgcb.scholar.mechanical.MechanicalDimension;
import dev.rgcb.scholar.mechanical.MechanicalDimensionKind;
import dev.rgcb.scholar.mechanical.MechanicalPrimitive;
import dev.rgcb.scholar.mechanical.MechanicalPrimitiveKind;
import dev.rgcb.scholar.mechanical.MechanicalSymbol;
import dev.rgcb.scholar.mechanical.MechanicalSymbolKind;
import dev.rgcb.scholar.plot.AxisDefinition;
import dev.rgcb.scholar.plot.DataPoint;
import dev.rgcb.scholar.plot.PlotDefinition;
import dev.rgcb.scholar.plot.PlotSeries;
import dev.rgcb.scholar.plot.PlotSeriesKind;
import java.util.List;
import java.util.Set;

final class EditorFoundationFixture {
    private EditorFoundationFixture() {
    }

    static Document canonicalDocument() {
        return new Document(List.of(
                heading("intro", 1, "Motion Lab"),
                paragraph(
                        text("This integrated fixture links "),
                        ref(CrossReferenceTargetKind.SECTION, "analysis"),
                        text(", "),
                        ref(CrossReferenceTargetKind.EQUATION, "velocity-eq"),
                        text(", "),
                        ref(CrossReferenceTargetKind.TABLE, "measurements"),
                        text(", and "),
                        ref(CrossReferenceTargetKind.FIGURE, "trajectory-figure"),
                        text(".")),
                new TableOfContentsBlock(),
                heading("analysis", 2, "Analysis"),
                paragraph(text("Velocity", TextMark.BOLD), text(" and "), text("time", TextMark.ITALIC), text(" remain editable prose.")),
                equation("velocity-eq"),
                table("measurements"),
                plot("Authored Plot"),
                genericDiagram(),
                electricalDiagram(),
                mechanicalDiagram(),
                new FigureBlock("trajectory-figure", plot("Figure Plot"), inline("Projected motion")),
                new FigureBlock("circuit-figure", genericDiagram(), inline("System diagram")),
                heading("data", 2, "Data"),
                new TableBlock(new DatasetTableBinding("projectile", List.of("time", "height", "note"))),
                datasetPlot(),
                paragraph("Extra paragraph 1 for scroll and structural navigation."),
                paragraph("Extra paragraph 2 for clipboard and undo redo checks."),
                paragraph("Extra paragraph 3 with cafe, lambda, theta, and omega symbols: café λ θ Ω.")),
                List.of(projectileDataset()));
    }

    static Document degradedDocument() {
        return new Document(List.of(
                paragraph(ref(CrossReferenceTargetKind.FIGURE, "missing-figure")),
                new TableBlock(new DatasetTableBinding("missing-dataset")),
                new PlotBlock(PlotDefinition.of(
                        "Broken Dataset Plot",
                        AxisDefinition.linear("time"),
                        AxisDefinition.linear("height"),
                        List.of(new PlotSeries("missing", PlotSeriesKind.LINE,
                                new DatasetPlotBinding("projectile", "time", "missing-column")))))),
                List.of(projectileDataset()));
    }

    static Paragraph paragraph(String text) {
        return new Paragraph(inline(text));
    }

    static Paragraph paragraph(InlineNode... nodes) {
        return new Paragraph(new InlineContent(List.of(nodes)));
    }

    static Heading heading(String id, int level, String text) {
        return new Heading(id, level, inline(text));
    }

    static EquationBlock equation(String id) {
        return new EquationBlock(id, new MathSequence(List.of(
                new MathIdentifier("v"),
                new MathOperator("=", MathOperatorRole.RELATION),
                new MathFraction(
                        new MathSequence(List.of(new MathIdentifier("d"))),
                        new MathSequence(List.of(new MathIdentifier("t")))))));
    }

    static TableBlock table(String id) {
        return new TableBlock(id, List.of(
                new TableRow(List.of(cell("Quantity"), cell("Value"), cell("Unit"))),
                new TableRow(List.of(cell("Time"), cell("2"), cell("s"))),
                new TableRow(List.of(cell("Height"), cell("5"), cell("m")))), 1);
    }

    static PlotBlock plot(String title) {
        return new PlotBlock(PlotDefinition.of(
                title,
                AxisDefinition.linear("t"),
                AxisDefinition.linear("x"),
                List.of(new PlotSeries("height", PlotSeriesKind.LINE,
                        List.of(new DataPoint(0, 0), new DataPoint(1, 5), new DataPoint(2, 0))))));
    }

    static PlotBlock datasetPlot() {
        return new PlotBlock(new PlotDefinition(
                "Dataset Plot",
                AxisDefinition.linear("time"),
                AxisDefinition.linear("height"),
                List.of(new PlotSeries("height", PlotSeriesKind.LINE, new DatasetPlotBinding("projectile", "time", "height"))),
                true,
                true,
                PlotDefinition.DEFAULT_HEIGHT));
    }

    static DiagramBlock genericDiagram() {
        var left = new DiagramElementId("left");
        var right = new DiagramElementId("right");
        var out = new DiagramPortId("out");
        var in = new DiagramPortId("in");
        return new DiagramBlock(new DiagramDefinition(
                "Generic Diagram",
                new DiagramCanvas(140, 80),
                List.of(
                        new DiagramNode(left, new DiagramBounds(8, 20, 34, 20), "A", List.of(
                                new DiagramPort(out, "", new DiagramPortPlacement(DiagramPortSide.RIGHT, 0.5)))),
                        new DiagramNode(right, new DiagramBounds(92, 20, 34, 20), "B", List.of(
                                new DiagramPort(in, "", new DiagramPortPlacement(DiagramPortSide.LEFT, 0.5))))),
                List.of(new DiagramConnection(new DiagramEndpoint(left, out), new DiagramEndpoint(right, in), "signal"))));
    }

    static DiagramBlock electricalDiagram() {
        return new DiagramBlock(new DiagramDefinition(
                "Electrical Diagram",
                new DiagramCanvas(140, 80),
                List.of(
                        new ElectricalComponent(
                                new DiagramElementId("r1"),
                                new DiagramBounds(16, 24, 36, 18),
                                ElectricalComponentKind.RESISTOR,
                                ElectricalOrientation.DEG_0,
                                "R1",
                                "220 Ω"),
                        new ElectricalComponent(
                                new DiagramElementId("c1"),
                                new DiagramBounds(78, 24, 30, 18),
                                ElectricalComponentKind.CAPACITOR,
                                ElectricalOrientation.DEG_0,
                                "C1",
                                "10 uF")),
                List.of()));
    }

    static DiagramBlock mechanicalDiagram() {
        var shaft = new DiagramElementId("shaft");
        var rail = new DiagramElementId("rail");
        return new DiagramBlock(new DiagramDefinition(
                "Mechanical Diagram",
                new DiagramCanvas(150, 90),
                List.of(
                        new MechanicalPrimitive(shaft, new DiagramBounds(12, 26, 70, 8), MechanicalPrimitiveKind.LINE),
                        new MechanicalPrimitive(rail, new DiagramBounds(12, 44, 70, 8), MechanicalPrimitiveKind.CENTERLINE),
                        new MechanicalDimension(new DiagramElementId("dim1"), new DiagramBounds(12, 58, 70, 12), MechanicalDimensionKind.HORIZONTAL),
                        MechanicalConstraint.binary(new DiagramElementId("parallel1"), new DiagramBounds(90, 28, 10, 10),
                                MechanicalConstraintKind.PARALLEL, shaft, rail),
                        new MechanicalAnnotation(new DiagramElementId("note1"), new DiagramBounds(96, 12, 36, 16),
                                MechanicalAnnotationKind.NOTE, "ALIGN"),
                        new MechanicalSymbol(new DiagramElementId("bearing1"), new DiagramBounds(104, 48, 22, 22), MechanicalSymbolKind.BEARING)),
                List.of()));
    }

    static ScientificDataset projectileDataset() {
        return new ScientificDataset(
                "projectile",
                "Projectile",
                List.of(
                        new DatasetColumn("time", "Time", DatasetColumnType.NUMBER),
                        new DatasetColumn("height", "Height", DatasetColumnType.NUMBER),
                        new DatasetColumn("note", "Note", DatasetColumnType.TEXT)),
                List.of(
                        new DatasetRow(List.of(DatasetValue.number("0"), DatasetValue.number("0"), DatasetValue.text("start"))),
                        new DatasetRow(List.of(DatasetValue.number("1"), DatasetValue.number("5"), DatasetValue.text("peak"))),
                        new DatasetRow(List.of(DatasetValue.number("2"), DatasetValue.number("0"), DatasetValue.text("end")))));
    }

    static InlineContent inline(String text) {
        return new InlineContent(List.of(text(text)));
    }

    static Text text(String text, TextMark... marks) {
        return new Text(text, Set.of(marks));
    }

    static CrossReference ref(CrossReferenceTargetKind kind, String targetId) {
        return new CrossReference(kind, targetId);
    }

    static TableCell cell(String text) {
        return new TableCell(new TableCellContent(inline(text)));
    }

    static int indexOf(Document document, Class<? extends BlockNode> type) {
        for (var index = 0; index < document.blocks().size(); index++) {
            if (type.isInstance(document.blocks().get(index))) {
                return index;
            }
        }
        throw new AssertionError("Missing block type " + type.getSimpleName());
    }
}
