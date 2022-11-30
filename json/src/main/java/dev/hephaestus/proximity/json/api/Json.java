package dev.hephaestus.proximity.json.api;

import dev.hephaestus.proximity.json.impl.json.JsonPrimitive;
import dev.hephaestus.proximity.json.impl.json.MutableJsonArray;
import dev.hephaestus.proximity.json.impl.json.MutableJsonObject;
import org.quiltmc.json5.JsonReader;
import org.quiltmc.json5.JsonToken;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Path;

public final class Json {
    private Json() {}

    public static JsonElement parseElement(JsonReader reader) throws IOException {
        return switch (reader.peek()) {
            case END_ARRAY -> throw new UnsupportedOperationException("Unexpected end of array");
            case BEGIN_OBJECT -> parseObject(reader);
            case BEGIN_ARRAY -> parseArray(reader);
            case END_OBJECT -> throw new RuntimeException("Unexpected end of object");
            case NAME -> throw new RuntimeException("Unexpected name");
            case STRING -> new JsonPrimitive(reader.nextString());
            case NUMBER -> new JsonPrimitive(reader.nextNumber());
            case BOOLEAN -> new JsonPrimitive(reader.nextBoolean());
            case NULL -> {
                reader.nextNull();
                yield new JsonPrimitive();
            }
            case END_DOCUMENT -> throw new RuntimeException("Unexpected end of file");
        };
    }

    public static JsonObject.Mutable parseObject(InputStream inputStream) throws IOException {
        return parseObject(JsonReader.json5(new InputStreamReader(inputStream)));
    }

    public static JsonObject.Mutable parseObject(Reader reader) throws IOException {
        return parseObject(JsonReader.json5(reader));
    }

    public static JsonObject.Mutable parseObject(Path path) throws IOException {
        return parseObject(JsonReader.json5(path));
    }

    private static JsonObject.Mutable parseObject(JsonReader reader) throws IOException {
        reader.beginObject();

        JsonObject.Mutable object = new MutableJsonObject();

        while (reader.hasNext() && reader.peek() == JsonToken.NAME) {
            object.put(reader.nextName(), parseElement(reader));
        }

        reader.endObject();

        return object;
    }

    public static JsonArray.Mutable parseArray(JsonReader reader) throws IOException {
        reader.beginArray();

        JsonArray.Mutable array = new MutableJsonArray();

        while (reader.hasNext() && reader.peek() != JsonToken.END_ARRAY) {
            array.add(parseElement(reader));
        }

        reader.endArray();

        return array;
    }

    public static JsonElement create() {
        return new JsonPrimitive();
    }

    public static JsonElement create(boolean value) {
        return new JsonPrimitive(value);
    }

    public static JsonElement create(int value) {
        return new JsonPrimitive(value);
    }

    public static JsonElement create(long value) {
        return new JsonPrimitive(value);
    }

    public static JsonElement create(double value) {
        return new JsonPrimitive(value);
    }

    public static JsonElement create(float value) {
        return new JsonPrimitive(value);
    }

    public static JsonElement create(String value) {
        return new JsonPrimitive(value);
    }

    public static JsonElement create(Number value) {
        return new JsonPrimitive(value);
    }
}
