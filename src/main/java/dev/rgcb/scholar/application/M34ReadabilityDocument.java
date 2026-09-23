package dev.rgcb.scholar.application;

import dev.rgcb.scholar.analysis.AnalysisKind;
import dev.rgcb.scholar.data.DatasetColumn;
import dev.rgcb.scholar.data.DatasetColumnType;
import dev.rgcb.scholar.data.DatasetPlotBinding;
import dev.rgcb.scholar.data.DatasetRow;
import dev.rgcb.scholar.data.DatasetTableBinding;
import dev.rgcb.scholar.data.DatasetValue;
import dev.rgcb.scholar.data.ScientificDataset;
import dev.rgcb.scholar.document.BlockNode;
import dev.rgcb.scholar.document.ColumnLayout;
import dev.rgcb.scholar.document.CrossReference;
import dev.rgcb.scholar.document.CrossReferenceTargetKind;
import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.DocumentTemplateId;
import dev.rgcb.scholar.document.DocumentTemplates;
import dev.rgcb.scholar.document.EquationBlock;
import dev.rgcb.scholar.document.FigureBlock;
import dev.rgcb.scholar.document.Heading;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.InlineNode;
import dev.rgcb.scholar.document.LayoutSectionBreak;
import dev.rgcb.scholar.document.Paragraph;
import dev.rgcb.scholar.document.ParagraphFormat;
import dev.rgcb.scholar.document.PlotBlock;
import dev.rgcb.scholar.document.SemanticStyle;
import dev.rgcb.scholar.document.TableBlock;
import dev.rgcb.scholar.document.Text;
import dev.rgcb.scholar.document.TextMark;
import dev.rgcb.scholar.document.DatasetAnalysisBlock;
import dev.rgcb.scholar.math.MathFraction;
import dev.rgcb.scholar.math.MathIdentifier;
import dev.rgcb.scholar.math.MathNumber;
import dev.rgcb.scholar.math.MathOperator;
import dev.rgcb.scholar.math.MathOperatorRole;
import dev.rgcb.scholar.math.MathRoot;
import dev.rgcb.scholar.math.MathScript;
import dev.rgcb.scholar.math.MathSequence;
import dev.rgcb.scholar.math.MathSymbol;
import dev.rgcb.scholar.math.MathSymbolKind;
import dev.rgcb.scholar.plot.AxisDefinition;
import dev.rgcb.scholar.plot.PlotDefinition;
import dev.rgcb.scholar.plot.PlotSeries;
import dev.rgcb.scholar.plot.PlotSeriesKind;
import dev.rgcb.scholar.quantity.NumberNotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Opt-in production document for M34 readability review; never auto-created. */
final class M34ReadabilityDocument {
    private static final String LOREM = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. "
            + "Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. "
            + "Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.";

    private M34ReadabilityDocument() { }

