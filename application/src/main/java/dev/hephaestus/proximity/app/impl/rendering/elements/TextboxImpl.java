package dev.hephaestus.proximity.app.impl.rendering.elements;

import dev.hephaestus.proximity.app.api.rendering.elements.Text;
import dev.hephaestus.proximity.app.api.rendering.elements.Text.LayoutConsumer;
import dev.hephaestus.proximity.app.api.rendering.elements.Textbox;
import dev.hephaestus.proximity.app.api.rendering.util.BoundingBox;
import dev.hephaestus.proximity.app.api.rendering.util.BoundingBoxes;
import dev.hephaestus.proximity.app.api.rendering.util.Padding;
import dev.hephaestus.proximity.app.api.rendering.Document;
import dev.hephaestus.proximity.app.api.rendering.RenderData;
import dev.hephaestus.proximity.app.api.text.TextComponent;
import dev.hephaestus.proximity.app.api.text.Word;
import dev.hephaestus.proximity.app.api.util.Properties;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ListProperty;
import javafx.beans.property.Property;
import javafx.beans.value.ObservableValue;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

public class TextboxImpl<D extends RenderData> extends AbstractTextImpl<D> implements Textbox {
    private final Property<Integer> width = Properties.of("width", Integer.MAX_VALUE), height = Properties.of("height", Integer.MAX_VALUE);
    private final ListProperty<Shape> wraps = Properties.list("wraps");
    private final Property<Padding> padding = Properties.of("padding", new Padding(0, 0, 0, 0));
    private final Property<Float> lineSpacing = Properties.of("lineSpacing", 1F);
    private final Property<Float> paragraphSpacing = Properties.of("paragraphSpacing", 1.25F);
    
    public TextboxImpl(String id, Document<D> document, ParentImpl<D> parent) {
        super(id, document, parent);
        this.bounds.bind(this.bindBounds());
    }

    @Override
    protected ObservableValue<Rectangle2D> bindBounds() {
        return Bindings.createObjectBinding(this::calculateBounds, this.x, this.y, this.text, this.style, this.style.getValue(), this.width, this.height, this.wraps, this.padding, this.lineSpacing, this.paragraphSpacing);
    }

    protected final BoundingBox measure(TextComponent component, AffineTransform transform, boolean strip) {
        Font font = this.document.getTemplate().getFont(
                component.italic ? component.style.getItalicFontName() : component.style.getFontName(), (float) (component.style.getSize() / 72F * this.document.getTemplate().getDPI()));

        if (component.style.getSize() <= 0 || font == null || component.text.isEmpty() || (strip && component.text.isBlank())) {
            return new BoundingBox(transform.getTranslateX(), transform.getTranslateY(), 0, 0, false);
        }

        FontRenderContext fontRenderContext = new FontRenderContext(transform, true, false);
        TextLayout textLayout = new TextLayout(strip ? component.text.stripTrailing() : component.text, font, fontRenderContext);

        Shape shape = textLayout.getOutline(transform);
        Rectangle bounds = shape.getBounds();

        return new BoundingBox(
                bounds.x,
                bounds.y,
                textLayout.getAdvance(),
                textLayout.getAscent(),
                true
        );
    }

    private BoundingBoxes layout(Word word, AffineTransform transform, boolean strip, LayoutConsumer consumer) {
        List<BoundingBox> boxes = new ArrayList<>();

        for (Iterator<TextComponent> iterator = word.iterator(); iterator.hasNext(); ) {
            TextComponent component = iterator.next();

            BoundingBox box = this.measure(component, transform, strip && !iterator.hasNext());

            consumer.render(component, box, (int) transform.getTranslateX(), (int) transform.getTranslateY());

            if (box.width >= 1 && box.height >= 1) {
                boxes.add(box);
            }

            transform.translate(box.width, 0);
        }

        return new BoundingBoxes(boxes);
    }

