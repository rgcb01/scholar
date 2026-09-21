package dev.rgcb.scholar.layout;

import dev.rgcb.scholar.diagram.layout.LaidOutDiagram;
import dev.rgcb.scholar.math.layout.LaidOutMath;
import dev.rgcb.scholar.plot.layout.LaidOutPlot;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record LaidOutBlock(
        LaidOutBlockKind kind,
        int headingLevel,
        int x,
        int y,
        int width,
        int height,
        List<LaidOutLine> lines,
        Optional<LaidOutMath> math,
        Optional<LaidOutTable> table,
        Optional<LaidOutPlot> plot,
        Optional<LaidOutDiagram> diagram,
        Optional<LaidOutFigure> figure,
        Optional<LaidOutTableOfContents> tableOfContents
) {
    public boolean intersectsVerticalViewport(int top, int viewportHeight) {
        return viewportHeight > 0 && (long) y + height >= top && y <= (long) top + viewportHeight;
    }

    public LaidOutBlock(
            LaidOutBlockKind kind,
            int headingLevel,
            int x,
            int y,
            int width,
            int height,
            List<LaidOutLine> lines
    ) {
        this(kind, headingLevel, x, y, width, height, lines, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public LaidOutBlock(
            LaidOutBlockKind kind,
            int headingLevel,
            int x,
            int y,
            int width,
            int height,
            List<LaidOutLine> lines,
            Optional<LaidOutMath> math
    ) {
        this(kind, headingLevel, x, y, width, height, lines, math, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public LaidOutBlock(
            LaidOutBlockKind kind,
            int headingLevel,
            int x,
            int y,
            int width,
            int height,
            List<LaidOutLine> lines,
            Optional<LaidOutMath> math,
            Optional<LaidOutTable> table
    ) {
        this(kind, headingLevel, x, y, width, height, lines, math, table, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public LaidOutBlock(
            LaidOutBlockKind kind,
            int headingLevel,
            int x,
            int y,
            int width,
            int height,
            List<LaidOutLine> lines,
            Optional<LaidOutMath> math,
            Optional<LaidOutTable> table,
            Optional<LaidOutPlot> plot
    ) {
        this(kind, headingLevel, x, y, width, height, lines, math, table, plot, Optional.empty(), Optional.empty(), Optional.empty());
    }

    public LaidOutBlock(
            LaidOutBlockKind kind,
            int headingLevel,
            int x,
            int y,
            int width,
            int height,
            List<LaidOutLine> lines,
            Optional<LaidOutMath> math,
            Optional<LaidOutTable> table,
            Optional<LaidOutPlot> plot,
            Optional<LaidOutDiagram> diagram
    ) {
        this(kind, headingLevel, x, y, width, height, lines, math, table, plot, diagram, Optional.empty(), Optional.empty());
    }

    public LaidOutBlock {
        kind = Objects.requireNonNull(kind, "kind");
        if (width < 0) {
            throw new IllegalArgumentException("width must not be negative.");
        }
        if (height < 0) {
            throw new IllegalArgumentException("height must not be negative.");
        }
        lines = List.copyOf(lines);
        math = Objects.requireNonNull(math, "math");
        table = Objects.requireNonNull(table, "table");
        plot = Objects.requireNonNull(plot, "plot");
        diagram = Objects.requireNonNull(diagram, "diagram");
        figure = Objects.requireNonNull(figure, "figure");
        tableOfContents = Objects.requireNonNull(tableOfContents, "tableOfContents");

        if (kind == LaidOutBlockKind.EQUATION && (math.isEmpty() || !lines.isEmpty() || table.isPresent() || plot.isPresent() || diagram.isPresent() || figure.isPresent() || tableOfContents.isPresent())) {
            throw new IllegalArgumentException("Equation blocks must contain laid-out math only.");
        }
        if (kind != LaidOutBlockKind.EQUATION && math.isPresent()) {
            throw new IllegalArgumentException("Only equation blocks may contain laid-out math.");
        }
        if (kind == LaidOutBlockKind.TABLE && (table.isEmpty() || !lines.isEmpty() || math.isPresent() || plot.isPresent() || diagram.isPresent() || figure.isPresent() || tableOfContents.isPresent())) {
            throw new IllegalArgumentException("Table blocks must contain laid-out table data only.");
        }
        if (kind != LaidOutBlockKind.TABLE && table.isPresent()) {
            throw new IllegalArgumentException("Only table blocks may contain laid-out table data.");
        }
        if (kind == LaidOutBlockKind.PLOT && (plot.isEmpty() || !lines.isEmpty() || math.isPresent() || table.isPresent() || diagram.isPresent() || figure.isPresent() || tableOfContents.isPresent())) {
            throw new IllegalArgumentException("Plot blocks must contain laid-out plot data only.");
        }
        if (kind != LaidOutBlockKind.PLOT && kind != LaidOutBlockKind.FIGURE && plot.isPresent()) {
            throw new IllegalArgumentException("Only plot blocks may contain laid-out plot data.");
        }
        if (kind == LaidOutBlockKind.DIAGRAM && (diagram.isEmpty() || !lines.isEmpty() || math.isPresent() || table.isPresent() || plot.isPresent() || figure.isPresent() || tableOfContents.isPresent())) {
            throw new IllegalArgumentException("Diagram blocks must contain laid-out diagram data only.");
        }
        if (kind != LaidOutBlockKind.DIAGRAM && kind != LaidOutBlockKind.FIGURE && diagram.isPresent()) {
            throw new IllegalArgumentException("Only diagram blocks may contain laid-out diagram data.");
        }
        if (kind == LaidOutBlockKind.FIGURE && (figure.isEmpty() || math.isPresent() || table.isPresent() || tableOfContents.isPresent())) {
            throw new IllegalArgumentException("Figure blocks must contain laid-out figure data only.");
        }
        if (kind != LaidOutBlockKind.FIGURE && figure.isPresent()) {
            throw new IllegalArgumentException("Only figure blocks may contain laid-out figure data.");
        }
        if (kind == LaidOutBlockKind.TABLE_OF_CONTENTS && (tableOfContents.isEmpty() || math.isPresent() || table.isPresent() || plot.isPresent() || diagram.isPresent() || figure.isPresent())) {
            throw new IllegalArgumentException("Table of contents blocks must contain laid-out TOC data only.");
        }
        if (kind != LaidOutBlockKind.TABLE_OF_CONTENTS && tableOfContents.isPresent()) {
            throw new IllegalArgumentException("Only table of contents blocks may contain laid-out TOC data.");
        }
    }
}
