package dev.hephaestus.proximity.app.api.rendering.properties;

import dev.hephaestus.proximity.app.api.rendering.util.Stateful;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public interface ListProperty<D, V, R extends Stateful> {
    List<V> get();

    R add(V value);
    R add(Function<D, Optional<V>> getter);
}
