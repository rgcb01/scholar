package dev.rgcb.scholar.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import dev.rgcb.scholar.data.*;
import dev.rgcb.scholar.diagram.*;
import dev.rgcb.scholar.document.*;
import dev.rgcb.scholar.electrical.*;
import dev.rgcb.scholar.math.*;
import dev.rgcb.scholar.mechanical.*;
import dev.rgcb.scholar.plot.*;
import dev.rgcb.scholar.quantity.*;
import dev.rgcb.scholar.compute.*;
import dev.rgcb.scholar.validation.DocumentValidator;
import dev.rgcb.scholar.validation.DocumentDiagnosticSeverity;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/** Explicit versioned schema mapping. Runtime objects never pass through reflection serialization. */
public final class DocumentJsonCodec {
    public static final String FORMAT = "scholar-document";
    public static final int VERSION = 2;
    public static final int MAX_CHARACTERS = 16 * 1024 * 1024;
    private static final int MAX_DEPTH = 128;
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().serializeNulls().create();

    public PersistenceResult<String> encode(Document document) {
        var diagnostics = validation(document);
        if (diagnostics.stream().anyMatch(d -> d.severity() == PersistenceDiagnostic.Severity.ERROR)) {
            return new PersistenceResult.Failure<>(diagnostics);
        }
        try {
            var root = object("format", FORMAT, "version", VERSION,
                    "settings", settings(document.settings()),
                    "blocks", array(document.blocks(), this::block), "datasets", array(document.datasets(), this::dataset));
            var text = escapeUnpairedSurrogates(JSON.toJson(root)) + "\n";
            if (text.length() > MAX_CHARACTERS) {
                return PersistenceResult.failure(PersistenceDiagnostic.Code.SIZE_LIMIT, "$", "Document exceeds the V1 size limit.");
            }
            // Apply the same bounds to authored trees as to external input.
            parse(text);
            return new PersistenceResult.Success<>(text, diagnostics);
        } catch (SchemaFailure failure) {
            return PersistenceResult.failure(failure.code, failure.path, failure.getMessage());
        } catch (StackOverflowError failure) {
            return PersistenceResult.failure(PersistenceDiagnostic.Code.SIZE_LIMIT, "$", "Document nesting exceeds the V1 limit.");
        } catch (IllegalArgumentException failure) {
            return PersistenceResult.failure(PersistenceDiagnostic.Code.INVALID_VALUE, "$", "Document cannot be represented by V1.");
        }
    }

    public PersistenceResult<Document> decode(String text) {
        if (text == null || text.length() > MAX_CHARACTERS) {
            return PersistenceResult.failure(PersistenceDiagnostic.Code.SIZE_LIMIT, "$", "Document exceeds the V1 size limit.");
        }
        try {
            var root = node(parse(text), "$");
            if (!FORMAT.equals(string(root, "format"))) {
                throw error(PersistenceDiagnostic.Code.WRONG_FORMAT, "$.format", "Not a Scholar document.");
            }
            var version = integer(root, "version");
            if (version != 1 && version != VERSION) {
                throw error(PersistenceDiagnostic.Code.UNSUPPORTED_VERSION, "$.version", "Unsupported Scholar document version: " + version);
            }
            // Version dispatch is deliberately separate from semantic reconstruction.
            var document = version == 1 ? readV1(root) : readV2(root);
            var diagnostics = validation(document);
            if (diagnostics.stream().anyMatch(d -> d.severity() == PersistenceDiagnostic.Severity.ERROR)) {
                return new PersistenceResult.Failure<>(diagnostics);
            }
            return new PersistenceResult.Success<>(document, diagnostics);
        } catch (SchemaFailure failure) {
            return PersistenceResult.failure(failure.code, failure.path, failure.getMessage());
        } catch (IllegalArgumentException failure) {
            return PersistenceResult.failure(PersistenceDiagnostic.Code.INVALID_VALUE, "$", "Invalid semantic document: " + failure.getMessage());
        }
    }

    private Document readV1(JsonObject root) {
        fields(root, "format", "version", "blocks", "datasets");
        return new Document(list(root, "blocks", this::readBlockV1), list(root, "datasets", this::readDataset));
    }

    private Document readV2(JsonObject root) {
        fields(root, "format", "version", "settings", "blocks", "datasets");
        return new Document(list(root, "blocks", this::readBlock), list(root, "datasets", this::readDataset), readSettings(required(root, "settings")));
    }

    private static List<PersistenceDiagnostic> validation(Document document) {
        return DocumentValidator.validate(document).diagnostics().stream().map(d -> new PersistenceDiagnostic(
                d.severity() == DocumentDiagnosticSeverity.ERROR ? PersistenceDiagnostic.Severity.ERROR : PersistenceDiagnostic.Severity.WARNING,
                PersistenceDiagnostic.Code.VALIDATION_FAILURE, d.context().orElse("$"), d.code() + ": " + d.message())).toList();
    }

    private JsonObject block(BlockNode value) {
        if (value instanceof DatasetAnalysisBlock v) return object("type", "dataset-analysis", "id", v.id(),
                "datasetId", v.datasetId(), "kind", m30Label(v.kind()), "xColumnId", v.xColumnId().orElse(null),
                "yColumnId", v.yColumnId(), "displayUnit", v.displayUnit().map(this::unitExpression).orElse(null),
                "notation", m30Label(v.notation()));
        if (value instanceof VariableDefinition v) return object("type", "variable", "id", v.id(), "name", v.name(),
                "value", scientificValue(v.value()), "label", v.label().orElse(null));
        if (value instanceof ComputedResult v) return object("type", "computed-result", "expression", computation(v.expression()),
                "source", v.authoredSource(), "label", v.label().orElse(null),
                "displayUnit", v.displayUnit().map(this::unitExpression).orElse(null), "notation", m30Label(v.notation()));
        if (value instanceof Paragraph v) return object("type", "paragraph", "style", m30Label(v.style()), "format", paragraphFormat(v.format()), "content", inline(v.content()));
        if (value instanceof Heading v) return object("type", "heading", "id", v.id().orElse(null), "level", v.level(), "content", inline(v.content()));
        if (value instanceof EquationBlock v) return object("type", "equation", "id", v.id().orElse(null), "expression", math(v.expression()));
        if (value instanceof TableBlock v) return object("type", "table", "id", v.id().orElse(null), "headerRows", v.headerRowCount(),
                "rows", array(v.rows(), row -> array(row.cells(), cell -> inline(cell.content().content()))),
                "binding", v.datasetBinding().map(this::tableBinding).orElse(null), "span", m30Label(v.span()));
        if (value instanceof PlotBlock v) return object("type", "plot", "definition", plot(v.definition()));
        if (value instanceof DiagramBlock v) return object("type", "diagram", "workspaceAspectRatio", v.workspaceAspectRatio(), "definition", diagram(v.definition()));
        if (value instanceof FigureBlock v) return object("type", "figure", "id", v.id(), "caption", inline(v.caption()), "content", block(v.content()), "span", m30Label(v.span()));
        if (value instanceof TableOfContentsBlock) return object("type", "toc");
        if (value instanceof PageBreak) return object("type", "page-break");
        if (value instanceof LayoutSectionBreak v) return object("type", "layout-section-break",
                "columns", columnLayout(v.columnLayout()));
        throw unknown("block");
    }

