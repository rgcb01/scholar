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
        if (blocks.get(0) instanceof Heading heading) {
            blocks.set(0, heading.withId("motion-section"));
        }
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
        blocks.add(19, paragraph(
                text("Cross-reference check: see "),
                ref(CrossReferenceTargetKind.SECTION, "motion-section"),
                text(", "),
                ref(CrossReferenceTargetKind.EQUATION, "average-velocity-equation"),
                text(", "),
                ref(CrossReferenceTargetKind.TABLE, "scientific-table"),
                text(", and "),
                ref(CrossReferenceTargetKind.FIGURE, "measured-response"),
                text(".")));
        blocks.add(20, paragraph(
                text("Broken reference check: "),
                ref(CrossReferenceTargetKind.FIGURE, "missing-development-figure"),
                text(".")));
        blocks.add(21, paragraph("The plot below is the M16D visual check for semantic LINE and SCATTER XY series."));
        blocks.add(22, samplePlot());
        blocks.add(23, paragraph("The next table and plot are M23 dataset-backed views. Editing the Projectile Test dataset should update both without changing their bindings."));
        blocks.add(24, datasetBackedProjectileTable());
        blocks.add(25, datasetBackedProjectilePlot());
        blocks.add(26, paragraph("The compact table below uses a stable column subset from the same dataset."));
        blocks.add(27, datasetBackedProjectileHeightTable());
        blocks.add(28, paragraph("The next table intentionally points at a missing dataset so broken bindings remain visible and deterministic."));
        blocks.add(29, brokenDatasetTable());
        blocks.add(30, paragraph("The mixed dataset below includes text, numbers, and missing values for import/export and rendering checks."));
        blocks.add(31, datasetBackedMixedTable());
        blocks.add(32, paragraph("The diagram below is the M17B visual check for semantic nodes, ports, and a derived connection."));
        blocks.add(33, sampleDiagram());
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
        blocks.add(paragraph("The assembly below is the first M20 Figure check: a semantic figure wraps an existing mechanical diagram and owns a generated number plus caption."));
        blocks.add(figure("shaft-assembly", sampleMechanicalAssemblyDiagram(), "Mechanical shaft assembly."));
        blocks.add(sampleMechanicalAssemblyBom());
        blocks.add(paragraph("The following figures verify document-order numbering across mixed scientific media."));
        blocks.add(figure("measured-response", samplePlot(), "Measured response."));
        blocks.add(figure("control-loop", sampleDiagram(), "Signal path from sensor to processor."));
        blocks.add(paragraph("The headings below are M22 structure fixtures for derived numbering, outline navigation, skipped levels, and section references."));
        blocks.add(heading("m22-introduction", 1, "M22 Document Structure"));
        blocks.add(paragraph(text("This paragraph references "), ref(CrossReferenceTargetKind.SECTION, "m22-methods"), text(" and "), ref(CrossReferenceTargetKind.SECTION, "m22-skipped-calibration"), text(".")));
        blocks.add(heading("m22-methods", 2, "Methods"));
        blocks.add(heading("m22-setup", 3, "Experimental setup"));
        blocks.add(heading("m22-results", 2, "Results"));
        blocks.add(heading("m22-skipped-calibration", 4, "Skipped calibration detail"));
        assignMissingHeadingIds(blocks);
        blocks.add(1, new TableOfContentsBlock());
        return new Document(blocks, developmentDatasets());
    }

    public static Document createEditable() {
        return new Document(List.of(
                paragraph("Editable paragraph: velocity changes over time, and notes may include Δx, Δt, θ, λ, or café. "
                        + "Type more text here to verify that the paragraph wraps and reflows while the caret follows the edited document."),
                new TableOfContentsBlock(),
                heading("m24b-editor-navigation", 2, "M24B editor navigation fixture"),
                paragraph(
                        text("Inline atomic reference check: move across "),
                        ref(CrossReferenceTargetKind.FIGURE, "shaft-assembly"),
                        text(" with Left/Right and Shift+Left/Right.")),
                heading("m24d-structural-editing", 2, "M24D structural editing fixture"),
                paragraph("Use this local fixture for Enter split, Backspace merge, Delete at atomic boundaries, block Cut/Paste, right-click structural actions, and validation checks."),
                heading("m24e-history-transactions", 2, "M24E history transaction fixture"),
                paragraph("Use this section for repeated Ctrl+Z and Ctrl+Y checks after text edits, heading splits, table edits, plot edits, diagram drags, figure caption edits, dataset cell edits, block paste, and structural deletes."),
                paragraph("Navigation, right-click menus, outline jumps, TOC jumps, scroll, zoom, pan, drag preview, and popup open or close should not add undo steps."),
                heading("m24f-input-focus", 2, "M24F input and focus fixture"),
                paragraph("Use this section to move between prose, equations, tables, plots, diagrams, figure captions, context menus, popups, and toolbar menus. One input event should have one visible owner and one deterministic result."),
                paragraph("Manual checks: Escape unwinds one layer at a time, Tab moves within nested controls, Ctrl+A selects the active scope, clipboard shortcuts follow the active selection, and undo/redo restore valid focus."),
                heading("m24g-final-foundation", 2, "M24G final foundation fixture"),
                paragraph("Use the mixed blocks below as the final editor-foundation regression document: prose, equation, dataset-backed views, electrical and mechanical diagrams, figures, cross-references, TOC, table editing, object selection, clipboard, and undo/redo should remain coherent together."),
                paragraph("Manual pass: click into each editable domain, use arrow keys and Tab where supported, right-click the active target, copy/cut/paste supported selections, then undo and redo across systems without leaving stale focus or invalid selections."),
                averageVelocityEquation(),
                paragraph("Paragraph after the equation for atomic block traversal, Enter-to-edit, and Escape-return checks."),
                paragraph("M23 dataset-backed table and plot below share the Projectile Test dataset."),
                datasetBackedProjectileTable(),
                datasetBackedProjectilePlot(),
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
                paragraph("The M20 figure below wraps the M19F assembly diagram with semantic caption and derived numbering."),
                figure("shaft-assembly", sampleMechanicalAssemblyDiagram(), "Mechanical shaft assembly."),
                paragraph(
                        text("Cross-reference editing check: see "),
                        ref(CrossReferenceTargetKind.FIGURE, "shaft-assembly"),
                        text(", "),
                        ref(CrossReferenceTargetKind.FIGURE, "measured-response"),
                        text(", and broken "),
                        ref(CrossReferenceTargetKind.FIGURE, "missing-editable-figure"),
                        text(".")),
                sampleMechanicalAssemblyBom(),
                paragraph("Paragraph between the electrical schematic and plot for atomic navigation checks."),
                figure("measured-response", samplePlot(), "Measured response."),
                paragraph("Paragraph between the plot and generic diagram for atomic navigation checks."),
                figure("system-diagram", sampleDiagram(), "Generic system diagram."),
                paragraph("Paragraph between the diagram and table for atomic navigation checks."),
                scientificTable(),
                paragraph("Paragraph after the table for atomic navigation, selection, deletion, and undo/redo checks.")), developmentDatasets());
    }

    private static EquationBlock averageVelocityEquation() {
        var numerator = new MathSequence(List.of(
                new MathSymbol("Δ", MathSymbolKind.GREEK),
                new MathIdentifier("x")));
        var denominator = new MathSequence(List.of(
                new MathSymbol("Δ", MathSymbolKind.GREEK),
                new MathIdentifier("t")));
        var fraction = new MathFraction(numerator, denominator);
        return new EquationBlock("average-velocity-equation", new MathSequence(List.of(
                new MathIdentifier("v"),
                new MathOperator("=", MathOperatorRole.RELATION),
                fraction)));
    }

    private static EquationBlock semanticTokenEquation() {
        return new EquationBlock("semantic-token-equation", new MathSequence(List.of(
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

    private static TableBlock datasetBackedProjectileTable() {
        return new TableBlock(new DatasetTableBinding("projectile-test"));
    }

    private static TableBlock datasetBackedProjectileHeightTable() {
        return new TableBlock(new DatasetTableBinding("projectile-test", List.of("time", "height")));
    }

    private static TableBlock datasetBackedMixedTable() {
        return new TableBlock(new DatasetTableBinding("mixed-observations"));
    }

    private static TableBlock brokenDatasetTable() {
        return new TableBlock(new DatasetTableBinding("missing-dataset"));
    }

    private static PlotBlock datasetBackedProjectilePlot() {
        return new PlotBlock(PlotDefinition.of(
                "Projectile Test Dataset",
                AxisDefinition.linear("Time (s)"),
                AxisDefinition.linear("Height (m)"),
                List.of(new PlotSeries(
                        "Height",
                        PlotSeriesKind.LINE,
                        new DatasetPlotBinding("projectile-test", "time", "height")))));
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
        return new TableBlock("assembly-bom", List.of(
                new TableRow(List.of(tableCell("ITEM"),tableCell("PART"),tableCell("QTY"),tableCell("DESCRIPTION"))),
                new TableRow(List.of(tableCell("1"),tableCell("SHAFT"),tableCell("1"),tableCell("Drive shaft"))),
                new TableRow(List.of(tableCell("2"),tableCell("BEARING"),tableCell("2"),tableCell("Support bearing"))),
                new TableRow(List.of(tableCell("3"),tableCell("GEAR"),tableCell("1"),tableCell("Output gear")))
        ),1);
    }

    private static TableBlock scientificTable() {
        return new TableBlock("scientific-table", List.of(
                tableRow("Quantity", "Value", "Unit"),
                tableRow("Voltage", "12", "V"),
                tableRow("Current", "2", "A"),
                tableRow("Resistance", "6", "Ω"),
                new TableRow(List.of(tableCell("Empty check"), TableCell.empty(), tableCell("reserved")))
        ), 1);
    }

    private static List<ScientificDataset> developmentDatasets() {
        return List.of(projectileDataset(), mixedObservationsDataset());
    }

    private static ScientificDataset projectileDataset() {
        return new ScientificDataset(
                "projectile-test",
                "Projectile Test",
                List.of(
                        new DatasetColumn("time", "Time (s)", DatasetColumnType.NUMBER),
                        new DatasetColumn("height", "Height (m)", DatasetColumnType.NUMBER),
                        new DatasetColumn("note", "Note", DatasetColumnType.TEXT)),
                List.of(
                        new DatasetRow(List.of(DatasetValue.number("0"), DatasetValue.number("0"), DatasetValue.text("launch"))),
                        new DatasetRow(List.of(DatasetValue.number("1"), DatasetValue.number("5"), DatasetValue.text("apex"))),
                        new DatasetRow(List.of(DatasetValue.number("2"), DatasetValue.number("0"), DatasetValue.text("landing")))));
    }

    private static ScientificDataset mixedObservationsDataset() {
        return new ScientificDataset(
                "mixed-observations",
                "Mixed Observations",
                List.of(
                        new DatasetColumn("trial", "Trial", DatasetColumnType.TEXT),
                        new DatasetColumn("temperature", "Temperature", DatasetColumnType.NUMBER),
                        new DatasetColumn("status", "Status", DatasetColumnType.TEXT),
                        new DatasetColumn("reading", "Reading", DatasetColumnType.NUMBER)),
                List.of(
                        new DatasetRow(List.of(DatasetValue.text("A"), DatasetValue.number("21.5"), DatasetValue.text("ok"), DatasetValue.number("3.2"))),
                        new DatasetRow(List.of(DatasetValue.text("B"), DatasetValue.missing(), DatasetValue.text("missing temperature"), DatasetValue.number("4.1"))),
                        new DatasetRow(List.of(DatasetValue.text("C"), DatasetValue.number("22"), DatasetValue.text("text reading"), DatasetValue.text("n/a")))));
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

    private static Paragraph paragraph(InlineNode... nodes) {
        return new Paragraph(new InlineContent(List.of(nodes)));
    }

    private static Heading heading(String id, int level, String text) {
        return new Heading(id, level, new InlineContent(List.of((InlineNode) new Text(text, Set.of()))));
    }

    private static Text text(String text) {
        return new Text(text, Set.of());
    }

    private static CrossReference ref(CrossReferenceTargetKind kind, String targetId) {
        return new CrossReference(kind, targetId);
    }

    private static FigureBlock figure(String id, BlockNode content, String caption) {
        return new FigureBlock(id, content, new InlineContent(List.of((InlineNode) new Text(caption, Set.of()))));
    }

    private static void assignMissingHeadingIds(List<BlockNode> blocks) {
        var headingNumber = 1;
        for (var index = 0; index < blocks.size(); index++) {
            if (blocks.get(index) instanceof Heading heading && heading.id().isEmpty()) {
                blocks.set(index, heading.withId("development-section-" + headingNumber));
            }
            if (blocks.get(index) instanceof Heading) {
                headingNumber++;
            }
        }
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
