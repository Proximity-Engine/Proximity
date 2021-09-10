package dev.hephaestus.proximity.cards;

import dev.hephaestus.proximity.json.JsonObject;
import dev.hephaestus.proximity.templates.Template;

public record Card(JsonObject representation, Template template) {
}
