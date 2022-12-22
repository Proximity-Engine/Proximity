package dev.hephaestus.proximity.json.api;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;

public interface JsonElement {
    void write(Path path) throws IOException;

    void write(OutputStream stream) throws IOException;
}
