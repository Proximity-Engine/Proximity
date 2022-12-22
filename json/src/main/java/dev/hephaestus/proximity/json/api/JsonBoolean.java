package dev.hephaestus.proximity.json.api;

import javafx.beans.property.SimpleBooleanProperty;

public abstract class JsonBoolean extends SimpleBooleanProperty implements JsonElement {
    public JsonBoolean() {
    }

    public JsonBoolean(boolean initialValue) {
        super(initialValue);
    }
}
