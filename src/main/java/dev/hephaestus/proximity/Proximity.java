package dev.hephaestus.proximity;

import com.github.yuchi.semver.Version;
import dev.hephaestus.proximity.api.DataSet;
import dev.hephaestus.proximity.api.Values;
import dev.hephaestus.proximity.api.json.JsonElement;
import dev.hephaestus.proximity.api.json.JsonObject;
import dev.hephaestus.proximity.api.tasks.DataFinalization;
import dev.hephaestus.proximity.api.tasks.DataPreparation;
import dev.hephaestus.proximity.cards.CardPrototype;
import dev.hephaestus.proximity.mtg.MTGValues;
import dev.hephaestus.proximity.plugins.Plugin;
import dev.hephaestus.proximity.plugins.PluginHandler;
import dev.hephaestus.proximity.plugins.TaskHandler;
import dev.hephaestus.proximity.plugins.util.Artifact;
import dev.hephaestus.proximity.templates.LayerRegistry;
import dev.hephaestus.proximity.templates.RemoteFileSource;
import dev.hephaestus.proximity.templates.TemplateSource;
import dev.hephaestus.proximity.util.*;
import dev.hephaestus.proximity.xml.LayerRenderer;
import dev.hephaestus.proximity.xml.RenderableData;
import dev.hephaestus.proximity.xml.XMLUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quiltmc.json5.JsonReader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.imageio.*;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

public final class Proximity {
    public static Logger LOG = LogManager.getLogger("Proximity");
    public static final Version VERSION;

    private static final Collection<String> WARNED_TEMPLATES = new ConcurrentSkipListSet<>();

    static {
        Version version = Version.from(Proximity.class.getPackage().getImplementationVersion(), false);

        LOG.info("Proximity version: {}", version);

        if (version == null) {
            Proximity.LOG.warn("Proximity version is null. If you're not running in a dev environment, something is wrong!");
        }

        VERSION = version;
    }

    private final JsonObject options;
    private final TaskHandler taskHandler;
    private final PluginHandler pluginHandler;
    private final LayerRegistry layers;
    private final RemoteFileCache cache;
    private final HashMap<String, Result<JsonObject>> cardInfo = new HashMap<>();
    private final HashMap<String, JsonObject> setInfo = new HashMap<>();

    private long lastScryfallRequest = 0;

