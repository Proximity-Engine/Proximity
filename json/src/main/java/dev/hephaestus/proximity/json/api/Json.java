package dev.hephaestus.proximity.json.api;

import dev.hephaestus.proximity.json.impl.json.*;
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
            case STRING -> new JsonStringImpl(reader.nextString());
            case NUMBER -> new JsonNumberImpl(reader.nextNumber());
            case BOOLEAN -> new JsonBooleanImpl(reader.nextBoolean());
            case NULL -> {
                reader.nextNull();
                yield null;
            }
            case END_DOCUMENT -> throw new RuntimeException("Unexpected end of file");
        };
    }

    public static JsonObject parseObject(InputStream inputStream) throws IOException {
        return parseObject(JsonReader.json5(new InputStreamReader(inputStream)));
    }

    public static JsonObject parseObject(Reader reader) throws IOException {
        return parseObject(JsonReader.json5(reader));
    }

    public static JsonObject parseObject(Path path) throws IOException {
        return parseObject(JsonReader.json5(path));
    }

    private static JsonObject parseObject(JsonReader reader) throws IOException {
        reader.beginObject();

        JsonObject object = new JsonObjectImpl();

        while (reader.hasNext() && reader.peek() == JsonToken.NAME) {
            object.put(reader.nextName(), parseElement(reader));
        }

        reader.endObject();

        return object;
    }

    public static JsonArray parseArray(JsonReader reader) throws IOException {
        reader.beginArray();

        JsonArray array = new JsonArrayImpl();

        while (reader.hasNext() && reader.peek() != JsonToken.END_ARRAY) {
            array.add(parseElement(reader));
        }

        reader.endArray();

        return array;
    }

    public static JsonBoolean create(boolean value) {
        return new JsonBooleanImpl(value);
    }

    public static JsonNumber create(int value) {
        return new JsonNumberImpl(value);
    }

    public static JsonNumber create(long value) {
        return new JsonNumberImpl(value);
    }

    public static JsonNumber create(double value) {
        return new JsonNumberImpl(value);
    }

    public static JsonNumber create(float value) {
        return new JsonNumberImpl(value);
    }

    public static JsonString create(String value) {
        return new JsonStringImpl(value);
    }
}
