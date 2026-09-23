package dev.rgcb.scholar.transfer;

import dev.rgcb.scholar.data.DatasetPlotBinding;
import dev.rgcb.scholar.data.DatasetTableBinding;
import dev.rgcb.scholar.data.ScientificDataset;
import dev.rgcb.scholar.document.*;
import dev.rgcb.scholar.plot.PlotDefinition;
import dev.rgcb.scholar.plot.PlotSeries;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Applies frozen plan decisions without allocation, destination mutation or resolver calls. */
public final class TransferMaterializer {
    public MaterializationResult materialize(DocumentFragment fragment, TransferPlan plan) {
        if (plan == null || fragment != plan.fragment()) {
            return failure("Materialization requires the exact planned fragment.");
        }
        try {
            var references = new HashMap<String, ReferenceDispositionPlan.Decision>();
            plan.references().decisions().forEach(d -> references.put(d.location(), d));
            FragmentContent content;
            if (fragment.content() instanceof FragmentContent.Blocks blocks) {
                var roots = new ArrayList<BlockNode>();
                for (var i = 0; i < blocks.roots().size(); i++) {
                    roots.add(block(blocks.roots().get(i), "roots[" + i + "]", plan, references));
                }
                content = new FragmentContent.Blocks(roots);
            } else if (fragment.content() instanceof FragmentContent.InlineSegments inline) {
                var segments = new ArrayList<InlineContent>();
                for (var i = 0; i < inline.segments().size(); i++) {
                    segments.add(inline(inline.segments().get(i), "segments[" + i + "]", references));
                }
                content = new FragmentContent.InlineSegments(segments);
            } else {
                var primary = (FragmentContent.ResourcePrimary) fragment.content();
                content = new FragmentContent.ResourcePrimary(primary.selectedResources().stream()
                        .map(plan.identities()::destinationOf).collect(Collectors.toSet()));
            }
            var additions = new ArrayList<ScientificDataset>();
            // Preserve source resource order, not unspecified immutable map iteration order.
            for (var value : fragment.resources()) {
                var decision = plan.resources().decisions().get(new StableIdentityKey(StableIdentityKind.DATASET, value.id()));
                if (decision.disposition() == ResourceTransferPlan.Disposition.TRANSFER_AS_NEW) {
                    additions.add(value.withId(decision.destination().id()));
                }
            }
            return new MaterializationResult.Success(new MaterializedTransfer(plan, content, additions));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return failure(exception.getMessage());
        }
    }

