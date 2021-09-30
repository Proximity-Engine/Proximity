package dev.hephaestus.proximity.templates.layers;

import dev.hephaestus.proximity.util.ContentAlignment;
import dev.hephaestus.proximity.util.DrawingUtil;
import dev.hephaestus.proximity.util.Outline;
import dev.hephaestus.proximity.util.StatefulGraphics;
import org.apache.batik.gvt.*;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;

public class SVGLayer extends Layer {
    private final String src;
    private final Integer fillColor;
    private final Outline outline;
    private final boolean forceOutline;
    private final Rectangle2D svgBounds;
    private final GraphicsNode svg;
    private final float scale;
    private final ContentAlignment verticalAlignment, horizontalAlignment;

    public SVGLayer(String id, String src, int x, int y, Integer fillColor, Outline outline, boolean forceOutline, Rectangle2D svgBounds, GraphicsNode svg, float scale, ContentAlignment verticalAlignment, ContentAlignment horizontalAlignment) {
        super(id, x, y);
        this.src = src;
        this.fillColor = fillColor;
        this.outline = outline;
        this.forceOutline = forceOutline;
        this.svgBounds = svgBounds;
        this.svg = svg;
        this.scale = scale;
        this.verticalAlignment = verticalAlignment;
        this.horizontalAlignment = horizontalAlignment;
    }

    @Override
    public Rectangle2D draw(StatefulGraphics out, Rectangle2D wrap, boolean draw, float scale) {
        int x = this.getX();
        int y = this.getY();

        switch (this.horizontalAlignment) {
            case MIDDLE -> x -= (int) (this.svgBounds.getWidth() * this.scale * 0.5);
            case END -> x -= (int) (this.svgBounds.getWidth() * this.scale);
            default -> x -= this.svgBounds.getX() * this.scale;
        }

        switch (this.verticalAlignment) {
            case MIDDLE -> y -= (int) (this.svgBounds.getHeight() * this.scale * 0.5);
            case END -> y -= (int) (this.svgBounds.getHeight() * this.scale);
            default -> y -= this.svgBounds.getY() * this.scale;
        }

        out.push((int) (x - this.svgBounds.getX() * this.scale), (int) (y - this.svgBounds.getY() * this.scale));
        out.push(this.scale, this.scale);
        out.push(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        this.adjust(this.svg.getRoot());
        this.svg.getRoot().setClip(null);
        this.svg.getRoot().paint(out);

        if (this.fillColor != null) {
            out.pop();
        }

        out.pop(3);

        return new Rectangle(x, y, (int) (this.svgBounds.getWidth() * this.scale), (int) (this.svgBounds.getHeight() * this.scale));
    }

    private void adjust(Object object) {
        if (object instanceof ShapeNode shape) {
            this.adjustPaint(shape.getShapePainter());
        } else if (object instanceof CompositeGraphicsNode composite) {
            for (Object o : composite) {
                adjust(o);
            }
        }

        if (object instanceof GraphicsNode node) {
            node.setClip(null);
        }
    }

    private void adjustPaint(ShapePainter shapePainter) {
        if (this.fillColor == null && this.outline == null) return;

        if (shapePainter instanceof CompositeShapePainter composite) {
            for (int i = 0; i < composite.getShapePainterCount(); ++i) {
                adjustPaint(composite.getShapePainter(i));
            }
        } else if (shapePainter instanceof StrokeShapePainter stroke && this.outline != null && (stroke.getPaint() != null || this.forceOutline)) {
            if (this.outline.weight() < 0.01) {
                stroke.setStroke(null);
                stroke.setPaint(null);
            } else {
                float weight = (stroke.getStroke() instanceof BasicStroke basic ? basic.getLineWidth() : 1)
                        * (this.outline.weight() / this.scale);

                stroke.setStroke(new BasicStroke(weight, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 4));
                stroke.setPaint(DrawingUtil.getColor(this.outline.color()));
            }
        } else if (shapePainter instanceof FillShapePainter fill) {
            boolean useStrokeColor = fill.getPaint() instanceof Color c && c.getRGB() == 0xFF010101;

            if (this.fillColor != null || useStrokeColor) {
                int color = useStrokeColor ? this.outline.color() : this.fillColor;
                fill.setPaint(DrawingUtil.getColor(color));
            }
        }
    }

    @Override
    public String toString() {
        return "SVG[" + this.getId() + ";" + this.src + "]";
    }
}
