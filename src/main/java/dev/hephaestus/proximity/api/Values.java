package dev.hephaestus.proximity.api;

import dev.hephaestus.proximity.api.json.JsonArray;

public final class Values {
    public static final Value<Integer> ITEM_NUMBER = Value.createInteger("number");
    public static final Value<Integer> COUNT = Value.createInteger("count");
    public static final Value<JsonArray> PATH = Value.createArray("path");
    public static final Value<String> LIST_NAME = Value.createString("list_name");
    public static final Value<Boolean> HELP = Value.createBoolean("options", "help");
    public static final Value<Boolean> DEBUG = Value.createBoolean("options" , "debug");

    private Values() {
    }
}
