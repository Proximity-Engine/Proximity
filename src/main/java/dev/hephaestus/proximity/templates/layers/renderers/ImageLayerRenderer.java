package dev.hephaestus.proximity.templates.layers.renderers;

import dev.hephaestus.proximity.util.*;
import dev.hephaestus.proximity.xml.LayerRenderer;
import dev.hephaestus.proximity.xml.RenderableData;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class ImageLayerRenderer extends LayerRenderer {
    private static final Map<String, Pair<Integer, Integer>> IMAGE_DIMENSION_CACHE = new ConcurrentHashMap<>();

    public ImageLayerRenderer(RenderableData data) {
        super( data);
    }

    @Override
    public Result<Optional<Rectangles>> renderLayer(RenderableData card, RenderableData.XMLElement element, StatefulGraphics graphics, Rectangles wrap, boolean draw, Box<Float> scale, Rectangle2D bounds) {
        if (!element.hasAttribute("src") && !element.hasAttribute("id") && !element.hasAttribute("url")) {
            return Result.error("Image layer must have either 'src', 'url', or 'id' attribute");
        }

        int x = (element.hasAttribute("x") ? Integer.decode(element.getAttribute("x")) : 0);
        int y = (element.hasAttribute("y") ? Integer.decode(element.getAttribute("y")) : 0);

        String cacheKey;

        if (element.hasAttribute("url")) {
            cacheKey = element.getAttribute("url");
        } else {
            cacheKey = element.hasAttribute("src") ? element.getAttribute("src") : null;
            cacheKey = ParsingUtil.getFileLocation(element.getParentId(), element.getAttribute("id"), cacheKey) + ".png";
        }

        if (draw) {
            Integer width = element.hasAttribute("width") ? Integer.decode(element.getAttribute("width")) : null;
            Integer height = element.hasAttribute("height") ? Integer.decode(element.getAttribute("height")) : null;

            BufferedImage image = scale(this.getImage(element), width, height);

            graphics.push(x, y);
            graphics.drawImage(image, null, null);
            graphics.pop();

            return Result.of(Optional.of(Rectangles.singleton(new Rectangle(
                    x,
                    y,
                    image.getWidth(),
                    image.getHeight()
            ))));
        } else {
            Pair<Integer, Integer> dimensions = IMAGE_DIMENSION_CACHE.computeIfAbsent(cacheKey, key -> {
                Integer width = element.hasAttribute("width") ? Integer.decode(element.getAttribute("width")) : null;
                Integer height = element.hasAttribute("height") ? Integer.decode(element.getAttribute("height")) : null;

                BufferedImage image = scale(this.getImage(element), width, height);

                return new Pair<>(image.getWidth(), image.getHeight());
            });

            return Result.of(Optional.of(Rectangles.singleton(new Rectangle2D.Double(x, y, dimensions.left(), dimensions.right()))));
        }
    }

    private BufferedImage getImage(RenderableData.XMLElement element) {
        if (element.hasAttribute("url")) {
            try {
                InputStream input = this.data.getProximity().getRemoteFileCache().open(URI.create(element.getAttribute("url")));

                synchronized (ImageLayerRenderer.class) {
                    return ImageIO.read(input);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            String src = element.hasAttribute("src") ? element.getAttribute("src") : null;
            src = ParsingUtil.getFileLocation(element.getParentId(), element.getAttribute("id"), src) + ".png";

            return this.data.getImage(src);
        }
    }

    private static BufferedImage scale(BufferedImage image, Integer width, Integer height) {
        BufferedImage result = image;

        if ((width != null || height != null) && (width == null || height == null || width != image.getWidth() || height != image.getHeight())) {
            if (width == null) {
                double ratio = image.getWidth() / (double) image.getHeight();
                width = (int) Math.round(ratio * height);
            } else if (height == null) {
                double ratio = image.getWidth() / (double) image.getHeight();
                height = (int) Math.round(width / ratio);
            }

            result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            float ri = image.getHeight() / (float) image.getWidth();
            float rs = height / (float) width;

            int w = rs > ri ? (int) (height / ri) : width;
            int h = rs > ri ? height : (int) (width * ri);

            Image scaled = image.getScaledInstance(w, h, Image.SCALE_SMOOTH);
            Graphics2D graphics = result.createGraphics();

            graphics.drawImage(scaled, (width - w) / 2, (height - h) / 2, null);
            graphics.dispose();
        }

        return result;
    }
}
