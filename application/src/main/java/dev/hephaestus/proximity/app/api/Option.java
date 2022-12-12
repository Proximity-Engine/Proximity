package dev.hephaestus.proximity.app.api;

import dev.hephaestus.proximity.json.api.JsonElement;
import javafx.beans.property.Property;
import javafx.scene.Node;

import java.util.function.Function;

public abstract class Option<T, W extends Node & Option.Widget<T>, D extends RenderJob<?>> implements Function<D, T> {
    private final String id;
    private final Function<D, T> defaultValue;

    protected Option(String id, T defaultValue) {
        this.id = id;
        this.defaultValue = d -> defaultValue;
    }

    protected Option(String id, Function<D, T> defaultValue) {
        this.id = id;
        this.defaultValue = defaultValue;
    }

    @Override
    public T apply(D d) {
        return d.getOption(this);
    }

    public final String getId() {
        return this.id;
    }

    public final T getDefaultValue(D renderJob) {
        return this.defaultValue.apply(renderJob);
    }

    public abstract JsonElement toJson(T value);

    public abstract T fromJson(JsonElement json);

    /**
     * The control should update the appropriate option value when necessary.
     *
     * @param renderJob
     * @return some widget that allows configuration of the option
     */
    public abstract W createControl(D renderJob);

    public interface Widget<T> {
        Property<T> getValueProperty();
    }
}