    private static BlockNode block(BlockNode source, String path, TransferPlan plan,
                                   Map<String, ReferenceDispositionPlan.Decision> references) {
        if (source instanceof ComputationTransferBlock computation) {
            var defined = computation.definedVariableId().map(id -> destination(plan, StableIdentityKind.VARIABLE, id));
            var dependencies = new ArrayList<VariableDependencyReference>();
            for (var i = 0; i < computation.variableDependencies().size(); i++) {
                var location = path + ".variableDependencies[" + i + "]";
                var decision = plan.variableDependencies().decisions().stream()
                        .filter(candidate -> candidate.location().equals(location)).findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Missing variable dependency decision."));
                dependencies.add(new VariableDependencyReference(decision.destination().id()));
            }
            var result = computation.withVariableTransferIds(defined, dependencies);
            if (!result.definedVariableId().equals(defined) || !result.variableDependencies().equals(dependencies)) {
                throw new IllegalArgumentException("Computation block did not apply planned variable identities.");
            }
            return result;
        }
        if (source instanceof Paragraph paragraph) { return new Paragraph(inline(paragraph.content(), path + ".content", references), paragraph.style(), paragraph.format()); }
        if (source instanceof Heading heading) {
            return new Heading(heading.level(), inline(heading.content(), path + ".content", references),
                    heading.id().map(id -> destination(plan, StableIdentityKind.SECTION, id)));
        }
        if (source instanceof EquationBlock equation) {
            return new EquationBlock(equation.expression(), equation.id().map(id -> destination(plan, StableIdentityKind.EQUATION, id)));
        }
        if (source instanceof DatasetAnalysisBlock analysis) {
            return new DatasetAnalysisBlock(destination(plan, StableIdentityKind.ANALYSIS, analysis.id()),
                    destination(plan, StableIdentityKind.DATASET, analysis.datasetId()), analysis.kind(),
                    analysis.xColumnId(), analysis.yColumnId(), analysis.displayUnit(), analysis.notation());
        }
        if (source instanceof TableBlock table) {
            var rows = new ArrayList<TableRow>();
            for (var r = 0; r < table.rows().size(); r++) {
                var cells = new ArrayList<TableCell>();
                for (var c = 0; c < table.columnCount(); c++) {
                    cells.add(new TableCell(new TableCellContent(inline(table.rows().get(r).cells().get(c).content().content(),
                            path + ".rows[" + r + "].cells[" + c + "]", references))));
                }
                rows.add(new TableRow(cells));
            }
            return new TableBlock(rows, table.headerRowCount(), table.id().map(id -> destination(plan, StableIdentityKind.TABLE, id)),
                    table.datasetBinding().map(b -> new DatasetTableBinding(destination(plan, StableIdentityKind.DATASET, b.datasetId()), b.columnIds())), table.span());
        }
        if (source instanceof PlotBlock plot) {
            var definition = plot.definition();
            var series = new ArrayList<PlotSeries>();
            for (var i = 0; i < definition.series().size(); i++) {
                var s = definition.series().get(i);
                var location = path + ".series[" + i + "].fitAnalysis";
                var fitId = s.fitAnalysisId().map(id -> plan.analysisDependencies().decisions().stream()
                        .filter(decision -> decision.location().equals(location) && decision.sourceId().equals(id))
                        .findFirst().orElseThrow(() -> new IllegalArgumentException("Missing fit dependency decision."))
                        .destination().id());
                series.add(new PlotSeries(s.name(), s.kind(), s.points(),
                        s.datasetBinding().map(b -> new DatasetPlotBinding(destination(plan, StableIdentityKind.DATASET, b.datasetId()), b.xColumnId(), b.yColumnId())), fitId));
            }
            return new PlotBlock(new PlotDefinition(definition.title(), definition.xAxis(), definition.yAxis(), series,
                    definition.legendVisible(), definition.gridVisible(), definition.height()));
        }
        if (source instanceof FigureBlock figure) {
            return new FigureBlock(destination(plan, StableIdentityKind.FIGURE, figure.id()),
                    block(figure.content(), path + ".content", plan, references), inline(figure.caption(), path + ".caption", references), figure.span());
        }
        if (source instanceof DiagramBlock || source instanceof TableOfContentsBlock || source instanceof PageBreak
                || source instanceof LayoutSectionBreak) { return source; }
        throw new IllegalArgumentException("Unsupported semantic root.");
    }

    private static InlineContent inline(InlineContent content, String path, Map<String, ReferenceDispositionPlan.Decision> decisions) {
        var nodes = new ArrayList<InlineNode>();
        for (var i = 0; i < content.nodes().size(); i++) {
            var node = content.nodes().get(i);
            if (node instanceof CrossReference reference) {
                var decision = decisions.get(path + ".nodes[" + i + "]");
                if (decision == null) { throw new IllegalArgumentException("Missing reference occurrence decision."); }
                node = switch (decision.disposition()) {
                    case REMAP_INTERNAL -> new CrossReference(reference.kind(), decision.destinationTarget().orElseThrow().id());
                    case PRESERVE_EXTERNAL_SAME_DOCUMENT -> reference;
                    case DEGRADE_TO_TEXT -> new Text(decision.fallbackText().orElseThrow(), Set.of());
                };
            }
            nodes.add(node);
        }
        return new InlineContent(nodes);
    }

    private static String destination(TransferPlan plan, StableIdentityKind kind, String id) {
        return plan.identities().destinationOf(new StableIdentityKey(kind, id)).id();
    }

    private static MaterializationResult failure(String message) {
        return new MaterializationResult.Failure(List.of(new TransferDiagnostic(TransferDiagnostic.Severity.ERROR,
                TransferDiagnostic.Code.MALFORMED_FRAGMENT, message, Optional.empty(), Optional.empty())));
    }
}
