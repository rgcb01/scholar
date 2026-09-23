package dev.rgcb.scholar.api.document;

import dev.rgcb.scholar.api.data.ScholarData;
import dev.rgcb.scholar.api.quantity.ScholarQuantity;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Restricted semantic authoring surface. IDs returned here are stable in the committed document. */
public interface ScholarEdit {
    /** Creates a document-owned dataset and returns its newly allocated stable ID. */
    String createDataset(String name, List<ScholarData.ColumnSpec> columns);
    /** Reads the staged dataset's column identities, including a dataset created in this edit. */
    List<ScholarData.Column> columns(String datasetId);
    /** Appends zero or more typed rows as part of this logical edit. */
    void appendRows(String datasetId, List<List<ScholarData.Cell>> rows);
    /** Appends one observation, converting named numeric measurements to their column units. Omitted columns are missing. */
    void appendMeasurement(String datasetId, Map<String, ScholarQuantity> byColumnId);
    void setCell(String datasetId, int rowIndex, String columnId, ScholarData.Cell value);
    /** Updates the value of a same-name variable in place, or creates a new one. */
    String defineVariable(String name, ScholarQuantity value);
    /** Adds an ordinary dataset-backed Scholar table. */
    void insertDatasetTable(String datasetId);
    /** Adds a dataset-backed scatter plot using numeric columns. */
    void insertDatasetPlot(String datasetId, String xColumnId, String yColumnId, String title);
    /** Adds an ordinary Figure containing a dataset-backed scatter plot and plain caption. */
    String insertFigurePlot(String datasetId, String xColumnId, String yColumnId, String title, String caption);
    /** Adds a supported authored analysis only when Scholar can evaluate it successfully. */
    String requestAnalysis(String datasetId, ScholarData.Analysis kind, Optional<String> xColumnId, String yColumnId);
}
