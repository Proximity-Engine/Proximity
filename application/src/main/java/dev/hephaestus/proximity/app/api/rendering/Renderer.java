package dev.hephaestus.proximity.app.api.rendering;


import dev.hephaestus.proximity.app.api.rendering.elements.Element;
import dev.hephaestus.proximity.app.api.rendering.elements.Group;
import dev.hephaestus.proximity.app.api.rendering.elements.Image;
import dev.hephaestus.proximity.app.api.rendering.elements.Selector;
import dev.hephaestus.proximity.app.api.rendering.elements.Text;
import dev.hephaestus.proximity.app.api.rendering.elements.Textbox;
import dev.hephaestus.proximity.app.api.rendering.util.BoundingBox;
import dev.hephaestus.proximity.app.api.text.TextComponent;
import dev.hephaestus.proximity.app.impl.rendering.elements.ElementImpl;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;

public abstract class Renderer<CANVAS> {
    public abstract CANVAS createCanvas(int width, int height, int dpi);
    protected abstract void render(Image image, CANVAS canvas) throws IOException;
    protected abstract void render(Document<?> document, CANVAS canvas, TextComponent component, int x, int y, BoundingBox bounds);
    public abstract void write(CANVAS canvas, OutputStream out) throws IOException;

    protected final void render(Element node, CANVAS canvas) throws IOException {
        if (node.isVisible()) {
            if (node instanceof Group group) {
                this.render(group, canvas);
            } else if (node instanceof Selector selector) {
                this.render(selector, canvas);
            } else if (node instanceof Image image) {
                this.render(image, canvas);
            } else if (node instanceof Textbox textBox) {
                this.render(textBox, canvas);
            } else if (node instanceof Text text) {
                this.render(text, canvas);
            } else {
                throw new UnsupportedOperationException(String.format("Unexpected node class: %s", node.getClass()));
            }
        }
    }

    private void render(Group group, CANVAS canvas) throws IOException {
        for (Element node : group) {
            if (node.isVisible()) {
                this.render(node, canvas);
            }
        }
    }

    private void render(Selector selector, CANVAS canvas) throws IOException {
        Optional<? extends Element> optional = selector.getSelected();

        if (optional.isPresent()) {
            this.render(optional.get(), canvas);
        }
    }

    private void render(Textbox text, CANVAS canvas) {
        text.layout(((component, bounds, x, y) -> this.render(((ElementImpl<?>) text).document, canvas, component, x, y, bounds)));
    }

    private void render(Text text, CANVAS canvas) {
        text.layout(((component, bounds, x, y) -> this.render(((ElementImpl<?>) text).document, canvas, component, x, y, bounds)));
    }

    public final void render(Document<?> document, CANVAS canvas) throws IOException {
        for (Element node : document.getElements()) {
            this.render(node, canvas);
        }
    }
}
