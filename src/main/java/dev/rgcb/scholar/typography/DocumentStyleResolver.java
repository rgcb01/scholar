package dev.rgcb.scholar.typography;

import dev.rgcb.scholar.document.*;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Resolves template defaults, then semantic style, then explicit local overrides. */
public final class DocumentStyleResolver {
    public SemanticStyleDefinition resolve(DocumentTemplateId template, SemanticStyle style) {
        return definitions(template).get(style);
    }

    public TextFormat resolveText(DocumentTemplateId template, SemanticStyle style, TextFormat local) {
        return resolve(template, style).text().overrideWith(local);
    }

    public ParagraphFormat resolveParagraph(DocumentTemplateId template, SemanticStyle style, ParagraphFormat local) {
        var base = resolve(template, style).paragraph();
        return new ParagraphFormat(
                choose(local.alignment(), base.alignment()),
                choose(local.lineSpacingPermille(), base.lineSpacingPermille()),
                choose(local.spaceBefore(), base.spaceBefore()),
                choose(local.spaceAfter(), base.spaceAfter()),
                choose(local.leftIndent(), base.leftIndent()),
                choose(local.rightIndent(), base.rightIndent()),
                choose(local.firstLineIndent(), base.firstLineIndent()));
    }

    private static <T> Optional<T> choose(Optional<T> local, Optional<T> base) {
        return local.isPresent() ? local : base;
    }

    private static Map<SemanticStyle, SemanticStyleDefinition> definitions(DocumentTemplateId template) {
        var result = new EnumMap<SemanticStyle, SemanticStyleDefinition>(SemanticStyle.class);
        for (var style : SemanticStyle.values()) {
            result.put(style, definition(style, template == DocumentTemplateId.IEEE_STYLE));
        }
        return result;
    }

    private static SemanticStyleDefinition definition(SemanticStyle style, boolean ieee) {
        var points = switch (style) {
            case TITLE -> ieee ? 24 : 32;
            case SUBTITLE -> 24;
            case AUTHOR -> ieee ? 20 : 22;
            case AFFILIATION, ABSTRACT, KEYWORDS, FIGURE_CAPTION, TABLE_CAPTION, REFERENCE, FOOTNOTE -> ieee ? 16 : 18;
            case CODE -> 18;
            case EQUATION, BODY_TEXT -> ieee ? 20 : 22;
        };
        var marks = switch (style) {
            case TITLE, ABSTRACT, KEYWORDS -> Set.of(TextMark.BOLD);
            default -> Set.<TextMark>of();
        };
        var alignment = switch (style) {
            case TITLE, SUBTITLE, AUTHOR, AFFILIATION -> ParagraphAlignment.CENTER;
            default -> ParagraphAlignment.LEFT;
        };
        var spacingAfter = switch (style) {
            case TITLE -> ieee ? 6 : 8;
            case AUTHOR -> ieee ? 2 : 6;
            case AFFILIATION -> ieee ? 8 : 6;
            case ABSTRACT -> ieee ? 4 : 8;
            case KEYWORDS -> ieee ? 10 : 8;
            case REFERENCE, FOOTNOTE, FIGURE_CAPTION, TABLE_CAPTION -> ieee ? 4 : 6;
            default -> ieee ? 6 : 10;
        };
        var paragraph = new ParagraphFormat(Optional.of(alignment), Optional.of(1000), Optional.empty(),
                Optional.of(spacingAfter), Optional.empty(), Optional.empty(), Optional.empty());
        return new SemanticStyleDefinition(new TextFormat(Optional.of(ScholarFontFamily.SOURCE_SANS_3), Optional.of(points)), marks, paragraph);
    }
}
