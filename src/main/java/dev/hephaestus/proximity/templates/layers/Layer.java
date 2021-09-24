package dev.hephaestus.proximity.templates.layers;

import dev.hephaestus.proximity.util.StatefulGraphics;

import java.awt.*;

public abstract class Layer {
    public static final Layer EMPTY = new Layer("", "", 0, 0) {
        @Override
        public Rectangle draw(StatefulGraphics out, Rectangle wrap, boolean draw, int scale) {
            return new Rectangle();
        }
    };

    private final String parentId;
    private final String id;
    private int x, y;
    protected Rectangle bounds;
    protected Rectangle wrap;

    public Layer(String parentId, String id, int x, int y) {
        this.parentId = parentId;
        this.id = id;
        this.x = x;
        this.y = y;
    }

    public void setBounds(Rectangle bounds) {
        this.bounds = bounds;
    }

    public void setWrap(Rectangle wrap) {
        this.wrap = wrap;
    }

    public String getParentId() {
        return this.parentId;
    }

    public String getId() {
        return id(this.parentId, this.id);
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

    public abstract Rectangle draw(StatefulGraphics out, Rectangle wrap, boolean draw, int scale);

    public static String id(String parentId, String id) {
        return (parentId.isEmpty() ? "" : parentId) + (id.isEmpty() ? "" : ((parentId.isEmpty() ? "" : ".") + id));
    }

    @Override
    public String toString() {
        return "Layer[" + this.getId() + "]";
    }
}
