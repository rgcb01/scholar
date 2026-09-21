package dev.rgcb.scholar.math.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.rgcb.scholar.math.MathFraction;
import dev.rgcb.scholar.math.MathDelimiter;
import dev.rgcb.scholar.math.MathGroup;
import dev.rgcb.scholar.math.MathIdentifier;
import dev.rgcb.scholar.math.MathNumber;
import dev.rgcb.scholar.math.MathSequence;
import dev.rgcb.scholar.math.layout.MathLayoutEngine;
import dev.rgcb.scholar.math.layout.MathTextKind;
import dev.rgcb.scholar.math.layout.MathTextMeasurer;
import dev.rgcb.scholar.math.layout.MathTextMetrics;
import java.util.List;
import org.junit.jupiter.api.Test;

class MathCaretAndHitTestTest {
    private final MathTextMeasurer measurer = new FixedMathTextMeasurer();
    private final MathLayoutEngine layoutEngine = new MathLayoutEngine();
    private final MathCaretGeometryResolver caretResolver = new MathCaretGeometryResolver();
    private final MathHitTester hitTester = new MathHitTester();

    @Test
    void sourceMappingResolvesSequenceAndTokenCaretGeometry() {
        var math = layoutEngine.layout(new MathSequence(List.of(new MathIdentifier("v"), new MathNumber("12"))), measurer);
        var before = caretResolver.resolve(new MathSequencePosition(MathPath.ROOT, 0), math, measurer);
        var insideNumber = caretResolver.resolve(new MathTokenPosition(MathPath.ROOT.append(new SequenceChild(1)), 1), math, measurer);
        var after = caretResolver.resolve(new MathSequencePosition(MathPath.ROOT, 2), math, measurer);

        assertEquals(0, before.x());
        assertEquals(10, insideNumber.x());
        assertEquals(15, after.x());
    }

    @Test
    void virtualRootTokenGeometryWorksWithoutChangingLayoutShape() {
        var math = layoutEngine.layout(new MathNumber("12"), measurer);

        assertEquals(0, caretResolver.resolve(new MathSequencePosition(MathPath.ROOT, 0), math, measurer).x());
        assertEquals(5, caretResolver.resolve(new MathTokenPosition(MathPath.ROOT.append(new SequenceChild(0)), 1), math, measurer).x());
        assertEquals(10, caretResolver.resolve(new MathSequencePosition(MathPath.ROOT, 1), math, measurer).x());
    }

    @Test
    void hitTestingReturnsCanonicalTokenAndSequencePositions() {
        var math = layoutEngine.layout(new MathNumber("123"), measurer);

        assertEquals(new MathSequencePosition(MathPath.ROOT, 0), hitTester.hit(math, 0, 0, measurer));
        assertEquals(new MathTokenPosition(MathPath.ROOT.append(new SequenceChild(0)), 1), hitTester.hit(math, 5, 0, measurer));
        assertEquals(new MathTokenPosition(MathPath.ROOT.append(new SequenceChild(0)), 2), hitTester.hit(math, 10, 0, measurer));
        assertEquals(new MathSequencePosition(MathPath.ROOT, 1), hitTester.hit(math, 15, 0, measurer));
    }

    @Test
    void fractionSlotGeometryAndHitTestingUseSlotPaths() {
        var fractionPath = MathPath.ROOT.append(new SequenceChild(0));
        var numeratorPath = fractionPath.append(new FractionNumerator());
        var denominatorPath = fractionPath.append(new FractionDenominator());
        var math = layoutEngine.layout(new MathSequence(List.of(new MathFraction(new MathIdentifier("x"), new MathIdentifier("y")))), measurer);

        var numerator = caretResolver.resolve(new MathSequencePosition(numeratorPath, 0), math, measurer);
        var denominator = caretResolver.resolve(new MathSequencePosition(denominatorPath, 0), math, measurer);

        assertEquals(new MathSequencePosition(numeratorPath, 0), hitTester.hit(math, numerator.x(), numerator.baselineY(), measurer));
        assertEquals(new MathSequencePosition(denominatorPath, 0), hitTester.hit(math, denominator.x(), denominator.baselineY(), measurer));
    }

    @Test
    void newlyInsertedEmptyFractionSupportsCaretGeometryAndParentHitTesting() {
        var fractionPath = MathPath.ROOT.append(new SequenceChild(0));
        var numeratorPath = fractionPath.append(new FractionNumerator());
        var denominatorPath = fractionPath.append(new FractionDenominator());
        var expression = new MathSequence(List.of(new MathFraction(new MathSequence(List.of()), new MathSequence(List.of()))));
        var math = layoutEngine.layout(expression, measurer);

        var numerator = caretResolver.resolve(new MathSequencePosition(numeratorPath, 0), math, measurer);
        var denominator = caretResolver.resolve(new MathSequencePosition(denominatorPath, 0), math, measurer);

        assertEquals(new MathSequencePosition(numeratorPath, 0), hitTester.hit(math, numerator.x(), numerator.baselineY(), measurer));
        assertEquals(new MathSequencePosition(denominatorPath, 0), hitTester.hit(math, denominator.x(), denominator.baselineY(), measurer));
        assertEquals(new MathSequencePosition(MathPath.ROOT, 0), hitTester.hit(math, -1, 0, measurer));
        assertEquals(new MathSequencePosition(MathPath.ROOT, 1), hitTester.hit(math, math.width() + 1, 0, measurer));
    }

    @Test
    void nestedStructuralNodeInsideVirtualGroupSequenceResolvesEveryCaretPosition() {
        var expression = new MathSequence(List.of(new MathGroup(
                new MathFraction(new MathIdentifier("x"), new MathIdentifier("y")),
                MathDelimiter.PARENTHESES)));
        var math = layoutEngine.layout(expression, measurer);

        for (var position : new MathExpressionEditor().positions(expression)) {
            caretResolver.resolve(position, math, measurer);
        }
    }

    private static final class FixedMathTextMeasurer implements MathTextMeasurer {
        @Override
        public MathTextMetrics measureText(String content, MathTextKind kind) {
            return new MathTextMetrics(content.length() * 5, 7, 3);
        }
    }
}
