package dev.hephaestus.proximity.json.impl.json;

import dev.hephaestus.proximity.json.api.JsonElement;
import org.quiltmc.json5.JsonWriter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Path;

public abstract class AbstractJsonElement implements JsonElement {
    @Override
    public void write(Path path) throws IOException {
        this.write(JsonWriter.json5(path));
    }

    @Override
    public void write(OutputStream stream) throws IOException {
        this.write(JsonWriter.json5(new BufferedWriter(new OutputStreamWriter(stream))));
    }

    protected abstract void write(JsonWriter writer) throws IOException;
}
