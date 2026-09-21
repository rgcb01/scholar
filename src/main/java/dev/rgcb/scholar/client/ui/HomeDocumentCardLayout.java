package dev.rgcb.scholar.client.ui;

import java.util.ArrayList;
import java.util.List;

/** Pure responsive geometry for the Scholar Home document library. */
public final class HomeDocumentCardLayout {
    public static final int CARD_WIDTH = 142;
    public static final int CARD_HEIGHT = 126;
    public static final int GAP = 10;
    public static final int TOP = 48;

    private HomeDocumentCardLayout() { }

    public static Result compute(int screenWidth, int screenHeight, int documentCount, int page) {
        var columns = Math.max(1, (Math.max(CARD_WIDTH, screenWidth - 28) + GAP) / (CARD_WIDTH + GAP));
        var rows = Math.max(1, (Math.max(CARD_HEIGHT, screenHeight - TOP - 34) + GAP) / (CARD_HEIGHT + GAP));
        var slots = Math.max(2, columns * rows);
        var documentsPerPage = Math.max(1, slots - 1);
        var maxPage = Math.max(0, (Math.max(0, documentCount) - 1) / documentsPerPage);
        var safePage = Math.max(0, Math.min(page, maxPage));
        var visibleDocuments = Math.min(documentsPerPage, Math.max(0, documentCount - safePage * documentsPerPage));
        var visibleSlots = visibleDocuments + 1;
        var usedColumns = Math.min(columns, visibleSlots);
        var gridWidth = usedColumns * CARD_WIDTH + Math.max(0, usedColumns - 1) * GAP;
        var startX = Math.max(8, (screenWidth - gridWidth) / 2);
        var cards = new ArrayList<ShellRect>();
        for (var slot = 0; slot < visibleSlots; slot++) {
            var column = slot % columns;
            var row = slot / columns;
            cards.add(new ShellRect(startX + column * (CARD_WIDTH + GAP), TOP + row * (CARD_HEIGHT + GAP),
                    CARD_WIDTH, CARD_HEIGHT));
        }
        return new Result(cards.getFirst(), List.copyOf(cards.subList(1, cards.size())),
                documentsPerPage, safePage, maxPage, columns);
    }

    public record Result(ShellRect newDocumentCard, List<ShellRect> documentCards, int documentsPerPage,
                         int page, int maxPage, int columns) { }
}
