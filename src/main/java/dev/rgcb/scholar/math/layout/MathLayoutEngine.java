package dev.rgcb.scholar.math.layout;

import dev.rgcb.scholar.math.MathFraction;
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
import dev.rgcb.scholar.math.MathText;
import dev.rgcb.scholar.math.MathExpression;
import dev.rgcb.scholar.math.MathQuantity;
import dev.rgcb.scholar.quantity.ScientificNumberFormatter;
import dev.rgcb.scholar.math.editor.FractionDenominator;
import dev.rgcb.scholar.math.editor.FractionNumerator;
import dev.rgcb.scholar.math.editor.GroupContent;
import dev.rgcb.scholar.math.editor.MathPath;
import dev.rgcb.scholar.math.editor.RootIndex;
import dev.rgcb.scholar.math.editor.RootRadicand;
import dev.rgcb.scholar.math.editor.ScriptBase;
import dev.rgcb.scholar.math.editor.ScriptSubscript;
import dev.rgcb.scholar.math.editor.ScriptSuperscript;
import dev.rgcb.scholar.math.editor.SequenceChild;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class MathLayoutEngine {
    private static final int FRACTION_HORIZONTAL_PADDING = 4;
    private static final int FRACTION_VERTICAL_GAP = 3;
    private static final int ROOT_RADICAL_WIDTH = 7;
    private static final int ROOT_RADICAL_PADDING = 2;
    private static final int ROOT_BAR_GAP = 4;
    private static final int SCRIPT_HORIZONTAL_GAP = 1;
    private static final int SCRIPT_SUPERSCRIPT_RISE = 7;
    private static final int SCRIPT_SUBSCRIPT_DROP = 5;
    private static final int SCRIPT_SCALE_NUMERATOR = 3;
    private static final int SCRIPT_SCALE_DENOMINATOR = 4;
    private static final int STRUCTURAL_SCRIPT_SCALE_NUMERATOR = 3;
    private static final int STRUCTURAL_SCRIPT_SCALE_DENOMINATOR = 5;
    private static final int EMPTY_SLOT_WIDTH = 8;
    private final MathSpacingPolicy spacingPolicy = new MathSpacingPolicy();

    public LaidOutMath layout(MathExpression expression, MathTextMeasurer measurer) {
        Objects.requireNonNull(expression, "expression");
        Objects.requireNonNull(measurer, "measurer");
        return new LaidOutMath(layoutExpression(expression, measurer, MathPath.ROOT));
    }

    private MathBox layoutExpression(MathExpression expression, MathTextMeasurer measurer, MathPath path) {
        if (expression instanceof MathSequence sequence) {
            return layoutSequence(sequence, measurer, path);
        }
        if (expression instanceof MathNumber number) {
            return layoutGlyph(number.content(), MathTextKind.NUMBER, measurer, path);
        }
        if (expression instanceof MathIdentifier identifier) {
            return layoutGlyph(identifier.name(), MathTextKind.IDENTIFIER, measurer, path);
        }
        if (expression instanceof MathNamedOperator namedOperator) {
            return layoutGlyph(namedOperator.name(), MathTextKind.NAMED_OPERATOR, measurer, path);
        }
        if (expression instanceof MathText text) {
            return layoutGlyph(text.content(), MathTextKind.TEXT, measurer, path);
        }
        if (expression instanceof MathQuantity quantity) {
            return layoutGlyph(new ScientificNumberFormatter().format(quantity.value(), quantity.notation(), true),
                    MathTextKind.TEXT, measurer, path);
        }
        if (expression instanceof MathSymbol symbol) {
            return layoutGlyph(symbol.symbol(), MathTextKind.SYMBOL, measurer, path);
        }
        if (expression instanceof MathOperator operator) {
            return layoutGlyph(operator.symbol(), MathTextKind.OPERATOR, measurer, path);
        }
        if (expression instanceof MathFraction fraction) {
            return layoutFraction(fraction, measurer, path);
        }
        if (expression instanceof MathRoot root) {
            return layoutRoot(root, measurer, path);
        }
        if (expression instanceof MathScript script) {
            return layoutScript(script, measurer, path);
        }
        if (expression instanceof MathGroup group) {
            return layoutGroup(group, measurer, path);
        }
        throw new UnsupportedOperationException("Unsupported math expression for layout: " + expression.getClass().getName());
    }

    private MathBox layoutGlyph(String content, MathTextKind kind, MathTextMeasurer measurer, MathPath path) {
        var metrics = measurer.measureText(content, kind);
        return new MathBox(
                metrics.width(),
                metrics.ascent(),
                metrics.descent(),
                List.of(),
                List.of(new MathGlyphRun(content, kind, 0, 0, metrics.ascent())),
                Optional.of(path));
    }

    private MathBox layoutSequence(MathSequence sequence, MathTextMeasurer measurer, MathPath path) {
        var children = new ArrayList<PositionedMathBox>();
        MathExpression previousExpression = null;
        var x = 0;
        var ascent = 0;
        var descent = 0;

        for (var expression : sequence.expressions()) {
            var spacing = previousExpression == null ? 0 : spacingPolicy.spacingBetween(previousExpression, expression, measurer);
            x += spacing;

            var child = layoutExpression(expression, measurer, path.append(new SequenceChild(children.size())));
            children.add(new PositionedMathBox(child, x, 0));
            x += child.width();
            ascent = Math.max(ascent, child.ascent());
            descent = Math.max(descent, child.descent());
            previousExpression = expression;
        }

        return new MathBox(x, ascent, descent, children, List.of(), Optional.of(path));
    }

    private MathBox layoutFraction(MathFraction fraction, MathTextMeasurer measurer, MathPath path) {
        var numerator = layoutExpression(fraction.numerator(), measurer, path.append(new FractionNumerator()));
        var denominator = layoutExpression(fraction.denominator(), measurer, path.append(new FractionDenominator()));
        var thickness = measurer.ruleThickness();
        var width = Math.max(numerator.width(), denominator.width()) + FRACTION_HORIZONTAL_PADDING * 2;
        var numeratorX = (width - numerator.width()) / 2;
        var denominatorX = (width - denominator.width()) / 2;
        var ruleY = 0;
        var numeratorBaselineOffset = ruleY - FRACTION_VERTICAL_GAP - numerator.descent();
        var denominatorBaselineOffset = ruleY + thickness + FRACTION_VERTICAL_GAP + denominator.ascent();
        var ascent = Math.max(-ruleY, -(numeratorBaselineOffset - numerator.ascent()));
        var descent = Math.max(ruleY + thickness, denominatorBaselineOffset + denominator.descent());

        return new MathBox(
                width,
                ascent,
                descent,
                List.of(
                        new PositionedMathBox(numerator, numeratorX, numeratorBaselineOffset),
                        new PositionedMathBox(denominator, denominatorX, denominatorBaselineOffset)),
                List.of(new MathHorizontalRule(0, ruleY, width, thickness)),
                Optional.of(path));
    }

    private MathBox layoutRoot(MathRoot root, MathTextMeasurer measurer, MathPath path) {
        var radicandPath = path.append(new RootRadicand());
        var radicand = ensureEditableSlot(layoutExpression(root.radicand(), measurer, radicandPath), measurer, radicandPath);
        MathBox index = root.index()
                .map(value -> scaleBox(
                        ensureEditableSlot(layoutExpression(value, measurer, path.append(new RootIndex())), measurer, path.append(new RootIndex())),
                        STRUCTURAL_SCRIPT_SCALE_NUMERATOR,
                        STRUCTURAL_SCRIPT_SCALE_DENOMINATOR))
                .orElse(null);
        var radicalX = index == null ? 0 : Math.max(0, index.width() - ROOT_RADICAL_WIDTH / 2);
        var radicandX = radicalX + ROOT_RADICAL_WIDTH + ROOT_RADICAL_PADDING;
        var barY = -radicand.ascent() - ROOT_BAR_GAP;
        var barX = radicalX + ROOT_RADICAL_WIDTH - measurer.ruleThickness();
        var width = radicandX + Math.max(EMPTY_SLOT_WIDTH, radicand.width());
        var ascent = -barY;
        var descent = radicand.descent();
        var children = new ArrayList<PositionedMathBox>();
        if (index != null) {
            var indexBaselineOffset = barY + index.ascent() + 1;
            children.add(new PositionedMathBox(index, 0, indexBaselineOffset));
            ascent = Math.max(ascent, -(indexBaselineOffset - index.ascent()));
        }
        children.add(new PositionedMathBox(radicand, radicandX, 0));

        return new MathBox(
                width,
                ascent,
                descent,
                children,
                List.of(
                        new MathLineSegment(radicalX, 0, radicalX + 2, radicand.descent(), measurer.ruleThickness()),
                        new MathLineSegment(radicalX + 2, radicand.descent(), barX, barY, measurer.ruleThickness()),
                        new MathHorizontalRule(barX, barY, width - barX, measurer.ruleThickness())),
                Optional.of(path));
    }

    private MathBox layoutScript(MathScript script, MathTextMeasurer measurer, MathPath path) {
        var base = layoutExpression(script.base(), measurer, path.append(new ScriptBase()));
        MathBox subscript = script.subscript()
                .map(value -> scaleBox(
                        ensureEditableSlot(layoutExpression(value, measurer, path.append(new ScriptSubscript())), measurer, path.append(new ScriptSubscript())),
                        scriptScaleNumerator(value),
                        scriptScaleDenominator(value)))
                .orElse(null);
        MathBox superscript = script.superscript()
                .map(value -> scaleBox(
                        ensureEditableSlot(layoutExpression(value, measurer, path.append(new ScriptSuperscript())), measurer, path.append(new ScriptSuperscript())),
                        scriptScaleNumerator(value),
                        scriptScaleDenominator(value)))
                .orElse(null);
        var scriptX = base.width() + SCRIPT_HORIZONTAL_GAP;
        var children = new ArrayList<PositionedMathBox>();
        children.add(new PositionedMathBox(base, 0, 0));

        var scriptWidth = 0;
        var ascent = base.ascent();
        var descent = base.descent();
        if (subscript != null) {
            children.add(new PositionedMathBox(subscript, scriptX, SCRIPT_SUBSCRIPT_DROP));
            scriptWidth = Math.max(scriptWidth, subscript.width());
            descent = Math.max(descent, SCRIPT_SUBSCRIPT_DROP + subscript.descent());
        }
        if (superscript != null) {
            children.add(new PositionedMathBox(superscript, scriptX, -SCRIPT_SUPERSCRIPT_RISE));
            scriptWidth = Math.max(scriptWidth, superscript.width());
            ascent = Math.max(ascent, SCRIPT_SUPERSCRIPT_RISE + superscript.ascent());
        }
        return new MathBox(
                base.width() + SCRIPT_HORIZONTAL_GAP + scriptWidth,
                ascent,
                descent,
                children,
                List.of(),
                Optional.of(path));
    }

    private MathBox layoutGroup(MathGroup group, MathTextMeasurer measurer, MathPath path) {
        var left = layoutGlyph(leftDelimiter(group), MathTextKind.SYMBOL, measurer, path);
        var contentPath = path.append(new GroupContent());
        var content = ensureEditableSlot(layoutExpression(group.content(), measurer, contentPath), measurer, contentPath);
        var right = layoutGlyph(rightDelimiter(group), MathTextKind.SYMBOL, measurer, path);
        var contentX = left.width();
        var rightX = contentX + content.width();
        return new MathBox(
                left.width() + content.width() + right.width(),
                Math.max(left.ascent(), Math.max(content.ascent(), right.ascent())),
                Math.max(left.descent(), Math.max(content.descent(), right.descent())),
                List.of(
                        new PositionedMathBox(left, 0, 0),
                        new PositionedMathBox(content, contentX, 0),
                        new PositionedMathBox(right, rightX, 0)),
                List.of(),
                Optional.of(path));
    }


    private MathBox ensureEditableSlot(MathBox box, MathTextMeasurer measurer, MathPath path) {
        if (box.width() > 0 || box.ascent() > 0 || box.descent() > 0 || !box.children().isEmpty() || !box.primitives().isEmpty()) {
            return box;
        }
        var metrics = measurer.measureText("0", MathTextKind.NUMBER);
        return new MathBox(EMPTY_SLOT_WIDTH, metrics.ascent(), metrics.descent(), List.of(), List.of(), Optional.of(path));
    }

    private static MathBox scaleBox(MathBox box, int numerator, int denominator) {
        var children = box.children().stream()
                .map(child -> new PositionedMathBox(
                        scaleBox(child.box(), numerator, denominator),
                        scaled(child.x(), numerator, denominator),
                        scaled(child.baselineOffset(), numerator, denominator)))
                .toList();
        var primitives = box.primitives().stream()
                .map(primitive -> scalePrimitive(primitive, numerator, denominator))
                .toList();
        return new MathBox(
                scaled(box.width(), numerator, denominator),
                scaled(box.ascent(), numerator, denominator),
                scaled(box.descent(), numerator, denominator),
                children,
                primitives,
                box.sourcePath());
    }

    private static MathPrimitive scalePrimitive(MathPrimitive primitive, int numerator, int denominator) {
        if (primitive instanceof MathGlyphRun glyph) {
            return new MathGlyphRun(
                    glyph.content(),
                    glyph.kind(),
                    scaled(glyph.x(), numerator, denominator),
                    scaled(glyph.baselineOffset(), numerator, denominator),
                    scaled(glyph.ascent(), numerator, denominator),
                    glyph.scale() * numerator / (double) denominator);
        }
        if (primitive instanceof MathHorizontalRule rule) {
            return new MathHorizontalRule(
                    scaled(rule.x(), numerator, denominator),
                    scaled(rule.y(), numerator, denominator),
                    scaled(rule.width(), numerator, denominator),
                    Math.max(1, scaled(rule.thickness(), numerator, denominator)));
        }
        if (primitive instanceof MathLineSegment line) {
            return new MathLineSegment(
                    scaled(line.x1(), numerator, denominator),
                    scaled(line.y1(), numerator, denominator),
                    scaled(line.x2(), numerator, denominator),
                    scaled(line.y2(), numerator, denominator),
                    Math.max(1, scaled(line.thickness(), numerator, denominator)));
        }
        throw new IllegalArgumentException("Unsupported math primitive: " + primitive.getClass().getName());
    }

    private static int scaled(int value, int numerator, int denominator) {
        return Math.round(value * numerator / (float) denominator);
    }

    private static int scriptScaleNumerator(MathExpression expression) {
        return isStructuralScriptContent(expression) ? STRUCTURAL_SCRIPT_SCALE_NUMERATOR : SCRIPT_SCALE_NUMERATOR;
    }

    private static int scriptScaleDenominator(MathExpression expression) {
        return isStructuralScriptContent(expression) ? STRUCTURAL_SCRIPT_SCALE_DENOMINATOR : SCRIPT_SCALE_DENOMINATOR;
    }

    private static boolean isStructuralScriptContent(MathExpression expression) {
        if (expression instanceof MathFraction || expression instanceof MathRoot || expression instanceof MathScript) {
            return true;
        }
        if (expression instanceof MathSequence sequence) {
            return sequence.expressions().size() > 1
                    || sequence.expressions().stream().anyMatch(MathLayoutEngine::isStructuralScriptContent);
        }
        return false;
    }

    private static String leftDelimiter(MathGroup group) {
        return switch (group.delimiter()) {
            case PARENTHESES -> "(";
            case BRACKETS -> "[";
            case BRACES -> "{";
        };
    }

    private static String rightDelimiter(MathGroup group) {
        return switch (group.delimiter()) {
            case PARENTHESES -> ")";
            case BRACKETS -> "]";
            case BRACES -> "}";
        };
    }

}
