package dev.hephaestus.proximity.templates.layers;

import dev.hephaestus.proximity.util.Rectangles;
import dev.hephaestus.proximity.util.StatefulGraphics;

import java.awt.geom.Rectangle2D;

public abstract class Layer {
    private final String id;
    private int x, y;
    protected Rectangle2D bounds;
    protected Rectangles wrap;

    public Layer(String id, int x, int y) {
        this.id = id;
        this.x = x;
        this.y = y;
    }

    public void setBounds(Rectangle2D bounds) {
        this.bounds = bounds;
    }

    public void setWrap(Rectangles wrap) {
        this.wrap = wrap;
    }

    public String getId() {
        return this.id;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public abstract Rectangles draw(StatefulGraphics out, Rectangles wrap, boolean draw, float scale);

    @Override
    public String toString() {
        return "Layer[" + this.id + "]";
    }
}
