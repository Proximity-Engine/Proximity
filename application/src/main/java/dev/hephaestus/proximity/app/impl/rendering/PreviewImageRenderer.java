package dev.hephaestus.proximity.app.impl.rendering;


import dev.hephaestus.proximity.app.api.Template;
import dev.hephaestus.proximity.app.api.rendering.Canvas;
import dev.hephaestus.proximity.app.api.rendering.ImageRenderer;
import dev.hephaestus.proximity.app.api.rendering.elements.Image;
import dev.hephaestus.proximity.app.api.rendering.properties.ImageProperty;
import dev.hephaestus.proximity.app.api.rendering.util.BoundingBox;
import dev.hephaestus.proximity.app.api.rendering.util.BoundingBoxes;
import dev.hephaestus.proximity.app.api.rendering.util.Rect;
import dev.hephaestus.proximity.app.impl.rendering.properties.ImagePropertyImpl;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class PreviewImageRenderer extends ImageRenderer {
    private final int width, height;

    public PreviewImageRenderer(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public Canvas createCanvas(int width, int height, int dpi) {
        Canvas canvas = new Canvas(this.width, this.height, dpi);

        float rW = ((float) this.width) / width;
        float rH = ((float) this.height) / height;

        canvas.push(rW, rH);

        return canvas;
    }

    private URL downscale(Image<?> node, int n) throws IOException {
        Path path = Path.of(".tmp", "downscaled", this.width + "x" + this.height);
        Template<?> template = node.getDocument().getTemplate();
        ImagePropertyImpl<?> src = (ImagePropertyImpl<?>) node.src();

        switch (src.getType()) {
            case UNSET -> throw new UnsupportedOperationException();
            case TEMPLATE_RESOURCE -> {
                path = path.resolve(template.getClass().getModule().getName());
                path = path.resolve(template.getName());
                path = path.resolve(node.getPath() + "." + node.getFormat());
            }
            case DYNAMIC -> {
                URL url = src.getUrl();

                path = path.resolve(URLEncoder.encode(url.getHost(), StandardCharsets.US_ASCII));

                if (url.getQuery() == null) {
                    for (String s : url.getPath().substring(1).split("/")) {
                        path = path.resolve(URLEncoder.encode(s, StandardCharsets.US_ASCII));
                    }

                    path = path.resolveSibling(path.getFileName().toString() + ".png");

                    Files.createDirectories(path.getParent());
                } else {
                    for (String s : url.getPath().substring(1).split("/")) {
                        path = path.resolve(URLEncoder.encode(s, StandardCharsets.US_ASCII));
                    }

                    Files.createDirectories(path);

                    path = path.resolve(URLEncoder.encode(url.getQuery(), StandardCharsets.US_ASCII) + node.getFormat());
                }

            }
        }

        Files.createDirectories(path.getParent());

        if (!Files.exists(path)) {
            BoundingBoxes boxes = node.getBounds();
            Rect destinationDimensions = new Rect((int) (boxes.getWidth() / n), (int) (boxes.getHeight() / n));

            BufferedImage image = ImageIO.read(node.src().get());

            BufferedImage out = new BufferedImage(destinationDimensions.width(), destinationDimensions.height(), BufferedImage.TYPE_INT_ARGB);
            out.getGraphics().drawImage(image.getScaledInstance(destinationDimensions.width(), destinationDimensions.height(), java.awt.Image.SCALE_SMOOTH), 0, 0, null);

            ImageIO.write(out, "png", Files.newOutputStream(path));
        }

        return path.toUri().toURL();
    }

    @Override
    protected void render(Image<?> image, Canvas canvas) throws IOException {
        int n = (int) Math.round(1D / canvas.getTransform().getScaleX());

        BoundingBox dimensions = image.getBounds().iterator().next();

        canvas.drawImage(ImageIO.read(this.downscale(image, n)),
                (int) dimensions.getX(),
                (int) dimensions.getY(),
                (int) dimensions.getWidth(),
                (int) dimensions.getHeight(),
                null
        );
    }
}
