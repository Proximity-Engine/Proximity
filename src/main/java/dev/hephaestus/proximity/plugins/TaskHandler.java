package dev.hephaestus.proximity.plugins;

import dev.hephaestus.proximity.api.TaskScheduler;
import dev.hephaestus.proximity.api.json.JsonElement;
import dev.hephaestus.proximity.api.json.JsonPrimitive;
import dev.hephaestus.proximity.api.tasks.*;
import dev.hephaestus.proximity.plugins.util.Artifact;
import dev.hephaestus.proximity.util.Result;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.*;
import java.util.function.Function;

public final class TaskHandler implements TaskScheduler {
    private final Set<Artifact> loadedPlugins = new HashSet<>();

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

    private Result<Void> loadPlugin(URL pluginUrl) {
        ClassLoader pluginClassLoader = new PluginClassLoader(pluginUrl);
        InputStream plugin = pluginClassLoader.getResourceAsStream("plugin.proximity.xml");

        if (plugin == null) {
            return Result.error("Plugin does not contain a plugin.proximity.xml file");
        }

        try {
            DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = documentBuilder.parse(plugin);
            Element root = document.getDocumentElement();
            NodeList tasksTags = root.getElementsByTagName("Tasks");

            for (int i = 0; i < tasksTags.getLength(); ++i) {
                Node n = tasksTags.item(i);

                if (n instanceof Element tasks) {
                    NodeList taskTags = tasks.getChildNodes();

                    for (int j = 0; j < taskTags.getLength(); ++j) {
                        n = taskTags.item(j);

                        if (n instanceof Element task && this.contains(task.getTagName())) {
                            String taskName = task.getTagName();
                            Result<?> parsingResult = this.definitions.get(taskName).parse(pluginClassLoader, task);

                            if (parsingResult.isError()) {
                                return parsingResult.unwrap();
                            }

                            if (task.hasAttribute("name")) {
                                this.namedTasks.computeIfAbsent(taskName, t -> new LinkedHashMap<>())
                                        .put(task.getAttribute("name"), parsingResult.get());
                            } else {
                                this.tasks.computeIfAbsent(taskName, t -> new ArrayList<>())
                                        .add(parsingResult.get());
                            }
                        }
                    }
                }
            }

            return Result.of(null);
        } catch (SAXException | ParserConfigurationException | IOException | ClassNotFoundException | NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            return Result.error("%s: %s", e.getClass().getSimpleName(), e.getMessage());
        }
    }

    public Result<Void> loadPlugin(Artifact artifact) {
        if (!this.loadedPlugins.contains(artifact)) {
            Result<URL> artifactUrl = artifact.getLatestMatchingVersionLocation();

            if (artifactUrl.isOk()) {
                Result<Void> result = this.loadPlugin(artifactUrl.get());

                if (result.isOk()) {
                    this.loadedPlugins.add(artifact);
                } else {
                    return result;
                }
            } else {
                return artifactUrl.unwrap();
            }
        }

        return Result.of(null);
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
