package dev.hephaestus.proximity.templates.layers;

import dev.hephaestus.proximity.util.DrawingUtil;
import dev.hephaestus.proximity.util.StatefulGraphics;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Group extends Layer {
    protected final List<Layer> layers;

    public Group(String parentId, String id, int x, int y, List<Layer> layers) {
        super(parentId, id, x, y);
        this.layers = new ArrayList<>(layers);
    }

    @Override
    public void setWrap(Rectangle wrap) {
        for (Layer layer : this.layers) {
            layer.setWrap(wrap);
        }

        super.setWrap(wrap);
    }

    @Override
    public void setX(int x) {
        for (Layer layer : this.layers) {
            layer.setX(layer.getX() + x - this.getX());
        }

        super.setX(x);
    }

    @Override
    public void setY(int y) {
        for (Layer layer : this.layers) {
            layer.setY(layer.getY() + y - this.getY());
        }

        super.setY(y);
    }

    @Override
    public void setBounds(Rectangle bounds) {
        super.setBounds(bounds);

        for (Layer layer : this.layers) {
            layer.setBounds(bounds);
        }
    }

    @Override
    public Rectangle draw(StatefulGraphics out, Rectangle wrap, boolean draw, int scale) {
        Rectangle bounds = null;

        for (Layer layer : this.layers) {
            Rectangle rectangle = layer.draw(out, wrap, draw, scale);
            bounds = bounds != null ?
                    DrawingUtil.encompassing(bounds, rectangle)
                    : rectangle;
        }

        return bounds;
    }
}
