# Scholar Math Specification

## Status

Current v0.1.

## Purpose

Scholar Math represents authored scientific and mathematical notation as structured value data. It is intended to support educational and scientific documents inside Scholar without storing equations as LaTeX strings and without turning Scholar into a computer algebra system.

The current scope includes the Minecraft-independent Math AST, a small Minecraft-independent math layout model, and the first display equation integration.

## Current v0.1 Node Model

All current math nodes live under `dev.rgcb.scholar.math`.

- `MathExpression`: common category for math AST nodes.
- `MathSequence`: ordered immutable sequence of math expressions.
- `MathNumber`: authored numeric content.
- `MathIdentifier`: explicitly authored mathematical identifiers, such as variables.
- `MathNamedOperator`: explicitly authored named mathematical operators.
- `MathText`: intentionally authored textual content embedded inside mathematical notation.
- `MathSymbol`: special mathematical glyphs or symbols that are not ordinary identifiers.
- `MathOperator`: authored operators and relations with a broad operator role.
- `MathFraction`: numerator over denominator.
- `MathScript`: base with subscript, superscript, or both.
- `MathRoot`: square root or indexed root.
- `MathGroup`: expression grouped by explicit delimiters.

Supporting enums:

- `MathOperatorRole`: `RELATION`, `BINARY`, `UNARY`, `PUNCTUATION`, `OTHER`.
- `MathSymbolKind`: `GREEK`, `CONSTANT`, `INFINITY`, `CALCULUS`, `OTHER`.
- `MathDelimiter`: `PARENTHESES`, `BRACKETS`, `BRACES`.

## Current v0.1 Layout Support

Visual math layout currently supports:

- `MathSequence`
- `MathNumber`
- `MathIdentifier`
- `MathNamedOperator`
- `MathText`
- `MathSymbol`
- `MathOperator`
- `MathFraction`
- `MathRoot` for square roots
- `MathScript`
- `MathGroup`

The layout pipeline is:

```text
MathExpression
-> MathLayoutEngine
-> LaidOutMath / MathBox tree
-> renderer boundary
```

Math layout uses a dedicated `MathTextMeasurer` that reports width, ascent, and descent. A `MathBox` reports width, ascent, and descent and may contain positioned child boxes plus minimal drawable primitives.

Current drawable primitives are:

- `MathGlyphRun`
- `MathHorizontalRule`
- `MathLineSegment`

`MathSequence` applies semantic inter-atom spacing while composing adjacent child boxes. Spacing is derived from neighboring expression categories and remains layout state only. Scholar does not insert whitespace expressions, mutate token content, or serialize layout-generated gaps.

The current spacing policy is a small Scholar-specific subset, not a TeX glue engine:

- `MathText` receives word-like exterior spacing from neighboring math atoms.
- `MathNamedOperator` remains compact before opening delimiters, as in `sin(x)`.
- `MathNamedOperator` receives a small gap before bare arguments and after preceding ordinary atoms, as in `sin x` or `2 sin(x)`.
- ordinary identifier adjacency remains compact, so `MathIdentifier("x")` followed by `MathIdentifier("y")` still lays out as `xy`.
- existing operator and relation spacing remains responsible for `+`, `/`, and `=`.

`MathFraction` is laid out as a true stacked fraction. The numerator and denominator are centered over a horizontal rule. The fraction width is based on the larger child width plus horizontal padding. The fraction baseline is placed at the rule region so surrounding sequence content, such as `v =`, aligns naturally with the fraction bar.

`MathRoot` square roots are laid out with a radical mark, presentation-only vinculum, and an editable radicand slot. When a root index is present, it is rendered as reduced math content to the upper-left of the radical rather than as a generic right-side subscript.

`MathScript` lays out its base at the normal math baseline and places present subscript and superscript slots to the lower-right and upper-right of the base. Subscript and superscript share the same horizontal origin after the base. Script text uses reduced presentation scale. Structural script content, such as a fraction used as an exponent, uses a more compact reduced scale so it reads as a script rather than a full-size equation beside the base.

`MathGroup` lays out authored delimiter pairs around an editable content slot. The initial renderer uses normal-size delimiter glyphs only; it does not stretch delimiters around tall content yet. Parenthesized groups participate in semantic spacing as opening delimiters, so named operators and adjacent atoms remain compact in examples such as `sin(x)`, `2(x + 1)`, and `x(y)`.

## Square Root Authoring v0.1

Square roots are authored explicitly through Scholar's equation editing actions. Typing `sqrt`, pasting `sqrt(x)`, or entering any other plain-text source never infers `MathRoot`.

