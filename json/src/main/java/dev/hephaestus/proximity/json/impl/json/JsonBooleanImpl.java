package dev.hephaestus.proximity.json.impl.json;

import dev.hephaestus.proximity.json.api.JsonBoolean;
import org.quiltmc.json5.JsonWriter;

import java.io.IOException;

public class JsonBooleanImpl extends JsonBoolean implements AbstractJsonElement {
    public JsonBooleanImpl() {
    }

    public JsonBooleanImpl(boolean initialValue) {
        super(initialValue);
    }

    @Override
    public void write(JsonWriter writer) throws IOException {
        writer.value(this.getValue());
    }

    @Override
    public JsonBoolean copy() {
        return new JsonBooleanImpl(this.getValue());
    }
}