    @Override
    public BoundingBoxes layout(Text.LayoutConsumer consumer) {
        Padding padding = this.padding.getValue();
        AffineTransform transform = AffineTransform.getTranslateInstance(this.x.getValue() + padding.left(), this.y.getValue() + padding.top());

        Deque<Word> words = new ArrayDeque<>(this.text);

        int dX = 0;
        int dY = 0;

        for (Word word : words) {
            if (transform.getTranslateX() + dX < this.x.getValue() + this.width.getValue()) {
                if (word.length() == 1) {
                    String text = word.iterator().next().text;

                    if (text.equalsIgnoreCase("\n\n") || text.equalsIgnoreCase("\n")) {
                        break;
                    }
                }

                BoundingBoxes bounds = this.layout(word, (AffineTransform) transform.clone(), false, (c, b, x, y) -> {});

                dX += bounds.getMaxX();
                dY = (int) Math.max(dY, this.y.getValue() - bounds.getMinY());
            }
        }

        transform = AffineTransform.getTranslateInstance(this.x.getValue() + padding.left(), this.y.getValue() + padding.top() + dY);

        BoundingBoxes boxes = new BoundingBoxes();

        while (!words.isEmpty()) {
            Word word = words.removeFirst();

            if (word.length() == 1) {
                String text = word.iterator().next().text;

                if (text.equalsIgnoreCase("\n\n")) {
                    transform = AffineTransform.getTranslateInstance(this.x.getValue() + padding.left(), transform.getTranslateY() + (float) (this.style.getValue().getSize() / 72F * this.document.getTemplate().getDPI()) * this.paragraphSpacing.getValue());
                    continue;
                } else if (text.equalsIgnoreCase("\n")) {
                    transform = AffineTransform.getTranslateInstance(this.x.getValue() + padding.left(), transform.getTranslateY() + (float) (this.style.getValue().getSize() / 72F * this.document.getTemplate().getDPI()) * this.lineSpacing.getValue());
                    continue;
                }
            }

            BoundingBoxes bounds = this.layout(word, (AffineTransform) transform.clone(), false, (c, b, x, y) -> {});

            if (bounds.getMaxX() > this.x.getValue() + this.width.getValue() - padding.right()) {
                bounds = this.layout(word, (AffineTransform) transform.clone(), true, (c, b, x, y) -> {});
            }

            bounds.forEach(boxes::add);

            /*if (bounds.getMaxY() > this.y.getValue() + this.height.getValue() - padding.bottom()) {
                // We've extended past the bottom of the text box. There's nowhere else to go!
                this.isOutOfBounds = true;
                return new BoundingBoxes();
            } else */if (bounds.getMaxX() > this.x.getValue() + this.width.getValue() - padding.right() || intersectsWraps(bounds)) {
                // Advance to the next line
                words.addFirst(word);

                transform = AffineTransform.getTranslateInstance(this.x.getValue() + padding.left(), transform.getTranslateY() + (float) (this.style.getValue().getSize() / 72F * this.document.getTemplate().getDPI()) * this.lineSpacing.getValue());
            } else {
                // Add the word and move on
                this.layout(word, (AffineTransform) transform.clone(), false, consumer);
                transform.translate(bounds.getWidth(), 0);
            }
        }

        return boxes;
    }

    private boolean intersectsWraps(BoundingBoxes boxes) {
        for (Shape shape : this.wraps) {
            for (BoundingBox box : boxes) {
                if (shape.intersects(box)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    protected void getAttributes(Consumer<Observable> attributes) {
        super.getAttributes(attributes);
        attributes.accept(this.width);
        attributes.accept(this.height);
        attributes.accept(this.wraps);
        attributes.accept(this.padding);
        attributes.accept(this.lineSpacing);
        attributes.accept(this.paragraphSpacing);
    }

    @Override
    public void pos(int x, int y, int width, int height) {
        super.pos(x, y);
        this.width.setValue(width);
        this.height.setValue(height);
    }

    @Override
    public void wrap(Shape wrap) {
        this.wraps.add(wrap);
    }

    @Override
    public void padding(int top, int right, int bottom, int left) {
        this.padding.setValue(new Padding(top, right, bottom, left));
    }

    @Override
    public void lineSpacing(float spacing) {
        this.lineSpacing.setValue(spacing);
    }

    @Override
    public void paragraphSpacing(float space) {
        this.paragraphSpacing.setValue(space);
    }
}
