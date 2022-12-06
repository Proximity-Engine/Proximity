package dev.hephaestus.proximity.app.api.rendering;

import dev.hephaestus.proximity.app.api.text.TextComponent;
import dev.hephaestus.proximity.app.api.rendering.elements.Image;
import dev.hephaestus.proximity.app.api.rendering.util.BoundingBox;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.io.OutputStream;

public class ImageRenderer extends Renderer<Canvas> {
    @Override
    public Canvas createCanvas(int width, int height, int dpi) {
        return new Canvas(width, height, dpi);
    }

    @Override
    protected void render(Image<?> image, Canvas canvas) throws IOException {
        BoundingBox dimensions = image.getBounds().iterator().next();

        canvas.drawImage(ImageIO.read(image.src().get()),
                (int) dimensions.getX(),
                (int) dimensions.getY(),
                (int) dimensions.getWidth(),
                (int) dimensions.getHeight(),
                null
        );
    }

    @Override
    protected void render(Document<?> document, Canvas canvas, TextComponent component, int x, int y, BoundingBox bounds) {
        Font font = document.getTemplate().getFont(
                component.italic ? component.style.getItalicFontName() : component.style.getFontName(), (float) (component.style.getSize() / 72F * canvas.getDPI()));

        if (component.style.getSize() <= 0 || font == null) {
            return;
        }

        FontRenderContext fontRenderContext = canvas.getFontRenderContext();
        TextLayout textLayout = new TextLayout(component.text, font, fontRenderContext);
        AffineTransform transform = AffineTransform.getTranslateInstance(x, y);

        if (component.style.getFontName().equals(component.style.getItalicFontName()) && component.italic) {
            // TODO: Shear, but not for symbols.
            // transform.shear(0.5, 0);
        }

        Shape shape = textLayout.getOutline(transform);

        canvas.push(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (component.style.hasOutline()) {
            canvas.setColor(component.style.getOutline().color());
            canvas.setStroke(new BasicStroke(component.style.getOutline().weight() * 2));
            canvas.draw(shape);
        }

        if (component.style.getShadow() != null) {
            canvas.push(component.style.getShadow().dX(), component.style.getShadow().dY());
            canvas.push(component.style.getShadow().color(), Graphics2D::setColor, Graphics2D::getColor);
            canvas.fill(shape);
            canvas.pop(2); // Pop shadow color and the translation
        }

        canvas.setColor(component.style.getColor());
        canvas.fill(shape);

        canvas.pop();
    }

    @Override
    public void write(Canvas canvas, OutputStream out) throws IOException {
        ImageIO.write(canvas.getImage(), "png", out);
    }
}
