package dev.rgcb.scholar.interchange;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** RFC 4180-style records, with LF or CRLF accepted on input. */
final class CsvRecords {
    private CsvRecords() { }

    record Field(String value, boolean quoted) { }

    static List<List<Field>> parse(String input) {
        Objects.requireNonNull(input, "input");
        var text = input.startsWith("\uFEFF") ? input.substring(1) : input;
        var rows = new ArrayList<List<Field>>();
        var row = new ArrayList<Field>();
        var field = new StringBuilder();
        var quoted = false;
        var closed = false;
        var started = false;
        var quotedField = false;
        for (var i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        quoted = false;
                        closed = true;
                    }
                } else field.append(c);
                continue;
            }
            if (c == '"') {
                if (started || closed) throw new IllegalArgumentException("Unexpected CSV quote at character " + i);
                quoted = true;
                started = true;
                quotedField = true;
            } else if (c == ',') {
                row.add(new Field(field.toString(), quotedField));
                field.setLength(0);
                started = false;
                closed = false;
                quotedField = false;
            } else if (c == '\r' || c == '\n') {
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') i++;
                row.add(new Field(field.toString(), quotedField));
                rows.add(List.copyOf(row));
                row.clear();
                field.setLength(0);
                started = false;
                closed = false;
                quotedField = false;
            } else {
                if (closed) throw new IllegalArgumentException("Unexpected text after CSV quote at character " + i);
                field.append(c);
                started = true;
            }
        }
        if (quoted) throw new IllegalArgumentException("Unclosed quoted CSV field");
        if (started || closed || !row.isEmpty() || field.length() > 0) {
            row.add(new Field(field.toString(), quotedField));
            rows.add(List.copyOf(row));
        }
        return List.copyOf(rows);
    }

    static String writeDataset(List<List<Field>> rows) {
        var out = new StringBuilder();
        for (var row : rows) {
            for (var i = 0; i < row.size(); i++) {
                if (i > 0) out.append(',');
                var field = row.get(i);
                var value = field.value();
                if (field.quoted() || value.indexOf(',') >= 0 || value.indexOf('"') >= 0
                        || value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0
                        || value.startsWith(" ") || value.endsWith(" ")) {
                    out.append('"').append(value.replace("\"", "\"\"")).append('"');
                } else out.append(value);
            }
            out.append("\r\n");
        }
        return out.toString();
    }
}
