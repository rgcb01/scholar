package dev.rgcb.scholar.transfer;

import dev.rgcb.scholar.document.ComputationTransferBlock;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class TransferBoundaryTest {
    @Test
    void transferSourceHasNoPlatformEditorClipboardLayoutOrClientDependencies() throws IOException {
        var root = Path.of("src/main/java/dev/rgcb/scholar/transfer");
        var forbidden = List.of("net.minecraft", "net.neoforged", "com.mojang", "dev.rgcb.scholar.client",
                "dev.rgcb.scholar.editor", "dev.rgcb.scholar.clipboard", "dev.rgcb.scholar.layout", "dev.rgcb.scholar.render");
        try (var files = Files.walk(root)) {
            var sources = files.filter(path -> path.toString().endsWith(".java")).toList();
            assertFalse(sources.isEmpty());
            for (var source : sources) {
                var text = Files.readString(source);
                for (var dependency : forbidden) {
                    assertFalse(text.contains(dependency), source + " depends on " + dependency);
                }
            }
        }
    }

    @Test
    void semanticContentFamilyDoesNotIncludeLocalMathOrPlainTextCarrier() {
        var names = java.util.Arrays.stream(FragmentContent.class.getPermittedSubclasses())
                .map(Class::getSimpleName).collect(java.util.stream.Collectors.toSet());
        assertTrue(FragmentContent.class.isSealed());
        org.junit.jupiter.api.Assertions.assertEquals(java.util.Set.of("Blocks", "InlineSegments", "ResourcePrimary"), names);
    }

    @Test
    void computationDependenciesUseTheDocumentTransferBoundary() throws IOException {
        assertTrue(java.util.Arrays.asList(StableIdentityKind.values()).contains(StableIdentityKind.VARIABLE));
        assertTrue(dev.rgcb.scholar.document.BlockNode.class.isAssignableFrom(ComputationTransferBlock.class));
        var planner = Files.readString(Path.of("src/main/java/dev/rgcb/scholar/transfer/TransferPlanner.java"));
        assertTrue(planner.contains("UNRESOLVED_EXTERNAL_VARIABLE_DEPENDENCY"));
        assertFalse(planner.contains("authoredName()"));
        assertTrue(planner.contains("new ReferenceDispositionPlan"));
    }
}
