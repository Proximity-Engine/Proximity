package dev.hephaestus.proximity.app.api;

import dev.hephaestus.proximity.json.api.JsonElement;
import dev.hephaestus.proximity.json.api.JsonObject;
import dev.hephaestus.proximity.utils.UnmodifiableIterator;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class RenderJob<E extends JsonElement> {
    protected final E json;
    private final Map<Option<?, ?, ?>, Property<?>> options = new LinkedHashMap<>();

    protected RenderJob(E json) {
        this.json = json;
    }

    public abstract String getName();

    public final <T, D extends RenderJob<?>> T getOption(Option<T, ?, D> option) {
        return this.getOptionProperty(option).getValue();
    }

    @SuppressWarnings("unchecked")
    public final <T, D extends RenderJob<?>> Property<T> getOptionProperty(Option<T, ?, D> option) {
        if (!this.options.containsKey(option)) {
            this.options.put(option, new SimpleObjectProperty<>(option.getDefaultValue((D) this)));
        }

        return (Property<T>) this.options.get(option);
    }

    public abstract JsonElement toJson();

    public final JsonElement toJsonImpl() {
        var json = JsonObject.create();

        json.put("class", this.getClass().getName());

        JsonObject options = json.createObject("options");

        for (var entry : this.options.entrySet()) {
            //noinspection unchecked,rawtypes
            this.put((Map.Entry) entry, options);
        }

        json.put("data", this.toJson());

        return json;
    }

    public Iterable<Option<?, ?, ?>> options() {
        return this.options.keySet();
    }

    private <T, D extends RenderJob<?>> void put(Map.Entry<Option<T, ?, D>, Object> entry, JsonObject json) {
        var option = entry.getKey();

        json.put(option.getId(), option.toJson(this.getOption(option)));
    }
}
