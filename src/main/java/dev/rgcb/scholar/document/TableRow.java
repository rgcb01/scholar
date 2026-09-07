package dev.rgcb.scholar.document;

import java.util.List;
import java.util.Objects;

public record TableRow(List<TableCell> cells) {
    public TableRow {
        cells = List.copyOf(Objects.requireNonNull(cells, "cells"));
        if (cells.isEmpty()) {
            throw new IllegalArgumentException("cells must not be empty.");
        }
    }
}
