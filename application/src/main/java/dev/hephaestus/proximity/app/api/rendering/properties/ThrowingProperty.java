package dev.hephaestus.proximity.app.api.rendering.properties;

import dev.hephaestus.proximity.app.api.rendering.util.ThrowingFunction;

public interface ThrowingProperty<D, V, R, E extends Exception> {
    V get() throws E;

    R set(V value);
    R set(ThrowingFunction<D, V, E> getter);
}
