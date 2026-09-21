package dev.rgcb.scholar.editor;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** Platform-neutral shortcut metadata shared by UI labels and host key matching. */
public record ActionShortcut(List<Stroke> strokes) {
    public ActionShortcut {
        strokes = List.copyOf(Objects.requireNonNull(strokes, "strokes"));
        if (strokes.isEmpty()) {
            throw new IllegalArgumentException("A shortcut requires at least one stroke.");
        }
    }

    public static ActionShortcut of(Stroke primary, Stroke... alternatives) {
        var strokes = new java.util.ArrayList<Stroke>();
        strokes.add(Objects.requireNonNull(primary, "primary"));
        strokes.addAll(Arrays.asList(alternatives));
        return new ActionShortcut(strokes);
    }

    public static ActionShortcut ctrl(Key key) {
        return of(new Stroke(key, true, false));
    }

    public static ActionShortcut plain(Key key) {
        return of(new Stroke(key, false, false));
    }

    public String displayText() {
        return strokes.getFirst().displayText();
    }

    public enum Key {
        A("A"), B("B"), C("C"), I("I"), N("N"), O("O"), S("S"), V("V"), X("X"), Y("Y"), Z("Z"),
        DELETE("Del");

        private final String displayText;

        Key(String displayText) {
            this.displayText = displayText;
        }
    }

    public record Stroke(Key key, boolean control, boolean shift) {
        public Stroke {
            Objects.requireNonNull(key, "key");
        }

        public String displayText() {
            return (control ? "Ctrl+" : "") + (shift ? "Shift+" : "") + key.displayText;
        }
    }
}
