package dev.rgcb.scholar.document;

import java.util.Objects;

public record DocumentSettings(DocumentTemplateId template, PaperSize paper, PageOrientation orientation,
                               PageMargins margins, ColumnLayout columns, PageDecoration decoration) {
    public DocumentSettings {
        template = Objects.requireNonNull(template); paper = Objects.requireNonNull(paper);
        orientation = Objects.requireNonNull(orientation); margins = Objects.requireNonNull(margins);
        columns = Objects.requireNonNull(columns); decoration = Objects.requireNonNull(decoration);
    }
    public static DocumentSettings blank() { return new DocumentSettings(DocumentTemplateId.BLANK, PaperSize.letter(), PageOrientation.PORTRAIT, PageMargins.normal(), ColumnLayout.one(), PageDecoration.none()); }
    public int pageWidth() { return (orientation == PageOrientation.PORTRAIT ? paper.width() : paper.height()).logicalUnits(); }
    public int pageHeight() { return (orientation == PageOrientation.PORTRAIT ? paper.height() : paper.width()).logicalUnits(); }
    public int contentWidth() { return pageWidth() - margins.left().logicalUnits() - margins.right().logicalUnits(); }
    public int contentHeight() { return pageHeight() - margins.top().logicalUnits() - margins.bottom().logicalUnits(); }
    public DocumentSettings withColumns(ColumnLayout value) { return new DocumentSettings(template, paper, orientation, margins, value, decoration); }
}
