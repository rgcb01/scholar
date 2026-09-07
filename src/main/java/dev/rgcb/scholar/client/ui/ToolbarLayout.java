package dev.rgcb.scholar.client.ui;

import dev.rgcb.scholar.editor.EditorAction;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.ToIntFunction;

public final class ToolbarLayout {
    public static final int LEFT_PADDING = 5;
    public static final int TOP_PADDING = 3;
    public static final int BUTTON_HEIGHT = 18;
    public static final int MIN_BUTTON_WIDTH = 34;
    public static final int BUTTON_HORIZONTAL_PADDING = 14;
    public static final int BLOCK_STYLE_WIDTH = 78;
    public static final int GROUP_WIDTH = 58;
    public static final int SEMANTIC_CONVERT_WIDTH = 72;
    public static final int GAP = 3;
    public static final int SEPARATOR_WIDTH = 8;

    private ToolbarLayout() {
    }

    public static List<ToolbarItemBounds> compute(
            List<ToolbarItem> items,
            int availableWidth,
            ToIntFunction<EditorAction> labelWidth
    ) {
        Objects.requireNonNull(items, "items");
        Objects.requireNonNull(labelWidth, "labelWidth");
        var bounds = new ArrayList<ToolbarItemBounds>();
        var x = LEFT_PADDING;
        for (var index = 0; index < items.size(); index++) {
            var item = items.get(index);
            var width = switch (item.kind()) {
                case SEPARATOR -> SEPARATOR_WIDTH;
                case BLOCK_STYLE -> BLOCK_STYLE_WIDTH;
                case GROUP -> GROUP_WIDTH;
                case SEMANTIC_CONVERT -> SEMANTIC_CONVERT_WIDTH;
                case ACTION -> Math.max(MIN_BUTTON_WIDTH, labelWidth.applyAsInt(item.action().orElseThrow()) + BUTTON_HORIZONTAL_PADDING);
            };
            if (x + width > availableWidth - LEFT_PADDING) {
                break;
            }
            bounds.add(new ToolbarItemBounds(index, item.kind(), x, TOP_PADDING, width, BUTTON_HEIGHT));
            x += width + GAP;
        }
        return List.copyOf(bounds);
    }
}
