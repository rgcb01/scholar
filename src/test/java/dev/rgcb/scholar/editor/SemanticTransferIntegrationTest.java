package dev.rgcb.scholar.editor;

import static org.junit.jupiter.api.Assertions.*;
import static dev.rgcb.scholar.editor.EditorFoundationFixture.*;
import dev.rgcb.scholar.clipboard.*;
import dev.rgcb.scholar.data.*;
import dev.rgcb.scholar.document.*;
import dev.rgcb.scholar.math.clipboard.MathClipboardPayload;
import dev.rgcb.scholar.math.MathIdentifier;
import dev.rgcb.scholar.math.MathSequence;
import dev.rgcb.scholar.transfer.*;
import dev.rgcb.scholar.validation.DocumentValidator;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class SemanticTransferIntegrationTest {
    enum Family { HEADING, EQUATION, TABLE, BOUND_TABLE, PLOT, BOUND_PLOT, DIAGRAM, ELECTRICAL, MECHANICAL, FIGURE_PLOT, FIGURE_DIAGRAM, TOC }

    @Test void layoutSectionBreakCopiesCutsPastesAndRestoresAsOneAtomicTransaction() {
        var marker = new LayoutSectionBreak(ColumnLayout.two());
        var source = new EditorSession(new Document(List.of(paragraph("before"), marker, paragraph("after"))), 0);
        source.setCurrent(new EditorState(source.current().document(), new BlockSelection(1), Optional.empty()));
        var copy = source.copyForClipboard().orElseThrow();
        assertEquals("", copy.plainText());

        var destination = new EditorSession(new Document(List.of(emptyParagraph())), 0);
        assertTrue(destination.pasteFromClipboard(copy.payload(), copy.plainText()));
        assertEquals(marker, destination.current().document().blocks().getFirst());
        assertEquals(new BlockSelection(0), destination.current().selection());
        var pasted = destination.current();
        assertTrue(destination.undo());
        assertTrue(destination.redo());
        assertEquals(pasted, destination.current());

        var cut = source.cutForClipboard().orElseThrow();
        assertTrue(source.applyCut(cut));
        assertFalse(source.current().document().blocks().contains(marker));
        assertTrue(source.undo());
        assertEquals(marker, source.current().document().blocks().get(1));
    }

    @ParameterizedTest @EnumSource(Family.class)
    void realCopyPasteTransfersWholeRootAndUndoRedoRestoresExactState(Family family) {
        var root = root(family);
        var source = new EditorSession(new Document(List.of(paragraph("Before"), root, paragraph("After")), List.of(projectileDataset())), 0);
        source.setCurrent(new EditorState(source.current().document(), new BlockSelection(1), Optional.empty()));
        var sourceState = source.current();
        var clipboard = new MemoryClipboard();
        var sidecar = new ScholarClipboardService();
        BuiltInEditorActions.copy().execute(new EditorActionContext(source, clipboard, sidecar));
        assertSame(sourceState, source.current());
        assertEquals(0, source.undoDepth());
        assertInstanceOf(DocumentFragmentClipboardPayload.class, sidecar.snapshot().orElseThrow().payload());
        var destination = new EditorSession(new Document(List.of(emptyParagraph())), 0);
        var before = destination.current();
        var action = new EditorActionContext(destination, clipboard, sidecar);
        assertTrue(BuiltInEditorActions.paste().execute(action).documentChanged());
        var pasted = destination.current();
        assertEquals(root, pasted.document().blocks().get(0));
        assertEquals(new BlockSelection(0), pasted.selection());
        assertEquals(1, destination.undoDepth());
        valid(destination);
        assertTrue(destination.undo());
        assertEquals(before, destination.current());
        assertTrue(destination.redo());
        assertEquals(pasted, destination.current());
        valid(destination);
    }

    @Test void inlineCopyPreservesExactMarksSegmentationUnicodeAndResolvedExternalExport() {
        var nodes = List.<InlineNode>of(text("plain"), text("bold", TextMark.BOLD), text("italic", TextMark.ITALIC),
                text("both", TextMark.BOLD, TextMark.ITALIC), text("café λ 😀"), ref(CrossReferenceTargetKind.FIGURE, "f"));
        var source = new EditorSession(new Document(List.of(new Paragraph(new InlineContent(nodes)), new FigureBlock("f", plot("F"), inline("caption")))), 0);
        select(source, new DocumentPosition(0, 0), source.current().caret());
        var copy = source.copyForClipboard().orElseThrow();
        assertTrue(copy.plainText().endsWith("Figure 1"));
        var destination = new EditorSession(new Document(List.of(emptyParagraph())), 0);
        assertTrue(destination.pasteFromClipboard(copy.payload(), copy.plainText()));
        var pasted = ((Paragraph) destination.current().document().blocks().get(0)).content().nodes();
        assertEquals(nodes.subList(0, 5), pasted.subList(0, 5));
        assertEquals(text("Figure 1"), pasted.get(5));
        assertTrue(destination.transferDiagnostics().stream().anyMatch(d -> d.code() == TransferDiagnostic.Code.EXTERNAL_REFERENCE_DEGRADED));
        valid(destination);
    }

    @Test void inlineHeadingSelectionDoesNotImportHeadingStyleOrIdentity() {
        var source = new EditorSession(new Document(List.of(heading("h", 3, "Title"))), 0);
        select(source, new DocumentPosition(0, 0), new DocumentPosition(0, 5));
        var inlineCopy = source.copyForClipboard().orElseThrow();
        assertInstanceOf(FragmentContent.InlineSegments.class, ((DocumentFragmentClipboardPayload) inlineCopy.payload().orElseThrow()).fragment().content());
        var destination = new EditorSession(new Document(List.of(emptyParagraph())), 0);
        assertTrue(destination.pasteFromClipboard(inlineCopy.payload(), inlineCopy.plainText()));
        assertEquals(paragraph("Title"), destination.current().document().blocks().get(0));
        source.setCurrent(new EditorState(source.current().document(), new BlockSelection(0), Optional.empty()));
        var blockCopy = source.copyForClipboard().orElseThrow();
        destination.setCurrent(new EditorState(destination.current().document(), new DocumentPosition(0, 0)));
        assertTrue(destination.pasteFromClipboard(blockCopy.payload(), blockCopy.plainText()));
        assertInstanceOf(Heading.class, destination.current().document().blocks().get(0));
        valid(destination);
    }

    @Test void multipleInlineSegmentsReplaceBackwardRangeWithLeftOwnershipAndCaretBeforeSuffix() {
        var source = new EditorSession(new Document(List.of(heading("source", 2, "AB"), emptyParagraph(), paragraph("CD"))), 0);
        select(source, new DocumentPosition(2, 1), new DocumentPosition(0, 1));
        var copy = source.copyForClipboard().orElseThrow();
        assertEquals("B\n\nC", copy.plainText());
        var destination = new EditorSession(new Document(List.of(heading("left", 4, "xy"), heading("right", 2, "uv"))), 0);
        select(destination, new DocumentPosition(1, 1), new DocumentPosition(0, 1));
        var before = destination.current();
        assertTrue(destination.pasteFromClipboard(copy.payload(), copy.plainText()));
        assertEquals(List.of(new Heading("left", 4, new InlineContent(List.of(text("x"), text("B")))), emptyParagraph(), paragraph(text("C"), text("v"))), destination.current().document().blocks());
        assertEquals(new DocumentPosition(2, 1), destination.current().caret());
        assertEquals(1, destination.undoDepth());
        destination.undo();
        assertEquals(before, destination.current());
    }

    @Test void boundaryOnlyInlineCopyPastePreservesBoundaryEvenInEmptyHeading() {
        var source = new EditorSession(new Document(List.of(paragraph("a"), paragraph("b"))), 0);
        select(source, new DocumentPosition(0, 1), new DocumentPosition(1, 0));
        var copy = source.copyForClipboard().orElseThrow();
        assertEquals("\n", copy.plainText());
        var destination = new EditorSession(new Document(List.of(heading("h", 2, ""))), 0);
        assertTrue(destination.pasteFromClipboard(copy.payload(), copy.plainText()));
        assertEquals(List.of(heading("h", 2, ""), emptyParagraph()), destination.current().document().blocks());
        assertEquals(new DocumentPosition(1, 0), destination.current().caret());
        valid(destination);
    }

    @Test void provenExternalReferencePreservesThenDeletedTargetDegradesOnPaste() {
        var source = new EditorSession(new Document(List.of(paragraph(ref(CrossReferenceTargetKind.FIGURE, "f")),
                new FigureBlock("f", plot("F"), inline("caption")), emptyParagraph())), 0);
        select(source, new DocumentPosition(0, 0), new DocumentPosition(0, 1));
        var copy = source.copyForClipboard().orElseThrow();
        source.setCurrent(new EditorState(source.current().document(), new DocumentPosition(2, 0)));
        assertTrue(source.pasteFromClipboard(copy.payload(), copy.plainText()));
        assertEquals(ref(CrossReferenceTargetKind.FIGURE, "f"), ((Paragraph) source.current().document().blocks().get(2)).content().nodes().get(0));
        source.setCurrent(new EditorState(source.current().document(), new BlockSelection(1), Optional.empty()));
        assertTrue(source.deleteForward());
        source.setCurrent(new EditorState(source.current().document(), new DocumentPosition(1, 1)));
        assertTrue(source.pasteFromClipboard(copy.payload(), copy.plainText()));
        assertEquals(text("Figure 1"), ((Paragraph) source.current().document().blocks().get(1)).content().nodes().get(1));
        valid(source);
    }

    @Test void externalCrossDocumentSameIdCannotCaptureUnrelatedTarget() {
        var source = new EditorSession(new Document(List.of(paragraph(ref(CrossReferenceTargetKind.FIGURE, "f")),
                new FigureBlock("f", plot("Source"), inline("source")))), 0);
        select(source, new DocumentPosition(0, 0), new DocumentPosition(0, 1));
        var clipboard = new MemoryClipboard();
        var sidecar = new ScholarClipboardService();
        BuiltInEditorActions.copy().execute(new EditorActionContext(source, clipboard, sidecar));
        var destination = new EditorSession(new Document(List.of(emptyParagraph(), new FigureBlock("f", plot("Unrelated"), inline("other")))), 0);
        assertTrue(BuiltInEditorActions.paste().execute(new EditorActionContext(destination, clipboard, sidecar)).documentChanged());
        assertEquals(List.of(text("Figure 1")), ((Paragraph) destination.current().document().blocks().get(0)).content().nodes());
        assertEquals("f", ((FigureBlock) destination.current().document().blocks().get(1)).id());
    }

    @Test void internalReferenceWithTargetRemapsAcrossActualClipboardPaste() {
        var source = new Document(List.of(new FigureBlock("f", plot("F"), inline("caption")), paragraph(ref(CrossReferenceTargetKind.FIGURE, "f"))));
        var carrier = extract(source, List.of(0, 1));
        var destination = new EditorSession(new Document(List.of(new FigureBlock("f", plot("Unrelated"), inline("other")), emptyParagraph())), 1);
        pasteThroughClipboard(destination, carrier);
        var figure = (FigureBlock) destination.current().document().blocks().get(1);
        assertEquals("f-2", figure.id());
        assertEquals(ref(CrossReferenceTargetKind.FIGURE, "f-2"), ((Paragraph) destination.current().document().blocks().get(2)).content().nodes().get(0));
        valid(destination);
    }

    @Test void datasetViewsShareOneFreshResourceAndUndoRedoAreAtomic() {
        var source = new Document(List.of(new TableBlock(new DatasetTableBinding("projectile")), datasetPlot(),
                new FigureBlock("f", datasetPlot(), inline("caption"))), List.of(projectileDataset()));
        var carrier = extract(source, List.of(0, 1, 2));
        var destination = new EditorSession(new Document(List.of(emptyParagraph()), List.of(projectileDataset().withDisplayName("Unrelated"))), 0);
        var before = destination.current();
        pasteThroughClipboard(destination, carrier);
        assertEquals(List.of("projectile", "projectile-2"), destination.current().document().datasets().stream().map(ScientificDataset::id).toList());
        assertBindings(destination.current().document(), "projectile-2");
        var pasted = destination.current();
        destination.undo();
        assertEquals(before, destination.current());
        destination.redo();
        assertEquals(pasted, destination.current());
        destination.setCurrent(new EditorState(destination.current().document(), new DocumentPosition(destination.current().document().blocks().size() - 1, 0)));
        pasteThroughClipboard(destination, carrier);
        assertEquals(List.of("projectile", "projectile-2", "projectile-3"), destination.current().document().datasets().stream().map(ScientificDataset::id).toList());
        valid(destination);
    }

    @Test void sameDocumentBoundViewPasteReusesExactDatasetButDirectDatasetImportDoesNot() {
        var source = new EditorSession(new Document(List.of(emptyParagraph(), new TableBlock(new DatasetTableBinding("projectile")), emptyParagraph()), List.of(projectileDataset())), 0);
        source.setCurrent(new EditorState(source.current().document(), new BlockSelection(1), Optional.empty()));
        var copy = source.copyForClipboard().orElseThrow();
        assertTrue(copy.plainText().contains("Time"));
        source.setCurrent(new EditorState(source.current().document(), new DocumentPosition(2, 0)));
        assertTrue(source.pasteFromClipboard(copy.payload(), copy.plainText()));
        assertEquals(1, source.current().document().datasets().size());
        assertEquals("projectile", ((TableBlock) source.current().document().blocks().get(2)).datasetBinding().orElseThrow().datasetId());
        var direct = source.copyDatasetForClipboard("projectile").orElseThrow();
        var blocks = source.current().document().blocks();
        assertTrue(source.pasteFromClipboard(direct.payload(), direct.plainText()));
        assertEquals(blocks, source.current().document().blocks());
        assertEquals(2, source.current().document().datasets().size());
        valid(source);
    }

    @Test void editedDatasetWitnessTransfersCarriedSnapshotInsteadOfBindingEditedResource() {
        var source = new EditorSession(new Document(List.of(emptyParagraph(), datasetPlot(), emptyParagraph()), List.of(projectileDataset())), 0);
        source.setCurrent(new EditorState(source.current().document(), new BlockSelection(1), Optional.empty()));
        var copy = source.copyForClipboard().orElseThrow();
        assertTrue(source.renameDataset("projectile", "Changed"));
        source.setCurrent(new EditorState(source.current().document(), new DocumentPosition(2, 0)));
        assertTrue(source.pasteFromClipboard(copy.payload(), copy.plainText()));
        assertEquals(2, source.current().document().datasets().size());
        assertEquals(projectileDataset().withId("projectile-2"), source.current().document().datasets().get(1));
        valid(source);
    }

    @Test void rejectedRecognizedPayloadNeverFallsBackToItsReadableTextOrTouchesHistory() {
        var session = new EditorSession(new Document(List.of(emptyParagraph())), 0);
        assertTrue(session.typeText("a"));
        assertTrue(session.undo());
        var before = session.current();
        var redo = session.redoDepth();
        var math = new MathClipboardPayload(new MathSequence(List.of(new MathIdentifier("x"))));
        assertFalse(session.pasteFromClipboard(Optional.of(math), "x"));
        assertSame(before, session.current());
        assertEquals(redo, session.redoDepth());
        var incomplete = new DocumentFragment(new FragmentContent.Blocks(List.of(new TableBlock(new DatasetTableBinding("missing")))), List.of());
        assertFalse(session.pasteFromClipboard(Optional.of(new DocumentFragmentClipboardPayload(incomplete, SourceTransferMetadata.unknown())), "Readable table"));
        assertSame(before, session.current());
        assertEquals(0, session.undoDepth());
        assertEquals(redo, session.redoDepth());
    }

    @Test void candidateValidationRejectsUnrelatedInvalidDestinationWithoutPartialResourceInsertion() {
        var badCell = new TableCell(new TableCellContent(new InlineContent(List.of(new InlineNode() {}))));
        var unknown = new TableBlock(List.of(new TableRow(List.of(badCell))), 0);
        var destination = new EditorSession(new Document(List.of(emptyParagraph(), unknown)), 0);
        var before = destination.current();
        var carrier = extract(new Document(List.of(datasetPlot()), List.of(projectileDataset())), List.of(0));
        assertFalse(destination.pasteFromClipboard(Optional.of(carrier), "plot"));
        assertSame(before, destination.current());
        assertTrue(destination.current().document().datasets().isEmpty());
        assertEquals(0, destination.undoDepth());
        assertTrue(destination.transferDiagnostics().stream().anyMatch(d -> d.code() == TransferDiagnostic.Code.INSERTION_VALIDATION_FAILURE));
    }

    @Test void staleInsertionAndMaterializationMismatchHaveNoApplicablePartialEdit() {
        var editor = new DocumentEditor();
        var inserter = new TransferInserter(editor);
        var captured = editor.initialState(new Document(List.of(paragraph("x"))), 0);
        var carrier = extract(new Document(List.of(table("t"))), List.of(0));
        var context = inserter.contextFor(captured, carrier.fragment(), RuntimeDocumentToken.create(), carrier.sourceMetadata());
        var plan = ((PlanningResult.Success) new TransferPlanner().plan(carrier.fragment(), context)).plan();
        var transfer = ((MaterializationResult.Success) new TransferMaterializer().materialize(carrier.fragment(), plan)).transfer();
        var changedSelection = new EditorState(captured.document(), new DocumentPosition(0, 0));
        var result = assertInstanceOf(TransferInsertionResult.Failure.class, inserter.stage(captured, changedSelection, transfer));
        assertEquals(TransferDiagnostic.Code.STALE_DESTINATION, result.diagnostics().get(0).code());
        assertInstanceOf(MaterializationResult.Failure.class, new TransferMaterializer().materialize(new DocumentFragment(carrier.fragment().content(), List.of()), plan));
        assertEquals(List.of(paragraph("x")), captured.document().blocks());
    }

    @Test void cutWritesBeforeDeleteAndClipboardFailureOrReentrantStateChangeCannotDelete() {
        var session = new EditorSession(new Document(List.of(emptyParagraph(), new FigureBlock("f", plot("F"), inline("caption")))), 0);
        session.setCurrent(new EditorState(session.current().document(), new BlockSelection(1), Optional.empty()));
        var before = session.current();
        var clipboard = new MemoryClipboard();
        var sidecar = new ScholarClipboardService();
        clipboard.succeeds = false;
        assertFalse(BuiltInEditorActions.cut().execute(new EditorActionContext(session, clipboard, sidecar)).documentChanged());
        assertSame(before, session.current());
        assertEquals(0, session.undoDepth());
        clipboard.succeeds = true;
        clipboard.onWrite = () -> session.setCurrent(new EditorState(session.current().document(), new DocumentPosition(0, 0)));
        assertFalse(BuiltInEditorActions.cut().execute(new EditorActionContext(session, clipboard, sidecar)).documentChanged());
        assertEquals(before.document(), session.current().document());
        clipboard.onWrite = null;
        session.setCurrent(before);
        assertTrue(BuiltInEditorActions.cut().execute(new EditorActionContext(session, clipboard, sidecar)).documentChanged());
        assertEquals(1, session.undoDepth());
        var payload = sidecar.snapshot().orElseThrow().payload();
        session.undo();
        assertEquals(before, session.current());
        assertSame(payload, sidecar.snapshot().orElseThrow().payload());
    }

    @Test void copyPreservesRedoAndNoOpPasteCreatesNoTransaction() {
        var session = new EditorSession(new Document(List.of(paragraph("a"))), 0);
        session.typeText("b");
        session.undo();
        select(session, new DocumentPosition(0, 0), new DocumentPosition(0, 1));
        var copy = session.copyForClipboard().orElseThrow();
        assertTrue(copy.payload().isPresent());
        assertEquals(1, session.redoDepth());
        session.setCurrent(new EditorState(session.current().document(), new DocumentPosition(0, 0)));
        var empty = new DocumentFragment(new FragmentContent.InlineSegments(List.of(inline(""))), List.of());
        assertFalse(session.pasteFromClipboard(Optional.of(new DocumentFragmentClipboardPayload(empty, SourceTransferMetadata.unknown())), ""));
        assertEquals(0, session.undoDepth());
        assertEquals(1, session.redoDepth());
    }

    @Test void externalPlainTextNormalizesNewlinesWithoutStructuralPaste() {
        var session = new EditorSession(new Document(List.of(emptyParagraph())), 0);
        assertTrue(session.pasteFromClipboard(Optional.empty(), "a\r\nb\nc"));
        assertEquals(paragraph("a b c"), session.current().document().blocks().get(0));
        assertEquals(1, session.current().document().blocks().size());
    }

    @Test void wholeAuthoredParagraphCollectionEndsInValidCaretAndAddsOnlyOneFallback() {
        var carrier = extract(new Document(List.of(paragraph("A"), paragraph("B"))), List.of(0, 1));
        var destination = new EditorSession(new Document(List.of(emptyParagraph())), 0);
        pasteThroughClipboard(destination, carrier);
        assertEquals(List.of(paragraph("A"), paragraph("B"), emptyParagraph()), destination.current().document().blocks());
        assertEquals(new DocumentPosition(1, 1), destination.current().caret());
        valid(destination);
    }

    @Test void tocEntriesComeFromDestinationRatherThanSourceSnapshot() {
        var source = new EditorSession(new Document(List.of(heading("source", 1, "Source"), new TableOfContentsBlock(), emptyParagraph())), 0);
        source.setCurrent(new EditorState(source.current().document(), new BlockSelection(1), Optional.empty()));
        var copy = source.copyForClipboard().orElseThrow();
        var destination = new EditorSession(new Document(List.of(heading("dest", 2, "Destination"), emptyParagraph())), 1);
        assertTrue(destination.pasteFromClipboard(copy.payload(), copy.plainText()));
        var structure = new DocumentStructureResolver().resolve(destination.current().document());
        assertEquals(List.of("dest"), structure.sections().stream().map(section -> section.id().orElseThrow()).toList());
        assertFalse(new DocumentPlainTextSerializer().serializeTableOfContents(destination.current().document()).contains("Source"));
    }

    @Test void goldenMixedTransferCollisionsSharingDegradationAndUndoAllRedoAll() {
        var roots = new ArrayList<>(canonicalDocument().blocks());
        roots.set(11, new FigureBlock("trajectory-figure", datasetPlot(), inline("Shared dataset")));
        var original = ((Paragraph) roots.get(1)).content().nodes();
        var nodes = new ArrayList<>(original);
        nodes.add(ref(CrossReferenceTargetKind.FIGURE, "left-behind"));
        roots.set(1, new Paragraph(new InlineContent(nodes)));
        roots.add(new FigureBlock("left-behind", plot("Source external"), inline("source")));
        var source = new Document(roots, canonicalDocument().datasets());
        var carrier = extract(source, java.util.stream.IntStream.range(0, roots.size() - 1).boxed().toList());
        var destination = new EditorSession(new Document(List.of(heading("intro", 1, "Existing"), heading("analysis", 2, "Existing analysis"),
                equation("velocity-eq"), table("measurements"), new FigureBlock("trajectory-figure", plot("Old"), inline("old")),
                new FigureBlock("left-behind", plot("Unrelated external"), inline("other")), paragraph("XY")), List.of(projectileDataset().withDisplayName("Other"))), 6);
        destination.setCurrent(new EditorState(destination.current().document(), new DocumentPosition(6, 1)));
        var before = destination.current();
        pasteThroughClipboard(destination, carrier);
        var pasted = destination.current();
        var inserted = pasted.document().blocks().subList(7, 7 + roots.size() - 1);
        assertEquals("intro-2", ((Heading) inserted.get(0)).id().orElseThrow());
        var paragraph = (Paragraph) inserted.get(1);
        assertTrue(paragraph.content().nodes().contains(ref(CrossReferenceTargetKind.SECTION, "analysis-2")));
        assertTrue(paragraph.content().nodes().contains(ref(CrossReferenceTargetKind.EQUATION, "velocity-eq-2")));
        assertTrue(paragraph.content().nodes().contains(ref(CrossReferenceTargetKind.TABLE, "measurements-2")));
        assertTrue(paragraph.content().nodes().contains(ref(CrossReferenceTargetKind.FIGURE, "trajectory-figure-2")));
        assertInstanceOf(Text.class, paragraph.content().nodes().get(paragraph.content().nodes().size() - 1));
        assertEquals(2, pasted.document().datasets().size());
        assertBindings(pasted.document(), "projectile-2");
        assertEquals("trajectory-figure-2", ((FigureBlock) inserted.get(11)).id());
        assertInstanceOf(PlotBlock.class, ((FigureBlock) inserted.get(11)).content());
        assertInstanceOf(DiagramBlock.class, ((FigureBlock) inserted.get(12)).content());
        valid(destination);
        assertEquals(1, destination.undoDepth());
        while (destination.canUndo()) { destination.undo(); valid(destination); }
        assertEquals(before, destination.current());
        while (destination.canRedo()) { destination.redo(); valid(destination); }
        assertEquals(pasted, destination.current());
        assertEquals(source, new Document(roots, canonicalDocument().datasets()));
    }

    @Test void boundedFixedSeedTransfersRemainValidAndUndoRedoExact() {
        var random = new java.util.Random(25025);
        for (var i = 0; i < 50; i++) {
            var id = "h-" + random.nextInt(8);
            var source = new Document(List.of(heading(id, 2, "New"), paragraph(ref(CrossReferenceTargetKind.SECTION, id)),
                    new TableBlock(new DatasetTableBinding("projectile")), datasetPlot()), List.of(projectileDataset()));
            var carrier = extract(source, List.of(0, 1, 2, 3));
            var existing = new ArrayList<BlockNode>();
            existing.add(heading(id, 1, "Old"));
            if (random.nextBoolean()) { existing.add(heading(id + "-2", 1, "Old suffix")); }
            existing.add(emptyParagraph());
            var destination = new EditorSession(new Document(existing, List.of(projectileDataset())), existing.size() - 1);
            var before = destination.current();
            pasteThroughClipboard(destination, carrier);
            var pasted = destination.current();
            assertBindings(pasted.document(), "projectile-2");
            valid(destination);
            destination.undo();
            assertEquals(before, destination.current());
            destination.redo();
            assertEquals(pasted, destination.current());
            valid(destination);
        }
    }

    @Test void tableCellRetainsExplicitTextualConversionButEquationRejectsDocumentCarrier() {
        var tableSession = new EditorSession(new Document(List.of(emptyParagraph(), TableBlock.empty(1, 1))), 0);
        tableSession.setCurrent(new EditorState(tableSession.current().document(), new BlockSelection(1), Optional.empty()));
        tableSession.enter();
        var carrier = extract(new Document(List.of(new FigureBlock("f", plot("F"), inline("caption")))), List.of(0));
        assertTrue(tableSession.pasteFromClipboard(Optional.of(carrier), "Figure\ncaption"));
        var table = (TableBlock) tableSession.current().document().blocks().get(1);
        assertEquals("Figure caption", new CrossReferenceResolver().inlineText(tableSession.current().document(), table.rows().get(0).cells().get(0).content().content()));
        assertTrue(tableSession.current().isTableEditingSelection());
        assertEquals(1, tableSession.undoDepth());
        var equationSession = new EditorSession(new Document(List.of(emptyParagraph(), equation("e"))), 0);
        equationSession.setCurrent(new EditorState(equationSession.current().document(), new BlockSelection(1), Optional.empty()));
        equationSession.enter();
        var before = equationSession.current();
        assertFalse(equationSession.canPasteFromClipboard(Optional.of(carrier), "x+1"));
        assertFalse(equationSession.pasteFromClipboard(Optional.of(carrier), "x+1"));
        assertSame(before, equationSession.current());
        assertEquals(0, equationSession.undoDepth());
    }

    @Test void sourceExportFailureDoesNotEnableCutOrDeleteContent() {
        var badCell = new TableCell(new TableCellContent(new InlineContent(List.of(text("A\tB")))));
        var table = new TableBlock(List.of(new TableRow(List.of(badCell))), 0);
        var session = new EditorSession(new Document(List.of(emptyParagraph(), table)), 0);
        session.setCurrent(new EditorState(session.current().document(), new BlockSelection(1), Optional.empty()));
        var before = session.current();
        assertTrue(session.copyForClipboard().isEmpty());
        assertTrue(session.cutForClipboard().isEmpty());
        assertFalse(BuiltInEditorActions.cut().execute(new EditorActionContext(session, new MemoryClipboard())).documentChanged());
        assertSame(before, session.current());
        assertEquals(0, session.undoDepth());
    }

    @Test void sourceMissingColumnWarningIsPreservedWithoutFlatteningOrRepair() {
        var table = new TableBlock(new DatasetTableBinding("projectile", List.of("missing-column")));
        var source = new EditorSession(new Document(List.of(emptyParagraph(), table), List.of(projectileDataset())), 0);
        source.setCurrent(new EditorState(source.current().document(), new BlockSelection(1), Optional.empty()));
        var copy = source.copyForClipboard().orElseThrow();
        assertEquals("[Missing column]", copy.plainText());
        var destination = new EditorSession(new Document(List.of(emptyParagraph())), 0);
        assertTrue(destination.pasteFromClipboard(copy.payload(), copy.plainText()));
        var actual = (TableBlock) destination.current().document().blocks().get(0);
        assertEquals(List.of("missing-column"), actual.datasetBinding().orElseThrow().columnIds());
        assertTrue(destination.transferDiagnostics().stream().anyMatch(d -> d.code() == TransferDiagnostic.Code.SOURCE_VALIDATION_WARNING));
        assertFalse(DocumentValidator.validate(destination.current().document()).warnings().isEmpty());
        valid(destination);
    }

    @Test void authoredTableSourceDisplayFallbackResolvesReferenceAndRichContentRemainsSemantic() {
        var table = new TableBlock(List.of(new TableRow(List.of(new TableCell(new TableCellContent(new InlineContent(List.of(ref(CrossReferenceTargetKind.SECTION, "h")))))))), 0);
        var source = new EditorSession(new Document(List.of(heading("h", 1, "Title"), table)), 0);
        source.setCurrent(new EditorState(source.current().document(), new BlockSelection(1), Optional.empty()));
        var copy = source.copyForClipboard().orElseThrow();
        assertEquals("Section 1", copy.plainText());
        assertInstanceOf(CrossReference.class, TransferClipboardAssertions.root(TableBlock.class, copy.payload().orElseThrow()).rows().get(0).cells().get(0).content().content().nodes().get(0));
        var destination = new EditorSession(new Document(List.of(emptyParagraph())), 0);
        assertTrue(destination.pasteFromClipboard(copy.payload(), copy.plainText()));
        assertEquals(text("Section 1"), ((TableBlock) destination.current().document().blocks().get(0)).rows().get(0).cells().get(0).content().content().nodes().get(0));
    }

    @Test void developmentFixtureSupportsSelfReferenceRemapAndExternalCaptionSafety() {
        var document = dev.rgcb.scholar.client.DevelopmentDocument.createEditable();
        assertTrue(DocumentValidator.validate(document).isValid());
        var index = java.util.stream.IntStream.range(0, document.blocks().size()).filter(i ->
                document.blocks().get(i) instanceof FigureBlock f && f.id().equals("m25-transfer-figure")).findFirst().orElseThrow();
        var source = new EditorSession(document, 0);
        source.setCurrent(new EditorState(document, new BlockSelection(index), Optional.empty()));
        var copy = source.copyForClipboard().orElseThrow();
        source.setCurrent(new EditorState(document, new DocumentPosition(0, 0)));
        assertTrue(source.pasteFromClipboard(copy.payload(), copy.plainText()));
        var same = (FigureBlock) source.current().document().blocks().get(0);
        assertTrue(same.caption().nodes().contains(ref(CrossReferenceTargetKind.FIGURE, "m25-transfer-figure-2")));
        assertTrue(same.caption().nodes().contains(ref(CrossReferenceTargetKind.FIGURE, "shaft-assembly")));
        assertEquals(document.datasets().size(), source.current().document().datasets().size());
        // A reopened owner gets a new token even when given the exact same AST object.
        var reopened = new EditorSession(document, 0);
        reopened.setCurrent(new EditorState(document, new DocumentPosition(0, 0)));
        assertTrue(reopened.pasteFromClipboard(copy.payload(), copy.plainText()));
        var cross = (FigureBlock) reopened.current().document().blocks().get(0);
        assertTrue(cross.caption().nodes().contains(ref(CrossReferenceTargetKind.FIGURE, "m25-transfer-figure-2")));
        assertFalse(cross.caption().nodes().contains(ref(CrossReferenceTargetKind.FIGURE, "shaft-assembly")));
        assertEquals(document.datasets().size() + 1, reopened.current().document().datasets().size());
        valid(source);
        valid(reopened);
    }

    private static Paragraph emptyParagraph() { return new Paragraph(new InlineContent(List.of())); }

    private static BlockNode root(Family family) {
        return switch (family) {
            case HEADING -> heading("h", 2, "Heading");
            case EQUATION -> equation("e");
            case TABLE -> table("t");
            case BOUND_TABLE -> new TableBlock(new DatasetTableBinding("projectile")).withId("t");
            case PLOT -> plot("Plot");
            case BOUND_PLOT -> datasetPlot();
            case DIAGRAM -> genericDiagram();
            case ELECTRICAL -> electricalDiagram();
            case MECHANICAL -> mechanicalDiagram();
            case FIGURE_PLOT -> new FigureBlock("f", datasetPlot(), inline("caption"));
            case FIGURE_DIAGRAM -> new FigureBlock("f", mechanicalDiagram(), inline("caption"));
            case TOC -> new TableOfContentsBlock();
        };
    }
    private static void select(EditorSession session, DocumentPosition anchor, DocumentPosition active) {
        session.setCurrent(new EditorState(session.current().document(), anchor, active));
    }
    private static DocumentFragmentClipboardPayload extract(Document source, List<Integer> indices) {
        var success = assertInstanceOf(ExtractionResult.Success.class, new FragmentExtractor().extract(source,
                new FragmentExtractionRequest.Blocks(indices), Optional.empty()));
        return new DocumentFragmentClipboardPayload(success.fragment(), success.sourceMetadata());
    }
    private static void pasteThroughClipboard(EditorSession destination, DocumentFragmentClipboardPayload payload) {
        var clipboard = new MemoryClipboard();
        clipboard.text = "semantic fixture fallback";
        var sidecar = new ScholarClipboardService();
        sidecar.install(clipboard.text, payload);
        assertTrue(BuiltInEditorActions.paste().execute(new EditorActionContext(destination, clipboard, sidecar)).documentChanged());
    }
    private static void assertBindings(Document document, String expected) {
        for (var root : document.blocks()) {
            if (root instanceof FigureBlock figure) { root = figure.content(); }
            if (root instanceof TableBlock table && table.datasetBinding().isPresent()) { assertEquals(expected, table.datasetBinding().orElseThrow().datasetId()); }
            if (root instanceof PlotBlock plot) {
                plot.definition().series().forEach(series -> series.datasetBinding().ifPresent(binding -> assertEquals(expected, binding.datasetId())));
            }
        }
    }
    private static void valid(EditorSession session) {
        assertTrue(DocumentValidator.validate(session.current().document()).isValid());
        assertTrue(new EditorSelectionValidator().isValid(session.current()));
    }
    private static final class MemoryClipboard implements ClipboardAdapter {
        String text = "";
        boolean succeeds = true;
        Runnable onWrite;
        public String getText() { return text; }
        public boolean setText(String value) {
            if (!succeeds) { return false; }
            text = value;
            if (onWrite != null) { onWrite.run(); }
            return true;
        }
    }
}
