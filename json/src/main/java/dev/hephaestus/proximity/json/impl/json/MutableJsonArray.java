package dev.hephaestus.proximity.json.impl.json;


import com.google.common.collect.ImmutableList;
import dev.hephaestus.proximity.json.api.JsonArray;
import dev.hephaestus.proximity.json.api.JsonElement;
import dev.hephaestus.proximity.json.api.JsonObject;

import java.util.ArrayList;
import java.util.List;

public final class MutableJsonArray extends AbstractJsonArray<List<JsonElement>> implements JsonArray.Mutable {
    public MutableJsonArray() {
        super(new ArrayList<>());
    }

    public JsonArray toImmutable() {
        ImmutableList.Builder<JsonElement> builder = ImmutableList.builder();

        for (JsonElement element : this) {
            if (element instanceof dev.hephaestus.proximity.json.api.Mutable mutable) {
                element = mutable.toImmutable();
            }

            builder.add(element);
        }

        return new ImmutableJsonArray(builder.build());
    }

    @Override
    public JsonObject.Mutable createObject() {
        JsonObject.Mutable object = new MutableJsonObject();

        this.add(object);

        return object;
    }

    @Override
    public Mutable createArray() {
        JsonArray.Mutable array = new MutableJsonArray();

        this.add(array);

        return array;
    }

    @Override
    public void add(JsonElement value) {
        this.values.add(value);
    }

    @Override
    public void add(boolean value) {
        this.values.add(new JsonBooleanImpl(value));
    }

    @Override
    public void add(int value) {
        this.values.add(new JsonNumberImpl(value));
    }

    @Override
    public void add(float value) {
        this.values.add(new JsonNumberImpl(value));
    }

    @Override
    public void add(double value) {
        this.values.add(new JsonNumberImpl(value));
    }

    @Override
    public void add(String value) {
        this.values.add(new JsonStringImpl(value));
    }

    @Override
    public void add(int index, JsonElement value) {
        this.values.add(index, value);
    }

    @Override
    public void add(int index, boolean value) {
        this.values.add(index, new JsonBooleanImpl(value));
    }

    @Override
    public void add(int index, int value) {
        this.values.add(index, new JsonNumberImpl(value));
    }

    @Override
    public void add(int index, float value) {
        this.values.add(index, new JsonNumberImpl(value));
    }

    @Override
    public void add(int index, double value) {
        this.values.add(index, new JsonNumberImpl(value));
    }

    @Override
    public void add(int index, String value) {
        this.values.add(index, new JsonStringImpl(value));
    }

    @Override
    public void remove(int index) {
        this.values.remove(index);
    }
}
