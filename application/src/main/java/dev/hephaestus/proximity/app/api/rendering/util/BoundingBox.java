package dev.hephaestus.proximity.app.api.rendering.util;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

public class BoundingBox extends Rectangle2D.Double {
    private final boolean scalable;

    public BoundingBox(boolean scalable) {
        this.scalable = scalable;
    }

    public BoundingBox(Rectangle rectangle, boolean scalable) {
        this(rectangle.getX(), rectangle.getY(), rectangle.getWidth(), rectangle.getHeight(), scalable);
    }

    public BoundingBox(double x, double y, double w, double h, boolean scalable) {
        super(x, y, w, h);
        this.scalable = scalable;
    }

    public boolean isScalable() {
        return this.scalable;
    }
}
