package dev.hephaestus.proximity.app.api.rendering.elements;

import dev.hephaestus.proximity.app.api.rendering.util.BoundingBoxes;
import dev.hephaestus.proximity.app.api.text.TextStyle;
import dev.hephaestus.proximity.app.api.text.Word;
import javafx.beans.property.Property;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.Collection;

public interface Textbox extends Element {
    void set(Word word);

    void set(String word);

    void set(Word... words);

    void set(String... words);

    void set(Collection<Word> words);

    void pos(int x, int y, int width, int height);

    void style(TextStyle style);

    void wrap(Shape wrap);

    void padding(int top, int right, int bottom, int left);

    void lineSpacing(float spacing);

    void paragraphSpacing(float space);

    Rectangle2D bounds();

    Property<Rectangle2D> boundsProperty();

    BoundingBoxes layout(Text.LayoutConsumer consumer);
}
