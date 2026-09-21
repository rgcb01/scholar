package dev.rgcb.scholar.layout;

import dev.rgcb.scholar.client.DevelopmentStressDocument;
import dev.rgcb.scholar.client.DevelopmentStressDocument.Profile;
import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.DocumentStructureResolver;
import dev.rgcb.scholar.editor.DocumentHitTester;
import dev.rgcb.scholar.math.layout.MathTextMeasurer;
import dev.rgcb.scholar.math.layout.MathTextMetrics;
import dev.rgcb.scholar.persistence.DocumentJsonCodec;
import dev.rgcb.scholar.persistence.PersistenceResult;
import dev.rgcb.scholar.validation.DocumentValidator;
import java.util.Arrays;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;

class EditorStressMeasurementTest {
    static final TextMeasurer TEXT = new TextMeasurer() {
        public int measureWidth(String text, TextStyle style) {
            return text.codePointCount(0, text.length()) * 6;
        }
        public int lineHeight(TextStyle style) { return style.isHeading() ? 14 : 10; }
    };
    static final MathTextMeasurer MATH = (text, kind) -> new MathTextMetrics(text.length() * 6, 8, 3);

    @ParameterizedTest
    @EnumSource(Profile.class)
    void measuresDeterministicFixtureWithoutTimingAssertions(Profile profile) {
        var document = DevelopmentStressDocument.create(profile);
        assertTrue(DocumentValidator.validate(document).isValid());
        var engine = new DocumentLayoutEngine();
        var layout = engine.layout(document, 480, TEXT, MATH);
        var allRuns = layout.blocks().stream().flatMap(b -> b.lines().stream()).mapToInt(l -> l.textRuns().size()).sum();
        var visibleRuns = layout.blocks().stream().filter(b -> (long) b.y() + b.height() >= layout.height() / 2
                && b.y() <= layout.height() / 2 + 300).flatMap(b -> b.lines().stream()).mapToInt(l -> l.textRuns().size()).sum();
        System.out.printf("M27 render-preparation %s all_runs=%d visible_block_runs=%d old_ms=%.3f bounded_ms=%.3f%n",
                profile, allRuns, visibleRuns,
                median(() -> scanRuns(layout, false)), median(() -> scanRuns(layout, true)));
        assertEquals(document.blocks().size(), layout.blocks().size());
        var codec = new DocumentJsonCodec();
        var resolver = new dev.rgcb.scholar.document.CrossReferenceResolver();
        var referenceTarget = resolver.targets(document).getFirst();
        var reference = new dev.rgcb.scholar.document.CrossReference(referenceTarget.kind(), referenceTarget.targetId());
        var state = new dev.rgcb.scholar.editor.EditorState(document, new dev.rgcb.scholar.editor.DocumentPosition(
                java.util.stream.IntStream.range(0, document.blocks().size()).filter(i -> document.blocks().get(i) instanceof dev.rgcb.scholar.document.Paragraph).findFirst().orElseThrow(), 0));
        System.out.printf("M27 auxiliary %s reference_ms=%.3f selection_validation_ms=%.3f scroll_ms=%.6f%n", profile,
                median(() -> resolver.resolve(document, reference)),
                median(() -> new dev.rgcb.scholar.editor.EditorSelectionValidator().isValid(state)),
                median(() -> dev.rgcb.scholar.editor.ScrollGeometry.reveal(100, layout.height(), 300, 200, 10)));
        var encoded = assertInstanceOf(PersistenceResult.Success.class, codec.encode(document));
        var json = (String) encoded.value();
        var decoded = assertInstanceOf(PersistenceResult.Success.class, codec.decode(json));
        assertEquals(document, decoded.value());
        System.out.printf("M27 %s blocks=%d rows=%d chars=%d layout_ms=%.3f structure_ms=%.3f hit_ms=%.3f encode_ms=%.3f decode_ms=%.3f%n",
                profile, document.blocks().size(), document.datasets().stream().mapToInt(d -> d.rows().size()).sum(), json.length(),
                median(() -> engine.layout(document, 480, TEXT, MATH)),
                median(() -> new DocumentStructureResolver().resolve(document)),
                median(() -> new DocumentHitTester().hit(layout, 8, layout.height() / 2, TEXT)),
                median(() -> codec.encode(document)), median(() -> codec.decode(json)));
        if (profile == Profile.LARGE) {
            for (var target : new int[] {0, document.blocks().size() / 2, document.blocks().size() - 1}) {
                var index = java.util.stream.IntStream.range(target, document.blocks().size()).filter(i -> document.blocks().get(i) instanceof dev.rgcb.scholar.document.Paragraph)
                        .findFirst().orElseGet(() -> java.util.stream.IntStream.range(0, target).filter(i -> document.blocks().get(i) instanceof dev.rgcb.scholar.document.Paragraph).max().orElseThrow());
                var position = new dev.rgcb.scholar.editor.DocumentPosition(index, 0);
                System.out.printf("M27 local-edit block=%d relayout_ms=%.3f%n", index, median(() -> {
                    var changed = new dev.rgcb.scholar.editor.DocumentEditor().replaceRange(document,
                            new dev.rgcb.scholar.editor.DocumentRange(position, position), "x");
                    engine.layout(changed.document(), 480, TEXT, MATH);
                }));
            }
        }
    }

    private static double median(Runnable operation) {
        operation.run();
        var samples = new long[3];
        for (var i = 0; i < samples.length; i++) {
            var start = System.nanoTime();
            operation.run();
            samples[i] = System.nanoTime() - start;
        }
        Arrays.sort(samples);
        return samples[1] / 1_000_000.0;
    }

    private static volatile int scanResult;
    private static void scanRuns(LaidOutDocument layout, boolean cull) {
        var count = 0;
        for (var block : layout.blocks()) {
            if (cull && !block.intersectsVerticalViewport(layout.height() / 2, 300)) {
                continue;
            }
            for (var line : block.lines()) {
                for (var run : line.textRuns()) { count += run.width(); }
            }
        }
        scanResult = count;
    }
}
