package dev.hephaestus.proximity.app.api.rendering.properties;

import java.util.function.Predicate;

public interface VisibilityProperty<D, R> {
    boolean get();

    R set(boolean value);
    R set(Predicate<D> getter);
}
