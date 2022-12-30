package dev.hephaestus.proximity.app.api.rendering;

import dev.hephaestus.proximity.app.api.Option;
import dev.hephaestus.proximity.json.api.JsonObject;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;

import java.util.HashMap;
import java.util.Map;

public class RenderData {
    public final JsonObject json;

    private final Map<Option<?, ?, ?>, Property<?>> options = new HashMap<>();

    public RenderData(JsonObject json) {
        this.json = json;
    }

    public <T, D extends RenderData> Property<T> getOption(Option<T, ?, ? super D> option) {
        return (Property<T>) this.options.computeIfAbsent(option, o -> new SimpleObjectProperty<>(option.getDefaultValue((D) this)));
    }

    public Iterable<Map.Entry<Option<?, ?, ?>, Property<?>>> options() {
        return this.options.entrySet();
    }
}
