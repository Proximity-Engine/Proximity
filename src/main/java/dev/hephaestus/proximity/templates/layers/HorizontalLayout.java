package dev.hephaestus.proximity.templates.layers;

import dev.hephaestus.proximity.util.DrawingUtil;
import dev.hephaestus.proximity.util.StatefulGraphics;

import java.awt.Rectangle;
import java.util.List;

public class HorizontalLayout extends Layer {
    private final List<Layer> layers;

    public HorizontalLayout(String parentId, String id, int x, int y, List<Layer> layers) {
        super(parentId, id, x, y);
        this.layers = layers;
    }

    @Override
    public Rectangle draw(StatefulGraphics out, Rectangle wrap) {
        Rectangle bounds = null;
        int x = this.x;

        for (Layer layer : this.layers) {
            layer.x = x;
            Rectangle layerBounds = layer.draw(out, wrap);
            bounds = bounds == null ? layerBounds : DrawingUtil.encompassing(bounds, layerBounds);

            x += layerBounds.width;
        }

        return bounds;
    }
}
