package dev.hephaestus.proximity.templates.layers;

import dev.hephaestus.proximity.util.Box;
import dev.hephaestus.proximity.util.Rectangles;
import dev.hephaestus.proximity.util.StatefulGraphics;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

public class FillLayer extends Layer {
    private final int width, height, color;

    public FillLayer(String id, int x, int y, int width, int height, int color) {
        super(id, x, y);
        this.width = width;
        this.height = height;
        this.color = color;
    }

    @Override
    public Rectangles draw(StatefulGraphics out, Rectangles wrap, boolean draw, Box<Float> scale) {
        Rectangle2D rectangle = new Rectangle2D.Double(this.getX(), this.getY(), this.width, this.height);

        if ((this.color & 0xFF000000) != 0) {
            out.push(new Color(this.color, (this.color & 0xFF000000) >>> 24 != 255), Graphics2D::setColor, Graphics2D::getColor);
            out.fill(rectangle);
            out.pop();
        }

        return Rectangles.singleton(rectangle);
    }
}
