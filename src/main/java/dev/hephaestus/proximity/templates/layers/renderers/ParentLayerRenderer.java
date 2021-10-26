package dev.hephaestus.proximity.templates.layers.renderers;

import dev.hephaestus.proximity.util.*;
import dev.hephaestus.proximity.xml.LayerRenderer;
import dev.hephaestus.proximity.xml.RenderableData;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class ParentLayerRenderer extends LayerRenderer {
    public ParentLayerRenderer(RenderableData data) {
        super(data);
    }

    protected abstract Result<Optional<Rectangles>> renderLayer(RenderableData card, RenderableData.XMLElement element, StatefulGraphics graphics, Rectangles wrap, boolean draw, Box<Float> scale, Rectangle2D bounds, List<Pair<RenderableData.XMLElement, LayerRenderer>> children);

    @Override
    public final Result<Optional<Rectangles>> renderLayer(RenderableData card, RenderableData.XMLElement element, StatefulGraphics graphics, Rectangles wrap, boolean draw, Box<Float> scale, Rectangle2D bounds) {
        int x = (element.hasAttribute("x") ? Integer.decode(element.getAttribute("x")) : 0);
        int y = (element.hasAttribute("y") ? Integer.decode(element.getAttribute("y")) : 0);

        List<Pair<RenderableData.XMLElement, LayerRenderer>> children = new ArrayList<>();

        element.iterate((e, i) -> {
            LayerRenderer renderer = this.data.getLayerRenderer(e.getTagName());

            if (renderer != null) {
                children.add(new Pair<>(e, renderer));
            }
        });

        for (var pair : children) {
            RenderableData.XMLElement e = pair.left();
            int eX = (e.hasAttribute("x") ? Integer.decode(e.getAttribute("x")) : 0) + x;
            int eY = (e.hasAttribute("y") ? Integer.decode(e.getAttribute("y")) : 0) + y;

            e.pushAttribute("x", Integer.toString(eX));
            e.pushAttribute("y", Integer.toString(eY));
        }

        Result<Optional<Rectangles>> result = this.renderLayer(card, element, graphics, wrap, draw, scale, bounds, children);

        for (var pair : children) {
            pair.left().popAttributes(2);
        }

        return result;
    }

    @Override
    public boolean scales(RenderableData card, RenderableData.XMLElement element) {
        int x = (element.hasAttribute("x") ? Integer.decode(element.getAttribute("x")) : 0);
        int y = (element.hasAttribute("y") ? Integer.decode(element.getAttribute("y")) : 0);

        List<Pair<RenderableData.XMLElement, LayerRenderer>> children = new ArrayList<>();

        element.iterate((e, i) -> {
            LayerRenderer renderer = this.data.getLayerRenderer(e.getTagName());

            if (renderer != null) {
                children.add(new Pair<>(e, renderer));
            }
        });

        boolean scale = false;

        for (var pair : children) {
            RenderableData.XMLElement e = pair.left();
            int eX = (e.hasAttribute("x") ? Integer.decode(e.getAttribute("x")) : 0) + x;
            int eY = (e.hasAttribute("y") ? Integer.decode(e.getAttribute("y")) : 0) + y;

            e.pushAttribute("x", Integer.toString(eX));
            e.pushAttribute("y", Integer.toString(eY));

            scale = pair.right().scales(card, pair.left());

            e.popAttributes(2);

            if (scale) break;
        }

        return scale;
    }
}