For square roots, Scholar uses the existing AST node:

```text
MathRoot(radicand, Optional.empty())
```

For roots, the Subscript command immediately after a `MathRoot` targets `MathRoot.index` instead of creating `MathScript(MathRoot(...), subscript)`. This gives indexed roots their conventional left-side radical index while preserving normal subscript behavior for ordinary bases such as `x_i`.

The radicand may be `MathSequence(List.of())` while the user is authoring incomplete notation. No placeholder node, whitespace node, underscore symbol, or fake source character is stored in the AST.

Collapsed root insertion creates an empty square root and places the caret at the start of the radicand. Inserting root over a compatible math range wraps the selected fragment as the radicand and places the caret immediately after the new root. Unsupported cross-slot ranges remain non-mutating.

Horizontal navigation enters and exits the radicand deterministically:

- before root + Right enters radicand start;
- radicand start + Left returns before root;
- radicand end + Right moves after root;
- after root + Left returns to radicand end.

Deletion remains conservative. A caret immediately before or after a root does not delete the root. Selecting the whole root as an atom and pressing Delete or Backspace removes it as one structural node. Editing inside the radicand follows normal math slot behavior.

The plain-text fallback serializer writes square roots as `sqrt(...)`. Programmatic indexed roots may serialize as `root(index, radicand)`, but indexed root authoring and layout are outside the current scope.

## Script Authoring v0.1

Scripts are authored explicitly through equation editing. Typing `^` creates or enters a superscript slot, and typing `_` creates or enters a subscript slot. These are one-character structural editor commands, not LaTeX parsing. External plain-text paste of `x^2`, `x_i`, or similar syntax remains non-structural and is rejected by the current external importer.

For scripts, Scholar uses:

```text
MathScript(base, Optional<subscript>, Optional<superscript>)
```

`MathScript` owns its base. A present empty script slot is represented as `Optional.of(MathSequence(List.of()))`; an absent slot remains `Optional.empty()`. A persisted `MathScript` must always contain at least one present script slot.

M13D authoring creates scripts only from one complete atom. Eligible automatic bases are identifiers, numbers, named operators, math text, fractions, roots, and existing scripts. Operators and linear delimiter symbols are not automatic script bases. Multi-atom bases such as `(x + 1)^2` are deferred until explicit grouping is implemented.

If the caret is immediately after an existing script, requesting a missing complementary slot extends the same script. Requesting an already present slot re-enters that slot at its end without mutating the AST. Navigation uses deterministic order:

- base;
- subscript, if present;
- superscript, if present;
- after the script.

Backspace at the start of an empty script slot removes that slot. If no script slots remain, the `MathScript` unwraps to its base. Navigation alone does not remove empty slots. Caret-adjacent Delete/Backspace does not delete a whole script; it enters the script for editing. Selecting the whole script as a parent-sequence atom and deleting removes it as one structural node.

The plain-text fallback serializer writes scripts in readable linear form, such as `x^2`, `x_i`, `x_i^2`, `sqrt(x)^2`, and `(a / b)^2`. Slot contents with operators are parenthesized, such as `x^(a + b)` and `x_(i + 1)`. This remains a lossy fallback and is not parsed back into structure by the external importer.

## Group Authoring v0.1

Groups are authored explicitly through equation editing actions. Typing `(`, `)`, `[`, `]`, `{`, or `}` remains ordinary linear symbol authoring and never infers `MathGroup`.

For groups, Scholar uses:

```text
MathGroup(content, MathDelimiter)
```

`MathDelimiter` determines both presentation delimiters. Supported delimiter kinds are `PARENTHESES`, `BRACKETS`, and `BRACES`. Delimiter glyphs are derived from this enum during layout and serialization; they are not stored as separate `MathSymbol` children.

The editable group content is addressed by the `GroupContent` math path segment. New collapsed group insertion creates `MathGroup(MathSequence(List.of()), delimiter)` and places the caret inside the empty content. Inserting a group over a compatible range wraps complete sibling atoms from one sequence and places the caret after the new group. Partial-token and cross-slot ranges remain non-mutating.

Horizontal navigation enters and exits group content deterministically:

- before group + Right enters content start;
- content start + Left returns before group;
- content end + Right moves after group;
- after group + Left returns to content end.

Deletion is conservative. Backspace at the start of an empty `GroupContent` slot removes the empty group template. Delete inside an empty group is a no-op. Non-empty groups are not implicitly unwrapped. Selecting the whole group as a parent-sequence atom and deleting removes the group.

