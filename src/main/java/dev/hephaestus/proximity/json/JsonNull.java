package dev.hephaestus.proximity.json;

import org.quiltmc.json5.JsonWriter;

import java.io.IOException;

public final class JsonNull extends JsonElement {
    public static final JsonNull INSTANCE = new JsonNull();

    private JsonNull() {}

    @Override
    public JsonNull deepCopy() {
        return INSTANCE;
    }

    @Override
    protected void write(JsonWriter writer) throws IOException {
        writer.nullValue();
    }

    @Override
    public int hashCode() {
        return JsonNull.class.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return other == this;
    }
}