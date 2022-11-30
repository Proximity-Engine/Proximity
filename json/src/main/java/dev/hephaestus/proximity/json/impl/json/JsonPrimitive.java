package dev.hephaestus.proximity.json.impl.json;


import dev.hephaestus.proximity.json.api.JsonArray;
import dev.hephaestus.proximity.json.api.JsonElement;
import dev.hephaestus.proximity.json.api.JsonObject;
import org.quiltmc.json5.JsonWriter;

import java.io.IOException;

public final class JsonPrimitive extends AbstractJsonElement implements JsonElement {
    private final Object value;

    public JsonPrimitive() {
        this.value = null;
    }

    public JsonPrimitive(boolean value) {
        this.value = value;
    }

    public JsonPrimitive(int value) {
        this.value = value;
    }

    public JsonPrimitive(long value) {
        this.value = value;
    }

    public JsonPrimitive(double value) {
        this.value = value;
    }

    public JsonPrimitive(float value) {
        this.value = value;
    }

    public JsonPrimitive(String value) {
        this.value = value;
    }

    public JsonPrimitive(Number value) {
        this.value = value;
    }

    @Override
    public boolean isObject() {
        return false;
    }

    @Override
    public boolean isArray() {
        return false;
    }

    @Override
    public boolean isNull() {
        return this.value == null;
    }

    @Override
    public boolean isBoolean() {
        return this.value instanceof Boolean;
    }

    @Override
    public boolean isNumber() {
        return this.value instanceof Number;
    }

    @Override
    public boolean isString() {
        return this.value instanceof String;
    }

    @Override
    public boolean asBoolean() {
        return (boolean) this.value;
    }

    @Override
    public JsonObject asObject() {
        throw new RuntimeException("Cannot convert JsonPrimitive to type 'JsonObject'");
    }

    @Override
    public JsonArray asArray() {
        throw new RuntimeException("Cannot convert JsonPrimitive to type 'JsonArray'");
    }

    @Override
    public int asInt() {
        return ((Number) this.value).intValue();
    }

    @Override
    public long asLong() {
        return ((Number) this.value).longValue();
    }

    @Override
    public double asDouble() {
        return ((Number) this.value).doubleValue();
    }

    @Override
    public float asFloat() {
        return ((Number) this.value).floatValue();
    }

    @Override
    public String asString() {
        return (String) this.value;
    }

    @Override
    public void write(JsonWriter writer) throws IOException {
        if (this.isString()) {
            writer.value(this.asString());
        } else if (this.isBoolean()) {
            writer.value(this.asBoolean());
        } else if (this.isNumber()) {
            writer.value((Number) this.value);
        } else {
            writer.nullValue();
        }
    }
}
