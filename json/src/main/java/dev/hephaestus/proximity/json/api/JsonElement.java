package dev.hephaestus.proximity.json.api;

import dev.hephaestus.proximity.json.impl.ReferenceImpl;
import javafx.beans.Observable;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;

public interface JsonElement extends Observable {
    JsonElement copy();

    void write(Path path) throws IOException;

    void write(OutputStream stream) throws IOException;

    interface Reference {
        static Reference of(String key1, String... keys) {
            return new ReferenceImpl(key1, keys);
        }

        JsonElement resolve(JsonElement element);
    }
}
