package dev.hephaestus.proximity;

import dev.hephaestus.proximity.cards.CardPrototype;
import dev.hephaestus.proximity.cards.layers.*;
import dev.hephaestus.proximity.json.JsonElement;
import dev.hephaestus.proximity.json.JsonObject;
import dev.hephaestus.proximity.scripting.Context;
import dev.hephaestus.proximity.scripting.ScriptingUtil;
import dev.hephaestus.proximity.templates.RemoteFileSource;
import dev.hephaestus.proximity.templates.TemplateSource;
import dev.hephaestus.proximity.util.*;
import dev.hephaestus.proximity.xml.LayerRenderer;
import dev.hephaestus.proximity.xml.RenderableCard;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.graalvm.polyglot.HostAccess;
import org.quiltmc.json5.JsonReader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.imageio.ImageIO;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
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
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public final class Proximity {
    public static Logger LOG = LogManager.getLogger("Proximity");

    private final JsonObject options;
    private final HashMap<String, Result<JsonObject>> cardInfo = new HashMap<>();

    static {
        LayerRenderer.register(new ArtLayerRenderer(), "ArtLayer");
        LayerRenderer.register(new FillLayerRenderer(), "Fill", "SpacingLayer", "Spacer");
        LayerRenderer.register(new ForkLayerRenderer(), "Fork");
        LayerRenderer.register(new LayerGroupRenderer(), "Group", "main");
        LayerRenderer.register(new ImageLayerRenderer(), "ImageLayer");
        LayerRenderer.register(new LayerSelectorRenderer(), "Selector", "flex");
        LayerRenderer.register(new SquishBoxRenderer(), "SquishBox");
        LayerRenderer.register(new TextLayerRenderer(), "TextLayer");
        LayerRenderer.register(new SVGLayerRenderer(), "SVG");
        LayerRenderer.register(new NoiseLayerRenderer(), "Noise");

        LayerRenderer.register(new LayoutElementRenderer("x", "y",
                Rectangle2D::getWidth,
                Rectangle2D::getHeight
        ), "HorizontalLayout");

        LayerRenderer.register(new LayoutElementRenderer("y", "x",
                Rectangle2D::getHeight,
                Rectangle2D::getWidth
        ), "VerticalLayout");
    }

    public Proximity(JsonObject options) {
        this.options = options;
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

    private Result<Deque<RenderableCard>> getCardInfo(Deque<CardPrototype> prototypes) {
        LOG.info("Fetching info for {} cards...", prototypes.size());

        long lastRequest = 0;

       Deque<RenderableCard> cards = new ArrayDeque<>();

        RemoteFileCache cache;

        try {
             cache = RemoteFileCache.load();
        } catch (IOException e) {
            return Result.error("Failed to load cache: %s" ,e.getMessage());
        }

        int i = 1;
       int prototypeCountLength = Integer.toString(prototypes.size()).length();
       long totalTime = System.currentTimeMillis();

       for (CardPrototype prototype : prototypes) {
           long time = System.currentTimeMillis();

           // We respect Scryfall's wishes and wait between 50-100 seconds between requests.
           if (time - lastRequest < 50) {
               try {
                   LOG.debug("Sleeping for {}ms", time - lastRequest);
                   Thread.sleep(time - lastRequest);
               } catch (InterruptedException e) {
                   e.printStackTrace();
               }
           }

           lastRequest = System.currentTimeMillis();

           getCardInfo(prototype)
                   .ifPresent(raw -> {
                       for (int j = 0; j < prototype.options().getAsInt("count"); ++j) {
                           int finalJ = j + prototype.number();

                           if (prototype.source().exists("template.xml")) {
                               XMLUtil.load(prototype.source(), "template.xml").ifError(LOG::warn)
                                       .then(root -> Result.of(this.runInitScripts(raw, prototype.source(), root, finalJ, prototype.options(), prototype.overrides())))
                                       .then((List<JsonObject> list) -> {
                                           list.forEach(card -> XMLUtil.load(prototype.source(), "template.xml").ifError(LOG::warn)
                                                   .then(e -> this.resolveResources(e, prototype.source(), cache))
                                                   .then(e -> this.resolveImports(e, prototype.source()))
                                                   .then(root -> Result.of(new RenderableCard(prototype.source(), root, card)))
                                                   .then(renderable -> {
                                                       cards.add(renderable);
                                                       return Result.of((Void) null);
                                                   })
                                           );

                                           return Result.of((Void) null);
                                       });
                           } else {
                               LOG.warn("template.xml not found for template {}", prototype.source().getTemplateName());
                           }
                       }
                   })
                   .ifError(LOG::warn);
           System.out.printf("%" + prototypeCountLength + "d/%d\r", i++, prototypes.size());
       }

        System.out.printf("%" + prototypeCountLength + "d/%d%n", i - 1, prototypes.size());

        LOG.info("Successfully found {} cards. Took {}ms", cards.size(), System.currentTimeMillis() - totalTime);

        return Result.of(cards);
    }

    private Result<Element> resolveResources(Element root, TemplateSource.Compound source, RemoteFileCache cache) {
        NodeList resourceList = root.getElementsByTagName("resources");

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
        NodeList importsList = root.getElementsByTagName("imports");

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

    private List<JsonObject> runInitScripts(JsonObject raw, TemplateSource source, Element root, int number, JsonObject options, JsonObject overrides) {
        NodeList scriptBlocks = root.getElementsByTagName("scripts");

        Map<String, List<Function<Object[], Object>>> tasks = new HashMap<>();
        List<JsonObject> cards = new ArrayList<>();
        cards.add(raw);
        Context context = Context.create(null, Collections.emptyMap(), Collections.emptyList(), (string, task) ->
                tasks.computeIfAbsent(string, s -> new ArrayList<>()).add(task)
        );

        for (int i = 0; i < scriptBlocks.getLength(); ++i) {
            Node r = scriptBlocks.item(i);

            if (r instanceof Element) {
                NodeList scriptsBlock = r.getChildNodes();

                for (int j = 0; j < scriptsBlock.getLength(); ++j) {
                    r = scriptsBlock.item(j);

                    if (r instanceof Element e && e.getTagName().equals("init")) {
                        NodeList scripts = r.getChildNodes();

                        for (int k = 0; k < scripts.getLength(); ++k) {
                            r = scripts.item(k);

                            if (r instanceof Element script) {
                                String src = script.getAttribute("src");

                                List<JsonObject> nextCards = new ArrayList<>();

                                for (var card : cards) {
                                    nextCards.addAll(ScriptingUtil.applyFunction(context, source, src, this::handleScriptResult, null, card, number, options, overrides));
                                }

                                cards = nextCards;
                            }
                        }
                    }
                }
            }
        }

        for (var task : tasks.getOrDefault("postInit", Collections.emptyList())) {
            task.apply(new Object[0]);
        }

        return cards;
    }

    private List<JsonObject> handleScriptResult(Object object) {
        if (object instanceof List list) {
            return list;
        } else if (object instanceof JsonObject json) {
            return Collections.singletonList(json);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private Result<JsonObject> getCardInfo(CardPrototype prototype) {
        StringBuilder uri = new StringBuilder("https://api.scryfall.com/cards/named?");

        uri.append("fuzzy=").append(URLEncoder.encode(prototype.cardName(), StandardCharsets.UTF_8));

        if (prototype.options().has("set_code")) {
            if (prototype.options().has("collector_number")) {
                uri = new StringBuilder("https://api.scryfall.com/cards/")
                        .append(prototype.options().getAsString("set_code").toLowerCase(Locale.ROOT))
                        .append("/")
                        .append(prototype.options().getAsString("collector_number"))
                ;
            } else {
                uri.append("&set=").append(prototype.options().getAsString("set_code"));
            }
        }

        String string = uri.toString();

        return this.cardInfo.computeIfAbsent(string, s -> {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(s))
                    .GET()
                    .build();

            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    String responseBody = response.body();
                    JsonObject cardInfo = JsonObject.parseObject(JsonReader.json5(responseBody));

                    return Result.of(cardInfo);
                } else {
                    StringBuilder message = new StringBuilder("Could not find card").append(prototype.cardName());

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

                    return Result.error("%s: %s", message.toString(), details);
                }
            } catch (Exception e) {
                return Result.error(e.getMessage());
            }
        });
    }

    private Result<AtomicInteger> renderAndSave(Deque<RenderableCard> cards) {
        int countStrLen = Integer.toString(cards.size()).length();
        int threadCount = Integer.min(options.has("threads") ? Integer.parseInt(options.getAsString("threads")) : 10, cards.size());

        if (threadCount > 0) {
            AtomicInteger finishedCards = new AtomicInteger();
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            Deque<String> errors = new ConcurrentLinkedDeque<>();

            LOG.info("Rendering {} cards on {} threads", cards.size(), threadCount);

            for (RenderableCard card : cards) {
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
                return Result.error(e.getMessage());
            }
        }

        return Result.error("No cards to render");
    }

    private void render(RenderableCard card, AtomicInteger finishedCards, Deque<String> errors, int countStrLen, int cardCount) {
        long cardTime = System.currentTimeMillis();

        String name = card.getName();

        try {
            BufferedImage image = new BufferedImage(card.getWidth(), card.getHeight(), BufferedImage.TYPE_INT_ARGB);

            Result<Void> result = card.render(new StatefulGraphics(image)).ifError(errors::add);

            if (result.isOk()) {
                Path path = Path.of("images");

                for (JsonElement element : card.getAsJsonArray("proximity", "path")) {
                    path = path.resolve(element.getAsString());
                }

                path = path.resolveSibling(path.getFileName().toString() + ".png");

                this.save(image, path);

                finishedCards.incrementAndGet();
                LOG.info(String.format("%" + countStrLen + "d/%" + countStrLen + "d  %5dms  %-55s {}SAVED{}", finishedCards.get(), cardCount, System.currentTimeMillis() - cardTime, name), Logging.ANSI_GREEN, Logging.ANSI_RESET);
            } else {
                LOG.error(String.format("%" + countStrLen + "d/%" + countStrLen + "d  %5dms  %-55s {}FAILED{}", finishedCards.get(), cardCount, System.currentTimeMillis() - cardTime, name), Logging.ANSI_RED, Logging.ANSI_RESET);
            }
        } catch (Throwable throwable) {
            LOG.error(String.format("%" + countStrLen + "d/%" + countStrLen + "d  %5dms  %-55s {}FAILED{}", finishedCards.get(), cardCount, System.currentTimeMillis() - cardTime, name), Logging.ANSI_RED, Logging.ANSI_RESET);
            LOG.error(throwable.getMessage());
        }
    }

    private void save(BufferedImage image, Path path) throws Throwable {
        if (!Files.isDirectory(path.getParent())) {
            Files.createDirectories(path.getParent());
        }

        OutputStream stream = Files.newOutputStream(path);

        ImageIO.write(image, "png", stream);
        stream.close();
    }

    @HostAccess.Export
    public static void log(String level, String message, Object... args) {
        LOG.printf(Level.getLevel(level.toUpperCase(Locale.ROOT)), message, args);
    }
}
