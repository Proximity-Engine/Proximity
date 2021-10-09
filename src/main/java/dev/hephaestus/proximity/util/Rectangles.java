package dev.hephaestus.proximity.util;

import org.jetbrains.annotations.NotNull;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

public final class Rectangles implements Iterable<Rectangle2D> {
    private final List<Rectangle2D> rectangles = new ArrayList<>();

    public static Rectangles singleton(Rectangle2D rectangle) {
        Rectangles rectangles = new Rectangles();

        rectangles.add(rectangle);

        return rectangles;
    }

    public void add(Rectangle2D rectangle) {
        if (rectangle != null) {
            this.rectangles.add(rectangle);
        }
    }

    public boolean intersects(Rectangle2D rectangle) {
        for (Rectangle2D r : this.rectangles) {
            if (r.intersects(rectangle)) {
                return true;
            }
        }

        return false;
    }

    public boolean intersects(Rectangles rectangles) {
        for (Rectangle2D r : this.rectangles) {
            if (rectangles.intersects(r)) {
                return true;
            }
        }

        return false;
    }

    public void apply(Consumer<Rectangle2D> consumer) {
        this.rectangles.forEach(consumer);
    }

    public Rectangle2D getBounds() {
        Rectangle2D bounds = null;

        for (Rectangle2D rectangle : this.rectangles) {
            bounds = bounds == null ? rectangle : DrawingUtil.encompassing(rectangle, bounds);
        }

        return bounds;
    }

    public boolean isEmpty() {
        return this.rectangles.isEmpty();
    }

    public boolean fitsWithin(Rectangle2D rectangle) {
        for (Rectangle2D r : this.rectangles) {
            if (!rectangle.contains(r)) return false;
        }

        return true;
    }

    @NotNull
    @Override
    public Iterator<Rectangle2D> iterator() {
        return this.rectangles.iterator();
    }

    public double getHeight() {
        return this.getBounds().getHeight();
    }

    public void addAll(Rectangles rectangles) {
        this.rectangles.addAll(rectangles.rectangles);
    }
}
