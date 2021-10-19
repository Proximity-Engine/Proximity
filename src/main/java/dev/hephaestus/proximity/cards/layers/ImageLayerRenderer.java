package dev.hephaestus.proximity.cards.layers;

import dev.hephaestus.proximity.templates.layers.ImageLayer;
import dev.hephaestus.proximity.util.*;
import dev.hephaestus.proximity.xml.LayerRenderer;
import dev.hephaestus.proximity.xml.RenderableCard;

import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ImageLayerRenderer extends LayerRenderer {
    private static final Map<String, Pair<Integer, Integer>> IMAGE_DIMENSION_CACHE = new HashMap<>();

    @Override
    public Result<Optional<Rectangles>> renderLayer(RenderableCard card, RenderableCard.XMLElement element, StatefulGraphics graphics, Rectangles wrap, boolean draw, Box<Float> scale, Rectangle2D bounds) {
        if (!element.hasAttribute("src") && !element.hasAttribute("id")) {
            return Result.error("Image layer must have either 'src' or 'id' attribute");
        }

        int x = (element.hasAttribute("x") ? Integer.decode(element.getAttribute("x")) : 0);
        int y = (element.hasAttribute("y") ? Integer.decode(element.getAttribute("y")) : 0);

        String src = element.hasAttribute("src") ? element.getAttribute("src") : null;
        src = ParsingUtil.getFileLocation(element.getParentId(), element.getAttribute("id"), src) + ".png";

        if (draw) {
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
        } else {
            Pair<Integer, Integer> dimensions = IMAGE_DIMENSION_CACHE.computeIfAbsent(src, s -> {
                BufferedImage image = card.getImage(s);
                int width = element.hasAttribute("width") ? Integer.decode(element.getAttribute("width")) : image.getWidth();
                int height = element.hasAttribute("height") ? Integer.decode(element.getAttribute("height")) : image.getHeight();

                return new Pair<>(width, height);
            });

            return Result.of(Optional.of(Rectangles.singleton(new Rectangle2D.Double(x, y, dimensions.left(), dimensions.right()))));
        }
    }
}
