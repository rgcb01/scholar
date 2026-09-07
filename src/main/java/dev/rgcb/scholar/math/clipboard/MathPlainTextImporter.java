package dev.rgcb.scholar.math.clipboard;

import dev.rgcb.scholar.math.MathExpression;
import dev.rgcb.scholar.math.MathIdentifier;
import dev.rgcb.scholar.math.MathNumber;
import dev.rgcb.scholar.math.MathOperator;
import dev.rgcb.scholar.math.MathOperatorRole;
import dev.rgcb.scholar.math.MathSequence;
import dev.rgcb.scholar.math.MathSymbol;
import dev.rgcb.scholar.math.MathSymbolKind;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class MathPlainTextImporter {
    public static final int MAX_INPUT_LENGTH = 4096;
    public static final int MAX_TOKENS = 512;

    public MathPlainTextImportResult importText(String text) {
        Objects.requireNonNull(text, "text");
        if (text.length() > MAX_INPUT_LENGTH) {
            return failure(MathImportError.TOO_LARGE);
        }

        var tokens = new ArrayList<MathExpression>();
        var index = 0;
        while (index < text.length()) {
            var codePoint = text.codePointAt(index);
            if (Character.isWhitespace(codePoint)) {
                index += Character.charCount(codePoint);
                continue;
            }
            if (Character.isDigit(codePoint) || codePoint == '.') {
                var number = readNumber(text, index);
                if (number.error() != null) {
                    return failure(number.error());
                }
                tokens.add(new MathNumber(number.text()));
                index = number.nextIndex();
            } else if (isIdentifierStart(codePoint)) {
                var identifier = readIdentifierAtom(text, index);
                tokens.add(new MathIdentifier(identifier.text()));
                index = identifier.nextIndex();
            } else {
                var normalized = normalizeOperator(codePoint);
                var atom = atomFor(normalized);
                if (atom == null) {
                    return failure(MathImportError.UNSUPPORTED_CHARACTER);
                }
                tokens.add(atom);
                index += Character.charCount(codePoint);
            }
            if (tokens.size() > MAX_TOKENS) {
                return failure(MathImportError.TOO_MANY_TOKENS);
            }
        }

        if (tokens.isEmpty()) {
            return failure(MathImportError.EMPTY);
        }
        return new MathPlainTextImportResult.Success(new MathSequence(tokens));
    }

    private static ReadToken readNumber(String text, int start) {
        var index = start;
        var decimalSeen = false;
        var digitSeen = false;
        while (index < text.length()) {
            var codePoint = text.codePointAt(index);
            if (Character.isDigit(codePoint)) {
                digitSeen = true;
                index += Character.charCount(codePoint);
            } else if (codePoint == '.') {
                if (decimalSeen) {
                    return new ReadToken("", index, MathImportError.INVALID_NUMBER);
                }
                decimalSeen = true;
                index += Character.charCount(codePoint);
            } else {
                break;
            }
        }
        if (!digitSeen) {
            return new ReadToken("", index, MathImportError.INVALID_NUMBER);
        }
        if (looksLikeScientificNotation(text, index)) {
            return new ReadToken("", index, MathImportError.UNSUPPORTED_SYNTAX);
        }
        return new ReadToken(text.substring(start, index), index, null);
    }

    private static boolean looksLikeScientificNotation(String text, int index) {
        if (index >= text.length()) {
            return false;
        }
        var codePoint = text.codePointAt(index);
        if (codePoint != 'e' && codePoint != 'E') {
            return false;
        }
        var next = index + Character.charCount(codePoint);
        if (next >= text.length()) {
            return false;
        }
        var afterE = text.codePointAt(next);
        if (afterE == '+' || afterE == '-') {
            next += Character.charCount(afterE);
            return next < text.length() && Character.isDigit(text.codePointAt(next));
        }
        return Character.isDigit(afterE);
    }

    private static ReadToken readIdentifierAtom(String text, int start) {
        var first = text.codePointAt(start);
        var index = start + Character.charCount(first);
        while (index < text.length()) {
            var codePoint = text.codePointAt(index);
            if (!isCombiningMark(codePoint)) {
                break;
            }
            index += Character.charCount(codePoint);
        }
        return new ReadToken(text.substring(start, index), index, null);
    }

    private static boolean isIdentifierStart(int codePoint) {
        return Character.isLetter(codePoint);
    }

    private static boolean isCombiningMark(int codePoint) {
        var type = Character.getType(codePoint);
        return type == Character.NON_SPACING_MARK
                || type == Character.COMBINING_SPACING_MARK
                || type == Character.ENCLOSING_MARK;
    }

    private static int normalizeOperator(int codePoint) {
        return switch (codePoint) {
            case '\u2212' -> '-';
            case '\u00d7' -> '*';
            case '\u00f7' -> '/';
            default -> codePoint;
        };
    }

    private static MathExpression atomFor(int codePoint) {
        return switch (codePoint) {
            case '+' -> new MathOperator("+", MathOperatorRole.BINARY);
            case '-' -> new MathOperator("-", MathOperatorRole.BINARY);
            case '*' -> new MathOperator("*", MathOperatorRole.BINARY);
            case '/' -> new MathOperator("/", MathOperatorRole.BINARY);
            case '=' -> new MathOperator("=", MathOperatorRole.RELATION);
            case '(' -> new MathSymbol("(", MathSymbolKind.OTHER);
            case ')' -> new MathSymbol(")", MathSymbolKind.OTHER);
            default -> null;
        };
    }

    private static MathPlainTextImportResult failure(MathImportError error) {
        return new MathPlainTextImportResult.Failure(error);
    }

    private record ReadToken(String text, int nextIndex, MathImportError error) {
    }
}
