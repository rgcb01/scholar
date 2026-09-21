package dev.rgcb.scholar.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rgcb.scholar.data.ScientificDataset;
import dev.rgcb.scholar.document.CrossReference;
import dev.rgcb.scholar.document.CrossReferenceTargetKind;
import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.Heading;
import dev.rgcb.scholar.document.InlineContent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TransferContextTest {
    @Test
    void sameTokenProvesLiveDocumentRelationOnly() {
        var token = RuntimeDocumentToken.create();
        var source = metadata(Optional.of(token));
        var context = new TransferContext(new Document(List.of()), Optional.of(token), source);
        assertTrue(context.hasSameDocumentToken());
        assertTrue(source.targetWitnesses().isEmpty());
        assertTrue(source.datasetWitnesses().isEmpty());
        assertSame(token, context.destinationToken().orElseThrow());
    }

    @Test
    void differentTokensDoNotProveRelationEvenWithEqualDocuments() {
        var first = RuntimeDocumentToken.create();
        var second = RuntimeDocumentToken.create();
        assertNotEquals(first, second);
        var context = new TransferContext(new Document(List.of()), Optional.of(second), metadata(Optional.of(first)));
        assertFalse(context.hasSameDocumentToken());
    }

    @Test
    void unknownSourceOrDestinationTokenNeverProvesRelation() {
        var token = RuntimeDocumentToken.create();
        var document = new Document(List.of());
        assertFalse(new TransferContext(document, Optional.empty(), metadata(Optional.of(token))).hasSameDocumentToken());
        assertFalse(new TransferContext(document, Optional.of(token), SourceTransferMetadata.unknown()).hasSameDocumentToken());
        assertFalse(new TransferContext(document, Optional.empty(), SourceTransferMetadata.unknown()).hasSameDocumentToken());
    }

    @Test
    void provenanceAndSourceExportMetadataAreNotPartOfDocumentOrFragment() {
        var heading = new Heading("h1", 1, new InlineContent(List.of()));
        var document = new Document(List.of(heading));
        var before = new Document(List.of(heading));
        var context = new TransferContext(document, Optional.of(RuntimeDocumentToken.create()), SourceTransferMetadata.unknown());
        assertSame(document, context.destination());
        assertEquals(before, document);
        assertEquals(Set.of("blocks", "datasets", "settings"), java.util.Arrays.stream(Document.class.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName).collect(java.util.stream.Collectors.toSet()));
        assertEquals(Set.of("content", "resources", "identities"), java.util.Arrays.stream(DocumentFragment.class.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName).collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void witnessAndExportMapsAreImmutableAndKeepOriginalInstances() {
        var targetKey = new StableIdentityKey(StableIdentityKind.SECTION, "h1");
        var resourceKey = new StableIdentityKey(StableIdentityKind.DATASET, "d1");
        var target = new Heading("h1", 1, new InlineContent(List.of()));
        var resource = new ScientificDataset("d1", Optional.empty(), List.of(), List.of());
        var targets = new HashMap<StableIdentityKey, dev.rgcb.scholar.document.BlockNode>();
        targets.put(targetKey, target);
        var resources = new HashMap<StableIdentityKey, ScientificDataset>();
        resources.put(resourceKey, resource);
        var text = new HashMap<StableIdentityKey, String>();
        text.put(targetKey, "Section 1");
        var metadata = new SourceTransferMetadata(Optional.empty(), targets, resources, text);
        targets.clear();
        resources.clear();
        text.clear();
        assertSame(target, metadata.targetWitnesses().get(targetKey));
        assertSame(resource, metadata.datasetWitnesses().get(resourceKey));
        assertEquals("Section 1", metadata.referenceText().get(targetKey));
        assertThrows(UnsupportedOperationException.class, () -> metadata.targetWitnesses().clear());
        assertThrows(UnsupportedOperationException.class, () -> metadata.datasetWitnesses().clear());
        assertThrows(UnsupportedOperationException.class, () -> metadata.referenceText().clear());
    }

    @Test
    void witnessKeyMustMatchValueAndNamespace() {
        var heading = new Heading("h1", 1, new InlineContent(List.of()));
        assertThrows(IllegalArgumentException.class, () -> new SourceTransferMetadata(Optional.empty(),
                Map.of(new StableIdentityKey(StableIdentityKind.SECTION, "other"), heading), Map.of(), Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new SourceTransferMetadata(Optional.empty(),
                Map.of(new StableIdentityKey(StableIdentityKind.FIGURE, "h1"), heading), Map.of(), Map.of()));
        var dataset = new ScientificDataset("d1", Optional.empty(), List.of(), List.of());
        assertThrows(IllegalArgumentException.class, () -> new SourceTransferMetadata(Optional.empty(), Map.of(),
                Map.of(new StableIdentityKey(StableIdentityKind.DATASET, "other"), dataset), Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new SourceTransferMetadata(Optional.empty(), Map.of(), Map.of(),
                Map.of(new StableIdentityKey(StableIdentityKind.DATASET, "d1"), "not a reference label")));
    }

    @Test
    void nullContextAndMetadataFieldsAreRejected() {
        assertThrows(NullPointerException.class, () -> new TransferContext(null, Optional.empty(), SourceTransferMetadata.unknown()));
        assertThrows(NullPointerException.class, () -> new TransferContext(new Document(List.of()), null, SourceTransferMetadata.unknown()));
        assertThrows(NullPointerException.class, () -> new TransferContext(new Document(List.of()), Optional.empty(), null));
        assertThrows(NullPointerException.class, () -> new SourceTransferMetadata(null, Map.of(), Map.of(), Map.of()));
        assertThrows(NullPointerException.class, () -> new SourceTransferMetadata(Optional.empty(), null, Map.of(), Map.of()));
    }

    @Test
    void typedIdentityKeysRejectNullAndBlankWithoutNormalizingSourceIds() {
        assertThrows(NullPointerException.class, () -> new StableIdentityKey(null, "a"));
        assertThrows(NullPointerException.class, () -> new StableIdentityKey(StableIdentityKind.FIGURE, null));
        assertThrows(IllegalArgumentException.class, () -> new StableIdentityKey(StableIdentityKind.FIGURE, ""));
        assertThrows(IllegalArgumentException.class, () -> new StableIdentityKey(StableIdentityKind.FIGURE, "  "));
        assertEquals(" original ", new StableIdentityKey(StableIdentityKind.FIGURE, " original ").id());
        assertNotEquals(new StableIdentityKey(StableIdentityKind.FIGURE, "a"), new StableIdentityKey(StableIdentityKind.TABLE, "a"));
    }

    @Test
    void eachReferenceKindMapsExplicitlyToItsNamespace() {
        assertEquals(new StableIdentityKey(StableIdentityKind.SECTION, "id"),
                StableIdentityKey.targetOf(new CrossReference(CrossReferenceTargetKind.SECTION, "id")));
        assertEquals(new StableIdentityKey(StableIdentityKind.TABLE, "id"),
                StableIdentityKey.targetOf(new CrossReference(CrossReferenceTargetKind.TABLE, "id")));
        assertEquals(new StableIdentityKey(StableIdentityKind.FIGURE, "id"),
                StableIdentityKey.targetOf(new CrossReference(CrossReferenceTargetKind.FIGURE, "id")));
        assertEquals(new StableIdentityKey(StableIdentityKind.EQUATION, "id"),
                StableIdentityKey.targetOf(new CrossReference(CrossReferenceTargetKind.EQUATION, "id")));
    }

    @Test
    void diagnosticKeepsOriginalTypedIdentityAndOperationalLocation() {
        var key = new StableIdentityKey(StableIdentityKind.FIGURE, "fig-a");
        var diagnostic = new TransferDiagnostic(TransferDiagnostic.Severity.WARNING,
                TransferDiagnostic.Code.EXTERNAL_REFERENCE_DEGRADED, "External reference will lose active link semantics.",
                Optional.of(key), Optional.of("roots[0].caption.nodes[1]"));
        assertEquals(key, diagnostic.sourceIdentity().orElseThrow());
        assertEquals(TransferDiagnostic.Severity.WARNING, diagnostic.severity());
        assertEquals("roots[0].caption.nodes[1]", diagnostic.fragmentLocation().orElseThrow());
        for (var severity : TransferDiagnostic.Severity.values()) {
            assertEquals(severity, new TransferDiagnostic(severity, TransferDiagnostic.Code.MALFORMED_FRAGMENT,
                    "Message", Optional.empty(), Optional.empty()).severity());
        }
    }

    @Test
    void malformedDiagnosticsAreRejected() {
        assertThrows(NullPointerException.class, () -> new TransferDiagnostic(null,
                TransferDiagnostic.Code.MALFORMED_FRAGMENT, "Message", Optional.empty(), Optional.empty()));
        assertThrows(NullPointerException.class, () -> new TransferDiagnostic(TransferDiagnostic.Severity.ERROR,
                null, "Message", Optional.empty(), Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> new TransferDiagnostic(TransferDiagnostic.Severity.ERROR,
                TransferDiagnostic.Code.MALFORMED_FRAGMENT, " ", Optional.empty(), Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> new TransferDiagnostic(TransferDiagnostic.Severity.ERROR,
                TransferDiagnostic.Code.MALFORMED_FRAGMENT, "Message", Optional.empty(), Optional.of("")));
    }

    private static SourceTransferMetadata metadata(Optional<RuntimeDocumentToken> token) {
        return new SourceTransferMetadata(token, Map.of(), Map.of(), Map.of());
    }
}
