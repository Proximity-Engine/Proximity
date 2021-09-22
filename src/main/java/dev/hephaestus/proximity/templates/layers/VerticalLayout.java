package dev.hephaestus.proximity.templates.layers;

import dev.hephaestus.proximity.util.DrawingUtil;
import dev.hephaestus.proximity.util.StatefulGraphics;

import java.awt.*;
import java.util.List;

public class VerticalLayout extends Layer {
    private final List<Layer> layers;

    public VerticalLayout(String parentId, String id, int x, int y, List<Layer> layers) {
        super(parentId, id, x, y);
        this.layers = layers;
    }

    @Override
    public Rectangle draw(StatefulGraphics out, Rectangle wrap) {
        Rectangle bounds = null;
        int y = this.y;

        for (Layer layer : this.layers) {
            layer.y = y;
            Rectangle layerBounds = layer.draw(out, wrap);
            bounds = bounds == null ? layerBounds : DrawingUtil.encompassing(bounds, layerBounds);

            y += layerBounds.height;
        }

        return bounds;
    }
}
