package dev.rgcb.scholar.typography;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonParser;

import java.awt.Font;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class TypographyResourceTest {
    private static final Path FONT_ROOT = Path.of("src/main/resources/assets/scholar/font");
    private static final Path LICENSE_ROOT = Path.of("docs/licenses/fonts");
    private static final String SCIENTIFIC_GLYPH_TEST_SET =
            "αβγδεζηθικλμνξοπρστυφχψω"
                    + "ΑΒΓΔΕΖΗΘΙΚΛΜΝΞΟΠΡΣΤΥΦΧΨΩ"
                    + "∞∂∇±×÷≤≥≠≈≡√∝∑∏∫"
                    + "→←↔↑↓";

    @Test
    void documentFontResourcesExist() {
        assertTrue(Files.isRegularFile(FONT_ROOT.resolve("document_regular.json")));
        assertTrue(Files.isRegularFile(FONT_ROOT.resolve("document_bold.json")));
        assertTrue(Files.isRegularFile(FONT_ROOT.resolve("document_italic.json")));
        assertTrue(Files.isRegularFile(FONT_ROOT.resolve("document_bold_italic.json")));
        assertTrue(Files.isRegularFile(FONT_ROOT.resolve("source_sans_3/regular.ttf")));
        assertTrue(Files.isRegularFile(FONT_ROOT.resolve("source_sans_3/bold.ttf")));
        assertTrue(Files.isRegularFile(FONT_ROOT.resolve("source_sans_3/italic.ttf")));
        assertTrue(Files.isRegularFile(FONT_ROOT.resolve("source_sans_3/bold_italic.ttf")));
    }

    @Test
    void experimentalMathFontResourcesExist() {
        assertTrue(Files.isRegularFile(FONT_ROOT.resolve("math.json")));
        assertTrue(Files.isRegularFile(FONT_ROOT.resolve("noto_sans_math/regular.ttf")));
    }

    @Test
    void highResolutionDocumentFontsMatchTheExistingFamilies() throws Exception {
        for (var name : List.of("document_regular", "document_bold", "document_italic", "document_bold_italic", "math")) {
            var base = JsonParser.parseString(Files.readString(FONT_ROOT.resolve(name + ".json"))).getAsJsonObject();
            var hi = JsonParser.parseString(Files.readString(FONT_ROOT.resolve(name + "_hi.json"))).getAsJsonObject();
            var baseProviders = base.getAsJsonArray("providers");
            var hiProviders = hi.getAsJsonArray("providers");
            assertEquals(baseProviders.size(), hiProviders.size(), name);
            for (var i = 0; i < baseProviders.size(); i++) {
                var source = baseProviders.get(i).getAsJsonObject();
                var enlarged = hiProviders.get(i).getAsJsonObject();
                assertEquals(source.get("type"), enlarged.get("type"), name);
                if (source.has("file")) {
                    assertEquals(source.get("file"), enlarged.get("file"), name);
                    assertEquals(source.get("size").getAsDouble() * 2, enlarged.get("size").getAsDouble(), name);
                }
            }
        }
    }

    @Test
    void distributedFontLicensesArePreserved() throws Exception {
        var notoLicense = Files.readString(LICENSE_ROOT.resolve("OFL-NotoSansMath.txt"));
        var sourceLicense = Files.readString(LICENSE_ROOT.resolve("OFL-SourceSans3.txt"));

        assertTrue(notoLicense.contains("SIL OPEN FONT LICENSE Version 1.1"));
        assertTrue(notoLicense.contains("The Noto Project Authors"));
        assertTrue(sourceLicense.contains("SIL OPEN FONT LICENSE Version 1.1"));
        assertTrue(sourceLicense.contains("Adobe"));
    }

    @Test
    void documentProfileCoversScientificGlyphSetWithControlledFallback() throws Exception {
        var source = loadFont("source_sans_3/regular.ttf");
        var fallback = loadFont("noto_sans_math/regular.ttf");

        assertTrue(missingGlyphs(SCIENTIFIC_GLYPH_TEST_SET, List.of(source, fallback)).isEmpty());
    }

    @Test
    void mathProfileCoversScientificGlyphSet() throws Exception {
        var math = loadFont("noto_sans_math/regular.ttf");

        assertTrue(missingGlyphs(SCIENTIFIC_GLYPH_TEST_SET, List.of(math)).isEmpty());
    }

    private static Font loadFont(String path) throws Exception {
        return Font.createFont(Font.TRUETYPE_FONT, FONT_ROOT.resolve(path).toFile());
    }

    private static List<String> missingGlyphs(String content, List<Font> fonts) {
        var missing = new ArrayList<String>();
        content.codePoints().forEach(codePoint -> {
            if (fonts.stream().noneMatch(font -> font.canDisplay(codePoint))) {
                missing.add("U+" + Integer.toHexString(codePoint).toUpperCase());
            }
        });
        return missing;
    }
}
