package dev.hephaestus.proximity.app.api.rendering.elements;

import dev.hephaestus.proximity.app.api.RenderJob;
import dev.hephaestus.proximity.app.api.rendering.properties.Property;
import dev.hephaestus.proximity.app.api.rendering.properties.ThrowingProperty;
import dev.hephaestus.proximity.app.api.rendering.properties.VisibilityProperty;
import dev.hephaestus.proximity.app.api.rendering.util.ImagePosition;
import dev.hephaestus.proximity.app.api.rendering.util.Rect;
import org.jetbrains.annotations.ApiStatus;

import java.io.IOException;
import java.io.InputStream;

@ApiStatus.NonExtendable
public interface Image<D extends RenderJob> extends Child<D> {
    ThrowingProperty<D, InputStream, Image<D>, IOException> src();
    Property<D, ImagePosition, Image<D>> position();
    VisibilityProperty<D, Image<D>> visibility();

    String getFormat();

    Rect getSourceImageDimensions();
}
