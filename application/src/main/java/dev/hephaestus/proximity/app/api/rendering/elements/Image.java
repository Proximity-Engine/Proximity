package dev.hephaestus.proximity.app.api.rendering.elements;

import dev.hephaestus.proximity.app.api.rendering.util.Alignment;
import dev.hephaestus.proximity.app.api.rendering.util.ImageSource;
import javafx.beans.property.Property;

import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.net.URL;

public interface Image extends Element {
    void source(URL url);

    /**
     * @param src a path to a resource in this templates package
     */
    void source(String src);

    void pos(int x, int y);

    void fill(int x, int y, int width, int height);

    void cover(int x, int y, int width, int height, Alignment horizontalAlignment, Alignment verticalAlignment);

    Rectangle2D bounds();

    Property<Rectangle2D> boundsProperty();

    ImageSource source() throws IOException;
}
