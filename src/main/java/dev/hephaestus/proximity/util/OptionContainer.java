package dev.hephaestus.proximity.util;

import java.util.HashMap;
import java.util.Map;

public interface OptionContainer {
    <T> T getOption(String name);
    Map<String, Object> getMap();

    final class Implementation implements OptionContainer {
        private final OptionContainer parent;
        private final Map<String, Object> options;

        public Implementation(OptionContainer parent, Map<String, Object> options) {
            this.parent = parent;
            this.options = options;
        }

        public Implementation(Map<String, Object> options) {
            this(null, options);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getOption(String name) {
            T result = (T) this.options.get(name);

            return result == null && this.parent != null ? this.parent.getOption(name) : result;
        }

        @Override
        public Map<String, Object> getMap() {
            return new HashMap<>(this.options);
        }
    }
}