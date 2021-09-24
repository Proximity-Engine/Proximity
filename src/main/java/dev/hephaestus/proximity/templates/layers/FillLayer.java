package dev.hephaestus.proximity.templates.layers;

import dev.hephaestus.proximity.util.StatefulGraphics;

import java.awt.*;

public class FillLayer extends Layer {
    private final int width, height, color;

    public FillLayer(String parentId, String id, int x, int y, int width, int height, int color) {
        super(parentId, id, x, y);
        this.width = width;
        this.height = height;
        this.color = color;
    }

    @Override
    public Rectangle draw(StatefulGraphics out, Rectangle wrap, boolean draw, int scale) {
        Rectangle rectangle = new Rectangle(this.getX(), this.getY(), this.width + scale * 5, this.height + scale * 5);

        if ((this.color & 0xFF000000) != 0) {
            out.push(new Color(this.color), Graphics2D::setColor, Graphics2D::getColor);
            out.draw(rectangle);
            out.pop();
        }

        return rectangle;
    }
}
