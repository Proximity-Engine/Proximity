package dev.hephaestus.proximity.api;

import dev.hephaestus.proximity.plugins.TaskDefinition;

public interface TaskScheduler {
    void submit(String taskId, Object handler);

    default <T> void submit(TaskDefinition<T, ?> definition, T handler) {
        this.submit(definition.getTagName(), handler);
    }

    void submit(String taskId, String name, Object handler);

    default <T> void submit(TaskDefinition<T, ?> definition, String name, T handler) {
        this.submit(definition.getTagName(), name, handler);
    }
}
