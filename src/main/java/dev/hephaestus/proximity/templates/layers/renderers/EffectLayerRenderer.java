package dev.hephaestus.proximity.templates.layers.renderers;

import dev.hephaestus.proximity.api.tasks.Effect;
import dev.hephaestus.proximity.util.Box;
import dev.hephaestus.proximity.util.Rectangles;
import dev.hephaestus.proximity.util.Result;
import dev.hephaestus.proximity.util.StatefulGraphics;
import dev.hephaestus.proximity.xml.LayerRenderer;
import dev.hephaestus.proximity.xml.RenderableData;

import java.awt.geom.Rectangle2D;
import java.util.Optional;

public class EffectLayerRenderer extends LayerRenderer {
    public EffectLayerRenderer(RenderableData data) {
        super(data);
    }

    @Override
    public Result<Optional<Rectangles>> renderLayer(RenderableData card, RenderableData.XMLElement element, StatefulGraphics graphics, Rectangles wrap, boolean draw, Box<Float> scale, Rectangle2D bounds) {
        Effect effect = card.getTaskHandler().getTask("Effect", element.getAttribute("name"));

        if (effect != null) {
            effect.apply(card, graphics.getImage(), element);
        }

        return Result.of(Optional.of(Rectangles.singleton(new Rectangle2D.Double(0, 0,
                graphics.getImage().getWidth(),
                graphics.getImage().getHeight()
        ))));
    }
}
