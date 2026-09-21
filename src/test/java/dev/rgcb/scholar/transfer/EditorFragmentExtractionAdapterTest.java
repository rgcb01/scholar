package dev.rgcb.scholar.transfer;

import static dev.rgcb.scholar.transfer.ExtractionFixtures.*;
import static dev.rgcb.scholar.transfer.FragmentExtractorTest.success;
import static org.junit.jupiter.api.Assertions.*;

import dev.rgcb.scholar.document.CrossReference;
import dev.rgcb.scholar.document.CrossReferenceTargetKind;
import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.EquationBlock;
import dev.rgcb.scholar.document.FigureBlock;
import dev.rgcb.scholar.document.Heading;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.Paragraph;
import dev.rgcb.scholar.document.TableBlock;
import dev.rgcb.scholar.document.TableCell;
import dev.rgcb.scholar.document.TableCellContent;
import dev.rgcb.scholar.document.TableRow;
import dev.rgcb.scholar.document.Text;
import dev.rgcb.scholar.document.TextMark;
import dev.rgcb.scholar.editor.BlockSelection;
import dev.rgcb.scholar.editor.DiagramEditingSelection;
import dev.rgcb.scholar.editor.DiagramElementTarget;
import dev.rgcb.scholar.editor.DocumentPosition;
import dev.rgcb.scholar.editor.EditorFragmentExtractionAdapter;
import dev.rgcb.scholar.editor.EditorSelection;
import dev.rgcb.scholar.editor.EquationEditingSelection;
import dev.rgcb.scholar.editor.FigureCaptionSelection;
import dev.rgcb.scholar.editor.PlotEditingSelection;
import dev.rgcb.scholar.editor.PlotProperty;
import dev.rgcb.scholar.editor.PlotPropertyTarget;
import dev.rgcb.scholar.editor.TableCellCoordinate;
import dev.rgcb.scholar.editor.TableCellTextSelection;
import dev.rgcb.scholar.editor.TableEditingSelection;
import dev.rgcb.scholar.editor.TextSelection;
import dev.rgcb.scholar.math.MathNumber;
import dev.rgcb.scholar.math.MathSequence;
import dev.rgcb.scholar.math.editor.MathPath;
import dev.rgcb.scholar.math.editor.MathRangeSelection;
import dev.rgcb.scholar.math.editor.MathSequencePosition;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class EditorFragmentExtractionAdapterTest {
    private final EditorFragmentExtractionAdapter adapter = new EditorFragmentExtractionAdapter();

    static Stream<Set<TextMark>> marks() {
        return Stream.of(Set.of(), Set.of(TextMark.BOLD), Set.of(TextMark.ITALIC), Set.of(TextMark.BOLD, TextMark.ITALIC));
    }

    @ParameterizedTest
    @MethodSource("marks")
    void partialParagraphSlicesExactTextAndMarks(Set<TextMark> marks) {
        var source = new Document(List.of(new Paragraph(inline(new Text("abcd", marks)))));
        var result = success(adapter.extract(source, text(0, 1, 0, 3), Optional.empty()));
        assertEquals(List.of(inline(new Text("bc", marks))), segments(result));
        assertTrue(result.fragment().identities().provided().isEmpty());
    }

    @Test
    void partialHeadingTextExcludesLevelAndSectionIdentity() {
        var source = new Document(List.of(new Heading("h", 4, inline(new Text("abcd", Set.of())))));
        var result = success(adapter.extract(source, text(0, 1, 0, 3), Optional.empty()));
        assertEquals(List.of(inline(new Text("bc", Set.of()))), segments(result));
        assertTrue(result.fragment().identities().provided().isEmpty());
        assertTrue(result.sourceMetadata().targetWitnesses().isEmpty());
    }

    @Test
    void allHeadingTextIsStillInlineButBlockSelectionCarriesSectionId() {
        var source = new Document(List.of(new Heading("h", 1, inline(new Text("abcd", Set.of())))));
        var inline = success(adapter.extract(source, text(0, 0, 0, 4), Optional.empty()));
        var block = success(adapter.extract(source, new BlockSelection(0), Optional.empty()));
        assertInstanceOf(FragmentContent.InlineSegments.class, inline.fragment().content());
        assertTrue(inline.fragment().identities().provided().isEmpty());
        assertInstanceOf(FragmentContent.Blocks.class, block.fragment().content());
        assertEquals(Set.of(new StableIdentityKey(StableIdentityKind.SECTION, "h")), block.fragment().identities().provided());
    }

    @Test
    void forwardAndBackwardSelectionsExtractIdenticalContent() {
        var source = new Document(List.of(new Paragraph(inline(new Text("abcd", Set.of(TextMark.BOLD))))));
        var forward = text(0, 1, 0, 3);
        var backward = text(0, 3, 0, 1);
        assertEquals(success(adapter.extract(source, forward, Optional.empty())).fragment(),
                success(adapter.extract(source, backward, Optional.empty())).fragment());
        assertEquals(new DocumentPosition(0, 3), backward.anchor());
    }

    @Test
    void mixedInlineRangeKeepsAtomicReferenceAndAdjacentPartialRuns() {
        var reference = new CrossReference(CrossReferenceTargetKind.FIGURE, "fig");
        var source = new Document(List.of(new Paragraph(inline(new Text("pre", Set.of(TextMark.BOLD)), reference,
                new Text("post", Set.of(TextMark.ITALIC))))));
        var result = success(adapter.extract(source, text(0, 2, 0, 5), Optional.empty()));
        assertEquals(List.of(inline(new Text("e", Set.of(TextMark.BOLD)), reference, new Text("p", Set.of(TextMark.ITALIC)))), segments(result));
        assertSame(reference, segments(result).getFirst().nodes().get(1));
        var atomic = success(adapter.extract(source, text(0, 3, 0, 4), Optional.empty()));
        assertEquals(List.of(inline(reference)), segments(atomic));
    }

    @Test
    void boundaryOnlySelectionExtractsTwoEmptySegments() {
        var source = new Document(List.of(new Paragraph(inline(new Text("abc", Set.of()))), new Heading("h", 1, inline(new Text("def", Set.of())))));
        var result = success(adapter.extract(source, text(0, 3, 1, 0), Optional.empty()));
        assertEquals(List.of(inline(), inline()), segments(result));
        assertTrue(result.fragment().identities().provided().isEmpty());
    }

    @Test
    void multiBlockRangePreservesEmptyInteriorAndEndpoints() {
        var source = new Document(List.of(new Paragraph(inline(new Text("abc", Set.of()))), new Paragraph(inline()),
                new Heading("h", 2, inline(new Text("def", Set.of(TextMark.BOLD))))));
        var result = success(adapter.extract(source, text(0, 2, 2, 1), Optional.empty()));
        assertEquals(List.of(inline(new Text("c", Set.of())), inline(), inline(new Text("d", Set.of(TextMark.BOLD)))), segments(result));
    }

    @Test
    void unicodeExtractionReusesGraphemeBoundaryRules() {
        var source = new Document(List.of(new Paragraph(inline(new Text("a\uD834\uDD1Ee\u0301z", Set.of())))));
        var result = success(adapter.extract(source, text(0, 1, 0, 3), Optional.empty()));
        assertEquals(List.of(inline(new Text("\uD834\uDD1Ee\u0301", Set.of()))), segments(result));
    }

    @Test
    void caretIsUnsupportedAndInvalidOffsetsFailWithoutNormalization() {
        var source = new Document(List.of(new Paragraph(inline(new Text("abc", Set.of())))));
        assertCode(source, text(0, 1, 0, 1), TransferDiagnostic.Code.UNSUPPORTED_SOURCE_SELECTION);
        assertCode(source, text(0, 0, 0, 4), TransferDiagnostic.Code.MALFORMED_FRAGMENT);
        assertCode(source, new BlockSelection(99), TransferDiagnostic.Code.MALFORMED_FRAGMENT);
        assertCode(source, new BlockSelection(0), TransferDiagnostic.Code.MALFORMED_FRAGMENT);
    }

    @Test
    void textSelectionCannotHarvestAtomicBlockBetweenEndpoints() {
        var source = new Document(List.of(new Paragraph(inline()), new EquationBlock(new MathNumber("1")), new Paragraph(inline())));
        assertCode(source, text(0, 0, 2, 0), TransferDiagnostic.Code.MALFORMED_FRAGMENT);
    }

    @Test
    void equationEditingRangeStaysInMathLocalDomainNotWholeEquation() {
        var equation = new EquationBlock("eq", new MathSequence(List.of(new MathNumber("12"))));
        var range = new MathRangeSelection(new MathSequencePosition(MathPath.ROOT, 0), new MathSequencePosition(MathPath.ROOT, 1));
        assertCode(new Document(List.of(equation)), new EquationEditingSelection(0, range), TransferDiagnostic.Code.UNSUPPORTED_SOURCE_SELECTION);
        var whole = success(adapter.extract(new Document(List.of(equation)), new BlockSelection(0), Optional.empty()));
        assertSame(equation, ((FragmentContent.Blocks) whole.fragment().content()).roots().getFirst());
    }

    @Test
    void tableCellRangeKeepsExistingPlainTextOnlyDomain() {
        var table = new TableBlock(List.of(new TableRow(List.of(new TableCell(new TableCellContent(inline(new Text("cell", Set.of()))))))), 0);
        assertCode(new Document(List.of(table)), new TableEditingSelection(0,
                new TableCellTextSelection(new TableCellCoordinate(0, 0), 0, 4)), TransferDiagnostic.Code.UNSUPPORTED_SOURCE_SELECTION);
    }

    @Test
    void plotEditingTargetDoesNotWidenToWholePlot() {
        assertCode(new Document(List.of(plot())), new PlotEditingSelection(0, new PlotPropertyTarget(PlotProperty.TITLE)),
                TransferDiagnostic.Code.UNSUPPORTED_SOURCE_SELECTION);
    }

    @Test
    void selectedDiagramElementDoesNotBecomePartialGraphOrWholeDiagram() {
        var diagram = diagram();
        assertCode(new Document(List.of(diagram)), new DiagramEditingSelection(0, new DiagramElementTarget(0, diagram.definition().elements().getFirst().id())),
                TransferDiagnostic.Code.UNSUPPORTED_SOURCE_SELECTION);
    }

    @Test
    void captionRangeRemainsUnsupportedAsFrozenClipboardContractRequires() {
        var source = new Document(List.of(new FigureBlock("fig", plot(), inline(new Text("caption", Set.of())))));
        assertCode(source, new FigureCaptionSelection(0, 0, 7), TransferDiagnostic.Code.UNSUPPORTED_SOURCE_SELECTION);
    }

    @Test
    void nullOrUnknownSelectionsFailCleanly() {
        var source = new Document(List.of(new Paragraph(inline())));
        assertCode(source, null, TransferDiagnostic.Code.MALFORMED_FRAGMENT);
        assertCode(source, new EditorSelection() {}, TransferDiagnostic.Code.MALFORMED_FRAGMENT);
        assertCode(null, new BlockSelection(0), TransferDiagnostic.Code.MALFORMED_FRAGMENT);
    }

    private void assertCode(Document source, EditorSelection selection, TransferDiagnostic.Code expected) {
        var result = assertInstanceOf(ExtractionResult.Failure.class, adapter.extract(source, selection, Optional.empty()));
        assertEquals(expected, result.diagnostics().getFirst().code());
    }

    private static TextSelection text(int startBlock, int start, int endBlock, int end) {
        return new TextSelection(new DocumentPosition(startBlock, start), new DocumentPosition(endBlock, end));
    }

    private static List<InlineContent> segments(ExtractionResult.Success result) {
        return ((FragmentContent.InlineSegments) result.fragment().content()).segments();
    }
}
