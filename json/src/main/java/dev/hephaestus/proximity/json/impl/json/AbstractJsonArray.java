package dev.hephaestus.proximity.json.impl.json;

import dev.hephaestus.proximity.json.api.JsonArray;
import dev.hephaestus.proximity.json.api.JsonCollection;
import dev.hephaestus.proximity.json.api.JsonElement;
import dev.hephaestus.proximity.json.api.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.quiltmc.json5.JsonWriter;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public abstract class AbstractJsonArray<L extends List<JsonElement>> extends AbstractJsonElement implements JsonArray {
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

    @Override
    public boolean isObject() {
        return false;
    }

    @Override
    public boolean isArray() {
        return true;
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
        return false;
    }

    @Override
    public JsonObject asObject() {
        throw new RuntimeException("Cannot convert JsonArray to type 'JsonObject'");
}

    @Override
    public JsonArray asArray() {
        return this;
    }

    @Override
    public int asInt() {
        throw new RuntimeException("Cannot convert JsonArray to type 'int'");
    }

    @Override
    public long asLong() {
        throw new RuntimeException("Cannot convert JsonArray to type 'long'");
    }

    @Override
    public float asFloat() {
        throw new RuntimeException("Cannot convert JsonArray to type 'float'");
    }

    @Override
    public double asDouble() {
        throw new RuntimeException("Cannot convert JsonArray to type 'double'");
    }

    @Override
    public String asString() {
        throw new RuntimeException("Cannot convert JsonArray to type 'String'");
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
            } else if (object instanceof String && element.isString() && element.asString().equals(object)) {
                return true;
            } else if (object instanceof Integer && element.isNumber() && object.equals(element.asInt())) {
                return true;
            } else if (object instanceof Double && element.isNumber() && object.equals(element.asDouble())) {
                return true;
            } else if (object instanceof Float && element.isNumber() && object.equals(element.asFloat())) {
                return true;
            } else if (object instanceof Long && element.isNumber() && object.equals(element.asLong())) {
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
