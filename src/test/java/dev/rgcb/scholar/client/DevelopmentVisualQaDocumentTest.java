package dev.rgcb.scholar.client;

import dev.rgcb.scholar.document.DiagramBlock;
import dev.rgcb.scholar.document.EquationBlock;
import dev.rgcb.scholar.document.FigureBlock;
import dev.rgcb.scholar.document.Heading;
import dev.rgcb.scholar.document.LayoutSectionBreak;
import dev.rgcb.scholar.document.ColumnLayout;
import dev.rgcb.scholar.document.Text;
import dev.rgcb.scholar.document.PlotBlock;
import dev.rgcb.scholar.document.TableBlock;
import dev.rgcb.scholar.editor.EditorSelectionValidator;
import dev.rgcb.scholar.editor.EditorSession;
import dev.rgcb.scholar.editor.BlockSelection;
import dev.rgcb.scholar.editor.DocumentPosition;
import dev.rgcb.scholar.editor.EditorState;
import dev.rgcb.scholar.editor.TextSelection;
import dev.rgcb.scholar.math.editor.MathCaretGeometryResolver;
import dev.rgcb.scholar.math.editor.MathExpressionEditor;
import dev.rgcb.scholar.math.layout.MathLayoutEngine;
import dev.rgcb.scholar.math.layout.MathTextMeasurer;
import dev.rgcb.scholar.math.layout.MathTextMetrics;
import dev.rgcb.scholar.validation.DocumentValidator;
import dev.rgcb.scholar.persistence.DocumentJsonCodec;
import dev.rgcb.scholar.persistence.PersistenceResult;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DevelopmentVisualQaDocumentTest {
    @Test
    void visualQaFixtureIsValidEditableAndCoversScientificBlocks() {
        var document = DevelopmentDocument.createVisualQa();
        var validation = DocumentValidator.validate(document);

        assertTrue(validation.isValid(), () -> validation.diagnostics().toString());
        assertTrue(validation.errors().isEmpty());
        assertFalse(document.datasets().isEmpty());
        assertTrue(document.blocks().stream().anyMatch(EquationBlock.class::isInstance));
        assertTrue(document.blocks().stream().anyMatch(TableBlock.class::isInstance));
        assertTrue(document.blocks().stream().anyMatch(PlotBlock.class::isInstance));
        assertTrue(document.blocks().stream().anyMatch(FigureBlock.class::isInstance));
        assertTrue(document.blocks().stream().anyMatch(DiagramBlock.class::isInstance));
        assertTrue(document.blocks().stream().anyMatch(block -> block.equals(new LayoutSectionBreak(ColumnLayout.two()))));
        assertTrue(document.blocks().stream().anyMatch(block -> block.equals(new LayoutSectionBreak(ColumnLayout.one()))));

        var session = new EditorSession(document, 0);
        var initialSelection = (TextSelection) session.current().selection();
        assertEquals(0, initialSelection.anchor().blockIndex());
        assertEquals(initialSelection.anchor(), initialSelection.active());
        assertTrue(new EditorSelectionValidator().isValid(session.current()));
    }

    @Test
    void highFidelityFixtureIncludesDerivedQuadraticCurveWithoutPersistingSamples() {
        var document = DevelopmentDocument.createHighFidelityQa();
        assertTrue(DocumentValidator.validate(document).isValid());
        var fitPlot = (PlotBlock) document.blocks().getLast();
        var fit = fitPlot.definition().series().get(1);
        assertTrue(fit.points().isEmpty());
        assertEquals(65, new dev.rgcb.scholar.data.DatasetPlotResolver()
                .resolve(document, fitPlot).definition().series().get(1).points().size());
        assertTrue(document.blocks().stream().filter(dev.rgcb.scholar.document.Paragraph.class::isInstance)
                .map(dev.rgcb.scholar.document.Paragraph.class::cast)
                .anyMatch(paragraph -> paragraph.content().nodes().stream()
                        .filter(Text.class::isInstance).map(Text.class::cast)
                        .anyMatch(text -> text.content().contains("H₂O H₂O"))));
    }

    @Test
    void explicitStressProfilesRemainSeparateFromVisualQaDefault() {
        var visualQa = DevelopmentDocument.createVisualQa();
        for (var profile : DevelopmentStressDocument.Profile.values()) {
            var stress = DevelopmentStressDocument.create(profile);
            assertFalse(stress.equals(visualQa), profile.name());
            assertTrue(DocumentValidator.validate(stress).isValid(), profile.name());
        }
    }

    @Test
    void visualQaHeadingsDoNotDuplicateDerivedNumbering() {
        var headings = DevelopmentDocument.createVisualQa().blocks().stream()
                .filter(Heading.class::isInstance)
                .map(Heading.class::cast)
                .toList();

        assertTrue(headings.stream().noneMatch(heading -> heading.content().nodes().stream()
                .filter(Text.class::isInstance)
                .map(Text.class::cast)
                .map(Text::content)
                .reduce("", String::concat)
                .matches("\\d+\\..*")));
    }

    @Test
    void everyReachableVisualQaMathCaretHasLayoutGeometry() {
        var editor = new MathExpressionEditor();
        var layoutEngine = new MathLayoutEngine();
        var geometryResolver = new MathCaretGeometryResolver();
        MathTextMeasurer measurer = (content, kind) -> new MathTextMetrics(content.length() * 5, 7, 3);

        for (var block : DevelopmentDocument.createVisualQa().blocks()) {
            if (!(block instanceof EquationBlock equation)) {
                continue;
            }
            var layout = layoutEngine.layout(equation.expression(), measurer);
            for (var position : editor.positions(equation.expression())) {
                try {
                    geometryResolver.resolve(position, layout, measurer);
                } catch (RuntimeException exception) {
                    fail("Equation " + equation.id() + " has no caret geometry for " + position, exception);
                }
            }
        }
    }

    @Test
    void visualQaGoldenScenarioPersistsTransfersAndRestoresHistoryExactly() {
        var source = DevelopmentDocument.createVisualQa();
        var codec = new DocumentJsonCodec();
        var encoded = (String) assertInstanceOf(PersistenceResult.Success.class, codec.encode(source)).value();
        var decoded = (dev.rgcb.scholar.document.Document) assertInstanceOf(
                PersistenceResult.Success.class, codec.decode(encoded)).value();
        assertEquals(source, decoded);

        var figureIndex = java.util.stream.IntStream.range(0, source.blocks().size())
                .filter(index -> source.blocks().get(index) instanceof FigureBlock figure
                        && figure.content() instanceof PlotBlock plot
                        && plot.definition().series().stream().anyMatch(series -> series.datasetBinding().isPresent()))
                .findFirst().orElseThrow();
        var sourceSession = new EditorSession(source, 0);
        sourceSession.setCurrent(new EditorState(source, new BlockSelection(figureIndex), Optional.empty()));
        var copy = sourceSession.copyForClipboard().orElseThrow();

        var empty = new dev.rgcb.scholar.document.Paragraph(new dev.rgcb.scholar.document.InlineContent(List.of()));
        var destination = new EditorSession(new dev.rgcb.scholar.document.Document(List.of(empty)), 0);
        var before = destination.current();
        assertTrue(destination.pasteFromClipboard(copy.payload(), copy.plainText()));
        var after = destination.current();
        assertTrue(DocumentValidator.validate(after.document()).isValid());
        assertTrue(new EditorSelectionValidator().isValid(after));
        assertFalse(after.document().datasets().isEmpty());
        assertTrue(destination.undo());
        assertEquals(before, destination.current());
        assertTrue(destination.redo());
        assertEquals(after, destination.current());
    }
}
