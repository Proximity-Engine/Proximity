package dev.hephaestus.proximity.json.impl.json;

import com.google.common.collect.ImmutableMap;
import dev.hephaestus.proximity.json.api.JsonArray;
import dev.hephaestus.proximity.json.api.JsonCollection;
import dev.hephaestus.proximity.json.api.JsonElement;
import dev.hephaestus.proximity.json.api.JsonObject;

import java.util.LinkedHashMap;

public class MutableJsonObject extends AbstractJsonObject<LinkedHashMap<String, JsonElement>> implements JsonObject.Mutable {
    public MutableJsonObject() {
        super(new LinkedHashMap<>());
    }

    public JsonObject toImmutable() {
        ImmutableMap.Builder<String, JsonElement> builder = ImmutableMap.builder();

        for (var entry : this) {
            JsonElement value = entry.getValue();

            if (value instanceof dev.hephaestus.proximity.json.api.Mutable mutable) {
                value = mutable.toImmutable();
            }

            builder.put(entry.getKey(), value);
        }

        return new ImmutableJsonObject(builder.build());
    }

    @Override
    public void put(String key, JsonElement value) {
        this.values.put(key, value);
    }

    @Override
    public void put(String key, boolean value) {
        this.values.put(key, new JsonPrimitive(value));
    }

    @Override
    public void put(String key, int value) {
        this.values.put(key, new JsonPrimitive(value));
    }

    @Override
    public void put(String key, float value) {
        this.values.put(key, new JsonPrimitive(value));
    }

    @Override
    public void put(String key, double value) {
        this.values.put(key, new JsonPrimitive(value));
    }

    @Override
    public void put(String key, String value) {
        this.values.put(key, new JsonPrimitive(value));
    }

    @Override
    public void copyAll(JsonObject other) {
        for (var entry : other) {
            JsonElement value = entry.getValue();

            this.put(entry.getKey(), value instanceof JsonCollection<?> collection
                    ? collection.mutableCopy()
                    : value);
        }
    }

    @Override
    public JsonObject.Mutable createObject(String key) {
        JsonObject.Mutable object = new MutableJsonObject();

        this.put(key, object);

        return object;
    }

    @Override
    public JsonArray.Mutable createArray(String key) {
        JsonArray.Mutable array = new MutableJsonArray();

        this.put(key, array);

        return array;
    }
}
