package dev.hephaestus.proximity.templates.layers.renderers;

import dev.hephaestus.proximity.util.Box;
import dev.hephaestus.proximity.util.Rectangles;
import dev.hephaestus.proximity.util.Result;
import dev.hephaestus.proximity.util.StatefulGraphics;
import dev.hephaestus.proximity.xml.LayerRenderer;
import dev.hephaestus.proximity.xml.RenderableData;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.Optional;

public class RectangleLayerRenderer extends LayerRenderer {
    public RectangleLayerRenderer(RenderableData data) {
        super(data);
    }

    @Override
    public Result<Optional<Rectangles>> renderLayer(RenderableData card, RenderableData.XMLElement element, StatefulGraphics graphics, Rectangles wrap, boolean draw, Box<Float> scale, Rectangle2D bounds) {
        int x = (element.hasAttribute("x") ? Integer.decode(element.getAttribute("x")) : 0);
        int y = (element.hasAttribute("y") ? Integer.decode(element.getAttribute("y")) : 0);
        int width = Integer.decode(element.getAttribute("width"));
        int height = Integer.decode(element.getAttribute("height"));
        int color = element.hasAttribute("color")
                ? element.getInteger("color") : 0;

        Rectangle2D rectangle = new Rectangle2D.Double(x, y, width, height);

        if (draw && (color & 0xFF000000) != 0) {
            graphics.push(new Color(color, (color & 0xFF000000) >>> 24 != 255), Graphics2D::setColor, Graphics2D::getColor);
            graphics.fill(rectangle);
            graphics.pop();
        }

        return Result.of(Optional.of(Rectangles.singleton(rectangle)));
    }
}
