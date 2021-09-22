package dev.hephaestus.proximity.templates.layers;

import dev.hephaestus.proximity.util.StatefulGraphics;

import java.awt.*;

public abstract class Layer {
    public static final Layer EMPTY = new Layer("", "", 0, 0) {
        @Override
        public Rectangle draw(StatefulGraphics out, Rectangle wrap) {
            return new Rectangle();
        }
    };

    private final String parentId;
    private final String id;
    protected int x, y;

    public Layer(String parentId, String id, int x, int y) {
        this.parentId = parentId;
        this.id = id;
        this.x = x;
        this.y = y;
    }

    public String getParentId() {
        return this.parentId;
    }

    public String getId() {
        return id(this.parentId, this.id);
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public abstract Rectangle draw(StatefulGraphics out, Rectangle wrap);

    public static String id(String parentId, String id) {
        return (parentId.isEmpty() ? "" : parentId + ".") + id;
    }

    @Override
    public String toString() {
        return "Layer[" + this.getId() + "]";
    }
}
