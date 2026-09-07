package dev.rgcb.scholar.client;

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
import dev.rgcb.scholar.document.BlockNode;
import dev.rgcb.scholar.document.DiagramBlock;
import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.EquationBlock;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.InlineNode;
import dev.rgcb.scholar.document.Paragraph;
import dev.rgcb.scholar.document.PlotBlock;
import dev.rgcb.scholar.document.TableBlock;
import dev.rgcb.scholar.document.TableCell;
import dev.rgcb.scholar.document.TableCellContent;
import dev.rgcb.scholar.document.TableRow;
import dev.rgcb.scholar.document.Text;
import dev.rgcb.scholar.electrical.ElectricalComponent;
import dev.rgcb.scholar.electrical.ElectricalComponentKind;
import dev.rgcb.scholar.electrical.ElectricalOrientation;
import dev.rgcb.scholar.electrical.ElectricalJunction;
import dev.rgcb.scholar.markdown.MarkdownParser;
import dev.rgcb.scholar.mechanical.MechanicalPrimitive;
import dev.rgcb.scholar.mechanical.MechanicalPrimitiveKind;
import dev.rgcb.scholar.mechanical.MechanicalDimension;
import dev.rgcb.scholar.mechanical.MechanicalDimensionKind;
import dev.rgcb.scholar.mechanical.MechanicalConstraint;
import dev.rgcb.scholar.mechanical.MechanicalConstraintKind;
import dev.rgcb.scholar.mechanical.MechanicalOrientation;
import dev.rgcb.scholar.mechanical.MechanicalSymbol;
import dev.rgcb.scholar.mechanical.MechanicalSymbolKind;
import dev.rgcb.scholar.mechanical.MechanicalAnnotation;
import dev.rgcb.scholar.mechanical.MechanicalAnnotationKind;
import dev.rgcb.scholar.mechanical.MechanicalPartReference;
import dev.rgcb.scholar.math.MathFraction;
import dev.rgcb.scholar.math.MathIdentifier;
import dev.rgcb.scholar.math.MathNamedOperator;
import dev.rgcb.scholar.math.MathNumber;
import dev.rgcb.scholar.math.MathOperator;
import dev.rgcb.scholar.math.MathOperatorRole;
import dev.rgcb.scholar.math.MathRoot;
import dev.rgcb.scholar.math.MathScript;
import dev.rgcb.scholar.math.MathSequence;
import dev.rgcb.scholar.math.MathSymbol;
import dev.rgcb.scholar.math.MathSymbolKind;
import dev.rgcb.scholar.math.MathText;
import dev.rgcb.scholar.plot.AxisDefinition;
import dev.rgcb.scholar.plot.DataPoint;
import dev.rgcb.scholar.plot.PlotDefinition;
import dev.rgcb.scholar.plot.PlotSeries;
import dev.rgcb.scholar.plot.PlotSeriesKind;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class DevelopmentDocument {
    private static final String SOURCE = """
            # Motion

            **Velocity** describes how quickly position changes. This first Scholar viewer is intentionally simple, but it exercises mixed formatted and unformatted text in one paragraph.

            ## Average velocity

            Average velocity describes displacement over time. *Displacement* keeps direction, while distance only counts how far something traveled.

            ## Reading the graph

            A graph can show ***position and time*** together. The slope of a line tells us how quickly the position changes.

            A short note can mix **bold terminology**, *italic emphasis*, and ***bold italic emphasis*** without changing the document model. The viewer should wrap this paragraph cleanly at the current window size.

            Scientific prose still uses plain body text for symbols such as Δx, Δt, θ, Ω, and λ when they are not part of a structured equation.

            ## Scrolling check

            This development document keeps a little extra prose so basic scrolling, clipping, and wrapping can be checked inside Minecraft without carrying the old typography stress-test dump.

            Another paragraph keeps the page tall enough for the mouse wheel. The selected typography should stay crisp while the equation, headings, and paragraph text move through the viewport.
            """;

    private DevelopmentDocument() {
    }

    public static Document create() {
        var parsedDocument = new MarkdownParser().parse(SOURCE).document();
        var blocks = new ArrayList<BlockNode>(parsedDocument.blocks());
        blocks.add(4, averageVelocityEquation());
        blocks.add(5, paragraph("The display equation above is built from the Scholar Math AST, not from Markdown math syntax."));
        blocks.add(6, semanticTokenEquation());
        blocks.add(7, paragraph("The second equation includes programmatic semantic math tokens for named operators and math text."));
        blocks.add(8, paragraph("The reference equations below are development-only visual checks for structural math rendering."));
        blocks.add(9, simpleFractionReference());
        blocks.add(10, compoundFractionReference());
        blocks.add(11, squareRootReference());
        blocks.add(12, indexedRootReference());
        blocks.add(13, rootFractionReference());
        blocks.add(14, scriptReference());
        blocks.add(15, combinedScriptReference());
        blocks.add(16, nestedStructureReference());
        blocks.add(17, paragraph("The table below is a development-only check for structured scientific table layout."));
        blocks.add(18, scientificTable());
        blocks.add(19, paragraph("The plot below is the M16D visual check for semantic LINE and SCATTER XY series."));
        blocks.add(20, samplePlot());
        blocks.add(21, paragraph("The diagram below is the M17B visual check for semantic nodes, ports, and a derived connection."));
        blocks.add(22, sampleDiagram());
        blocks.add(paragraph("The schematic below is the M18B visual check for semantic electrical components and derived symbols."));
        blocks.add(sampleElectricalDiagram());
        blocks.add(paragraph("The symbol sheet below is the M18C visual check for diode, LED, SPST switch, and quarter-turn geometry."));
        blocks.add(sampleElectricalSymbolsDiagram());
        blocks.add(paragraph("The branched schematic below is the M18E visual check for explicit junctions and derived electrical nets."));
        blocks.add(sampleElectricalNetDiagram());
        blocks.add(paragraph("The drawing below is the M19A visual check for authored mechanical primitives."));
        blocks.add(sampleMechanicalPrimitivesDiagram());
        blocks.add(paragraph("The drawing below is the M19B visual check for semantic dimensions and derived callouts."));
        blocks.add(sampleMechanicalDimensionsDiagram());
        blocks.add(paragraph("The drawing below is the M19C visual check for semantic mechanical constraints and derived relationship markers."));
        blocks.add(sampleMechanicalConstraintsDiagram());
        blocks.add(paragraph("The drawing below is the M19D visual check for reusable semantic mechanical symbols."));
        blocks.add(sampleMechanicalSymbolsDiagram());
        blocks.add(paragraph("The drawing below is the M19E visual check for editable mechanical labels, notes, and leader callouts."));
        blocks.add(sampleMechanicalAnnotationsDiagram());
        blocks.add(paragraph("The assembly below is the M19F visual check for semantic item balloons and generated BOM data."));
        blocks.add(sampleMechanicalAssemblyDiagram());
        blocks.add(sampleMechanicalAssemblyBom());
        return new Document(blocks);
    }

    public static Document createEditable() {
        return new Document(List.of(
                paragraph("Editable paragraph: velocity changes over time, and notes may include Δx, Δt, θ, λ, or café. "
                        + "Type more text here to verify that the paragraph wraps and reflows while the caret follows the edited document."),
                sampleElectricalDiagram(),
                paragraph("The M18E branched circuit below exercises an explicit junction and one multi-terminal electrical net."),
                sampleElectricalNetDiagram(),
                paragraph("The M18C symbol sheet below exercises the remaining approved basic electrical symbols."),
                sampleElectricalSymbolsDiagram(),
                paragraph("The M19A drawing below exercises the first mechanical primitive vocabulary."),
                sampleMechanicalPrimitivesDiagram(),
                paragraph("The M19B drawing below exercises semantic horizontal, vertical, aligned, radius, diameter, and angular dimensions."),
                sampleMechanicalDimensionsDiagram(),
                paragraph("The M19C drawing below exercises horizontal, vertical, coincident, parallel, perpendicular, and concentric relationships."),
                sampleMechanicalConstraintsDiagram(),
                paragraph("The M19D drawing below exercises reusable semantic mechanical symbols."),
                sampleMechanicalSymbolsDiagram(),
                paragraph("The M19E drawing below exercises semantic part labels, notes, and leader callouts."),
                sampleMechanicalAnnotationsDiagram(),
                paragraph("The M19F assembly below exercises item balloons linked by semantic target IDs."),
                sampleMechanicalAssemblyDiagram(),
                sampleMechanicalAssemblyBom(),
                paragraph("Paragraph between the electrical schematic and plot for atomic navigation checks."),
                samplePlot(),
                paragraph("Paragraph between the plot and generic diagram for atomic navigation checks."),
                sampleDiagram(),
                paragraph("Paragraph between the diagram and table for atomic navigation checks."),
                scientificTable(),
                paragraph("Paragraph after the table for atomic navigation, selection, deletion, and undo/redo checks.")));
    }

    private static EquationBlock averageVelocityEquation() {
        var numerator = new MathSequence(List.of(
                new MathSymbol("Δ", MathSymbolKind.GREEK),
                new MathIdentifier("x")));
        var denominator = new MathSequence(List.of(
                new MathSymbol("Δ", MathSymbolKind.GREEK),
                new MathIdentifier("t")));
        var fraction = new MathFraction(numerator, denominator);
        return new EquationBlock(new MathSequence(List.of(
                new MathIdentifier("v"),
                new MathOperator("=", MathOperatorRole.RELATION),
                fraction)));
    }

    private static EquationBlock semanticTokenEquation() {
        return new EquationBlock(new MathSequence(List.of(
                new MathNamedOperator("sin"),
                new MathSymbol("(", MathSymbolKind.OTHER),
                new MathIdentifier("x"),
                new MathSymbol(")", MathSymbolKind.OTHER),
                new MathOperator("=", MathOperatorRole.RELATION),
                new MathNumber("0"),
                new MathText("if"),
                new MathIdentifier("x"),
                new MathOperator("=", MathOperatorRole.RELATION),
                new MathNumber("0"))));
    }

    private static EquationBlock simpleFractionReference() {
        return equation(
                id("a"),
                op("+"),
                new MathFraction(id("b"), id("c")),
                op("="),
                id("d"));
    }

    private static EquationBlock compoundFractionReference() {
        return equation(
                id("v"),
                op("="),
                new MathFraction(
                        sequence(id("x"), op("+"), number("1")),
                        sequence(id("t"), op("-"), number("1"))));
    }

    private static EquationBlock squareRootReference() {
        return equation(
                id("r"),
                op("="),
                new MathRoot(sequence(id("x"), op("+"), number("1")), Optional.empty()));
    }

    private static EquationBlock indexedRootReference() {
        return equation(
                id("q"),
                op("="),
                new MathRoot(id("x"), Optional.of(number("3"))));
    }

    private static EquationBlock rootFractionReference() {
        return equation(
                id("m"),
                op("="),
                new MathRoot(new MathFraction(
                        sequence(id("x"), op("+"), number("1")),
                        sequence(id("y"), op("+"), number("2"))), Optional.empty()));
    }

    private static EquationBlock scriptReference() {
        return equation(
                script(id("x"), null, number("2")),
                op("+"),
                script(id("a"), id("i"), null),
                op("+"),
                script(id("v"), id("0"), number("2")));
    }

    private static EquationBlock combinedScriptReference() {
        return equation(
                script(new MathRoot(id("x"), Optional.empty()), null, number("2")),
                op("+"),
                script(new MathFraction(id("a"), id("b")), id("i"), null),
                op("="),
                number("0"));
    }

    private static EquationBlock nestedStructureReference() {
        return equation(
                id("E"),
                op("="),
                new MathFraction(
                        sequence(
                                script(id("x"), null, number("2")),
                                op("+"),
                                script(id("y"), null, number("2"))),
                        new MathRoot(sequence(id("z"), op("+"), number("1")), Optional.empty())));
    }

    private static PlotBlock samplePlot() {
        return new PlotBlock(PlotDefinition.of(
                "Position vs Time",
                AxisDefinition.linear("Time (s)"),
                AxisDefinition.linear("Position (m)"),
                List.of(
                        new PlotSeries(
                                "Motion",
                                PlotSeriesKind.LINE,
                                List.of(
                                        new DataPoint(0, 0),
                                        new DataPoint(1, 1),
                                        new DataPoint(2, 4),
                                        new DataPoint(3, 9))),
                        new PlotSeries(
                                "Samples",
                                PlotSeriesKind.SCATTER,
                                List.of(
                                        new DataPoint(0.5, 0.25),
                                        new DataPoint(1.5, 2.25),
                                        new DataPoint(2.5, 6.25))))));
    }

    private static DiagramBlock sampleDiagram() {
        var sensorId = new DiagramElementId("sensor");
        var processorId = new DiagramElementId("processor");
        var sensorOut = new DiagramPortId("out");
        var processorIn = new DiagramPortId("in");
        return new DiagramBlock(new DiagramDefinition(
                "System Diagram",
                new DiagramCanvas(100, 50),
                List.of(
                        new DiagramNode(
                                sensorId,
                                new DiagramBounds(8, 8, 30, 18),
                                "Sensor",
                                List.of(new DiagramPort(
                                        sensorOut,
                                        "",
                                        new DiagramPortPlacement(DiagramPortSide.RIGHT, 0.5)))),
                        new DiagramNode(
                                processorId,
                                new DiagramBounds(62, 18, 30, 18),
                                "Processor",
                                List.of(new DiagramPort(
                                        processorIn,
                                        "",
                                        new DiagramPortPlacement(DiagramPortSide.LEFT, 0.5))))),
                List.of(new DiagramConnection(
                        new DiagramEndpoint(sensorId, sensorOut),
                        new DiagramEndpoint(processorId, processorIn),
                        "signal"))));
    }

    private static DiagramBlock sampleElectricalDiagram() {
        var sourceId = new DiagramElementId("v1");
        var resistorId = new DiagramElementId("r1");
        var capacitorId = new DiagramElementId("c1");
        var groundId = new DiagramElementId("gnd");
        return new DiagramBlock(new DiagramDefinition(
                "Basic DC Circuit",
                new DiagramCanvas(120, 75),
                List.of(
                        new ElectricalComponent(
                                sourceId,
                                new DiagramBounds(10, 18, 18, 30),
                                ElectricalComponentKind.DC_VOLTAGE_SOURCE,
                                ElectricalOrientation.DEG_90,
                                "V1",
                                "5 V"),
                        new ElectricalComponent(
                                resistorId,
                                new DiagramBounds(42, 10, 34, 12),
                                ElectricalComponentKind.RESISTOR,
                                ElectricalOrientation.DEG_0,
                                "R1",
                                "10 kΩ"),
                        new ElectricalComponent(
                                capacitorId,
                                new DiagramBounds(88, 24, 14, 26),
                                ElectricalComponentKind.CAPACITOR,
                                ElectricalOrientation.DEG_90,
                                "C1",
                                "100 nF"),
                        new ElectricalComponent(
                                groundId,
                                new DiagramBounds(82, 58, 18, 12),
                                ElectricalComponentKind.GROUND,
                                ElectricalOrientation.DEG_0,
                                "GND",
                                "")),
                List.of(
                        new DiagramConnection(
                                new DiagramEndpoint(sourceId, new DiagramPortId("positive")),
                                new DiagramEndpoint(resistorId, new DiagramPortId("a")),
                                ""),
                        new DiagramConnection(
                                new DiagramEndpoint(resistorId, new DiagramPortId("b")),
                                new DiagramEndpoint(capacitorId, new DiagramPortId("a")),
                                ""),
                        new DiagramConnection(
                                new DiagramEndpoint(capacitorId, new DiagramPortId("b")),
                                new DiagramEndpoint(groundId, new DiagramPortId("ground")),
                                ""),
                        new DiagramConnection(
                                new DiagramEndpoint(sourceId, new DiagramPortId("negative")),
                                new DiagramEndpoint(groundId, new DiagramPortId("ground")),
                                ""))));
    }

    private static DiagramBlock sampleElectricalNetDiagram() {
        var sourceId = new DiagramElementId("net-v1");
        var resistorId = new DiagramElementId("net-r1");
        var capacitorId = new DiagramElementId("net-c1");
        var diodeId = new DiagramElementId("net-d1");
        var groundId = new DiagramElementId("net-gnd");
        var junctionId = new DiagramElementId("junction-1");
        var junction = new ElectricalJunction(junctionId, new DiagramBounds(67, 24, 4, 4), "VOUT");
        return new DiagramBlock(new DiagramDefinition(
                "Junction + Net",
                new DiagramCanvas(130, 82),
                List.of(
                        new ElectricalComponent(sourceId, new DiagramBounds(8, 24, 18, 30),
                                ElectricalComponentKind.DC_VOLTAGE_SOURCE, ElectricalOrientation.DEG_90, "V1", "5 V"),
                        new ElectricalComponent(resistorId, new DiagramBounds(34, 18, 28, 12),
                                ElectricalComponentKind.RESISTOR, ElectricalOrientation.DEG_0, "R1", "1 kΩ"),
                        junction,
                        new ElectricalComponent(capacitorId, new DiagramBounds(82, 14, 14, 28),
                                ElectricalComponentKind.CAPACITOR, ElectricalOrientation.DEG_90, "C1", "100 nF"),
                        new ElectricalComponent(diodeId, new DiagramBounds(82, 48, 28, 12),
                                ElectricalComponentKind.DIODE, ElectricalOrientation.DEG_0, "D1", "1N4148"),
                        new ElectricalComponent(groundId, new DiagramBounds(52, 66, 18, 12),
                                ElectricalComponentKind.GROUND, ElectricalOrientation.DEG_0, "GND", "")),
                List.of(
                        new DiagramConnection(new DiagramEndpoint(sourceId, new DiagramPortId("positive")),
                                new DiagramEndpoint(resistorId, new DiagramPortId("a")), ""),
                        new DiagramConnection(new DiagramEndpoint(resistorId, new DiagramPortId("b")),
                                new DiagramEndpoint(junctionId, ElectricalJunction.LEFT), ""),
                        new DiagramConnection(new DiagramEndpoint(junctionId, ElectricalJunction.RIGHT),
                                new DiagramEndpoint(capacitorId, new DiagramPortId("a")), ""),
                        new DiagramConnection(new DiagramEndpoint(junctionId, ElectricalJunction.BOTTOM),
                                new DiagramEndpoint(diodeId, new DiagramPortId("anode")), ""),
                        new DiagramConnection(new DiagramEndpoint(capacitorId, new DiagramPortId("b")),
                                new DiagramEndpoint(groundId, new DiagramPortId("ground")), ""),
                        new DiagramConnection(new DiagramEndpoint(diodeId, new DiagramPortId("cathode")),
                                new DiagramEndpoint(groundId, new DiagramPortId("ground")), ""),
                        new DiagramConnection(new DiagramEndpoint(sourceId, new DiagramPortId("negative")),
                                new DiagramEndpoint(groundId, new DiagramPortId("ground")), ""))));
    }

    private static DiagramBlock sampleElectricalSymbolsDiagram() {
        return new DiagramBlock(new DiagramDefinition(
                "Electrical Symbols",
                new DiagramCanvas(120, 58),
                List.of(
                        new ElectricalComponent(
                                new DiagramElementId("d1"),
                                new DiagramBounds(10, 18, 28, 12),
                                ElectricalComponentKind.DIODE,
                                ElectricalOrientation.DEG_0,
                                "D1",
                                "1N4148"),
                        new ElectricalComponent(
                                new DiagramElementId("led1"),
                                new DiagramBounds(52, 10, 14, 30),
                                ElectricalComponentKind.LED,
                                ElectricalOrientation.DEG_90,
                                "D2",
                                "LED"),
                        new ElectricalComponent(
                                new DiagramElementId("s1"),
                                new DiagramBounds(82, 18, 28, 12),
                                ElectricalComponentKind.SWITCH_SPST,
                                ElectricalOrientation.DEG_180,
                                "S1",
                                "SPST")),
                List.of()));
    }

    private static DiagramBlock sampleMechanicalPrimitivesDiagram() {
        return new DiagramBlock(new DiagramDefinition(
                "Mechanical Primitives",
                new DiagramCanvas(130, 82),
                List.of(
                        new MechanicalPrimitive(new DiagramElementId("mech-line"), new DiagramBounds(8, 12, 34, 8), MechanicalPrimitiveKind.LINE),
                        new MechanicalPrimitive(new DiagramElementId("mech-centerline"), new DiagramBounds(50, 12, 60, 8), MechanicalPrimitiveKind.CENTERLINE),
                        new MechanicalPrimitive(new DiagramElementId("mech-rect"), new DiagramBounds(10, 32, 28, 22), MechanicalPrimitiveKind.RECTANGLE),
                        new MechanicalPrimitive(new DiagramElementId("mech-circle"), new DiagramBounds(48, 30, 24, 24), MechanicalPrimitiveKind.CIRCLE),
                        new MechanicalPrimitive(new DiagramElementId("mech-arc"), new DiagramBounds(80, 30, 24, 24), MechanicalPrimitiveKind.ARC),
                        new MechanicalPrimitive(new DiagramElementId("mech-arrow"), new DiagramBounds(12, 64, 42, 10), MechanicalPrimitiveKind.ARROW),
                        new MechanicalPrimitive(new DiagramElementId("mech-ref"), new DiagramBounds(82, 64, 10, 10), MechanicalPrimitiveKind.REFERENCE_POINT)),
                List.of()));
    }

    private static DiagramBlock sampleMechanicalDimensionsDiagram() {
        return new DiagramBlock(new DiagramDefinition(
                "Mechanical Dimensions",
                new DiagramCanvas(130, 86),
                List.of(
                        new MechanicalPrimitive(new DiagramElementId("dim-rect"), new DiagramBounds(12, 24, 36, 24), MechanicalPrimitiveKind.RECTANGLE),
                        new MechanicalDimension(new DiagramElementId("dim-h"), new DiagramBounds(12, 10, 36, 12), MechanicalDimensionKind.HORIZONTAL),
                        new MechanicalDimension(new DiagramElementId("dim-v"), new DiagramBounds(50, 24, 12, 24), MechanicalDimensionKind.VERTICAL),
                        new MechanicalPrimitive(new DiagramElementId("dim-circle"), new DiagramBounds(76, 22, 24, 24), MechanicalPrimitiveKind.CIRCLE),
                        new MechanicalDimension(new DiagramElementId("dim-r"), new DiagramBounds(76, 22, 24, 24), MechanicalDimensionKind.RADIUS),
                        new MechanicalDimension(new DiagramElementId("dim-dia"), new DiagramBounds(102, 22, 24, 24), MechanicalDimensionKind.DIAMETER),
                        new MechanicalDimension(new DiagramElementId("dim-aligned"), new DiagramBounds(14, 58, 38, 20), MechanicalDimensionKind.ALIGNED),
                        new MechanicalDimension(new DiagramElementId("dim-angle"), new DiagramBounds(72, 56, 32, 24), MechanicalDimensionKind.ANGLE)),
                List.of()));
    }

    private static DiagramBlock sampleMechanicalConstraintsDiagram() {
        var h1 = new DiagramElementId("constraint-h-line");
        var v1 = new DiagramElementId("constraint-v-line");
        var p1 = new DiagramElementId("constraint-parallel-a");
        var p2 = new DiagramElementId("constraint-parallel-b");
        var perp = new DiagramElementId("constraint-perp");
        var c1 = new DiagramElementId("constraint-circle-a");
        var c2 = new DiagramElementId("constraint-circle-b");
        var co1 = new DiagramElementId("constraint-co-a");
        var co2 = new DiagramElementId("constraint-co-b");
        return new DiagramBlock(new DiagramDefinition(
                "Mechanical Constraints",
                new DiagramCanvas(130, 92),
                List.of(
                        new MechanicalPrimitive(h1, new DiagramBounds(8, 10, 34, 8), MechanicalPrimitiveKind.LINE, MechanicalOrientation.DEG_0),
                        MechanicalConstraint.unary(new DiagramElementId("constraint-h"), new DiagramBounds(22, 11, 6, 6), MechanicalConstraintKind.HORIZONTAL, h1),
                        new MechanicalPrimitive(v1, new DiagramBounds(50, 8, 10, 34), MechanicalPrimitiveKind.LINE, MechanicalOrientation.DEG_90),
                        MechanicalConstraint.unary(new DiagramElementId("constraint-v"), new DiagramBounds(52, 21, 6, 6), MechanicalConstraintKind.VERTICAL, v1),

                        new MechanicalPrimitive(p1, new DiagramBounds(8, 48, 30, 8), MechanicalPrimitiveKind.LINE, MechanicalOrientation.DEG_0),
                        new MechanicalPrimitive(p2, new DiagramBounds(8, 62, 30, 8), MechanicalPrimitiveKind.LINE, MechanicalOrientation.DEG_0),
                        MechanicalConstraint.binary(new DiagramElementId("constraint-par"), new DiagramBounds(22, 54, 6, 6), MechanicalConstraintKind.PARALLEL, p1, p2),

                        new MechanicalPrimitive(perp, new DiagramBounds(48, 48, 10, 30), MechanicalPrimitiveKind.LINE, MechanicalOrientation.DEG_90),
                        MechanicalConstraint.binary(new DiagramElementId("constraint-perp-rel"), new DiagramBounds(42, 55, 6, 6), MechanicalConstraintKind.PERPENDICULAR, p1, perp),

                        new MechanicalPrimitive(c1, new DiagramBounds(76, 10, 28, 28), MechanicalPrimitiveKind.CIRCLE),
                        new MechanicalPrimitive(c2, new DiagramBounds(82, 16, 16, 16), MechanicalPrimitiveKind.CIRCLE),
                        MechanicalConstraint.binary(new DiagramElementId("constraint-conc"), new DiagramBounds(87, 21, 6, 6), MechanicalConstraintKind.CONCENTRIC, c1, c2),

                        new MechanicalPrimitive(co1, new DiagramBounds(78, 54, 10, 10), MechanicalPrimitiveKind.REFERENCE_POINT),
                        new MechanicalPrimitive(co2, new DiagramBounds(78, 54, 10, 10), MechanicalPrimitiveKind.REFERENCE_POINT),
                        MechanicalConstraint.binary(new DiagramElementId("constraint-co"), new DiagramBounds(80, 56, 6, 6), MechanicalConstraintKind.COINCIDENT, co1, co2)),
                List.of()));
    }

    private static DiagramBlock sampleMechanicalAnnotationsDiagram() {
        return new DiagramBlock(new DiagramDefinition(
                "Mechanical Annotations", new DiagramCanvas(130, 76),
                List.of(
                        new MechanicalAnnotation(new DiagramElementId("annotation-part"), new DiagramBounds(12,12,32,14), MechanicalAnnotationKind.PART_LABEL, "BRACKET A"),
                        new MechanicalAnnotation(new DiagramElementId("annotation-note"), new DiagramBounds(58,12,52,16), MechanicalAnnotationKind.NOTE, "REMOVE BURRS"),
                        new MechanicalAnnotation(new DiagramElementId("annotation-leader"), new DiagramBounds(18,44,54,18), MechanicalAnnotationKind.LEADER, "M6 HOLE"),
                        new MechanicalAnnotation(new DiagramElementId("annotation-leader-2"), new DiagramBounds(76,42,42,18), MechanicalAnnotationKind.LEADER, "SHAFT")),
                List.of()));
    }

    private static DiagramBlock sampleMechanicalSymbolsDiagram() {
        return new DiagramBlock(new DiagramDefinition("Mechanical Symbols", new DiagramCanvas(130,92), List.of(
                new MechanicalSymbol(new DiagramElementId("symbol-shaft"), new DiagramBounds(8,12,42,10), MechanicalSymbolKind.SHAFT),
                new MechanicalSymbol(new DiagramElementId("symbol-gear"), new DiagramBounds(60,8,30,30), MechanicalSymbolKind.GEAR),
                new MechanicalSymbol(new DiagramElementId("symbol-bearing"), new DiagramBounds(98,8,26,26), MechanicalSymbolKind.BEARING),
                new MechanicalSymbol(new DiagramElementId("symbol-spring"), new DiagramBounds(8,48,42,18), MechanicalSymbolKind.SPRING),
                new MechanicalSymbol(new DiagramElementId("symbol-piston"), new DiagramBounds(58,48,36,22), MechanicalSymbolKind.PISTON),
                new MechanicalSymbol(new DiagramElementId("symbol-bolt"), new DiagramBounds(98,50,28,16), MechanicalSymbolKind.BOLT)
        ), List.of()));
    }

    private static DiagramBlock sampleMechanicalAssemblyDiagram() {
        var shaftId=new DiagramElementId("assembly-shaft");var bearingId=new DiagramElementId("assembly-bearing");var gearId=new DiagramElementId("assembly-gear");
        return new DiagramBlock(new DiagramDefinition("Mechanical Assembly + BOM",new DiagramCanvas(130,76),List.of(
                new MechanicalSymbol(shaftId,new DiagramBounds(18,30,72,10),MechanicalSymbolKind.SHAFT),
                new MechanicalSymbol(bearingId,new DiagramBounds(50,22,22,22),MechanicalSymbolKind.BEARING),
                new MechanicalSymbol(gearId,new DiagramBounds(84,20,26,26),MechanicalSymbolKind.GEAR),
                new MechanicalPartReference(new DiagramElementId("part-reference-1"),new DiagramBounds(18,10,12,12),shaftId,1,"SHAFT",1,"Drive shaft"),
                new MechanicalPartReference(new DiagramElementId("part-reference-2"),new DiagramBounds(52,52,12,12),bearingId,2,"BEARING",2,"Support bearing"),
                new MechanicalPartReference(new DiagramElementId("part-reference-3"),new DiagramBounds(108,8,12,12),gearId,3,"GEAR",1,"Output gear")
        ),List.of()));
    }

    private static TableBlock sampleMechanicalAssemblyBom() {
        return new TableBlock(List.of(
                new TableRow(List.of(tableCell("ITEM"),tableCell("PART"),tableCell("QTY"),tableCell("DESCRIPTION"))),
                new TableRow(List.of(tableCell("1"),tableCell("SHAFT"),tableCell("1"),tableCell("Drive shaft"))),
                new TableRow(List.of(tableCell("2"),tableCell("BEARING"),tableCell("2"),tableCell("Support bearing"))),
                new TableRow(List.of(tableCell("3"),tableCell("GEAR"),tableCell("1"),tableCell("Output gear")))
        ),1);
    }

    private static TableBlock scientificTable() {
        return new TableBlock(List.of(
                tableRow("Quantity", "Value", "Unit"),
                tableRow("Voltage", "12", "V"),
                tableRow("Current", "2", "A"),
                tableRow("Resistance", "6", "Ω"),
                new TableRow(List.of(tableCell("Empty check"), TableCell.empty(), tableCell("reserved")))
        ), 1);
    }

    private static EquationBlock equation(dev.rgcb.scholar.math.MathExpression... expressions) {
        return new EquationBlock(sequence(expressions));
    }

    private static MathSequence sequence(dev.rgcb.scholar.math.MathExpression... expressions) {
        return new MathSequence(List.of(expressions));
    }

    private static MathIdentifier id(String value) {
        return new MathIdentifier(value);
    }

    private static MathNumber number(String value) {
        return new MathNumber(value);
    }

    private static MathOperator op(String value) {
        var role = "=".equals(value) ? MathOperatorRole.RELATION : MathOperatorRole.BINARY;
        return new MathOperator(value, role);
    }

    private static MathScript script(
            dev.rgcb.scholar.math.MathExpression base,
            dev.rgcb.scholar.math.MathExpression subscript,
            dev.rgcb.scholar.math.MathExpression superscript
    ) {
        return new MathScript(base, Optional.ofNullable(subscript), Optional.ofNullable(superscript));
    }

    private static Paragraph paragraph(String text) {
        return new Paragraph(new InlineContent(List.of((InlineNode) new Text(text, Set.of()))));
    }

    private static TableRow tableRow(String... cells) {
        return new TableRow(java.util.Arrays.stream(cells)
                .map(DevelopmentDocument::tableCell)
                .toList());
    }

    private static TableCell tableCell(String text) {
        return new TableCell(new TableCellContent(new InlineContent(List.of((InlineNode) new Text(text, Set.of())))));
    }
}
