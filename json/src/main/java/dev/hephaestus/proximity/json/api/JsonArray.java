package dev.hephaestus.proximity.json.api;

import dev.hephaestus.proximity.json.impl.json.MutableJsonArray;

public interface JsonArray extends JsonElement, Iterable<JsonElement>, JsonCollection<JsonArray.Mutable> {
    int size();

    JsonElement get(int i);

    boolean isEmpty();

    Mutable mutableCopy();

    boolean contains(Object object);

    static Mutable create() {
        return new MutableJsonArray();
    }

    interface Mutable extends JsonArray, dev.hephaestus.proximity.json.api.Mutable {
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

        @Override
        JsonArray toImmutable();

        JsonObject.Mutable createObject();

        Mutable createArray();
    }
}
