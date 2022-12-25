package dev.hephaestus.proximity.json.impl.json;

import dev.hephaestus.proximity.json.api.JsonNumber;
import org.quiltmc.json5.JsonWriter;

import java.io.IOException;

public class JsonNumberImpl extends JsonNumber implements AbstractJsonElement {
    public JsonNumberImpl() {
    }

    public JsonNumberImpl(int initialValue) {
        super(initialValue);
    }

    public JsonNumberImpl(long initialValue) {
        super(initialValue);
    }

    public JsonNumberImpl(float initialValue) {
        super(initialValue);
    }

    public JsonNumberImpl(double initialValue) {
        super(initialValue);
    }

    public JsonNumberImpl(Number initialValue) {
        super(initialValue);
    }

    @Override
    public void write(JsonWriter writer) throws IOException {
        writer.value(this.getValue());
    }

    @Override
    public JsonNumber copy() {
        return new JsonNumberImpl(this.getValue());
    }
}
