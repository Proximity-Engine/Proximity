package dev.hephaestus.deckbuilder.templates;

import java.awt.*;
import java.awt.image.BufferedImage;

public class ImageLayer implements Layer {
    private final String name;
    private final BufferedImage image;

    public ImageLayer(String name, BufferedImage image, Integer width, Integer height) {
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
    public void draw(Graphics2D out) {
        out.drawImage(this.image, null, null);
    }

    @Override
    public String toString() {
        return "Image[" + this.name + "]";
    }
}
