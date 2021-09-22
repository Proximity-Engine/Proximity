package dev.hephaestus.proximity.templates.layers;

import dev.hephaestus.proximity.util.StatefulGraphics;

import java.awt.*;

public class SpacerLayer extends Layer {
    private final int width, height;

    public SpacerLayer(String parentId, String id, int x, int y, int width, int height) {
        super(parentId, id, x, y);
        this.width = width;
        this.height = height;
    }

    @Override
    public Rectangle draw(StatefulGraphics out, Rectangle wrap) {
        return new Rectangle(this.getX(), this.getY(), this.width, this.height);
    }
}
