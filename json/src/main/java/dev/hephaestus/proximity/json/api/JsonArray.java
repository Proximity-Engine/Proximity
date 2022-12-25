package dev.hephaestus.proximity.json.api;

import dev.hephaestus.proximity.json.impl.json.JsonArrayImpl;

public interface JsonArray extends JsonElement, Iterable<JsonElement> {
    int size();

    JsonElement get(int i);

    boolean isEmpty();

    boolean contains(Object object);

    static JsonArray create() {
        return new JsonArrayImpl();
    }

    void add(JsonElement value);

    void add(boolean value);

    void add(int value);

    void add(float value);

    void add(double value);

    void add(String value);

    void add(int index, JsonElement value);

    void add(int index, boolean value);

    void add(int index, int value);

    void add(int index, float value);

    void add(int index, double value);

    void add(int index, String value);

    void remove(int index);

    JsonObject createObject();

    JsonArray createArray();

    JsonArray copy();
}