    private BlockNode readBlock(JsonElement value) {
        var o = node(value, "block");
        return switch (string(o, "type")) {
            case "dataset-analysis" -> { fields(o, "type", "id", "datasetId", "kind", "xColumnId", "yColumnId", "displayUnit", "notation");
                yield new DatasetAnalysisBlock(identity(o, "id"), identity(o, "datasetId"),
                        m30Enum(required(o, "kind"), dev.rgcb.scholar.analysis.AnalysisKind.class), optionalString(o, "xColumnId"),
                        identity(o, "yColumnId"), optional(o, "displayUnit", this::readUnitExpression),
                        m30Enum(required(o, "notation"), NumberNotation.class)); }
            case "variable" -> { fields(o, "type", "id", "name", "value", "label");
                yield new VariableDefinition(identity(o, "id"), string(o, "name"), readScientificValue(required(o, "value")), optionalString(o, "label")); }
            case "computed-result" -> { fields(o, "type", "expression", "source", "label", "displayUnit", "notation");
                yield new ComputedResult(readComputation(required(o, "expression")), string(o, "source"), optionalString(o, "label"),
                        optional(o, "displayUnit", this::readUnitExpression), m30Enum(required(o, "notation"), NumberNotation.class)); }
            case "paragraph" -> { fields(o, "type", "style", "format", "content"); yield new Paragraph(readInline(required(o, "content")), m30Enum(required(o, "style"), SemanticStyle.class), readParagraphFormat(required(o, "format"))); }
            case "heading" -> { fields(o, "type", "id", "level", "content"); yield new Heading(integer(o, "level"), readInline(required(o, "content")), optionalString(o, "id")); }
            case "equation" -> { fields(o, "type", "id", "expression"); yield new EquationBlock(readMath(required(o, "expression")), optionalString(o, "id")); }
            case "table" -> {
                fields(o, "type", "id", "headerRows", "rows", "binding", "span");
                var rows = list(o, "rows", row -> new TableRow(elements(row).stream()
                        .map(cell -> new TableCell(new TableCellContent(readInline(cell)))).toList()));
                yield new TableBlock(rows, integer(o, "headerRows"), optionalString(o, "id"), optional(o, "binding", this::readTableBinding), m30Enum(required(o, "span"), ContentSpan.class));
            }
            case "plot" -> { fields(o, "type", "definition"); yield new PlotBlock(readPlot(required(o, "definition"))); }
            case "diagram" -> { fields(o, "type", "definition", "workspaceAspectRatio"); yield new DiagramBlock(readDiagram(required(o, "definition")), decimal(o, "workspaceAspectRatio")); }
            case "figure" -> { fields(o, "type", "id", "caption", "content", "span"); yield new FigureBlock(identity(o, "id"), readBlock(required(o, "content")), readInline(required(o, "caption")), m30Enum(required(o, "span"), ContentSpan.class)); }
            case "toc" -> { fields(o, "type"); yield new TableOfContentsBlock(); }
            case "page-break" -> { fields(o, "type"); yield new PageBreak(); }
            case "layout-section-break" -> { fields(o, "type", "columns"); yield new LayoutSectionBreak(
                    readColumnLayout(required(o, "columns"))); }
            default -> throw unknown("block");
        };
    }

    private BlockNode readBlockV1(JsonElement value) {
        var o = node(value, "block");
        return switch (string(o, "type")) {
            case "paragraph" -> { fields(o, "type", "content"); yield new Paragraph(readInlineV1(required(o, "content"))); }
            case "heading" -> { fields(o, "type", "id", "level", "content"); yield new Heading(integer(o, "level"), readInlineV1(required(o, "content")), optionalString(o, "id")); }
            case "equation" -> { fields(o, "type", "id", "expression"); yield new EquationBlock(readMath(required(o, "expression")), optionalString(o, "id")); }
            case "table" -> {
                fields(o, "type", "id", "headerRows", "rows", "binding");
                var rows = list(o, "rows", row -> new TableRow(elements(row).stream()
                        .map(cell -> new TableCell(new TableCellContent(readInlineV1(cell)))).toList()));
                yield new TableBlock(rows, integer(o, "headerRows"), optionalString(o, "id"), optional(o, "binding", this::readTableBinding));
            }
            case "plot" -> { fields(o, "type", "definition"); yield new PlotBlock(readPlot(required(o, "definition"))); }
            case "diagram" -> { fields(o, "type", "definition", "workspaceAspectRatio"); yield new DiagramBlock(readDiagram(required(o, "definition")), decimal(o, "workspaceAspectRatio")); }
            case "figure" -> { fields(o, "type", "id", "caption", "content"); yield new FigureBlock(identity(o, "id"), readBlockV1(required(o, "content")), readInlineV1(required(o, "caption"))); }
            case "toc" -> { fields(o, "type"); yield new TableOfContentsBlock(); }
            default -> throw unknown("block");
        };
    }

    private JsonArray inline(InlineContent value) {
        return array(value.nodes(), v -> {
            if (v instanceof Text t) return object("type", "text", "content", t.content(), "marks", array(t.marks().stream().sorted().toList(), m -> new JsonPrimitive(m30Label(m))), "format", textFormat(t.format()));
            if (v instanceof CrossReference r) return object("type", "reference", "kind", label(r.kind()), "targetId", r.targetId());
            if (v instanceof QuantityInline q) return object("type", "quantity", "value", quantity(q.value()), "notation", m30Label(q.notation()));
            throw unknown("inline");
        });
    }

