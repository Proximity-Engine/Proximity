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
        JsonWriter writer = JsonWriter.json5(path);

        this.write(writer);
        writer.flush();
        writer.close();
    }

    @Override
    public void write(OutputStream stream) throws IOException {
        JsonWriter writer = JsonWriter.json5(new BufferedWriter(new OutputStreamWriter(stream)));

        this.write(writer);
        writer.flush();
        writer.close();
    }

    protected abstract void write(JsonWriter writer) throws IOException;
}
