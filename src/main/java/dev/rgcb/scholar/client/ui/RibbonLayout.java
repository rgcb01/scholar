package dev.rgcb.scholar.client.ui;

import java.util.ArrayList;
import java.util.List;

/** Pure responsive ribbon geometry. Horizontal overflow remains reachable by scrolling. */
public final class RibbonLayout {
    public static final int GROUP_GAP = 3;
    public static final int GROUP_LABEL_HEIGHT = 10;
    public static final int COMMAND_GAP = 2;
    public static final int LARGE_HEIGHT = 43;
    public static final int ROW_HEIGHT = 20;

    private RibbonLayout() { }

    public static Result compute(RibbonTabDefinition tab, int availableWidth) {
        var full = compute(tab, RibbonLabelMode.FULL);
        if (full.contentWidth() <= availableWidth) return full;
        var shortened = compute(tab, RibbonLabelMode.SHORT);
        if (shortened.contentWidth() <= availableWidth) return shortened;
        return compute(tab, RibbonLabelMode.ICON_ONLY);
    }

    private static Result compute(RibbonTabDefinition tab, RibbonLabelMode labelMode) {
        var commandBounds = new ArrayList<CommandBounds>();
        var groupBounds = new ArrayList<GroupBounds>();
        var x = 4;
        for (var groupIndex = 0; groupIndex < tab.groups().size(); groupIndex++) {
            var group = tab.groups().get(groupIndex);
            var groupStart = x;
            var columnX = x + 3;
            var commandIndex = 0;
            while (commandIndex < group.commands().size()) {
                var command = group.commands().get(commandIndex);
                if (command.size() == RibbonCommandSize.LARGE) {
                    var width = width(command, labelMode);
                    commandBounds.add(new CommandBounds(groupIndex, commandIndex, command,
                            new ShellRect(columnX, 3, width, LARGE_HEIGHT), labelMode));
                    columnX += width + COMMAND_GAP;
                    commandIndex++;
                    continue;
                }
                var secondIndex = commandIndex + 1;
                var second = secondIndex < group.commands().size() ? group.commands().get(secondIndex) : null;
                if (second != null && second.size() == RibbonCommandSize.LARGE) second = null;
                var columnWidth = Math.max(width(command, labelMode), second == null ? 0 : width(second, labelMode));
                commandBounds.add(new CommandBounds(groupIndex, commandIndex, command,
                        new ShellRect(columnX, 3, columnWidth, ROW_HEIGHT), labelMode));
                if (second != null) {
                    commandBounds.add(new CommandBounds(groupIndex, secondIndex, second,
                            new ShellRect(columnX, 3 + ROW_HEIGHT + COMMAND_GAP, columnWidth, ROW_HEIGHT), labelMode));
                    commandIndex += 2;
                } else {
                    commandIndex++;
                }
                columnX += columnWidth + COMMAND_GAP;
            }
            var groupWidth = Math.max(estimatedTextWidth(group.label()) + 10, columnX - groupStart + 2);
            groupBounds.add(new GroupBounds(groupIndex, group.label(), new ShellRect(groupStart, 1, groupWidth, 55)));
            x = groupStart + groupWidth + GROUP_GAP;
        }
        return new Result(List.copyOf(groupBounds), List.copyOf(commandBounds), Math.max(0, x + 2), labelMode);
    }

    private static int width(RibbonCommandPresentation command, RibbonLabelMode labelMode) {
        if (command.size() == RibbonCommandSize.SMALL || labelMode == RibbonLabelMode.ICON_ONLY) {
            return command.size() == RibbonCommandSize.LARGE ? 36 : 22;
        }
        var labelWidth = command.dropdown() && labelMode == RibbonLabelMode.FULL
                ? command.choices().stream().mapToInt(choice -> estimatedTextWidth(command.label(choice, labelMode)))
                        .max().orElse(estimatedTextWidth(command.label(command.action(), labelMode)))
                : estimatedTextWidth(command.label(command.action(), labelMode));
        return switch (command.size()) {
            case LARGE -> Math.max(48, labelWidth + 8);
            case MEDIUM -> Math.max(58, labelWidth + 28 + (command.dropdown() ? 8 : 0));
            case SMALL -> 22;
        };
    }

    static int estimatedTextWidth(String label) {
        return label.length() * 6;
    }

    public record Result(List<GroupBounds> groups, List<CommandBounds> commands, int contentWidth,
                         RibbonLabelMode labelMode) { }
    public record GroupBounds(int groupIndex, String label, ShellRect bounds) { }
    public record CommandBounds(int groupIndex, int commandIndex, RibbonCommandPresentation command,
                                ShellRect bounds, RibbonLabelMode labelMode) { }
}
