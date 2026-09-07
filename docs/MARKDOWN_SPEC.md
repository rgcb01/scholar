# Scholar Markdown v0.1

Status: Current v0.1

Scholar Markdown is a small Markdown interchange format for the Scholar document model. Markdown is not Scholar's internal source of truth.

This specification is intentionally limited to constructs that the current document model can represent:

- Plain text
- Paragraphs
- ATX headings levels 1 through 6
- Bold text
- Italic text
- Bold italic text
- Restricted pipe tables with one semantic header row

Unsupported Markdown syntax is not assigned Scholar-specific semantics.

## Parsing Model

Scholar Markdown v0.1 should parse Markdown into a `ParseResult` containing:

- a `Document`;
- zero or more diagnostics.

Diagnostics are for recoverable parse concerns such as unsupported syntax, malformed emphasis, or suspicious heading syntax. The parser should preserve user text whenever practical rather than rejecting the entire document.

The semantic document model must not store Markdown source positions.

## Line Endings

Parsers must treat LF and CRLF identically. Serializer output should use LF.

## Blank Lines

Blank lines separate block nodes. A blank line is a line containing only whitespace.

Multiple blank lines are equivalent to one block separator.

Leading and trailing blank lines do not create empty blocks.

## Paragraphs

A paragraph is one or more non-blank physical lines that do not start a supported heading.

Multiple physical lines in one paragraph are joined with a single space.

Example:

```md
This is one
paragraph.
```

Conceptual output:

```text
Paragraph
└── Text("This is one paragraph.", marks=[])
```

This keeps normal Markdown paragraph expectations while avoiding layout-specific hard line breaks in the semantic model.

## Headings

Supported headings are ATX headings with 1 through 6 leading `#` characters followed by at least one whitespace character.

Examples:

```md
# Heading 1
## Heading 2
###### Heading 6
```

Conceptual output:

```text
Heading(level=1)
└── Text("Heading 1", marks=[])
```

Heading content may contain the same inline formatting as paragraph content.

Trailing closing `#` characters are not supported as special heading syntax in v0.1. They are parsed as normal heading content when the leading heading marker is otherwise valid. The serializer must not emit closing `#` characters as a formatting convention.

Seven or more leading `#` characters do not create a heading. They should be parsed as paragraph text and reported with a diagnostic.

Lines beginning with `#` but lacking the required following whitespace do not create headings. They should be parsed as paragraph text and reported with a diagnostic.

Empty headings are allowed when the line contains only a valid heading marker and whitespace. This avoids speculative content restrictions in the parser.

## Inline Formatting

Scholar Markdown v0.1 supports only `*`-based emphasis delimiters.

Supported forms:

```md
**bold**
*italic*
***bold italic***
```

Conceptual output:

```text
Text("bold", marks=[BOLD])
Text("italic", marks=[ITALIC])
Text("bold italic", marks=[BOLD, ITALIC])
```

Empty emphasis is not treated as formatting. Delimiters for empty emphasis should remain literal text and may produce a diagnostic.

Unmatched delimiters remain literal text and should produce a diagnostic.

Nested or overlapping emphasis is not supported in v0.1. The parser preserves unsupported nested structures as literal text with diagnostics rather than inventing partial semantics.

Examples:

```md
This is **bold** text.
This is *italic* text.
This is ***both*** text.
```

Supported.

```md
This is **bold and *italic*** text.
This is *italic and **bold*** text.
```

Not supported as nested emphasis in v0.1. Preserve content as text and report diagnostics.

## Escaping

Backslash escaping is supported for Markdown control characters used by v0.1:

- `\#`
- `\*`
- `\\`

Escapes are interpreted in block and inline contexts where relevant.

Examples:

```md
\# not a heading
\*not italic\*
```

Conceptual output:

```text
Paragraph
└── Text("# not a heading", marks=[])

Paragraph
└── Text("*not italic*", marks=[])
```

Backslashes before unsupported escape targets should be preserved as literal text.

## Tables

