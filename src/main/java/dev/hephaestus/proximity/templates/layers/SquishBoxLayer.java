package dev.hephaestus.proximity.templates.layers;

import dev.hephaestus.proximity.util.DrawingUtil;
import dev.hephaestus.proximity.util.StatefulGraphics;

import java.awt.*;

public class SquishBoxLayer extends Layer {
    private final Layer main, flex;

    public SquishBoxLayer(String parentId, String id, int x, int y, Layer main, Layer flex) {
        super(parentId, id, x, y);
        this.main = main;
        this.flex = flex;
    }

    @Override
    public Rectangle draw(StatefulGraphics out, Rectangle wrap) {
        Rectangle mainLayerBounds = this.main.draw(out, wrap);
        Rectangle flexLayerBounds = this.flex.draw(out, mainLayerBounds);

        return mainLayerBounds != null && flexLayerBounds != null
                ? DrawingUtil.encompassing(mainLayerBounds, flexLayerBounds)
                : mainLayerBounds == null ? flexLayerBounds : mainLayerBounds;

    }
}
