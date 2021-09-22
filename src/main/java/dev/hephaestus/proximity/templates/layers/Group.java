package dev.hephaestus.proximity.templates.layers;

import dev.hephaestus.proximity.util.DrawingUtil;
import dev.hephaestus.proximity.util.StatefulGraphics;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Group extends Layer {
    private final List<Layer> layers;

    public Group(String parentId, String id, int x, int y, List<Layer> layers) {
        super(parentId, id, x, y);
        this.layers = new ArrayList<>(layers);
    }

    @Override
    public Rectangle draw(StatefulGraphics out, Rectangle wrap) {
        Rectangle bounds = null;

        for (Layer layer : this.layers) {
            Rectangle rectangle = layer.draw(out, wrap);
            bounds = bounds != null ?
                    DrawingUtil.encompassing(bounds, rectangle)
                    : rectangle;
        }

        return bounds;
    }
}
