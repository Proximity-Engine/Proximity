package dev.hephaestus.proximity.json.api;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;

public interface JsonElement {
    boolean isObject();

    boolean isArray();

    boolean isNull();

    boolean isBoolean();

    boolean isNumber();

    boolean isString();

    boolean asBoolean();

    JsonObject asObject();

    JsonArray asArray();

    int asInt();

    long asLong();

    float asFloat();

    double asDouble();

    String asString();

    void write(Path path) throws IOException;

    void write(OutputStream stream) throws IOException;
}
