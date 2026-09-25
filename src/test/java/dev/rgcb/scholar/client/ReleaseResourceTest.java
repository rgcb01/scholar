package dev.rgcb.scholar.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReleaseResourceTest {
    @Test
    void distributableResourcesIncludeLicensesAndProductMetadata() throws IOException {
        var loader = getClass().getClassLoader();
        for (var name : new String[] {"LICENSE", "LICENSE-EXCEPTION"}) {
            try (var stream = loader.getResourceAsStream("META-INF/licenses/scholar/" + name)) {
                assertNotNull(stream, name);
                assertTrue(stream.readAllBytes().length > 100, name);
            }
        }
        for (var name : new String[] {"OFL-SourceSans3.txt", "OFL-NotoSansMath.txt"}) {
            try (var stream = loader.getResourceAsStream("META-INF/licenses/fonts/" + name)) {
                assertNotNull(stream, name);
                assertTrue(new String(stream.readAllBytes(), StandardCharsets.UTF_8).contains("SIL OPEN FONT LICENSE"));
            }
        }
        try (var stream = loader.getResourceAsStream("META-INF/neoforge.mods.toml")) {
            assertNotNull(stream);
            var metadata = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(metadata.contains("modId=\"scholar\""));
            assertTrue(metadata.contains("version=\"1.0.0\""));
            assertTrue(metadata.contains("license=\"GPL-3.0-or-later\""));
            assertTrue(metadata.contains("authors=\"Rómulo Colorado (rgcb0)\""));
            assertTrue(metadata.contains("Scientific document editor for Minecraft"));
            assertFalse(metadata.contains("Minimal NeoForge mod foundation"));
        }
    }
}
