package dev.hephaestus.proximity.json;

import org.graalvm.polyglot.HostAccess;
import org.quiltmc.json5.JsonReader;
import org.quiltmc.json5.JsonWriter;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;

public abstract class JsonElement {
    public static JsonElement parseElement(JsonReader reader) throws IOException {
        return switch (reader.peek()) {
            case END_ARRAY -> throw new UnsupportedOperationException("Unexpected end of array");
            case BEGIN_OBJECT -> JsonObject.parseObject(reader);
            case BEGIN_ARRAY -> JsonArray.parseArray(reader);
            case END_OBJECT -> throw new RuntimeException("Unexpected end of object");
            case NAME -> throw new RuntimeException("Unexpected name");
            case STRING -> new JsonPrimitive(reader.nextString());
            case NUMBER -> new JsonPrimitive(reader.nextNumber());
            case BOOLEAN -> new JsonPrimitive(reader.nextBoolean());
            case NULL -> consumeNull(reader);
            case END_DOCUMENT -> throw new RuntimeException("Unexpected end of file");
        };
    }

    private static JsonElement consumeNull(JsonReader reader) throws IOException {
        reader.nextNull();
        return JsonNull.INSTANCE;
    }

    public abstract JsonElement deepCopy();

    @HostAccess.Export
    public boolean isJsonArray() {
        return this instanceof JsonArray;
    }

    @HostAccess.Export
    public boolean isJsonObject() {
        return this instanceof JsonObject;
    }

    @HostAccess.Export
    public boolean isJsonPrimitive() {
        return this instanceof JsonPrimitive;
    }

    @HostAccess.Export
    public boolean isJsonNull() {
        return this instanceof JsonNull;
    }

    @HostAccess.Export
    public JsonObject getAsJsonObject() {
        if (isJsonObject()) {
            return (JsonObject) this;
        }
        throw new IllegalStateException("Not a JSON Object: " + this);
    }

    @HostAccess.Export
    public JsonArray getAsJsonArray() {
        if (isJsonArray()) {
            return (JsonArray) this;
        }
        throw new IllegalStateException("Not a JSON Array: " + this);
    }

    @HostAccess.Export
    public JsonPrimitive getAsJsonPrimitive() {
        if (isJsonPrimitive()) {
            return (JsonPrimitive) this;
        }
        throw new IllegalStateException("Not a JSON Primitive: " + this);
    }

    public boolean getAsBoolean() {
        throw new UnsupportedOperationException(getClass().getSimpleName());
    }

    public Number getAsNumber() {
        throw new UnsupportedOperationException(getClass().getSimpleName());
    }

    public String getAsString() {
        throw new UnsupportedOperationException(getClass().getSimpleName());
    }

    public double getAsDouble() {
        throw new UnsupportedOperationException(getClass().getSimpleName());
    }

    public float getAsFloat() {
        throw new UnsupportedOperationException(getClass().getSimpleName());
    }

    public long getAsLong() {
        throw new UnsupportedOperationException(getClass().getSimpleName());
    }

    public int getAsInt() {
        throw new UnsupportedOperationException(getClass().getSimpleName());
    }

    public byte getAsByte() {
        throw new UnsupportedOperationException(getClass().getSimpleName());
    }

    public short getAsShort() {
        throw new UnsupportedOperationException(getClass().getSimpleName());
    }

    @HostAccess.Export
    public String toString() {
        StringWriter stringWriter = new StringWriter();
        JsonWriter writer = JsonWriter.json(stringWriter);

        writer.setCompact();
        writer.setHtmlSafe(true);

        try {
            this.write(writer);
            writer.close();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        return stringWriter.toString();
    }

    protected abstract void write(JsonWriter writer) throws IOException;
}