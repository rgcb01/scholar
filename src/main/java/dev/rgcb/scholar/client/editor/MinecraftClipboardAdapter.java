package dev.rgcb.scholar.client.editor;

import dev.rgcb.scholar.editor.ClipboardAdapter;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MinecraftClipboardAdapter implements ClipboardAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(MinecraftClipboardAdapter.class);

    private final Minecraft minecraft;

    public MinecraftClipboardAdapter(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    @Override
    public String getText() {
        if (minecraft == null) {
            return "";
        }
        try {
            return minecraft.keyboardHandler.getClipboard();
        } catch (RuntimeException exception) {
            LOGGER.warn("Could not read system clipboard.", exception);
            return "";
        }
    }

    @Override
    public boolean setText(String text) {
        if (minecraft == null) {
            return false;
        }
        try {
            minecraft.keyboardHandler.setClipboard(text);
            return true;
        } catch (RuntimeException exception) {
            LOGGER.warn("Could not write system clipboard.", exception);
            return false;
        }
    }
}
