package dev.hephaestus.proximity.app.impl.rendering.elements;

import dev.hephaestus.proximity.app.api.RenderJob;
import dev.hephaestus.proximity.app.api.rendering.elements.Text;
import dev.hephaestus.proximity.app.api.rendering.properties.Property;
import dev.hephaestus.proximity.app.api.rendering.properties.TextProperty;
import dev.hephaestus.proximity.app.api.rendering.util.Alignment;
import dev.hephaestus.proximity.app.api.rendering.util.BoundingBox;
import dev.hephaestus.proximity.app.api.rendering.util.BoundingBoxes;
import dev.hephaestus.proximity.app.api.text.TextComponent;
import dev.hephaestus.proximity.app.api.text.TextStyle;
import dev.hephaestus.proximity.app.api.text.Word;
import dev.hephaestus.proximity.app.impl.rendering.DocumentImpl;
import dev.hephaestus.proximity.app.impl.rendering.properties.BasicProperty;
import dev.hephaestus.proximity.app.impl.rendering.properties.TextPropertyImpl;
import org.jetbrains.annotations.NotNull;

import java.awt.Font;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TextImpl<D extends RenderJob<?>> extends ElementImpl<D> implements Text<D> {
    private final VisibilityProperty<Text<D>> visibility;
    protected final BasicProperty<D, Integer, Text<D>> x, y;
    protected final BasicProperty<D, TextStyle, Text<D>> style;
    protected final BasicProperty<D, Alignment, Text<D>> alignment;
    protected final TextProperty<D, Text<D>> text;

    public TextImpl(DocumentImpl<D> document, String id, ElementImpl<D> parent) {
        super(document, id, parent);

        D data = document.getData();

        this.visibility = new VisibilityProperty<Text<D>>(this, data);
        this.x = new BasicProperty<>(this, data, 0);
        this.y = new BasicProperty<>(this, data, 0);
        this.style = new BasicProperty<>(this, data);
        this.alignment = new BasicProperty<>(this, data, Alignment.START);
        this.text = new TextPropertyImpl<>(this, data);
    }

    @Override
    public Property<D, Integer, Text<D>> x() {
        return this.x;
    }

    @Override
    public Property<D, Integer, Text<D>> y() {
        return this.y;
    }

    @Override
    public Property<D, TextStyle, Text<D>> style() {
        return this.style;
    }

    @Override
    public Property<D, Alignment, Text<D>> alignment() {
        return this.alignment;
    }

    @Override
    public TextProperty<D, Text<D>> text() {
        return this.text;
    }

    protected final BoundingBox measure(TextComponent component, AffineTransform transform) {
        Font font = this.getDocument().getTemplate().getFont(
                component.italic ? component.style.getItalicFontName() : component.style.getFontName(), (float) (component.style.getSize() / 72F * getDocument().getTemplate().getDPI()));

        if (component.style.getSize() <= 0 || font == null) {
            return new BoundingBox(transform.getTranslateX(), transform.getTranslateY(), 0, 0, false);
        }

        FontRenderContext fontRenderContext = new FontRenderContext(transform, true, false);
        TextLayout textLayout = new TextLayout(component.text, font, fontRenderContext);

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

    @Override
    public BoundingBoxes getBounds() {
        List<BoundingBox> boxes = new ArrayList<>(this.text.wordCount());

        this.layout((component, bounds, x, y) -> {
            boxes.add(bounds);
        });

        return new BoundingBoxes(boxes);
    }

    @Override
    public VisibilityProperty<Text<D>> visibility() {
        return this.visibility;
    }

    @Override
    public void layout(Consumer consumer) {
        AffineTransform transform = AffineTransform.getTranslateInstance(this.x.get(), this.y.get());

        for (Word word : this) {
            for (TextComponent component : word) {
                component.style = component.style.derive(this.style.get());
                BoundingBox bounds = this.measure(component, transform);

                transform.translate(bounds.width, 0);
            }
        }

        double dX = transform.getTranslateX();

        transform.setTransform(AffineTransform.getTranslateInstance(this.x.get(), this.y.get()));

        switch (this.alignment.get()) {
            case CENTER -> transform.translate(-((dX - this.x.get()) / 2), 0);
            case END -> transform.translate(-(dX - this.x.get()), 0);
        }

        for (Word word : this) {
            for (TextComponent component : word) {
                component.style = component.style.derive(this.style.get());
                BoundingBox bounds = this.measure(component, transform);

                consumer.render(component, bounds, (int) transform.getTranslateX(), (int) transform.getTranslateY());

                transform.translate(bounds.width, 0);
            }
        }
    }

    @NotNull
    @Override
    public Iterator<Word> iterator() {
        return new Iterator<>() {
            private final Iterator<Word> itr = TextImpl.this.text.iterator();

            @Override
            public boolean hasNext() {
                return this.itr.hasNext();
            }

            @Override
            public Word next() {
                Word word = this.itr.next();

                for (TextComponent component : word) {
                    component.style = component.style.derive(TextImpl.this.style.get());
                }

                return word;
            }
        };
    }
}
