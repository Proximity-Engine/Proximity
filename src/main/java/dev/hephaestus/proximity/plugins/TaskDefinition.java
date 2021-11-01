package dev.hephaestus.proximity.plugins;

import dev.hephaestus.proximity.plugins.util.TaskParser;

import java.util.function.Function;

public abstract class TaskDefinition<T, R> implements TaskParser<T> {
    private final String tagName;

    public TaskDefinition(String tagName) {
        this.tagName = tagName;
    }

    public final String getTagName() {
        return this.tagName;
    }

    public abstract T from(Function<Object[], Object> fn);
    public abstract R interpretScriptResult(Object object);

    public static abstract class Void<T> extends TaskDefinition<T, java.lang.Void> {
        public Void(String tagName) {
            super(tagName);
        }

        @Override
        public final java.lang.Void interpretScriptResult(Object object) {
            return null;
        }
    }
}
