package dev.rgcb.scholar.client.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ScholarScreenRenderingPolicyTest {
    @Test void opaqueApplicationScreensRenderWidgetsWithoutReapplyingMinecraftBlur() throws Exception {
        for (var file : new String[] { "ScholarHomeScreen.java", "ScholarFileDialog.java" }) {
            var source = Files.readString(Path.of("src/main/java/dev/rgcb/scholar/client/screen").resolve(file));
            assertTrue(source.contains("ScholarScreenRendering.renderWidgets(renderables"), file);
            assertFalse(source.contains("super.render(graphics"), file);
        }
    }
}
