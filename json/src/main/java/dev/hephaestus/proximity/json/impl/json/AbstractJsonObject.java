package dev.hephaestus.proximity.json.impl.json;

import dev.hephaestus.proximity.json.api.*;
import org.jetbrains.annotations.NotNull;
import org.quiltmc.json5.JsonWriter;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.Map;

public abstract class AbstractJsonObject<M extends Map<String, JsonElement>> implements AbstractJsonElement, JsonObject {
    protected final M values;

    protected AbstractJsonObject(M values) {
        this.values = values;
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

                if (element instanceof JsonObject o) {
                    object = o;
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

                if (element instanceof JsonObject o) {
                    object = o;
                } else {
                    return null;
                }
            }

            return object.get(keys[keys.length - 1]);
        }
    }

    @Override
    public boolean getBoolean(String... keys) {
        return ((JsonBoolean) this.get(keys)).get();
    }

    @Override
    public JsonObject getObject(String... keys) {
        return (JsonObject) this.get(keys);
    }

    @Override
    public JsonArray getArray(String... keys) {
        return (JsonArray) this.get(keys);
    }

    @Override
    public int getInt(String... keys) {
        return ((JsonNumberImpl) this.get(keys)).get().intValue();
    }

    @Override
    public long getLong(String... keys) {
        return ((JsonNumberImpl) this.get(keys)).get().longValue();
    }

    @Override
    public double getDouble(String... keys) {
        return ((JsonNumberImpl) this.get(keys)).get().doubleValue();
    }

    @Override
    public float getFloat(String... keys) {
        return ((JsonNumberImpl) this.get(keys)).get().floatValue();
    }

    @Override
    public String getString(String... keys) {
        return ((JsonString) this.get(keys)).get();
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

    @Override
    public String toString() {
        StringWriter stringWriter = new StringWriter();
        JsonWriter writer = JsonWriter.json(stringWriter);

        try {
            this.write(writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return stringWriter.toString();
    }
}
