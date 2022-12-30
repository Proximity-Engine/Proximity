package dev.hephaestus.proximity.app.api.rendering.elements;

import dev.hephaestus.proximity.app.api.rendering.util.Alignment;
import dev.hephaestus.proximity.app.api.rendering.util.BoundingBox;
import dev.hephaestus.proximity.app.api.rendering.util.BoundingBoxes;
import dev.hephaestus.proximity.app.api.text.TextComponent;
import dev.hephaestus.proximity.app.api.text.TextStyle;
import dev.hephaestus.proximity.app.api.text.Word;
import javafx.beans.property.Property;

import java.awt.geom.Rectangle2D;
import java.util.Collection;

public interface Text extends Element {
    void set(Word word);

    void set(String word);

    void set(Word... words);

    void set(String... words);

    void set(Collection<Word> words);

    void pos(int x, int y);

    void style(TextStyle style);

    void alignment(Alignment alignment);

    Rectangle2D bounds();

    Property<Rectangle2D> boundsProperty();

    BoundingBoxes layout(Text.LayoutConsumer consumer);

    interface LayoutConsumer {
        void render(TextComponent component, BoundingBox bounds, int x, int y);
    }
}
