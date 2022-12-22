package dev.hephaestus.proximity.json.impl.json;

import dev.hephaestus.proximity.json.api.JsonElement;
import org.quiltmc.json5.JsonWriter;

import java.io.*;
import java.nio.file.Path;

public interface AbstractJsonElement extends JsonElement {
    @Override
    default void write(Path path) throws IOException {
        JsonWriter writer = JsonWriter.json5(path);

        this.write(writer);
        writer.flush();
        writer.close();
    }

    @Override
    default void write(OutputStream stream) throws IOException {
        JsonWriter writer = JsonWriter.json5(new BufferedWriter(new OutputStreamWriter(stream)));

        this.write(writer);
        writer.flush();
        writer.close();
    }

    void write(JsonWriter writer) throws IOException;
}
