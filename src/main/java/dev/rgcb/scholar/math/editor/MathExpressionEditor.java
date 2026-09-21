package dev.rgcb.scholar.math.editor;

import dev.rgcb.scholar.math.MathDelimiter;
import dev.rgcb.scholar.math.MathExpression;
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
import dev.rgcb.scholar.math.MathSymbolKind;
import dev.rgcb.scholar.math.MathText;
import dev.rgcb.scholar.math.MathQuantity;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class MathExpressionEditor {
    public MathSelection rootStart() {
        return new MathCaretSelection(new MathSequencePosition(MathPath.ROOT, 0));
    }

    public MathSelection selectAll(MathExpression expression) {
        Objects.requireNonNull(expression, "expression");
        var positions = positions(expression);
        if (positions.size() <= 1) {
            return rootStart();
        }
        return new MathRangeSelection(positions.get(0), positions.get(positions.size() - 1));
    }

    public MathEditResult insertText(MathExpression expression, MathSelection selection, String text) {
        Objects.requireNonNull(expression, "expression");
        Objects.requireNonNull(text, "text");
        if (text.length() != 1) {
            return unchanged(expression, selection);
        }
        var character = text.charAt(0);
        if (character == ' ') {
            return unchanged(expression, selection);
        }
        if (character == '^') {
            return insertScriptSlot(expression, selection, ScriptSlot.SUPERSCRIPT);
        }
        if (character == '_') {
            return insertScriptSlot(expression, selection, ScriptSlot.SUBSCRIPT);
        }
        var atom = atomFor(character);
        if (atom.isEmpty() && character != '.') {
            return unchanged(expression, selection);
        }
        if (selection instanceof MathRangeSelection) {
            var deleted = deleteSelection(expression, selection);
            if (!deleted.changed()) {
                return unchanged(expression, selection);
            }
            var inserted = insertText(deleted.expression(), deleted.selection(), text);
            if (!inserted.changed()) {
                return deleted;
            }
            return inserted;
        }
        var caret = caret(selection);
        if (caret instanceof MathTokenPosition tokenPosition) {
            return insertInsideToken(expression, tokenPosition, character);
        }
        var sequencePosition = (MathSequencePosition) caret;
        return character == '.'
                ? insertDecimalAtSequencePosition(expression, sequencePosition)
                : insertAtomAtSequencePosition(expression, sequencePosition, atom.orElseThrow(), character);
    }

    public MathEditResult insertFraction(MathExpression expression, MathSelection selection) {
        Objects.requireNonNull(expression, "expression");
        var fraction = new MathFraction(new MathSequence(List.of()), new MathSequence(List.of()));
        if (selection instanceof MathRangeSelection) {
            return wrapRangeInFraction(expression, selection, fraction);
        }
        return insertFractionAtCaret(expression, caret(selection), fraction);
    }

    public MathEditResult insertRoot(MathExpression expression, MathSelection selection) {
        Objects.requireNonNull(expression, "expression");
        var root = new MathRoot(new MathSequence(List.of()), Optional.empty());
        if (selection instanceof MathRangeSelection) {
            return wrapRangeInRoot(expression, selection);
        }
        return insertRootAtCaret(expression, caret(selection), root);
    }

    public MathEditResult insertGroup(MathExpression expression, MathSelection selection, MathDelimiter delimiter) {
        Objects.requireNonNull(expression, "expression");
        Objects.requireNonNull(delimiter, "delimiter");
        var group = new MathGroup(new MathSequence(List.of()), delimiter);
        if (selection instanceof MathRangeSelection rangeSelection) {
            return wrapRangeInGroup(expression, rangeSelection, delimiter);
        }
        return insertGroupAtCaret(expression, caret(selection), group);
    }

    public MathEditResult insertScriptSlot(MathExpression expression, MathSelection selection, ScriptSlot slot) {
        Objects.requireNonNull(expression, "expression");
        Objects.requireNonNull(slot, "slot");
        if (selection instanceof MathRangeSelection rangeSelection) {
            return wrapSingleAtomRangeInScript(expression, rangeSelection, slot);
        }
        var position = caret(selection);
        if (!(position instanceof MathSequencePosition sequencePosition)) {
            return unchanged(expression, selection);
        }
        validateSequencePosition(expression, sequencePosition);
        if (sequencePosition.childOffset() == 0) {
            return unchanged(expression, selection);
        }
        var sequence = sequenceAt(expression, sequencePosition.sequencePath());
        var atomIndex = sequencePosition.childOffset() - 1;
        var previous = sequence.children().get(atomIndex);
        var scriptPath = sequencePosition.sequencePath().append(new SequenceChild(atomIndex));
        if (previous instanceof MathRoot root && slot == ScriptSlot.SUBSCRIPT) {
            return insertOrEnterRootIndex(expression, sequencePosition, scriptPath, root);
        }
        if (previous instanceof MathScript script) {
            return insertOrEnterExistingScriptSlot(expression, sequencePosition, scriptPath, script, slot);
        }
        if (!isScriptableBase(previous)) {
            return unchanged(expression, selection);
        }
        var script = scriptWithSlot(previous, slot);
        var children = new ArrayList<>(sequence.children());
        children.set(atomIndex, script);
        var updated = replaceSequence(expression, sequencePosition.sequencePath(), children);
        return new MathEditResult(
                updated,
                new MathCaretSelection(new MathSequencePosition(scriptPath.append(pathSegmentFor(slot)), 0)),
                true);
    }

    public MathEditResult deleteBackward(MathExpression expression, MathSelection selection) {
        Objects.requireNonNull(expression, "expression");
        if (selection instanceof MathRangeSelection) {
            return deleteSelection(expression, selection);
        }
        var caret = caret(selection);
        if (caret instanceof MathTokenPosition tokenPosition) {
            return deleteTokenCharacter(expression, tokenPosition, -1);
        }
        var position = (MathSequencePosition) caret;
        validateSequencePosition(expression, position);
        if (position.childOffset() == 0) {
            var removedSlot = removeEmptyScriptSlotAtStart(expression, position);
            if (removedSlot.isPresent()) {
                return removedSlot.orElseThrow();
            }
            var removedRootIndex = removeEmptyRootIndexAtStart(expression, position);
            if (removedRootIndex.isPresent()) {
                return removedRootIndex.orElseThrow();
            }
            var removedEmptyGroup = removeEmptyGroupAtStart(expression, position);
            if (removedEmptyGroup.isPresent()) {
                return removedEmptyGroup.orElseThrow();
            }
            return unchanged(expression, selection);
        }
        var sequence = sequenceAt(expression, position.sequencePath());
        var previous = sequence.children().get(position.childOffset() - 1);
        if (previous instanceof MathScript script) {
            var scriptPath = position.sequencePath().append(new SequenceChild(position.childOffset() - 1));
            return moved(expression, sequenceEnd(expression, scriptPath.append(lastPresentScriptSlot(script))));
        }
        if (previous instanceof MathGroup) {
            var groupPath = position.sequencePath().append(new SequenceChild(position.childOffset() - 1));
            return moved(expression, sequenceEnd(expression, groupPath.append(new GroupContent())));
        }
        if (!isSimpleAtom(previous)) {
            return unchanged(expression, selection);
        }
        return removeAtom(expression, position.sequencePath(), position.childOffset() - 1);
    }

    public MathEditResult deleteForward(MathExpression expression, MathSelection selection) {
        Objects.requireNonNull(expression, "expression");
        if (selection instanceof MathRangeSelection) {
            return deleteSelection(expression, selection);
        }
        var caret = caret(selection);
        if (caret instanceof MathTokenPosition tokenPosition) {
            return deleteTokenCharacter(expression, tokenPosition, 0);
        }
        var position = (MathSequencePosition) caret;
        validateSequencePosition(expression, position);
        var sequence = sequenceAt(expression, position.sequencePath());
        if (position.childOffset() >= sequence.children().size()) {
            return unchanged(expression, selection);
        }
        var current = sequence.children().get(position.childOffset());
        if (current instanceof MathScript) {
            var scriptPath = position.sequencePath().append(new SequenceChild(position.childOffset()));
            return moved(expression, new MathSequencePosition(scriptPath.append(new ScriptBase()), 0));
        }
        if (current instanceof MathGroup) {
            var groupPath = position.sequencePath().append(new SequenceChild(position.childOffset()));
            return moved(expression, new MathSequencePosition(groupPath.append(new GroupContent()), 0));
        }
        if (!isSimpleAtom(current)) {
            return unchanged(expression, selection);
        }
        return removeAtom(expression, position.sequencePath(), position.childOffset());
    }

    public MathEditResult moveLeft(MathExpression expression, MathSelection selection) {
        Objects.requireNonNull(expression, "expression");
        if (selection instanceof MathRangeSelection rangeSelection) {
            return moved(expression, normalizeRange(expression, rangeSelection).start());
        }
        var caret = caret(selection);
        if (caret instanceof MathTokenPosition tokenPosition) {
            validateTokenPosition(expression, tokenPosition);
            var token = tokenAt(expression, tokenPosition.tokenPath());
            var previous = MathTextBoundary.previousOffset(token.text(), tokenPosition.characterOffset());
            return moved(expression, canonicalTokenPosition(tokenPosition.tokenPath(), previous, token.length()));
        }
        var position = (MathSequencePosition) caret;
        validateSequencePosition(expression, position);
        if (position.childOffset() == 0) {
            return moveLeftOutOfContainer(expression, position);
        }
        var sequence = sequenceAt(expression, position.sequencePath());
        var childIndex = position.childOffset() - 1;
        var child = sequence.children().get(childIndex);
        var childPath = position.sequencePath().append(new SequenceChild(childIndex));
        if (child instanceof MathScript script) {
            return moved(expression, sequenceEnd(expression, childPath.append(lastPresentScriptSlot(script))));
        }
        if (child instanceof MathGroup) {
            return moved(expression, sequenceEnd(expression, childPath.append(new GroupContent())));
        }
        if (child instanceof MathFraction) {
            return moved(expression, sequenceEnd(expression, childPath.append(new FractionDenominator())));
        }
        if (child instanceof MathRoot) {
            return moved(expression, sequenceEnd(expression, childPath.append(new RootRadicand())));
        }
        if (isToken(child) && tokenLength(child) > 1) {
            return moved(expression, new MathTokenPosition(childPath, tokenLength(child) - 1));
        }
        return moved(expression, new MathSequencePosition(position.sequencePath(), childIndex));
    }

    public MathEditResult moveRight(MathExpression expression, MathSelection selection) {
        Objects.requireNonNull(expression, "expression");
        if (selection instanceof MathRangeSelection rangeSelection) {
            return moved(expression, normalizeRange(expression, rangeSelection).end());
        }
        var caret = caret(selection);
        if (caret instanceof MathTokenPosition tokenPosition) {
            validateTokenPosition(expression, tokenPosition);
            var token = tokenAt(expression, tokenPosition.tokenPath());
            var next = MathTextBoundary.nextOffset(token.text(), tokenPosition.characterOffset());
            return moved(expression, canonicalTokenPosition(tokenPosition.tokenPath(), next, token.length()));
        }
        var position = (MathSequencePosition) caret;
        validateSequencePosition(expression, position);
        var sequence = sequenceAt(expression, position.sequencePath());
        if (position.childOffset() == sequence.children().size()) {
            return moveRightOutOfContainer(expression, position);
        }
        var child = sequence.children().get(position.childOffset());
        var childPath = position.sequencePath().append(new SequenceChild(position.childOffset()));
        if (child instanceof MathScript) {
            return moved(expression, new MathSequencePosition(childPath.append(new ScriptBase()), 0));
        }
        if (child instanceof MathGroup) {
            return moved(expression, new MathSequencePosition(childPath.append(new GroupContent()), 0));
        }
        if (child instanceof MathFraction) {
            return moved(expression, new MathSequencePosition(childPath.append(new FractionNumerator()), 0));
        }
        if (child instanceof MathRoot) {
            var root = root(child);
            return root.index().isPresent()
                    ? moved(expression, new MathSequencePosition(childPath.append(new RootIndex()), 0))
                    : moved(expression, new MathSequencePosition(childPath.append(new RootRadicand()), 0));
        }
        if (isToken(child) && tokenLength(child) > 1) {
            return moved(expression, new MathTokenPosition(childPath, 1));
        }
        return moved(expression, new MathSequencePosition(position.sequencePath(), position.childOffset() + 1));
    }

    public boolean isRootStart(MathExpression expression, MathSelection selection) {
        if (!(selection instanceof MathCaretSelection)) {
            return false;
        }
        var caret = caret(selection);
        return caret instanceof MathSequencePosition position
                && position.sequencePath().equals(MathPath.ROOT)
                && position.childOffset() == 0;
    }

    public boolean isRootEnd(MathExpression expression, MathSelection selection) {
        if (!(selection instanceof MathCaretSelection)) {
            return false;
        }
        var caret = caret(selection);
        return caret instanceof MathSequencePosition position
                && position.sequencePath().equals(MathPath.ROOT)
                && position.childOffset() == sequenceAt(expression, MathPath.ROOT).children().size();
    }

    public void validateSelection(MathExpression expression, MathSelection selection) {
        Objects.requireNonNull(selection, "selection");
        if (selection instanceof MathCaretSelection caretSelection) {
            validatePosition(expression, caretSelection.caret());
        } else if (selection instanceof MathRangeSelection rangeSelection) {
            validatePosition(expression, rangeSelection.anchor());
            validatePosition(expression, rangeSelection.active());
            normalizeRange(expression, rangeSelection);
        } else {
            throw new IllegalArgumentException("Unsupported math selection: " + selection.getClass().getName());
        }
    }

    public MathSelection extendLeft(MathExpression expression, MathSelection selection) {
        Objects.requireNonNull(expression, "expression");
        var anchor = selection instanceof MathRangeSelection rangeSelection
                ? rangeSelection.anchor()
                : caret(selection);
        var active = selection instanceof MathRangeSelection rangeSelection
                ? rangeSelection.active()
                : caret(selection);
        var moved = moveLeft(expression, new MathCaretSelection(active)).selection();
        var nextActive = caret(moved);
        return selection(anchor, nextActive);
    }

    public MathSelection extendRight(MathExpression expression, MathSelection selection) {
        Objects.requireNonNull(expression, "expression");
        var anchor = selection instanceof MathRangeSelection rangeSelection
                ? rangeSelection.anchor()
                : caret(selection);
        var active = selection instanceof MathRangeSelection rangeSelection
                ? rangeSelection.active()
                : caret(selection);
        var moved = moveRight(expression, new MathCaretSelection(active)).selection();
        var nextActive = caret(moved);
        return selection(anchor, nextActive);
    }

    public MathRange normalizeRange(MathExpression expression, MathRangeSelection selection) {
        var positions = positions(expression);
        var anchor = positions.indexOf(selection.anchor());
        var active = positions.indexOf(selection.active());
        if (anchor < 0 || active < 0) {
            throw new IllegalArgumentException("Math range endpoint is outside the expression.");
        }
        return anchor < active
                ? new MathRange(selection.anchor(), selection.active())
                : new MathRange(selection.active(), selection.anchor());
    }

    public List<MathPosition> positions(MathExpression expression) {
        Objects.requireNonNull(expression, "expression");
        var positions = new ArrayList<MathPosition>();
        MathPosition current = new MathSequencePosition(MathPath.ROOT, 0);
        positions.add(current);
        for (var guard = 0; guard < 10_000; guard++) {
            var next = caret(moveRight(expression, new MathCaretSelection(current)).selection());
            if (next.equals(current)) {
                return List.copyOf(positions);
            }
            positions.add(next);
            current = next;
        }
        throw new IllegalStateException("Math position traversal did not terminate.");
    }

    public MathEditResult deleteSelection(MathExpression expression, MathSelection selection) {
        Objects.requireNonNull(expression, "expression");
        if (!(selection instanceof MathRangeSelection rangeSelection)) {
            return unchanged(expression, selection);
        }
        var range = normalizeRange(expression, rangeSelection);
        var sameToken = deleteSameTokenRange(expression, range);
        if (sameToken.isPresent()) {
            return sameToken.get();
        }
        var start = sequenceBoundary(expression, range.start(), true);
        var end = sequenceBoundary(expression, range.end(), false);
        if (!start.sequencePath().equals(end.sequencePath())) {
            return unchanged(expression, selection);
        }
        return replaceSameSequenceRange(expression, start, end, List.of(), false);
    }

    public Optional<MathSequence> extractSelection(MathExpression expression, MathRangeSelection selection) {
        Objects.requireNonNull(expression, "expression");
        Objects.requireNonNull(selection, "selection");
        var range = normalizeRange(expression, selection);
        var sameToken = extractSameTokenRange(expression, range);
        if (sameToken.isPresent()) {
            return sameToken;
        }
        var wholeFractionInterior = extractCompleteFractionInteriorRange(expression, range);
        if (wholeFractionInterior.isPresent()) {
            return wholeFractionInterior;
        }
        var start = sequenceBoundary(expression, range.start(), true);
        var end = sequenceBoundary(expression, range.end(), false);
        if (!start.sequencePath().equals(end.sequencePath())) {
            return Optional.empty();
        }
        var selected = selectedExpressions(expression, start, end);
        return selected.isEmpty() ? Optional.empty() : Optional.of(new MathSequence(selected));
    }

    public Optional<String> semanticTokenContent(MathExpression expression, MathSelection selection) {
        Objects.requireNonNull(expression, "expression");
        Objects.requireNonNull(selection, "selection");
        if (!(selection instanceof MathRangeSelection rangeSelection)) {
            return Optional.empty();
        }
        var selected = semanticTokenSourceExpressions(expression, rangeSelection);
        if (selected.isEmpty()) {
            return Optional.empty();
        }
        var content = new StringBuilder();
        for (var expressionPart : selected.orElseThrow()) {
            var text = simpleText(expressionPart);
            if (text.isEmpty()) {
                return Optional.empty();
            }
            content.append(text.orElseThrow());
        }
        return content.isEmpty() ? Optional.empty() : Optional.of(content.toString());
    }

    public Optional<SemanticMathTokenDraft> semanticTokenDraft(
            MathExpression expression,
            MathSelection selection,
            SemanticMathTokenKind preferredKind
    ) {
        Objects.requireNonNull(preferredKind, "preferredKind");
        return semanticTokenContent(expression, selection)
                .map(content -> new SemanticMathTokenDraft(preferredKind, content));
    }

    public boolean canConvertSelectionToSemanticToken(
            MathExpression expression,
            MathSelection selection,
            SemanticMathTokenKind kind,
            String content
    ) {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(content, "content");
        if (semanticTokenContent(expression, selection).isEmpty()) {
            return false;
        }
        return isValidSemanticTokenContent(kind, content);
    }

    public MathEditResult convertSelectionToSemanticToken(
            MathExpression expression,
            MathSelection selection,
            SemanticMathTokenKind kind,
            String content
    ) {
        Objects.requireNonNull(expression, "expression");
        Objects.requireNonNull(selection, "selection");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(content, "content");
        if (!canConvertSelectionToSemanticToken(expression, selection, kind, content)) {
            return unchanged(expression, selection);
        }
        var rangeSelection = (MathRangeSelection) selection;
        var range = normalizeRange(expression, rangeSelection);
        var start = sequenceBoundary(expression, range.start(), true);
        var end = sequenceBoundary(expression, range.end(), false);
        var replacement = switch (kind) {
            case NAMED_OPERATOR -> new MathNamedOperator(content);
            case MATH_TEXT -> new MathText(content);
        };
        return replaceSameSequenceRange(expression, start, end, List.of(replacement), false);
    }

    private Optional<MathSequence> extractCompleteFractionInteriorRange(MathExpression expression, MathRange range) {
        if (!(range.start() instanceof MathSequencePosition start)
                || !(range.end() instanceof MathSequencePosition end)
                || start.sequencePath().segments().isEmpty()
                || end.sequencePath().segments().isEmpty()
                || !(start.sequencePath().last() instanceof FractionNumerator)
                || !(end.sequencePath().last() instanceof FractionDenominator)) {
            return Optional.empty();
        }
        var fractionPath = start.sequencePath().parent();
        if (!fractionPath.equals(end.sequencePath().parent())) {
            return Optional.empty();
        }
        if (start.childOffset() != 0 || end.childOffset() != sequenceAt(expression, end.sequencePath()).children().size()) {
            return Optional.empty();
        }
        return Optional.of(new MathSequence(List.of(fraction(nodeAt(expression, fractionPath)))));
    }

    private Optional<List<MathExpression>> semanticTokenSourceExpressions(MathExpression expression, MathRangeSelection selection) {
        var range = normalizeRange(expression, selection);
        var start = sequenceBoundary(expression, range.start(), true);
        var end = sequenceBoundary(expression, range.end(), false);
        if (!start.sequencePath().equals(end.sequencePath())) {
            return Optional.empty();
        }
        var selected = selectedExpressions(expression, start, end);
        if (selected.isEmpty() || selected.stream().anyMatch(part -> simpleText(part).isEmpty())) {
            return Optional.empty();
        }
        return Optional.of(selected);
    }

    public MathEditResult pasteFragment(MathExpression expression, MathSelection selection, MathSequence fragment) {
        Objects.requireNonNull(expression, "expression");
        Objects.requireNonNull(selection, "selection");
        Objects.requireNonNull(fragment, "fragment");
        if (fragment.expressions().isEmpty()) {
            return unchanged(expression, selection);
        }
        if (selection instanceof MathRangeSelection rangeSelection) {
            var range = normalizeRange(expression, rangeSelection);
            var start = sequenceBoundary(expression, range.start(), true);
            var end = sequenceBoundary(expression, range.end(), false);
            if (!start.sequencePath().equals(end.sequencePath())) {
                return unchanged(expression, selection);
            }
            return replaceSameSequenceRange(expression, start, end, fragment.expressions(), false);
        }
        var caret = caret(selection);
        if (caret instanceof MathSequencePosition sequencePosition) {
            return pasteFragmentAtSequencePosition(expression, sequencePosition, fragment);
        }
        return pasteFragmentInsideToken(expression, (MathTokenPosition) caret, fragment);
    }

    private MathEditResult insertAtomAtSequencePosition(
            MathExpression expression,
            MathSequencePosition position,
            MathExpression atom,
            char typed
    ) {
        validateSequencePosition(expression, position);
        if (isDirectScriptBasePath(position.sequencePath())) {
            return unchanged(expression, new MathCaretSelection(position));
        }
        if ((Character.isDigit(typed) || typed == '.') && atom instanceof MathNumber) {
            var coalesced = coalesceNumber(expression, position, String.valueOf(typed));
            if (coalesced.isPresent()) {
                return coalesced.get();
            }
        }
        var sequence = sequenceAt(expression, position.sequencePath());
        var children = new ArrayList<>(sequence.children());
        children.add(position.childOffset(), atom);
        var updated = replaceSequence(expression, position.sequencePath(), children);
        return new MathEditResult(
                updated,
                new MathCaretSelection(new MathSequencePosition(position.sequencePath(), position.childOffset() + 1)),
                true);
    }

    private MathEditResult wrapRangeInFraction(MathExpression expression, MathSelection selection, MathFraction emptyFraction) {
        var rangeSelection = (MathRangeSelection) selection;
        var range = normalizeRange(expression, rangeSelection);
        var sameToken = wrapSameTokenRangeInFraction(expression, range, emptyFraction);
        if (sameToken.isPresent()) {
            return sameToken.get();
        }
        var start = sequenceBoundary(expression, range.start(), true);
        var end = sequenceBoundary(expression, range.end(), false);
        if (!start.sequencePath().equals(end.sequencePath())) {
            return unchanged(expression, selection);
        }
        var selected = selectedExpressions(expression, start, end);
        if (selected.isEmpty()) {
            return unchanged(expression, selection);
        }
        var fraction = new MathFraction(new MathSequence(selected), emptyFraction.denominator());
        return replaceSameSequenceRange(expression, start, end, List.of(fraction), true);
    }

    private MathEditResult wrapRangeInRoot(MathExpression expression, MathSelection selection) {
        var rangeSelection = (MathRangeSelection) selection;
        var range = normalizeRange(expression, rangeSelection);
        var sameToken = wrapSameTokenRangeInRoot(expression, range);
        if (sameToken.isPresent()) {
            return sameToken.get();
        }
        var start = sequenceBoundary(expression, range.start(), true);
        var end = sequenceBoundary(expression, range.end(), false);
        if (!start.sequencePath().equals(end.sequencePath())) {
            return unchanged(expression, selection);
        }
        var selected = selectedExpressions(expression, start, end);
        if (selected.isEmpty()) {
            return unchanged(expression, selection);
        }
        var root = new MathRoot(new MathSequence(selected), Optional.empty());
        return replaceSameSequenceRange(expression, start, end, List.of(root), false);
    }

    private Optional<MathSequence> extractSameTokenRange(MathExpression expression, MathRange range) {
        if (!(range.start() instanceof MathTokenPosition start)
                || !(range.end() instanceof MathTokenPosition end)
                || !start.tokenPath().equals(end.tokenPath())) {
            return Optional.empty();
        }
        validateTokenPosition(expression, start);
        validateTokenPosition(expression, end);
        var token = tokenAt(expression, start.tokenPath());
        var selected = MathTextBoundary.substring(token.text(), start.characterOffset(), end.characterOffset());
        return splitTokenExpression(token.expression(), selected)
                .map(fragment -> new MathSequence(List.of(fragment)));
    }

    private MathEditResult pasteFragmentAtSequencePosition(
            MathExpression expression,
            MathSequencePosition position,
            MathSequence fragment
    ) {
        validateSequencePosition(expression, position);
        if (isDirectScriptBasePath(position.sequencePath())) {
            return unchanged(expression, new MathCaretSelection(position));
        }
        var children = new ArrayList<>(sequenceAt(expression, position.sequencePath()).children());
        children.addAll(position.childOffset(), fragment.expressions());
        var updated = replaceSequence(expression, position.sequencePath(), children);
        return new MathEditResult(
                updated,
                new MathCaretSelection(new MathSequencePosition(position.sequencePath(), position.childOffset() + fragment.expressions().size())),
                true);
    }

    private MathEditResult pasteFragmentInsideToken(
            MathExpression expression,
            MathTokenPosition position,
            MathSequence fragment
    ) {
        validateTokenPosition(expression, position);
        var token = tokenAt(expression, position.tokenPath());
        var prefix = MathTextBoundary.substring(token.text(), 0, position.characterOffset());
        var suffix = MathTextBoundary.substring(token.text(), position.characterOffset(), token.length());
        var parent = parentSequencePosition(position.tokenPath());
        var sourceChildren = sequenceAt(expression, parent.sequencePath()).children();
        var children = new ArrayList<MathExpression>();
        children.addAll(sourceChildren.subList(0, parent.childOffset()));
        splitTokenExpression(token.expression(), prefix).ifPresent(children::add);
        var caretOffset = children.size() + fragment.expressions().size();
        children.addAll(fragment.expressions());
        splitTokenExpression(token.expression(), suffix).ifPresent(children::add);
        children.addAll(sourceChildren.subList(parent.childOffset() + 1, sourceChildren.size()));
        var updated = replaceSequence(expression, parent.sequencePath(), children);
        return new MathEditResult(
                updated,
                new MathCaretSelection(new MathSequencePosition(parent.sequencePath(), caretOffset)),
                true);
    }

    private Optional<MathEditResult> deleteSameTokenRange(MathExpression expression, MathRange range) {
        if (!(range.start() instanceof MathTokenPosition start)
                || !(range.end() instanceof MathTokenPosition end)
                || !start.tokenPath().equals(end.tokenPath())) {
            return Optional.empty();
        }
        validateTokenPosition(expression, start);
        validateTokenPosition(expression, end);
        var token = tokenAt(expression, start.tokenPath());
        var updatedText = MathTextBoundary.substring(token.text(), 0, start.characterOffset())
                + MathTextBoundary.substring(token.text(), end.characterOffset(), token.length());
        var parent = parentSequencePosition(start.tokenPath());
        if (updatedText.isEmpty()) {
            return Optional.of(removeAtom(expression, parent.sequencePath(), parent.childOffset()));
        }
        var replacement = token.expression() instanceof MathNumber ? new MathNumber(updatedText) : new MathIdentifier(updatedText);
        var updated = replaceNode(expression, start.tokenPath(), replacement);
        var caret = canonicalTokenPosition(
                start.tokenPath(),
                start.characterOffset(),
                MathTextBoundary.characterCount(updatedText));
        return Optional.of(new MathEditResult(updated, new MathCaretSelection(caret), true));
    }

    private Optional<MathEditResult> wrapSameTokenRangeInFraction(MathExpression expression, MathRange range, MathFraction emptyFraction) {
        if (!(range.start() instanceof MathTokenPosition start)
                || !(range.end() instanceof MathTokenPosition end)
                || !start.tokenPath().equals(end.tokenPath())) {
            return Optional.empty();
        }
        validateTokenPosition(expression, start);
        validateTokenPosition(expression, end);
        var token = tokenAt(expression, start.tokenPath());
        var prefix = MathTextBoundary.substring(token.text(), 0, start.characterOffset());
        var selected = MathTextBoundary.substring(token.text(), start.characterOffset(), end.characterOffset());
        var suffix = MathTextBoundary.substring(token.text(), end.characterOffset(), token.length());
        var selectedExpression = splitTokenExpression(token.expression(), selected);
        if (selectedExpression.isEmpty()) {
            return Optional.of(unchanged(expression, new MathRangeSelection(start, end)));
        }
        var parent = parentSequencePosition(start.tokenPath());
        var children = new ArrayList<MathExpression>();
        children.addAll(sequenceAt(expression, parent.sequencePath()).children().subList(0, parent.childOffset()));
        splitTokenExpression(token.expression(), prefix).ifPresent(children::add);
        var insertedIndex = children.size();
        children.add(new MathFraction(new MathSequence(List.of(selectedExpression.get())), emptyFraction.denominator()));
        splitTokenExpression(token.expression(), suffix).ifPresent(children::add);
        var sourceChildren = sequenceAt(expression, parent.sequencePath()).children();
        children.addAll(sourceChildren.subList(parent.childOffset() + 1, sourceChildren.size()));
        var updated = replaceSequence(expression, parent.sequencePath(), children);
        var insertedPath = parent.sequencePath().append(new SequenceChild(insertedIndex));
        return Optional.of(new MathEditResult(
                updated,
                new MathCaretSelection(new MathSequencePosition(insertedPath.append(new FractionDenominator()), 0)),
                true));
    }

    private Optional<MathEditResult> wrapSameTokenRangeInRoot(MathExpression expression, MathRange range) {
        if (!(range.start() instanceof MathTokenPosition start)
                || !(range.end() instanceof MathTokenPosition end)
                || !start.tokenPath().equals(end.tokenPath())) {
            return Optional.empty();
        }
        validateTokenPosition(expression, start);
        validateTokenPosition(expression, end);
        var token = tokenAt(expression, start.tokenPath());
        var prefix = MathTextBoundary.substring(token.text(), 0, start.characterOffset());
        var selected = MathTextBoundary.substring(token.text(), start.characterOffset(), end.characterOffset());
        var suffix = MathTextBoundary.substring(token.text(), end.characterOffset(), token.length());
        var selectedExpression = splitTokenExpression(token.expression(), selected);
        if (selectedExpression.isEmpty()) {
            return Optional.of(unchanged(expression, new MathRangeSelection(start, end)));
        }
        var parent = parentSequencePosition(start.tokenPath());
        var children = new ArrayList<MathExpression>();
        children.addAll(sequenceAt(expression, parent.sequencePath()).children().subList(0, parent.childOffset()));
        splitTokenExpression(token.expression(), prefix).ifPresent(children::add);
        var insertedIndex = children.size();
        children.add(new MathRoot(new MathSequence(List.of(selectedExpression.get())), Optional.empty()));
        splitTokenExpression(token.expression(), suffix).ifPresent(children::add);
        var sourceChildren = sequenceAt(expression, parent.sequencePath()).children();
        children.addAll(sourceChildren.subList(parent.childOffset() + 1, sourceChildren.size()));
        var updated = replaceSequence(expression, parent.sequencePath(), children);
        return Optional.of(new MathEditResult(
                updated,
                new MathCaretSelection(new MathSequencePosition(parent.sequencePath(), insertedIndex + 1)),
                true));
    }

    private MathEditResult wrapRangeInGroup(MathExpression expression, MathRangeSelection selection, MathDelimiter delimiter) {
        var range = normalizeRange(expression, selection);
        if (!(range.start() instanceof MathSequencePosition start)
                || !(range.end() instanceof MathSequencePosition end)
                || !start.sequencePath().equals(end.sequencePath())
                || end.childOffset() <= start.childOffset()) {
            return unchanged(expression, selection);
        }
        validateSequencePosition(expression, start);
        validateSequencePosition(expression, end);
        var selected = selectedExpressions(
                expression,
                new SequenceBoundary(start.sequencePath(), start.childOffset(), start.childOffset(), Optional.empty(), Optional.empty()),
                new SequenceBoundary(end.sequencePath(), end.childOffset(), end.childOffset(), Optional.empty(), Optional.empty()));
        if (selected.isEmpty()) {
            return unchanged(expression, selection);
        }
        var group = new MathGroup(new MathSequence(selected), delimiter);
        return replaceSameSequenceRange(
                expression,
                new SequenceBoundary(start.sequencePath(), start.childOffset(), start.childOffset(), Optional.empty(), Optional.empty()),
                new SequenceBoundary(end.sequencePath(), end.childOffset(), end.childOffset(), Optional.empty(), Optional.empty()),
                List.of(group),
                false);
    }

    private MathEditResult insertGroupAtCaret(MathExpression expression, MathPosition position, MathGroup group) {
        Objects.requireNonNull(group, "group");
        if (position instanceof MathSequencePosition sequencePosition) {
            return insertGroupAtSequencePosition(expression, sequencePosition, group);
        }
        if (position instanceof MathTokenPosition tokenPosition) {
            return insertGroupInsideToken(expression, tokenPosition, group);
        }
        throw new IllegalArgumentException("Unsupported math position: " + position.getClass().getName());
    }

    private MathEditResult insertGroupAtSequencePosition(
            MathExpression expression,
            MathSequencePosition position,
            MathGroup group
    ) {
        validateSequencePosition(expression, position);
        if (isDirectScriptBasePath(position.sequencePath())) {
            return unchanged(expression, new MathCaretSelection(position));
        }
        var sequence = sequenceAt(expression, position.sequencePath());
        var children = new ArrayList<>(sequence.children());
        children.add(position.childOffset(), group);
        var updated = replaceSequence(expression, position.sequencePath(), children);
        var insertedPath = position.sequencePath().append(new SequenceChild(position.childOffset()));
        return new MathEditResult(
                updated,
                new MathCaretSelection(new MathSequencePosition(insertedPath.append(new GroupContent()), 0)),
                true);
    }

    private MathEditResult insertGroupInsideToken(
            MathExpression expression,
            MathTokenPosition position,
            MathGroup group
    ) {
        validateTokenPosition(expression, position);
        var token = tokenAt(expression, position.tokenPath());
        var prefix = MathTextBoundary.substring(token.text(), 0, position.characterOffset());
        var suffix = MathTextBoundary.substring(token.text(), position.characterOffset(), token.length());
        var prefixExpression = splitTokenExpression(token.expression(), prefix);
        var suffixExpression = splitTokenExpression(token.expression(), suffix);
        if (prefixExpression.isEmpty() || suffixExpression.isEmpty()) {
            return unchanged(expression, new MathCaretSelection(position));
        }

        var parent = parentSequencePosition(position.tokenPath());
        var sequence = sequenceAt(expression, parent.sequencePath());
        var children = new ArrayList<>(sequence.children());
        children.remove(parent.childOffset());
        children.add(parent.childOffset(), prefixExpression.get());
        children.add(parent.childOffset() + 1, group);
        children.add(parent.childOffset() + 2, suffixExpression.get());
        var updated = replaceSequence(expression, parent.sequencePath(), children);
        var insertedPath = parent.sequencePath().append(new SequenceChild(parent.childOffset() + 1));
        return new MathEditResult(
                updated,
                new MathCaretSelection(new MathSequencePosition(insertedPath.append(new GroupContent()), 0)),
                true);
    }

    private MathEditResult replaceSameSequenceRange(
            MathExpression expression,
            SequenceBoundary start,
            SequenceBoundary end,
            List<MathExpression> replacement,
            boolean caretInsideReplacementDenominator
    ) {
        var sourceChildren = sequenceAt(expression, start.sequencePath()).children();
        if (start.removeIndex() > end.removeExclusive() || end.removeExclusive() > sourceChildren.size()) {
            return unchanged(expression, new MathCaretSelection(new MathSequencePosition(start.sequencePath(), start.removeIndex())));
        }
        var children = new ArrayList<MathExpression>();
        children.addAll(sourceChildren.subList(0, start.removeIndex()));
        start.prefix().ifPresent(children::add);
        var replacementIndex = children.size();
        children.addAll(replacement);
        end.suffix().ifPresent(children::add);
        children.addAll(sourceChildren.subList(end.removeExclusive(), sourceChildren.size()));
        var updated = replaceSequence(expression, start.sequencePath(), children);
        MathPosition caret = caretInsideReplacementDenominator
                ? new MathSequencePosition(start.sequencePath()
                .append(new SequenceChild(replacementIndex))
                .append(new FractionDenominator()), 0)
                : new MathSequencePosition(start.sequencePath(), replacementIndex + replacement.size());
        return new MathEditResult(updated, new MathCaretSelection(caret), true);
    }

    private List<MathExpression> selectedExpressions(MathExpression expression, SequenceBoundary start, SequenceBoundary end) {
        var sourceChildren = sequenceAt(expression, start.sequencePath()).children();
        var selected = new ArrayList<MathExpression>();
        if (start.prefix().isPresent() && sourceChildren.get(start.removeIndex()) instanceof MathNumber number) {
            var text = MathTextBoundary.substring(number.content(),
                    MathTextBoundary.characterCount(((MathNumber) start.prefix().orElseThrow()).content()),
                    MathTextBoundary.characterCount(number.content()));
            splitTokenExpression(number, text).ifPresent(selected::add);
        } else if (start.prefix().isPresent() && sourceChildren.get(start.removeIndex()) instanceof MathIdentifier identifier) {
            var text = MathTextBoundary.substring(identifier.name(),
                    MathTextBoundary.characterCount(((MathIdentifier) start.prefix().orElseThrow()).name()),
                    MathTextBoundary.characterCount(identifier.name()));
            splitTokenExpression(identifier, text).ifPresent(selected::add);
        }

        var firstWhole = start.prefix().isPresent() ? start.removeIndex() + 1 : start.removeIndex();
        var lastWholeExclusive = end.suffix().isPresent() ? end.removeExclusive() - 1 : end.removeExclusive();
        if (firstWhole < lastWholeExclusive) {
            selected.addAll(sourceChildren.subList(firstWhole, lastWholeExclusive));
        }

        if (end.suffix().isPresent()) {
            var child = sourceChildren.get(end.removeExclusive() - 1);
            if (child instanceof MathNumber number) {
                var suffix = (MathNumber) end.suffix().orElseThrow();
                var text = MathTextBoundary.substring(number.content(), 0,
                        MathTextBoundary.characterCount(number.content()) - MathTextBoundary.characterCount(suffix.content()));
                splitTokenExpression(number, text).ifPresent(selected::add);
            } else if (child instanceof MathIdentifier identifier) {
                var suffix = (MathIdentifier) end.suffix().orElseThrow();
                var text = MathTextBoundary.substring(identifier.name(), 0,
                        MathTextBoundary.characterCount(identifier.name()) - MathTextBoundary.characterCount(suffix.name()));
                splitTokenExpression(identifier, text).ifPresent(selected::add);
            }
        }
        return List.copyOf(selected);
    }

    private SequenceBoundary sequenceBoundary(MathExpression expression, MathPosition position, boolean startBoundary) {
        if (position instanceof MathSequencePosition sequencePosition) {
            validateSequencePosition(expression, sequencePosition);
            return new SequenceBoundary(sequencePosition.sequencePath(), sequencePosition.childOffset(), sequencePosition.childOffset(), Optional.empty(), Optional.empty());
        }
        var tokenPosition = (MathTokenPosition) position;
        validateTokenPosition(expression, tokenPosition);
        var token = tokenAt(expression, tokenPosition.tokenPath());
        var parent = parentSequencePosition(tokenPosition.tokenPath());
        var prefix = MathTextBoundary.substring(token.text(), 0, tokenPosition.characterOffset());
        var suffix = MathTextBoundary.substring(token.text(), tokenPosition.characterOffset(), token.length());
        return startBoundary
                ? new SequenceBoundary(parent.sequencePath(), parent.childOffset(), parent.childOffset() + 1,
                splitTokenExpression(token.expression(), prefix), Optional.empty())
                : new SequenceBoundary(parent.sequencePath(), parent.childOffset(), parent.childOffset() + 1,
                Optional.empty(), splitTokenExpression(token.expression(), suffix));
    }

    private MathEditResult insertFractionAtCaret(MathExpression expression, MathPosition position, MathFraction fraction) {
        Objects.requireNonNull(fraction, "fraction");
        if (position instanceof MathSequencePosition sequencePosition) {
            return insertFractionAtSequencePosition(expression, sequencePosition, fraction);
        }
        if (position instanceof MathTokenPosition tokenPosition) {
            return insertFractionInsideToken(expression, tokenPosition, fraction);
        }
        throw new IllegalArgumentException("Unsupported math position: " + position.getClass().getName());
    }

    private MathEditResult insertRootAtCaret(MathExpression expression, MathPosition position, MathRoot root) {
        Objects.requireNonNull(root, "root");
        if (position instanceof MathSequencePosition sequencePosition) {
            return insertRootAtSequencePosition(expression, sequencePosition, root);
        }
        if (position instanceof MathTokenPosition tokenPosition) {
            return insertRootInsideToken(expression, tokenPosition, root);
        }
        throw new IllegalArgumentException("Unsupported math position: " + position.getClass().getName());
    }

    private MathEditResult insertRootAtSequencePosition(
            MathExpression expression,
            MathSequencePosition position,
            MathRoot root
    ) {
        validateSequencePosition(expression, position);
        if (isDirectScriptBasePath(position.sequencePath())) {
            return unchanged(expression, new MathCaretSelection(position));
        }
        var sequence = sequenceAt(expression, position.sequencePath());
        var children = new ArrayList<>(sequence.children());
        children.add(position.childOffset(), root);
        var updated = replaceSequence(expression, position.sequencePath(), children);
        var insertedPath = position.sequencePath().append(new SequenceChild(position.childOffset()));
        return new MathEditResult(
                updated,
                new MathCaretSelection(new MathSequencePosition(insertedPath.append(new RootRadicand()), 0)),
                true);
    }

    private MathEditResult insertRootInsideToken(
            MathExpression expression,
            MathTokenPosition position,
            MathRoot root
    ) {
        validateTokenPosition(expression, position);
        var token = tokenAt(expression, position.tokenPath());
        var prefix = MathTextBoundary.substring(token.text(), 0, position.characterOffset());
        var suffix = MathTextBoundary.substring(token.text(), position.characterOffset(), token.length());
        var prefixExpression = splitTokenExpression(token.expression(), prefix);
        var suffixExpression = splitTokenExpression(token.expression(), suffix);
        if (prefixExpression.isEmpty() || suffixExpression.isEmpty()) {
            return unchanged(expression, new MathCaretSelection(position));
        }

        var parent = parentSequencePosition(position.tokenPath());
        var sequence = sequenceAt(expression, parent.sequencePath());
        var children = new ArrayList<>(sequence.children());
        children.remove(parent.childOffset());
        children.add(parent.childOffset(), prefixExpression.get());
        children.add(parent.childOffset() + 1, root);
        children.add(parent.childOffset() + 2, suffixExpression.get());
        var updated = replaceSequence(expression, parent.sequencePath(), children);
        var insertedPath = parent.sequencePath().append(new SequenceChild(parent.childOffset() + 1));
        return new MathEditResult(
                updated,
                new MathCaretSelection(new MathSequencePosition(insertedPath.append(new RootRadicand()), 0)),
                true);
    }

    private MathEditResult insertFractionAtSequencePosition(
            MathExpression expression,
            MathSequencePosition position,
            MathFraction fraction
    ) {
        validateSequencePosition(expression, position);
        if (isDirectScriptBasePath(position.sequencePath())) {
            return unchanged(expression, new MathCaretSelection(position));
        }
        var sequence = sequenceAt(expression, position.sequencePath());
        var children = new ArrayList<>(sequence.children());
        children.add(position.childOffset(), fraction);
        var updated = replaceSequence(expression, position.sequencePath(), children);
        var insertedPath = position.sequencePath().append(new SequenceChild(position.childOffset()));
        return new MathEditResult(
                updated,
                new MathCaretSelection(new MathSequencePosition(insertedPath.append(new FractionNumerator()), 0)),
                true);
    }

    private MathEditResult insertFractionInsideToken(
            MathExpression expression,
            MathTokenPosition position,
            MathFraction fraction
    ) {
        validateTokenPosition(expression, position);
        var token = tokenAt(expression, position.tokenPath());
        var prefix = MathTextBoundary.substring(token.text(), 0, position.characterOffset());
        var suffix = MathTextBoundary.substring(token.text(), position.characterOffset(), token.length());
        var prefixExpression = splitTokenExpression(token.expression(), prefix);
        var suffixExpression = splitTokenExpression(token.expression(), suffix);
        if (prefixExpression.isEmpty() || suffixExpression.isEmpty()) {
            return unchanged(expression, new MathCaretSelection(position));
        }

        var parent = parentSequencePosition(position.tokenPath());
        var sequence = sequenceAt(expression, parent.sequencePath());
        var children = new ArrayList<>(sequence.children());
        children.remove(parent.childOffset());
        children.add(parent.childOffset(), prefixExpression.get());
        children.add(parent.childOffset() + 1, fraction);
        children.add(parent.childOffset() + 2, suffixExpression.get());
        var updated = replaceSequence(expression, parent.sequencePath(), children);
        var insertedPath = parent.sequencePath().append(new SequenceChild(parent.childOffset() + 1));
        return new MathEditResult(
                updated,
                new MathCaretSelection(new MathSequencePosition(insertedPath.append(new FractionNumerator()), 0)),
                true);
    }

    private MathEditResult wrapSingleAtomRangeInScript(MathExpression expression, MathRangeSelection selection, ScriptSlot slot) {
        var range = normalizeRange(expression, selection);
        if (!(range.start() instanceof MathSequencePosition start)
                || !(range.end() instanceof MathSequencePosition end)
                || !start.sequencePath().equals(end.sequencePath())
                || end.childOffset() != start.childOffset() + 1) {
            return unchanged(expression, selection);
        }
        validateSequencePosition(expression, start);
        validateSequencePosition(expression, end);
        var sequence = sequenceAt(expression, start.sequencePath());
        var selected = sequence.children().get(start.childOffset());
        if (!isScriptableBase(selected)) {
            return unchanged(expression, selection);
        }
        var script = scriptWithSlot(selected, slot);
        var children = new ArrayList<>(sequence.children());
        children.set(start.childOffset(), script);
        var updated = replaceSequence(expression, start.sequencePath(), children);
        var scriptPath = start.sequencePath().append(new SequenceChild(start.childOffset()));
        return new MathEditResult(
                updated,
                new MathCaretSelection(new MathSequencePosition(scriptPath.append(pathSegmentFor(slot)), 0)),
                true);
    }

    private MathEditResult insertOrEnterExistingScriptSlot(
            MathExpression expression,
            MathSequencePosition parentPositionAfterScript,
            MathPath scriptPath,
            MathScript script,
            ScriptSlot slot
    ) {
        var slotPath = scriptPath.append(pathSegmentFor(slot));
        if (slotExpression(script, slot).isPresent()) {
            return moved(expression, sequenceEnd(expression, slotPath));
        }
        var updatedScript = withSlot(script, slot, new MathSequence(List.of()));
        var sequence = sequenceAt(expression, parentPositionAfterScript.sequencePath());
        var children = new ArrayList<>(sequence.children());
        children.set(parentPositionAfterScript.childOffset() - 1, updatedScript);
        var updated = replaceSequence(expression, parentPositionAfterScript.sequencePath(), children);
        return new MathEditResult(
                updated,
                new MathCaretSelection(new MathSequencePosition(slotPath, 0)),
                true);
    }

    private MathEditResult insertOrEnterRootIndex(
            MathExpression expression,
            MathSequencePosition parentPositionAfterRoot,
            MathPath rootPath,
            MathRoot root
    ) {
        var indexPath = rootPath.append(new RootIndex());
        if (root.index().isPresent()) {
            return moved(expression, sequenceEnd(expression, indexPath));
        }
        var updatedRoot = new MathRoot(root.radicand(), Optional.of(new MathSequence(List.of())));
        var sequence = sequenceAt(expression, parentPositionAfterRoot.sequencePath());
        var children = new ArrayList<>(sequence.children());
        children.set(parentPositionAfterRoot.childOffset() - 1, updatedRoot);
        var updated = replaceSequence(expression, parentPositionAfterRoot.sequencePath(), children);
        return new MathEditResult(
                updated,
                new MathCaretSelection(new MathSequencePosition(indexPath, 0)),
                true);
    }

    private Optional<MathEditResult> removeEmptyScriptSlotAtStart(MathExpression expression, MathSequencePosition position) {
        if (position.childOffset() != 0 || position.sequencePath().segments().isEmpty()) {
            return Optional.empty();
        }
        var slotSegment = position.sequencePath().last();
        if (!(slotSegment instanceof ScriptSubscript) && !(slotSegment instanceof ScriptSuperscript)) {
            return Optional.empty();
        }
        var slotSequence = sequenceAt(expression, position.sequencePath());
        if (!slotSequence.children().isEmpty()) {
            return Optional.empty();
        }
        var scriptPath = position.sequencePath().parent();
        var script = script(nodeAt(expression, scriptPath));
        var removingSubscript = slotSegment instanceof ScriptSubscript;
        var updatedSubscript = removingSubscript ? Optional.<MathExpression>empty() : script.subscript();
        var updatedSuperscript = removingSubscript ? script.superscript() : Optional.<MathExpression>empty();
        var parent = parentSequencePosition(scriptPath);
        MathExpression replacement = updatedSubscript.isEmpty() && updatedSuperscript.isEmpty()
                ? script.base()
                : new MathScript(script.base(), updatedSubscript, updatedSuperscript);
        var updated = replaceNode(expression, scriptPath, replacement);
        return Optional.of(new MathEditResult(
                updated,
                new MathCaretSelection(new MathSequencePosition(parent.sequencePath(), parent.childOffset() + 1)),
                true));
    }

    private Optional<MathEditResult> removeEmptyRootIndexAtStart(MathExpression expression, MathSequencePosition position) {
        if (position.childOffset() != 0 || position.sequencePath().segments().isEmpty()) {
            return Optional.empty();
        }
        if (!(position.sequencePath().last() instanceof RootIndex)) {
            return Optional.empty();
        }
        var indexSequence = sequenceAt(expression, position.sequencePath());
        if (!indexSequence.children().isEmpty()) {
            return Optional.empty();
        }
        var rootPath = position.sequencePath().parent();
        var root = root(nodeAt(expression, rootPath));
        var updated = replaceNode(expression, rootPath, new MathRoot(root.radicand(), Optional.empty()));
        var parent = parentSequencePosition(rootPath);
        return Optional.of(new MathEditResult(
                updated,
                new MathCaretSelection(new MathSequencePosition(parent.sequencePath(), parent.childOffset() + 1)),
                true));
    }

    private Optional<MathEditResult> removeEmptyGroupAtStart(MathExpression expression, MathSequencePosition position) {
        if (position.childOffset() != 0 || position.sequencePath().segments().isEmpty()) {
            return Optional.empty();
        }
        if (!(position.sequencePath().last() instanceof GroupContent)) {
            return Optional.empty();
        }
        var content = sequenceAt(expression, position.sequencePath());
        if (!content.children().isEmpty()) {
            return Optional.empty();
        }
        var groupPath = position.sequencePath().parent();
        var parent = parentSequencePosition(groupPath);
        var updated = removeAtom(expression, parent.sequencePath(), parent.childOffset()).expression();
        return Optional.of(new MathEditResult(
                updated,
                new MathCaretSelection(new MathSequencePosition(parent.sequencePath(), parent.childOffset())),
                true));
    }

    private static MathScript scriptWithSlot(MathExpression base, ScriptSlot slot) {
        return slot == ScriptSlot.SUBSCRIPT
                ? new MathScript(base, Optional.of(new MathSequence(List.of())), Optional.empty())
                : new MathScript(base, Optional.empty(), Optional.of(new MathSequence(List.of())));
    }

    private static MathScript withSlot(MathScript script, ScriptSlot slot, MathExpression expression) {
        return slot == ScriptSlot.SUBSCRIPT
                ? new MathScript(script.base(), Optional.of(expression), script.superscript())
                : new MathScript(script.base(), script.subscript(), Optional.of(expression));
    }

    private static Optional<MathExpression> slotExpression(MathScript script, ScriptSlot slot) {
        return slot == ScriptSlot.SUBSCRIPT ? script.subscript() : script.superscript();
    }

    private static MathPathSegment pathSegmentFor(ScriptSlot slot) {
        return slot == ScriptSlot.SUBSCRIPT ? new ScriptSubscript() : new ScriptSuperscript();
    }

    private static MathPathSegment lastPresentScriptSlot(MathScript script) {
        return script.superscript().isPresent() ? new ScriptSuperscript() : new ScriptSubscript();
    }

    private Optional<MathExpression> splitTokenExpression(MathExpression originalToken, String text) {
        if (text.isEmpty()) {
            return Optional.empty();
        }
        if (originalToken instanceof MathNumber && isValidNumber(text)) {
            return Optional.of(new MathNumber(text));
        }
        if (originalToken instanceof MathIdentifier && isValidIdentifier(text)) {
            return Optional.of(new MathIdentifier(text));
        }
        return Optional.empty();
    }

    private static Optional<String> simpleText(MathExpression expression) {
        if (expression instanceof MathIdentifier identifier) {
            return Optional.of(identifier.name());
        }
        if (expression instanceof MathNumber number) {
            return Optional.of(number.content());
        }
        if (expression instanceof MathNamedOperator namedOperator) {
            return Optional.of(namedOperator.name());
        }
        if (expression instanceof MathText text) {
            return Optional.of(text.content());
        }
        return Optional.empty();
    }

    public static boolean isValidSemanticTokenContent(SemanticMathTokenKind kind, String content) {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(content, "content");
        if (content.isBlank()) {
            return false;
        }
        return kind != SemanticMathTokenKind.MATH_TEXT || content.equals(content.strip());
    }

    private MathEditResult insertDecimalAtSequencePosition(MathExpression expression, MathSequencePosition position) {
        validateSequencePosition(expression, position);
        var coalesced = coalesceNumber(expression, position, ".");
        return coalesced.orElseGet(() -> unchanged(expression, new MathCaretSelection(position)));
    }

    private Optional<MathEditResult> coalesceNumber(MathExpression expression, MathSequencePosition position, String text) {
        var sequence = sequenceAt(expression, position.sequencePath());
        if (position.childOffset() > 0 && sequence.children().get(position.childOffset() - 1) instanceof MathNumber previous) {
            var candidate = previous.content() + text;
            if (isValidNumber(candidate)) {
                var children = new ArrayList<>(sequence.children());
                children.set(position.childOffset() - 1, new MathNumber(candidate));
                return Optional.of(new MathEditResult(
                        replaceSequence(expression, position.sequencePath(), children),
                        new MathCaretSelection(new MathSequencePosition(position.sequencePath(), position.childOffset())),
                        true));
            }
        }
        return Optional.empty();
    }

    private MathEditResult insertInsideToken(MathExpression expression, MathTokenPosition position, char character) {
        validateTokenPosition(expression, position);
        var token = tokenAt(expression, position.tokenPath());
        var original = token.text();
        var updatedText = MathTextBoundary.substring(original, 0, position.characterOffset())
                + character
                + MathTextBoundary.substring(original, position.characterOffset(), token.length());
        if (token.expression() instanceof MathNumber && !isValidNumber(updatedText)) {
            return unchanged(expression, new MathCaretSelection(position));
        }
        if (token.expression() instanceof MathIdentifier && !isIdentifierCharacter(character)) {
            return unchanged(expression, new MathCaretSelection(position));
        }
        var updatedExpression = token.expression() instanceof MathNumber
                ? new MathNumber(updatedText)
                : new MathIdentifier(updatedText);
        var updated = replaceNode(expression, position.tokenPath(), updatedExpression);
        var updatedOffset = position.characterOffset() + 1;
        return new MathEditResult(
                updated,
                new MathCaretSelection(canonicalTokenPosition(position.tokenPath(), updatedOffset, MathTextBoundary.characterCount(updatedText))),
                true);
    }

    private MathEditResult deleteTokenCharacter(MathExpression expression, MathTokenPosition position, int relativeOffset) {
        validateTokenPosition(expression, position);
        var token = tokenAt(expression, position.tokenPath());
        var deleteOffset = position.characterOffset() + relativeOffset;
        if (deleteOffset < 0 || deleteOffset >= token.length()) {
            return unchanged(expression, new MathCaretSelection(position));
        }
        var updatedText = MathTextBoundary.substring(token.text(), 0, deleteOffset)
                + MathTextBoundary.substring(token.text(), deleteOffset + 1, token.length());
        var parent = parentSequencePosition(position.tokenPath());
        if (updatedText.isEmpty()) {
            return removeAtom(expression, parent.sequencePath(), parent.childOffset());
        }
        var updatedExpression = token.expression() instanceof MathNumber
                ? new MathNumber(updatedText)
                : new MathIdentifier(updatedText);
        var updated = replaceNode(expression, position.tokenPath(), updatedExpression);
        var nextOffset = relativeOffset < 0 ? deleteOffset : position.characterOffset();
        return new MathEditResult(
                updated,
                new MathCaretSelection(canonicalTokenPosition(position.tokenPath(), nextOffset, MathTextBoundary.characterCount(updatedText))),
                true);
    }

    private MathEditResult removeAtom(MathExpression expression, MathPath sequencePath, int atomIndex) {
        var sequence = sequenceAt(expression, sequencePath);
        if (isDirectScriptBasePath(sequencePath) && sequence.children().size() <= 1) {
            return unchanged(expression, new MathCaretSelection(new MathSequencePosition(sequencePath, atomIndex)));
        }
        var children = new ArrayList<>(sequence.children());
        children.remove(atomIndex);
        return new MathEditResult(
                replaceSequence(expression, sequencePath, children),
                new MathCaretSelection(new MathSequencePosition(sequencePath, atomIndex)),
                true);
    }

    private MathEditResult moveLeftOutOfContainer(MathExpression expression, MathSequencePosition position) {
        if (position.sequencePath().equals(MathPath.ROOT)) {
            return unchanged(expression, new MathCaretSelection(position));
        }
        var slot = position.sequencePath().last();
        var fractionPath = position.sequencePath().parent();
        if (slot instanceof FractionNumerator) {
            var parent = parentSequencePosition(fractionPath);
            return moved(expression, new MathSequencePosition(parent.sequencePath(), parent.childOffset()));
        }
        if (slot instanceof FractionDenominator) {
            return moved(expression, sequenceEnd(expression, fractionPath.append(new FractionNumerator())));
        }
        if (slot instanceof RootRadicand) {
            var parent = parentSequencePosition(fractionPath);
            return moved(expression, new MathSequencePosition(parent.sequencePath(), parent.childOffset()));
        }
        if (slot instanceof RootIndex) {
            var parent = parentSequencePosition(fractionPath);
            return moved(expression, new MathSequencePosition(parent.sequencePath(), parent.childOffset()));
        }
        if (slot instanceof GroupContent) {
            var parent = parentSequencePosition(fractionPath);
            return moved(expression, new MathSequencePosition(parent.sequencePath(), parent.childOffset()));
        }
        if (slot instanceof ScriptBase) {
            var parent = parentSequencePosition(fractionPath);
            return moved(expression, new MathSequencePosition(parent.sequencePath(), parent.childOffset()));
        }
        if (slot instanceof ScriptSubscript) {
            return moved(expression, sequenceEnd(expression, fractionPath.append(new ScriptBase())));
        }
        if (slot instanceof ScriptSuperscript) {
            var script = script(nodeAt(expression, fractionPath));
            if (script.subscript().isPresent()) {
                return moved(expression, sequenceEnd(expression, fractionPath.append(new ScriptSubscript())));
            }
            return moved(expression, sequenceEnd(expression, fractionPath.append(new ScriptBase())));
        }
        return unchanged(expression, new MathCaretSelection(position));
    }

    private MathEditResult moveRightOutOfContainer(MathExpression expression, MathSequencePosition position) {
        if (position.sequencePath().equals(MathPath.ROOT)) {
            return unchanged(expression, new MathCaretSelection(position));
        }
        var slot = position.sequencePath().last();
        var fractionPath = position.sequencePath().parent();
        if (slot instanceof FractionNumerator) {
            return moved(expression, new MathSequencePosition(fractionPath.append(new FractionDenominator()), 0));
        }
        if (slot instanceof FractionDenominator) {
            var parent = parentSequencePosition(fractionPath);
            return moved(expression, new MathSequencePosition(parent.sequencePath(), parent.childOffset() + 1));
        }
        if (slot instanceof RootRadicand) {
            var parent = parentSequencePosition(fractionPath);
            return moved(expression, new MathSequencePosition(parent.sequencePath(), parent.childOffset() + 1));
        }
        if (slot instanceof RootIndex) {
            return moved(expression, new MathSequencePosition(fractionPath.append(new RootRadicand()), 0));
        }
        if (slot instanceof GroupContent) {
            var parent = parentSequencePosition(fractionPath);
            return moved(expression, new MathSequencePosition(parent.sequencePath(), parent.childOffset() + 1));
        }
        if (slot instanceof ScriptBase) {
            var script = script(nodeAt(expression, fractionPath));
            if (script.subscript().isPresent()) {
                return moved(expression, new MathSequencePosition(fractionPath.append(new ScriptSubscript()), 0));
            }
            if (script.superscript().isPresent()) {
                return moved(expression, new MathSequencePosition(fractionPath.append(new ScriptSuperscript()), 0));
            }
        }
        if (slot instanceof ScriptSubscript) {
            var script = script(nodeAt(expression, fractionPath));
            if (script.superscript().isPresent()) {
                return moved(expression, new MathSequencePosition(fractionPath.append(new ScriptSuperscript()), 0));
            }
            var parent = parentSequencePosition(fractionPath);
            return moved(expression, new MathSequencePosition(parent.sequencePath(), parent.childOffset() + 1));
        }
        if (slot instanceof ScriptSuperscript) {
            var parent = parentSequencePosition(fractionPath);
            return moved(expression, new MathSequencePosition(parent.sequencePath(), parent.childOffset() + 1));
        }
        return unchanged(expression, new MathCaretSelection(position));
    }

    private MathSequencePosition sequenceEnd(MathExpression expression, MathPath sequencePath) {
        return new MathSequencePosition(sequencePath, sequenceAt(expression, sequencePath).children().size());
    }

    private MathSequencePosition parentSequencePosition(MathPath childPath) {
        var last = childPath.last();
        if (!(last instanceof SequenceChild child)) {
            throw new IllegalArgumentException("Path does not identify a sequence child.");
        }
        return new MathSequencePosition(childPath.parent(), child.index());
    }

    private MathPosition canonicalTokenPosition(MathPath tokenPath, int offset, int tokenLength) {
        var parent = parentSequencePosition(tokenPath);
        if (offset <= 0) {
            return new MathSequencePosition(parent.sequencePath(), parent.childOffset());
        }
        if (offset >= tokenLength) {
            return new MathSequencePosition(parent.sequencePath(), parent.childOffset() + 1);
        }
        return new MathTokenPosition(tokenPath, offset);
    }

    private void validateSequencePosition(MathExpression expression, MathSequencePosition position) {
        var sequence = sequenceAt(expression, position.sequencePath());
        if (position.childOffset() > sequence.children().size()) {
            throw new IllegalArgumentException("childOffset is outside the sequence.");
        }
    }

    private void validateTokenPosition(MathExpression expression, MathTokenPosition position) {
        var token = tokenAt(expression, position.tokenPath());
        if (position.characterOffset() >= token.length()) {
            throw new IllegalArgumentException("MathTokenPosition is only valid strictly inside a token.");
        }
    }

    private void validatePosition(MathExpression expression, MathPosition position) {
        if (position instanceof MathSequencePosition sequencePosition) {
            validateSequencePosition(expression, sequencePosition);
        } else if (position instanceof MathTokenPosition tokenPosition) {
            validateTokenPosition(expression, tokenPosition);
        } else {
            throw new IllegalArgumentException("Unsupported math position: " + position.getClass().getName());
        }
    }

    private VirtualSequence sequenceAt(MathExpression expression, MathPath path) {
        var node = nodeAt(expression, path);
        if (node instanceof MathSequence sequence) {
            return new VirtualSequence(sequence.expressions(), true);
        }
        return new VirtualSequence(List.of(node), false);
    }

    private MathExpression nodeAt(MathExpression expression, MathPath path) {
        MathExpression current = expression;
        for (var segment : path.segments()) {
            if (segment instanceof SequenceChild child) {
                var sequence = current instanceof MathSequence actual
                        ? actual.expressions()
                        : List.of(current);
                if (child.index() >= sequence.size()) {
                    throw new IllegalArgumentException("Sequence child path is outside the expression.");
                }
                current = sequence.get(child.index());
            } else if (segment instanceof FractionNumerator) {
                current = fraction(current).numerator();
            } else if (segment instanceof FractionDenominator) {
                current = fraction(current).denominator();
            } else if (segment instanceof RootRadicand) {
                current = root(current).radicand();
            } else if (segment instanceof RootIndex) {
                current = root(current).index()
                        .orElseThrow(() -> new IllegalArgumentException("Path does not identify a present root index."));
            } else if (segment instanceof GroupContent) {
                current = group(current).content();
            } else if (segment instanceof ScriptBase) {
                current = script(current).base();
            } else if (segment instanceof ScriptSubscript) {
                current = script(current).subscript()
                        .orElseThrow(() -> new IllegalArgumentException("Path does not identify a present script subscript."));
            } else if (segment instanceof ScriptSuperscript) {
                current = script(current).superscript()
                        .orElseThrow(() -> new IllegalArgumentException("Path does not identify a present script superscript."));
            } else {
                throw new IllegalArgumentException("Unsupported math path segment: " + segment.getClass().getName());
            }
        }
        return current;
    }

    private MathExpression replaceSequence(MathExpression expression, MathPath sequencePath, List<MathExpression> children) {
        var current = nodeAt(expression, sequencePath);
        var replacement = current instanceof MathSequence || children.size() != 1
                ? new MathSequence(children)
                : children.get(0);
        return replaceNode(expression, sequencePath, replacement);
    }

    private MathExpression replaceNode(MathExpression expression, MathPath path, MathExpression replacement) {
        if (path.equals(MathPath.ROOT)) {
            return replacement;
        }
        var parentPath = path.parent();
        var last = path.last();
        var parent = nodeAt(expression, parentPath);
        MathExpression updatedParent;
        if (last instanceof SequenceChild child) {
            var sequence = parent instanceof MathSequence actual
                    ? actual.expressions()
                    : List.of(parent);
            if (child.index() >= sequence.size()) {
                throw new IllegalArgumentException("Sequence child path is outside the expression.");
            }
            var updatedChildren = new ArrayList<>(sequence);
            updatedChildren.set(child.index(), replacement);
            updatedParent = parent instanceof MathSequence || updatedChildren.size() != 1
                    ? new MathSequence(updatedChildren)
                    : updatedChildren.get(0);
        } else if (last instanceof FractionNumerator) {
            var fraction = fraction(parent);
            updatedParent = new MathFraction(replacement, fraction.denominator());
        } else if (last instanceof FractionDenominator) {
            var fraction = fraction(parent);
            updatedParent = new MathFraction(fraction.numerator(), replacement);
        } else if (last instanceof RootRadicand) {
            var root = root(parent);
            updatedParent = new MathRoot(replacement, root.index());
        } else if (last instanceof RootIndex) {
            var root = root(parent);
            if (root.index().isEmpty()) {
                throw new IllegalArgumentException("Path does not identify a present root index.");
            }
            updatedParent = new MathRoot(root.radicand(), Optional.of(replacement));
        } else if (last instanceof GroupContent) {
            var group = group(parent);
            updatedParent = new MathGroup(replacement, group.delimiter());
        } else if (last instanceof ScriptBase) {
            var script = script(parent);
            updatedParent = new MathScript(replacement, script.subscript(), script.superscript());
        } else if (last instanceof ScriptSubscript) {
            var script = script(parent);
            if (script.subscript().isEmpty()) {
                throw new IllegalArgumentException("Path does not identify a present script subscript.");
            }
            updatedParent = new MathScript(script.base(), Optional.of(replacement), script.superscript());
        } else if (last instanceof ScriptSuperscript) {
            var script = script(parent);
            if (script.superscript().isEmpty()) {
                throw new IllegalArgumentException("Path does not identify a present script superscript.");
            }
            updatedParent = new MathScript(script.base(), script.subscript(), Optional.of(replacement));
        } else {
            throw new IllegalArgumentException("Unsupported math path segment: " + last.getClass().getName());
        }
        return replaceNode(expression, parentPath, updatedParent);
    }

    private Token tokenAt(MathExpression expression, MathPath path) {
        var node = nodeAt(expression, path);
        if (node instanceof MathNumber number) {
            return new Token(number, number.content(), MathTextBoundary.characterCount(number.content()));
        }
        if (node instanceof MathIdentifier identifier) {
            return new Token(identifier, identifier.name(), MathTextBoundary.characterCount(identifier.name()));
        }
        throw new IllegalArgumentException("Path does not identify an editable math token.");
    }

    private static MathFraction fraction(MathExpression expression) {
        if (expression instanceof MathFraction fraction) {
            return fraction;
        }
        throw new IllegalArgumentException("Path does not identify a fraction.");
    }

    private static MathRoot root(MathExpression expression) {
        if (expression instanceof MathRoot root) {
            return root;
        }
        throw new IllegalArgumentException("Path does not identify a root.");
    }

    private static MathGroup group(MathExpression expression) {
        if (expression instanceof MathGroup group) {
            return group;
        }
        throw new IllegalArgumentException("Path does not identify a group.");
    }

    private static MathScript script(MathExpression expression) {
        if (expression instanceof MathScript script) {
            return script;
        }
        throw new IllegalArgumentException("Path does not identify a script.");
    }

    private static Optional<MathExpression> atomFor(char character) {
        if (Character.isDigit(character)) {
            return Optional.of(new MathNumber(String.valueOf(character)));
        }
        if (isLatinLetter(character)) {
            return Optional.of(new MathIdentifier(String.valueOf(character)));
        }
        return switch (character) {
            case '+' -> Optional.of(new MathOperator("+", MathOperatorRole.BINARY));
            case '-' -> Optional.of(new MathOperator("-", MathOperatorRole.BINARY));
            case '*' -> Optional.of(new MathOperator("*", MathOperatorRole.BINARY));
            case '/' -> Optional.of(new MathOperator("/", MathOperatorRole.BINARY));
            case '=' -> Optional.of(new MathOperator("=", MathOperatorRole.RELATION));
            case '(' -> Optional.of(new MathSymbol("(", MathSymbolKind.OTHER));
            case ')' -> Optional.of(new MathSymbol(")", MathSymbolKind.OTHER));
            default -> Optional.empty();
        };
    }

    private static boolean isLatinLetter(char character) {
        return (character >= 'a' && character <= 'z') || (character >= 'A' && character <= 'Z');
    }

    private static boolean isIdentifierCharacter(char character) {
        return Character.isLetter(character);
    }

    private static boolean isValidIdentifier(String value) {
        if (value.isBlank()) {
            return false;
        }
        for (var index = 0; index < value.length(); index++) {
            if (!Character.isLetter(value.charAt(index))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isValidNumber(String value) {
        if (value.isBlank()) {
            return false;
        }
        var decimalSeen = false;
        var digitSeen = false;
        for (var index = 0; index < value.length(); index++) {
            var character = value.charAt(index);
            if (Character.isDigit(character)) {
                digitSeen = true;
            } else if (character == '.') {
                if (decimalSeen) {
                    return false;
                }
                decimalSeen = true;
            } else {
                return false;
            }
        }
        return digitSeen;
    }

    private static boolean isToken(MathExpression expression) {
        return expression instanceof MathNumber || expression instanceof MathIdentifier;
    }

    private static boolean isSimpleAtom(MathExpression expression) {
        return expression instanceof MathNumber
                || expression instanceof MathIdentifier
                || expression instanceof MathNamedOperator
                || expression instanceof MathText
                || expression instanceof MathQuantity
                || expression instanceof MathOperator
                || expression instanceof MathSymbol;
    }

    private static boolean isScriptableBase(MathExpression expression) {
        return expression instanceof MathNumber
                || expression instanceof MathIdentifier
                || expression instanceof MathNamedOperator
                || expression instanceof MathText
                || expression instanceof MathQuantity
                || expression instanceof MathFraction
                || expression instanceof MathRoot
                || expression instanceof MathScript
                || expression instanceof MathGroup;
    }

    private static boolean isDirectScriptBasePath(MathPath path) {
        return !path.segments().isEmpty() && path.last() instanceof ScriptBase;
    }

    private static int tokenLength(MathExpression expression) {
        if (expression instanceof MathNumber number) {
            return MathTextBoundary.characterCount(number.content());
        }
        if (expression instanceof MathIdentifier identifier) {
            return MathTextBoundary.characterCount(identifier.name());
        }
        throw new IllegalArgumentException("Expression is not a token.");
    }

    private static MathPosition caret(MathSelection selection) {
        Objects.requireNonNull(selection, "selection");
        if (selection instanceof MathCaretSelection caretSelection) {
            return caretSelection.caret();
        }
        throw new IllegalArgumentException("Unsupported math selection: " + selection.getClass().getName());
    }

    private static MathSelection selection(MathPosition anchor, MathPosition active) {
        return anchor.equals(active)
                ? new MathCaretSelection(anchor)
                : new MathRangeSelection(anchor, active);
    }

    private static MathEditResult unchanged(MathExpression expression, MathSelection selection) {
        return new MathEditResult(expression, selection, false);
    }

    private static MathEditResult moved(MathExpression expression, MathPosition position) {
        return new MathEditResult(expression, new MathCaretSelection(position), false);
    }

    private record VirtualSequence(List<MathExpression> children, boolean realSequence) {
    }

    private record Token(MathExpression expression, String text, int length) {
    }

    private record SequenceBoundary(
            MathPath sequencePath,
            int removeIndex,
            int removeExclusive,
            Optional<MathExpression> prefix,
            Optional<MathExpression> suffix
    ) {
    }
}