    private InlineContent readInline(JsonElement value) {
        return new InlineContent(elements(value).stream().map(v -> {
            var o = node(v, "inline");
            return switch (string(o, "type")) {
                case "text" -> {
                    fields(o, "type", "content", "marks", "format");
                    var marks = list(o, "marks", m -> m30Enum(m, TextMark.class));
                    if (new HashSet<>(marks).size() != marks.size()) throw invalid("marks", "Duplicate text mark.");
                    yield (InlineNode) new Text(string(o, "content"), new HashSet<>(marks), readTextFormat(required(o, "format")));
                }
                case "reference" -> { fields(o, "type", "kind", "targetId"); yield new CrossReference(enumeration(required(o, "kind"), CrossReferenceTargetKind.class), identity(o, "targetId")); }
                case "quantity" -> { fields(o, "type", "value", "notation"); yield new QuantityInline(readQuantity(required(o, "value")), m30Enum(required(o, "notation"), NumberNotation.class)); }
                default -> throw unknown("inline");
            };
        }).toList());
    }

    private InlineContent readInlineV1(JsonElement value) {
        return new InlineContent(elements(value).stream().map(v -> {
            var o = node(v, "inline");
            return switch (string(o, "type")) {
                case "text" -> {
                    fields(o, "type", "content", "marks");
                    var marks = list(o, "marks", m -> enumeration(m, TextMark.class));
                    if (new HashSet<>(marks).size() != marks.size()) throw invalid("marks", "Duplicate text mark.");
                    yield (InlineNode) new Text(string(o, "content"), new HashSet<>(marks));
                }
                case "reference" -> { fields(o, "type", "kind", "targetId"); yield new CrossReference(enumeration(required(o, "kind"), CrossReferenceTargetKind.class), identity(o, "targetId")); }
                default -> throw unknown("inline");
            };
        }).toList());
    }

    private JsonObject settings(DocumentSettings value) {
        return object(
                "template", m30Label(value.template()),
                "paper", object("kind", m30Label(value.paper().kind()),
                        "widthMicrometres", value.paper().width().micrometres(),
                        "heightMicrometres", value.paper().height().micrometres()),
                "orientation", m30Label(value.orientation()),
                "margins", object("topMicrometres", value.margins().top().micrometres(),
                        "rightMicrometres", value.margins().right().micrometres(),
                        "bottomMicrometres", value.margins().bottom().micrometres(),
                        "leftMicrometres", value.margins().left().micrometres()),
                "columns", object("count", value.columns().count(), "gapMicrometres", value.columns().gap().micrometres()),
                "decoration", object("header", value.decoration().headerText(), "footer", value.decoration().footerText(),
                        "pageNumbers", value.decoration().pageNumbers()));
    }

    private static JsonObject columnLayout(ColumnLayout value) {
        return object("count", value.count(), "gapMicrometres", value.gap().micrometres());
    }

    private static ColumnLayout readColumnLayout(JsonElement value) {
        var column = node(value, "columns", "count", "gapMicrometres");
        return new ColumnLayout(integer(column, "count"), new PhysicalLength(longInteger(column, "gapMicrometres")));
    }

    private DocumentSettings readSettings(JsonElement value) {
        var o = node(value, "settings", "template", "paper", "orientation", "margins", "columns", "decoration");
        var paper = node(required(o, "paper"), "paper", "kind", "widthMicrometres", "heightMicrometres");
        var kind = m30Enum(required(paper, "kind"), PaperKind.class);
        var width = new PhysicalLength(longInteger(paper, "widthMicrometres"));
        var height = new PhysicalLength(longInteger(paper, "heightMicrometres"));
        var paperSize = kind == PaperKind.CUSTOM ? PaperSize.custom(width, height) : new PaperSize(kind, width, height);
        var margins = node(required(o, "margins"), "margins", "topMicrometres", "rightMicrometres", "bottomMicrometres", "leftMicrometres");
        var column = node(required(o, "columns"), "columns", "count", "gapMicrometres");
        var decoration = node(required(o, "decoration"), "decoration", "header", "footer", "pageNumbers");
        return new DocumentSettings(
                m30Enum(required(o, "template"), DocumentTemplateId.class),
                paperSize,
                m30Enum(required(o, "orientation"), PageOrientation.class),
                new PageMargins(new PhysicalLength(longInteger(margins, "topMicrometres")),
                        new PhysicalLength(longInteger(margins, "rightMicrometres")),
                        new PhysicalLength(longInteger(margins, "bottomMicrometres")),
                        new PhysicalLength(longInteger(margins, "leftMicrometres"))),
                new ColumnLayout(integer(column, "count"), new PhysicalLength(longInteger(column, "gapMicrometres"))),
                new PageDecoration(string(decoration, "header"), string(decoration, "footer"), bool(decoration, "pageNumbers")));
    }

    private static JsonObject textFormat(TextFormat value) {
        return object("fontFamily", value.fontFamily().map(DocumentJsonCodec::m30Label).orElse(null),
                "fontSizeHalfPoints", value.fontSizeHalfPoints().orElse(null));
    }

    private static TextFormat readTextFormat(JsonElement value) {
        var o = node(value, "text format", "fontFamily", "fontSizeHalfPoints");
        return new TextFormat(optional(o, "fontFamily", v -> m30Enum(v, ScholarFontFamily.class)),
                optional(o, "fontSizeHalfPoints", DocumentJsonCodec::integerValue));
    }

    private static JsonObject paragraphFormat(ParagraphFormat value) {
        return object("alignment", value.alignment().map(DocumentJsonCodec::m30Label).orElse(null),
                "lineSpacingPermille", value.lineSpacingPermille().orElse(null),
                "spaceBefore", value.spaceBefore().orElse(null), "spaceAfter", value.spaceAfter().orElse(null),
                "leftIndent", value.leftIndent().orElse(null), "rightIndent", value.rightIndent().orElse(null),
                "firstLineIndent", value.firstLineIndent().orElse(null));
    }

