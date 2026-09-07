package dev.rgcb.scholar.editor;

import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.InlineNode;
import dev.rgcb.scholar.document.Text;
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
            if (!(node instanceof Text text)) {
                throw new IllegalArgumentException("Unsupported inline node: " + node.getClass().getName());
            }

            var nodeLength = TextBoundary.characterCount(text.content());
            var nodeStart = offset;
            var nodeEnd = nodeStart + nodeLength;
            if (nodeEnd <= logicalOffset) {
                left.add(text);
            } else if (nodeStart >= logicalOffset) {
                right.add(text);
            } else {
                var splitOffset = logicalOffset - nodeStart;
                if (splitOffset > 0) {
                    left.add(new Text(TextBoundary.substring(text.content(), 0, splitOffset), text.marks()));
                }
                if (splitOffset < nodeLength) {
                    right.add(new Text(TextBoundary.substring(text.content(), splitOffset, nodeLength), text.marks()));
                }
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

    static int characterCount(InlineContent content) {
        Objects.requireNonNull(content, "content");
        var count = 0;
        for (var node : content.nodes()) {
            if (!(node instanceof Text text)) {
                throw new IllegalArgumentException("Unsupported inline node: " + node.getClass().getName());
            }
            count += TextBoundary.characterCount(text.content());
        }
        return count;
    }
}
