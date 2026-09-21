package dev.rgcb.scholar.client.ui;

import dev.rgcb.scholar.editor.ActionShortcut;
import java.util.Objects;
import org.lwjgl.glfw.GLFW;

/** Maps platform-neutral action shortcuts to GLFW input without leaking host keys into core. */
public final class MinecraftShortcutMatcher {
    private MinecraftShortcutMatcher() {
    }

    public static boolean matches(ActionShortcut shortcut, int keyCode, int modifiers) {
        Objects.requireNonNull(shortcut, "shortcut");
        var control = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        var shift = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
        return shortcut.strokes().stream().anyMatch(stroke ->
                stroke.control() == control && stroke.shift() == shift && keyCode(stroke.key()) == keyCode);
    }

    private static int keyCode(ActionShortcut.Key key) {
        return switch (key) {
            case A -> GLFW.GLFW_KEY_A;
            case B -> GLFW.GLFW_KEY_B;
            case C -> GLFW.GLFW_KEY_C;
            case I -> GLFW.GLFW_KEY_I;
            case N -> GLFW.GLFW_KEY_N;
            case O -> GLFW.GLFW_KEY_O;
            case S -> GLFW.GLFW_KEY_S;
            case V -> GLFW.GLFW_KEY_V;
            case X -> GLFW.GLFW_KEY_X;
            case Y -> GLFW.GLFW_KEY_Y;
            case Z -> GLFW.GLFW_KEY_Z;
            case DELETE -> GLFW.GLFW_KEY_DELETE;
        };
    }
}