    private static ParagraphFormat readParagraphFormat(JsonElement value) {
        var o = node(value, "paragraph format", "alignment", "lineSpacingPermille", "spaceBefore", "spaceAfter",
                "leftIndent", "rightIndent", "firstLineIndent");
        return new ParagraphFormat(optional(o, "alignment", v -> m30Enum(v, ParagraphAlignment.class)),
                optional(o, "lineSpacingPermille", DocumentJsonCodec::integerValue),
                optional(o, "spaceBefore", DocumentJsonCodec::integerValue), optional(o, "spaceAfter", DocumentJsonCodec::integerValue),
                optional(o, "leftIndent", DocumentJsonCodec::integerValue), optional(o, "rightIndent", DocumentJsonCodec::integerValue),
                optional(o, "firstLineIndent", DocumentJsonCodec::integerValue));
    }

    private JsonObject math(MathExpression value) {
        if (value instanceof MathSequence v) return object("type", "sequence", "expressions", array(v.expressions(), this::math));
        if (value instanceof MathNumber v) return object("type", "number", "content", v.content());
        if (value instanceof MathIdentifier v) return object("type", "identifier", "name", v.name());
        if (value instanceof MathNamedOperator v) return object("type", "named-operator", "name", v.name());
        if (value instanceof MathText v) return object("type", "text", "content", v.content());
        if (value instanceof MathQuantity v) return object("type", "quantity", "value", quantity(v.value()), "notation", m30Label(v.notation()));
        if (value instanceof MathOperator v) return object("type", "operator", "symbol", v.symbol(), "role", label(v.role()));
        if (value instanceof MathSymbol v) return object("type", "symbol", "symbol", v.symbol(), "kind", label(v.kind()));
        if (value instanceof MathFraction v) return object("type", "fraction", "numerator", math(v.numerator()), "denominator", math(v.denominator()));
        if (value instanceof MathScript v) return object("type", "script", "base", math(v.base()), "subscript", v.subscript().map(this::math).orElse(null), "superscript", v.superscript().map(this::math).orElse(null));
        if (value instanceof MathRoot v) return object("type", "root", "radicand", math(v.radicand()), "index", v.index().map(this::math).orElse(null));
        if (value instanceof MathGroup v) return object("type", "group", "content", math(v.content()), "delimiter", label(v.delimiter()));
        throw unknown("math");
    }

    private MathExpression readMath(JsonElement value) {
        var o = node(value, "math");
        return switch (string(o, "type")) {
            case "sequence" -> { fields(o, "type", "expressions"); yield new MathSequence(list(o, "expressions", this::readMath)); }
            case "number" -> { fields(o, "type", "content"); yield new MathNumber(string(o, "content")); }
            case "identifier" -> { fields(o, "type", "name"); yield new MathIdentifier(string(o, "name")); }
            case "named-operator" -> { fields(o, "type", "name"); yield new MathNamedOperator(string(o, "name")); }
            case "text" -> { fields(o, "type", "content"); yield new MathText(string(o, "content")); }
            case "quantity" -> { fields(o, "type", "value", "notation"); yield new MathQuantity(readQuantity(required(o, "value")), m30Enum(required(o, "notation"), NumberNotation.class)); }
            case "operator" -> { fields(o, "type", "symbol", "role"); yield new MathOperator(string(o, "symbol"), enumeration(required(o, "role"), MathOperatorRole.class)); }
            case "symbol" -> { fields(o, "type", "symbol", "kind"); yield new MathSymbol(string(o, "symbol"), enumeration(required(o, "kind"), MathSymbolKind.class)); }
            case "fraction" -> { fields(o, "type", "numerator", "denominator"); yield new MathFraction(readMath(required(o, "numerator")), readMath(required(o, "denominator"))); }
            case "script" -> { fields(o, "type", "base", "subscript", "superscript"); yield new MathScript(readMath(required(o, "base")), optional(o, "subscript", this::readMath), optional(o, "superscript", this::readMath)); }
            case "root" -> { fields(o, "type", "radicand", "index"); yield new MathRoot(readMath(required(o, "radicand")), optional(o, "index", this::readMath)); }
            case "group" -> { fields(o, "type", "content", "delimiter"); yield new MathGroup(readMath(required(o, "content")), enumeration(required(o, "delimiter"), MathDelimiter.class)); }
            default -> throw unknown("math");
        };
    }

    private JsonObject dataset(ScientificDataset v) {
        return object("id", v.id(), "displayName", v.displayName().orElse(null),
                "columns", array(v.columns(), c -> object("id", c.id(), "displayName", c.displayName(), "type", label(c.type()),
                        "unit", c.unit().map(this::unitExpression).orElse(null), "quantitySemantics", m30Label(c.quantitySemantics()))),
                "rows", array(v.rows(), r -> object("id", r.id().orElse(null), "values", array(r.values(), cell -> switch (cell.kind()) {
                    case NUMBER -> object("type", "number", "value", cell.number().orElseThrow());
                    case TEXT -> object("type", "text", "value", cell.text().orElseThrow());
                    case MISSING -> object("type", "missing");
                }))));
    }

    private ScientificDataset readDataset(JsonElement value) {
        var o = node(value, "dataset", "id", "displayName", "columns", "rows");
        return new ScientificDataset(identity(o, "id"), optionalString(o, "displayName"), list(o, "columns", v -> {
            var c = node(v, "column");
            fieldsWithOptional(c, java.util.Set.of("quantitySemantics"), "id", "displayName", "type", "unit");
            var unit = optional(c, "unit", this::readUnitExpression);
            var semantics = optionalAdditive(c, "quantitySemantics", encoded -> m30Enum(encoded, QuantitySemantics.class))
                    .orElseGet(() -> unit.map(QuantitySemantics::defaultFor).orElse(QuantitySemantics.LINEAR));
            return new DatasetColumn(identity(c, "id"), identity(c, "displayName"), enumeration(required(c, "type"), DatasetColumnType.class),
                    unit, semantics);
        }), list(o, "rows", v -> {
            var r = node(v, "row", "id", "values");
            return new DatasetRow(optionalString(r, "id"), list(r, "values", cell -> {
                var c = node(cell, "dataset value");
                return switch (string(c, "type")) {
                    case "number" -> { fields(c, "type", "value"); yield DatasetValue.number(number(required(c, "value")).getAsBigDecimal()); }
                    case "text" -> { fields(c, "type", "value"); yield DatasetValue.text(string(c, "value")); }
                    case "missing" -> { fields(c, "type"); yield DatasetValue.missing(); }
                    default -> throw unknown("dataset value");
                };
            }));
        }));
    }

