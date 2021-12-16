package dev.hephaestus.proximity.cards;


import dev.hephaestus.proximity.api.json.JsonObject;
import dev.hephaestus.proximity.templates.TemplateSource;

import java.util.Objects;

public final class CardPrototype {
    private final String listName;
    private final String cardName;
    private final int number;
    private final JsonObject options;
    private final TemplateSource.Compound source;
    private final JsonObject overrides;

    private JsonObject data;

    public CardPrototype(String listName, String cardName, int number, JsonObject options, TemplateSource.Compound source, JsonObject overrides) {
        this.listName = listName;
        this.cardName = cardName;
        this.number = number;
        this.options = options;
        this.source = source;
        this.overrides = overrides;
    }

    public void setData(JsonObject data) {
        this.data = data;
    }

    public String listName() {
        return listName;
    }

    public String cardName() {
        return cardName;
    }

    public int number() {
        return number;
    }

    public JsonObject options() {
        return options;
    }

    public TemplateSource.Compound source() {
        return source;
    }

    public JsonObject overrides() {
        return overrides;
    }

    public JsonObject getData() {
        return this.data;
    }
}
