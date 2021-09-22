package dev.hephaestus.proximity.util;

import dev.hephaestus.proximity.json.JsonElement;
import dev.hephaestus.proximity.json.JsonNull;
import dev.hephaestus.proximity.json.JsonPrimitive;
import org.jetbrains.annotations.Nullable;

public class ParsingUtil {
    public static JsonElement parseStringValue(@Nullable String value) {
        if (value != null) {
            if (value.startsWith("\"") && value.endsWith("\"")) {
                value = value.substring(1, value.length() - 1);
            }
        }

        if (value == null) {
            return JsonNull.INSTANCE;
        } else if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            return new JsonPrimitive(Boolean.parseBoolean(value));
        } else {
            return new JsonPrimitive(value);
        }
    }
}