    private JsonObject tableBinding(DatasetTableBinding v) { return object("datasetId", v.datasetId(), "columnIds", array(v.columnIds(), JsonPrimitive::new)); }
    private DatasetTableBinding readTableBinding(JsonElement value) {
        var o = node(value, "table binding", "datasetId", "columnIds");
        return new DatasetTableBinding(identity(o, "datasetId"), list(o, "columnIds", v -> canonical(stringValue(v), "columnIds")));
    }
    private JsonObject plotBinding(DatasetPlotBinding v) { return object("datasetId", v.datasetId(), "xColumnId", v.xColumnId(), "yColumnId", v.yColumnId()); }
    private DatasetPlotBinding readPlotBinding(JsonElement value) {
        var o = node(value, "plot binding", "datasetId", "xColumnId", "yColumnId");
        return new DatasetPlotBinding(identity(o, "datasetId"), identity(o, "xColumnId"), identity(o, "yColumnId"));
    }
    private JsonObject axis(AxisDefinition v) { return object("label", v.label(), "scale", label(v.scale()), "range", v.explicitRange().map(r -> object("min", r.min(), "max", r.max())).orElse(null),
            "displayUnit", v.displayUnit().map(this::unitExpression).orElse(null),
            "displayUnitSemantics", v.displayUnitSemantics().map(DocumentJsonCodec::m30Label).orElse(null)); }
    private AxisDefinition readAxis(JsonElement value) {
        var o = node(value, "axis");
        fieldsWithOptional(o, java.util.Set.of("displayUnitSemantics"), "label", "scale", "range", "displayUnit");
        var unit = optional(o, "displayUnit", this::readUnitExpression);
        var semantics = optionalAdditive(o, "displayUnitSemantics", v -> m30Enum(v, QuantitySemantics.class));
        if (semantics.isEmpty()) semantics = unit.map(QuantitySemantics::defaultFor);
        return new AxisDefinition(string(o, "label"), optional(o, "range", v -> {
            var r = node(v, "range", "min", "max"); return new AxisRange(decimal(r, "min"), decimal(r, "max"));
        }), enumeration(required(o, "scale"), AxisScale.class), unit, semantics);
    }

    private JsonObject unitExpression(UnitExpression value) {
        return object("factors", array(value.factors(), factor -> object("unitId", factor.unitId(),
                "prefix", factor.prefix().map(MetricPrefix::symbol).orElse(null), "exponent", factor.exponent())));
    }

    private UnitExpression readUnitExpression(JsonElement value) {
        var o = node(value, "unit expression", "factors");
        return new UnitExpression(list(o, "factors", element -> {
            var factor = node(element, "unit factor", "unitId", "prefix", "exponent");
            var prefix = optionalString(factor, "prefix").map(symbol -> java.util.Arrays.stream(MetricPrefix.values())
                    .filter(candidate -> candidate.symbol().equals(symbol)).findFirst()
                    .orElseThrow(() -> unknown("metric prefix")));
            return new UnitFactor(identity(factor, "unitId"), prefix, integer(factor, "exponent"));
        }));
    }

    private JsonObject scientificValue(ScientificValue value) {
        if (value instanceof ScientificValue.Scalar scalar) return object("kind", "scalar", "value", scalar.value());
        return object("kind", "physical", "quantity", quantity(((ScientificValue.Physical) value).value()));
    }

    private ScientificValue readScientificValue(JsonElement value) {
        var o = node(value, "scientific value");
        return switch (string(o, "kind")) {
            case "scalar" -> { fields(o, "kind", "value");
                yield new ScientificValue.Scalar(number(required(o, "value")).getAsBigDecimal()); }
            case "physical" -> { fields(o, "kind", "quantity");
                yield new ScientificValue.Physical(readQuantity(required(o, "quantity"))); }
            default -> throw unknown("scientific value");
        };
    }

    private JsonObject computation(Expression value) {
        if (value instanceof Expression.NumberLiteral v) return object("type", "number", "value", v.value());
        if (value instanceof Expression.ValueLiteral v) return object("type", "value", "value", scientificValue(v.value()));
        if (value instanceof Expression.Variable v) return object("type", "variable", "id", v.reference().variableId(), "name", v.authoredName());
        if (value instanceof Expression.UnresolvedName v) return object("type", "unresolved-name", "name", v.name());
        if (value instanceof Expression.Unary v) return object("type", "unary", "operator", m30Label(v.operator()), "operand", computation(v.operand()));
        if (value instanceof Expression.Binary v) return object("type", "binary", "operator", m30Label(v.operator()),
                "left", computation(v.left()), "right", computation(v.right()));
        if (value instanceof Expression.Power v) return object("type", "power", "base", computation(v.base()), "exponent", v.exponent());
        if (value instanceof Expression.Group v) return object("type", "group", "inner", computation(v.inner()));
        if (value instanceof Expression.Invalid v) return object("type", "invalid", "source", v.source(), "code", m30Label(v.code()));
        throw unknown("computation expression");
    }

    private Expression readComputation(JsonElement value) {
        var o = node(value, "computation expression");
        return switch (string(o, "type")) {
            case "number" -> { fields(o, "type", "value"); yield new Expression.NumberLiteral(number(required(o, "value")).getAsBigDecimal()); }
            case "value" -> { fields(o, "type", "value"); yield new Expression.ValueLiteral(readScientificValue(required(o, "value"))); }
            case "variable" -> { fields(o, "type", "id", "name"); yield new Expression.Variable(
                    new VariableDependencyReference(identity(o, "id")), string(o, "name")); }
            case "unresolved-name" -> { fields(o, "type", "name"); yield new Expression.UnresolvedName(string(o, "name")); }
            case "unary" -> { fields(o, "type", "operator", "operand"); yield new Expression.Unary(
                    m30Enum(required(o, "operator"), Expression.UnaryOperator.class), readComputation(required(o, "operand"))); }
            case "binary" -> { fields(o, "type", "operator", "left", "right"); yield new Expression.Binary(
                    m30Enum(required(o, "operator"), Expression.Operator.class), readComputation(required(o, "left")),
                    readComputation(required(o, "right"))); }
            case "power" -> { fields(o, "type", "base", "exponent"); yield new Expression.Power(
                    readComputation(required(o, "base")), integer(o, "exponent")); }
            case "group" -> { fields(o, "type", "inner"); yield new Expression.Group(readComputation(required(o, "inner"))); }
            case "invalid" -> { fields(o, "type", "source", "code"); yield new Expression.Invalid(string(o, "source"),
                    m30Enum(required(o, "code"), ComputationDiagnostic.Code.class)); }
            default -> throw unknown("computation expression");
        };
    }

