package dev.hephaestus.proximity.scripting;

import dev.hephaestus.proximity.json.JsonArray;
import dev.hephaestus.proximity.json.JsonObject;

import java.util.List;
import java.util.Map;

public final class Context {
    private Context() {
    }

    public static String create(String layerId, Map<String, String> namedContexts, List<String> contextFlags) {

        JsonObject result = new JsonObject();

        result.addProperty("layer", layerId);
        JsonObject named = result.getAsJsonObject("named");
        JsonArray flags = result.getAsJsonArray("flags");

        for (var entry : namedContexts.entrySet()) {
            named.addProperty(entry.getKey(), entry.getValue());
        }

        for (var flag : contextFlags) {
            flags.add(flag);
        }

        return result.toString();
    }
}
