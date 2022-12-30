package dev.hephaestus.proximity.json.impl.json;

import dev.hephaestus.proximity.json.api.JsonArray;
import dev.hephaestus.proximity.json.api.JsonBoolean;
import dev.hephaestus.proximity.json.api.JsonElement;
import dev.hephaestus.proximity.json.api.JsonObject;
import dev.hephaestus.proximity.json.api.JsonString;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableBooleanValue;
import org.jetbrains.annotations.NotNull;
import org.quiltmc.json5.JsonWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JsonObjectImpl implements JsonObject, Observable, AbstractJsonElement {
    protected final LinkedHashMap<String, JsonElement> values = new LinkedHashMap<>();
    private final List<InvalidationListener> listeners = new ArrayList<>();
    private boolean frozen = false;

    public JsonObjectImpl() {
    }

    @Override
    public void put(String key, JsonElement value) {
        this.values.put(key, value);

        if (value != null) {
            value.addListener(o -> this.invalidate());
        }

        this.invalidate();
    }

    @Override
    public void put(String key, boolean value) {
        this.put(key, new JsonBooleanImpl(value));
    }

    @Override
    public void put(String key, int value) {
        this.put(key, new JsonNumberImpl(value));
    }

    @Override
    public void put(String key, float value) {
        this.put(key, new JsonNumberImpl(value));
    }

    @Override
    public void put(String key, double value) {
        this.put(key, new JsonNumberImpl(value));
    }

    @Override
    public void put(String key, String value) {
        this.put(key, new JsonStringImpl(value));
    }

    @Override
    public void copyAll(JsonObject other) {
        this.freeze();

        for (var entry : other) {
            JsonElement value = entry.getValue();

            this.put(entry.getKey(), value.copy());
        }

        this.unfreeze();

        this.invalidate();
    }

    private void freeze() {
        this.frozen = true;
    }

    private void unfreeze() {
        this.frozen = false;
    }

    private boolean isFrozen() {
        return this.frozen;
    }

    @Override
    public JsonObject createObject(String key) {
        var object = new JsonObjectImpl();

        this.put(key, object);

        return object;
    }

    @Override
    public JsonArray createArray(String key) {
        var array = new JsonArrayImpl();

        this.put(key, array);

        return array;
    }

    @Override
    public void addListener(InvalidationListener listener) {
        this.listeners.add(listener);
    }

    @Override
    public void removeListener(InvalidationListener listener) {
        this.listeners.remove(listener);
    }

    private void invalidate() {
        if (!this.isFrozen()) {
            for (InvalidationListener listener : this.listeners) {
                listener.invalidated(this);
            }
        }
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
    public ObservableBooleanValue contains(String... keys) {
        return Bindings.createBooleanBinding(() -> {
            return this.has(keys);
        }, this);
    }

    @Override
    public <T extends JsonElement> T get(String... keys) {
        if (keys.length == 0) {
            return null;
        } if (keys.length == 1) {
            return (T) this.values.get(keys[0]);
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
    public JsonObject copy() {
        JsonObjectImpl result = new JsonObjectImpl();

        for (var entry : this) {
            JsonElement value = entry.getValue();

            result.put(entry.getKey(), value.copy());
        }

        return result;
    }
}
