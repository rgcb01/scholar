package dev.rgcb.scholar.persistence;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PersistenceBoundaryTest {
    @Test void codecAndStorageStayPlatformAndEditorIndependent() throws Exception {
        try (var files = Files.walk(Path.of("src/main/java/dev/rgcb/scholar/persistence"))) {
            for (var file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                var source = Files.readString(file);
                assertFalse(source.matches("(?s).*import (net\\.minecraft|net\\.neoforged|com\\.mojang|dev\\.rgcb\\.scholar\\.(client|editor|layout|transfer|clipboard)).*"), file.toString());
                assertFalse(source.contains("ObjectOutputStream"), file.toString());
                assertFalse(source.contains("ObjectInputStream"), file.toString());
            }
        }
    }
}
