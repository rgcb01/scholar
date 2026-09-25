package dev.rgcb.scholar.client.ui;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** Guards UI chrome only; user/scientific values, IDs, filenames, symbols and diagnostics remain valid literals. */
class UiLiteralHardcodeBoundaryTest {
    private static final Path CLIENT = Path.of("src/main/java/dev/rgcb/scholar/client");
    private static final Pattern COMPONENT_LITERAL = Pattern.compile("Component\\.literal\\(\"([^\"]*[A-Za-z][^\"]*)\"\\)");
    private static final Pattern DRAW_LITERAL = Pattern.compile("draw(?:Centered)?String\\(font,\\s*\"([^\"]*[A-Za-z][^\"]*)\"");
    private static final Set<String> ALLOWED_CHROME_LITERALS = Set.of("Scholar", "X", "Y", "x", "v");

    @Test void productionUiDoesNotReintroduceObviousEnglishChromeLiterals() throws Exception {
        try (var files = Files.walk(CLIENT)) {
            for (var source : files.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> path.toString().contains("screen") || path.toString().contains("ui")).toList()) {
                var text = Files.readString(source);
                assertAllowed(source, text, COMPONENT_LITERAL);
                assertAllowed(source, text, DRAW_LITERAL);
            }
        }
    }

    private static void assertAllowed(Path source, String text, Pattern pattern) {
        var matcher = pattern.matcher(text);
        while (matcher.find()) {
            var literal = matcher.group(1);
            assertTrue(ALLOWED_CHROME_LITERALS.contains(literal),
                    source + " contains non-localized UI literal: " + literal);
        }
    }
}
