package dev.hephaestus.proximity.json.impl.json;

import dev.hephaestus.proximity.json.api.JsonArray;
import dev.hephaestus.proximity.json.api.JsonCollection;
import dev.hephaestus.proximity.json.api.JsonElement;
import dev.hephaestus.proximity.json.api.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.quiltmc.json5.JsonWriter;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

public abstract class AbstractJsonObject<M extends Map<String, JsonElement>> extends AbstractJsonElement implements JsonObject {
    protected final M values;

    protected AbstractJsonObject(M values) {
        this.values = values;
    }

    @Override
    public boolean isObject() {
        return true;
    }

    @Override
    public boolean isArray() {
        return false;
    }

    @Override
    public boolean isNull() {
        return false;
    }

    @Override
    public boolean isBoolean() {
        return false;
    }

    @Override
    public boolean isNumber() {
        return false;
    }

    @Override
    public boolean isString() {
        return false;
    }

    @Override
    public boolean asBoolean() {
        throw new RuntimeException("Cannot convert JsonObject to type 'boolean'");
    }

    @Override
    public JsonObject asObject() {
        return this;
    }

    @Override
    public JsonArray asArray() {
        throw new RuntimeException("Cannot convert JsonObject to type 'JsonArray'");
    }

    @Override
    public int asInt() {
        throw new RuntimeException("Cannot convert JsonObject to type 'int'");
    }

    @Override
    public long asLong() {
        throw new RuntimeException("Cannot convert JsonObject to type 'long'");
    }

    @Override
    public double asDouble() {
        throw new RuntimeException("Cannot convert JsonObject to type 'double'");    }

    @Override
    public float asFloat() {
        throw new RuntimeException("Cannot convert JsonObject to type 'float'");
    }

    @Override
    public String asString() {
        throw new RuntimeException("Cannot convert JsonObject to type 'String'");
    }

    @Override
    public boolean has(String... keys) {
        if (keys.length == 0) {
            return true;
        } if (keys.length == 1) {
            return this.values.containsKey(keys[0]);
        } else {
            JsonObject object = this;

            for (int i = 0, keysLength = keys.length; i < keysLength - 1; i++) {
                String key = keys[i];
                JsonElement element = this.get(key);

                if (element.isObject()) {
                    object = (JsonObject) element;
                } else {
                    return false;
                }
            }

            return object.has(keys[keys.length - 1]);
        }
    }

    @Override
    public JsonElement get(String... keys) {
        if (keys.length == 0) {
            return null;
        } if (keys.length == 1) {
            return this.values.get(keys[0]);
        } else {
            JsonObject object = this;

            for (int i = 0, keysLength = keys.length; i < keysLength - 1; i++) {
                String key = keys[i];
                JsonElement element = this.get(key);

                if (element.isObject()) {
                    object = (JsonObject) element;
                } else {
                    return null;
                }
            }

            return object.get(keys[keys.length - 1]);
        }
    }

    @Override
    public boolean getBoolean(String... keys) {
        return this.get(keys).asBoolean();
    }

    @Override
    public JsonObject getObject(String... keys) {
        return this.get(keys).asObject();
    }

    @Override
    public JsonArray getArray(String... keys) {
        return this.get(keys).asArray();
    }

    @Override
    public int getInt(String... keys) {
        return this.get(keys).asInt();
    }

    @Override
    public long getLong(String... keys) {
        return this.get(keys).asLong();
    }

    @Override
    public double getDouble(String... keys) {
        return this.get(keys).asDouble();
    }

    @Override
    public float getFloat(String... keys) {
        return this.get(keys).asFloat();
    }

    @Override
    public String getString(String... keys) {
        return this.get(keys).asString();
    }

    @NotNull
    @Override
    public Iterator<Map.Entry<String, JsonElement>> iterator() {
        return this.values.entrySet().iterator();
    }

    @Override
    public void write(JsonWriter writer) throws IOException {
        writer.beginObject();

        for (var entry : this) {
            writer.name(entry.getKey());
            ((AbstractJsonElement) entry.getValue()).write(writer);
        }

        writer.endObject();
    }

    @Override
    public Mutable mutableCopy() {
        MutableJsonObject result = new MutableJsonObject();

        for (var entry : this) {
            JsonElement value = entry.getValue();

            result.put(entry.getKey(), value instanceof JsonCollection<?> collection
                    ? collection.mutableCopy()
                    : value
            );
        }

        return result;
    }
}
