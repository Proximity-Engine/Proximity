package dev.hephaestus.proximity.json.api;

import javafx.beans.property.SimpleObjectProperty;

public abstract class JsonNumber extends SimpleObjectProperty<Number> implements JsonElement {
    public JsonNumber() {
    }

    public JsonNumber(int initialValue) {
        super(initialValue);
    }

    public JsonNumber(long initialValue) {
        super(initialValue);
    }

    public JsonNumber(float initialValue) {
        super(initialValue);
    }

    public JsonNumber(double initialValue) {
        super(initialValue);
    }

    public JsonNumber(Number initialValue) {
        super(initialValue);
    }
}
