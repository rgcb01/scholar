package dev.rgcb.scholar.math.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.rgcb.scholar.math.MathFraction;
import dev.rgcb.scholar.math.MathDelimiter;
import dev.rgcb.scholar.math.MathGroup;
import dev.rgcb.scholar.math.MathIdentifier;
import dev.rgcb.scholar.math.MathNamedOperator;
import dev.rgcb.scholar.math.MathNumber;
import dev.rgcb.scholar.math.MathOperator;
import dev.rgcb.scholar.math.MathOperatorRole;
import dev.rgcb.scholar.math.MathRoot;
import dev.rgcb.scholar.math.MathScript;
import dev.rgcb.scholar.math.MathSequence;
import dev.rgcb.scholar.math.MathSymbol;
import dev.rgcb.scholar.math.MathSymbolKind;
import dev.rgcb.scholar.math.MathText;
import dev.rgcb.scholar.math.editor.FractionNumerator;
import dev.rgcb.scholar.math.editor.GroupContent;
import dev.rgcb.scholar.math.editor.MathPath;
import dev.rgcb.scholar.math.editor.RootIndex;
import dev.rgcb.scholar.math.editor.RootRadicand;
import dev.rgcb.scholar.math.editor.ScriptBase;
import dev.rgcb.scholar.math.editor.ScriptSubscript;
import dev.rgcb.scholar.math.editor.ScriptSuperscript;
import dev.rgcb.scholar.math.editor.SequenceChild;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MathLayoutEngineTest {
    private final MathLayoutEngine layoutEngine = new MathLayoutEngine();
    private final MathTextMeasurer measurer = new FixedMathTextMeasurer();

    @Test
    void laysOutIdentifierSymbolAndNumberUsingMeasuredMetrics() {
        assertGlyph(layoutEngine.layout(new MathIdentifier("velocity"), measurer), "velocity", MathTextKind.IDENTIFIER, 40);
        assertGlyph(layoutEngine.layout(new MathSymbol("Δ", MathSymbolKind.GREEK), measurer), "Δ", MathTextKind.SYMBOL, 5);
        assertGlyph(layoutEngine.layout(new MathNumber("42"), measurer), "42", MathTextKind.NUMBER, 10);
    }

    @Test
    void laysOutNamedOperatorAndMathTextWithSemanticTextKinds() {
        assertGlyph(layoutEngine.layout(new MathNamedOperator("sin"), measurer), "sin", MathTextKind.NAMED_OPERATOR, 15);
        assertGlyph(layoutEngine.layout(new MathText("if"), measurer), "if", MathTextKind.TEXT, 10);
        assertGlyph(layoutEngine.layout(new MathText("for all"), measurer), "for all", MathTextKind.TEXT, 35);
    }

    @Test
    void sourcePathsRemainMappedForSemanticTokenKinds() {
        var layout = layoutEngine.layout(new MathSequence(List.of(
                new MathNamedOperator("sin"),
                new MathIdentifier("x"),
                new MathText("if"))), measurer);

        assertEquals(Optional.of(MathPath.ROOT.append(new SequenceChild(0))), layout.root().children().get(0).box().sourcePath());
        assertEquals(Optional.of(MathPath.ROOT.append(new SequenceChild(1))), layout.root().children().get(1).box().sourcePath());
        assertEquals(Optional.of(MathPath.ROOT.append(new SequenceChild(2))), layout.root().children().get(2).box().sourcePath());
    }

    @Test
    void laysOutSequenceWithBaselineAlignedChildren() {
        var layout = layoutEngine.layout(new MathSequence(List.of(
                new MathIdentifier("x"),
                new MathIdentifier("y"))), measurer);

        assertEquals(10, layout.width());
        assertEquals(7, layout.root().ascent());
        assertEquals(3, layout.root().descent());
        assertEquals(0, layout.root().children().get(0).baselineOffset());
        assertEquals(0, layout.root().children().get(1).baselineOffset());
    }

    @Test
    void completeGroupKeepsRelationSpacingInComplexExpression() {
        var group = new MathGroup(new MathFraction(
                new MathSequence(List.of(new MathIdentifier("x"), new MathOperator("+", MathOperatorRole.BINARY), new MathNumber("1"))),
                new MathRoot(new MathIdentifier("z"), Optional.of(new MathNumber("3")))), MathDelimiter.PARENTHESES);
        var layout = layoutEngine.layout(new MathSequence(List.of(
                new MathRoot(group, Optional.empty()),
                new MathOperator("=", MathOperatorRole.RELATION),
                new MathScript(new MathIdentifier("r"), Optional.empty(), Optional.of(new MathNumber("2"))))), measurer);

        var root = layout.root();
        var left = root.children().get(0);
        var relation = root.children().get(1);
        assertEquals(left.x() + left.box().width() + 6, relation.x());
    }

    @Test
    void preservesCompactOrdinaryIdentifierAndNumberAdjacency() {
        var identifiers = layoutEngine.layout(new MathSequence(List.of(
                new MathIdentifier("x"),
                new MathIdentifier("y"))), measurer);
        var numberIdentifier = layoutEngine.layout(new MathSequence(List.of(
                new MathNumber("2"),
                new MathIdentifier("x"))), measurer);

        assertEquals(5, identifiers.root().children().get(1).x());
        assertEquals(5, numberIdentifier.root().children().get(1).x());
    }

    @Test
    void keepsNamedOperatorCompactBeforeOpeningDelimiter() {
        var layout = layoutEngine.layout(new MathSequence(List.of(
                new MathNamedOperator("sin"),
                new MathSymbol("(", MathSymbolKind.OTHER),
                new MathIdentifier("x"),
                new MathSymbol(")", MathSymbolKind.OTHER))), measurer);

        assertEquals(15, layout.root().children().get(1).x());
        assertEquals(20, layout.root().children().get(2).x());
        assertEquals(25, layout.root().children().get(3).x());
    }

    @Test
    void separatesNamedOperatorFromBareArgumentAndPrecedingAtoms() {
        var bareArgument = layoutEngine.layout(new MathSequence(List.of(
                new MathNamedOperator("sin"),
                new MathIdentifier("x"))), measurer);
        var preceded = layoutEngine.layout(new MathSequence(List.of(
                new MathNumber("2"),
                new MathNamedOperator("sin"),
                new MathSymbol("(", MathSymbolKind.OTHER),
                new MathIdentifier("x"),
                new MathSymbol(")", MathSymbolKind.OTHER))), measurer);

        assertEquals(17, bareArgument.root().children().get(1).x());
        assertEquals(7, preceded.root().children().get(1).x());
        assertEquals(22, preceded.root().children().get(2).x());
    }

    @Test
    void separatesMathTextFromNeighboringMathAtoms() {
        var layout = layoutEngine.layout(new MathSequence(List.of(
                new MathNumber("0"),
                new MathText("if"),
                new MathIdentifier("x"))), measurer);

        assertEquals(10, layout.root().children().get(1).x());
        assertEquals(25, layout.root().children().get(2).x());
    }

    @Test
    void spacesFullSemanticTextRegressionExampleWithoutMutatingAst() {
        var expression = new MathSequence(List.of(
                new MathNamedOperator("sin"),
                new MathSymbol("(", MathSymbolKind.OTHER),
                new MathIdentifier("x"),
                new MathSymbol(")", MathSymbolKind.OTHER),
                new MathOperator("=", MathOperatorRole.RELATION),
                new MathNumber("0"),
                new MathText("if"),
                new MathIdentifier("x"),
                new MathOperator("=", MathOperatorRole.RELATION),
                new MathNumber("0")));

        var layout = layoutEngine.layout(expression, measurer);

        assertEquals(15, layout.root().children().get(1).x());
        assertEquals(57, layout.root().children().get(6).x());
        assertEquals(72, layout.root().children().get(7).x());
        assertEquals(new MathSequence(List.of(
                new MathNamedOperator("sin"),
                new MathSymbol("(", MathSymbolKind.OTHER),
                new MathIdentifier("x"),
                new MathSymbol(")", MathSymbolKind.OTHER),
                new MathOperator("=", MathOperatorRole.RELATION),
                new MathNumber("0"),
                new MathText("if"),
                new MathIdentifier("x"),
                new MathOperator("=", MathOperatorRole.RELATION),
                new MathNumber("0"))), expression);
    }

    @Test
    void appliesSemanticSpacingInsideNestedFractionSequences() {
        var layout = layoutEngine.layout(new MathFraction(
                new MathSequence(List.of(new MathIdentifier("x"), new MathText("if"), new MathIdentifier("y"))),
                new MathIdentifier("z")), measurer);
        var numerator = layout.root().children().get(0).box();

        assertEquals(10, numerator.children().get(1).x());
        assertEquals(25, numerator.children().get(2).x());
    }

    @Test
    void addsSpacingAroundRelationOperator() {
        var layout = layoutEngine.layout(new MathSequence(List.of(
                new MathIdentifier("v"),
                new MathOperator("=", MathOperatorRole.RELATION),
                new MathIdentifier("d"))), measurer);

        assertEquals(27, layout.width());
        assertEquals(0, layout.root().children().get(0).x());
        assertEquals(11, layout.root().children().get(1).x());
        assertEquals(22, layout.root().children().get(2).x());
    }

    @Test
    void laysOutFractionWithCenteredNumeratorAndDenominator() {
        var layout = layoutEngine.layout(new MathFraction(new MathIdentifier("x"), new MathIdentifier("time")), measurer);
        var box = layout.root();

        assertEquals(28, box.width());
        assertEquals(13, box.ascent());
        assertEquals(14, box.descent());
        assertEquals(11, box.children().get(0).x());
        assertEquals(4, box.children().get(1).x());

        var rule = assertInstanceOf(MathHorizontalRule.class, box.primitives().get(0));
        assertEquals(0, rule.x());
        assertEquals(0, rule.y());
        assertEquals(28, rule.width());
        assertEquals(1, rule.thickness());
    }

    @Test
    void nestedFractionsIncreaseDimensionsNaturally() {
        var inner = new MathFraction(new MathIdentifier("a"), new MathIdentifier("b"));
        var outer = new MathFraction(inner, new MathIdentifier("c"));

        var innerLayout = layoutEngine.layout(inner, measurer);
        var outerLayout = layoutEngine.layout(outer, measurer);

        assertEquals(13, innerLayout.width());
        assertEquals(21, outerLayout.width());
        assertEquals(30, outerLayout.root().ascent());
        assertEquals(14, outerLayout.root().descent());
    }

    @Test
    void emptyFractionHasUsableStructuralGeometry() {
        var layout = layoutEngine.layout(new MathFraction(new MathSequence(List.of()), new MathSequence(List.of())), measurer);

        assertEquals(8, layout.width());
        assertEquals(3, layout.root().ascent());
        assertEquals(4, layout.root().descent());
        var rule = assertInstanceOf(MathHorizontalRule.class, layout.root().primitives().get(0));
        assertEquals(8, rule.width());
        assertEquals(1, rule.thickness());
    }

    @Test
    void laysOutSquareRootWithRadicandSourcePathAndVinculum() {
        var layout = layoutEngine.layout(new MathRoot(new MathIdentifier("x"), Optional.empty()), measurer);
        var box = layout.root();

        assertEquals(17, box.width());
        assertEquals(11, box.ascent());
        assertEquals(3, box.descent());
        assertEquals(Optional.of(MathPath.ROOT.append(new RootRadicand())), box.children().get(0).box().sourcePath());
        assertEquals(9, box.children().get(0).x());

        var radicalHook = assertInstanceOf(MathLineSegment.class, box.primitives().get(0));
        var radicalStem = assertInstanceOf(MathLineSegment.class, box.primitives().get(1));
        var rule = assertInstanceOf(MathHorizontalRule.class, box.primitives().get(2));
        assertEquals(0, radicalHook.x1());
        assertEquals(0, radicalHook.y1());
        assertEquals(2, radicalHook.x2());
        assertEquals(3, radicalHook.y2());
        assertEquals(2, radicalStem.x1());
        assertEquals(3, radicalStem.y1());
        assertEquals(6, radicalStem.x2());
        assertEquals(-11, radicalStem.y2());
        assertEquals(6, rule.x());
        assertEquals(-11, rule.y());
        assertEquals(11, rule.width());
    }

    @Test
    void indexedRootPlacesReducedIndexToUpperLeftOfRadical() {
        var layout = layoutEngine.layout(new MathRoot(new MathIdentifier("x"), Optional.of(new MathNumber("3"))), measurer);
        var box = layout.root();

        assertEquals(17, box.width());
        assertEquals(11, box.ascent());
        assertEquals(Optional.of(MathPath.ROOT.append(new RootIndex())), box.children().get(0).box().sourcePath());
        assertEquals(Optional.of(MathPath.ROOT.append(new RootRadicand())), box.children().get(1).box().sourcePath());
        assertEquals(0, box.children().get(0).x());
        assertEquals(-6, box.children().get(0).baselineOffset());
        assertEquals(9, box.children().get(1).x());
        assertEquals(0.6, assertInstanceOf(MathGlyphRun.class, box.children().get(0).box().primitives().get(0)).scale());
    }

    @Test
    void emptyRootRadicandHasMinimumEditableGeometry() {
        var layout = layoutEngine.layout(new MathRoot(new MathSequence(List.of()), Optional.empty()), measurer);
        var radicand = layout.root().children().get(0).box();

        assertEquals(8, radicand.width());
        assertEquals(7, radicand.ascent());
        assertEquals(3, radicand.descent());
        assertEquals(Optional.of(MathPath.ROOT.append(new RootRadicand())), radicand.sourcePath());
    }

    @Test
    void nestedRootsAndRootFractionsLayoutWithoutLosingPaths() {
        var nested = layoutEngine.layout(new MathRoot(new MathRoot(new MathIdentifier("x"), Optional.empty()), Optional.empty()), measurer);
        var fractionInsideRoot = layoutEngine.layout(new MathRoot(new MathFraction(new MathIdentifier("a"), new MathIdentifier("b")), Optional.empty()), measurer);

        assertEquals(Optional.of(MathPath.ROOT.append(new RootRadicand())), nested.root().children().get(0).box().sourcePath());
        assertEquals(Optional.of(MathPath.ROOT.append(new RootRadicand()).append(new RootRadicand())),
                nested.root().children().get(0).box().children().get(0).box().sourcePath());
        assertEquals(Optional.of(MathPath.ROOT.append(new RootRadicand())),
                fractionInsideRoot.root().children().get(0).box().sourcePath());
    }

    @Test
    void nestedRootRadicalStemConnectsToOuterVinculum() {
        var layout = layoutEngine.layout(new MathRoot(new MathRoot(new MathIdentifier("x"), Optional.empty()), Optional.empty()), measurer);
        var outerStem = assertInstanceOf(MathLineSegment.class, layout.root().primitives().get(1));
        var outerRule = assertInstanceOf(MathHorizontalRule.class, layout.root().primitives().get(2));

        assertEquals(outerRule.x(), outerStem.x2());
        assertEquals(outerRule.y(), outerStem.y2());
    }

    @Test
    void laysOutGroupsWithDerivedDelimiterGlyphsAndContentPath() {
        var parentheses = layoutEngine.layout(new MathGroup(new MathSequence(List.of()), MathDelimiter.PARENTHESES), measurer).root();
        var brackets = layoutEngine.layout(new MathGroup(new MathIdentifier("x"), MathDelimiter.BRACKETS), measurer).root();
        var braces = layoutEngine.layout(new MathGroup(new MathIdentifier("x"), MathDelimiter.BRACES), measurer).root();

        assertEquals(18, parentheses.width());
        assertEquals(7, parentheses.ascent());
        assertEquals(3, parentheses.descent());
        assertEquals(Optional.of(MathPath.ROOT), parentheses.children().get(0).box().sourcePath());
        assertEquals(Optional.of(MathPath.ROOT.append(new GroupContent())), parentheses.children().get(1).box().sourcePath());
        assertEquals(Optional.of(MathPath.ROOT), parentheses.children().get(2).box().sourcePath());
        assertEquals("(", assertInstanceOf(MathGlyphRun.class, parentheses.children().get(0).box().primitives().get(0)).content());
        assertEquals(")", assertInstanceOf(MathGlyphRun.class, parentheses.children().get(2).box().primitives().get(0)).content());
        assertEquals("[", assertInstanceOf(MathGlyphRun.class, brackets.children().get(0).box().primitives().get(0)).content());
        assertEquals("]", assertInstanceOf(MathGlyphRun.class, brackets.children().get(2).box().primitives().get(0)).content());
        assertEquals("{", assertInstanceOf(MathGlyphRun.class, braces.children().get(0).box().primitives().get(0)).content());
        assertEquals("}", assertInstanceOf(MathGlyphRun.class, braces.children().get(2).box().primitives().get(0)).content());
    }

    @Test
    void groupLayoutComposesWithNestedRootFractionAndScriptPaths() {
        var layout = layoutEngine.layout(new MathGroup(new MathSequence(List.of(
                new MathRoot(new MathGroup(new MathIdentifier("x"), MathDelimiter.PARENTHESES), Optional.empty()),
                new MathFraction(new MathGroup(new MathIdentifier("a"), MathDelimiter.BRACKETS), new MathIdentifier("b")),
                new MathScript(new MathGroup(new MathIdentifier("y"), MathDelimiter.BRACES), Optional.empty(), Optional.of(new MathNumber("2"))))), MathDelimiter.PARENTHESES), measurer);
        var content = layout.root().children().get(1).box();

        assertEquals(Optional.of(MathPath.ROOT.append(new GroupContent())), content.sourcePath());
        assertEquals(Optional.of(MathPath.ROOT.append(new GroupContent()).append(new SequenceChild(0)).append(new RootRadicand())),
                content.children().get(0).box().children().get(0).box().sourcePath());
        assertEquals(Optional.of(MathPath.ROOT.append(new GroupContent()).append(new SequenceChild(1)).append(new FractionNumerator())),
                content.children().get(1).box().children().get(0).box().sourcePath());
        assertEquals(Optional.of(MathPath.ROOT.append(new GroupContent()).append(new SequenceChild(2)).append(new ScriptBase())),
                content.children().get(2).box().children().get(0).box().sourcePath());
    }

    @Test
    void targetEquationHasExpectedWidthAndFractionPosition() {
        var layout = layoutEngine.layout(targetEquation(), measurer);

        assertEquals(40, layout.width());
        assertEquals(13, layout.root().ascent());
        assertEquals(14, layout.root().descent());
        assertEquals(22, layout.root().children().get(2).x());
    }

    @Test
    void layoutIsDeterministicForSameExpressionAndMetrics() {
        var expression = targetEquation();

        assertEquals(layoutEngine.layout(expression, measurer), layoutEngine.layout(expression, measurer));
    }

    @Test
    void laysOutSuperscriptAndSubscriptWithSharedScriptXOrigin() {
        var layout = layoutEngine.layout(new MathScript(
                new MathIdentifier("x"),
                Optional.of(new MathIdentifier("i")),
                Optional.of(new MathNumber("2"))), measurer);
        var box = layout.root();

        assertEquals(10, box.width());
        assertEquals(12, box.ascent());
        assertEquals(7, box.descent());
        assertEquals(Optional.of(MathPath.ROOT.append(new ScriptBase())), box.children().get(0).box().sourcePath());
        assertEquals(Optional.of(MathPath.ROOT.append(new ScriptSubscript())), box.children().get(1).box().sourcePath());
        assertEquals(Optional.of(MathPath.ROOT.append(new ScriptSuperscript())), box.children().get(2).box().sourcePath());
        assertEquals(6, box.children().get(1).x());
        assertEquals(6, box.children().get(2).x());
        assertEquals(5, box.children().get(1).baselineOffset());
        assertEquals(-7, box.children().get(2).baselineOffset());
        assertEquals(0.75, assertInstanceOf(MathGlyphRun.class, box.children().get(1).box().primitives().get(0)).scale());
        assertEquals(0.75, assertInstanceOf(MathGlyphRun.class, box.children().get(2).box().primitives().get(0)).scale());
    }

    @Test
    void compactStructuralSuperscriptContentMoreThanSimpleScriptText() {
        var script = layoutEngine.layout(new MathScript(
                new MathIdentifier("x"),
                Optional.empty(),
                Optional.of(new MathFraction(new MathNumber("1"), new MathNumber("2")))), measurer).root();

        var superscript = script.children().get(1).box();
        var numeratorGlyph = assertInstanceOf(
                MathGlyphRun.class,
                superscript.children().get(0).box().primitives().get(0));
        var denominatorGlyph = assertInstanceOf(
                MathGlyphRun.class,
                superscript.children().get(1).box().primitives().get(0));

        assertEquals(8, superscript.width());
        assertEquals(-7, script.children().get(1).baselineOffset());
        assertEquals(0.6, numeratorGlyph.scale());
        assertEquals(0.6, denominatorGlyph.scale());
    }

    private static void assertGlyph(LaidOutMath layout, String content, MathTextKind kind, int width) {
        assertEquals(width, layout.width());
        assertEquals(7, layout.root().ascent());
        assertEquals(3, layout.root().descent());
        var glyph = assertInstanceOf(MathGlyphRun.class, layout.root().primitives().get(0));
        assertEquals(content, glyph.content());
        assertEquals(kind, glyph.kind());
    }

    private static MathSequence targetEquation() {
        return new MathSequence(List.of(
                new MathIdentifier("v"),
                new MathOperator("=", MathOperatorRole.RELATION),
                new MathFraction(
                        new MathSequence(List.of(new MathSymbol("Δ", MathSymbolKind.GREEK), new MathIdentifier("x"))),
                        new MathSequence(List.of(new MathSymbol("Δ", MathSymbolKind.GREEK), new MathIdentifier("t"))))));
    }

    private static final class FixedMathTextMeasurer implements MathTextMeasurer {
        @Override
        public MathTextMetrics measureText(String content, MathTextKind kind) {
            return new MathTextMetrics(content.length() * 5, 7, 3);
        }
    }
}
