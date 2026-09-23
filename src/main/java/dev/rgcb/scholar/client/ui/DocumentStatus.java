package dev.rgcb.scholar.client.ui;

import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.FigureBlock;
import dev.rgcb.scholar.document.Heading;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.Paragraph;
import dev.rgcb.scholar.document.TableBlock;
import dev.rgcb.scholar.document.Text;
import dev.rgcb.scholar.layout.LaidOutDocument;
import java.text.BreakIterator;
import java.util.Locale;

/** Derived status only; never part of document history or persistence. */
public final class DocumentStatus {
    private DocumentStatus() { }

    public static int pageAt(LaidOutDocument layout, int logicalY) {
        if (layout.pages().isEmpty()) return 0;
        int best = 0;
        int distance = Integer.MAX_VALUE;
        for (int i = 0; i < layout.pages().size(); i++) {
            var page = layout.pages().get(i);
            int gap = logicalY < page.y() ? page.y() - logicalY
                    : Math.max(0, logicalY - (page.y() + page.height()));
            if (gap < distance) { best = i; distance = gap; }
        }
        return best + 1;
    }

    public static int wordCount(Document document) {
        int count = 0;
        for (var block : document.blocks()) {
            if (block instanceof Paragraph paragraph) count += count(paragraph.content());
            else if (block instanceof Heading heading) count += count(heading.content());
            else if (block instanceof FigureBlock figure) count += count(figure.caption());
            else if (block instanceof TableBlock table && table.datasetBinding().isEmpty()) {
                for (var row : table.rows()) for (var cell : row.cells()) count += count(cell.content().content());
            }
        }
        return count;
    }

    private static int count(InlineContent content) {
        var text = new StringBuilder();
        for (var node : content.nodes()) {
            if (node instanceof Text run) text.append(run.content());
            else text.append(' ');
        }
        var iterator = BreakIterator.getWordInstance(Locale.ROOT);
        iterator.setText(text.toString());
        int count = 0;
        for (int start = iterator.first(), end = iterator.next(); end != BreakIterator.DONE;
             start = end, end = iterator.next()) {
            if (text.substring(start, end).codePoints().anyMatch(Character::isLetterOrDigit)) count++;
        }
        return count;
    }
}
