package dev.hephaestus.proximity.cards.layers;

import dev.hephaestus.proximity.templates.layers.ImageLayer;
import dev.hephaestus.proximity.util.*;
import dev.hephaestus.proximity.xml.LayerRenderer;
import dev.hephaestus.proximity.xml.RenderableCard;

import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Optional;

public class ImageLayerRenderer extends LayerRenderer {
    @Override
    public Result<Optional<Rectangles>> renderLayer(RenderableCard card, RenderableCard.XMLElement element, StatefulGraphics graphics, Rectangles wrap, boolean draw, Box<Float> scale, Rectangle2D bounds) {
        if (!element.hasAttribute("src") && !element.hasAttribute("id")) {
            return Result.error("Image layer must have either 'src' or 'id' attribute");
        }

        int x = (element.hasAttribute("x") ? Integer.decode(element.getAttribute("x")) : 0);
        int y = (element.hasAttribute("y") ? Integer.decode(element.getAttribute("y")) : 0);

        String src = element.hasAttribute("src") ? element.getAttribute("src") : null;

        src = ParsingUtil.getFileLocation(element.getParentId(), element.getAttribute("id"), src) + ".png";
        BufferedImage image = card.getImage(src);

        int width = element.hasAttribute("width") ? Integer.decode(element.getAttribute("width")) : image.getWidth();
        int height = element.hasAttribute("height") ? Integer.decode(element.getAttribute("height")) : image.getHeight();

        return Result.of(Optional.ofNullable(new ImageLayer(
                element.getId(),
                x,
                y,
                image,
                width,
                height,
                src).draw(graphics, wrap, draw, scale
        )));
    }
}
