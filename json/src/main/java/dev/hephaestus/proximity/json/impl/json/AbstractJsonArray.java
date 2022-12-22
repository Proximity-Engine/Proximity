package dev.hephaestus.proximity.json.impl.json;

import dev.hephaestus.proximity.json.api.*;
import org.jetbrains.annotations.NotNull;
import org.quiltmc.json5.JsonWriter;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public abstract class AbstractJsonArray<L extends List<JsonElement>> implements AbstractJsonElement, JsonArray {
    protected final L values;

    AbstractJsonArray(L values) {
        this.values = values;
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
    public Mutable mutableCopy() {
        MutableJsonArray result = new MutableJsonArray();

        for (JsonElement element : this) {
            result.add(element instanceof JsonCollection<?> collection
                    ? collection.mutableCopy()
                    : element
            );
        }

        return result;
    }
}
