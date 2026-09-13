package dev.rgcb.scholar.client.render;

import dev.rgcb.scholar.diagram.layout.LaidOutDiagram;
import dev.rgcb.scholar.diagram.layout.LaidOutDiagramLabel;
import dev.rgcb.scholar.electrical.layout.LaidOutElectricalCircle;
import dev.rgcb.scholar.electrical.layout.LaidOutElectricalLine;
import dev.rgcb.scholar.electrical.layout.LaidOutElectricalPolyline;
import dev.rgcb.scholar.electrical.layout.LaidOutElectricalPrimitive;
import dev.rgcb.scholar.mechanical.layout.LaidOutMechanicalPrimitive;
import dev.rgcb.scholar.mechanical.layout.LaidOutMechanicalDimension;
import dev.rgcb.scholar.mechanical.layout.LaidOutMechanicalConstraint;
import dev.rgcb.scholar.mechanical.layout.LaidOutMechanicalSymbol;
import dev.rgcb.scholar.mechanical.layout.LaidOutMechanicalAnnotation;
import dev.rgcb.scholar.mechanical.layout.LaidOutMechanicalPartReference;
import dev.rgcb.scholar.layout.LaidOutDocument;
import dev.rgcb.scholar.layout.LaidOutFigure;
import dev.rgcb.scholar.layout.LaidOutTable;
import dev.rgcb.scholar.layout.LaidOutText;
import dev.rgcb.scholar.plot.layout.LaidOutPlot;
import dev.rgcb.scholar.plot.PlotSeriesKind;
import dev.rgcb.scholar.plot.layout.LaidOutPlotLabel;
import dev.rgcb.scholar.plot.layout.PlotLinePattern;
import dev.rgcb.scholar.plot.layout.PlotMarkerShape;
import dev.rgcb.scholar.plot.layout.PlotLayoutEngine;
import dev.rgcb.scholar.typography.ScholarTypography;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class MinecraftDocumentRenderer {
    private static final int PAGE_COLOR = 0xFFF6F2E8;
    private static final int BORDER_COLOR = 0xFF8A806F;
    private static final int TABLE_GRID_COLOR = 0xFF6F675A;
    private static final int TABLE_HEADER_FILL = 0x1A1F2933;
    private static final int PLOT_FRAME_COLOR = 0xFF8A806F;
    private static final int PLOT_AXIS_COLOR = 0xFF4F4A42;
    private static final int PLOT_GRID_COLOR = 0x335F5A50;
    private static final int PLOT_LEGEND_FILL = 0xE8F6F2E8;
    private static final int DIAGRAM_CANVAS_BORDER = 0x668A806F;
    private static final int DIAGRAM_NODE_FILL = 0xFFF0ECE2;
    private static final int DIAGRAM_NODE_BORDER = 0xFF5D574E;
    private static final int DIAGRAM_CONNECTION_COLOR = 0xFF4F4A42;
    private static final int DIAGRAM_PORT_COLOR = 0xFF1F5E9C;
    private static final int ELECTRICAL_SYMBOL_COLOR = 0xFF3F3A34;
    private static final int MECHANICAL_PRIMITIVE_COLOR = 0xFF3F3A34;
    private static final int[] PLOT_SERIES_COLORS = {
            0xFF1F5E9C,
            0xFFB05A2A,
            0xFF2F7D4A,
            0xFF6E4A9E,
            0xFF9A3E3E,
            0xFF2D7C83
    };

    private final MinecraftTypographyResolver typographyResolver;
    private final MinecraftMathRenderer mathRenderer;

    public MinecraftDocumentRenderer(Font font) {
        this(new MinecraftTypographyResolver(font, ScholarTypography.defaultProfile()));
    }

    public MinecraftDocumentRenderer(MinecraftTypographyResolver typographyResolver) {
        this.typographyResolver = typographyResolver;
        this.mathRenderer = new MinecraftMathRenderer(typographyResolver);
    }

    public void render(
            GuiGraphics graphics,
            LaidOutDocument document,
            int viewportX,
            int viewportY,
            int viewportWidth,
            int viewportHeight,
            int scrollOffset
    ) {
        graphics.pose().pushPose();
        graphics.fill(viewportX - 8, viewportY - 8, viewportX + viewportWidth + 8, viewportY + viewportHeight + 8, PAGE_COLOR);
        graphics.renderOutline(viewportX - 8, viewportY - 8, viewportWidth + 16, viewportHeight + 16, BORDER_COLOR);
        graphics.enableScissor(viewportX, viewportY, viewportX + viewportWidth, viewportY + viewportHeight);
        try {
            for (var block : document.blocks()) {
                if (block.math().isPresent()) {
                    var math = block.math().orElseThrow();
                    var baselineY = viewportY + block.y() + math.root().ascent() - scrollOffset;
                    if (baselineY + math.root().descent() >= viewportY && baselineY - math.root().ascent() <= viewportY + viewportHeight) {
                        mathRenderer.render(graphics, math, viewportX + block.x() + Math.max(0, (block.width() - math.width()) / 2), baselineY);
                    }
                } else if (block.figure().isPresent()) {
                    renderFigure(graphics, block.figure().orElseThrow(), viewportX, viewportY, viewportHeight, scrollOffset);
                } else if (block.table().isPresent()) {
                    renderTable(graphics, block.table().orElseThrow(), viewportX, viewportY, viewportHeight, scrollOffset);
                } else if (block.plot().isPresent()) {
                    renderPlot(graphics, block.plot().orElseThrow(), viewportX, viewportY, viewportHeight, scrollOffset);
                } else if (block.diagram().isPresent()) {
                    renderDiagram(graphics, block.diagram().orElseThrow(), viewportX, viewportY, viewportHeight, scrollOffset);
                } else {
                    for (var line : block.lines()) {
                        for (var run : line.textRuns()) {
                            var drawX = viewportX + run.x();
                            var drawY = viewportY + run.y() - scrollOffset;
                            if (drawY + line.height() >= viewportY && drawY <= viewportY + viewportHeight) {
                                var resolved = typographyResolver.resolve(run.style());
                                graphics.drawString(
                                        typographyResolver.fontFor(resolved.role()),
                                        typographyResolver.component(run.text(), resolved),
                                        drawX,
                                        drawY,
                                        resolved.color(),
                                        false);
                            }
                        }
                    }
                }
            }
        } finally {
            graphics.disableScissor();
            graphics.pose().popPose();
        }
    }

    private void renderFigure(
            GuiGraphics graphics,
            LaidOutFigure figure,
            int viewportX,
            int viewportY,
            int viewportHeight,
            int scrollOffset
    ) {
        var top = viewportY + figure.y() - scrollOffset;
        var bottom = top + figure.height();
        if (bottom < viewportY || top > viewportY + viewportHeight) {
            return;
        }
        var content = figure.content();
        if (content.plot().isPresent()) {
            renderPlot(graphics, content.plot().orElseThrow(), viewportX, viewportY, viewportHeight, scrollOffset);
        } else if (content.diagram().isPresent()) {
            renderDiagram(graphics, content.diagram().orElseThrow(), viewportX, viewportY, viewportHeight, scrollOffset);
        }
        for (var line : figure.captionLines()) {
            for (var run : line.textRuns()) {
                var drawX = viewportX + run.x();
                var drawY = viewportY + run.y() - scrollOffset;
                if (drawY + line.height() >= viewportY && drawY <= viewportY + viewportHeight) {
                    var resolved = typographyResolver.resolve(run.style());
                    graphics.drawString(
                            typographyResolver.fontFor(resolved.role()),
                            typographyResolver.component(run.text(), resolved),
                            drawX,
                            drawY,
                            resolved.color(),
                            false);
                }
            }
        }
    }

    private void renderTable(
            GuiGraphics graphics,
            LaidOutTable table,
            int viewportX,
            int viewportY,
            int viewportHeight,
            int scrollOffset
    ) {
        var tableTop = viewportY + table.y() - scrollOffset;
        var tableBottom = tableTop + table.height();
        if (tableBottom < viewportY || tableTop > viewportY + viewportHeight) {
            return;
        }

        for (var row : table.rows()) {
            for (var cell : row.cells()) {
                var cellX = viewportX + cell.x();
                var cellY = viewportY + cell.y() - scrollOffset;
                if (cellY + cell.height() < viewportY || cellY > viewportY + viewportHeight) {
                    continue;
                }
                if (row.rowIndex() < table.headerRowCount()) {
                    graphics.fill(cellX + 1, cellY + 1, cellX + cell.width(), cellY + cell.height(), TABLE_HEADER_FILL);
                }
                for (var line : cell.lines()) {
                    for (var run : line.textRuns()) {
                        var drawX = viewportX + run.x();
                        var drawY = viewportY + run.y() - scrollOffset;
                        if (drawY + line.height() >= viewportY && drawY <= viewportY + viewportHeight) {
                            var resolved = typographyResolver.resolve(run.style());
                            graphics.drawString(
                                    typographyResolver.fontFor(resolved.role()),
                                    typographyResolver.component(run.text(), resolved),
                                    drawX,
                                    drawY,
                                    resolved.color(),
                                    false);
                        }
                    }
                }
            }
        }

        for (var row : table.rows()) {
            for (var cell : row.cells()) {
                graphics.renderOutline(
                        viewportX + cell.x(),
                        viewportY + cell.y() - scrollOffset,
                        cell.width(),
                        cell.height(),
                        TABLE_GRID_COLOR);
            }
        }
    }

    private void renderDiagram(
            GuiGraphics graphics,
            LaidOutDiagram diagram,
            int viewportX,
            int viewportY,
            int viewportHeight,
            int scrollOffset
    ) {
        var top = viewportY + diagram.y() - scrollOffset;
        var bottom = top + diagram.height();
        if (bottom < viewportY || top > viewportY + viewportHeight) {
            return;
        }

        var workspaceX = viewportX + diagram.workspaceX();
        var workspaceY = viewportY + diagram.workspaceY() - scrollOffset;
        var workspaceRight = workspaceX + diagram.workspaceWidth();
        var workspaceBottom = workspaceY + diagram.workspaceHeight();
        graphics.renderOutline(
                workspaceX, workspaceY, diagram.workspaceWidth(), diagram.workspaceHeight(), DIAGRAM_CANVAS_BORDER);

        graphics.enableScissor(workspaceX, workspaceY, workspaceRight, workspaceBottom);
        try {
            var canvasX = viewportX + diagram.canvasX();
            var canvasY = viewportY + diagram.canvasY() - scrollOffset;
            if (canvasX != workspaceX || canvasY != workspaceY
                    || diagram.canvasWidth() != diagram.workspaceWidth()
                    || diagram.canvasHeight() != diagram.workspaceHeight()) {
                graphics.renderOutline(canvasX, canvasY, diagram.canvasWidth(), diagram.canvasHeight(), DIAGRAM_CANVAS_BORDER);
            }

            // Connections are drawn first so node surfaces remain visually primary.
            for (var connection : diagram.connections()) {
                var path = connection.path();
                for (var index = 1; index < path.size(); index++) {
                    var from = path.get(index - 1);
                    var to = path.get(index);
                    drawDiagramSegment(
                            graphics,
                            viewportX + from.x(),
                            viewportY + from.y() - scrollOffset,
                            viewportX + to.x(),
                            viewportY + to.y() - scrollOffset);
                }
                connection.label().ifPresent(label -> renderDiagramLabel(
                        graphics, label, viewportX, viewportY, viewportHeight, scrollOffset));
            }

            for (var component : diagram.electricalComponents()) {
                for (var primitive : component.primitives()) {
                    renderElectricalPrimitive(graphics, primitive, viewportX, viewportY, scrollOffset);
                }
                component.referenceDesignator().ifPresent(label -> renderDiagramLabel(
                        graphics, label, viewportX, viewportY, viewportHeight, scrollOffset));
                component.valueLabel().ifPresent(label -> renderDiagramLabel(
                        graphics, label, viewportX, viewportY, viewportHeight, scrollOffset));
            }

            // M19A mechanical primitives are independent authored drawing objects.
            for (var primitive : diagram.mechanicalPrimitives()) {
                renderMechanicalPrimitive(graphics, primitive, viewportX, viewportY, scrollOffset);
            }
            for (var dimension : diagram.mechanicalDimensions()) {
                renderMechanicalDimension(graphics, dimension, viewportX, viewportY, viewportHeight, scrollOffset);
            }
            for (var constraint : diagram.mechanicalConstraints()) { renderMechanicalConstraint(graphics, constraint, viewportX, viewportY, scrollOffset); }
            for (var symbol : diagram.mechanicalSymbols()) { renderMechanicalSymbol(graphics, symbol, viewportX, viewportY, scrollOffset); }
            for (var annotation : diagram.mechanicalAnnotations()) { renderMechanicalAnnotation(graphics, annotation, viewportX, viewportY, scrollOffset); }
            for (var reference : diagram.mechanicalPartReferences()) { renderMechanicalPartReference(graphics, reference, viewportX, viewportY, scrollOffset); }

            // Explicit electrical junctions are drawn after wires so the filled dot
            // unambiguously communicates an electrical splice rather than a mere crossing.
            for (var junction : diagram.electricalJunctions()) {
                var centerX = viewportX + junction.centerX();
                var centerY = viewportY + junction.centerY() - scrollOffset;
                graphics.fill(centerX - 2, centerY - 2, centerX + 3, centerY + 3, DIAGRAM_PORT_COLOR);
                junction.netLabel().ifPresent(label -> renderDiagramLabel(
                        graphics, label, viewportX, viewportY, viewportHeight, scrollOffset));
            }

            for (var node : diagram.nodes()) {
                var nodeX = viewportX + node.x();
                var nodeY = viewportY + node.y() - scrollOffset;
                graphics.fill(nodeX + 1, nodeY + 1, nodeX + node.width(), nodeY + node.height(), DIAGRAM_NODE_FILL);
                graphics.renderOutline(nodeX, nodeY, node.width(), node.height(), DIAGRAM_NODE_BORDER);
                node.label().ifPresent(label -> renderDiagramLabel(
                        graphics, label, viewportX, viewportY, viewportHeight, scrollOffset));

                for (var port : node.ports()) {
                    var px = viewportX + port.centerX();
                    var py = viewportY + port.centerY() - scrollOffset;
                    graphics.fill(px - 1, py - 1, px + 2, py + 2, DIAGRAM_PORT_COLOR);
                    port.label().ifPresent(label -> renderDiagramLabel(
                            graphics, label, viewportX, viewportY, viewportHeight, scrollOffset));
                }
            }
        } finally {
            graphics.disableScissor();
        }

        // Title is outside the internal workspace clip and remains visible/selectable.
        diagram.title().ifPresent(label -> renderDiagramLabel(
                graphics, label, viewportX, viewportY, viewportHeight, scrollOffset));
    }

    private void renderMechanicalDimension(
            GuiGraphics graphics,
            LaidOutMechanicalDimension dimension,
            int viewportX,
            int viewportY,
            int viewportHeight,
            int scrollOffset
    ) {
        var rect = dimension.bounds();
        var left = viewportX + rect.x();
        var top = viewportY + rect.y() - scrollOffset;
        var right = left + rect.width();
        var bottom = top + rect.height();
        var centerX = left + rect.width() / 2;
        var centerY = top + rect.height() / 2;
        var head = Math.max(3, Math.min(6, Math.min(rect.width(), rect.height()) / 3));

        switch (dimension.kind()) {
            case HORIZONTAL -> {
                var y = top + Math.max(3, rect.height() / 3);
                drawMechanicalLine(graphics, left, bottom, left, y);
                drawMechanicalLine(graphics, right, bottom, right, y);
                drawMechanicalLine(graphics, left, y, right, y);
                drawDimensionArrowHeads(graphics, left, y, right, y, head);
            }
            case VERTICAL -> {
                var x = left + Math.max(3, rect.width() / 3);
                drawMechanicalLine(graphics, right, top, x, top);
                drawMechanicalLine(graphics, right, bottom, x, bottom);
                drawMechanicalLine(graphics, x, top, x, bottom);
                drawDimensionArrowHeads(graphics, x, top, x, bottom, head);
            }
            case ALIGNED -> {
                drawMechanicalLine(graphics, left, bottom, right, top);
                drawDimensionArrowHeads(graphics, left, bottom, right, top, head);
            }
            case RADIUS -> {
                var radius = Math.max(1, Math.min(rect.width(), rect.height()) / 2);
                drawMechanicalCircle(graphics, centerX, centerY, radius);
                drawMechanicalLine(graphics, centerX, centerY, centerX + radius, centerY);
                drawSingleArrowHead(graphics, centerX + radius, centerY, centerX, centerY, head);
            }
            case DIAMETER -> {
                var radius = Math.max(1, Math.min(rect.width(), rect.height()) / 2);
                drawMechanicalCircle(graphics, centerX, centerY, radius);
                drawMechanicalLine(graphics, centerX - radius, centerY, centerX + radius, centerY);
                drawDimensionArrowHeads(graphics, centerX - radius, centerY, centerX + radius, centerY, head);
            }
            case ANGLE -> {
                drawMechanicalLine(graphics, left, bottom, right, bottom);
                drawMechanicalLine(graphics, left, bottom, right, top);
                var radius = Math.max(4, Math.min(rect.width(), rect.height()) / 3);
                var angle = Math.atan2(rect.height(), Math.max(1, rect.width()));
                var prevX = left + radius;
                var prevY = bottom;
                var steps = Math.max(6, radius * 2);
                for (var i = 1; i <= steps; i++) {
                    var a = angle * i / steps;
                    var x = left + (int) Math.round(Math.cos(a) * radius);
                    var y = bottom - (int) Math.round(Math.sin(a) * radius);
                    drawMechanicalLine(graphics, prevX, prevY, x, y);
                    prevX = x; prevY = y;
                }
            }
        }
        renderDiagramLabel(graphics, dimension.label(), viewportX, viewportY, viewportHeight, scrollOffset);
    }

    private static void drawDimensionArrowHeads(
            GuiGraphics graphics, int x1, int y1, int x2, int y2, int size
    ) {
        drawSingleArrowHead(graphics, x1, y1, x2, y2, size);
        drawSingleArrowHead(graphics, x2, y2, x1, y1, size);
    }

    private static void drawSingleArrowHead(
            GuiGraphics graphics, int tipX, int tipY, int towardX, int towardY, int size
    ) {
        var angle = Math.atan2(towardY - tipY, towardX - tipX);
        var spread = Math.PI / 7.0;
        var xA = tipX + (int) Math.round(Math.cos(angle + spread) * size);
        var yA = tipY + (int) Math.round(Math.sin(angle + spread) * size);
        var xB = tipX + (int) Math.round(Math.cos(angle - spread) * size);
        var yB = tipY + (int) Math.round(Math.sin(angle - spread) * size);
        drawMechanicalLine(graphics, tipX, tipY, xA, yA);
        drawMechanicalLine(graphics, tipX, tipY, xB, yB);
    }

    private static void renderMechanicalPrimitive(
            GuiGraphics graphics,
            LaidOutMechanicalPrimitive primitive,
            int viewportX,
            int viewportY,
            int scrollOffset
    ) {
        var rect = primitive.bounds();
        var left = viewportX + rect.x();
        var top = viewportY + rect.y() - scrollOffset;
        var right = left + rect.width();
        var bottom = top + rect.height();
        var centerX = left + rect.width() / 2;
        var centerY = top + rect.height() / 2;
        var vertical = primitive.orientation() == dev.rgcb.scholar.mechanical.MechanicalOrientation.DEG_90;

        switch (primitive.kind()) {
            case LINE -> {
                if (vertical) drawMechanicalLine(graphics, centerX, top, centerX, bottom);
                else drawMechanicalLine(graphics, left, centerY, right, centerY);
            }
            case CENTERLINE -> {
                if (vertical) {
                    var y = top;
                    while (y <= bottom) {
                        var end = Math.min(bottom, y + 5);
                        drawMechanicalLine(graphics, centerX, y, centerX, end);
                        y += 9;
                    }
                } else {
                    var x = left;
                    while (x <= right) {
                        var end = Math.min(right, x + 5);
                        drawMechanicalLine(graphics, x, centerY, end, centerY);
                        x += 9;
                    }
                }
            }
            case RECTANGLE -> graphics.renderOutline(
                    left, top, Math.max(1, rect.width()), Math.max(1, rect.height()), MECHANICAL_PRIMITIVE_COLOR);
            case CIRCLE -> drawMechanicalCircle(
                    graphics, centerX, centerY, Math.max(1, Math.min(rect.width(), rect.height()) / 2));
            case ARC -> drawMechanicalArc(
                    graphics, centerX, centerY, Math.max(1, Math.min(rect.width(), rect.height()) / 2));
            case ARROW -> {
                if (vertical) {
                    drawMechanicalLine(graphics, centerX, bottom, centerX, top);
                    var head = Math.max(3, Math.min(7, rect.width() / 2));
                    drawMechanicalLine(graphics, centerX, top, centerX - head, top + head);
                    drawMechanicalLine(graphics, centerX, top, centerX + head, top + head);
                } else {
                    drawMechanicalLine(graphics, left, centerY, right, centerY);
                    var head = Math.max(3, Math.min(7, rect.height() / 2));
                    drawMechanicalLine(graphics, right, centerY, right - head, centerY - head);
                    drawMechanicalLine(graphics, right, centerY, right - head, centerY + head);
                }
            }
            case REFERENCE_POINT -> {
                var arm = Math.max(2, Math.min(rect.width(), rect.height()) / 2);
                drawMechanicalLine(graphics, centerX - arm, centerY, centerX + arm, centerY);
                drawMechanicalLine(graphics, centerX, centerY - arm, centerX, centerY + arm);
                graphics.fill(centerX - 1, centerY - 1, centerX + 2, centerY + 2, MECHANICAL_PRIMITIVE_COLOR);
            }
        }
    }

    private static void renderMechanicalSymbol(GuiGraphics graphics, LaidOutMechanicalSymbol symbol, int viewportX, int viewportY, int scrollOffset) {
        var r=symbol.bounds(); int l=viewportX+r.x(), t=viewportY+r.y()-scrollOffset, rr=l+r.width(), b=t+r.height(), cx=(l+rr)/2, cy=(t+b)/2;
        switch(symbol.kind()) {
            case SHAFT -> { drawMechanicalLine(graphics,l,cy-2,rr,cy-2); drawMechanicalLine(graphics,l,cy+2,rr,cy+2); drawMechanicalLine(graphics,l,cy-4,l,cy+4); drawMechanicalLine(graphics,rr,cy-4,rr,cy+4); }
            case BEARING -> { int rad=Math.max(3,Math.min(r.width(),r.height())/2); drawMechanicalCircle(graphics,cx,cy,rad); drawMechanicalCircle(graphics,cx,cy,Math.max(2,rad/2)); drawMechanicalLine(graphics,l,cy,rr,cy); }
            case GEAR -> { int rad=Math.max(4,Math.min(r.width(),r.height())/2-3); drawMechanicalCircle(graphics,cx,cy,rad); drawMechanicalCircle(graphics,cx,cy,Math.max(2,rad/3)); for(int i=0;i<8;i++){ double a=Math.PI*2*i/8; int x1=cx+(int)Math.round(Math.cos(a)*rad), y1=cy+(int)Math.round(Math.sin(a)*rad), x2=cx+(int)Math.round(Math.cos(a)*(rad+3)), y2=cy+(int)Math.round(Math.sin(a)*(rad+3)); drawMechanicalLine(graphics,x1,y1,x2,y2); } }
            case SPRING -> { int span=Math.max(1,rr-l), amp=Math.max(3,r.height()/3), segments=8; int px=l,py=cy; for(int i=1;i<=segments;i++){ int x=l+span*i/segments; int y=(i==segments)?cy:cy+((i%2==0)?-amp:amp); drawMechanicalLine(graphics,px,py,x,y); px=x;py=y; } }
            case PISTON -> { int head=Math.max(5,r.width()/4); graphics.renderOutline(l, t+3, head, Math.max(5,r.height()-6), MECHANICAL_PRIMITIVE_COLOR); drawMechanicalLine(graphics,l+head,cy,rr-4,cy); drawMechanicalLine(graphics,rr-4,t+2,rr-4,b-2); }
            case BOLT -> { int head=Math.max(5,r.height()-4); graphics.renderOutline(l,t+2,head,Math.max(4,r.height()-4),MECHANICAL_PRIMITIVE_COLOR); drawMechanicalLine(graphics,l+head,cy,rr,cy); for(int x=l+head+3;x<rr;x+=4) drawMechanicalLine(graphics,x,cy-2,Math.min(rr,x+3),cy+2); }
        }
    }

    private void renderMechanicalPartReference(GuiGraphics graphics, LaidOutMechanicalPartReference reference, int viewportX, int viewportY, int scrollOffset) {
        var b=reference.balloonBounds();int l=viewportX+b.x(),t=viewportY+b.y()-scrollOffset,r=l+b.width(),bot=t+b.height();int cx=(l+r)/2,cy=(t+bot)/2;int tx=viewportX+reference.targetX(),ty=viewportY+reference.targetY()-scrollOffset;
        drawMechanicalLine(graphics,cx,cy,tx,ty);var radius=Math.max(3,Math.min(b.width(),b.height())/2);drawMechanicalCircle(graphics,cx,cy,radius);
        renderDiagramLabel(graphics,reference.itemLabel(),viewportX,viewportY,Integer.MAX_VALUE/4,scrollOffset);
    }

    private void renderMechanicalAnnotation(
            GuiGraphics graphics,
            LaidOutMechanicalAnnotation annotation,
            int viewportX,
            int viewportY,
            int scrollOffset
    ) {
        var rect = annotation.bounds();
        var left = viewportX + rect.x();
        var top = viewportY + rect.y() - scrollOffset;
        var right = left + rect.width();
        var bottom = top + rect.height();

        if (annotation.kind() == dev.rgcb.scholar.mechanical.MechanicalAnnotationKind.PART_LABEL) {
            graphics.renderOutline(
                    left,
                    top,
                    Math.max(1, rect.width()),
                    Math.max(1, rect.height()),
                    MECHANICAL_PRIMITIVE_COLOR);
        }

        if (annotation.kind() == dev.rgcb.scholar.mechanical.MechanicalAnnotationKind.LEADER) {
            var label = annotation.label();
            var labelBottom = viewportY + label.y() + label.height() - scrollOffset;
            // Keep the leader rule visibly below the callout text instead of
            // running through glyphs at Minecraft GUI scale.
            var ruleY = Math.min(bottom - 3, labelBottom + 3);
            var elbow = left + Math.max(7, rect.width() / 5);
            var arrowX = left + 1;
            var arrowY = bottom - 2;

            drawMechanicalLine(graphics, arrowX, arrowY, elbow, ruleY);
            drawMechanicalLine(graphics, elbow, ruleY, right - 2, ruleY);
            drawMechanicalLine(graphics, arrowX, arrowY, arrowX + 5, arrowY - 1);
            drawMechanicalLine(graphics, arrowX, arrowY, arrowX + 2, arrowY - 6);
        }

        renderDiagramLabel(
                graphics,
                annotation.label(),
                viewportX,
                viewportY,
                Integer.MAX_VALUE / 4,
                scrollOffset);
    }

    private static void renderMechanicalConstraint(
            GuiGraphics graphics,
            LaidOutMechanicalConstraint constraint,
            int viewportX,
            int viewportY,
            int scrollOffset
    ) {
        var rect = constraint.bounds();
        var left = viewportX + rect.x();
        var top = viewportY + rect.y() - scrollOffset;
        var right = left + rect.width();
        var bottom = top + rect.height();
        var cx = left + rect.width() / 2;
        var cy = top + rect.height() / 2;
        var arm = Math.max(2, Math.min(rect.width(), rect.height()) / 3);

        switch (constraint.kind()) {
            case HORIZONTAL -> {
                drawMechanicalLine(graphics, cx - arm, cy, cx + arm, cy);
                drawMechanicalLine(graphics, cx - arm, cy - 2, cx - arm, cy + 2);
                drawMechanicalLine(graphics, cx + arm, cy - 2, cx + arm, cy + 2);
            }
            case VERTICAL -> {
                drawMechanicalLine(graphics, cx, cy - arm, cx, cy + arm);
                drawMechanicalLine(graphics, cx - 2, cy - arm, cx + 2, cy - arm);
                drawMechanicalLine(graphics, cx - 2, cy + arm, cx + 2, cy + arm);
            }
            case COINCIDENT -> {
                drawMechanicalLine(graphics, cx - arm, cy, cx + arm, cy);
                drawMechanicalLine(graphics, cx, cy - arm, cx, cy + arm);
                graphics.fill(cx - 1, cy - 1, cx + 2, cy + 2, MECHANICAL_PRIMITIVE_COLOR);
            }
            case PARALLEL -> {
                drawMechanicalLine(graphics, cx - arm, cy + 2, cx + 1, cy - arm);
                drawMechanicalLine(graphics, cx - 1, cy + arm, cx + arm, cy - 2);
            }
            case PERPENDICULAR -> {
                drawMechanicalLine(graphics, cx - arm, cy + arm, cx + arm, cy + arm);
                drawMechanicalLine(graphics, cx, cy + arm, cx, cy - arm);
            }
            case CONCENTRIC -> {
                drawMechanicalCircle(graphics, cx, cy, Math.max(2, arm));
                drawMechanicalCircle(graphics, cx, cy, Math.max(1, arm / 2));
            }
        }
    }

    private static void drawMechanicalLine(GuiGraphics graphics, int x1, int y1, int x2, int y2) {
        var dx = Math.abs(x2 - x1);
        var sx = x1 < x2 ? 1 : -1;
        var dy = -Math.abs(y2 - y1);
        var sy = y1 < y2 ? 1 : -1;
        var error = dx + dy;
        var x = x1;
        var y = y1;
        while (true) {
            graphics.fill(x, y, x + 1, y + 1, MECHANICAL_PRIMITIVE_COLOR);
            if (x == x2 && y == y2) break;
            var twice = error * 2;
            if (twice >= dy) { error += dy; x += sx; }
            if (twice <= dx) { error += dx; y += sy; }
        }
    }

    private static void drawMechanicalCircle(GuiGraphics graphics, int cx, int cy, int radius) {
        var steps = Math.max(20, radius * 6);
        var prevX = cx + radius;
        var prevY = cy;
        for (var i = 1; i <= steps; i++) {
            var angle = Math.PI * 2.0 * i / steps;
            var x = cx + (int) Math.round(Math.cos(angle) * radius);
            var y = cy + (int) Math.round(Math.sin(angle) * radius);
            drawMechanicalLine(graphics, prevX, prevY, x, y);
            prevX = x; prevY = y;
        }
    }

    private static void drawMechanicalArc(GuiGraphics graphics, int cx, int cy, int radius) {
        var steps = Math.max(10, radius * 3);
        var prevX = cx - radius;
        var prevY = cy;
        for (var i = 1; i <= steps; i++) {
            var angle = Math.PI - Math.PI * i / steps;
            var x = cx + (int) Math.round(Math.cos(angle) * radius);
            var y = cy - (int) Math.round(Math.sin(angle) * radius);
            drawMechanicalLine(graphics, prevX, prevY, x, y);
            prevX = x; prevY = y;
        }
    }

    private static void renderElectricalPrimitive(
            GuiGraphics graphics,
            LaidOutElectricalPrimitive primitive,
            int viewportX,
            int viewportY,
            int scrollOffset
    ) {
        if (primitive instanceof LaidOutElectricalLine line) {
            drawElectricalLine(
                    graphics,
                    viewportX + line.start().x(),
                    viewportY + line.start().y() - scrollOffset,
                    viewportX + line.end().x(),
                    viewportY + line.end().y() - scrollOffset);
            return;
        }
        if (primitive instanceof LaidOutElectricalPolyline polyline) {
            var points = polyline.points();
            for (var index = 1; index < points.size(); index++) {
                var from = points.get(index - 1);
                var to = points.get(index);
                drawElectricalLine(
                        graphics,
                        viewportX + from.x(),
                        viewportY + from.y() - scrollOffset,
                        viewportX + to.x(),
                        viewportY + to.y() - scrollOffset);
            }
            return;
        }
        if (primitive instanceof LaidOutElectricalCircle circle) {
            drawElectricalCircle(
                    graphics,
                    viewportX + circle.center().x(),
                    viewportY + circle.center().y() - scrollOffset,
                    circle.radius());
            return;
        }
        throw new IllegalArgumentException("Unsupported laid-out electrical primitive: " + primitive.getClass().getName());
    }

    /** Integer Bresenham line for schematic diagonals; semantic geometry is already resolved by core layout. */
    private static void drawElectricalLine(GuiGraphics graphics, int x1, int y1, int x2, int y2) {
        var dx = Math.abs(x2 - x1);
        var sx = x1 < x2 ? 1 : -1;
        var dy = -Math.abs(y2 - y1);
        var sy = y1 < y2 ? 1 : -1;
        var error = dx + dy;
        var x = x1;
        var y = y1;
        while (true) {
            graphics.fill(x, y, x + 1, y + 1, ELECTRICAL_SYMBOL_COLOR);
            if (x == x2 && y == y2) {
                break;
            }
            var twiceError = error * 2;
            if (twiceError >= dy) {
                error += dy;
                x += sx;
            }
            if (twiceError <= dx) {
                error += dx;
                y += sy;
            }
        }
    }

    /** Midpoint circle renderer for the derived DC-source circle primitive. */
    private static void drawElectricalCircle(GuiGraphics graphics, int centerX, int centerY, int radius) {
        var x = radius;
        var y = 0;
        var decision = 1 - radius;
        while (x >= y) {
            plotElectricalCircleOctants(graphics, centerX, centerY, x, y);
            y++;
            if (decision <= 0) {
                decision += 2 * y + 1;
            } else {
                x--;
                decision += 2 * (y - x) + 1;
            }
        }
    }

    private static void plotElectricalCircleOctants(
            GuiGraphics graphics,
            int centerX,
            int centerY,
            int x,
            int y
    ) {
        plotElectricalPixel(graphics, centerX + x, centerY + y);
        plotElectricalPixel(graphics, centerX + y, centerY + x);
        plotElectricalPixel(graphics, centerX - y, centerY + x);
        plotElectricalPixel(graphics, centerX - x, centerY + y);
        plotElectricalPixel(graphics, centerX - x, centerY - y);
        plotElectricalPixel(graphics, centerX - y, centerY - x);
        plotElectricalPixel(graphics, centerX + y, centerY - x);
        plotElectricalPixel(graphics, centerX + x, centerY - y);
    }

    private static void plotElectricalPixel(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 1, y + 1, ELECTRICAL_SYMBOL_COLOR);
    }

    private static void drawDiagramSegment(GuiGraphics graphics, int x1, int y1, int x2, int y2) {
        if (x1 == x2) {
            graphics.fill(x1, Math.min(y1, y2), x1 + 1, Math.max(y1, y2) + 1, DIAGRAM_CONNECTION_COLOR);
        } else if (y1 == y2) {
            graphics.fill(Math.min(x1, x2), y1, Math.max(x1, x2) + 1, y1 + 1, DIAGRAM_CONNECTION_COLOR);
        } else {
            // M17C paths are expected to be orthogonal. Keep a deterministic
            // fallback rather than moving routing responsibility into Minecraft.
            graphics.fill(Math.min(x1, x2), y1, Math.max(x1, x2) + 1, y1 + 1, DIAGRAM_CONNECTION_COLOR);
            graphics.fill(x2, Math.min(y1, y2), x2 + 1, Math.max(y1, y2) + 1, DIAGRAM_CONNECTION_COLOR);
        }
    }

    private void renderDiagramLabel(
            GuiGraphics graphics,
            LaidOutDiagramLabel label,
            int viewportX,
            int viewportY,
            int viewportHeight,
            int scrollOffset
    ) {
        var drawY = viewportY + label.y() - scrollOffset;
        if (drawY + label.height() < viewportY || drawY > viewportY + viewportHeight) {
            return;
        }
        var resolved = typographyResolver.resolve(label.style());
        graphics.drawString(
                typographyResolver.fontFor(resolved.role()),
                typographyResolver.component(label.text(), resolved),
                viewportX + label.x(),
                drawY,
                resolved.color(),
                false);
    }

    private void renderPlot(
            GuiGraphics graphics,
            LaidOutPlot plot,
            int viewportX,
            int viewportY,
            int viewportHeight,
            int scrollOffset
    ) {
        var plotTop = viewportY + plot.y() - scrollOffset;
        var plotBottom = plotTop + plot.height();
        if (plotBottom < viewportY || plotTop > viewportY + viewportHeight) {
            return;
        }

        var areaX = viewportX + plot.plotAreaX();
        var areaY = viewportY + plot.plotAreaY() - scrollOffset;
        var areaRight = areaX + plot.plotAreaWidth();
        var areaBottom = areaY + plot.plotAreaHeight();

        if (plot.gridVisible()) {
            for (var tick : plot.xTicks()) {
                var x = viewportX + tick.coordinate();
                if (x > areaX && x < areaRight) {
                    graphics.fill(x, areaY, x + 1, areaBottom, PLOT_GRID_COLOR);
                }
            }
            for (var tick : plot.yTicks()) {
                var y = viewportY + tick.coordinate() - scrollOffset;
                if (y > areaY && y < areaBottom) {
                    graphics.fill(areaX, y, areaRight, y + 1, PLOT_GRID_COLOR);
                }
            }
        }

        renderPlotSeries(graphics, plot, viewportX, viewportY, scrollOffset, areaX, areaY, areaRight, areaBottom);

        graphics.renderOutline(areaX, areaY, plot.plotAreaWidth(), plot.plotAreaHeight(), PLOT_FRAME_COLOR);
        graphics.fill(areaX, areaY, areaX + 1, areaBottom, PLOT_AXIS_COLOR);
        graphics.fill(areaX, areaBottom - 1, areaRight, areaBottom, PLOT_AXIS_COLOR);

        for (var tick : plot.xTicks()) {
            var x = viewportX + tick.coordinate();
            graphics.fill(x, areaBottom - 1, x + 1, areaBottom + PlotLayoutEngine.TICK_LENGTH, PLOT_AXIS_COLOR);
            renderPlotLabel(graphics, tick.label(), viewportX, viewportY, viewportHeight, scrollOffset);
        }
        for (var tick : plot.yTicks()) {
            var y = viewportY + tick.coordinate() - scrollOffset;
            graphics.fill(areaX - PlotLayoutEngine.TICK_LENGTH + 1, y, areaX + 1, y + 1, PLOT_AXIS_COLOR);
            renderPlotLabel(graphics, tick.label(), viewportX, viewportY, viewportHeight, scrollOffset);
        }

        plot.legend().ifPresent(legend -> {
            var legendX = viewportX + legend.x();
            var legendY = viewportY + legend.y() - scrollOffset;
            graphics.fill(legendX, legendY, legendX + legend.width(), legendY + legend.height(), PLOT_LEGEND_FILL);
            graphics.renderOutline(legendX, legendY, legend.width(), legend.height(), PLOT_FRAME_COLOR);
            for (var item : legend.items()) {
                var color = seriesColor(item.style().styleIndex());
                var sampleX1 = viewportX + item.sampleX1();
                var sampleX2 = viewportX + item.sampleX2();
                var sampleY = viewportY + item.sampleY() - scrollOffset;
                if (item.kind() == PlotSeriesKind.LINE) {
                    drawPatternedLine(
                            graphics,
                            sampleX1,
                            sampleY,
                            sampleX2,
                            sampleY,
                            color,
                            item.style().linePattern(),
                            areaX,
                            areaY,
                            areaRight,
                            areaBottom);
                } else {
                    drawMarker(
                            graphics,
                            (sampleX1 + sampleX2) / 2,
                            sampleY,
                            color,
                            item.style().markerShape(),
                            areaX,
                            areaY,
                            areaRight,
                            areaBottom);
                }
                renderPlotLabel(graphics, item.label(), viewportX, viewportY, viewportHeight, scrollOffset);
            }
        });

        plot.title().ifPresent(label -> renderPlotLabel(graphics, label, viewportX, viewportY, viewportHeight, scrollOffset));
        plot.xAxisLabel().ifPresent(label -> renderPlotLabel(graphics, label, viewportX, viewportY, viewportHeight, scrollOffset));
        plot.yAxisLabel().ifPresent(label -> renderPlotLabel(graphics, label, viewportX, viewportY, viewportHeight, scrollOffset));
    }

    private void renderPlotSeries(
            GuiGraphics graphics,
            LaidOutPlot plot,
            int viewportX,
            int viewportY,
            int scrollOffset,
            int areaX,
            int areaY,
            int areaRight,
            int areaBottom
    ) {
        for (var series : plot.series()) {
            var color = seriesColor(series.style().styleIndex());
            if (series.kind() == PlotSeriesKind.LINE) {
                for (var segment : series.lineSegments()) {
                    drawPatternedLine(
                            graphics,
                            viewportX + segment.x1(),
                            viewportY + segment.y1() - scrollOffset,
                            viewportX + segment.x2(),
                            viewportY + segment.y2() - scrollOffset,
                            color,
                            series.style().linePattern(),
                            areaX,
                            areaY,
                            areaRight,
                            areaBottom);
                }
            } else if (series.kind() == PlotSeriesKind.SCATTER) {
                for (var point : series.points()) {
                    drawMarker(
                            graphics,
                            viewportX + point.x(),
                            viewportY + point.y() - scrollOffset,
                            color,
                            series.style().markerShape(),
                            areaX,
                            areaY,
                            areaRight,
                            areaBottom);
                }
            }
        }
    }

    private static int seriesColor(int styleIndex) {
        return PLOT_SERIES_COLORS[Math.floorMod(styleIndex, PLOT_SERIES_COLORS.length)];
    }

    private static void drawPatternedLine(
            GuiGraphics graphics,
            int x1,
            int y1,
            int x2,
            int y2,
            int color,
            PlotLinePattern pattern,
            int clipLeft,
            int clipTop,
            int clipRight,
            int clipBottom
    ) {
        var dx = Math.abs(x2 - x1);
        var sx = x1 < x2 ? 1 : -1;
        var dy = -Math.abs(y2 - y1);
        var sy = y1 < y2 ? 1 : -1;
        var error = dx + dy;
        var x = x1;
        var y = y1;
        var step = 0;
        while (true) {
            if (patternDraws(pattern, step)) {
                fillPlotPixel(graphics, x, y, color, clipLeft, clipTop, clipRight, clipBottom);
            }
            if (x == x2 && y == y2) {
                break;
            }
            var twice = 2 * error;
            if (twice >= dy) {
                error += dy;
                x += sx;
            }
            if (twice <= dx) {
                error += dx;
                y += sy;
            }
            step++;
        }
    }

    private static boolean patternDraws(PlotLinePattern pattern, int step) {
        return switch (pattern) {
            case SOLID -> true;
            case DASHED -> Math.floorMod(step, 8) < 5;
            case DOTTED -> Math.floorMod(step, 4) == 0;
            case DASH_DOT -> {
                var phase = Math.floorMod(step, 12);
                yield phase < 6 || phase == 9;
            }
            case LONG_DASH -> Math.floorMod(step, 12) < 8;
            case DENSE_DOT -> Math.floorMod(step, 3) == 0;
        };
    }

    private static void drawMarker(
            GuiGraphics graphics,
            int centerX,
            int centerY,
            int color,
            PlotMarkerShape shape,
            int clipLeft,
            int clipTop,
            int clipRight,
            int clipBottom
    ) {
        switch (shape) {
            case SQUARE -> {
                for (var y = -2; y <= 2; y++) {
                    for (var x = -2; x <= 2; x++) {
                        fillPlotPixel(graphics, centerX + x, centerY + y, color, clipLeft, clipTop, clipRight, clipBottom);
                    }
                }
            }
            case DIAMOND -> {
                for (var y = -2; y <= 2; y++) {
                    var halfWidth = 2 - Math.abs(y);
                    for (var x = -halfWidth; x <= halfWidth; x++) {
                        fillPlotPixel(graphics, centerX + x, centerY + y, color, clipLeft, clipTop, clipRight, clipBottom);
                    }
                }
            }
            case CROSS -> {
                for (var d = -2; d <= 2; d++) {
                    fillPlotPixel(graphics, centerX + d, centerY, color, clipLeft, clipTop, clipRight, clipBottom);
                    fillPlotPixel(graphics, centerX, centerY + d, color, clipLeft, clipTop, clipRight, clipBottom);
                }
            }
            case X -> {
                for (var d = -2; d <= 2; d++) {
                    fillPlotPixel(graphics, centerX + d, centerY + d, color, clipLeft, clipTop, clipRight, clipBottom);
                    fillPlotPixel(graphics, centerX + d, centerY - d, color, clipLeft, clipTop, clipRight, clipBottom);
                }
            }
            case CIRCLE -> {
                for (var y = -2; y <= 2; y++) {
                    var halfWidth = Math.abs(y) == 2 ? 1 : 2;
                    for (var x = -halfWidth; x <= halfWidth; x++) {
                        fillPlotPixel(graphics, centerX + x, centerY + y, color, clipLeft, clipTop, clipRight, clipBottom);
                    }
                }
            }
            case TRIANGLE -> {
                for (var y = -2; y <= 2; y++) {
                    var halfWidth = Math.min(2, y + 2);
                    for (var x = -halfWidth; x <= halfWidth; x++) {
                        fillPlotPixel(graphics, centerX + x, centerY + y, color, clipLeft, clipTop, clipRight, clipBottom);
                    }
                }
            }
        }
    }

    private static void fillPlotPixel(
            GuiGraphics graphics,
            int x,
            int y,
            int color,
            int clipLeft,
            int clipTop,
            int clipRight,
            int clipBottom
    ) {
        if (x < clipLeft || x >= clipRight || y < clipTop || y >= clipBottom) {
            return;
        }
        graphics.fill(x, y, x + 1, y + 1, color);
    }

    private void renderPlotLabel(
            GuiGraphics graphics,
            LaidOutPlotLabel label,
            int viewportX,
            int viewportY,
            int viewportHeight,
            int scrollOffset
    ) {
        var drawY = viewportY + label.y() - scrollOffset;
        if (drawY + label.height() < viewportY || drawY > viewportY + viewportHeight) {
            return;
        }
        var resolved = typographyResolver.resolve(label.style());
        graphics.drawString(
                typographyResolver.fontFor(resolved.role()),
                typographyResolver.component(label.text(), resolved),
                viewportX + label.x(),
                drawY,
                resolved.color(),
                false);
    }

}
