package dev.hephaestus.proximity.app.impl.rendering;


import dev.hephaestus.proximity.app.api.Template;
import dev.hephaestus.proximity.app.api.rendering.Canvas;
import dev.hephaestus.proximity.app.api.rendering.ImageRenderer;
import dev.hephaestus.proximity.app.api.rendering.elements.Image;
import dev.hephaestus.proximity.app.api.rendering.util.BoundingBox;
import dev.hephaestus.proximity.app.api.rendering.util.BoundingBoxes;
import dev.hephaestus.proximity.app.api.rendering.util.Rect;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

public class PreviewImageRenderer extends ImageRenderer {
    private final int width, height;

    public PreviewImageRenderer(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public Canvas createCanvas(int width, int height) {
        Canvas canvas = new Canvas(this.width, this.height);

        float rW = ((float) this.width) / width;
        float rH = ((float) this.height) / height;

        canvas.push(rW, rH);

        return canvas;
    }

    private URL downscale(Image<?> node, int n) throws IOException {
        Path path = Path.of(".tmp", "downscaled", "" + this.width, "" + this.height);

        Template<?> template = node.getDocument().getTemplate();

        path = path.resolve(template.getClass().getModule().getName());
        path = path.resolve(template.getName());
        path = path.resolve(node.getId());

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
