package dev.rgcb.scholar.editor;

import dev.rgcb.scholar.client.DevelopmentStressDocument;
import dev.rgcb.scholar.client.DevelopmentStressDocument.Profile;
import dev.rgcb.scholar.document.*;
import dev.rgcb.scholar.data.*;
import dev.rgcb.scholar.persistence.*;
import dev.rgcb.scholar.validation.DocumentValidator;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;

class FoundationV2HardeningTest {
    @TempDir Path directory;

    @ParameterizedTest
    @EnumSource(value = Profile.class, names = {"LARGE", "MIXED_STRESS"})
    void largeFileSaveLoadIsLosslessAndCanonical(Profile profile) {
        var document = DevelopmentStressDocument.create(profile);
        var storage = new FileDocumentStorage(directory);
        var start = System.nanoTime();
        success(storage.save("stress", document));
        var saveMs = (System.nanoTime() - start) / 1_000_000.0;
        start = System.nanoTime();
        var loaded = success(storage.load("stress"));
        var loadMs = (System.nanoTime() - start) / 1_000_000.0;
        assertEquals(document, loaded);
        assertTrue(DocumentValidator.validate(loaded).isValid());
        var codec = new DocumentJsonCodec();
        assertEquals(success(codec.encode(document)), success(codec.encode(loaded)));
        System.out.printf("M27 file %s save_ms=%.3f load_ms=%.3f%n", profile, saveMs, loadMs);
    }

    @Test
    void largerMixedFragmentSharesClosureAndRepeatedPasteKeepsHistoryAtomic() {
        var source = DevelopmentStressDocument.create(Profile.MEDIUM);
        var indices = java.util.stream.IntStream.range(0, 100).boxed().toList();
        var start = System.nanoTime();
        var extracted = assertInstanceOf(dev.rgcb.scholar.transfer.ExtractionResult.Success.class,
                new dev.rgcb.scholar.transfer.FragmentExtractor().extract(source,
                        new dev.rgcb.scholar.transfer.FragmentExtractionRequest.Blocks(indices)));
        var extractionMs = (System.nanoTime() - start) / 1_000_000.0;
        var carrier = new dev.rgcb.scholar.clipboard.DocumentFragmentClipboardPayload(extracted.fragment(), extracted.sourceMetadata());
        var destination = new EditorSession(new Document(List.of(new Paragraph(new InlineContent(List.of())))), 0);
        var before = destination.current();
        start = System.nanoTime();
        assertTrue(destination.pasteFromClipboard(Optional.of(carrier), "fallback"));
        System.out.printf("M27 transfer roots=100 resources=%d extraction_ms=%.3f paste_ms=%.3f%n",
                extracted.fragment().resources().size(), extractionMs, (System.nanoTime() - start) / 1_000_000.0);
        assertEquals(extracted.fragment().resources().size(), destination.current().document().datasets().size());
        assertEquals(1, destination.undoDepth());
        var pasted = destination.current();
        valid(destination);
        assertTrue(destination.undo()); assertEquals(before, destination.current());
        assertTrue(destination.redo()); assertEquals(pasted, destination.current());
        var prose = find(destination.current().document(), b -> b instanceof Paragraph);
        destination.setCurrent(new EditorState(destination.current().document(), new DocumentPosition(prose, 0)));
        assertTrue(destination.pasteFromClipboard(Optional.of(carrier), "fallback"));
        assertEquals(extracted.fragment().resources().size() * 2, destination.current().document().datasets().size());
        assertEquals(2, destination.undoDepth());
        valid(destination);
    }

