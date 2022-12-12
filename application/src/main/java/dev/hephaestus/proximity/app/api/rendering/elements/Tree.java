package dev.hephaestus.proximity.app.api.rendering.elements;

import dev.hephaestus.proximity.app.api.RenderJob;

import java.util.function.Predicate;

public interface Tree<D extends RenderJob<?>> {
    void branch(String id, Predicate<D> visibilityPredicate);
    void branch(String id);
}
