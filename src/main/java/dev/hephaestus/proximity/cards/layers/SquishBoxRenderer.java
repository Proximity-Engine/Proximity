package dev.hephaestus.proximity.cards.layers;

import dev.hephaestus.proximity.util.Rectangles;
import dev.hephaestus.proximity.util.Result;
import dev.hephaestus.proximity.util.StatefulGraphics;
import dev.hephaestus.proximity.xml.LayerRenderer;
import dev.hephaestus.proximity.xml.RenderableCard;

import java.awt.geom.Rectangle2D;
import java.util.Optional;

public class SquishBoxRenderer extends LayerRenderer {
    @Override
    public Result<Optional<Rectangles>> renderLayer(RenderableCard card, RenderableCard.XMLElement element, StatefulGraphics graphics, Rectangles wrap, boolean draw, float scale, Rectangle2D bounds) {
        float finalScale1 = scale;
        Optional<Result<Optional<Rectangles>>> main = element.apply("main", (RenderableCard.XMLElement e) -> {
            LayerRenderer renderer = LayerRenderer.get(e.getTagName());

            if (renderer == null) {
                return Result.error("Tag '%s' not recognized as layer type", e.getTagName());
            } else {
                return renderer.render(card, e, graphics, wrap, draw, finalScale1, bounds);
            }
        });

        if (main.isPresent() && main.get().isError()) {
            return main.get();
        }

        Rectangles flexWrap = main.isPresent() && main.get().isOk() && main.get().get().isPresent() ? main.get().get().get() : wrap;

        Optional<Result<Optional<Rectangles>>> flex;

        do {
            float finalScale = scale;
            flex = element.apply("flex", (RenderableCard.XMLElement e) -> {
                LayerRenderer renderer = LayerRenderer.get(e.getTagName());

                if (renderer == null) {
                    return Result.error("Tag '%s' not recognized as layer type", e.getTagName());
                } else {
                    return renderer.render(card, e, graphics, flexWrap, false, finalScale, bounds);
                }
            });

            scale -= 0.5;
        } while (flex.isPresent() && flex.get().isOk() && flex.get().get().isPresent() && flexWrap != null && flex.get().get().get().intersects(flexWrap));

        float finalScale = scale;
        flex = element.apply("flex", (RenderableCard.XMLElement e) -> {
            LayerRenderer renderer = LayerRenderer.get(e.getTagName());

            if (renderer == null) {
                return Result.error("Tag '%s' not recognized as layer type", e.getTagName());
            } else {
                return renderer.render(card, e, graphics, flexWrap, draw, finalScale + 0.5F, bounds);
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