`MathGroup` is an eligible single-atom script base, enabling authored structures such as `(x + 1)^2` without raw multi-atom script bases. Native Scholar clipboard preserves groups structurally. The plain-text fallback serializer writes groups with their delimiters, such as `(x + 1)`, `[x]`, and `{}`. External plain-text paste still imports delimiters as linear symbols and does not infer structural groups.

## Display Equation Integration

Display equations enter the document model through `EquationBlock(MathExpression)`.

`EquationBlock` contains only the math expression. It does not contain alignment, numbering, labels, anchors, captions, or metadata.

Document layout delegates equation internals to `MathLayoutEngine`, places the resulting `LaidOutMath` in block flow, and centers equations when they fit within the available content width.

Oversized equations are not scaled, wrapped, or horizontally scrolled in v0.1. They keep their natural layout width and may be clipped by the existing document viewport. This is a temporary limitation, not a final educational-document behavior.

## Identifiers vs Symbols

`MathIdentifier` represents explicitly authored mathematical identifiers such as `x`, `v`, `t`, `θ`, or `λ`. Ordinary Greek variable letters conceptually belong to `MathIdentifier`. Multi-character identifiers such as `velocity` remain valid for explicit structured content, APIs, and native clipboard data, but ordinary keyboard authoring and external plain-text import do not infer multi-character identifiers from adjacent letters.

`MathNamedOperator` represents an explicitly authored named mathematical operator such as `sin`, `cos`, `log`, `lim`, `rank`, or `det`. It does not represent a function-call AST, evaluation semantics, or a hardcoded keyword enum. Ordinary typing and external plain-text import never silently create named operators.

`MathText` represents intentionally authored textual content embedded inside mathematical notation, such as `if`, `where`, `otherwise`, or `for all`. It is not document text, Markdown, a variable, a named operator, or an equation comment. Internal whitespace is preserved exactly, but leading and trailing whitespace are invalid so layout spacing remains separate from text content.

`MathSymbol` represents special mathematical glyphs or symbols such as `π`, `∞`, `∂`, or `∇`. `π` may be represented as a `CONSTANT` symbol, and `∞` as an `INFINITY` symbol; neither implies evaluation or constant folding. `MathSymbolKind.GREEK` remains for compatibility during the transitional symbol taxonomy period, but ordinary Greek variable letters should be authored as identifiers going forward.

`MathOperator` represents authored operator, relation, and punctuation-like operator behavior. Existing `+`, `-`, `*`, `/`, and `=` behavior remains unchanged.

The external plain-text importer is intentionally lexical and does not resolve the full identifier-versus-symbol taxonomy. Accepted Unicode letters from external text become identifier atoms unless a future explicit syntax or authoring action creates a different structured node.

## Math Clipboard v0.1

Math clipboard operations preserve Scholar-native structure only within the current Minecraft client process. Copy and Cut place a deterministic, human-readable plain-text fallback on the OS clipboard while keeping an in-memory structured sidecar when the OS clipboard write succeeds.

The structured math payload stores semantic content only: a canonical `MathSequence` fragment. It does not store editor paths, block indices, selection direction, layout geometry, IDs, history state, Markdown, LaTeX, or serialized Java objects.

Structured Paste is used only when the current OS clipboard text exactly matches the active Scholar sidecar snapshot. If the text differs, the sidecar is discarded and math paste falls back to the external plain-text math importer. Because Minecraft/GLFW exposes only string clipboard transport, Scholar cannot distinguish a later external copy of identical text from the original Scholar copy in v0.1.

The plain-text fallback is intentionally lossy. Structural fractions and linear slash expressions may both appear as readable `/` text. Default Copy does not emit LaTeX, does not emit Markdown math, and does not embed hidden metadata.

## External Plain-Text Math Import v0.1

External plain-text paste into an active equation uses a bounded lexical importer. It constructs Math AST nodes directly and does not use Markdown, LaTeX, a computer algebra system, or the plain-text serializer in reverse.

Native Scholar clipboard sidecar data has priority. If the OS clipboard still matches a Scholar sidecar snapshot, Scholar pastes the native structured payload and does not run the external importer. If the sidecar is absent or stale, Scholar attempts external import.

The v0.1 importer accepts simple authored notation only:

- numbers with digits and at most one `.`, such as `12`, `3.14`, `.14`, and `3.`;
- ordinary Unicode letter atoms as individual identifiers, such as `x`, `y`, `θ`, and `Δ`;
- `+`, `-`, `*`, `/`, and `=`;
- `(` and `)` as linear `MathSymbol` nodes;
- whitespace, tabs, and newlines as token separators;
- the explicit Unicode operator normalizations `− -> -`, `× -> *`, and `÷ -> /`.

