package dev.rgcb.scholar.application;

import static org.junit.jupiter.api.Assertions.*;

import dev.rgcb.scholar.client.DevelopmentDocument;
import dev.rgcb.scholar.clipboard.DocumentFragmentClipboardPayload;
import dev.rgcb.scholar.document.FigureBlock;
import dev.rgcb.scholar.document.Text;
import dev.rgcb.scholar.editor.BlockSelection;
import dev.rgcb.scholar.editor.DocumentPosition;
import dev.rgcb.scholar.editor.EditorSelectionValidator;
import dev.rgcb.scholar.editor.EditorState;
import dev.rgcb.scholar.persistence.PersistenceResult;
import dev.rgcb.scholar.validation.DocumentValidator;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class M29GoldenScenarioTest {
    @TempDir Path directory;

    @Test void twoScientificDocumentsSurviveTransferSaveAndApplicationRestartIndependently() {
        var ids = new AtomicInteger();
        var repository = repository(ids);
        assertTrue(success(repository.listDocuments()).isEmpty());
        var fixture = DevelopmentDocument.createEditable();
        var projectileRecord = success(repository.createDocument("Projectile Experiment", fixture));
        var electricalRecord = success(repository.createDocument("Electrical Lab", fixture));
        var application = new ScholarApplication(repository);
        var projectile = success(application.openDocument(projectileRecord.descriptor().id()));
        var electrical = success(application.openDocument(electricalRecord.descriptor().id()));

        assertNotEquals(projectile.id(), electrical.id());
        assertEquals(fixture.datasets().getFirst().id(), electrical.session().current().document().datasets().getFirst().id());
        assertNotSame(projectile.session(), electrical.session());
        var figureIndex = java.util.stream.IntStream.range(0, fixture.blocks().size())
                .filter(index -> fixture.blocks().get(index) instanceof FigureBlock figure && figure.id().equals("m25-transfer-figure"))
                .findFirst().orElseThrow();
        projectile.session().setCurrent(new EditorState(fixture, new BlockSelection(figureIndex), Optional.empty()));
        var copied = projectile.session().copyForClipboard().orElseThrow();
        var payload = (DocumentFragmentClipboardPayload) copied.payload().orElseThrow();

        electrical.session().setCurrent(new EditorState(electrical.session().current().document(),
                new DocumentPosition(0, 0), new DocumentPosition(0, 0)));
        assertTrue(electrical.session().pasteFromClipboard(Optional.of(payload), copied.plainText()));
        assertEquals(1, electrical.session().undoDepth());
        assertEquals(fixture.datasets().size() + 1, electrical.session().current().document().datasets().size());
        assertEquals(fixture, projectile.session().current().document());
        var pasted = (FigureBlock) electrical.session().current().document().blocks().getFirst();
        assertInstanceOf(Text.class, pasted.caption().nodes().get(3));
        assertTrue(DocumentValidator.validate(electrical.session().current().document()).isValid());
        success(electrical.save());
        success(projectile.save());

        var restarted = new ScholarApplication(new FileScholarDocumentRepository(directory));
        assertEquals(2, success(restarted.documents()).size());
        var restoredProjectile = success(restarted.openDocument(projectile.id()));
        var restoredElectrical = success(restarted.openDocument(electrical.id()));
        assertEquals(0, restoredProjectile.session().undoDepth());
        assertEquals(0, restoredElectrical.session().undoDepth());
        assertNotEquals(restoredProjectile.session(), restoredElectrical.session());
        assertTrue(DocumentValidator.validate(restoredProjectile.session().current().document()).isValid());
        assertTrue(DocumentValidator.validate(restoredElectrical.session().current().document()).isValid());
        assertTrue(new EditorSelectionValidator().isValid(restoredProjectile.session().current()));
        assertTrue(new EditorSelectionValidator().isValid(restoredElectrical.session().current()));
    }

    private FileScholarDocumentRepository repository(AtomicInteger ids) {
        return new FileScholarDocumentRepository(directory,
                Clock.fixed(Instant.parse("2026-09-19T18:00:00Z"), ZoneOffset.UTC),
                () -> new ScholarDocumentId("golden-" + ids.incrementAndGet()));
    }
    @SuppressWarnings("unchecked") private static <T> T success(PersistenceResult<T> result) { return ((PersistenceResult.Success<T>) result).value(); }
}
