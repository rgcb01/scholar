package dev.rgcb.scholar.editor;

import java.util.Objects;
import java.util.Optional;

/** Immutable presentation/discovery metadata for one built-in action. */
public record EditorActionDescriptor(
        EditorActionId id,
        String translationKey,
        Optional<String> tooltipKey,
        String englishLabel,
        String englishTooltip,
        EditorActionIcon icon,
        EditorActionCategory category,
        Optional<ActionShortcut> shortcut
) {
    public EditorActionDescriptor {
        id = Objects.requireNonNull(id, "id");
        translationKey = requireText(translationKey, "translationKey");
        tooltipKey = Objects.requireNonNull(tooltipKey, "tooltipKey");
        tooltipKey.ifPresent(value -> requireText(value, "tooltipKey"));
        englishLabel = requireText(englishLabel, "englishLabel");
        englishTooltip = requireText(englishTooltip, "englishTooltip");
        icon = Objects.requireNonNull(icon, "icon");
        category = Objects.requireNonNull(category, "category");
        shortcut = Objects.requireNonNull(shortcut, "shortcut");
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }
}
