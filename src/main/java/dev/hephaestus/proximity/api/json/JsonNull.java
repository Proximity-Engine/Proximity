package dev.hephaestus.proximity.api.json;

import org.graalvm.polyglot.HostAccess;
import org.quiltmc.json5.JsonWriter;

import java.io.IOException;

public final class JsonNull extends JsonElement {
    @HostAccess.Export
    public static final JsonNull INSTANCE = new JsonNull();

    private JsonNull() {
    }

    @Override
    @HostAccess.Export
    public JsonNull deepCopy() {
        return INSTANCE;
    }

    @Override
    protected void write(JsonWriter writer) throws IOException {
        writer.nullValue();
    }

    @Override
    @HostAccess.Export
    public int hashCode() {
        return JsonNull.class.hashCode();
    }

    @Override
    @HostAccess.Export
    public boolean equals(Object other) {
        return other == this;
    }
}