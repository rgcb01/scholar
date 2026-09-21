package dev.rgcb.scholar.application;

import static org.junit.jupiter.api.Assertions.*;

import dev.rgcb.scholar.document.CrossReference;
import dev.rgcb.scholar.document.CrossReferenceTargetKind;
import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.Heading;
import dev.rgcb.scholar.document.InlineContent;
import dev.rgcb.scholar.document.Paragraph;
import dev.rgcb.scholar.document.Text;
import dev.rgcb.scholar.editor.BlockSelection;
import dev.rgcb.scholar.editor.EditorState;
import dev.rgcb.scholar.persistence.FileDocumentStorage;
import dev.rgcb.scholar.persistence.PersistenceResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileScholarDocumentRepositoryTest {
    @TempDir Path directory;

    @Test void createListOpenAndRestartUseStableApplicationIdentityAndDerivedPreview() {
        var repository = repository();
        var document = document("section-id", "Motion", "Velocity changes over time.");
        var created = success(repository.createDocument("Kinematics Notes", document));

        assertEquals("doc-1", created.descriptor().id().value());
        assertEquals("Kinematics Notes", created.descriptor().displayName());
        assertEquals("Motion", created.descriptor().preview().title());
        assertEquals("Velocity changes over time.", created.descriptor().preview().excerpt());
        assertEquals(document, success(repository.openDocument(created.descriptor().id())).document());

        var restarted = new FileScholarDocumentRepository(directory);
        var listed = success(restarted.listDocuments());
        assertEquals(1, listed.size());
        assertEquals(created.descriptor().id(), listed.getFirst().id());
        assertEquals("Kinematics Notes", listed.getFirst().displayName());
    }

    @Test void corruptMetadataDegradesToRecoverableDescriptorWithoutDamagingM26Document() throws Exception {
        var repository = repository();
        var created = success(repository.createDocument("Safe", document("h", "Title", "Body")));
        Files.writeString(directory.resolve("workspace.json"), "not json");

        var listed = success(new FileScholarDocumentRepository(directory).listDocuments());
        assertEquals(List.of(created.descriptor().id()), listed.stream().map(ScholarDocumentDescriptor::id).toList());
        assertEquals(created.document(), success(new FileScholarDocumentRepository(directory).openDocument(created.descriptor().id())).document());
        assertEquals("Preview unavailable", listed.getFirst().preview().excerpt());
    }

    @Test void existingM26StorageFilesRemainDiscoverableAndLoadable() {
        var document = document("legacy-heading", "Legacy", "Still valid");
        success(new FileDocumentStorage(directory.resolve("documents")).save("legacy", document));

        var repository = new FileScholarDocumentRepository(directory);
        var descriptor = success(repository.listDocuments()).getFirst();
        assertEquals(new ScholarDocumentId("legacy"), descriptor.id());
        assertEquals(document, success(repository.openDocument(descriptor.id())).document());
    }

    @Test void renameChangesOnlyLibraryMetadataAndSaveAsCreatesDistinctIdentityWithExactAst() {
        var repository = repository();
        var original = success(repository.createDocument("Original", document("same-semantic-id", "A", "B")));
        var renamed = success(repository.renameDocument(original.descriptor().id(), "Renamed"));
        assertEquals(original.descriptor().id(), renamed.id());
        assertEquals("Renamed", renamed.displayName());
        assertEquals(original.document(), success(repository.openDocument(renamed.id())).document());

        var copy = success(repository.saveAs("Independent", original.document()));
        assertNotEquals(original.descriptor().id(), copy.descriptor().id());
        assertEquals(original.document(), copy.document());
        assertEquals("same-semantic-id", ((Heading) copy.document().blocks().getFirst()).id().orElseThrow());
    }

    @Test void invalidDisplayNamesAndRepeatedIdsFailWithoutReplacingExistingContent() {
        var repository = new FileScholarDocumentRepository(directory, Clock.systemUTC(), () -> new ScholarDocumentId("same-id"));
        assertInstanceOf(PersistenceResult.Failure.class, repository.createDocument(" \u0000 ", ScholarDocuments.blank()));
        var first = success(repository.createDocument("First", ScholarDocuments.blank()));
        assertInstanceOf(PersistenceResult.Failure.class, repository.createDocument("Second", ScholarDocuments.blank()));
        assertEquals(ScholarDocuments.blank(), success(repository.openDocument(first.descriptor().id())).document());
    }

    @Test void listUsesPersistedModifiedRecencyWithoutLoadingEveryDocument() {
        var clock = new MutableClock(100);
        var counter = new AtomicInteger();
        var repository = new FileScholarDocumentRepository(directory, clock,
                () -> new ScholarDocumentId("recent-" + counter.incrementAndGet()));
        var first = success(repository.createDocument("First", ScholarDocuments.blank()));
        clock.millis = 200;
        var second = success(repository.createDocument("Second", ScholarDocuments.blank()));
        assertEquals(second.descriptor().id(), success(repository.listDocuments()).getFirst().id());
        clock.millis = 300;
        success(repository.saveDocument(first.descriptor().id(), ScholarDocuments.blank()));
        assertEquals(first.descriptor().id(), success(repository.listDocuments()).getFirst().id());
    }

    private FileScholarDocumentRepository repository() {
        var counter = new AtomicInteger();
        return new FileScholarDocumentRepository(directory, Clock.fixed(Instant.parse("2026-09-19T12:00:00Z"), ZoneOffset.UTC),
                () -> new ScholarDocumentId("doc-" + counter.incrementAndGet()));
    }

    private static Document document(String headingId, String heading, String body) {
        return new Document(List.of(
                new Heading(headingId, 1, inline(heading)),
                new Paragraph(new InlineContent(List.of(new Text(body, Set.of()), new CrossReference(CrossReferenceTargetKind.SECTION, headingId))))));
    }

    private static InlineContent inline(String text) { return new InlineContent(List.of(new Text(text, Set.of()))); }

    @SuppressWarnings("unchecked")
    private static <T> T success(PersistenceResult<T> result) {
        return ((PersistenceResult.Success<T>) result).value();
    }

    private static final class MutableClock extends Clock {
        private long millis;
        private MutableClock(long millis) { this.millis = millis; }
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return Instant.ofEpochMilli(millis); }
        public long millis() { return millis; }
    }
}
