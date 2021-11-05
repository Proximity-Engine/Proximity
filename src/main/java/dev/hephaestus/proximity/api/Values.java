package dev.hephaestus.proximity.api;

import dev.hephaestus.proximity.api.json.JsonArray;

public final class Values {
    public static final Value<Integer> ITEM_NUMBER = Value.createInteger("number");
    public static final Value<Integer> COUNT = Value.createInteger("count");
    public static final Value<JsonArray> PATH = Value.createArray("path");
    public static final Value<String> LIST_NAME = Value.createString("list_name");

    private Values() {
    }
}