Whitespace is not stored in the AST. It separates lexical input but does not create a node, so `mass velocity` imports as adjacent single-letter identifier atoms and `12 34` imports as two numbers. Case and identifier Unicode are preserved as authored text. Scholar does not apply broad Unicode normalization.

The importer is lexical, not algebraic. It does not infer multiplication, functions, precedence, validity, or simplification. `2x` imports as adjacent `MathNumber("2")` and `MathIdentifier("x")`; `xy` imports as adjacent `MathIdentifier("x")` and `MathIdentifier("y")`; `sin(x)` imports as ordinary letter identifiers followed by linear parenthesis symbols. Adjacency preserves authored notation without inserting an implicit multiplication operator.

External `/` remains linear division and never creates `MathFraction`. Structural fractions are created by Scholar's explicit fraction authoring action or restored through native sidecar paste.

External text import produces only `MathSequence`, `MathNumber`, `MathIdentifier`, `MathOperator`, and `MathSymbol`. It does not produce `MathNamedOperator`, `MathText`, `MathFraction`, `MathScript`, `MathRoot`, `MathGroup`, or future structural nodes.

Import is atomic. Empty input, oversized input, unsupported characters, unsupported syntax such as `^`, `_`, comma decimals, or scientific notation, and token overflows produce no AST fragment and no editor mutation. The v0.1 bounds are 4096 UTF-16 code units of input and 512 produced tokens.

## Explicit Semantic Token Authoring v0.1

Semantic math tokens are authored explicitly. Ordinary keyboard input and external plain-text paste continue to create ordinary lexical math atoms and never infer `MathNamedOperator` or `MathText` from strings such as `sin`, `rank`, `if`, or `where`.

The first authoring workflow is conversion-oriented:

```text
ordinary selected math atoms
-> explicit semantic token command
-> compact semantic token popup
-> one semantic AST atom
```

The conversion source must be a non-empty, same-sequence math range made from simple textual atoms. `MathIdentifier`, `MathNumber`, existing `MathNamedOperator`, and existing `MathText` can contribute their textual content. Operators, delimiter symbols, fractions, scripts, roots, groups, and cross-slot structural ranges are not converted.

The semantic token popup edits exactly two properties:

- type: `MathNamedOperator` or `MathText`;
- content: one string.

`MathNamedOperator` supports arbitrary non-blank authored names such as `sin`, `rank`, `det`, `trace`, or `foo`; it does not imply function-call semantics or evaluation. `MathText` supports intentionally authored text such as `if`, `where`, `otherwise`, or `for all`; internal whitespace is preserved and leading or trailing whitespace remains invalid.

Applying the popup replaces the selected eligible source with one immutable semantic node, records one history transaction, and collapses the math caret immediately after the inserted node. Canceling or editing popup fields does not mutate the document or create history.

`MathNamedOperator` and `MathText` are atomic in the initial editor slice. The caret can sit before or after the semantic token, Shift selection can select the whole token, and Backspace/Delete remove the whole token when adjacent. There is no internal per-character caret inside semantic tokens yet. Typing while a semantic token is selected replaces it with ordinary math input, providing a simple escape path without a dedicated reverse-conversion command.

Native Scholar structured clipboard preserves semantic tokens exactly. Plain clipboard fallback remains only the textual content, and external plain-text paste remains non-semantic.

## Structural Validation

Scholar Math validates structure only:

- required values must not be null;
- collections must not be null;
- collection elements must not be null;
- exposed collections must be immutable defensive copies;
- textual values for numbers, identifiers, named operators, math text, symbols, and operators must not be blank;
- math text must not contain leading or trailing whitespace;
- `MathScript` must contain at least one of subscript or superscript;
- fractions require both numerator and denominator;
- roots require a radicand;
- groups require content and a delimiter.

## Explicit Non-Goals

Current v0.1 does not:

- determine whether equations are true;
- solve equations;
- simplify expressions;
- validate units or dimensions;
- define operator precedence;
- parse LaTeX-like syntax;
- integrate math into Scholar Markdown;
- provide inline math;
- provide editor placeholders or cursor behavior.

Scholar can represent intentionally incorrect authored content such as `2 + 2 = 5`.

## Future Direction

Future milestones may add inline math, stretch delimiter layout, a LaTeX-like input parser, Markdown math syntax, canonical serialization, matrices, sums, integrals, limits, vectors, derivatives, accents, chemistry notation, scientific units, and better oversized-equation behavior.

Those future features should build on the Math AST rather than replacing it with stored syntax.
