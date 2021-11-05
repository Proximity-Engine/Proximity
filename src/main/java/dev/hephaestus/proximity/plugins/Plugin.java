package dev.hephaestus.proximity.plugins;

import dev.hephaestus.proximity.api.json.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class Plugin {
    private final List<Consumer<JsonObject>> optionInitializers;

    private Plugin(List<Consumer<JsonObject>> optionInitializers) {
        this.optionInitializers = optionInitializers;
    }

    public void initialize(JsonObject json) {
        this.optionInitializers.forEach(consumer -> consumer.accept(json));
    }

    public static final class Builder {
        private final List<Consumer<JsonObject>> optionInitializers = new ArrayList<>();

        public Builder add(Consumer<JsonObject> optionInitializer) {
            this.optionInitializers.add(optionInitializer);
            return this;
        }

        public Plugin build() {
            return new Plugin(this.optionInitializers);
        }
    }
}
