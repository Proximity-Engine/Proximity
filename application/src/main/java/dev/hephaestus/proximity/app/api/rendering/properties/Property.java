package dev.hephaestus.proximity.app.api.rendering.properties;

import dev.hephaestus.proximity.app.api.rendering.util.Stateful;

import java.util.function.Function;

public interface Property<D, V, R extends Stateful> {
    V get();

    R set(V value);
    R set(Function<D, V> getter);
}
