package dev.rgcb.scholar.compute;

import static org.junit.jupiter.api.Assertions.*;

import dev.rgcb.scholar.clipboard.DocumentFragmentClipboardPayload;
import dev.rgcb.scholar.document.*;
import dev.rgcb.scholar.editor.*;
import dev.rgcb.scholar.layout.*;
import dev.rgcb.scholar.persistence.*;
import dev.rgcb.scholar.quantity.*;
import dev.rgcb.scholar.transfer.*;
import dev.rgcb.scholar.validation.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class M32IntegrationTest {
    private static VariableDefinition variable(String id, String name, String amount, String unit) {
        return new VariableDefinition(id, name, new ScientificValue.Physical(new Quantity(amount, new UnitParser().parseRequired(unit))));
    }

    private static Paragraph paragraph() { return new Paragraph(new InlineContent(List.of(new Text("", Set.of())))); }

    @Test void sessionGoldenScenarioRecomputesWithoutExtraHistoryAndRenameKeepsIdentity() {
        var mass = variable("m-id", "m", "2.5", "kg");
        var gravity = variable("g-id", "g", "9.81", "m/s²");
        var height = variable("h-id", "h", "1.2", "m");
        var session = new EditorSession(new Document(List.of(mass, gravity, height, paragraph())), 3);
        assertTrue(session.insertComputedResult("m*g*h", Optional.of("Potential energy"), Optional.empty(), NumberNotation.DECIMAL));
        var resultIndex = session.current().blockSelection().blockIndex();
        assertEquals("29.43", quantity(session.computations().results().get(resultIndex)).value().stripTrailingZeros().toPlainString());
        var initialDepth = session.undoDepth();
        session.setCurrent(new EditorState(session.current().document(), new BlockSelection(2), Optional.empty()));
        assertTrue(session.editVariable("h", new ScientificValue.Physical(new Quantity("2", UnitExpression.of("metre"))), Optional.empty()));
        assertEquals(initialDepth + 1, session.undoDepth());
        assertEquals("49.05", quantity(session.computations().results().get(resultIndex)).value().stripTrailingZeros().toPlainString());
        assertTrue(session.undo());
        assertEquals("29.43", quantity(session.computations().results().get(resultIndex)).value().stripTrailingZeros().toPlainString());
        assertTrue(session.redo());
        assertEquals("49.05", quantity(session.computations().results().get(resultIndex)).value().stripTrailingZeros().toPlainString());
        session.setCurrent(new EditorState(session.current().document(), new BlockSelection(0), Optional.empty()));
        assertTrue(session.editVariable("mass", mass.value(), Optional.empty()));
        assertEquals("49.05", quantity(session.computations().results().get(resultIndex)).value().stripTrailingZeros().toPlainString());
        var computed = (ComputedResult) session.current().document().blocks().get(resultIndex);
        assertEquals("m-id", computed.variableDependencies().getFirst().variableId());
        assertTrue(new ComputationFormatter().expression(computed.expression(), session.current().document(), false).startsWith("mass"));
    }

    @Test void dependencyCacheInvalidatesOnlyAffectedResult() {
        var source = new Document(List.of(variable("a", "a", "1", "m"), variable("b", "b", "2", "s")));
        var first = new ComputedResult(new ExpressionParser().parse("a*2", source).expression(), "a*2");
        var second = new ComputedResult(new ExpressionParser().parse("b*3", source).expression(), "b*3");
        var engine = new ComputationEngine();
        var document = new Document(List.of(source.blocks().get(0), source.blocks().get(1), first, second));
        engine.update(document);
        assertEquals(2, engine.evaluationCount());
        engine.update(new Document(List.of(variable("a", "a", "3", "m"), source.blocks().get(1), first, second)));
        assertEquals(3, engine.evaluationCount());
    }

    @Test void v2RoundTripPersistsAuthoredStateButNoDerivedCache() {
        var source = new Document(List.of(variable("var-distance", "distance", "10", "m"),
                variable("var-time", "time", "2", "s")));
        var expression = new ExpressionParser().parse("distance/time", source).expression();
        var document = new Document(List.of(source.blocks().get(0), source.blocks().get(1),
                new ComputedResult(expression, "distance/time", Optional.of("Speed"),
                        Optional.of(new UnitParser().parseRequired("km/h")), NumberNotation.SCIENTIFIC)));
        var codec = new DocumentJsonCodec();
        var encoded = assertInstanceOf(String.class,
                assertInstanceOf(PersistenceResult.Success.class, codec.encode(document)).value());
        assertTrue(encoded.contains("\"version\": 2"));
        assertFalse(encoded.contains("29.43"));
        var decoded = assertInstanceOf(Document.class,
                assertInstanceOf(PersistenceResult.Success.class, codec.decode(encoded)).value());
        assertEquals(document, decoded);
        assertTrue(new ComputationEngine().update(decoded).results().get(2).value().isPresent());
    }

    @Test void invalidComputationIsSaveableAndDuplicateVariableIdsAreNot() {
        var parsed = new ExpressionParser().parse("5 m + 3 s", new Document(List.of()));
        var document = new Document(List.of(new ComputedResult(parsed.expression(), "5 m + 3 s")));
        assertTrue(DocumentValidator.validate(document).isValid());
        assertInstanceOf(PersistenceResult.Success.class, new DocumentJsonCodec().encode(document));
        assertEquals(ComputationDiagnostic.Code.INCOMPATIBLE_DIMENSIONS,
                new ComputationEngine().update(document).results().get(0).diagnostics().getFirst().code());
        var duplicate = new Document(List.of(variable("x", "x", "1", "m"), variable("x", "y", "2", "m")));
        assertFalse(DocumentValidator.validate(duplicate).isValid());
        assertTrue(DocumentValidator.validate(duplicate).diagnostics().stream()
                .anyMatch(issue -> issue.code() == DocumentDiagnosticCode.DUPLICATE_VARIABLE_ID));
    }

    @Test void transferRealBlocksRemapsTravelingVariableAndRejectsUnsafePasteAtomically() {
        var variable = variable("mass-id", "m", "2", "kg");
        var source = new Document(List.of(variable));
        var result = new ComputedResult(new ExpressionParser().parse("m*2", source).expression(), "m*2");
        source = new Document(List.of(variable, result));
        var extracted = assertInstanceOf(ExtractionResult.Success.class, new FragmentExtractor().extract(source,
                new FragmentExtractionRequest.Blocks(List.of(0, 1))));
        var destination = new Document(List.of(variable));
        var planned = assertInstanceOf(PlanningResult.Success.class, new TransferPlanner().plan(extracted.fragment(),
                new TransferContext(destination, Optional.empty(), extracted.sourceMetadata()))).plan();
        var materialized = assertInstanceOf(MaterializationResult.Success.class,
                new TransferMaterializer().materialize(extracted.fragment(), planned));
        var roots = ((FragmentContent.Blocks) materialized.transfer().content()).roots();
        assertEquals("mass-id-2", ((VariableDefinition) roots.getFirst()).id());
        assertEquals("mass-id-2", ((ComputedResult) roots.get(1)).variableDependencies().getFirst().variableId());

        var unsafe = assertInstanceOf(ExtractionResult.Success.class, new FragmentExtractor().extract(source,
                new FragmentExtractionRequest.Blocks(List.of(1))));
        var session = new EditorSession(new Document(List.of(paragraph())), 0);
        var before = session.current();
        assertFalse(session.pasteFromClipboard(Optional.of(new DocumentFragmentClipboardPayload(
                unsafe.fragment(), unsafe.sourceMetadata())), "m*2"));
        assertSame(before, session.current());
        assertEquals(0, session.undoDepth());
        assertEquals(0, session.redoDepth());
        assertEquals(TransferDiagnostic.Code.UNRESOLVED_EXTERNAL_VARIABLE_DEPENDENCY,
                session.transferDiagnostics().getFirst().code());
    }

    @Test void computationIsAtomicHitTargetAndExportsWithoutIds() {
        var variable = variable("private-id", "room", "20", "°C");
        var source = new Document(List.of(variable));
        var result = new ComputedResult(new ExpressionParser().parse("room+Δ5 °C", source).expression(), "room+Δ5 °C");
        var document = new Document(List.of(variable, result));
        var plain = new DocumentPlainTextSerializer().serialize(document);
        assertTrue(plain.contains("room = 20 °C"), plain);
        assertTrue(plain.contains("25 °C"), plain);
        assertFalse(plain.contains("private-id"));
        assertTrue(new dev.rgcb.scholar.markdown.MarkdownSerializer().serialize(document).contains("25 °C"));
        var layout = new DocumentLayoutEngine().layout(document, 300, new FixedTextMeasurer());
        assertEquals(LaidOutBlockKind.COMPUTATION, layout.blocks().getFirst().kind());
        assertEquals(DocumentHit.Kind.BLOCK, new DocumentHitTester().hit(layout, 5, layout.blocks().getFirst().y() + 1,
                new FixedTextMeasurer()).kind());
        var pages = new DocumentLayoutEngine().layoutPaginated(document, new FixedTextMeasurer(), null);
        assertEquals(LaidOutBlockKind.COMPUTATION, pages.blocks().getFirst().kind());
    }

    @Test void forwardReferencesResolveIndependentOfBlockOrderAndDuplicateNamesDiagnose() {
        var definitions = new Document(List.of(variable("x-id", "x", "5", "m")));
        var computed = new ComputedResult(new ExpressionParser().parse("x+1 m", definitions).expression(), "x+1 m");
        var forward = new Document(List.of(computed, definitions.blocks().getFirst()));
        assertEquals("6", quantity(new ComputationEngine().update(forward).results().get(0)).value().toPlainString());
        var duplicate = new Document(List.of(computed, definitions.blocks().getFirst(), variable("other", "x", "1", "m")));
        assertEquals("6", quantity(new ComputationEngine().update(duplicate).results().get(0)).value().toPlainString());
        assertEquals(ComputationDiagnostic.Code.AMBIGUOUS_VARIABLE,
                new ExpressionParser().parse("x+1 m", duplicate).diagnostics().getFirst().code());
    }

    @Test void insertingDefinitionAfterResultBindsPreviouslyUnknownNameInSameTransaction() {
        var initial = new Document(List.of());
        var unresolved = new ComputedResult(new ExpressionParser().parse("x+1 m", initial).expression(), "x+1 m");
        var session = new EditorSession(new Document(List.of(unresolved, paragraph())), 1);
        assertTrue(session.insertVariable("x", new ScientificValue.Physical(new Quantity("5", UnitExpression.of("metre"))), Optional.empty()));
        var computed = (ComputedResult) session.current().document().blocks().stream()
                .filter(ComputedResult.class::isInstance).findFirst().orElseThrow();
        var variable = (VariableDefinition) session.current().document().blocks().stream()
                .filter(VariableDefinition.class::isInstance).findFirst().orElseThrow();
        assertEquals(variable.id(), computed.variableDependencies().getFirst().variableId());
        assertEquals(1, session.undoDepth());
        assertTrue(session.undo());
        var restored = (ComputedResult) session.current().document().blocks().getFirst();
        assertTrue(restored.variableDependencies().isEmpty());
        assertTrue(session.redo());
        assertEquals(variable.id(), ((ComputedResult) session.current().document().blocks().getFirst())
                .variableDependencies().getFirst().variableId());
    }

    @Test void displayUnitAndNotationArePresentationOnlyAndIncompatibleUnitIsRejected() {
        var session = new EditorSession(new Document(List.of(paragraph())), 0);
        assertTrue(session.insertComputedResult("5 m", Optional.empty(), Optional.of(new UnitParser().parseRequired("cm")),
                NumberNotation.ENGINEERING));
        var index = session.current().blockSelection().blockIndex();
        var computed = (ComputedResult) session.current().document().blocks().get(index);
        var semantic = session.computations().results().get(index).value().orElseThrow();
        var displayed = new ComputationFormatter().result(computed, session.computations().results().get(index),
                session.current().document(), false);
        assertTrue(displayed.contains("500 cm"), displayed);
        assertEquals("5", ((ScientificValue.Physical) semantic).value().nominal().value().toPlainString());
        var before = session.current();
        assertFalse(session.editComputedResult("5 m", Optional.empty(), Optional.of(new UnitParser().parseRequired("s")),
                NumberNotation.SCIENTIFIC));
        assertSame(before, session.current());
    }

    @Test void insertActionsOpenProductionDialogsWithoutMutatingDocument() {
        var session = new EditorSession(new Document(List.of(paragraph())), 0);
        var action = BuiltInEditorActions.insertMenuActions().stream()
                .filter(candidate -> candidate.id() == EditorActionId.INSERT_VARIABLE).findFirst().orElseThrow();
        var context = new EditorActionContext(session, new ClipboardAdapter() {
            @Override public String getText() { return ""; }
            @Override public boolean setText(String text) { return true; }
        });
        assertTrue(action.isEnabled(context));
        assertEquals(Optional.of(ComputationDialogKind.INSERT_VARIABLE), action.execute(context).computationDialog());
        assertEquals(0, session.undoDepth());
    }

    @Test void variableBlockCopiesCutsPastesAndUndoesAsOneSemanticTransaction() {
        var original = variable("variable-a", "a", "2", "m");
        var session = new EditorSession(new Document(List.of(original, paragraph())), 1);
        session.setCurrent(new EditorState(session.current().document(), new BlockSelection(0), Optional.empty()));
        var copy = session.copyForClipboard().orElseThrow();
        assertEquals("a = 2 m", copy.plainText());
        var cut = session.cutForClipboard().orElseThrow();
        assertEquals(0, session.undoDepth());
        assertTrue(session.applyCut(cut));
        assertEquals(1, session.undoDepth());
        assertTrue(session.undo());
        assertEquals(original, session.current().document().blocks().getFirst());
        assertTrue(session.redo());
        assertFalse(session.current().document().blocks().contains(original));
        assertTrue(session.pasteFromClipboard(copy.payload(), copy.plainText()));
        assertTrue(session.current().document().blocks().stream().anyMatch(VariableDefinition.class::isInstance));
    }

    @Test void resultOnlyPasteWithinProvenSameDocumentKeepsStableDependency() {
        var variable = variable("original-id", "x", "4", "m");
        var result = new ComputedResult(new ExpressionParser().parse("x*2", new Document(List.of(variable))).expression(), "x*2");
        var session = new EditorSession(new Document(List.of(variable, result, paragraph())), 2);
        session.setCurrent(new EditorState(session.current().document(), new BlockSelection(1), Optional.empty()));
        var copy = session.copyForClipboard().orElseThrow();
        session.setCurrent(new EditorState(session.current().document(), new DocumentPosition(2, 0)));
        assertTrue(session.pasteFromClipboard(copy.payload(), copy.plainText()));
        var inserted = (ComputedResult) session.current().document().blocks().get(session.current().blockSelection().blockIndex());
        assertEquals("original-id", inserted.variableDependencies().getFirst().variableId());
        assertTrue(session.computations().results().values().stream().allMatch(value -> value.value().isPresent()));
    }

    @Test void ordinaryEquationRemainsNonComputationalAndDiagnosticsStayScoped() {
        var equation = new EquationBlock(new dev.rgcb.scholar.math.MathSequence(List.of(
                new dev.rgcb.scholar.math.MathIdentifier("E"))));
        var invalid = new ComputedResult(new ExpressionParser().parse("5 m + 3 s", new Document(List.of())).expression(),
                "5 m + 3 s");
        var document = new Document(List.of(equation, invalid));
        var snapshot = new ComputationEngine().update(document);
        assertEquals(Set.of(1), snapshot.results().keySet());
        assertEquals(ComputationDiagnostic.Code.INCOMPATIBLE_DIMENSIONS,
                snapshot.results().get(1).diagnostics().getFirst().code());
        assertTrue(DocumentValidator.validate(document).isValid());
    }

    private static Quantity quantity(ComputationResult result) {
        assertTrue(result.diagnostics().isEmpty(), result.diagnostics().toString());
        return ((ScientificValue.Physical) result.value().orElseThrow()).value().nominal();
    }

    private static final class FixedTextMeasurer implements TextMeasurer {
        @Override public int measureWidth(String text, TextStyle style) { return text.length() * 6; }
        @Override public int lineHeight(TextStyle style) { return 12; }
    }
}
