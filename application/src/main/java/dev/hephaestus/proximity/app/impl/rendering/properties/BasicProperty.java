package dev.hephaestus.proximity.app.impl.rendering.properties;

import dev.hephaestus.proximity.app.api.rendering.properties.Property;
import dev.hephaestus.proximity.app.api.rendering.util.Stateful;

import java.util.function.Function;

public class BasicProperty<D, V, R extends Stateful> implements Property<D, V, R>, Stateful {
    private final D data;
    private final R result;
    private Function<D, V> getter;

    private boolean isSet = false;
    private V value;

    public BasicProperty(R result, D data) {
        this(result, data, d -> null);
    }

    public BasicProperty(R result, D data, V defaultValue) {
        this(result, data, d -> defaultValue);
    }

    public BasicProperty(R result, D data, Function<D, V> defaultValueGetter) {
        this.data = data;
        this.result = result;
        this.getter = defaultValueGetter;
    }

    @Override
    public V get() {
        if (!this.isSet) {
            this.value = this.getter.apply(this.data);
        }

        return this.value;
    }

    @Override
    public R set(V value) {
        this.getter = d -> value;

        this.invalidate();

        return this.result;
    }

    @Override
    public R set(Function<D, V> getter) {
        this.getter = getter;

        this.invalidate();

        return this.result;
    }

    @Override
    public void invalidate() {
        this.isSet = false;
        this.result.invalidate();
    }
}
