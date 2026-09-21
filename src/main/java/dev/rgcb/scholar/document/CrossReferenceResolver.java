package dev.rgcb.scholar.document;

import dev.rgcb.scholar.math.clipboard.MathPlainTextSerializer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class CrossReferenceResolver {
    private final MathPlainTextSerializer mathSerializer = new MathPlainTextSerializer();
    private final DocumentStructureResolver structureResolver = new DocumentStructureResolver();

    public CrossReferenceResolution resolve(Document document, CrossReference reference) {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(reference, "reference");
        return targets(document).stream()
                .filter(target -> target.kind() == reference.kind())
                .filter(target -> target.targetId().equals(reference.targetId()))
                .findFirst()
                .map(target -> CrossReferenceResolution.resolved(reference, target))
                .orElseGet(() -> CrossReferenceResolution.missing(reference));
    }

    public List<CrossReferenceTarget> targets(Document document) {
        Objects.requireNonNull(document, "document");
        var targets = new ArrayList<CrossReferenceTarget>();
        var figureNumber = 1;
        var tableNumber = 1;
        var equationNumber = 1;
        var structure = structureResolver.resolve(document);
        for (var blockIndex = 0; blockIndex < document.blocks().size(); blockIndex++) {
            var block = document.blocks().get(blockIndex);
            if (block instanceof FigureBlock figure) {
                targets.add(new CrossReferenceTarget(
                        CrossReferenceTargetKind.FIGURE,
                        figure.id(),
                        blockIndex,
                        figureNumber,
                        "Figure " + figureNumber,
                        inlinePreview(figure.caption())));
                figureNumber++;
            } else if (block instanceof TableBlock table) {
                var currentBlockIndex = blockIndex;
                var currentTableNumber = tableNumber;
                table.id().ifPresent(id -> targets.add(new CrossReferenceTarget(
                        CrossReferenceTargetKind.TABLE,
                        id,
                        currentBlockIndex,
                        currentTableNumber,
                        "Table " + currentTableNumber,
                        tableDescription(table))));
                tableNumber++;
            } else if (block instanceof EquationBlock equation) {
                var currentBlockIndex = blockIndex;
                var currentEquationNumber = equationNumber;
                equation.id().ifPresent(id -> targets.add(new CrossReferenceTarget(
                        CrossReferenceTargetKind.EQUATION,
                        id,
                        currentBlockIndex,
                        currentEquationNumber,
                        "Equation " + currentEquationNumber,
                        equationDescription(equation))));
                equationNumber++;
            } else if (block instanceof Heading heading) {
                var currentBlockIndex = blockIndex;
                var section = structure.sectionAtBlock(blockIndex).orElseThrow();
                heading.id().ifPresent(id -> targets.add(new CrossReferenceTarget(
                        CrossReferenceTargetKind.SECTION,
                        id,
                        currentBlockIndex,
                        section.number().parts().get(section.number().parts().size() - 1),
                        "Section " + section.number().displayText(),
                        inlinePreview(heading.content()))));
            }
        }
        return List.copyOf(targets);
    }

    public String inlineText(Document document, InlineContent content) {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(content, "content");
        var text = new StringBuilder();
        for (var node : content.nodes()) {
            if (node instanceof Text run) {
                text.append(run.content());
            } else if (node instanceof QuantityInline quantity) {
                text.append(new dev.rgcb.scholar.quantity.ScientificNumberFormatter()
                        .format(quantity.value(), quantity.notation(), true));
            } else if (node instanceof CrossReference reference) {
                text.append(resolve(document, reference).displayText());
            } else if (node instanceof QuantityInline quantity) {
                text.append(new dev.rgcb.scholar.quantity.ScientificNumberFormatter()
                        .format(quantity.value(), quantity.notation(), false));
            } else {
                throw new IllegalArgumentException("Unsupported inline node: " + node.getClass().getName());
            }
        }
        return text.toString();
    }

    private static String tableDescription(TableBlock table) {
        if (table.rows().isEmpty() || table.rows().get(0).cells().isEmpty()) {
            return "";
        }
        var firstCell = table.rows().get(0).cells().get(0);
        var text = new StringBuilder();
        for (var node : firstCell.content().content().nodes()) {
            if (node instanceof Text run) {
                text.append(run.content());
            }
        }
        return text.toString();
    }

    private static String inlinePreview(InlineContent content) {
        var text = new StringBuilder();
        for (var node : content.nodes()) {
            if (node instanceof Text run) {
                text.append(run.content());
            } else if (node instanceof CrossReference) {
                text.append("[Reference]");
            } else if (node instanceof QuantityInline quantity) {
                text.append(new dev.rgcb.scholar.quantity.ScientificNumberFormatter()
                        .format(quantity.value(), quantity.notation(), true));
            }
        }
        return text.toString();
    }

    private String equationDescription(EquationBlock equation) {
        Optional<String> text = mathSerializer.serialize(equation.expression());
        return text.orElse("");
    }
}