    private JsonObject quantity(QuantityValue value) {
        if (value instanceof Quantity q) return object("kind", "quantity", "value", q.value(), "unit", unitExpression(q.unit()),
                "semantics", m30Label(q.semantics()));
        if (value instanceof MeasuredQuantity q) return object("kind", "measured", "value", q.nominal().value(),
                "uncertainty", q.absoluteUncertainty(), "unit", unitExpression(q.nominal().unit()),
                "semantics", m30Label(q.nominal().semantics()));
        throw unknown("quantity");
    }

    private QuantityValue readQuantity(JsonElement value) {
        var o = node(value, "quantity");
        return switch (string(o, "kind")) {
            case "quantity" -> {
                fieldsWithOptional(o, java.util.Set.of("semantics"), "kind", "value", "unit");
                var unit = readUnitExpression(required(o, "unit"));
                yield new Quantity(number(required(o, "value")).getAsBigDecimal(), unit,
                        optionalAdditive(o, "semantics", v -> m30Enum(v, QuantitySemantics.class)).orElseGet(() -> QuantitySemantics.defaultFor(unit)));
            }
            case "measured" -> {
                fieldsWithOptional(o, java.util.Set.of("semantics"), "kind", "value", "uncertainty", "unit");
                var unit = readUnitExpression(required(o, "unit"));
                yield new MeasuredQuantity(
                    new Quantity(number(required(o, "value")).getAsBigDecimal(), unit,
                            optionalAdditive(o, "semantics", v -> m30Enum(v, QuantitySemantics.class)).orElseGet(() -> QuantitySemantics.defaultFor(unit))),
                    number(required(o, "uncertainty")).getAsBigDecimal()); }
            default -> throw unknown("quantity");
        };
    }
    private JsonObject plot(PlotDefinition v) {
        return object("title", v.title(), "xAxis", axis(v.xAxis()), "yAxis", axis(v.yAxis()), "legendVisible", v.legendVisible(), "gridVisible", v.gridVisible(), "height", v.height(),
                "series", array(v.series(), s -> object("name", s.name(), "kind", label(s.kind()), "points", array(s.points(), p -> object("x", p.x(), "y", p.y())), "binding", s.datasetBinding().map(this::plotBinding).orElse(null), "fitAnalysisId", s.fitAnalysisId().orElse(null))));
    }
    private PlotDefinition readPlot(JsonElement value) {
        var o = node(value, "plot", "title", "xAxis", "yAxis", "legendVisible", "gridVisible", "height", "series");
        return new PlotDefinition(string(o, "title"), readAxis(required(o, "xAxis")), readAxis(required(o, "yAxis")), list(o, "series", v -> {
            var s = node(v, "series");
            fieldsWithOptional(s, java.util.Set.of("fitAnalysisId"), "name", "kind", "points", "binding");
            return new PlotSeries(string(s, "name"), enumeration(required(s, "kind"), PlotSeriesKind.class), list(s, "points", p -> {
                var point = node(p, "point", "x", "y"); return new DataPoint(decimal(point, "x"), decimal(point, "y"));
            }), optional(s, "binding", this::readPlotBinding), optionalAdditive(s, "fitAnalysisId", JsonElement::getAsString));
        }), bool(o, "legendVisible"), bool(o, "gridVisible"), integer(o, "height"));
    }

