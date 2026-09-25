package dev.rgcb.scholar.client.ui;

import dev.rgcb.scholar.editor.EditorAction;
import net.minecraft.network.chat.Component;

import java.util.Objects;

/** Translation boundary for Scholar's Minecraft UI chrome. */
public final class ScholarText {
    private ScholarText() { }

    public static Component component(String key, Object... arguments) {
        return Component.translatable(requireKey(key), arguments);
    }

    public static String get(String key, Object... arguments) {
        return ScholarTranslations.get(key, arguments);
    }

    public static Component actionLabel(EditorAction action) {
        return component(Objects.requireNonNull(action, "action").translationKey());
    }

    public static String actionLabelText(EditorAction action) {
        return ScholarTranslations.actionLabel(action);
    }

    public static Component actionTooltip(EditorAction action) {
        Objects.requireNonNull(action, "action");
        return action.tooltipTranslationKey().map(ScholarText::component).orElseGet(() -> actionLabel(action));
    }

    public static String actionTooltipText(EditorAction action) {
        return ScholarTranslations.actionTooltip(action);
    }

    private static String requireKey(String key) {
        Objects.requireNonNull(key, "key");
        if (!key.startsWith("scholar.") || key.isBlank()) {
            throw new IllegalArgumentException("Invalid Scholar translation key: " + key);
        }
        return key;
    }
}
