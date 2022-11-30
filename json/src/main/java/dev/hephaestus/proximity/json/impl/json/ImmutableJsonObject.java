package dev.hephaestus.proximity.json.impl.json;

import com.google.common.collect.ImmutableMap;
import dev.hephaestus.proximity.json.api.JsonElement;

public class ImmutableJsonObject extends AbstractJsonObject<ImmutableMap<String, JsonElement>> {
    ImmutableJsonObject(ImmutableMap<String, JsonElement> values) {
        super(values);
    }
}
