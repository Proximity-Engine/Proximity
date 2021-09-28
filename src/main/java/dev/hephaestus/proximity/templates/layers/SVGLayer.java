package dev.hephaestus.proximity.templates.layers;

import com.kitfox.svg.SVGException;
import dev.hephaestus.proximity.util.ContentAlignment;
import dev.hephaestus.proximity.util.SVG;
import dev.hephaestus.proximity.util.StatefulGraphics;

import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;

public class SVGLayer extends Layer {
    private final String src;
    private final SVG svg;
    private final float scale;
    private final ContentAlignment verticalAlignment, horizontalAlignment;

    public SVGLayer(String id, String src, int x, int y, SVG svg, float scale, ContentAlignment verticalAlignment, ContentAlignment horizontalAlignment) {
        super(id, x, y);
        this.src = src;
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
            case MIDDLE -> x -= (int) (this.svg.drawnBounds().getWidth() * this.scale * 0.5);
            case END -> x -= (int) (this.svg.drawnBounds().getWidth() * this.scale);
            default -> x -= this.svg.drawnBounds().getX() * this.scale;
        }

        switch (this.verticalAlignment) {
            case MIDDLE -> y -= (int) (this.svg.drawnBounds().getHeight() * this.scale * 0.5);
            case END -> y -= (int) (this.svg.drawnBounds().getHeight() * this.scale);
            default -> y -= this.svg.drawnBounds().getY() * this.scale;
        }

        try {
            out.push((int) (x - this.svg.drawnBounds().getX() * this.scale), (int) (y - this.svg.drawnBounds().getY() * this.scale));
            out.push(this.scale, this.scale);
            out.push(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);


            this.svg.diagram().render(out);

            out.pop(3);
        } catch (SVGException e) {
            e.printStackTrace();
        }

        return new Rectangle(x, y, (int) (this.svg.drawnBounds().getWidth() * this.scale), (int) (this.svg.drawnBounds().getHeight() * this.scale));
    }

    @Override
    public String toString() {
        return "SRC[" + this.getId() + ";" + this.src + "]";
    }
}
