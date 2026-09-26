package dev.rgcb.scholar.client.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import dev.rgcb.scholar.editor.BuiltInEditorActionCatalog;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class ScholarLocalizationCompletenessTest {
    private static final Path LANG = Path.of("src/main/resources/assets/scholar/lang");
    private static final Path CLIENT = Path.of("src/main/java/dev/rgcb/scholar/client");
    private static final Pattern ENTRY = Pattern.compile("(?m)^\\s*\"([^\"]+)\"\\s*:");
    private static final Pattern SOURCE_KEY = Pattern.compile(
            "\"(scholar\\.(?:action|menu|dialog|tooltip|status|home|template|dataset|diagram|figure|table|document|error|confirm|export|import|ribbon)\\.[a-z0-9_.]+)\"");
    private static final Pattern VALID_KEY = Pattern.compile("scholar\\.[a-z0-9_]+(?:\\.[a-z0-9_]+)+");

    @Test void englishAndMexicanSpanishResourcesAreCompleteAndWellFormed() throws Exception {
        var english = read(LANG.resolve("en_us.json"));
        var spanish = read(LANG.resolve("es_mx.json"));

        assertEquals(english.keySet(), spanish.keySet(), "en_us and es_mx must contain the same Scholar keys");
        assertFalse(english.isEmpty());
        english.forEach((key, value) -> {
            assertTrue(VALID_KEY.matcher(key).matches(), "Malformed translation key: " + key);
            assertFalse(value.isBlank(), "Blank en_us translation: " + key);
            assertFalse(spanish.get(key).isBlank(), "Blank es_mx translation: " + key);
        });
    }

    @Test void everyActionDescriptorReferencesExistingTranslations() throws Exception {
        var english = read(LANG.resolve("en_us.json"));
        for (var descriptor : BuiltInEditorActionCatalog.descriptors()) {
            assertTrue(english.containsKey(descriptor.translationKey()), descriptor.id() + " label key");
            descriptor.tooltipKey().ifPresent(key -> assertTrue(english.containsKey(key), descriptor.id() + " tooltip key"));
        }
    }

    @Test void everyStaticProductionClientTranslationKeyExists() throws Exception {
        var english = read(LANG.resolve("en_us.json"));
        try (var files = Files.walk(CLIENT)) {
            for (var source : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                var matcher = SOURCE_KEY.matcher(Files.readString(source));
                while (matcher.find()) {
                    var key = matcher.group(1);
                    if (!key.endsWith(".")) assertTrue(english.containsKey(key), source + " uses missing key " + key);
                }
            }
        }
    }

    private static Map<String, String> read(Path path) throws Exception {
        var source = Files.readString(path);
        var names = new HashSet<String>();
        var matcher = ENTRY.matcher(source);
        while (matcher.find()) assertTrue(names.add(matcher.group(1)), "Duplicate translation key in " + path + ": " + matcher.group(1));
        var parsed = JsonParser.parseString(source);
        assertTrue(parsed.isJsonObject(), path + " must contain one JSON object");
        var result = new java.util.LinkedHashMap<String, String>();
        for (var entry : parsed.getAsJsonObject().entrySet()) {
            assertTrue(entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isString(),
                    path + " value must be a string: " + entry.getKey());
            result.put(entry.getKey(), entry.getValue().getAsString());
        }
        assertEquals(Set.copyOf(names), result.keySet(), path + " parser/key scan disagree");
        return Map.copyOf(result);
    }
}
