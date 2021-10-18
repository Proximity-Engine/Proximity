package dev.hephaestus.proximity.templates.layers;

import dev.hephaestus.proximity.util.Box;
import dev.hephaestus.proximity.util.Rectangles;
import dev.hephaestus.proximity.util.StatefulGraphics;

import java.awt.*;
import java.awt.image.BufferedImage;

public class ImageLayer extends Layer {
    private final BufferedImage image;
    private final String fileLocation;

    public ImageLayer(String id, int x, int y, BufferedImage image, Integer width, Integer height, String fileLocation) {
        super(id, x, y);
        this.fileLocation = fileLocation;

        if ((width == null && height == null) || (width != null && height != null&& width == image.getWidth() && height == image.getHeight())) {
            this.image = image;
        } else {
            if (width == null) {
                double ratio = image.getWidth() / (double) image.getHeight();
                width = (int) Math.round(ratio * height);
            } else if (height == null) {
                double ratio = image.getWidth() / (double) image.getHeight();
                height = (int) Math.round(width / ratio);
            }

            this.image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            float ri = image.getHeight() / (float) image.getWidth();
            float rs = height / (float) width;

            int w = rs > ri ? (int) (height / ri) : width;
            int h = rs > ri ? height : (int) (width * ri);

            Image scaled = image.getScaledInstance(w, h, Image.SCALE_SMOOTH);
            Graphics2D graphics = this.image.createGraphics();

            graphics.drawImage(scaled, (width - w) / 2, (height - h) / 2, null);
            graphics.dispose();
        }
    }

    @Override
    public Rectangles draw(StatefulGraphics out, Rectangles wrap, boolean draw, Box<Float> scale) {
        if (draw) {
            out.push(this.getX(), this.getY());
            out.drawImage(this.image, null, null);
            out.pop();
        }

        return Rectangles.singleton(new Rectangle(this.getX(), this.getY(), this.image.getWidth(), this.image.getHeight()));
    }

    @Override
    public String toString() {
        return "Image[" + this.getId() + ";" + this.fileLocation + "]";
    }
}