    public Proximity(JsonObject options, TaskHandler taskHandler, PluginHandler pluginHandler, LayerRegistry layers) {
        this.options = options;
        this.taskHandler = taskHandler;
        this.pluginHandler = pluginHandler;
        this.layers = layers;

        try {
            this.cache = RemoteFileCache.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public RemoteFileCache getRemoteFileCache() {
        return this.cache;
    }

    public void run(Deque<CardPrototype> prototypes) {
        long startTime = System.currentTimeMillis();

        Result<?> result = this.getCardInfo(prototypes)
                .then(this::renderAndSave)
                .ifError(LOG::error);

        if (!result.isError()) {
            LOG.info("Done! Took {}ms", System.currentTimeMillis() - startTime);
        }
    }

    private Result<Deque<RenderableData>> getCardInfo(Deque<CardPrototype> prototypes) {
        LOG.info("Fetching info for {} cards...", prototypes.size());
        Deque<RenderableData> cards = new ArrayDeque<>();

        int i = 1;
        int prototypeCountLength = Integer.toString(prototypes.size()).length();
        long totalTime = System.currentTimeMillis();

        List<CardPrototype> found = new ArrayList<>(prototypes.size());

        for (CardPrototype prototype : prototypes) {
           long time = System.currentTimeMillis();

           // We respect Scryfall's wishes and wait between 50-100 seconds between requests.
           if (time - this.lastScryfallRequest < 50) {
               try {
                   LOG.debug("Sleeping for {}ms", time - this.lastScryfallRequest);
                   Thread.sleep(time - this.lastScryfallRequest);
               } catch (InterruptedException e) {
                   e.printStackTrace();
               }
           }

           getCardInfo(prototype)
                   .ifError(LOG::warn)
                   .ifPresent(raw -> {
                       prototype.setData(raw);
                       found.add(prototype);
                   });

            System.out.printf("%" + prototypeCountLength + "d/%d\r", i++, prototypes.size());
        }

        LOG.info("Successfully found {} cards. Took {}ms", found.size(), System.currentTimeMillis() - totalTime);

        totalTime = System.currentTimeMillis();

        Deque<String> errors = new ConcurrentLinkedDeque<>();

        LOG.info("Processing {} cards", found.size());

        for (CardPrototype prototype : found) {
            this.processCard(prototype, cards::add, errors::add);
        }

        LOG.info("Successfully processed {} cards. Took {}ms", cards.size(), System.currentTimeMillis() - totalTime);

        return errors.isEmpty()
                ? Result.of(cards)
                : Result.error("Error rendering cards:\n\t%s", String.join("\n\t", errors));
    }

    private void processCard(CardPrototype prototype, Consumer<RenderableData> dataConsumer, Consumer<String> errorConsumer) {
        prototype.getData().getAsJsonObject("proximity", "options")
                .copyAll(this.options)
                .copyAll(prototype.options());

        Values.LIST_NAME.set(prototype.getData(), prototype.listName());

        for (int j = 0; j < prototype.options().getAsInt("count"); ++j) {
            int finalJ = j + prototype.number();
            Values.ITEM_NUMBER.set(prototype.getData(), finalJ);

            if (prototype.source().exists("template.xml")) {
                XMLUtil.load(prototype.source(), "template.xml").ifError(LOG::warn)
                        .then(this::checkVersion)
                        .then(root -> this.loadPluginsAndTasks(prototype.source(), root, false))
                        .then((List<Plugin> plugins) -> {
                            plugins.forEach(plugin -> plugin.initialize(prototype.getData()));
                            return this.runScripts(prototype.getData(), prototype.overrides());
                        })
                        .then((List<JsonObject> list) -> {
                            list.forEach(card -> XMLUtil.load(prototype.source(), "template.xml").ifError(LOG::warn)
                                    .then(e -> this.resolveResources(e, prototype.source()))
                                    .then(e -> this.resolveImports(e, prototype.source()))
                                    .then(root -> Result.of(new RenderableData(this, prototype.source(), root, card)))
                                    .then(renderable -> {
                                        dataConsumer.accept(renderable);
                                        return Result.of((Void) null);
                                    })
                            );

                            return Result.of((Void) null);
                        }).ifError(errorConsumer::accept);
            } else {
                LOG.error("template.xml not found for template {}", prototype.source().getTemplateName());
            }
        }
    }

    private Result<Element> checkVersion(Element root) {
        if (VERSION != null) {
            Version templateProximityVersion = new Version(root.getAttribute("proximity_version"));
            String name = root.getAttribute("name");

            if (templateProximityVersion.getMajor() != VERSION.getMajor() || templateProximityVersion.getMinor() != VERSION.getMinor() && !WARNED_TEMPLATES.contains(name)) {
                Proximity.LOG.warn("Template '{}' created for Proximity version '{}'. There may be errors.", name, templateProximityVersion);
                WARNED_TEMPLATES.add(name);
            }
        }

        return Result.of(root);
    }

    private Result<Element> resolveResources(Element root, TemplateSource.Compound source) {
        NodeList resourceList = root.getElementsByTagName("Resources");

        for (int i = 0; i < resourceList.getLength(); ++i) {
            Node r = resourceList.item(i);

            if (r instanceof Element) {
                NodeList resources = r.getChildNodes();

                for (int j = 0; j < resources.getLength(); ++j) {
                    r = resources.item(j);

                    if (r instanceof Element resource) {
                        String location = resource.getAttribute("location") + "/" + resource.getAttribute("version");

                        //noinspection SwitchStatementWithTooFewBranches May add more kinds of resources later
                        switch (resource.getAttribute("type")) {
                            case "assets" -> {
                                if (cache != null) {
                                    source.wrapped.add(new RemoteFileSource(cache, location));
                                }
                            }
                        }
                    }
                }
            }
        }

        return Result.of(root);
    }

    private Result<Element> resolveImports(Element root, TemplateSource source) {
        Node firstChild = root.getFirstChild();
        Document document = root.getOwnerDocument();
        NodeList importsList = root.getElementsByTagName("Imports");

        for (int i = 0; i < importsList.getLength(); ++i) {
            Node n = importsList.item(i);

            if (n instanceof Element) {
                NodeList imports = n.getChildNodes();

                for (int j = 0; j < imports.getLength(); ++j) {
                    n = imports.item(j);

                    if (n instanceof Element element) {
                        String src = element.getAttribute("src") + ".xml";

                        XMLUtil.load(source, src)
                                .ifError(LOG::warn)
                                .then(imported -> {
                                    NodeList children = imported.getChildNodes();

                                    for (int k = 0; k < children.getLength(); ++k) {
                                        Node m = children.item(k);

                                        if (m instanceof Element e) {
                                            root.insertBefore(document.adoptNode(e), firstChild);
                                        }
                                    }

                                    return Result.of((Void) null);
                                });
                    }
                }
            }
        }

        return Result.of(root);
    }

    private Result<List<Plugin>> loadPluginsAndTasks(TemplateSource source, Element root, boolean help) {
        NodeList pluginBlocks = root.getElementsByTagName("Plugins");
        List<Plugin> plugins = new ArrayList<>();

        for (int i = 0; i < pluginBlocks.getLength(); ++i) {
            Node n = pluginBlocks.item(i);

            if (n instanceof Element e) {
                NodeList imports = e.getElementsByTagName("Import");

                for (int j = 0; j < imports.getLength(); ++j) {
                    n = imports.item(j);

                    if (n instanceof Element importElement) {
                        String repository = importElement.getAttribute("repository");
                        String group = importElement.getAttribute("group");
                        String artifact = importElement.getAttribute("artifact");
                        String versionRange = importElement.getAttribute("version");

                        Result<Plugin> result = this.pluginHandler.loadPlugin(Artifact.create(
                                repository, group, artifact, versionRange
                        ), this.taskHandler, help);

                        if (result.isError()) {
                            return result.unwrap();
                        } else {
                            plugins.add(result.get());
                        }
                    }
                }
            }
        }

        NodeList tasksTags = root.getElementsByTagName("Tasks");

        for (int i = 0; i < tasksTags.getLength(); ++i) {
            Node n = tasksTags.item(i);

            if (n instanceof Element tasks) {
                NodeList taskTags = tasks.getChildNodes();

                for (int j = 0; j < taskTags.getLength(); ++j) {
                    n = taskTags.item(j);

                    if (n instanceof Element task && this.taskHandler.contains(task.getTagName())) {
                        Function<Object[], Object> function = ScriptingUtil.getFunction(source, task.getAttribute("location"));
                        String taskName = task.getTagName();

                        if (task.hasAttribute("name")) {
                            this.taskHandler.put(taskName, task.getAttribute("name"), function);
                        } else {
                            this.taskHandler.put(taskName, function);
                        }
                    }
                }
            }
        }

        return Result.of(plugins);
    }

    public Result<List<JsonObject>> runScripts(JsonObject raw, JsonObject overrides) {
        List<JsonObject> list = new ArrayList<>();

        list.add(raw);

        DataSet data = new DataSet(list);
        List<DataPreparation> preparations = this.taskHandler.getTasks(DataPreparation.DEFINITION);

        for (var task : preparations) {
            task.apply(this.taskHandler, data, overrides);
        }

        List<DataFinalization> finalizations = this.taskHandler.getTasks(DataFinalization.DEFINITION);

        for (var task : finalizations) {
            task.apply();
        }

        return Result.of(list);
    }

    private Result<JsonObject> getCardInfo(CardPrototype prototype) {
        StringBuilder builder = new StringBuilder("https://api.scryfall.com/cards/named?");

        builder.append("fuzzy=").append(URLEncoder.encode(prototype.cardName(), StandardCharsets.UTF_8));

        if (prototype.options().has("set_code")) {
            if (prototype.options().has("collector_number")) {
                builder = new StringBuilder("https://api.scryfall.com/cards/")
                        .append(prototype.options().getAsString("set_code").toLowerCase(Locale.ROOT))
                        .append("/")
                        .append(prototype.options().getAsString("collector_number"));

                if (prototype.options().has("lang")) {
                    builder.append("/").append(prototype.options().getAsString("lang"));
                }
            } else {
                builder.append("&set=").append(prototype.options().getAsString("set_code"));
            }
        }

        String string = builder.toString();

        return this.cardInfo.computeIfAbsent(string, s -> {
            try {
                JsonObject card = JsonObject.parseObject(JsonReader.json5(this.cache.compute(URI.create(s), uri -> {
                    HttpClient client = HttpClient.newHttpClient();
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(uri)
                            .GET()
                            .build();

                    this.lastScryfallRequest = System.currentTimeMillis();

                    try {
                        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                        if (response.statusCode() == 200) {
                            return new ByteArrayInputStream(response.body().getBytes(StandardCharsets.UTF_8));
                        } else {
                            StringBuilder message = new StringBuilder("Could not find card ").append(prototype.cardName());

                            if (prototype.options().has("set_code")) {
                                message.append(" (")
                                        .append(prototype.options().getAsString("set_code").toUpperCase(Locale.ROOT))
                                        .append(")");

                                if (prototype.options().has("collector_number")) {
                                    message.append(" #")
                                            .append(prototype.options().getAsString("collector_number").toUpperCase(Locale.ROOT));
                                }
                            }

                            JsonObject body = JsonObject.parseObject(JsonReader.json(response.body()));
                            String details = "[" + response.statusCode() + "] " + (body.has("details") ? body.getAsString("details") : "");
                            LOG.error("{}: {}", message.toString(), details);

                            return null;
                        }
                    } catch (Exception e) {
                        return null;
                    }
                })));

                long time = System.currentTimeMillis();

                if (time - this.lastScryfallRequest < 50) {
                    try {
                        LOG.debug("Sleeping for {}ms", time - this.lastScryfallRequest);
                        Thread.sleep(time - this.lastScryfallRequest);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                JsonObject set = JsonObject.parseObject(JsonReader.json5(this.cache.compute(URI.create("https://api.scryfall.com/sets/" + card.getAsString("set")), uri -> {
                    HttpClient client = HttpClient.newHttpClient();
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(uri)
                            .GET()
                            .build();

                    this.lastScryfallRequest = System.currentTimeMillis();

                    try {
                        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                        if (response.statusCode() == 200) {
                            return new ByteArrayInputStream(response.body().getBytes(StandardCharsets.UTF_8));
                        } else {
                            JsonObject body = JsonObject.parseObject(JsonReader.json(response.body()));
                            String details = "[" + response.statusCode() + "] " + (body.has("details") ? body.getAsString("details") : "");
                            LOG.error("{}: {}", "Could not find set " + uri, details);

                            return null;
                        }
                    } catch (Exception e) {
                        return null;
                    }
                })));

                card.add(new String[] {"proximity", "set"}, set.deepCopy());

                return Result.of(card);
            } catch (IOException e) {
                return Result.error("Failed to get info for '%s': %s", prototype.cardName(), ExceptionUtil.getErrorMessage(e));
            }
        });
    }

    private Result<AtomicInteger> renderAndSave(Deque<RenderableData> cards) {
        int countStrLen = Integer.toString(cards.size()).length();
        int threadCount = Integer.min(options.has("threads") ? Integer.parseInt(options.getAsString("threads")) : 10, cards.size());

        if (threadCount > 0) {
            AtomicInteger finishedCards = new AtomicInteger();
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            Deque<String> errors = new ConcurrentLinkedDeque<>();

            LOG.info("Rendering {} cards on {} threads", cards.size(), threadCount);

            for (RenderableData card : cards) {
                if (Values.DEBUG.get(card)) {
                    LOG.debug(card.toString());
                }

                if (threadCount > 1) {
                    //                    this.render(card, finishedCards, errors, countStrLen, finalI, cards.size());
                    executor.submit(() -> this.render(card, finishedCards, errors, countStrLen, cards.size()));
                } else {
                    this.render(card, finishedCards, errors, countStrLen, cards.size());
                }

            }

            try {
                executor.shutdown();

                //noinspection StatementWithEmptyBody
                while (!executor.awaitTermination(Integer.MAX_VALUE, TimeUnit.NANOSECONDS)) {

                }

                return errors.isEmpty()
                        ? Result.of(finishedCards)
                        : Result.error("Error rendering cards:\n\t%s", String.join("\n\t", errors));
            } catch (InterruptedException e) {
                return Result.error(ExceptionUtil.getErrorMessage(e));
            }
        }

        return Result.error("No cards to render");
    }

    private void render(RenderableData card, AtomicInteger finishedCards, Deque<String> errors, int countStrLen, int cardCount) {
        String name = card.getName();

        if (Values.OVERWRITE.exists(card) && !Values.OVERWRITE.get(card) && Files.exists(card.getPath())) {
            LOG.info(String.format("%" + countStrLen + "d/%" + countStrLen + "d           %-55s {}PASS{}", finishedCards.get(), cardCount, name), Logging.ANSI_YELLOW, Logging.ANSI_RESET);
            return;
        }

        long cardTime = System.currentTimeMillis();

        try {
            BufferedImage image = new BufferedImage(card.getWidth(), card.getHeight(), BufferedImage.TYPE_INT_ARGB);

            Result<Void> result = card.render(new StatefulGraphics(image)).ifError(errors::add);

            if (result.isOk()) {
                this.save(image, card.getPath());

                finishedCards.incrementAndGet();
                LOG.info(String.format("%" + countStrLen + "d/%" + countStrLen + "d  %5dms  %-55s {}SAVED{}", finishedCards.get(), cardCount, System.currentTimeMillis() - cardTime, name), Logging.ANSI_GREEN, Logging.ANSI_RESET);
            } else {
                LOG.error(String.format("%" + countStrLen + "d/%" + countStrLen + "d  %5dms  %-55s {}FAILED{}", finishedCards.get(), cardCount, System.currentTimeMillis() - cardTime, name), Logging.ANSI_RED, Logging.ANSI_RESET);
            }
        } catch (Throwable throwable) {
            LOG.error(String.format("%" + countStrLen + "d/%" + countStrLen + "d  %5dms  %-55s {}FAILED{}", finishedCards.get(), cardCount, System.currentTimeMillis() - cardTime, name), Logging.ANSI_RED, Logging.ANSI_RESET);
            LOG.error(ExceptionUtil.getErrorMessage(throwable));

            for (StackTraceElement element : throwable.getStackTrace()) {
                LOG.debug(element);
            }
        }
    }

    private void save(BufferedImage image, Path path) throws Throwable {
        if (!Files.isDirectory(path.getParent())) {
            Files.createDirectories(path.getParent());
        }

        ImageWriter writer = ImageIO.getImageWritersByFormatName("png").next();

        ImageWriteParam param = writer.getDefaultWriteParam();
        ImageTypeSpecifier typeSpecifier = ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_ARGB);
        IIOMetadata metadata = writer.getDefaultImageMetadata(typeSpecifier, param);

        IIOMetadataNode discriminate = new IIOMetadataNode("tEXtEntry");
        Random random = new Random();
        byte[] bytes = new byte[128];

        discriminate.setAttribute("keyword", "ProximityDiscriminate");
        random.nextBytes(bytes);
        discriminate.setAttribute("value", new String(Base64.getEncoder().encode(bytes)));

        IIOMetadataNode text = new IIOMetadataNode("tEXt");
        text.appendChild(discriminate);

        IIOMetadataNode root = new IIOMetadataNode("javax_imageio_png_1.0");
        root.appendChild(text);

        metadata.mergeTree("javax_imageio_png_1.0", root);

        OutputStream stream = Files.newOutputStream(path);
        ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(stream);

        writer.setOutput(imageOutputStream);
        writer.write(metadata, new IIOImage(image, null, metadata), param);
        imageOutputStream.close();
        stream.close();
    }

    public Map<String, LayerRenderer> createLayerRenderers(RenderableData data) {
        return this.layers.create(data);
    }

    public TaskHandler getTaskHandler() {
        return this.taskHandler;
    }

    public void help(TemplateSource.Compound source) {
        JsonObject json = new JsonObject();

        Values.HELP.set(json, true);

        if (source.exists("template.xml")) {
            XMLUtil.load(source, "template.xml").ifError(LOG::warn)
                    .then(root -> {
                        this.loadPluginsAndTasks(source, root, true);
                        new RenderableData(this, source, root, json).parseOptions();
                        return null;
                    });
        } else {
            LOG.warn("template.xml not found for template {}", source.getTemplateName());
        }
    }
}
