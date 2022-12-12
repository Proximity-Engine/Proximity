package dev.hephaestus.proximity.app.api.rendering.elements;

import dev.hephaestus.proximity.app.api.RenderJob;
import dev.hephaestus.proximity.app.api.rendering.properties.VisibilityProperty;

public interface Child<D extends RenderJob<?>> extends Element<D> {
    VisibilityProperty<D, ?> visibility();
}