    static Document create() {
        var blocks = new ArrayList<BlockNode>();
        blocks.add(new Paragraph(text("M34 — High-Fidelity Rendering Test"), SemanticStyle.TITLE, ParagraphFormat.none()));
        blocks.add(new Paragraph(text("Scientific document readability sample"), SemanticStyle.AUTHOR, ParagraphFormat.none()));
        blocks.add(new Heading("m34-typography", 1, text("Typography")));
        blocks.add(paragraph(LOREM));
        blocks.add(new Paragraph(new InlineContent(List.of(
                new Text("Normal prose, ", Set.of()), new Text("bold", Set.of(TextMark.BOLD)),
                new Text(", ", Set.of()), new Text("italic", Set.of(TextMark.ITALIC)),
                new Text(", and ", Set.of()), new Text("bold italic", Set.of(TextMark.BOLD, TextMark.ITALIC)),
                new Text(" remain distinct across wrapped scientific paragraphs. " + LOREM, Set.of())))));
        blocks.add(paragraph(LOREM));
        blocks.add(new Heading("m34-scientific", 2, text("Scientific Typography")));
        blocks.add(paragraph("H₂O and CO₂ are measured alongside x², x₁, m/s², ΔT, °C, Δ°C, Σ, ∫, and √. " + LOREM));
        blocks.add(new Heading("m34-equations", 3, text("Mathematics")));
        blocks.add(new EquationBlock("m34-energy", new MathSequence(List.of(new MathIdentifier("E"),
                new MathOperator("=", MathOperatorRole.RELATION), new MathIdentifier("m"),
                new MathScript(new MathIdentifier("c"), Optional.empty(), Optional.of(new MathNumber("2")))))));
        blocks.add(new EquationBlock("m34-acceleration", new MathSequence(List.of(new MathIdentifier("g"),
                new MathOperator("=", MathOperatorRole.RELATION), new MathNumber("9.81"),
                new MathIdentifier("m"), new MathOperator("/", MathOperatorRole.BINARY),
                new MathScript(new MathIdentifier("s"), Optional.empty(), Optional.of(new MathNumber("2")))))));
        blocks.add(new EquationBlock("m34-fraction", new MathSequence(List.of(
                new MathRoot(new MathFraction(new MathIdentifier("x"), new MathNumber("2")), Optional.empty()),
                new MathOperator("+", MathOperatorRole.BINARY),
                new MathFraction(new MathNumber("1"), new MathFraction(new MathIdentifier("g"), new MathNumber("2")))))));
        blocks.add(new EquationBlock("m34-summation", new MathSequence(List.of(
                new MathScript(new MathSymbol("∑", MathSymbolKind.CALCULUS),
                        Optional.of(new MathSequence(List.of(new MathIdentifier("i"),
                                new MathOperator("=", MathOperatorRole.RELATION), new MathNumber("1")))),
                        Optional.of(new MathIdentifier("n"))), new MathIdentifier("x")))));
        blocks.add(new EquationBlock("m34-integral", new MathSequence(List.of(
                new MathScript(new MathSymbol("∫", MathSymbolKind.CALCULUS),
                        Optional.of(new MathIdentifier("a")), Optional.of(new MathIdentifier("b"))),
                new MathIdentifier("f"), new MathIdentifier("x"), new MathIdentifier("dx")))));
        blocks.add(paragraph("The numerical acceleration used below is g = 9.81 m/s². " + LOREM));
        blocks.add(new Heading("m34-data", 2, text("Measurement and Fit")));
        blocks.add(new TableBlock(new DatasetTableBinding("m34-response")).withId("m34-table"));
        blocks.add(new DatasetAnalysisBlock("m34-fit", "m34-response", AnalysisKind.QUADRATIC_FIT,
                Optional.of("time"), "value", Optional.empty(), NumberNotation.DECIMAL));
        var plot = new PlotBlock(PlotDefinition.of("Measured response and quadratic fit",
                AxisDefinition.linear("Time (s)"), AxisDefinition.linear("Response (m)"),
                List.of(new PlotSeries("Measured", PlotSeriesKind.SCATTER,
                                new DatasetPlotBinding("m34-response", "time", "value")),
                        PlotSeries.fit("Quadratic fit", "m34-fit"))));
        blocks.add(new FigureBlock("m34-figure", plot, text("Measured response with a quadratic fit.")));
        blocks.add(new Paragraph(new InlineContent(List.of(new Text("The data in ", Set.of()),
                new CrossReference(CrossReferenceTargetKind.FIGURE, "m34-figure"),
                new Text(" are summarized in the table above. " + LOREM, Set.of())))));
        blocks.add(new LayoutSectionBreak(ColumnLayout.two()));
        blocks.add(new Heading("m34-columns", 2, text("Two-Column Discussion")));
        for (var index = 0; index < 10; index++) blocks.add(paragraph(LOREM + " " + LOREM));
        blocks.add(new LayoutSectionBreak(ColumnLayout.one()));
        blocks.add(new Heading("m34-conclusion", 2, text("Conclusion")));
        blocks.add(paragraph(LOREM));
        blocks.add(paragraph(""));
        var dataset = new ScientificDataset("m34-response", "Response Trial", List.of(
                new DatasetColumn("time", "Time (s)", DatasetColumnType.NUMBER),
                new DatasetColumn("value", "Response (m)", DatasetColumnType.NUMBER)), List.of(
                row("0", "0.2"), row("1", "1.1"), row("2", "4.0"), row("3", "8.9"), row("4", "16.2")));
        return new Document(blocks, List.of(dataset), DocumentTemplates.settings(DocumentTemplateId.IEEE_STYLE));
    }

    private static DatasetRow row(String x, String y) {
        return new DatasetRow(List.of(DatasetValue.number(x), DatasetValue.number(y)));
    }

    private static Paragraph paragraph(String value) { return new Paragraph(text(value)); }
    private static InlineContent text(String value) { return new InlineContent(List.<InlineNode>of(new Text(value, Set.of()))); }
}
