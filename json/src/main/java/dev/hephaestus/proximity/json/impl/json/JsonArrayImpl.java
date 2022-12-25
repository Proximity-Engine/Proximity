package dev.hephaestus.proximity.json.impl.json;


import dev.hephaestus.proximity.json.api.JsonArray;
import dev.hephaestus.proximity.json.api.JsonElement;
import dev.hephaestus.proximity.json.api.JsonObject;
import dev.hephaestus.proximity.json.api.JsonString;
import javafx.beans.InvalidationListener;
import org.jetbrains.annotations.NotNull;
import org.quiltmc.json5.JsonWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class JsonArrayImpl implements JsonArray, AbstractJsonElement {
    private final List<JsonElement> values = new ArrayList<>();
    private final List<InvalidationListener> listeners = new ArrayList<>();

    public JsonArrayImpl() {
    }

    @Override
    public JsonObject createObject() {
        JsonObject object = new JsonObjectImpl();

        this.add(object);

        return object;
    }

    @Override
    public JsonArray createArray() {
        JsonArray array = new JsonArrayImpl();

        this.add(array);

        return array;
    }

    @Override
    public void add(JsonElement value) {
        this.values.add(value);

        value.addListener(o -> this.invalidate());

        this.invalidate();
    }

    @Override
    public void add(boolean value) {
        this.add(new JsonBooleanImpl(value));
    }

    @Override
    public void add(int value) {
        this.add(new JsonNumberImpl(value));
    }

    @Override
    public void add(float value) {
        this.add(new JsonNumberImpl(value));
    }

    @Override
    public void add(double value) {
        this.add(new JsonNumberImpl(value));
    }

    @Override
    public void add(String value) {
        this.add(new JsonStringImpl(value));
    }

    @Override
    public void add(int index, JsonElement value) {
        this.values.add(index, value);

        value.addListener(o -> this.invalidate());

        this.invalidate();
    }

    @Override
    public void add(int index, boolean value) {
        this.add(index, new JsonBooleanImpl(value));
    }

    @Override
    public void add(int index, int value) {
        this.add(index, new JsonNumberImpl(value));
    }

    @Override
    public void add(int index, float value) {
        this.add(index, new JsonNumberImpl(value));
    }

    @Override
    public void add(int index, double value) {
        this.add(index, new JsonNumberImpl(value));
    }

    @Override
    public void add(int index, String value) {
        this.add(index, new JsonStringImpl(value));
    }

    @Override
    public void remove(int index) {
        this.values.remove(index);
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
        for (InvalidationListener listener : this.listeners) {
            listener.invalidated(this);
        }
    }

    @Override
    public int size() {
        return this.values.size();
    }

    @Override
    public JsonElement get(int i) {
        return this.values.get(i);
    }

    @Override
    public boolean isEmpty() {
        return this.values.isEmpty();
    }

    @NotNull
    @Override
    public Iterator<JsonElement> iterator() {
        return this.values.iterator();
    }

    @Override
    public boolean contains(Object object) {
        for (JsonElement element : this.values) {
            if (object instanceof JsonElement && element.equals(object)) {
                return true;
            } else if (object instanceof String && element instanceof JsonString s && s.get().equals(object)) {
                return true;
            } else if (object instanceof Integer && element instanceof JsonNumberImpl n && n.get().equals(object)) {
                return true;
            } else if (object instanceof Double && element instanceof JsonNumberImpl n && n.get().equals(object)) {
                return true;
            } else if (object instanceof Float && element instanceof JsonNumberImpl n && n.get().equals(object)) {
                return true;
            } else if (object instanceof Long && element instanceof JsonNumberImpl n && n.get().equals(object)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void write(JsonWriter writer) throws IOException {
        writer.beginArray();

        for (JsonElement element : this) {
            ((AbstractJsonElement) element).write(writer);
        }

        writer.endArray();
    }

    @Override
    public JsonArray copy() {
        JsonArray array = new JsonArrayImpl();

        for (JsonElement element : this) {
            array.add(element.copy());
        }

        return array;
    }
}
