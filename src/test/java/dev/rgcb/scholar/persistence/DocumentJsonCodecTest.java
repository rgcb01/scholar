package dev.rgcb.scholar.persistence;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.rgcb.scholar.client.DevelopmentDocument;
import dev.rgcb.scholar.data.*;
import dev.rgcb.scholar.diagram.*;
import dev.rgcb.scholar.document.*;
import dev.rgcb.scholar.math.*;
import dev.rgcb.scholar.validation.DocumentValidator;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class DocumentJsonCodecTest {
    final DocumentJsonCodec codec = new DocumentJsonCodec();
    static Paragraph paragraph(String value) { return new Paragraph(new InlineContent(List.of(new Text(value, Set.of())))); }
    static <T> T success(PersistenceResult<T> result) {
        assertInstanceOf(PersistenceResult.Success.class, result, () -> result.diagnostics().toString());
        return ((PersistenceResult.Success<T>) result).value();
    }

    @Test void goldenCompleteDocumentAndCanonicalEncoding() {
        var source = DevelopmentDocument.createPersistenceFixture();
        assertTrue(DocumentValidator.validate(source).isValid());
        var encoded = success(codec.encode(source));
        var loaded = success(codec.decode(encoded));
        assertEquals(source, loaded);
        assertTrue(DocumentValidator.validate(loaded).isValid());
        assertEquals(encoded, success(codec.encode(loaded)));
        assertEquals(encoded, success(codec.encode(source)));
        var root = JsonParser.parseString(encoded).getAsJsonObject();
        assertEquals(Set.of("format", "version", "settings", "blocks", "datasets"), root.keySet());
        assertFalse(encoded.contains("RuntimeDocumentToken"));
        assertFalse(encoded.contains("LaidOut"));
        assertFalse(encoded.contains("dev.rgcb"));
    }

    @Test void independentlyAuthoredV1SchemaDoesNotDependOnRuntimeClassNames() throws Exception {
        String text;
        try (var stream = getClass().getResourceAsStream("/persistence/v1-minimal.scholar.json")) {
            assertNotNull(stream); text = new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
        var expected = new Document(List.of(
                new Heading("intro", 1, new InlineContent(List.of(new Text("Scholar", Set.of(TextMark.BOLD))))),
                new Paragraph(new InlineContent(List.of(new Text("See ", Set.of()), new CrossReference(CrossReferenceTargetKind.SECTION, "intro")))),
                new EquationBlock(new MathFraction(new MathNumber("1"), new MathIdentifier("x"))), new TableOfContentsBlock()));
        assertEquals(expected, success(codec.decode(text)));
        var migrated = success(codec.encode(expected));
        assertEquals(2, JsonParser.parseString(migrated).getAsJsonObject().get("version").getAsInt());
        assertEquals(expected, success(codec.decode(migrated)));
    }

    @Test void sharedDatasetBindingsAndReferenceResolutionSurviveExactly() {
        var loaded = success(codec.decode(success(codec.encode(DevelopmentDocument.createPersistenceFixture()))));
        assertEquals(1, loaded.datasets().stream().filter(d -> d.id().equals("projectile-test")).count());
        var table = loaded.blocks().stream().filter(TableBlock.class::isInstance).map(TableBlock.class::cast).filter(t -> t.id().orElse("").equals("m25-bound-table")).findFirst().orElseThrow();
        var figure = loaded.blocks().stream().filter(FigureBlock.class::isInstance).map(FigureBlock.class::cast).filter(f -> f.id().equals("m25-transfer-figure")).findFirst().orElseThrow();
        var plot = (PlotBlock) figure.content();
        assertEquals("projectile-test", table.datasetBinding().orElseThrow().datasetId());
        assertTrue(plot.definition().series().stream().allMatch(s -> s.datasetBinding().orElseThrow().datasetId().equals("projectile-test")));
        assertFalse(new DatasetTableResolver().resolve(loaded, table).rows().isEmpty());
        var resolver = new CrossReferenceResolver();
        assertTrue(resolver.resolve(loaded, new CrossReference(CrossReferenceTargetKind.SECTION, "m26-persistence")).resolved());
        assertTrue(resolver.resolve(loaded, new CrossReference(CrossReferenceTargetKind.FIGURE, "m25-transfer-figure")).resolved());
        assertEquals(CrossReferenceResolution.MISSING_REFERENCE_TEXT, resolver.resolve(loaded, new CrossReference(CrossReferenceTargetKind.FIGURE, "missing")).displayText());
    }

    @Test void diagramsKeepLocalIdentityEndpointsConstraintsAndAuthoredGeometry() {
        var source = DevelopmentDocument.createPersistenceFixture();
        var loaded = success(codec.decode(success(codec.encode(source))));
        for (int i = 0; i < source.blocks().size(); i++) {
            var block = source.blocks().get(i);
            if (block instanceof DiagramBlock || block instanceof FigureBlock f && f.content() instanceof DiagramBlock) {
                assertEquals(block, loaded.blocks().get(i));
            }
        }
        assertTrue(new dev.rgcb.scholar.electrical.ElectricalNetResolver()
                .resolve(((DiagramBlock) ((FigureBlock) loaded.blocks().stream().filter(b -> b instanceof FigureBlock f && f.id().equals("m25-diagram-figure")).findFirst().orElseThrow()).content()).definition()).nets().size() > 0);
    }

    static Stream<MathExpression> mathNodes() {
        var x = new MathIdentifier("namedVariable");
        return Stream.of(x, new MathNumber("12.50"), new MathNamedOperator("sin"), new MathText("if x = 0"),
                new MathSymbol("\u03bb", MathSymbolKind.GREEK), new MathOperator("/", MathOperatorRole.BINARY),
                new MathSequence(List.of()), new MathFraction(x, new MathSequence(List.of())),
                new MathRoot(x, Optional.empty()), new MathRoot(x, Optional.of(new MathNumber("3"))),
                new MathScript(x, Optional.of(x), Optional.empty()), new MathScript(x, Optional.empty(), Optional.of(x)),
                new MathScript(x, Optional.of(x), Optional.of(x)),
                new MathGroup(x, MathDelimiter.PARENTHESES), new MathGroup(x, MathDelimiter.BRACKETS), new MathGroup(x, MathDelimiter.BRACES));
    }
    @ParameterizedTest @MethodSource("mathNodes") void allMathNodesPreserveAuthoredStructure(MathExpression expression) {
        var source = new Document(List.of(paragraph(""), new EquationBlock("equation", expression)));
        assertEquals(source, success(codec.decode(success(codec.encode(source)))));
    }

    @Test void textSegmentationEmptyNodesAllMarksUnicodeAndDecimalScaleAreExact() {
        var inline = new InlineContent(List.of(new Text("", Set.of()), new Text("\u03bb\ud83d\ude80", Set.of(TextMark.BOLD)),
                new Text("abc", Set.of(TextMark.ITALIC)), new Text("both", Set.of(TextMark.BOLD, TextMark.ITALIC))));
        var dataset = new ScientificDataset("d", Optional.empty(), List.of(new DatasetColumn("n", "Number", DatasetColumnType.NUMBER), new DatasetColumn("t", "Text", DatasetColumnType.TEXT)),
                List.of(new DatasetRow("r", List.of(DatasetValue.number("1.2300"), DatasetValue.text(""))),
                        new DatasetRow(List.of(DatasetValue.missing(), DatasetValue.text("\u03a9")))));
        var doc = new Document(List.of(new Paragraph(inline)), List.of(dataset));
        assertEquals(doc, success(codec.decode(success(codec.encode(doc)))));
    }

    @Test void warningsAreNotRepairAndDerivedStructuresRecompute() {
        var doc = new Document(List.of(new Heading("s", 1, new InlineContent(List.of())), new TableOfContentsBlock(),
                new Paragraph(new InlineContent(List.of(new CrossReference(CrossReferenceTargetKind.FIGURE, "absent")))), new TableBlock(new DatasetTableBinding("missing-dataset"))));
        var encoded = codec.encode(doc);
        assertInstanceOf(PersistenceResult.Success.class, encoded);
        assertFalse(encoded.diagnostics().isEmpty());
        var decoded = codec.decode(success(encoded));
        assertEquals(doc, success(decoded));
        assertFalse(decoded.diagnostics().isEmpty());
        assertEquals(1, new DocumentStructureResolver().resolve(success(decoded)).sections().size());
        assertFalse(success(encoded).contains("Figure 1"));
    }

    @Test void fixedSeedMixedRoundTripsAndCanonicalReplay() {
        var random = new Random(26026);
        var fixture = DevelopmentDocument.createPersistenceFixture();
        for (int scenario = 0; scenario < 75; scenario++) {
            var blocks = new ArrayList<BlockNode>(); blocks.add(paragraph("scenario " + scenario));
            for (var block : fixture.blocks()) if (random.nextBoolean()) blocks.add(block);
            var source = new Document(blocks, fixture.datasets());
            var encoded = success(codec.encode(source));
            var loaded = success(codec.decode(encoded));
            assertEquals(source, loaded, "scenario " + scenario);
            assertTrue(DocumentValidator.validate(loaded).isValid());
            assertEquals(encoded, success(codec.encode(loaded)));
        }
    }

    static Stream<String> malformed() {
        var codec = new DocumentJsonCodec();
        var minimal = success(codec.encode(new Document(List.of(paragraph("")))));
        var cases = new ArrayList<String>(List.of("", "{", "[]", "null", "{}", minimal + " true", minimal.replace("scholar-document", "foreign"),
                minimal.replace("\"version\": 2", "\"version\": 999"), minimal.replace("\"version\": 2,", ""),
                minimal.replace("\"version\": 2", "\"version\": 1.5"), minimal.replace("\"version\": 2", "\"version\": \"2\""),
                minimal.replace("\"paragraph\"", "\"future-block\""), minimal.replace("\"text\"", "\"future-inline\""),
                minimal.replace("\"version\": 2", "\"version\": 2, \"version\": 2"), minimal.replace("\"version\": 2", "\"version\": 2, \"surprise\": true")));
        var golden = success(codec.encode(DevelopmentDocument.createPersistenceFixture()));
        cases.add(golden.replaceFirst("\"sequence\"", "\"future-math\""));
        cases.add(golden.replaceFirst("\"number\"", "\"future-dataset-kind\""));
        for (String category : List.of("duplicate-section", "duplicate-dataset", "missing-binding", "invalid-table", "invalid-endpoint", "invalid-figure", "invalid-reference",
                "unknown-dataset-value", "unknown-diagram-element", "invalid-reference-kind", "invalid-mark", "null-expression", "invalid-heading-level", "null-blocks")) {
            var o = JsonParser.parseString(golden).getAsJsonObject();
            var blocks = o.getAsJsonArray("blocks");
            switch (category) {
                case "duplicate-section" -> blocks.add(blocks.get(0).deepCopy());
                case "duplicate-dataset" -> o.getAsJsonArray("datasets").add(o.getAsJsonArray("datasets").get(0).deepCopy());
                case "missing-binding" -> { var t = find(blocks, "table", true); t.getAsJsonObject("binding").remove("datasetId"); }
                case "invalid-table" -> find(blocks, "table", false).getAsJsonArray("rows").add(new com.google.gson.JsonArray());
                case "invalid-endpoint" -> find(blocks, "diagram", false).getAsJsonObject("definition").getAsJsonArray("connections").get(0).getAsJsonObject().getAsJsonObject("source").addProperty("elementId", "missing");
                case "invalid-figure" -> find(blocks, "figure", false).add("content", blocks.get(1).deepCopy());
                case "invalid-reference" -> find(blocks, "figure", false).getAsJsonArray("caption").get(1).getAsJsonObject().remove("targetId");
                case "unknown-dataset-value" -> o.getAsJsonArray("datasets").get(0).getAsJsonObject().getAsJsonArray("rows").get(0).getAsJsonObject().getAsJsonArray("values").get(0).getAsJsonObject().addProperty("type", "future-value");
                case "unknown-diagram-element" -> find(blocks, "diagram", false).getAsJsonObject("definition").getAsJsonArray("elements").get(0).getAsJsonObject().addProperty("type", "future-element");
                case "invalid-reference-kind" -> find(blocks, "figure", false).getAsJsonArray("caption").get(1).getAsJsonObject().addProperty("kind", "future-target");
                case "invalid-mark" -> find(blocks, "heading", false).getAsJsonArray("content").get(0).getAsJsonObject().getAsJsonArray("marks").add("unknown");
                case "null-expression" -> find(blocks, "equation", false).add("expression", com.google.gson.JsonNull.INSTANCE);
                case "invalid-heading-level" -> find(blocks, "heading", false).addProperty("level", 7);
                case "null-blocks" -> o.add("blocks", com.google.gson.JsonNull.INSTANCE);
                default -> throw new AssertionError();
            }
            cases.add(o.toString());
        }
        return cases.stream();
    }
    static JsonObject find(com.google.gson.JsonArray blocks, String type, boolean bound) {
        return java.util.stream.StreamSupport.stream(blocks.spliterator(), false).map(e -> e.getAsJsonObject())
                .filter(o -> o.get("type").getAsString().equals(type)).filter(o -> !bound || o.has("binding") && !o.get("binding").isJsonNull()).findFirst().orElseThrow();
    }
    @ParameterizedTest @MethodSource("malformed") void malformedCorpusFailsWithoutPartialDocument(String text) {
        var result = assertDoesNotThrow(() -> codec.decode(text));
        assertInstanceOf(PersistenceResult.Failure.class, result);
        assertFalse(result.diagnostics().isEmpty());
    }

    @Test void boundedNestingAndSizePreventUnboundedInput() {
        assertInstanceOf(PersistenceResult.Failure.class, codec.decode("[".repeat(140) + "0" + "]".repeat(140)));
        assertInstanceOf(PersistenceResult.Failure.class, codec.decode(" ".repeat(DocumentJsonCodec.MAX_CHARACTERS + 1)));
    }

    @Test void versionDispatchDoesNotInterpretFutureBodyAsV1() {
        var result = codec.decode("{\"format\":\"scholar-document\",\"version\":999,\"future-schema\":true}");
        assertInstanceOf(PersistenceResult.Failure.class, result);
        assertEquals(PersistenceDiagnostic.Code.UNSUPPORTED_VERSION, result.diagnostics().getFirst().code());
    }

    @Test void signedZeroCoordinatesAreNotNormalizedAndExtremeNumbersFailSafely() {
        var plot = new PlotBlock(dev.rgcb.scholar.plot.PlotDefinition.of("zero", dev.rgcb.scholar.plot.AxisDefinition.linear("x"), dev.rgcb.scholar.plot.AxisDefinition.linear("y"),
                List.of(new dev.rgcb.scholar.plot.PlotSeries("s", dev.rgcb.scholar.plot.PlotSeriesKind.SCATTER,
                        List.of(new dev.rgcb.scholar.plot.DataPoint(-0.0, Double.MIN_VALUE))))));
        var source = new Document(List.of(paragraph(""), plot));
        var encoded = success(codec.encode(source));
        assertEquals(source, success(codec.decode(encoded)));
        assertEquals(encoded, success(codec.encode(success(codec.decode(encoded)))));
        assertInstanceOf(PersistenceResult.Failure.class, codec.decode(encoded.replace("-0.0", "1e2147483647")));
    }

    @Test void decoderDoesNotSilentlyTrimStableIdentity() {
        var heading = new Heading("h", 1, paragraph("").content());
        var encoded = success(codec.encode(new Document(List.of(heading))));
        assertInstanceOf(PersistenceResult.Failure.class, codec.decode(encoded.replace("\"h\"", "\" h \"")));
    }

    @Test void invalidSaveRefusedAndUnknownAuthorNodesNeverReflectSerialized() {
        var heading = new Heading("same", 1, new InlineContent(List.of()));
        assertInstanceOf(PersistenceResult.Failure.class, codec.encode(new Document(List.of(heading, heading))));
        assertInstanceOf(PersistenceResult.Failure.class, codec.encode(new Document(List.of(new BlockNode() { }))));
        assertInstanceOf(PersistenceResult.Failure.class, codec.encode(new Document(List.of(new EquationBlock(new MathExpression() { })))));
    }
}
