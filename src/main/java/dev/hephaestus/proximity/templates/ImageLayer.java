package dev.hephaestus.proximity.templates;

import dev.hephaestus.proximity.util.StatefulGraphics;

import java.awt.*;
import java.awt.image.BufferedImage;

public class ImageLayer extends Layer {
    private final String name;
    private final BufferedImage image;

    public ImageLayer(String name, BufferedImage image, Integer width, Integer height, int x, int y) {
        super(x, y);

        this.name = name;

        if (width == image.getWidth() && height == image.getHeight()) {
            this.image = image;
        } else {
            if (width == null && height != null) {
                double ratio = image.getWidth() / (double) image.getHeight();
                width = (int) Math.round(ratio * height);
            } else if (height == null && width != null) {
                double ratio = image.getWidth() / (double) image.getHeight();
                height = (int) Math.round(width / ratio);
            }

            this.image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

            Image scaled = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            Graphics2D graphics = this.image.createGraphics();

            graphics.drawImage(scaled, 0, 0, null);
            graphics.dispose();
        }
    }

    @Override
    public Rectangle draw(StatefulGraphics out, Rectangle wrap) {
        out.push(this.x, this.y);
        out.drawImage(this.image, null, null);
        out.pop();

        return new Rectangle(this.x, this.y, this.image.getWidth(), this.image.getHeight());
    }

    @Override
    public String toString() {
        return "Image[" + this.name + "]";
    }
}
