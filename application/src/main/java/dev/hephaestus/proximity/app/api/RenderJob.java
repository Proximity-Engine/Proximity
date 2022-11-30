package dev.hephaestus.proximity.app.api;

import dev.hephaestus.proximity.json.api.JsonElement;
import dev.hephaestus.proximity.json.api.JsonObject;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class RenderJob {
    private final String plugin, name;
    private final Map<Option<?, ?, ?>, Object> options = new LinkedHashMap<>();

    protected RenderJob(String plugin, String name) {
        this.plugin = plugin;
        this.name = name;
    }

    public final String getName() {
        return this.name;
    }

    @SuppressWarnings("unchecked")
    public final <T, D extends RenderJob> T getOption(Option<T, ?, D> option) {
        if (!this.options.containsKey(option)) {
            this.options.put(option, option.getDefaultValue((D) this));
        }

        return (T) this.options.get(option);
    }

    public abstract JsonElement toJson();

    public final JsonElement toJsonImpl() {
        JsonObject.Mutable json = JsonObject.create();

        json.put("plugin", this.plugin);
        json.put("name", this.name);

        JsonObject.Mutable options = json.createObject("options");

        for (var entry : this.options.entrySet()) {
            //noinspection unchecked,rawtypes
            this.put((Map.Entry) entry, options);
        }

        json.put("data", this.toJson());

        return json;
    }

    private <T, D extends RenderJob> void put(Map.Entry<Option<T, ?, D>, Object> entry, JsonObject.Mutable json) {
        var option = entry.getKey();

        //noinspection unchecked
        json.put(option.getId(), option.toJson(option.getValue((D) this)));
    }
}
