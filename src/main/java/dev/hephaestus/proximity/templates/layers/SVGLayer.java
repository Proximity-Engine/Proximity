package dev.hephaestus.proximity.templates.layers;

import dev.hephaestus.proximity.util.ContentAlignment;
import dev.hephaestus.proximity.util.StatefulGraphics;
import org.apache.batik.gvt.CompositeGraphicsNode;
import org.apache.batik.gvt.GraphicsNode;

import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;

public class SVGLayer extends Layer {
    private final String src;
    private final Rectangle2D svgBounds;
    private final GraphicsNode svg;
    private final float scale;
    private final ContentAlignment verticalAlignment, horizontalAlignment;

    public SVGLayer(String id, String src, int x, int y, Rectangle2D svgBounds, GraphicsNode svg, float scale, ContentAlignment verticalAlignment, ContentAlignment horizontalAlignment) {
        super(id, x, y);
        this.src = src;
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
        }

        switch (this.verticalAlignment) {
            case MIDDLE -> y -= (int) (this.svgBounds.getHeight() * this.scale * 0.5);
            case END -> y -= (int) (this.svgBounds.getHeight() * this.scale);
        }

        out.push((int) (x - this.svgBounds.getX() * this.scale), (int) (y - this.svgBounds.getY() * this.scale));
        out.push(this.scale, this.scale);
        out.push(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        this.adjust(this.svg.getRoot());
        this.svg.getRoot().setClip(null);
        this.svg.getRoot().paint(out);

        out.pop(3);

        return new Rectangle(x, y, (int) (this.svgBounds.getWidth() * this.scale), (int) (this.svgBounds.getHeight() * this.scale));
    }

    private void adjust(Object object) {
        if (object instanceof CompositeGraphicsNode composite) {
            for (Object o : composite) {
                adjust(o);
            }
        }

        if (object instanceof GraphicsNode node) {
            node.setClip(null);
        }
    }

    @Override
    public String toString() {
        return "SVG[" + this.getId() + ";" + this.src + "]";
    }
}
