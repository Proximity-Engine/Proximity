package dev.hephaestus.proximity.templates.layers;

import dev.hephaestus.proximity.util.DrawingUtil;
import dev.hephaestus.proximity.util.StatefulGraphics;

import java.awt.*;

public class SquishBoxLayer extends Layer {
    private final Layer main, flex;

    @Override
    public void setBounds(Rectangle bounds) {
        this.main.setBounds(bounds);
        this.flex.setBounds(bounds);

        super.setBounds(bounds);
    }

    @Override
    public void setWrap(Rectangle wrap) {
        this.main.setWrap(wrap);
        this.flex.setWrap(wrap);

        super.setWrap(wrap);
    }

    @Override
    public void setX(int x) {
        this.main.setX(x - this.getX());
        this.flex.setX(x - this.getX());

        super.setX(x);
    }

    @Override
    public void setY(int y) {
        this.main.setY(y - this.getY());
        this.flex.setY(y - this.getY());

        super.setY(y);
    }

    public SquishBoxLayer(String parentId, String id, int x, int y, Layer main, Layer flex) {
        super(parentId, id, x, y);
        this.main = main;
        this.flex = flex;
    }

    @Override
    public Rectangle draw(StatefulGraphics out, Rectangle wrap, boolean draw, int scale) {
        Rectangle mainLayerBounds = this.main.draw(out, wrap, draw, scale);
        Rectangle flexLayerBounds = this.flex.draw(out, mainLayerBounds, draw, scale);

        return mainLayerBounds != null && flexLayerBounds != null
                ? DrawingUtil.encompassing(mainLayerBounds, flexLayerBounds)
                : mainLayerBounds == null ? flexLayerBounds : mainLayerBounds;

    }
}
