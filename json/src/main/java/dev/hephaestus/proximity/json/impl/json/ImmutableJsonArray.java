package dev.hephaestus.proximity.json.impl.json;

import com.google.common.collect.ImmutableList;
import dev.hephaestus.proximity.json.api.JsonElement;

public final class ImmutableJsonArray extends AbstractJsonArray<ImmutableList<JsonElement>> {
    ImmutableJsonArray(ImmutableList<JsonElement> values) {
        super(values);
    }
}
