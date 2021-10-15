package dev.hephaestus.proximity.scripting;

import dev.hephaestus.proximity.json.JsonArray;
import dev.hephaestus.proximity.json.JsonObject;
import org.graalvm.polyglot.HostAccess;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

public final class Context extends JsonObject {
    private final BiConsumer<String, Function<Object[], Object>> taskHandler;

    private Context(BiConsumer<String, Function<Object[], Object>> taskHandler) {
        this.taskHandler = taskHandler;
    }

    @HostAccess.Export
    public void submit(String step, Function<Object[], Object> task) {
        this.taskHandler.accept(step, task);
    }

    public static Context create(String layerId, Map<String, String> namedContexts, List<String> contextFlags, BiConsumer<String, Function<Object[], Object>> taskHandler) {
        Context result = new Context(taskHandler);

        result.addProperty("layer", layerId);
        JsonObject named = result.getAsJsonObject("named");
        JsonArray flags = result.getAsJsonArray("flags");

        for (var entry : namedContexts.entrySet()) {
            named.addProperty(entry.getKey(), entry.getValue());
        }

        for (var flag : contextFlags) {
            flags.add(flag);
        }

        return result;
    }
}
