package dev.hephaestus.proximity.json.impl;

import dev.hephaestus.proximity.json.api.JsonArray;
import dev.hephaestus.proximity.json.api.JsonElement;
import dev.hephaestus.proximity.json.api.JsonObject;

public class ReferenceImpl implements JsonElement.Reference {
    private final String[] keys;

    public ReferenceImpl(String key1, String... keys) {
        this.keys = new String[keys.length + 1];

        this.keys[0] = key1;

        System.arraycopy(keys, 0, this.keys, 1, keys.length);
    }

    @Override
    public JsonElement resolve(JsonElement element) {
        for (String key : this.keys) {
            if (element instanceof JsonObject object) {
                element = object.get(key);
            } else if (element instanceof JsonArray array) {
                element = array.get(Integer.parseInt(key));
            } else if (element == null) {
                return null;
            } else {
                throw new RuntimeException("Cannot index into element of type \"" + element.getClass().getSimpleName() + "\"");
            }
        }

        return element;
    }
}
