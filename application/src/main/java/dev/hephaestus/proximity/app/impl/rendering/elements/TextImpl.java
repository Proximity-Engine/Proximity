package dev.hephaestus.proximity.app.impl.rendering.elements;

import dev.hephaestus.proximity.app.api.rendering.elements.Text;
import dev.hephaestus.proximity.app.api.rendering.util.Alignment;
import dev.hephaestus.proximity.app.api.rendering.util.BoundingBox;
import dev.hephaestus.proximity.app.api.rendering.util.BoundingBoxes;
import dev.hephaestus.proximity.app.api.rendering.Document;
import dev.hephaestus.proximity.app.api.rendering.RenderData;
import dev.hephaestus.proximity.app.api.text.TextComponent;
import dev.hephaestus.proximity.app.api.text.Word;
import dev.hephaestus.proximity.app.api.util.Properties;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.value.ObservableValue;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.function.Consumer;

public class TextImpl<D extends RenderData> extends AbstractTextImpl<D> implements Text {
    private final Property<Alignment> alignment = Properties.of("alignment", Alignment.START);

    public TextImpl(String id, Document<D> document, ParentImpl<D> parent) {
        super(id, document, parent);
        this.bounds.bind(this.bindBounds());
    }

    @Override
    protected ObservableValue<Rectangle2D> bindBounds() {
        return Bindings.createObjectBinding(this::calculateBounds, this.x, this.y, this.text, this.style, this.style.getValue(), this.alignment);
    }

    protected final BoundingBox measure(TextComponent component, AffineTransform transform) {
        float size = component.style.getSize() == null ? this.style.getValue().getSize().floatValue() : component.style.getSize().floatValue();

        java.awt.Font font = this.document.getTemplate().getFont(
                component.italic ? component.style.getItalicFontName() : component.style.getFontName(), size / 72F * this.document.getTemplate().getDPI());

        if (size <= 0 || font == null) {
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
                textLayout.getAscent(),
                true
        );
    }

    @Override
    public BoundingBoxes layout(Text.LayoutConsumer consumer) {
        AffineTransform transform = AffineTransform.getTranslateInstance(this.x.getValue(), this.y.getValue());

        for (Word word : this.text) {
            for (TextComponent component : word) {
//                component.style = component.style.derive(this.style.getValue());
                BoundingBox bounds = this.measure(component, transform);

                transform.translate(bounds.width, 0);
            }
        }

        double dX = transform.getTranslateX();

        transform.setTransform(AffineTransform.getTranslateInstance(this.x.getValue(), this.y.getValue()));

        switch (this.alignment.getValue()) {
            case CENTER -> transform.translate(-((dX - this.x.getValue()) / 2), 0);
            case END -> transform.translate(-(dX - this.x.getValue()), 0);
        }

        BoundingBoxes boxes = new BoundingBoxes();

        for (Word word : this.text) {
            for (TextComponent component : word) {
                component.style = component.style.derive(this.style.getValue());
                BoundingBox bounds = this.measure(component, transform);

                boxes.add(bounds);
                consumer.render(component, bounds, (int) transform.getTranslateX(), (int) transform.getTranslateY());

                transform.translate(bounds.width, 0);
            }
        }

        return boxes;
    }

    @Override
    protected void getAttributes(Consumer<Observable> attributes) {
        super.getAttributes(attributes);
        attributes.accept(this.alignment);
    }

    @Override
    public void alignment(Alignment alignment) {
        this.alignment.setValue(alignment);
    }
}
