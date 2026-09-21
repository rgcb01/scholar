package dev.rgcb.scholar.document;

import java.util.List;
import java.util.Optional;

/** Closed M30 template policy. Templates configure documents; they are not demo fixtures. */
public final class DocumentTemplates {
    private DocumentTemplates() { }

    public static Document create(DocumentTemplateId id) {
        return switch (id) {
            case BLANK -> new Document(List.of(emptyBody()), List.of(), DocumentSettings.blank());
            case IEEE_STYLE -> ieeeStyle();
        };
    }

    public static DocumentSettings settings(DocumentTemplateId id) {
        return switch (id) {
            case BLANK -> DocumentSettings.blank();
            case IEEE_STYLE -> new DocumentSettings(
                    DocumentTemplateId.IEEE_STYLE,
                    PaperSize.letter(),
                    PageOrientation.PORTRAIT,
                    new PageMargins(PhysicalLength.inches(0.75), PhysicalLength.inches(0.625),
                            PhysicalLength.inches(0.75), PhysicalLength.inches(0.625)),
                    ColumnLayout.one(),
                    PageDecoration.none());
        };
    }

    private static Document ieeeStyle() {
        return new Document(List.of(
                paragraph(SemanticStyle.TITLE, "Paper Title"),
                paragraph(SemanticStyle.AUTHOR, "Author Name"),
                paragraph(SemanticStyle.AFFILIATION, "Affiliation"),
                paragraph(SemanticStyle.ABSTRACT, "Abstract—Write a concise summary."),
                paragraph(SemanticStyle.KEYWORDS, "Keywords—Add keywords."),
                new LayoutSectionBreak(ColumnLayout.two()),
                new Heading(1, text("Introduction"), Optional.of("introduction")),
                paragraph(SemanticStyle.BODY_TEXT, "Begin writing here.")), List.of(), settings(DocumentTemplateId.IEEE_STYLE));
    }

    private static Paragraph paragraph(SemanticStyle style) {
        return new Paragraph(new InlineContent(List.of()), style, ParagraphFormat.none());
    }

    private static Paragraph paragraph(SemanticStyle style, String value) {
        return new Paragraph(text(value), style, ParagraphFormat.none());
    }

    private static Paragraph emptyBody() {
        return paragraph(SemanticStyle.BODY_TEXT);
    }

    private static InlineContent text(String value) {
        return new InlineContent(List.of(new Text(value, java.util.Set.of())));
    }
}