    private JsonObject bounds(DiagramBounds b) { return object("x", b.x(), "y", b.y(), "width", b.width(), "height", b.height()); }
    private DiagramBounds readBounds(JsonElement v) {
        var b = node(v, "bounds", "x", "y", "width", "height"); return new DiagramBounds(decimal(b, "x"), decimal(b, "y"), decimal(b, "width"), decimal(b, "height"));
    }
    private JsonObject endpoint(DiagramEndpoint v) { return object("elementId", v.elementId().value(), "portId", v.portId().value()); }
    private DiagramEndpoint readEndpoint(JsonElement v) {
        var o = node(v, "endpoint", "elementId", "portId"); return new DiagramEndpoint(new DiagramElementId(string(o, "elementId")), new DiagramPortId(string(o, "portId")));
    }
    private JsonObject diagram(DiagramDefinition v) {
        return object("title", v.title(), "canvas", object("width", v.canvas().width(), "height", v.canvas().height()), "elements", array(v.elements(), this::element),
                "connections", array(v.connections(), c -> object("source", endpoint(c.source()), "target", endpoint(c.target()), "label", c.label())));
    }
    private DiagramDefinition readDiagram(JsonElement v) {
        var o = node(v, "diagram", "title", "canvas", "elements", "connections");
        var c = node(required(o, "canvas"), "canvas", "width", "height");
        return new DiagramDefinition(string(o, "title"), new DiagramCanvas(decimal(c, "width"), decimal(c, "height")), list(o, "elements", this::readElement), list(o, "connections", value -> {
            var connection = node(value, "connection", "source", "target", "label");
            return new DiagramConnection(readEndpoint(required(connection, "source")), readEndpoint(required(connection, "target")), string(connection, "label"));
        }));
    }
    private JsonObject element(DiagramElement v) {
        var o = object("id", v.id().value(), "bounds", bounds(v.bounds()));
        if (v instanceof DiagramNode n) {
            put(o, "type", "node", "label", n.label(), "ports", array(n.ports(), p -> object("id", p.id().value(), "label", p.label(), "side", label(p.placement().side()), "offset", p.placement().offset())));
        } else if (v instanceof ElectricalComponent e) {
            put(o, "type", "electrical-component", "kind", label(e.kind()), "orientation", label(e.orientation()), "referenceDesignator", e.referenceDesignator(), "valueLabel", e.valueLabel());
        } else if (v instanceof ElectricalJunction j) {
            put(o, "type", "electrical-junction", "netLabel", j.netLabel());
        } else if (v instanceof MechanicalPrimitive p) {
            put(o, "type", "mechanical-primitive", "kind", label(p.kind()), "orientation", label(p.orientation()));
        } else if (v instanceof MechanicalSymbol s) {
            put(o, "type", "mechanical-symbol", "kind", label(s.kind()), "orientation", label(s.orientation()));
        } else if (v instanceof MechanicalDimension d) {
            put(o, "type", "mechanical-dimension", "kind", label(d.kind()));
        } else if (v instanceof MechanicalAnnotation a) {
            put(o, "type", "mechanical-annotation", "kind", label(a.kind()), "text", a.text());
        } else if (v instanceof MechanicalConstraint c) {
            put(o, "type", "mechanical-constraint", "kind", label(c.kind()), "subjectId", c.subjectId().value(), "peerId", c.peerId().map(DiagramElementId::value).orElse(null));
        } else if (v instanceof MechanicalPartReference p) {
            put(o, "type", "mechanical-part-reference", "targetId", p.targetId().value(), "itemNumber", p.itemNumber(), "partName", p.partName(), "quantity", p.quantity(), "description", p.description());
        } else throw unknown("diagram element");
        return o;
    }
    private DiagramElement readElement(JsonElement v) {
        var o = node(v, "diagram element");
        var id = new DiagramElementId(string(o, "id"));
        var b = readBounds(required(o, "bounds"));
        return switch (string(o, "type")) {
            case "node" -> {
                fields(o, "type", "id", "bounds", "label", "ports");
                yield new DiagramNode(id, b, string(o, "label"), list(o, "ports", value -> {
                    var p = node(value, "port", "id", "label", "side", "offset");
                    return new DiagramPort(new DiagramPortId(string(p, "id")), string(p, "label"), new DiagramPortPlacement(enumeration(required(p, "side"), DiagramPortSide.class), decimal(p, "offset")));
                }));
            }
            case "electrical-component" -> { fields(o, "type", "id", "bounds", "kind", "orientation", "referenceDesignator", "valueLabel"); yield new ElectricalComponent(id, b, enumeration(required(o, "kind"), ElectricalComponentKind.class), enumeration(required(o, "orientation"), ElectricalOrientation.class), string(o, "referenceDesignator"), string(o, "valueLabel")); }
            case "electrical-junction" -> { fields(o, "type", "id", "bounds", "netLabel"); yield new ElectricalJunction(id, b, string(o, "netLabel")); }
            case "mechanical-primitive" -> { fields(o, "type", "id", "bounds", "kind", "orientation"); yield new MechanicalPrimitive(id, b, enumeration(required(o, "kind"), MechanicalPrimitiveKind.class), enumeration(required(o, "orientation"), MechanicalOrientation.class)); }
            case "mechanical-symbol" -> { fields(o, "type", "id", "bounds", "kind", "orientation"); yield new MechanicalSymbol(id, b, enumeration(required(o, "kind"), MechanicalSymbolKind.class), enumeration(required(o, "orientation"), MechanicalOrientation.class)); }
            case "mechanical-dimension" -> { fields(o, "type", "id", "bounds", "kind"); yield new MechanicalDimension(id, b, enumeration(required(o, "kind"), MechanicalDimensionKind.class)); }
            case "mechanical-annotation" -> { fields(o, "type", "id", "bounds", "kind", "text"); yield new MechanicalAnnotation(id, b, enumeration(required(o, "kind"), MechanicalAnnotationKind.class), string(o, "text")); }
            case "mechanical-constraint" -> { fields(o, "type", "id", "bounds", "kind", "subjectId", "peerId"); yield new MechanicalConstraint(id, b, enumeration(required(o, "kind"), MechanicalConstraintKind.class), new DiagramElementId(string(o, "subjectId")), optionalString(o, "peerId").map(DiagramElementId::new)); }
            case "mechanical-part-reference" -> { fields(o, "type", "id", "bounds", "targetId", "itemNumber", "partName", "quantity", "description"); yield new MechanicalPartReference(id, b, new DiagramElementId(string(o, "targetId")), integer(o, "itemNumber"), string(o, "partName"), integer(o, "quantity"), string(o, "description")); }
            default -> throw unknown("diagram element");
        };
    }

