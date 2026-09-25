package dev.rgcb.scholar.client.ui;

import com.google.gson.JsonParser;
import dev.rgcb.scholar.editor.EditorAction;

import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** String resolver usable by pure layout/model tests without loading Minecraft chat classes. */
public final class ScholarTranslations {
    private static final Map<String, String> ENGLISH = loadEnglish();
    private static final Method MINECRAFT_GET = findMinecraftGet();

    private ScholarTranslations() { }

    public static String get(String key, Object... arguments) {
        requireKey(key);
        if (MINECRAFT_GET != null) {
            try {
                return (String) MINECRAFT_GET.invoke(null, key, arguments);
            } catch (ReflectiveOperationException ignored) {
                // Pure tests and early bootstrap use the bundled English baseline.
            }
        }
        var template = ENGLISH.getOrDefault(key, key);
        return arguments.length == 0 ? template : String.format(Locale.ROOT, template, arguments);
    }

    public static String actionLabel(EditorAction action) {
        return get(Objects.requireNonNull(action, "action").translationKey());
    }

    public static String actionTooltip(EditorAction action) {
        Objects.requireNonNull(action, "action");
        return action.tooltipTranslationKey().map(key -> get(key)).orElseGet(() -> actionLabel(action));
    }

    private static Method findMinecraftGet() {
        try {
            return Class.forName("net.minecraft.client.resources.language.I18n")
                    .getMethod("get", String.class, Object[].class);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static Map<String, String> loadEnglish() {
        try (var stream = ScholarTranslations.class.getResourceAsStream("/assets/scholar/lang/en_us.json")) {
            if (stream == null) return Map.of();
            var object = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            var values = new java.util.HashMap<String, String>();
            object.entrySet().forEach(entry -> values.put(entry.getKey(), entry.getValue().getAsString()));
            return Map.copyOf(values);
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    private static void requireKey(String key) {
        Objects.requireNonNull(key, "key");
        if (!key.startsWith("scholar.") || key.isBlank()) {
            throw new IllegalArgumentException("Invalid Scholar translation key: " + key);
        }
    }
}