Scholar Markdown v0.1 supports a deliberately restricted GitHub-style pipe-table subset for `TableBlock` interchange.

Supported table syntax requires:

- an outer pipe at the start and end of every table row;
- a header row followed immediately by a separator row;
- one or more columns;
- rectangular rows with a matching cell count;
- separator cells containing only three or more hyphens;
- cell content limited to the currently supported inline content.

Example:

```md
| Quantity | Value | Unit |
| --- | --- | --- |
| Voltage | 12 | V |
| Current | 2 | A |
```

Conceptual output:

```text
TableBlock(headerRowCount=1)
├── Row 0, semantic header
│   ├── Cell Text("Quantity")
│   ├── Cell Text("Value")
│   └── Cell Text("Unit")
├── Row 1
│   ├── Cell Text("Voltage")
│   ├── Cell Text("12")
│   └── Cell Text("V")
└── Row 2
    ├── Cell Text("Current")
    ├── Cell Text("2")
    └── Cell Text("A")
```

The separator row is syntax only and is not stored in the document model.

Markdown table headers map to semantic table header rows. Header cells are not authored as Bold unless the source cell explicitly contains Bold markup such as `**Quantity**`.

Empty cells and empty header cells are allowed. Empty cells are represented as empty `InlineContent`, not `Text("")`.

Spaces around cell content are treated as table syntax padding and trimmed during parsing. Internal spaces and supported escapes inside cell content are preserved.

Literal pipes in cell text must be escaped as `\|`. Backslash escaping for `#`, `*`, `\`, and `|` composes with normal inline parsing.

The parser rejects malformed table syntax with diagnostics rather than building partial tables. Malformed cases include:

- separator/header column count mismatch;
- body rows with the wrong cell count;
- separator cells with fewer than three hyphens;
- alignment markers such as `:---`, `---:`, or `:---:`.

Table parsing ends at a blank line or the next non-table block. A line containing `|` is not a table unless a valid pipe-table header row is immediately followed by a valid separator row.

Headerless `TableBlock` values cannot be serialized to Markdown tables in v0.1. The serializer must fail explicitly rather than promoting row 0 to a header or synthesizing a blank header.

## Unsupported Syntax

Unsupported Markdown syntax should be preserved as literal text and reported with diagnostics when it is recognizable.

Examples include:

- Links
- Images
- Lists
- Blockquotes
- Code blocks
- Inline code
- Horizontal rules
- HTML
- Footnotes
- Task lists
- Strikethrough
- Math syntax
- Diagrams
- Front matter
- Citations
- Reference links
- Autolinks
- Embedded Minecraft content

## Canonical Serialization

Serialization from `Document` to Scholar Markdown v0.1 should be deterministic.

Rules:

- Use LF line endings.
- Separate block nodes with exactly one blank line.
- Emit headings as `#` repeated by heading level, one space, then serialized inline content.
- Emit paragraphs as serialized inline content on one physical line.
- Emit `BOLD` text as `**text**`.
- Emit `ITALIC` text as `*text*`.
- Emit text with both marks as `***text***`.
- Emit headered tables with outer pipes, one space inside pipe boundaries, `---` separator cells, and no alignment markers.
- Escape literal `#`, `*`, `\`, and `|` where required to preserve parse results.
- End output with exactly one trailing newline.
- Do not preserve arbitrary original Markdown spacing or equivalent source formatting.

## Round-Trip Contract

The primary invariant is:

```text
Document -> serialize -> parse -> equivalent Document
```

Markdown source does not need to serialize back to identical bytes. Equivalent documents are equal by document model value semantics.

## Open Decisions

- Future Markdown versions may decide whether to support trailing ATX closing `#` syntax.
- Future Markdown versions may decide whether to support nested or overlapping emphasis.
- Future Markdown versions may expand diagnostics with source positions outside the semantic document model.
- Future Markdown versions may decide whether to support alignment, headerless tables, multiline cells, Markdown math in cells, or richer table interchange.
