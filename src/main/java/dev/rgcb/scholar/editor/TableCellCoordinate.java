package dev.rgcb.scholar.editor;

public record TableCellCoordinate(int rowIndex, int columnIndex) {
    public TableCellCoordinate {
        if (rowIndex < 0) {
            throw new IllegalArgumentException("rowIndex must not be negative.");
        }
        if (columnIndex < 0) {
            throw new IllegalArgumentException("columnIndex must not be negative.");
        }
    }
}
