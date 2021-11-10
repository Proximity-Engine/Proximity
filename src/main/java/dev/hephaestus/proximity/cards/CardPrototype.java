package dev.hephaestus.proximity.cards;


import dev.hephaestus.proximity.api.json.JsonObject;
import dev.hephaestus.proximity.templates.TemplateSource;

public record CardPrototype(String listName, String cardName, int number, JsonObject options, TemplateSource.Compound source, JsonObject overrides) {
}
