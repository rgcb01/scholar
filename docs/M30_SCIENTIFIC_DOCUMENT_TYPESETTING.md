# M30 - Scientific Document Typesetting

## Status

Technically complete. Final Minecraft visual/manual acceptance is pending. M30 does not claim
publication compliance for a specific IEEE venue, and no later milestone has started.

## Architecture Decision

M30 keeps four concerns separate:

```text
immutable semantic Document
    -> deterministic logical-unit layout
    -> paginated LaidOutDocument
    -> Minecraft rendering at transient viewport zoom
```

`DocumentSettings`, text/paragraph formatting, templates, spans, and manual `PageBreak` are semantic.
Page assignment, column positions, line fragments, derived TOC geometry, and page numbers are layout
results. Scroll, zoom, Fit Page, and Fit Width are screen state. Zoom is absent from `Document`,
persistence, transfer, and history, so it cannot change wrapping or page count.

The existing immutable AST, M24 transaction/selection contracts, M25 transfer pipeline, M26 bounded
codec, and M29 application shell remain the owners of their established responsibilities. Pagination
does not create semantic blocks at automatic page boundaries.

## Semantic Page Model

`Document` now owns `DocumentSettings` alongside blocks and datasets. Compatibility constructors
default to a blank Letter/portrait/one-column policy. Page configuration contains:

- `PaperSize`: Letter, A4, Legal, or validated custom dimensions.
- `PageOrientation`: portrait or landscape.
- `PageMargins`: deterministic top/right/bottom/left physical lengths.
- `ColumnLayout`: one or two columns plus a deterministic physical gap.
- `PageDecoration`: minimal header/footer text and page-number visibility.
- `DocumentTemplateId`: Blank or IEEE-style Scientific Paper.

`PhysicalLength` stores integer micrometres and converts to stable logical document units. Paper and
margin dimensions never use screen pixels. Orientation swaps the logical paper axes without changing
the physical `PaperSize` value.

`PageBreak` is a real semantic block. It persists, transfers, validates, participates in structural
editing, and creates one normal undoable edit. It is not represented by newlines or empty paragraphs.

`FigureBlock` and `TableBlock` own an explicit `ContentSpan`: `COLUMN` or `PAGE_WIDTH`. Existing
constructors default to `COLUMN`. M30 intentionally does not introduce floating placement.

## Pagination And Column Flow

`DocumentLayoutEngine.layoutPaginated` derives `LaidOutPage` and `LaidOutColumn` values in logical
units. Pages are vertically ordered with a fixed logical gap. Content flows down column one, then
column two, then to the next page. A text block remains one semantic/laid-out block even when its
lines occupy multiple columns or pages, preserving continuous caret and selection offsets.

Paragraph and heading lines may continue across automatic boundaries. Equations, tables, plots,
diagrams, and figures are placed atomically and move to the next available column/page when they fit
there. Figure captions remain inside their `FigureBlock`. A page-width Figure/Table in a two-column
document begins on a full-width page and normal column flow resumes on the following page.

`TableOfContentsBlock` remains semantic-without-entries. Its entries and paginated hit geometry are
recomputed from headings on every layout pass; no page assignment is persisted.

## Typography And Paragraph Formatting

`TextFormat` provides optional controlled `ScholarFontFamily` and half-point font size overrides.
The controlled family set uses Scholar-owned fonts only. `TextMark` now includes underline,
superscript, and subscript in addition to bold/italic. Split, merge, range formatting, persistence,
transfer, history, measurement, and Minecraft rendering preserve these values.

`Paragraph` owns a `SemanticStyle` and optional `ParagraphFormat`. The format supports left, center,
right, and justified alignment; line spacing; space before/after; left/right indentation; and
first-line indentation. Empty paragraphs are never inserted to represent spacing.

Style resolution is deterministic and layered:

```text
template defaults -> semantic style -> explicit paragraph/text overrides
```

The initial semantic style vocabulary includes Body Text, Title, Subtitle, Author, Affiliation,
Abstract, Keywords, Figure/Table Caption, Equation, Reference, Footnote, and Code. Headings remain
`Heading` nodes and retain their existing heading-level typography instead of being disguised as
styled paragraphs. Figure captions consume the Figure Caption semantic defaults without copying
those defaults into caption text nodes.

## Templates And Creation

`DocumentTemplates` is pure configuration plus a useful initial semantic structure. Production Home
offers Minecraft-native cards for Blank Document and IEEE-style Scientific Paper.

The IEEE-style preset uses Letter paper, publication-oriented margins, two columns, page numbering,
and semantic Title/Author/Affiliation/Abstract/Keywords/body/reference defaults. Its initial document
contains conventional scientific authoring prompts. It is intentionally named “IEEE-style”; it does
not claim conformance with every IEEE journal or conference template.

## Editor And Ribbon

