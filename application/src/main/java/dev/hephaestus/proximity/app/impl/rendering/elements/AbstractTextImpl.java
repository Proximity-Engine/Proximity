package dev.hephaestus.proximity.app.impl.rendering.elements;

import dev.hephaestus.proximity.app.api.rendering.Document;
import dev.hephaestus.proximity.app.api.rendering.RenderData;
import dev.hephaestus.proximity.app.api.rendering.elements.Text;
import dev.hephaestus.proximity.app.api.rendering.util.BoundingBox;
import dev.hephaestus.proximity.app.api.rendering.util.BoundingBoxes;
import dev.hephaestus.proximity.app.api.text.TextComponent;
import dev.hephaestus.proximity.app.api.text.TextStyle;
import dev.hephaestus.proximity.app.api.text.Word;
import dev.hephaestus.proximity.app.api.util.Properties;
import javafx.beans.Observable;
import javafx.beans.property.ListProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public abstract class AbstractTextImpl<D extends RenderData> extends ElementImpl<D> {
    protected final Property<Integer> x = Properties.of("x", 0), y = Properties.of("y", 0);
    protected final ListProperty<Word> text = Properties.list("text");
    protected final Property<TextStyle> style = Properties.of("style", new TextStyle());
    protected final Property<Rectangle2D> bounds = new SimpleObjectProperty<>();

    public AbstractTextImpl(String id, Document<D> document, ParentImpl<D> parent) {
        super(id, document, parent);
    }

    protected abstract ObservableValue<Rectangle2D> bindBounds();

    protected abstract BoundingBoxes layout(Text.LayoutConsumer consumer);

    @Override
    protected void getAttributes(Consumer<Observable> attributes) {
        attributes.accept(this.x);
        attributes.accept(this.y);
        attributes.accept(this.text);
        attributes.accept(this.style.getValue());
    }

    public final Rectangle2D bounds() {
        return this.bounds.getValue();
    }

    public final Property<Rectangle2D> boundsProperty() {
        return this.bounds;
    }

    protected final Rectangle2D calculateBounds() {
        BoundingBoxes boxes = new BoundingBoxes();

        this.layout(((component, bounds, x1, y1) -> boxes.add(bounds)));

        return boxes.getBounds() == null
                ? new Rectangle2D.Double(this.x.getValue(), this.y.getValue(), 0, 0)
                : boxes.getBounds();
    }

    @Override
    public final Node render() {
        StackPane pane = new StackPane();

        pane.setAlignment(Pos.TOP_LEFT);
        Property<Rectangle2D> bounds = new SimpleObjectProperty<>();

        pane.setUserData(bounds);

        this.render(pane, bounds);

        this.getAttributes(attribute -> attribute.addListener(o -> this.render(pane, bounds)));

        return pane;
    }

    private void render(StackPane pane, Property<Rectangle2D> bounds) {
        List<Node> nodes = new ArrayList<>();

        Rectangle2D result = this.layout(((component, b, x, y) -> this.drawText(
                nodes::add, component, b, x, y
        ))).getBounds();

        pane.getChildren().setAll(nodes);
        bounds.setValue(result);
    }

    public final void set(Word word) {
        this.text.setAll(word);
    }

    public final void set(String word) {
        this.text.setAll(new Word(new TextComponent(this.style.getValue(), word)));
    }

    public void set(Word... words) {
        this.text.setAll(words);
    }

    public void set(String... words) {
        Word[] ws = new Word[words.length];

        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            ws[i] = new Word(new TextComponent(this.style.getValue(), word));
        }

        this.set(ws);
    }

    public void set(Collection<Word> words) {
        this.text.setAll(words);
    }

    public void pos(int x, int y) {
        this.x.setValue(x);
        this.y.setValue(y);
    }

    public final void style(TextStyle style) {
        this.style.setValue(style);
        this.bounds.bind(this.bindBounds());
    }

    protected final void drawText(Consumer<Node> nodes, TextComponent component, BoundingBox bounds, int x, int y) {
        Font font = document.getTemplate().getFXFont(
                component.italic ? component.style.getItalicFontName() : component.style.getFontName(), (float) (component.style.getSize() / 72F * document.getTemplate().getDPI()));

        TextStyle.Shadow shadow = component.style.getShadow();

        if (shadow != null) {
            Color color = shadow.color();
            javafx.scene.text.Text text = new javafx.scene.text.Text(component.text);

            text.setFill(javafx.scene.paint.Color.color(color.getRed() / 255D, color.getGreen() / 255D, color.getBlue() / 255D));

            text.setFont(font);

            text.setTranslateX((x + shadow.dX()));
            text.setTranslateY((y + shadow.dY()));

            StackPane.setAlignment(text, Pos.BASELINE_LEFT);

            nodes.accept(text);
        }

        Color color = component.style.getColor();
        javafx.scene.text.Text text = new javafx.scene.text.Text(component.text);

        if (color != null) {
            text.setFill(javafx.scene.paint.Color.color(color.getRed() / 255D, color.getGreen() / 255D, color.getBlue() / 255D));
        }

        text.setFont(font);

        text.setTranslateX(x);
        text.setTranslateY(y);

        StackPane.setAlignment(text, Pos.BASELINE_LEFT);

        nodes.accept(text);
    }
}
