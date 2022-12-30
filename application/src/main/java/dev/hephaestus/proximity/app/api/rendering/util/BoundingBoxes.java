package dev.hephaestus.proximity.app.api.rendering.util;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public final class BoundingBoxes implements Iterable<BoundingBox> {
    private final List<BoundingBox> boxes = new ArrayList<>();
    private Rectangle2D bounds = null;

    public BoundingBoxes(BoundingBox... boxes) {
        this(Arrays.asList(boxes));
    }

    public BoundingBoxes(List<BoundingBox> boxes) {
        this.boxes.addAll(boxes);
        this.computeBounds();
    }

    public Rectangle2D getBounds() {
        return this.bounds;
    }

    public void add(BoundingBox rectangle2D) {
        this.boxes.add(rectangle2D);
        this.computeBounds();
    }

    public boolean intersects(BoundingBox rectangle) {
        for (BoundingBox r : this.boxes) {
            if (r.intersects(rectangle)) {
                return true;
            }
        }

        return false;
    }

    public boolean intersects(BoundingBoxes boxes) {
        for (BoundingBox box : this.boxes) {
            if (boxes.intersects(box)) {
                return true;
            }
        }

        return false;
    }

    public boolean isEmpty() {
        return this.boxes.isEmpty();
    }

    public double getMinX() {
        return this.bounds == null ? 0 : this.bounds.getMinX();
    }

    public double getMinY() {
        return this.bounds == null ? 0 : this.bounds.getMinY();
    }

    public double getMaxX() {
        return this.bounds == null ? 0 : this.bounds.getMaxX();
    }

    public double getMaxY() {
        return this.bounds == null ? 0 : this.bounds.getMaxY();
    }

    public double getWidth() {
        return this.bounds == null ? 0 : this.bounds.getWidth();
    }

    public double getHeight() {
        return this.bounds == null ? 0 : this.bounds.getHeight();
    }

    @Override
    public Iterator<BoundingBox> iterator() {
        return this.boxes.iterator();
    }

    private void computeBounds() {
        for (Rectangle2D box : this.boxes) {
            this.bounds = this.bounds == null ? box : encompassing(box, this.bounds);
        }
    }

    private static Rectangle2D encompassing(Rectangle2D r1, Rectangle2D r2) {
        double x = Math.min(r1.getX(), r2.getX());
        double y = Math.min(r1.getY(), r2.getY());

        double width = Math.max(
                r1.getMaxX(),
                r2.getMaxX()
        ) - x;

        double height = Math.max(
                r1.getMaxY(),
                r2.getMaxY()
        ) - y;

        return new Rectangle2D.Double(x, y, width, height);
    }
}