The existing action system remains authoritative. Home adds semantic style, controlled font size and
family, underline, subscript/superscript, alignment, line spacing, and indentation commands. Insert
offers Page Break. The production Layout tab contains Page Setup (margins, orientation, size), Columns,
and Breaks. View exposes real outline and zoom/Fit commands.

Semantic formatting, page settings, and Page Break insertion are one history transaction and no-op
when they do not change the document. Zoom/Fit are transient screen operations and produce no history.

## Rendering And Page View

`MinecraftDocumentRenderer` consumes paginated output and draws distinct paper sheets, boundaries,
page gaps, headers/footers, page numbers, and already-positioned scientific content. Page sheets and
content are clipped to the document viewport. `ScholarEditorScreen` and `ScholarDocumentScreen` use
the paginated path in production. The legacy continuous layout entry point remains for compatibility
with existing focused layout tests and callers; it is not the production editor page model.

Font size affects measured width, line height, wrapping, paragraph height, and pagination.
Superscript/subscript scale and shift their rendered runs while preserving semantic offsets.

## Persistence Compatibility

Scholar JSON V2 stores only new semantic M30 state: document settings, paragraph style/format, text
format/new marks, Figure/Table span, and PageBreak. The codec retains a dedicated strict V1 read path.
V1 files load with deterministic blank settings and empty formatting overrides; opening alone does
not rewrite them. A later explicit save writes canonical V2. Corrupt values are rejected rather than
silently reinterpreted.

Page assignment, laid-out pages, zoom, scroll, selection, history, resolved labels, and renderer state
remain excluded.

## Transfer And Validation

M25 materialization preserves paragraph styles/formats, text formatting, spans, and PageBreak. Ordinary
block transfer does not carry source `DocumentSettings`, so destination paper/template policy remains
authoritative. Existing identity, reference degradation, resource closure, and diagram-local identity
rules are unchanged.

Pure-Java validation checks page/content geometry and column viability in addition to existing model
validation. Value constructors bound physical lengths, font sizes, line spacing, paragraph spacing,
and indentation. Layout-only constraints do not become persistence truth.

## Golden Scenario

`M30GoldenScenarioTest` uses the deterministic scientific QA document to verify an IEEE-style,
two-column, multipage document containing a manual break, local typography, equations, tables,
dataset-backed plots, Figures, electrical/mechanical diagrams, TOC, references, and a page-width
Figure. It persists and reopens the document, recomputes identical layout, transfers styled content
into a blank destination without its page settings, performs exact undo/redo, and validates both.

## Known Boundaries

- Oversized tables and scientific objects are atomic; M30 does not fragment table rows or split nested
  media across pages. Very large objects need a later explicit overflow/fragmentation policy.
- TOC entries paginate and remain navigable, but page-number display inside TOC entries is deferred.
- Equation numbering presentation is not added; current semantic equation IDs, derived references,
  and math layout remain intact.
- Header/footer editing is template/document configuration, not a rich nested editor.
- Only Figure and Table expose full-page span. Arbitrary floating placement and section-local column
  changes are outside M30.
- Layout remains synchronous full-document recomputation. Incremental pagination belongs to a measured
  future performance pass.

## Automated Verification

- Focused M30 tests cover physical units, A4/Letter/orientation, margins, pagination, continuation,
  columns, PageBreak, spans, style precedence, paragraph rhythm, justification, font metrics,
  decorations, zoom independence, cross-page hit testing, atomic media, transfer, history, V1
  migration, V2 round trip, template creation, save/reopen, and validation.
- Full suite result at the implementation checkpoint: 1,525 tests, zero failures, zero errors, zero
  skipped.
- Build, diff check, and Transfer/Persistence/Application boundary results are recorded in the final
  implementation response after their final execution.

## Manual Minecraft QA

Run `gradlew runClient`, enter a world, and execute `/scholar`.

1. Capture Home with both template cards.
2. Create Blank Document; capture its page sheet and Home ribbon.
3. Return Home, create IEEE-style Scientific Paper, and capture its two-column page.
4. Capture Home, Layout, and View ribbon tabs at normal width.
5. Use style/font/paragraph controls; verify undo/redo and that no-op commands add no history.
6. Switch Letter/A4/Legal, portrait/landscape, margins, and one/two columns.
7. Insert a Page Break and type around an automatic page boundary; verify deterministic reflow.
8. Select text across an automatic page boundary; verify one continuous semantic selection.
9. Open `/scholar_dev_editor`; inspect the multipage QA fixture, page-width Figure, equations,
   tables, plots, electrical/mechanical diagrams, TOC, and page numbers.
10. Capture a zoomed-out multipage view; Fit Page/Fit Width must not change wrapping or page count.
11. Save, return Home, reopen, and verify all semantic settings and formatting.
12. Copy styled/PageBreak content into another document and verify destination page settings remain.

Manual screenshots and acceptance must be supplied by the user; automated completion does not claim
that visual approval.
