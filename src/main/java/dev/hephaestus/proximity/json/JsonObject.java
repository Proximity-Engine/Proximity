package dev.hephaestus.proximity.json;

import org.quiltmc.json5.JsonReader;
import org.quiltmc.json5.JsonToken;
import org.quiltmc.json5.JsonWriter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class JsonObject extends JsonElement {
    private final Map<String, JsonElement> members = new LinkedHashMap<>();

    public JsonObject() {
    }

    public JsonObject(String key, JsonElement entry) {
        this.members.put(key, entry);
    }

    public static JsonObject interpret(Map map) {
        JsonObject object = new JsonObject();

        for (Map.Entry entry : (Set<Map.Entry>) map.entrySet()) {
            String key = entry.getKey().toString();
            Object value = entry.getValue();

            if (value instanceof Number n) {
                object.add(key, new JsonPrimitive(n));
            } else if (value instanceof String s) {
                object.add(key, new JsonPrimitive(s));
            } else if (value instanceof Boolean b) {
                object.add(key, new JsonPrimitive(b));
            } else if (value instanceof JsonElement element) {
                object.add(key, element);
            } else if (value instanceof Map valueMap) {
                object.add(key, interpret(valueMap));
            } else if (value != null) {
                throw new UnsupportedOperationException();
            }
        }

        return object;
    }

    @Override
    public JsonObject deepCopy() {
        JsonObject result = new JsonObject();
        for (Map.Entry<String, JsonElement> entry : members.entrySet()) {
            result.add(entry.getKey(), entry.getValue().deepCopy());
        }
        return result;
    }

    @Override
    protected void write(JsonWriter writer) throws IOException {
        writer.beginObject();

        for (var entry : this.members.entrySet()) {
            writer.name(entry.getKey());
            entry.getValue().write(writer);
        }

        writer.endObject();
    }

    public void add(String property, JsonElement value) {
        members.put(property, value == null ? JsonNull.INSTANCE : value);
    }

    public void addProperty(String property, String value) {
        add(property, value == null ? JsonNull.INSTANCE : new JsonPrimitive(value));
    }

    public void addProperty(String property, Number value) {
        add(property, value == null ? JsonNull.INSTANCE : new JsonPrimitive(value));
    }

    public void addProperty(String property, Boolean value) {
        add(property, value == null ? JsonNull.INSTANCE : new JsonPrimitive(value));
    }

    public void remove(String key) {
        this.members.remove(key);
    }

    public Set<Map.Entry<String, JsonElement>> entrySet() {
        return members.entrySet();
    }

    public int size() {
        return members.size();
    }

    public boolean has(String memberName) {
        return members.containsKey(memberName);
    }

    public JsonElement get(String memberName) {
        return members.get(memberName);
    }

    public Optional<JsonElement> getIfPresent(String key) {
        return Optional.ofNullable(this.members.get(key));
    }

    @SuppressWarnings("unchecked")
    public <T extends JsonElement> T get(String key, Supplier<T> orAdd) {
        if (!this.members.containsKey(key)) {
            this.add(key, orAdd.get());
        }

        return (T) this.get(key);
    }

    public JsonArray getAsJsonArray(String... keys) {
        return this.get((o, k) -> o.get(k, JsonArray::new).getAsJsonArray(), keys);
    }

    @Override
    public boolean equals(Object o) {
        return (o == this) || (o instanceof JsonObject
                && ((JsonObject) o).members.equals(members));
    }

    @Override
    public int hashCode() {
        return members.hashCode();
    }

    public boolean getAsBoolean(String... keys) {
        JsonElement e = get(JsonObject::get, keys);

        return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isBoolean() && e.getAsBoolean();
    }

    public String getAsString(String... keys) {
        return get((o, k) -> o.get(k).getAsString(), keys);
    }

    public int getAsInt(String... keys) {
        return get((o, k) -> o.get(k).getAsInt(), keys);
    }

    public JsonObject getAsJsonObject(String... keys) {
        return get((o, k) -> {
            if (!o.has(k)) {
                o.add(k, new JsonObject());
            }

            return o.get(k).getAsJsonObject();
        }, keys);
    }

    public <T> T get(BiFunction<JsonObject, String, T> getter, String... keys) {
        if (keys.length == 0) throw new UnsupportedOperationException();
        if (keys.length == 1) return getter.apply(this, keys[0]);

        JsonObject object = this;

        for (int i = 0; i < keys.length - 1; ++i) {
            object = object.get(keys[i], JsonObject::new);
        }

        return getter.apply(object, keys[keys.length - 1]);
    }

    public static JsonObject parseObject(JsonReader reader) throws IOException {
        reader.beginObject();

        JsonObject object = new JsonObject();

        while (reader.hasNext() && reader.peek() == JsonToken.NAME) {
            object.add(reader.nextName(), parseElement(reader));
        }

        reader.endObject();

        return object;
    }

    public void add(String[] key, boolean value) {
        this.add(key, new JsonPrimitive(value));
    }

    public void add(String[] key, String value) {
        this.add(key, new JsonPrimitive(value));
    }

    public void add(String[] key, Number value) {
        this.add(key, new JsonPrimitive(value));
    }

    public void add(String[] key, JsonElement value) {
        if (key.length == 0) throw new UnsupportedOperationException();

        JsonObject object = this;

        for (int i = 0; i < key.length - 1; ++i) {
            object = object.get(key[i], JsonObject::new);
        }

        object.add(key[key.length - 1], value);
    }

    public boolean has(String... key) {
        if (key.length == 0) throw new UnsupportedOperationException();

        JsonObject object = this;

        for (int i = 0; i < key.length - 1; ++i) {
            if (!object.has(key[i])) return false;

            object = object.getAsJsonObject(key[i]);
        }

        return object.has(key[key.length - 1]);
    }

    public JsonElement get(String... key) {
        if (key.length == 0) throw new UnsupportedOperationException();

        JsonObject object = this;

        for (int i = 0; i < key.length - 1; ++i) {
            if (!object.has(key[i])) return null;

            object = object.getAsJsonObject(key[i]);
        }

        return object.get(key[key.length - 1]);
    }

    public JsonObject copyAll(JsonObject object) {
        for (var entry : object.entrySet()) {
            if (entry.getValue() instanceof JsonObject o) {
                this.getAsJsonObject(entry.getKey()).copyAll(o);
            } else {
                this.add(entry.getKey(), entry.getValue().deepCopy());
            }
        }

        return this;
    }

    public float getAsFloat(String... keys) {
        return get((o, k) -> o.get(k).getAsFloat(), keys);
    }
}
