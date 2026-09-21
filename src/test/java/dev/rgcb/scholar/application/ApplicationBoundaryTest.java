package dev.rgcb.scholar.application;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class ApplicationBoundaryTest {
    @Test void applicationLayerStaysPlatformRenderingAndWidgetIndependent() throws Exception {
        var root = Path.of("src/main/java/dev/rgcb/scholar/application");
        var forbidden = List.of("net.minecraft", "net.neoforged", "com.mojang", "dev.rgcb.scholar.client",
                "dev.rgcb.scholar.layout", "dev.rgcb.scholar.render");
        try (var files = Files.walk(root)) {
            var sources = files.filter(path -> path.toString().endsWith(".java")).toList();
            assertFalse(sources.isEmpty());
            for (var source : sources) {
                var text = Files.readString(source);
                for (var dependency : forbidden) assertFalse(text.contains(dependency), source + " depends on " + dependency);
            }
        }
        assertTrue(Files.exists(root.resolve("ScholarDocumentRepository.java")));
    }
}
