package dev.rgcb.scholar.figure.clipboard;

import dev.rgcb.scholar.diagram.clipboard.DiagramPlainTextSerializer;
import dev.rgcb.scholar.document.CrossReference;
import dev.rgcb.scholar.document.CrossReferenceResolver;
import dev.rgcb.scholar.document.DiagramBlock;
import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.FigureBlock;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.PlotBlock;
import dev.rgcb.scholar.document.Text;
import dev.rgcb.scholar.plot.clipboard.PlotPlainTextSerializer;
import java.util.Objects;

public final class FigurePlainTextSerializer {
    private final PlotPlainTextSerializer plotSerializer = new PlotPlainTextSerializer();
    private final DiagramPlainTextSerializer diagramSerializer = new DiagramPlainTextSerializer();
    private final CrossReferenceResolver referenceResolver = new CrossReferenceResolver();

    public String serialize(FigureBlock figure, int number) {
        return serialize(null, figure, number);
    }

    public String serialize(Document document, FigureBlock figure, int number) {
        Objects.requireNonNull(figure, "figure");
        if (number <= 0) {
            throw new IllegalArgumentException("Figure number must be positive.");
        }
        var output = new StringBuilder();
        output.append("Figure ")
                .append(number)
                .append(": ")
                .append(captionText(document, figure.caption()));
        if (figure.content() instanceof PlotBlock plot) {
            output.append("\n").append(plotSerializer.serialize(plot));
        } else if (figure.content() instanceof DiagramBlock diagram) {
            output.append("\n").append(diagramSerializer.serialize(diagram));
        }
        return output.toString();
    }

    private String captionText(Document document, InlineContent caption) {
        var text = new StringBuilder();
        for (var node : caption.nodes()) {
            if (node instanceof Text run) {
                text.append(run.content());
            } else if (document != null && node instanceof CrossReference reference) {
                text.append(referenceResolver.resolve(document, reference).displayText());
            } else {
                throw new IllegalArgumentException("Unsupported figure caption inline node: " + node.getClass().getName());
            }
        }
        return text.toString().replaceAll("[\\r\\n\\t ]+", " ").trim();
    }
}
