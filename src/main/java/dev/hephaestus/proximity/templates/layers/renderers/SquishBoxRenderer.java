package dev.hephaestus.proximity.templates.layers.renderers;

import dev.hephaestus.proximity.util.Box;
import dev.hephaestus.proximity.util.Rectangles;
import dev.hephaestus.proximity.util.Result;
import dev.hephaestus.proximity.util.StatefulGraphics;
import dev.hephaestus.proximity.xml.LayerRenderer;
import dev.hephaestus.proximity.xml.RenderableData;

import java.awt.geom.Rectangle2D;
import java.util.Optional;

public class SquishBoxRenderer extends LayerRenderer {
    public SquishBoxRenderer(RenderableData data) {
        super(data);
    }

    @Override
    public Result<Optional<Rectangles>> renderLayer(RenderableData card, RenderableData.XMLElement element, StatefulGraphics graphics, Rectangles wrap, boolean draw, Box<Float> scale, Rectangle2D bounds) {
        Optional<Result<Optional<Rectangles>>> main = element.apply("Main", (RenderableData.XMLElement e) -> {
            LayerRenderer renderer = this.data.getLayerRenderer(e.getTagName());

            if (renderer == null) {
                return Result.error("Tag '%s' not recognized as layer type", e.getTagName());
            } else {
                return renderer.render(card, e, graphics, wrap, draw, scale, bounds);
            }
        });

        if (main.isPresent() && main.get().isError()) {
            return main.get();
        }

        Rectangles flexWrap = main.isPresent() && main.get().isOk() && main.get().get().isPresent() ? main.get().get().get() : wrap;

        Optional<Result<Optional<Rectangles>>> flex;

        do {
            flex = element.apply("Flex", (RenderableData.XMLElement e) -> {
                LayerRenderer renderer = this.data.getLayerRenderer(e.getTagName());

                if (renderer == null) {
                    return Result.error("Tag '%s' not recognized as layer type", e.getTagName());
                } else {
                    return renderer.render(card, e, graphics, flexWrap, false, scale, bounds);
                }
            });

            scale.set(scale.get() - 0.5F);
        } while (flex.isPresent() && flex.get().isOk() && flex.get().get().isPresent() && flexWrap != null && flex.get().get().get().intersects(flexWrap));

        flex = element.apply("Flex", (RenderableData.XMLElement e) -> {
            LayerRenderer renderer = this.data.getLayerRenderer(e.getTagName());

            if (renderer == null) {
                return Result.error("Tag '%s' not recognized as layer type", e.getTagName());
            } else {
                return renderer.render(card, e, graphics, flexWrap, draw, scale.set(scale.get() + 0.5F), bounds);
            }
        });

        if (flex.isPresent() && flex.get().isError()) {
            return flex.get();
        }

        if (main.isPresent() && main.get().get().isPresent() && flex.isPresent() && flex.get().get().isPresent()) {
            Rectangles rs = main.get().get().get();
            rs.addAll(flex.get().get().get());
            return Result.of(Optional.of(rs));
        } else if (main.isPresent() && main.get().get().isPresent()) {
            return Result.of(Optional.of(main.get().get().get()));
        } else if (flex.isPresent() && flex.get().get().isPresent()) {
            return Result.of(Optional.of(flex.get().get().get()));
        } else {
            return Result.of(Optional.empty());
        }
    }
}
