package dev.hephaestus.proximity.cards.layers;

import dev.hephaestus.proximity.util.Box;
import dev.hephaestus.proximity.util.Rectangles;
import dev.hephaestus.proximity.util.Result;
import dev.hephaestus.proximity.util.StatefulGraphics;
import dev.hephaestus.proximity.xml.LayerRenderer;
import dev.hephaestus.proximity.xml.RenderableCard;

import java.awt.geom.Rectangle2D;
import java.util.Optional;
import java.util.Random;

public class NoiseLayerRenderer extends LayerRenderer {
    @Override
    public Result<Optional<Rectangles>> renderLayer(RenderableCard card, RenderableCard.XMLElement element, StatefulGraphics graphics, Rectangles wrap, boolean draw, Box<Float> scale, Rectangle2D bounds) {
        int x = (element.hasAttribute("x") ? Integer.decode(element.getAttribute("x")) : 0);
        int y = (element.hasAttribute("y") ? Integer.decode(element.getAttribute("y")) : 0);
        int width = Integer.decode(element.getAttribute("width"));
        int height = Integer.decode(element.getAttribute("height"));

        if (width > 0 && height > 0 && x < card.getWidth() && y < card.getHeight()) {
            int[] colors = graphics.getImage().getRGB(x, y, width, height, null, 0, width);

            Random random = new Random();

            for (int i = 0; i < colors.length; ++i) {
                colors[i] = random.nextInt();
            }

            graphics.getImage().setRGB(x, y, width, height, colors, 0, width);

            return Result.of(Optional.of(Rectangles.singleton(new Rectangle2D.Double(x, y, width, height))));
        } else {
            return Result.of(Optional.empty());
        }
    }
}
