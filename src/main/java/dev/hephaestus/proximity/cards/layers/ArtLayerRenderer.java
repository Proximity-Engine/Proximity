package dev.hephaestus.proximity.cards.layers;

import dev.hephaestus.proximity.cardart.ArtResolver;
import dev.hephaestus.proximity.templates.layers.ImageLayer;
import dev.hephaestus.proximity.util.Rectangles;
import dev.hephaestus.proximity.util.Result;
import dev.hephaestus.proximity.util.StatefulGraphics;
import dev.hephaestus.proximity.xml.LayerRenderer;
import dev.hephaestus.proximity.xml.RenderableCard;

import javax.imageio.ImageIO;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;

public class ArtLayerRenderer extends LayerRenderer {
    @Override
    public Result<Optional<Rectangles>> renderLayer(RenderableCard card, RenderableCard.XMLElement element, StatefulGraphics graphics, Rectangles wrap, boolean draw, float scale, Rectangle2D bounds) {
        if (!element.hasAttribute("width") && !element.hasAttribute("height")) {
            return Result.error("Image layer must have either 'width' or 'height' attribute");
        }

        int x = (element.hasAttribute("x") ? Integer.decode(element.getAttribute("x")) : 0);
        int y = (element.hasAttribute("y") ? Integer.decode(element.getAttribute("y")) : 0);
        Integer width = element.hasAttribute("width") ? Integer.decode(element.getAttribute("width")) : null;
        Integer height = element.hasAttribute("height") ? Integer.decode(element.getAttribute("height")) : null;

        try {
            Optional<String> optionalFileLocation = new ArtResolver().findArt(card);
            if(optionalFileLocation.isPresent()) {
                String fileLocation = optionalFileLocation.get();
                return Result.of(Optional.ofNullable(new ImageLayer(
                    element.getId(),
                    x,
                    y,
                    ImageIO.read(new URL(fileLocation)),
                    width,
                    height,
                    fileLocation).draw(graphics, wrap, draw, scale)
                ));
            }
            else
                return Result.of(Optional.empty());
        } catch (IOException e) {
            return Result.error("Failed to create layer '%s': %s", element.getId(), e.getMessage());
        }
    }
}
