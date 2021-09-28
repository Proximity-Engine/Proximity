package dev.hephaestus.proximity.cards.layers;

import dev.hephaestus.proximity.xml.LayerRenderer;
import dev.hephaestus.proximity.xml.RenderableCard;
import dev.hephaestus.proximity.util.Pair;
import dev.hephaestus.proximity.util.Result;
import dev.hephaestus.proximity.util.StatefulGraphics;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class ParentLayerRenderer extends LayerRenderer {
    protected abstract Result<Optional<Rectangle2D>> renderLayer(RenderableCard card, RenderableCard.XMLElement element, StatefulGraphics graphics, Rectangle2D wrap, boolean draw, float scale, Rectangle2D bounds, List<Pair<RenderableCard.XMLElement, LayerRenderer>> children);

    @Override
    public final Result<Optional<Rectangle2D>> renderLayer(RenderableCard card, RenderableCard.XMLElement element, StatefulGraphics graphics, Rectangle2D wrap, boolean draw, float scale, Rectangle2D bounds) {
        int x = (element.hasAttribute("x") ? Integer.decode(element.getAttribute("x")) : 0);
        int y = (element.hasAttribute("y") ? Integer.decode(element.getAttribute("y")) : 0);

        List<Pair<RenderableCard.XMLElement, LayerRenderer>> children = new ArrayList<>();

        element.iterate((e, i) -> {
            LayerRenderer renderer = LayerRenderer.get(e.getTagName());

            if (renderer != null) {
                children.add(new Pair<>(e, renderer));
            }
        });

        for (var pair : children) {
            RenderableCard.XMLElement e = pair.left();
            int eX = (e.hasAttribute("x") ? Integer.decode(e.getAttribute("x")) : 0) + x;
            int eY = (e.hasAttribute("y") ? Integer.decode(e.getAttribute("y")) : 0) + y;

            e.pushAttribute("x", Integer.toString(eX));
            e.pushAttribute("y", Integer.toString(eY));
        }

        Result<Optional<Rectangle2D>> result = this.renderLayer(card, element, graphics, wrap, draw, scale, bounds, children);

        for (var pair : children) {
            pair.left().popAttributes(2);
        }

        return result;
    }
}
