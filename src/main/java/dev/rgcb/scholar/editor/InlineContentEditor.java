package dev.rgcb.scholar.editor;

import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.InlineNode;
import dev.rgcb.scholar.document.Text;
import dev.rgcb.scholar.document.CrossReference;
import dev.rgcb.scholar.document.QuantityInline;
import java.util.ArrayList;
import java.util.Objects;

final class InlineContentEditor {
    private InlineContentEditor() {
    }

    static InlineContentSplit split(InlineContent content, int logicalOffset) {
        Objects.requireNonNull(content, "content");
        var length = characterCount(content);
        if (logicalOffset < 0 || logicalOffset > length) {
            throw new IllegalArgumentException("logicalOffset is outside the inline content.");
        }

        var left = new ArrayList<InlineNode>();
        var right = new ArrayList<InlineNode>();
        var offset = 0;
        for (var node : content.nodes()) {
            var nodeLength = characterCount(node);
            var nodeStart = offset;
            var nodeEnd = nodeStart + nodeLength;
            if (nodeEnd <= logicalOffset) {
                left.add(node);
            } else if (nodeStart >= logicalOffset) {
                right.add(node);
            } else if (node instanceof Text text) {
                var splitOffset = logicalOffset - nodeStart;
                if (splitOffset > 0) {
                    left.add(new Text(TextBoundary.substring(text.content(), 0, splitOffset), text.marks(), text.format()));
                }
                if (splitOffset < nodeLength) {
                    right.add(new Text(TextBoundary.substring(text.content(), splitOffset, nodeLength), text.marks(), text.format()));
                }
            } else {
                throw new IllegalArgumentException("Cannot split through atomic inline node: " + node.getClass().getName());
            }
            offset = nodeEnd;
        }

        return new InlineContentSplit(new InlineContent(left), new InlineContent(right));
    }

    static InlineContent concat(InlineContent left, InlineContent right) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        var nodes = new ArrayList<InlineNode>(left.nodes());
        nodes.addAll(right.nodes());
        return new InlineContent(nodes);
    }

    static InlineContent slice(InlineContent content, int startOffset, int endOffset) {
        Objects.requireNonNull(content, "content");
        TextBoundary.validateRange(logicalText(content), startOffset, endOffset);
        var leftSplit = split(content, startOffset);
        return split(leftSplit.right(), endOffset - startOffset).left();
    }

    static int characterCount(InlineContent content) {
        Objects.requireNonNull(content, "content");
        var count = 0;
        for (var node : content.nodes()) {
            count += characterCount(node);
        }
        return count;
    }

    static String logicalText(InlineContent content) {
        Objects.requireNonNull(content, "content");
        var text = new StringBuilder();
        for (var node : content.nodes()) {
            if (node instanceof Text run) {
                text.append(run.content());
            } else if (node instanceof CrossReference || node instanceof QuantityInline) {
                text.append('\uFFFC');
            } else {
                throw new IllegalArgumentException("Unsupported inline node: " + node.getClass().getName());
            }
        }
        return text.toString();
    }

    static int characterCount(InlineNode node) {
        Objects.requireNonNull(node, "node");
        if (node instanceof Text text) {
            return TextBoundary.characterCount(text.content());
        }
        if (node instanceof CrossReference || node instanceof QuantityInline) {
            return 1;
        }
        throw new IllegalArgumentException("Unsupported inline node: " + node.getClass().getName());
    }
}
