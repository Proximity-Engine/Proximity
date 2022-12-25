package dev.hephaestus.proximity.app.impl.rendering;


import dev.hephaestus.proximity.app.api.Parent;
import dev.hephaestus.proximity.app.api.Template;
import dev.hephaestus.proximity.app.api.rendering.Document;
import dev.hephaestus.proximity.app.api.rendering.elements.Child;
import dev.hephaestus.proximity.app.api.rendering.elements.Element;
import dev.hephaestus.proximity.app.api.rendering.elements.Group;
import dev.hephaestus.proximity.app.api.rendering.elements.Image;
import dev.hephaestus.proximity.app.api.rendering.elements.Selector;
import dev.hephaestus.proximity.app.api.rendering.elements.Text;
import dev.hephaestus.proximity.app.api.rendering.elements.TextBox;
import dev.hephaestus.proximity.app.api.rendering.util.BoundingBox;
import dev.hephaestus.proximity.app.api.rendering.util.BoundingBoxes;
import dev.hephaestus.proximity.app.api.rendering.util.Rect;
import dev.hephaestus.proximity.app.api.text.TextComponent;
import dev.hephaestus.proximity.app.api.text.TextStyle;
import dev.hephaestus.proximity.app.impl.rendering.properties.ImagePropertyImpl;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class PreviewImageRenderer {
    private final int width, height;
    private final Cache cache = new Cache(15);

    public PreviewImageRenderer(int width, int height) {
        this.width = width;
        this.height = height;
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

    protected void render(Image<?> image, Canvas canvas) throws IOException {
        int n = (int) Math.round(1D / canvas.rW);

        BoundingBox dimensions = image.getBounds().iterator().next();
        URL url = this.downscale(image, n);
        ImageView imageView = new ImageView(this.cache.get(url));

        imageView.setTranslateX(dimensions.getX() * canvas.rW);
        imageView.setTranslateY(dimensions.getY() * canvas.rH);
        imageView.setFitWidth(dimensions.getWidth() * canvas.rW);
        imageView.setFitHeight(dimensions.getHeight() * canvas.rH);

        canvas.nodes.add(imageView);

        StackPane.setAlignment(imageView, Pos.TOP_LEFT);
    }

    private void render(Group<?> group, Canvas canvas) throws IOException {
        for (Element<?> node : group) {
            if (node instanceof Child<?> child && child.visibility().get() || node instanceof Parent<?> parent && parent.isVisible()) {
                this.render(node, canvas);
            }
        }
    }

    private void render(Selector<?> selector, Canvas canvas) throws IOException {
        var selected = selector.getSelected();

        if (selected.isPresent()) {
            this.render(selected.get(), canvas);
        }
    }

    private void render(TextBox<?> text, Canvas canvas) {
        text.layout(canvas::drawText);
    }

    private void render(Text<?> text, Canvas canvas) {
        text.layout(canvas::drawText);
    }

    private void render(Element<?> node, Canvas canvas) throws IOException {
        if (node instanceof Child<?> child && child.visibility().get() || node instanceof Parent<?> parent && parent.isVisible()) {
            if (node instanceof Group<?> group) {
                this.render(group, canvas);
            } else if (node instanceof Selector<?> selector) {
                this.render(selector, canvas);
            } else if (node instanceof Image<?> image) {
                this.render(image, canvas);
            } else if (node instanceof TextBox<?> textBox) {
                this.render(textBox, canvas);
            } else if (node instanceof Text<?> text) {
                this.render(text, canvas);
            } else {
                throw new UnsupportedOperationException(String.format("Unexpected node class: %s", node.getClass()));
            }
        }
    }

    public void render(Document<?> document, ObservableList<Node> nodes) throws IOException {
        float rW = ((float) this.width) / document.getTemplate().getWidth();
        float rH = ((float) this.height) / document.getTemplate().getHeight();

        for (Element<?> node : document) {
            this.render(node, new Canvas(document, rW, rH, nodes));
        }
    }

    private static class Cache {
        private final URL[] urls;
        private final javafx.scene.image.Image[] images;

        private int p = 0;

        private Cache(int size) {
            this.urls = new URL[size];
            this.images = new javafx.scene.image.Image[size];
        }

        private javafx.scene.image.Image get(URL url) throws IOException {
            for (int i = 0, cacheLength = this.urls.length; i < cacheLength; ++i) {
                if (url.equals(this.urls[i])) {
                    return this.images[i];
                }
            }

            javafx.scene.image.Image image = new javafx.scene.image.Image(url.openStream());

            this.urls[p] = url;
            this.images[p] = image;

            this.p = (this.p + 1) % this.urls.length;

            return image;
        }
    }

    private record Canvas(Document<?> document, float rW, float rH, ObservableList<Node> nodes) {
        public void drawText(TextComponent component, BoundingBox bounds, int x, int y) {
            Font font = document.getTemplate().getFXFont(
                    component.italic ? component.style.getItalicFontName() : component.style.getFontName(), (float) (component.style.getSize() / 72F * document.getTemplate().getDPI() * rW));

            TextStyle.Shadow shadow = component.style.getShadow();

            if (shadow != null) {
                Color color = shadow.color();
                String hex = String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
                Label label = new Label(component.text);

                label.setStyle("-fx-text-fill: " + hex);

                label.setFont(font);

                label.setTranslateX((x + shadow.dX()) * rW);
                label.setTranslateY((y + shadow.dY()) * rH);

                StackPane.setAlignment(label, Pos.BASELINE_LEFT);

                nodes.add(label);
            }

            Color color = component.style.getColor();
            Label label = new Label(component.text);

            if (color != null) {
                String hex = String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
                label.setStyle("-fx-text-fill: " + hex);
            }

            label.setFont(font);

            label.setTranslateX(x * rW);
            label.setTranslateY(y * rH);

            StackPane.setAlignment(label, Pos.BASELINE_LEFT);

            nodes.add(label);
        }
    }

//    private static final class TextRenderer implements Text.Consumer {
//        private final Document<?> document;
//        private final dev.hephaestus.proximity.app.api.rendering.Canvas canvas;
//
//        private TextRenderer(Document<?> document, int width, int height, int dpi) {
//            this.document = document;
//            this.canvas = new dev.hephaestus.proximity.app.api.rendering.Canvas(width, height, dpi);
//        }
//
//        @Override
//        public void render(TextComponent component, BoundingBox bounds, int x, int y) {
//            Font font = document.getTemplate().getFont(
//                    component.italic ? component.style.getItalicFontName() : component.style.getFontName(), (float) (component.style.getSize() / 72F * canvas.getDPI()));
//
//            if (component.style.getSize() <= 0 || font == null) {
//                return;
//            }
//
//            FontRenderContext fontRenderContext = canvas.getFontRenderContext();
//            TextLayout textLayout = new TextLayout(component.text, font, fontRenderContext);
//            AffineTransform transform = AffineTransform.getTranslateInstance(x, y);
//
//            if (component.style.getFontName().equals(component.style.getItalicFontName()) && component.italic) {
//                // TODO: Shear, but not for symbols.
//                // transform.shear(0.5, 0);
//            }
//
//            Shape shape = textLayout.getOutline(transform);
//
//            canvas.push(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//
//            if (component.style.hasOutline()) {
//                canvas.setColor(component.style.getOutline().color());
//                canvas.setStroke(new BasicStroke(component.style.getOutline().weight() * 2));
//                canvas.draw(shape);
//            }
//
//            if (component.style.getShadow() != null) {
//                canvas.push(component.style.getShadow().dX(), component.style.getShadow().dY());
//                canvas.push(component.style.getShadow().color(), Graphics2D::setColor, Graphics2D::getColor);
//                canvas.fill(shape);
//                canvas.pop(2); // Pop shadow color and the translation
//            }
//
//            canvas.setColor(component.style.getColor());
//            canvas.fill(shape);
//
//            canvas.pop();
//        }
//    }
}
