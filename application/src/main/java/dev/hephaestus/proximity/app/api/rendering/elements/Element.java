package dev.hephaestus.proximity.app.api.rendering.elements;

import dev.hephaestus.proximity.app.api.RenderJob;
import dev.hephaestus.proximity.app.api.rendering.Document;
import dev.hephaestus.proximity.app.api.rendering.util.BoundingBoxes;
import dev.hephaestus.proximity.app.api.rendering.util.Stateful;
import org.jetbrains.annotations.ApiStatus;

/**
 * An element of Proximity's layer tree that can be configured prior to rendering.
 *
 * @param <D> the type of data this element operates off of
 */
@ApiStatus.NonExtendable
public interface Element<D extends RenderJob> extends Stateful {
    Document<D> getDocument();
    String getId();
    BoundingBoxes getBounds();
}
