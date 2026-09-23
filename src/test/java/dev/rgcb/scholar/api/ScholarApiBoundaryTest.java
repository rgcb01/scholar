package dev.rgcb.scholar.api;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class ScholarApiBoundaryTest {
    @Test void publicSignaturesContainOnlyApiAndJdkTypes() {
        var types = List.of(ScholarApi.class, ScholarApiException.class,
                dev.rgcb.scholar.api.document.ScholarDocuments.class,
                dev.rgcb.scholar.api.document.ScholarDocument.class,
                dev.rgcb.scholar.api.document.ScholarEdit.class,
                dev.rgcb.scholar.api.data.ScholarData.class,
                dev.rgcb.scholar.api.quantity.ScholarQuantity.class,
                dev.rgcb.scholar.api.quantity.ScholarUnits.class,
                dev.rgcb.scholar.api.event.ScholarEvents.class);
        for (var type : types) {
            for (var method : type.getDeclaredMethods()) {
                if (!Modifier.isPublic(method.getModifiers())) continue;
                checkType(method.getGenericReturnType());
                for (var parameter : method.getGenericParameterTypes()) checkType(parameter);
                for (var exception : method.getGenericExceptionTypes()) checkType(exception);
            }
            for (var nested : type.getDeclaredClasses()) {
                if (!Modifier.isPublic(nested.getModifiers())) continue;
                for (var field : nested.getDeclaredFields()) {
                    if (Modifier.isPublic(field.getModifiers())) checkType(field.getGenericType());
                }
                for (var method : nested.getDeclaredMethods()) {
                    if (!Modifier.isPublic(method.getModifiers())) continue;
                    checkType(method.getGenericReturnType());
                    for (var parameter : method.getGenericParameterTypes()) checkType(parameter);
                }
            }
        }
    }

    private static void checkType(Type type) {
        if (type instanceof Class<?> clazz) {
            if (clazz.isArray()) { checkType(clazz.getComponentType()); return; }
            assertTrue(clazz.isPrimitive() || clazz.getName().startsWith("java.")
                    || clazz.getName().startsWith("dev.rgcb.scholar.api."), clazz.getName());
        } else if (type instanceof ParameterizedType parameterized) {
            checkType(parameterized.getRawType());
            for (var argument : parameterized.getActualTypeArguments()) checkType(argument);
        } else if (type instanceof WildcardType wildcard) {
            for (var bound : wildcard.getUpperBounds()) checkType(bound);
            for (var bound : wildcard.getLowerBounds()) checkType(bound);
        }
    }

    @Test void apiSourceDoesNotDependOnClientOrStorageImplementations() throws Exception {
        try (var files = Files.walk(Path.of("src/main/java/dev/rgcb/scholar/api"))) {
            for (var path : files.filter(file -> file.toString().endsWith(".java")).toList()) {
                var source = Files.readString(path);
                for (var forbidden : List.of("dev.rgcb.scholar.client.", "dev.rgcb.scholar.editor.",
                        "dev.rgcb.scholar.persistence.", "dev.rgcb.scholar.render.",
                        "FileScholarDocumentRepository", "ScholarEditorScreen")) {
                    assertFalse(source.contains(forbidden), path + " contains " + forbidden);
                }
            }
        }
    }

    @Test void referenceAddonImportsOnlyPublicScholarApi() throws Exception {
        var root = Path.of("examples/reference-addon/src/main/java");
        try (var files = Files.walk(root)) {
            var sources = files.filter(path -> path.toString().endsWith(".java")).toList();
            assertFalse(sources.isEmpty());
            for (var path : sources) {
                for (var line : Files.readAllLines(path)) {
                    if (line.strip().startsWith("import dev.rgcb.scholar.")) {
                        assertTrue(line.strip().startsWith("import dev.rgcb.scholar.api."), path + ": " + line);
                    }
                }
            }
        }
    }
}
