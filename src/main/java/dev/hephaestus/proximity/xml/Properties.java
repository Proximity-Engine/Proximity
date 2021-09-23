package dev.hephaestus.proximity.xml;

import dev.hephaestus.proximity.json.JsonObject;

import java.util.function.Function;

public class Properties {
    private final Properties parent;
    private final Function<LayerProperty<?>, Function<JsonObject, ?>> propertyGetter;

    public Properties(Properties parent, Function<LayerProperty<?>, Function<JsonObject, ?>> propertyGetter) {
        this.parent = parent;
        this.propertyGetter = propertyGetter;
    }

    @SuppressWarnings("unchecked")
    public <T> Function<JsonObject, T> get(LayerProperty<T> property) {
        var f = (Function<JsonObject, T>) this.propertyGetter.apply(property);

        if (f == null && this.parent != null) {
            return this.parent.get(property);
        } else {
            return f;
        }
    }
}
