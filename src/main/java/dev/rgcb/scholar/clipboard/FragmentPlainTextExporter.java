package dev.rgcb.scholar.clipboard;

import dev.rgcb.scholar.data.DatasetTableResolver;
import dev.rgcb.scholar.data.DatasetPlotResolver;
import dev.rgcb.scholar.data.DatasetTsvSerializer;
import dev.rgcb.scholar.diagram.clipboard.DiagramPlainTextSerializer;
import dev.rgcb.scholar.document.*;
import dev.rgcb.scholar.figure.clipboard.FigurePlainTextSerializer;
import dev.rgcb.scholar.math.clipboard.MathPlainTextSerializer;
import dev.rgcb.scholar.plot.clipboard.PlotPlainTextSerializer;
import dev.rgcb.scholar.table.clipboard.TableTsvSerializer;
import dev.rgcb.scholar.transfer.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Source-display export only; never used to materialize rich structure. */
public final class FragmentPlainTextExporter {
    private final CrossReferenceResolver references = new CrossReferenceResolver();
    private final DatasetPlotResolver plots = new DatasetPlotResolver();
    private final MathPlainTextSerializer math = new MathPlainTextSerializer();
    private final DocumentPlainTextSerializer documentText = new DocumentPlainTextSerializer();
    public String export(Document source, DocumentFragment fragment) {
        if (fragment.content() instanceof FragmentContent.InlineSegments inline) {
            return String.join("\n", inline.segments().stream().map(segment -> references.inlineText(source, segment)).toList());
        }
        if (fragment.content() instanceof FragmentContent.ResourcePrimary) {
            return String.join("\n\n", fragment.resources().stream().map(new DatasetTsvSerializer()::serialize).toList());
        }
        var text = new ArrayList<String>();
        for (var block : ((FragmentContent.Blocks) fragment.content()).roots()) {
            var index = identityIndex(source, block);
            if (block instanceof TableBlock table) {
                var display = new DatasetTableResolver().resolve(source, table);
                var rows = display.rows().stream().map(row -> new TableRow(row.cells().stream().map(cell ->
                        new TableCell(new TableCellContent(new InlineContent(List.of(new Text(
                                references.inlineText(source, cell.content().content()), Set.of())))))).toList())).toList();
                text.add(new TableTsvSerializer().serialize(new TableBlock(rows, display.headerRowCount())));
            } else if (block instanceof EquationBlock equation) {
                text.add(math.serialize(equation.expression()).orElseThrow(
                        () -> new IllegalArgumentException("Equation cannot be represented as plain text.")));
            } else if (block instanceof PlotBlock plot) {
                text.add(new PlotPlainTextSerializer().serialize(plots.resolve(source, plot)));
            }
            else if (block instanceof DiagramBlock diagram) { text.add(new DiagramPlainTextSerializer().serialize(diagram)); }
            else if (block instanceof FigureBlock figure) {
                text.add(new FigurePlainTextSerializer().serialize(source, figure, FigureNumbering.numberFor(source, index).orElseThrow()));
            } else { text.add(documentText.serializeBlock(source, index, block)); }
        }
        return String.join("\n\n", text);
    }

    private static int identityIndex(Document source, BlockNode block) {
        for (var i = 0; i < source.blocks().size(); i++) { if (source.blocks().get(i) == block) { return i; } }
        throw new IllegalArgumentException("Exported root must belong to source.");
    }
}
