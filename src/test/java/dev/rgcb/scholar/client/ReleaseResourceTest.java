package dev.rgcb.scholar.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReleaseResourceTest {
    @Test
    void distributableResourcesIncludeFontNoticesAndProductMetadata() throws IOException {
        var loader = getClass().getClassLoader();
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
            assertTrue(metadata.contains("Scientific document editor for Minecraft"));
            assertFalse(metadata.contains("Minimal NeoForge mod foundation"));
        }
    }
}
