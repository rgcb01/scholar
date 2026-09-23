package dev.rgcb.scholar.transfer;

import dev.rgcb.scholar.data.ScientificDataset;
import dev.rgcb.scholar.document.BlockNode;
import dev.rgcb.scholar.document.ComputationTransferBlock;
import dev.rgcb.scholar.document.DatasetAnalysisBlock;
import dev.rgcb.scholar.document.CrossReference;
import dev.rgcb.scholar.document.DiagramBlock;
import dev.rgcb.scholar.document.EquationBlock;
import dev.rgcb.scholar.document.FigureBlock;
import dev.rgcb.scholar.document.Heading;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.Paragraph;
import dev.rgcb.scholar.document.PlotBlock;
import dev.rgcb.scholar.document.TableBlock;
import dev.rgcb.scholar.document.TableOfContentsBlock;
import dev.rgcb.scholar.document.PageBreak;
import dev.rgcb.scholar.document.LayoutSectionBreak;
import dev.rgcb.scholar.document.Text;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Derived semantic membership, never independent authority over the fragment AST. */
public record FragmentIdentityIndex(Set<StableIdentityKey> provided, Set<StableIdentityKey> referenced) {
    public FragmentIdentityIndex {
        provided = Set.copyOf(provided);
        referenced = Set.copyOf(referenced);
    }

    static FragmentIdentityIndex derive(FragmentContent content, List<ScientificDataset> resources) {
        var provided = new LinkedHashSet<StableIdentityKey>();
        var referenced = new LinkedHashSet<StableIdentityKey>();
        collectContent(content, provided, referenced);
        for (var resource : resources) {
            provide(provided, new StableIdentityKey(StableIdentityKind.DATASET, resource.id()));
        }
        if (content instanceof FragmentContent.ResourcePrimary primary
                && !provided.containsAll(primary.selectedResources())) {
            throw new IllegalArgumentException("Selected resource must exist in fragment resources.");
        }
        return new FragmentIdentityIndex(provided, referenced);
    }

    static List<StableIdentityKey> requiredDatasets(FragmentContent content) {
        var referenced = new LinkedHashSet<StableIdentityKey>();
        collectContent(content, new LinkedHashSet<>(), referenced);
        return referenced.stream().filter(key -> key.kind() == StableIdentityKind.DATASET).toList();
    }

    private static void collectContent(FragmentContent content, Set<StableIdentityKey> provided, Set<StableIdentityKey> referenced) {
        if (content instanceof FragmentContent.Blocks blocks) {
            blocks.roots().forEach(root -> visitBlock(root, provided, referenced));
        } else if (content instanceof FragmentContent.InlineSegments inline) {
            inline.segments().forEach(segment -> visitInline(segment, referenced));
        }
    }

    public static Optional<StableIdentityKey> blockIdentity(BlockNode block) {
        if (block instanceof DatasetAnalysisBlock analysis) {
            return Optional.of(new StableIdentityKey(StableIdentityKind.ANALYSIS, analysis.id()));
        }
        if (block instanceof ComputationTransferBlock computation) {
            return computation.definedVariableId().map(id -> new StableIdentityKey(StableIdentityKind.VARIABLE, id));
        }
        if (block instanceof Heading heading) {
            return heading.id().map(id -> new StableIdentityKey(StableIdentityKind.SECTION, id));
        }
        if (block instanceof EquationBlock equation) {
            return equation.id().map(id -> new StableIdentityKey(StableIdentityKind.EQUATION, id));
        }
        if (block instanceof TableBlock table) {
            return table.id().map(id -> new StableIdentityKey(StableIdentityKind.TABLE, id));
        }
        if (block instanceof FigureBlock figure) {
            return Optional.of(new StableIdentityKey(StableIdentityKind.FIGURE, figure.id()));
        }
        return Optional.empty();
    }

    private static void visitBlock(BlockNode block, Set<StableIdentityKey> provided, Set<StableIdentityKey> referenced) {
        blockIdentity(block).ifPresent(key -> provide(provided, key));
        if (block instanceof Paragraph paragraph) {
            visitInline(paragraph.content(), referenced);
        } else if (block instanceof Heading heading) {
            visitInline(heading.content(), referenced);
        } else if (block instanceof TableBlock table) {
            table.rows().forEach(row -> row.cells().forEach(cell -> visitInline(cell.content().content(), referenced)));
            table.datasetBinding().ifPresent(binding -> referenced.add(datasetKey(binding.datasetId())));
        } else if (block instanceof PlotBlock plot) {
            plot.definition().series().forEach(series -> series.datasetBinding()
                    .ifPresent(binding -> referenced.add(datasetKey(binding.datasetId()))));
            plot.definition().series().forEach(series -> series.fitAnalysisId()
                    .ifPresent(id -> referenced.add(new StableIdentityKey(StableIdentityKind.ANALYSIS, id))));
        } else if (block instanceof DatasetAnalysisBlock analysis) {
            referenced.add(datasetKey(analysis.datasetId()));
        } else if (block instanceof FigureBlock figure) {
            visitInline(figure.caption(), referenced);
            visitBlock(figure.content(), provided, referenced);
        } else if (block instanceof ComputationTransferBlock computation) {
            computation.variableDependencies().forEach(dependency -> referenced.add(
                    new StableIdentityKey(StableIdentityKind.VARIABLE, dependency.variableId())));
        } else if (!(block instanceof EquationBlock || block instanceof DiagramBlock || block instanceof TableOfContentsBlock
                || block instanceof PageBreak || block instanceof LayoutSectionBreak)) {
            throw new IllegalArgumentException("Unsupported fragment block type: " + block.getClass().getName());
        }
    }

    private static void visitInline(InlineContent content, Set<StableIdentityKey> referenced) {
        for (var node : content.nodes()) {
            if (node instanceof CrossReference reference) {
                referenced.add(StableIdentityKey.targetOf(reference));
            } else if (!(node instanceof Text) && !(node instanceof dev.rgcb.scholar.document.QuantityInline)) {
                throw new IllegalArgumentException("Unsupported fragment inline type: " + node.getClass().getName());
            }
        }
    }

    private static StableIdentityKey datasetKey(String id) {
        return new StableIdentityKey(StableIdentityKind.DATASET, id);
    }

    private static void provide(Set<StableIdentityKey> provided, StableIdentityKey key) {
        if (!provided.add(key)) {
            throw new IllegalArgumentException("Duplicate source identity: " + key);
        }
    }
}
