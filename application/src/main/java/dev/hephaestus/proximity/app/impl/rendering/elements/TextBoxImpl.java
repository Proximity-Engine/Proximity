package dev.hephaestus.proximity.app.impl.rendering.elements;

import dev.hephaestus.proximity.app.api.RenderJob;
import dev.hephaestus.proximity.app.api.rendering.elements.Text;
import dev.hephaestus.proximity.app.api.rendering.elements.TextBox;
import dev.hephaestus.proximity.app.api.rendering.properties.ListProperty;
import dev.hephaestus.proximity.app.api.rendering.properties.Property;
import dev.hephaestus.proximity.app.api.rendering.properties.TextProperty;
import dev.hephaestus.proximity.app.api.rendering.util.BoundingBox;
import dev.hephaestus.proximity.app.api.rendering.util.BoundingBoxes;
import dev.hephaestus.proximity.app.api.rendering.util.Padding;
import dev.hephaestus.proximity.app.api.text.TextComponent;
import dev.hephaestus.proximity.app.api.text.TextStyle;
import dev.hephaestus.proximity.app.api.text.Word;
import dev.hephaestus.proximity.app.impl.rendering.DocumentImpl;
import dev.hephaestus.proximity.app.impl.rendering.properties.BasicProperty;
import dev.hephaestus.proximity.app.impl.rendering.properties.ListPropertyImpl;
import dev.hephaestus.proximity.app.impl.rendering.properties.TextPropertyImpl;

import java.awt.Font;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.util.*;

public class TextBoxImpl<D extends RenderJob> extends ElementImpl<D> implements TextBox<D> {
    private final VisibilityProperty<TextBox<D>> visibility;
    private final BasicProperty<D, Integer, TextBox<D>> x, y, width, height;
    private final BasicProperty<D, TextStyle, TextBox<D>> style;
    private final BasicProperty<D, Float, TextBox<D>> lineSpacing;
    private final BasicProperty<D, Float, TextBox<D>> spaceAfterParagraph;
    private final BasicProperty<D, Padding, TextBox<D>> padding;
    private final TextProperty<D, TextBox<D>> text;
    private final ListPropertyImpl<D, Shape, TextBox<D>> wraps;

    private boolean isOutOfBounds;

    public TextBoxImpl(DocumentImpl<D> document, String id, ElementImpl<D> parent) {
        super(document, id, parent);

        D data = document.getData();

        this.visibility = new VisibilityProperty<TextBox<D>>(this, data);
        this.x = new BasicProperty<>(this, data, 0);
        this.y = new BasicProperty<>(this, data, 0);
        this.width = new BasicProperty<>(this, data);
        this.height = new BasicProperty<>(this, data);
        this.style = new BasicProperty<>(this, data);
        this.padding = new BasicProperty<>(this, data, new Padding(0, 0, 0, 0));
        this.lineSpacing = new BasicProperty<>(this, data, 1F);
        this.spaceAfterParagraph = new BasicProperty<>(this, data, 1.25F);
        this.text = new TextPropertyImpl<>(this, data);
        this.wraps = new ListPropertyImpl<>(this, data);
    }

