package dev.hephaestus.proximity.app.impl.rendering.properties;

import dev.hephaestus.proximity.app.api.rendering.properties.ThrowingProperty;
import dev.hephaestus.proximity.app.api.rendering.util.ThrowingFunction;

public class BasicThrowingProperty<D, V, R, E extends Exception> implements ThrowingProperty<D, V, R, E> {
    private final D data;
    private final R result;

    private ThrowingFunction<D, V, E> getter;

    public BasicThrowingProperty(R result, D data) {
        this(result, data, d -> null);
    }

    public BasicThrowingProperty(R result, D data, V defaultValue) {
        this(result, data, d -> defaultValue);
    }

    public BasicThrowingProperty(R result, D data, ThrowingFunction<D, V, E> defaultValueGetter) {
        this.data = data;
        this.result = result;
        this.getter = defaultValueGetter;
    }

    @Override
    public V get() throws E {
        return this.getter.apply(this.data);
    }

    @Override
    public R set(V value) {
        this.getter = d -> value;

        return this.result;
    }

    @Override
    public R set(ThrowingFunction<D, V, E> getter) {
        this.getter = getter;

        return this.result;
    }
}
