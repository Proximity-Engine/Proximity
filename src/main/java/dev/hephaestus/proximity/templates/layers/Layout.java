package dev.hephaestus.proximity.templates.layers;

import dev.hephaestus.proximity.util.ContentAlignment;
import dev.hephaestus.proximity.util.DrawingUtil;
import dev.hephaestus.proximity.util.StatefulGraphics;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class Layout extends Group {
    private final ContentAlignment alignment;
    private final BiConsumer<Layer, Integer> inLineSetter;
    private final BiConsumer<Layer, Integer> offLineSetter;
    private final Function<Layer, Integer> inLineGetter;
    private final Function<Layer, Integer> offLineGetter;
    private final Function<Rectangle, Double> inLineSizeGetter;
    private final Function<Rectangle, Double> offLineSizeGetter;
    private final Rectangle bounds;

    public Layout(String parentId, String id, int x, int y, List<Layer> layers, Integer width, Integer height, ContentAlignment alignment, BiConsumer<Layer, Integer> inLineSetter, BiConsumer<Layer, Integer> offLineSetter, Function<Layer, Integer> inLineGetter, Function<Layer, Integer> offLineGetter, Function<Rectangle, Double> inLineSizeGetter, Function<Rectangle, Double> offLineSizeGetter) {
        super(parentId, id, x, y, layers);
        this.bounds = width == null || height == null ? null : new Rectangle(x, y, width, height);
        this.alignment = alignment;
        this.inLineSetter = inLineSetter;
        this.offLineSetter = offLineSetter;
        this.inLineGetter = inLineGetter;
        this.offLineGetter = offLineGetter;
        this.inLineSizeGetter = inLineSizeGetter;
        this.offLineSizeGetter = offLineSizeGetter;
    }

    @Override
    public Rectangle draw(StatefulGraphics out, Rectangle wrap, boolean draw, int scale) {
        if (this.layers.isEmpty()) return new Rectangle();

        Rectangle bounds = null;

        int i = 0;
        int inLine = this.inLineGetter.apply(this);


        Map<Layer, Rectangle> drawnBounds = new HashMap<>();

        do {
            for (Layer layer : this.layers) {
                this.inLineSetter.accept(layer, inLine);

                if (this.bounds != null) {
                    this.offLineSetter.accept(layer, this.offLineGetter.apply(this));
                    layer.setBounds(new Rectangle(this.offLineGetter.apply(this), inLine, this.offLineSizeGetter.apply(this.bounds).intValue(), (int) (this.inLineSizeGetter.apply(this.bounds) - (inLine - this.inLineGetter.apply(this)))));
                }

                Rectangle layerBounds = layer.draw(out, wrap, false, scale);

                drawnBounds.put(layer, layerBounds);

                bounds = bounds == null
                        ? this.offLineSizeGetter.apply(layerBounds) > 0 && this.inLineSizeGetter.apply(layerBounds) > 0
                                ? layerBounds : null
                        : this.offLineSizeGetter.apply(layerBounds) > 0 && this.inLineSizeGetter.apply(layerBounds) > 0
                                ? DrawingUtil.encompassing(bounds, layerBounds) : bounds;

                inLine += this.inLineSizeGetter.apply(layerBounds);
            }

            if (bounds != null && this.bounds != null && (this.offLineSizeGetter.apply(bounds) > this.offLineSizeGetter.apply(this.bounds) || this.inLineSizeGetter.apply(bounds) > this.inLineSizeGetter.apply(this.bounds))) {
                scale -= 1;
                bounds = null;
                inLine = this.inLineGetter.apply(this);
            }

            ++i;
        } while (bounds == null && i < 100);

        int dY = 0;

        if (this.bounds != null) {
            dY = switch (this.alignment) {
                case START -> 0;
                case MIDDLE -> bounds == null ? 0 : (this.inLineSizeGetter.apply(this.bounds).intValue() - this.inLineSizeGetter.apply(bounds).intValue()) / 2;
                case END -> bounds == null ? 0 : this.inLineSizeGetter.apply(this.bounds).intValue() - this.inLineSizeGetter.apply(bounds).intValue();
            };
        }

        for (Layer layer : this.layers) {
            this.inLineSetter.accept(layer, this.inLineGetter.apply(layer) + dY);

            if (this.bounds != null) {
                layer.setBounds(new Rectangle(this.offLineGetter.apply(this), this.inLineGetter.apply(layer) + dY, this.offLineSizeGetter.apply(this.bounds).intValue(), this.inLineSizeGetter.apply(drawnBounds.get(layer)).intValue()));
            }

            Rectangle layerBounds = layer.draw(out, wrap, draw, scale);

            bounds = bounds == null
                    ? this.offLineSizeGetter.apply(layerBounds) > 0 && this.inLineSizeGetter.apply(layerBounds) > 0
                    ? layerBounds : null
                    : this.offLineSizeGetter.apply(layerBounds) > 0 && this.inLineSizeGetter.apply(layerBounds) > 0
                    ? DrawingUtil.encompassing(bounds, layerBounds) : bounds;
        }

        return bounds == null ? new Rectangle() : bounds;
    }
}
