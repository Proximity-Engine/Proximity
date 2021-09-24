package dev.hephaestus.proximity.templates.layers;

import com.kitfox.svg.RenderableElement;
import com.kitfox.svg.SVGException;
import com.kitfox.svg.ShapeElement;
import dev.hephaestus.proximity.util.ContentAlignment;
import dev.hephaestus.proximity.util.DrawingUtil;
import dev.hephaestus.proximity.util.SVG;
import dev.hephaestus.proximity.util.StatefulGraphics;

import java.awt.*;

public class SVGLayer extends Layer {
    private final String src;
    private final SVG svg;
    private final float scale;
    private final ContentAlignment verticalAlignment, horizontalAlignment;

    public SVGLayer(String parentId, String id, String src, int x, int y, SVG svg, float scale, ContentAlignment verticalAlignment, ContentAlignment horizontalAlignment) {
        super(parentId, id, x, y);
        this.src = src;
        this.svg = svg;
        this.scale = scale;
        this.verticalAlignment = verticalAlignment;
        this.horizontalAlignment = horizontalAlignment;
    }

    @Override
    public Rectangle draw(StatefulGraphics out, Rectangle wrap, boolean draw, int scale) {
        int x = this.getX();
        int y = this.getY();

        switch (this.horizontalAlignment) {
            case MIDDLE -> x -= (int) (this.svg.drawnBounds().getWidth() * this.scale * 0.5);
            case END -> x -= (int) (this.svg.drawnBounds().getWidth() * this.scale);
            default -> x -= this.svg.drawnBounds().x * this.scale;
        }

        switch (this.verticalAlignment) {
            case MIDDLE -> y -= (int) (this.svg.drawnBounds().getHeight() * this.scale * 0.5);
            case END -> y -= (int) (this.svg.drawnBounds().getHeight() * this.scale);
            default -> y -= this.svg.drawnBounds().y * this.scale;
        }

        try {
            out.push((int) (x - this.svg.drawnBounds().x * this.scale), (int) (y - this.svg.drawnBounds().y * this.scale));
            out.push(this.scale, this.scale);
            out.push(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            out.push(DrawingUtil.getColor(0xFF000000), Graphics2D::setColor, Graphics2D::getColor);

            for (RenderableElement element : this.svg.elements()) {
                if (element instanceof ShapeElement shape) {
                    out.fill(shape.getShape());
                } else {
                    element.render(out);
                }
            }

            out.pop(4);
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
