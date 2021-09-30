package dev.hephaestus.proximity.cards.layers;

import dev.hephaestus.proximity.templates.layers.FillLayer;
import dev.hephaestus.proximity.util.Result;
import dev.hephaestus.proximity.util.StatefulGraphics;
import dev.hephaestus.proximity.xml.LayerRenderer;
import dev.hephaestus.proximity.xml.RenderableCard;

import java.awt.geom.Rectangle2D;
import java.util.Optional;

public class FillLayerRenderer extends LayerRenderer {
    @Override
    public Result<Optional<Rectangle2D>> renderLayer(RenderableCard card, RenderableCard.XMLElement element, StatefulGraphics graphics, Rectangle2D wrap, boolean draw, float scale, Rectangle2D bounds) {
        int x = (element.hasAttribute("x") ? Integer.decode(element.getAttribute("x")) : 0);
        int y = (element.hasAttribute("y") ? Integer.decode(element.getAttribute("y")) : 0);
        int width = Integer.decode(element.getAttribute("width"));
        int height = Integer.decode(element.getAttribute("height"));
        int color = element.hasAttribute("color")
                ? element.getInteger("color") : 0;

        return Result.of(Optional.ofNullable(new FillLayer(element.getId(), x, y, width, height, color)
                .draw(graphics, wrap, draw, scale)
        ));
    }
}