    private static JsonObject object(Object... values) { var o = new JsonObject(); put(o, values); return o; }
    private static void put(JsonObject o, Object... values) {
        for (int i = 0; i < values.length; i += 2) {
            var value = values[i + 1];
            JsonElement e = value == null ? JsonNull.INSTANCE : value instanceof JsonElement j ? j
                    : value instanceof Number n ? new JsonPrimitive(n) : value instanceof Boolean b ? new JsonPrimitive(b) : new JsonPrimitive((String) value);
            o.add((String) values[i], e);
        }
    }
    private static <T> JsonArray array(List<T> values, Function<T, ? extends JsonElement> convert) {
        var a = new JsonArray(); for (var value : values) a.add(convert.apply(value)); return a;
    }
    private static JsonObject node(JsonElement value, String path, String... allowed) {
        if (!value.isJsonObject()) throw invalid(path, "Expected an object.");
        var o = value.getAsJsonObject(); if (allowed.length > 0) fields(o, allowed); return o;
    }
    private static void fields(JsonObject o, String... allowed) {
        var names = java.util.Set.of(allowed);
        for (var name : o.keySet()) if (!names.contains(name)) throw invalid(name, "Unknown V1 field: " + name);
        for (var name : allowed) required(o, name);
    }
    private static void fieldsWithOptional(JsonObject o, java.util.Set<String> optional, String... required) {
        var names = new java.util.HashSet<>(optional);
        names.addAll(java.util.List.of(required));
        for (var name : o.keySet()) if (!names.contains(name)) throw invalid(name, "Unknown V2 field: " + name);
        for (var name : required) required(o, name);
    }
    private static JsonElement required(JsonObject o, String key) {
        if (!o.has(key)) throw error(PersistenceDiagnostic.Code.MISSING_FIELD, key, "Missing required field: " + key);
        return o.get(key);
    }
    private static List<JsonElement> elements(JsonElement value) {
        if (!value.isJsonArray()) throw invalid("array", "Expected an array.");
        var result = new ArrayList<JsonElement>(); value.getAsJsonArray().forEach(result::add); return result;
    }
    private static <T> List<T> list(JsonObject o, String key, Function<JsonElement, T> convert) { return elements(required(o, key)).stream().map(convert).toList(); }
    private static String string(JsonObject o, String key) { return stringValue(required(o, key)); }
    private static String identity(JsonObject o, String key) { return canonical(string(o, key), key); }
    private static String canonical(String s, String key) {
        if (s.isBlank() || !s.equals(s.trim())) throw invalid(key, "Identity/name must be nonblank and canonical.");
        return s;
    }
    private static String stringValue(JsonElement v) {
        if (!v.isJsonPrimitive() || !v.getAsJsonPrimitive().isString()) throw invalid("string", "Expected a string.");
        return v.getAsString();
    }
    private static Optional<String> optionalString(JsonObject o, String key) {
        return optional(o, key, v -> {
            var s = stringValue(v); if (s.isBlank() || !s.equals(s.trim())) throw invalid(key, "Identity/name must be nonblank and canonical."); return s;
        });
    }
    private static <T> Optional<T> optional(JsonObject o, String key, Function<JsonElement, T> convert) {
        var v = required(o, key); return v.isJsonNull() ? Optional.empty() : Optional.of(convert.apply(v));
    }
    private static <T> Optional<T> optionalAdditive(JsonObject o, String key, Function<JsonElement, T> convert) {
        if (!o.has(key) || o.get(key).isJsonNull()) return Optional.empty();
        return Optional.of(convert.apply(o.get(key)));
    }
    private static JsonPrimitive number(JsonElement v) {
        if (!v.isJsonPrimitive() || !v.getAsJsonPrimitive().isNumber()) throw invalid("number", "Expected a number."); return v.getAsJsonPrimitive();
    }
    private static int integer(JsonObject o, String key) {
        try { return number(required(o, key)).getAsBigDecimal().intValueExact(); }
        catch (ArithmeticException e) { throw invalid(key, "Expected a bounded integer."); }
    }
    private static int integerValue(JsonElement value) {
        try { return number(value).getAsBigDecimal().intValueExact(); }
        catch (ArithmeticException e) { throw invalid("integer", "Expected a bounded integer."); }
    }
    private static long longInteger(JsonObject o, String key) {
        try { return number(required(o, key)).getAsBigDecimal().longValueExact(); }
        catch (ArithmeticException e) { throw invalid(key, "Expected a bounded integer."); }
    }
    private static double decimal(JsonObject o, String key) {
        var d = number(required(o, key)).getAsDouble(); if (!Double.isFinite(d)) throw invalid(key, "Expected a finite number."); return d;
    }
    private static boolean bool(JsonObject o, String key) {
        var v = required(o, key); if (!v.isJsonPrimitive() || !v.getAsJsonPrimitive().isBoolean()) throw invalid(key, "Expected a boolean."); return v.getAsBoolean();
    }
    private static String label(Enum<?> value) { return V1WireLabels.label(value); }
    private static String m30Label(Enum<?> value) { return value.name().toLowerCase(java.util.Locale.ROOT).replace('_', '-'); }
    private static <T extends Enum<T>> T m30Enum(JsonElement value, Class<T> type) {
        var text = stringValue(value);
        for (var candidate : type.getEnumConstants()) if (m30Label(candidate).equals(text)) return candidate;
        throw invalid("enum", "Unknown V2 enum value: " + text);
    }
    private static String escapeUnpairedSurrogates(String value) {
        var result = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isHighSurrogate(c) && i + 1 < value.length() && Character.isLowSurrogate(value.charAt(i + 1))) {
                result.append(c).append(value.charAt(++i));
            } else if (Character.isSurrogate(c)) {
                result.append("\\u").append(Integer.toHexString(c));
            } else result.append(c);
        }
        return result.toString();
    }
    private static <T extends Enum<T>> T enumeration(JsonElement v, Class<T> type) {
        var text = stringValue(v);
        for (var candidate : type.getEnumConstants()) if (label(candidate).equals(text)) return candidate;
        throw invalid("enum", "Unknown V1 enum value: " + text);
    }
    private static SchemaFailure unknown(String path) { return error(PersistenceDiagnostic.Code.UNKNOWN_TYPE, path, "Unknown semantic type: " + path); }
    private static SchemaFailure invalid(String path, String message) { return error(PersistenceDiagnostic.Code.INVALID_VALUE, path, message); }
    private static SchemaFailure error(PersistenceDiagnostic.Code code, String path, String message) { return new SchemaFailure(code, path, message); }
    private static final class SchemaFailure extends IllegalArgumentException {
        final PersistenceDiagnostic.Code code; final String path;
        SchemaFailure(PersistenceDiagnostic.Code code, String path, String message) { super(message); this.code = code; this.path = path; }
    }

    // Streaming parse rejects duplicate members and excessive nesting before AST construction.
    private static JsonElement parse(String text) {
        try (var reader = new JsonReader(new StringReader(text))) {
            reader.setLenient(false);
            var result = readJson(reader, 0);
            if (reader.peek() != JsonToken.END_DOCUMENT) throw invalid("$", "Trailing JSON content.");
            return result;
        } catch (IOException | NumberFormatException e) {
            throw error(PersistenceDiagnostic.Code.MALFORMED_JSON, "$", "Malformed Scholar JSON.");
        }
    }
    private static JsonElement readJson(JsonReader reader, int depth) throws IOException {
        if (depth > MAX_DEPTH) throw error(PersistenceDiagnostic.Code.SIZE_LIMIT, reader.getPath(), "JSON nesting exceeds V1 limit.");
        return switch (reader.peek()) {
            case BEGIN_OBJECT -> {
                reader.beginObject(); var o = new JsonObject();
                while (reader.hasNext()) {
                    var name = reader.nextName(); if (o.has(name)) throw invalid(reader.getPath(), "Duplicate JSON member.");
                    o.add(name, readJson(reader, depth + 1));
                }
                reader.endObject(); yield o;
            }
            case BEGIN_ARRAY -> {
                reader.beginArray(); var a = new JsonArray(); while (reader.hasNext()) a.add(readJson(reader, depth + 1)); reader.endArray(); yield a;
            }
            case STRING -> new JsonPrimitive(reader.nextString());
            case NUMBER -> {
                var raw = reader.nextString();
                if (raw.length() > 1024) throw invalid(reader.getPath(), "Numeric literal exceeds V1 limit.");
                var number = new BigDecimal(raw);
                if (number.precision() > 1024 || Math.abs((long) number.scale()) > 1024) {
                    throw invalid(reader.getPath(), "Numeric precision/scale exceeds V1 limit.");
                }
                // Keep the lexical sign of -0.0 for authored double coordinates.
                yield JsonParser.parseString(raw);
            }
            case BOOLEAN -> new JsonPrimitive(reader.nextBoolean());
            case NULL -> { reader.nextNull(); yield JsonNull.INSTANCE; }
            default -> throw error(PersistenceDiagnostic.Code.MALFORMED_JSON, reader.getPath(), "Expected JSON value.");
        };
    }
}
