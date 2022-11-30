package dev.hephaestus.proximity.app.api.rendering.properties;

import com.google.common.collect.ImmutableList;
import dev.hephaestus.proximity.app.api.rendering.util.Stateful;

import java.util.Optional;
import java.util.function.Function;

public interface ListProperty<D, V, R extends Stateful> {
    ImmutableList<V> get();

    R add(V value);
    R add(Function<D, Optional<V>> getter);
}
