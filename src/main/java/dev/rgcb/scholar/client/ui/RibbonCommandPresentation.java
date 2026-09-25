package dev.rgcb.scholar.client.ui;

import dev.rgcb.scholar.editor.EditorAction;
import java.util.List;
import java.util.Objects;

public record RibbonCommandPresentation(
        EditorAction action,
        ScholarIcon icon,
        RibbonCommandSize size,
        List<EditorAction> choices,
        String labelOverride,
        String shortLabel,
        String valuePrefix
) {
    public RibbonCommandPresentation {
        action = Objects.requireNonNull(action, "action");
        icon = Objects.requireNonNull(icon, "icon");
        size = Objects.requireNonNull(size, "size");
        choices = List.copyOf(Objects.requireNonNull(choices, "choices"));
    }

    public static RibbonCommandPresentation action(EditorAction action, RibbonCommandSize size) {
        return new RibbonCommandPresentation(action, ScholarIcons.forAction(action.id()), size, List.of(), null, null, null);
    }

    public static RibbonCommandPresentation action(EditorAction action, RibbonCommandSize size, String shortLabel) {
        return new RibbonCommandPresentation(action, ScholarIcons.forAction(action.id()), size, List.of(), null, shortLabel, null);
    }

    public static RibbonCommandPresentation dropdown(EditorAction selected, List<EditorAction> choices) {
        return new RibbonCommandPresentation(selected, ScholarIcons.STYLE, RibbonCommandSize.MEDIUM, choices,
                null, "scholar.ribbon.group.style", null);
    }

    public static RibbonCommandPresentation valueDropdown(EditorAction selected, List<EditorAction> choices,
                                                           String prefix, String shortLabel) {
        return new RibbonCommandPresentation(selected, ScholarIcons.forAction(selected.id()), RibbonCommandSize.MEDIUM,
                choices, null, shortLabel, Objects.requireNonNull(prefix, "prefix"));
    }

    public static RibbonCommandPresentation palette(String label, String shortLabel, ScholarIcon icon,
                                                     List<EditorAction> choices) {
        if (choices.isEmpty()) throw new IllegalArgumentException("A ribbon palette requires choices.");
        return new RibbonCommandPresentation(choices.getFirst(), icon, RibbonCommandSize.MEDIUM, choices,
                Objects.requireNonNull(label, "label"), Objects.requireNonNull(shortLabel, "shortLabel"), null);
    }

    public boolean dropdown() { return !choices.isEmpty(); }

    public boolean palette() { return labelOverride != null; }

    public String label(EditorAction presentedAction, RibbonLabelMode mode) {
        if (mode == RibbonLabelMode.ICON_ONLY) return "";
        if (mode == RibbonLabelMode.SHORT && shortLabel != null) return ScholarTranslations.get(shortLabel);
        if (labelOverride != null) return ScholarTranslations.get(labelOverride);
        return valuePrefix == null ? ScholarTranslations.actionLabel(presentedAction)
                : ScholarTranslations.get("scholar.ribbon.value", ScholarTranslations.get(valuePrefix),
                ScholarTranslations.actionLabel(presentedAction));
    }

    public String tooltipText() {
        return ScholarTranslations.actionLabel(action) + "\n" + ScholarTranslations.actionTooltip(action)
                + action.shortcut().map(shortcut -> "\n" + shortcut.displayText()).orElse("");
    }
}