    protected final BoundingBox measure(TextComponent component, AffineTransform transform, boolean strip) {
        Font font = this.getDocument().getTemplate().getFont(
                component.italic ? component.style.getItalicFontName() : component.style.getFontName(), (float) (component.style.getSize() / 72F * getDocument().getTemplate().getDPI()));

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
                bounds.height,
                true
        );
    }

    private BoundingBoxes layout(Word word, AffineTransform transform, boolean strip, Text.Consumer consumer) {
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
    public BoundingBoxes getBounds() {
        List<BoundingBox> boxes = new ArrayList<>(this.text.wordCount());

        this.layout((component, bounds, x, y) -> {
            if (bounds.width >= 1 && bounds.height >= 1) {
                boxes.add(bounds);
            }
        });

        return new BoundingBoxes(boxes);
    }

    @Override
    public VisibilityProperty<TextBox<D>> visibility() {
        return this.visibility;
    }

    @Override
    public Property<D, Integer, TextBox<D>> x() {
        return this.x;
    }

    @Override
    public Property<D, Integer, TextBox<D>> y() {
        return this.y;
    }

    @Override
    public Property<D, Integer, TextBox<D>> width() {
        return this.width;
    }

    @Override
    public Property<D, Integer, TextBox<D>> height() {
        return this.height;
    }

    @Override
    public Property<D, TextStyle, TextBox<D>> style() {
        return this.style;
    }

    @Override
    public TextProperty<D, TextBox<D>> text() {
        return this.text;
    }

    @Override
    public ListProperty<D, Shape, TextBox<D>> wraps() {
        return this.wraps;
    }

    @Override
    public Property<D, Padding, TextBox<D>> padding() {
        return this.padding;
    }

    @Override
    public Property<D, Float, TextBox<D>> lineSpacing() {
        return this.lineSpacing;
    }

    @Override
    public Property<D, Float, TextBox<D>> spaceAfterParagraph() {
        return this.spaceAfterParagraph;
    }

    @Override
    public boolean isOutOfBounds() {
        this.getBounds();

        return this.isOutOfBounds;
    }

    @Override
    public void layout(Text.Consumer consumer) {
        Padding padding = this.padding.get();
        AffineTransform transform = AffineTransform.getTranslateInstance(this.x.get() + padding.left(), this.y.get() + padding.top());

        Deque<Word> words = new ArrayDeque<>();

        this.text.forEach(words::add);

        int dX = 0;
        int dY = 0;

        for (Word word : words) {
            if (transform.getTranslateX() + dX < this.x.get() + this.width.get()) {
                if (word.length() == 1) {
                    String text = word.iterator().next().text;

                    if (text.equalsIgnoreCase("\n\n") || text.equalsIgnoreCase("\n")) {
                        break;
                    }
                }

                BoundingBoxes bounds = this.layout(word, (AffineTransform) transform.clone(), false, (c, b, x, y) -> {});

                dX += bounds.getMaxX();
                dY = (int) Math.max(dY, this.y.get() - bounds.getMinY());
            }
        }

        transform = AffineTransform.getTranslateInstance(this.x.get() + padding.left(), this.y.get() + padding.top() + dY);

        while (!words.isEmpty()) {
            Word word = words.removeFirst();

            if (word.length() == 1) {
                String text = word.iterator().next().text;

                if (text.equalsIgnoreCase("\n\n")) {
                    transform = AffineTransform.getTranslateInstance(this.x.get() + padding.left(), transform.getTranslateY() + (float) (this.style.get().getSize() / 72F * getDocument().getTemplate().getDPI()) * this.spaceAfterParagraph.get());
                    continue;
                } else if (text.equalsIgnoreCase("\n")) {
                    transform = AffineTransform.getTranslateInstance(this.x.get() + padding.left(), transform.getTranslateY() + (float) (this.style.get().getSize() / 72F * getDocument().getTemplate().getDPI()) * this.lineSpacing.get());
                    continue;
                }
            }

            BoundingBoxes bounds = this.layout(word, (AffineTransform) transform.clone(), false, (c, b, x, y) -> {});

            if (bounds.getMaxX() > this.x.get() + this.width.get() - padding.right()) {
                bounds = this.layout(word, (AffineTransform) transform.clone(), true, (c, b, x, y) -> {});
            }

            if (bounds.getMaxY() > this.y.get() + this.height.get() - padding.bottom()) {
                // We've extended past the bottom of the text box. There's nowhere else to go!
                this.isOutOfBounds = true;
                return;
            } else if (bounds.getMaxX() > this.x.get() + this.width.get() - padding.right() || intersectsWraps(bounds)) {
                // Advance to the next line
                words.addFirst(word);

                transform = AffineTransform.getTranslateInstance(this.x.get() + padding.left(), transform.getTranslateY() + (float) (this.style.get().getSize() / 72F * getDocument().getTemplate().getDPI()) * this.lineSpacing.get());
            } else {
                // Add the word and move on
                this.layout(word, (AffineTransform) transform.clone(), false, consumer);
                transform.translate(bounds.getWidth(), 0);
            }
        }
    }

    private boolean intersectsWraps(BoundingBoxes boxes) {
        for (Shape shape : this.wraps.get()) {
            for (BoundingBox box : boxes) {
                if (shape.intersects(box)) {
                    return true;
                }
            }
        }

        return false;
    }

    // TODO: Wrap smartly around wraps instead of just breaking the line.
    private enum Direction {
        UP, DOWN, LEFT, RIGHT;
    }
}
