package dev.hephaestus.proximity.app.api.rendering.properties;

import dev.hephaestus.proximity.app.api.RenderJob;
import dev.hephaestus.proximity.app.api.rendering.elements.Image;
import dev.hephaestus.proximity.app.api.rendering.util.ThrowingFunction;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public interface ImageProperty<D extends RenderJob> {
    InputStream get() throws IOException;

    /**
     * Allows you to dynamically derive an image resource from data
     */
    Image<D> set(ThrowingFunction<D, URL, IOException> getter);

    /**
     * Sets this properties value to the template resource for the passed string
     */
    Image<D> set(String src);

    Type getType();

    enum Type {
        UNSET, TEMPLATE_RESOURCE, DYNAMIC
    }
}
