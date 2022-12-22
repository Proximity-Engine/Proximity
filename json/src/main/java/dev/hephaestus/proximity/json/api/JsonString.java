package dev.hephaestus.proximity.json.api;

import javafx.beans.property.SimpleStringProperty;

public abstract class JsonString extends SimpleStringProperty implements JsonElement {
    public JsonString() {
    }

    public JsonString(String initialValue) {
        super(initialValue);
    }

    public static String get(JsonElement element) {
        return ((JsonString) element).get();
    }
}
