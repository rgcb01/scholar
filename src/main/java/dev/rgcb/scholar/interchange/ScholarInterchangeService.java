package dev.rgcb.scholar.interchange;

import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.StableIdAllocator;
import dev.rgcb.scholar.editor.EditorSession;
import dev.rgcb.scholar.layout.LaidOutDocument;
import dev.rgcb.scholar.validation.DocumentValidator;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.Collectors;

/** Application-owned interchange operations; never used as native document persistence. */
public final class ScholarInterchangeService {
    private final CsvDatasetInterchange csv = new CsvDatasetInterchange();

    public CsvDatasetInterchange.Preview previewCsv(Path file) throws IOException {
        return csv.preview(InterchangeFiles.readUtf8(file));
    }

    public boolean importCsv(EditorSession session, CsvDatasetInterchange.Preview preview, String name) {
        Objects.requireNonNull(session);
        Objects.requireNonNull(preview);
        var source = session.current().document();
        var occupied = source.datasets().stream().map(dataset -> dataset.id()).collect(Collectors.toSet());
        var id = StableIdAllocator.firstFree("imported-dataset", occupied);
        var dataset = preview.dataset(id, name);
        var datasets = new ArrayList<>(source.datasets());
        datasets.add(dataset);
        var candidate = new Document(source.blocks(), datasets, source.settings());
        if (!DocumentValidator.validate(candidate).isValid()) {
            throw new IllegalArgumentException("Imported dataset does not satisfy Scholar document validation");
        }
        return session.addDataset(dataset);
    }

    public void exportCsv(dev.rgcb.scholar.data.ScientificDataset dataset, Path target, boolean overwrite) throws IOException {
        InterchangeFiles.writeUtf8(target, csv.export(dataset), overwrite);
    }

    public void exportMarkdown(Document document, Path target, boolean overwrite) throws IOException {
        InterchangeFiles.writeUtf8(target, new MarkdownDocumentExporter().export(document), overwrite);
    }

    public void exportPdf(Document document, LaidOutDocument layout, Path target, boolean overwrite) throws IOException {
        InterchangeFiles.write(target, new PdfDocumentExporter().export(document, layout), overwrite);
    }
}
