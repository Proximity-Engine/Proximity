package dev.hephaestus.proximity.plugins;

import dev.hephaestus.proximity.api.TaskScheduler;
import dev.hephaestus.proximity.api.json.JsonElement;
import dev.hephaestus.proximity.api.json.JsonPrimitive;
import dev.hephaestus.proximity.api.tasks.*;

import java.util.*;
import java.util.function.Function;

public final class TaskHandler implements TaskScheduler {
    private final Map<String, TaskDefinition<?, ?>> definitions;
    private final Map<String, List<Object>> tasks;
    private final Map<String, Map<String, Object>> namedTasks;

    public TaskHandler() {
        this(new HashMap<>(), new HashMap<>(), new HashMap<>());
    }

    public TaskHandler(Map<String, TaskDefinition<?, ?>> definitions, Map<String, List<Object>> tasks, Map<String, Map<String, Object>> namedTasks) {
        this.definitions = definitions;
        this.tasks = tasks;
        this.namedTasks = namedTasks;
    }

    public TaskHandler derive() {
        return new TaskHandler(
                this.definitions,
                new HashMap<>(this.tasks),
                new HashMap<>(this.namedTasks)
        );
    }

    public void register(TaskDefinition<?, ?> taskDefinition) {
        this.definitions.put(taskDefinition.getTagName(), taskDefinition);
    }

    public boolean contains(String taskName) {
        return this.definitions.containsKey(taskName);
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> getTasks(String taskName) {
        return (List<T>) this.tasks.getOrDefault(taskName, Collections.emptyList());
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> getTasks(TaskDefinition<T, ?> definition) {
        return (List<T>) this.tasks.getOrDefault(definition.getTagName(), Collections.emptyList());
    }

    @SuppressWarnings("unchecked")
    public <T> T getTask(String taskId, String name) {
        return this.namedTasks.containsKey(taskId)
                ? (T) this.namedTasks.get(taskId).get(name)
                : null;
    }

    @SuppressWarnings("unchecked")
    public <T> T getTask(TaskDefinition<T, ?> definition, String name) {
        return this.namedTasks.containsKey(definition.getTagName())
                ? (T) this.namedTasks.get(definition.getTagName()).get(name)
                : null;
    }

    @SuppressWarnings("unchecked")
    public <T, R> TaskDefinition<T, R> getTaskDefinition(String name) {
        return (TaskDefinition<T, R>) this.definitions.get(name);
    }

    public <T, R> void put(TaskDefinition<T, R> definition, T handler) {
        this.tasks.computeIfAbsent(definition.getTagName(), t -> new ArrayList<>()).add(handler);
    }

    public <T, R> void put(TaskDefinition<T, R> definition, String name, T handler) {
        this.namedTasks.computeIfAbsent(definition.getTagName(), t -> new LinkedHashMap<>()).put(name, handler);
    }

    public <T, R> void put(String tagName, Function<Object[], Object> handler) {
        TaskDefinition<T, R> definition = this.getTaskDefinition(tagName);

        this.put(definition, definition.from(handler));
    }

    public <T, R> void put(String tagName, String name, Function<Object[], Object> handler) {
        TaskDefinition<T, R> definition = this.getTaskDefinition(tagName);

        this.put(definition, name, definition.from(handler));
    }

    public static TaskHandler createDefault() {
        TaskHandler handler = new TaskHandler();

        handler.register(DataPreparation.DEFINITION);
        handler.register(DataFinalization.DEFINITION);
        handler.register(TemplateModification.DEFINITION);
        handler.register(TextFunction.DEFINITION);
        handler.register(AttributeModifier.DEFINITION);

        handler.put(AttributeModifier.DEFINITION, "join", (input, data) -> {
            if (input.isJsonArray()) {
                StringBuilder builder = new StringBuilder();

                for (JsonElement element : input.getAsJsonArray()) {
                    builder.append(element instanceof JsonPrimitive primitive && primitive.isString() ? primitive.getAsString() : element.toString());
                }

                return builder.toString();
            } else {
                return input.toString();
            }
        });

        return handler;
    }

    @Override
    public void submit(String taskId, Object handler) {
        this.tasks.computeIfAbsent(taskId, t -> new ArrayList<>()).add(handler);
    }

    @Override
    public void submit(String taskId, String name, Object handler) {
        this.namedTasks.computeIfAbsent(taskId, t -> new LinkedHashMap<>()).put(name, handler);
    }
}
