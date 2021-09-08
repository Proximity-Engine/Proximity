package dev.hephaestus.proximity.templates;

import dev.hephaestus.proximity.json.JsonObject;

public interface LayerFactory {
    Layer create(JsonObject card, int x, int y);
}
