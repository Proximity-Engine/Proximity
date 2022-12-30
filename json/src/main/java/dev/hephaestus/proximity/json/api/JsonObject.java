package dev.hephaestus.proximity.json.api;

import dev.hephaestus.proximity.json.impl.json.JsonObjectImpl;
import javafx.beans.value.ObservableBooleanValue;

import java.util.Map;

public interface JsonObject extends JsonElement, Iterable<Map.Entry<String, JsonElement>> {
    boolean has(String... keys);

    ObservableBooleanValue contains(String... keys);

    <T extends JsonElement> T get(String... keys);

    boolean getBoolean(String... keys);

    JsonObject getObject(String... keys);

    JsonArray getArray(String... keys);

    int getInt(String... keys);

    long getLong(String... keys);

    double getDouble(String... keys);

    float getFloat(String... keys);

    String getString(String... keys);

    static JsonObject create() {
        return new JsonObjectImpl();
    }

    void put(String key, JsonElement value);

    void put(String key, boolean value);

    void put(String key, int value);

    void put(String key, float value);

    void put(String key, double value);

    void put(String key, String value);

    /**
     * Copies all members from another JsonObject to this one, overriding existing members if present.
     */
    void copyAll(JsonObject other);

    /**
     * Creates {@link JsonObject}, inserts it into this {@link JsonObject} with the given key, then returns it.
     */
    JsonObject createObject(String key);

    /**
     * Creates {@link JsonArray}, inserts it into this {@link JsonArray} with the given key, then returns it.
     */
    JsonArray createArray(String key);

    JsonObject copy();
}