    @Test
    void hundredMixedTransactionsUndoAndRedoExactSnapshotsWithoutIdDrift() {
        var document = DevelopmentStressDocument.create(Profile.MEDIUM);
        var session = new EditorSession(document, find(document, b -> b instanceof Paragraph));
        var before = new ArrayList<EditorState>();
        for (var i = 0; i < 100; i++) {
            prepare(session, i % 10);
            before.add(session.current());
            assertTrue(edit(session, i % 10, i), "family " + i % 10 + " step " + i);
            assertEquals(i + 1, session.undoDepth());
            valid(session);
        }
        var finalState = session.current();
        var uniqueBlocks = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<BlockNode, Boolean>());
        before.forEach(state -> uniqueBlocks.addAll(state.document().blocks()));
        uniqueBlocks.addAll(finalState.document().blocks());
        System.out.printf("M27 history snapshots=%d block_slots=%d unique_block_values=%d capacity=%d%n",
                before.size() + 1, before.stream().mapToInt(s -> s.document().blocks().size()).sum()
                        + finalState.document().blocks().size(), uniqueBlocks.size(), EditorHistory.DEFAULT_CAPACITY);
        for (var i = 99; i >= 0; i--) {
            assertTrue(session.undo());
            assertEquals(before.get(i), session.current());
            valid(session);
        }
        assertFalse(session.canUndo());
        for (var i = 0; i < 100; i++) {
            assertTrue(session.redo());
            assertEquals(i == 99 ? finalState : before.get(i + 1), session.current());
            valid(session);
        }
        assertFalse(session.canRedo());
    }

    @Test
    void goldenPersistedMixedSessionEditsNestedContentTransfersCutsAndReloads() {
        var fixture = DevelopmentStressDocument.create(Profile.SMALL);
        var workspace = new DocumentWorkspace(fixture, new FileDocumentStorage(directory));
        success(workspace.saveAs("golden-v2"));
        success(workspace.open("golden-v2"));
        var session = workspace.session();
        var section = new DocumentStructureResolver().resolve(session.current().document()).sections().stream()
                .filter(s -> s.id().isPresent()).findFirst().orElseThrow();
        assertTrue(session.navigateToHeadingId(section.id().orElseThrow()));
        assertFalse(workspace.isDirty());
        var reference = new CrossReferenceResolver().targets(session.current().document()).getFirst();
        var resolved = new CrossReferenceResolver().resolve(session.current().document(), new CrossReference(reference.kind(), reference.targetId()));
        assertEquals(reference.blockIndex(), resolved.target().blockIndex());
        for (var i = 0; i < 10; i++) {
            prepare(session, i);
            assertTrue(edit(session, i, i), "family " + i);
            valid(session);
            assertTrue(workspace.isDirty());
        }
        var copiedFigure = find(session.current().document(), b -> b instanceof FigureBlock);
        session.setCurrent(new EditorState(session.current().document(), new BlockSelection(copiedFigure), Optional.empty()));
        var priorCut = session.current();
        var cut = session.cutForClipboard().orElseThrow();
        assertTrue(session.applyCut(cut));
        valid(session);
        assertTrue(session.undo());
        assertEquals(priorCut, session.current());
        assertReflow(session);
        var expected = session.current().document();
        success(workspace.save());
        assertFalse(workspace.isDirty());
        success(workspace.open("golden-v2"));
        assertEquals(expected, workspace.session().current().document());
        assertEquals(0, workspace.session().undoDepth());
        valid(workspace.session());
        prepare(workspace.session(), 0);
        var before = workspace.session().current();
        assertTrue(edit(workspace.session(), 0, 999));
        var after = workspace.session().current();
        assertTrue(workspace.session().undo());
        assertEquals(before, workspace.session().current());
        assertTrue(workspace.session().redo());
        assertEquals(after, workspace.session().current());
    }

    @Test
    void boundedFixedSeedMixedEditsValidateAfterEveryStepAndPeriodicRoundTrip() {
        var fixture = DevelopmentStressDocument.create(Profile.SMALL);
        var session = new EditorSession(fixture, find(fixture, b -> b instanceof Paragraph));
        var random = new Random(27026);
        var codec = new DocumentJsonCodec();
        for (var i = 0; i < 60; i++) {
            var family = random.nextInt(10);
            prepare(session, family);
            assertTrue(edit(session, family, i + 1000), "family " + family + " step " + i);
            valid(session);
            if (i % 5 == 0) {
                var after = session.current();
                assertTrue(session.undo()); valid(session);
                assertTrue(session.redo()); assertEquals(after, session.current());
                assertEquals(after.document(), success(codec.decode(success(codec.encode(after.document())))));
                assertReflow(session);
            }
        }
    }

    private static void prepare(EditorSession session, int family) {
        var document = session.current().document();
        var index = find(document, switch (family) {
            case 1 -> b -> b instanceof TableBlock t && t.datasetBinding().isEmpty();
            case 2 -> b -> b instanceof PlotBlock;
            case 3 -> b -> b instanceof DiagramBlock;
            case 4 -> b -> b instanceof EquationBlock;
            case 7 -> b -> b instanceof FigureBlock;
            case 8 -> b -> b instanceof Paragraph p && p.content().nodes().stream().anyMatch(n -> n instanceof Text t && !t.content().isEmpty());
            default -> b -> b instanceof Paragraph;
        });
        if (document.blocks().get(index) instanceof Paragraph) {
            session.setCurrent(new EditorState(document, new DocumentPosition(index, 0)));
        } else {
            session.setCurrent(new EditorState(document, new BlockSelection(index), Optional.empty()));
        }
        if (family == 8) {
            session.setCurrent(new EditorState(document, new DocumentPosition(index, 0), new DocumentPosition(index, 1)));
            return;
        }
        if (family == 0 || family == 1 || family == 4 || family == 5 || family == 8 || family == 9) {
            if (document.blocks().get(index) instanceof Paragraph) {
                session.setCurrent(new EditorState(document, new DocumentPosition(index, 0)));
            } else {
                session.enter();
            }
        } else if (family == 2) {
            session.enterPlotEditing(index, new PlotPropertyTarget(PlotProperty.TITLE));
        } else if (family == 3) {
            session.enterDiagramEditing(index, new DiagramPropertyTarget(DiagramProperty.TITLE));
        }
    }

    private static boolean edit(EditorSession session, int family, int step) {
        return switch (family) {
            case 0, 1 -> session.typeText("edit" + step);
            case 2 -> session.applyPlotText("Title " + step);
            case 3 -> session.applyDiagramText("Diagram " + step);
            case 4 -> session.typeText("x");
            case 5 -> session.enter();
            case 6 -> {
                var receiving = session.current();
                session.setCurrent(new EditorState(receiving.document(),
                        new BlockSelection(find(receiving.document(), b -> b instanceof FigureBlock)), Optional.empty()));
                var copy = session.copyForClipboard().orElseThrow();
                session.setCurrent(receiving);
                yield session.pasteFromClipboard(copy.payload(), copy.plainText());
            }
            case 7 -> session.setFigureCaptionText(session.current().blockSelection().blockIndex(), "Caption " + step);
            case 8 -> session.toggleMark(TextMark.BOLD);
            case 9 -> {
                var dataset = session.current().document().datasets().getFirst();
                var column = dataset.columns().stream().filter(c -> c.type() == DatasetColumnType.NUMBER).findFirst().orElseThrow();
                yield session.editDatasetCell(dataset.id(), 0, column.id(), DatasetValue.number(Integer.toString(step + 2000)));
            }
            default -> throw new IllegalArgumentException("Unknown family");
        };
    }

    private static int find(Document document, Predicate<BlockNode> predicate) {
        return java.util.stream.IntStream.range(0, document.blocks().size()).filter(i -> predicate.test(document.blocks().get(i))).findFirst().orElseThrow();
    }
    private static void valid(EditorSession session) {
        assertTrue(DocumentValidator.validate(session.current().document()).isValid());
        assertTrue(new EditorSelectionValidator().isValid(session.current()));
    }
    private static void assertReflow(EditorSession session) {
        var document = session.current().document();
        var text = new dev.rgcb.scholar.layout.TextMeasurer() {
            public int measureWidth(String value, dev.rgcb.scholar.layout.TextStyle style) { return TextBoundary.characterCount(value) * 6; }
            public int lineHeight(dev.rgcb.scholar.layout.TextStyle style) { return 10; }
        };
        dev.rgcb.scholar.math.layout.MathTextMeasurer math = (value, kind) -> new dev.rgcb.scholar.math.layout.MathTextMetrics(value.length() * 6, 8, 3);
        var engine = new dev.rgcb.scholar.layout.DocumentLayoutEngine();
        var wide = engine.layout(document, 480, text, math);
        engine.layout(document, 180, text, math);
        engine.layout(document, 24, text, math);
        assertEquals(wide, engine.layout(document, 480, text, math));
        assertSame(document, session.current().document());
        valid(session);
    }
    private static <T> T success(PersistenceResult<T> result) {
        assertInstanceOf(PersistenceResult.Success.class, result);
        return ((PersistenceResult.Success<T>) result).value();
    }
}
