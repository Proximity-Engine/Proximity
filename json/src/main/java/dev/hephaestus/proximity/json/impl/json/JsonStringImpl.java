package dev.hephaestus.proximity.json.impl.json;

import dev.hephaestus.proximity.json.api.JsonString;
import org.quiltmc.json5.JsonWriter;

import java.io.IOException;

public class JsonStringImpl extends JsonString implements AbstractJsonElement {
    public JsonStringImpl() {
    }

    public JsonStringImpl(String initialValue) {
        super(initialValue);
    }

    @Override
    public void write(JsonWriter writer) throws IOException {
        writer.value(this.getValue());
    }

    @Override
    public JsonString copy() {
        return new JsonStringImpl(this.getValue());
    }
}
