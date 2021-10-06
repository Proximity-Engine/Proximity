package dev.hephaestus.proximity;

import dev.hephaestus.proximity.cards.CardPrototype;
import dev.hephaestus.proximity.cards.layers.*;
import dev.hephaestus.proximity.json.JsonObject;
import dev.hephaestus.proximity.json.JsonPrimitive;
import dev.hephaestus.proximity.templates.TemplateLoader;
import dev.hephaestus.proximity.templates.TemplateSource;
import dev.hephaestus.proximity.util.*;
import dev.hephaestus.proximity.xml.LayerRenderer;
import dev.hephaestus.proximity.xml.RenderableCard;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quiltmc.json5.JsonReader;

import javax.imageio.ImageIO;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Proximity {
    public static Logger LOG = LogManager.getLogger("Proximity");

    private final JsonObject options;
    private final JsonObject overrides;
    private final List<TemplateLoader> loaders;
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

        LayerRenderer.register(new LayoutElementRenderer("x", "y",
                Rectangle2D::getWidth,
                Rectangle2D::getHeight
        ), "HorizontalLayout");

        LayerRenderer.register(new LayoutElementRenderer("y", "x",
                Rectangle2D::getHeight,
                Rectangle2D::getWidth
        ), "VerticalLayout");
    }

    public Proximity(JsonObject options, JsonObject overrides, TemplateLoader... loaders) {
        this.options = options;
        this.overrides = overrides;
        this.loaders = new ArrayList<>(Arrays.asList(loaders));
    }

    public void run() {
        long startTime = System.currentTimeMillis();

        Result<?> result = getDefaultTemplateName()
                .then(this::loadCardsFromFile)
                .then(this::getCardInfo)
                .then(this::renderAndSave)
                .ifError(LOG::error);

        if (!result.isError()) {
            LOG.info("Done! Took {}ms", System.currentTimeMillis() - startTime);
        }
    }

    private Result<String> getDefaultTemplateName() {
        if (!options.has("template")) {
            return Result.error("Default template not provided");
        }

        if (!(options.get("template") instanceof JsonPrimitive primitive) || !primitive.isString()) {
            return Result.error(String.format("Default template must be a string. Found: %s", options.get("template")));
        }

        return Result.of(options.getAsString("template"));
    }

    private Result<Deque<CardPrototype>> loadCardsFromFile(String defaultTemplate) {
        Deque<CardPrototype> result = new ArrayDeque<>();

        Path path = Path.of(this.options.getAsString("cards"));

        try (BufferedReader reader = Files.newBufferedReader(path)) {
            Pattern pattern = Pattern.compile("([0-9]+)x? (.+)( \\d+)?");

            int cardNumber = 1;

            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                String info = line.contains("--") ? line.substring(0, line.indexOf("--")): line;
                Matcher matcher = pattern.matcher(info);

                if (matcher.matches()) {
                    JsonObject cardOptions = this.options.deepCopy();
                    JsonObject overrides = new JsonObject();
                    String cardName = parseCardName(matcher.group(2));

                    int count = matcher.groupCount() == 2 ? Integer.parseInt(matcher.group(1)) : 1;
                    cardOptions.addProperty("count", count);

                    if (info.contains("(") && info.contains(")")) {
                        cardOptions.addProperty("set_code", line.substring(line.indexOf("(") + 1, line.indexOf(")")).toUpperCase(Locale.ROOT));
                    }

                    int i = line.indexOf("--");

                    while (i >= 0) {
                        int j;

                        for (j = i; j < line.length() && !Character.isWhitespace(line.charAt(j)); ++j) {
                            char c = line.charAt(j);

                            if (c == '"') {
                                do {
                                    ++j;
                                } while (j < line.length() && line.charAt(j) != '"');
                            }
                        }

                        String[] split = line.substring(i + 2, j).split("=", 2);
                        String key = split[0];
                        String value = split.length == 2 ? split[1] : null;

                        if (key.equals("override")) {
                            if (value != null) {
                                split = value.split(":", 2);
                                String[] overrideKey = split[0].split("\\.");
                                value = split.length == 2 ? split[1] : null;

                                if (value != null && value.length() > 1 && value.startsWith("\"") && value.endsWith("\"")) {
                                    value = value.substring(1, value.length() - 1);
                                }

                                overrides.add(overrideKey, ParsingUtil.parseStringValue(value));
                            }
                        } else {
                            cardOptions.add(key, ParsingUtil.parseStringValue(value));
                        }

                        if (j < line.length()) {
                            String sub = line.substring(j + 1);
                            i = j + 1 + sub.indexOf("--");
                        } else {
                            i = -1;
                        }
                    }

                    String template = defaultTemplate;

                    if (cardOptions.has("template")) {
                        template = cardOptions.getAsString("template");
                    }

                    TemplateSource source = null;

                    for (TemplateLoader loader : this.loaders) {
                        Result<TemplateSource> r = loader.getTemplateFiles(template);

                        if (r.isError()) {
                            return r.unwrap();
                        } else {
                            source = r.get();
                            break;
                        }
                    }

                    result.add(new CardPrototype(cardName, cardNumber, cardOptions, source, overrides));
                    cardNumber += count;
                } else if (!line.trim().isEmpty()) {
                    return Result.error("Could not parse line '%s'", line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return Result.of(result);
    }

    private String parseCardName(String line) {
        return line.contains("(") // if the line contains a set code, ignore that
                ? line.substring(0, line.indexOf('(') - 1)
                : line.contains("--") // only extend to the beginning of the option flags
                ? line.substring(0, line.indexOf("--") - 1)
                : line;
    }

    private Result<Deque<Pair<RenderableCard, Optional<RenderableCard>>>> getCardInfo(Deque<CardPrototype> prototypes) {
        LOG.info("Fetching info for {} cards...", prototypes.size());

        long lastRequest = 0;

       Deque<Pair<RenderableCard, Optional<RenderableCard>>> cards = new ArrayDeque<>();


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
                       JsonObject card = prototype.parse(raw);

                       for (int j = 0; j < card.getAsInt(Keys.COUNT); ++j) {
                           Result<RenderableCard> front = XMLUtil.load(prototype.source()).ifError(LOG::warn)
                                   .then(root -> Result.of(new RenderableCard(prototype.source(), root, card)));

                           Result<Optional<RenderableCard>> back = card.getAsBoolean(Keys.DOUBLE_SIDED) ? XMLUtil.load(prototype.source()).ifError(LOG::warn)
                                   .then(root -> Result.of(Optional.of(new RenderableCard(prototype.source(), root, card.getAsJsonObject(Keys.FLIPPED).deepCopy()))))
                                   : Result.of(Optional.empty());

                           if (front.isOk() && back.isOk()) {
                               cards.add(new Pair<>(front.get(), back.get()));
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

    private Result<JsonObject> getCardInfo(CardPrototype prototype) {
        StringBuilder uri = new StringBuilder("https://api.scryfall.com/cards/named?");

        uri.append("fuzzy=").append(URLEncoder.encode(prototype.cardName(), StandardCharsets.UTF_8));

        if (prototype.options().has("set_code")) {
            uri.append("&set=").append(prototype.options().getAsString("set_code"));
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
                    return Result.error("Response %s:", response.statusCode(), response.body());
                }
            } catch (Exception e) {
                return Result.error(e.getMessage());
            }
        });
    }

    private Result<AtomicInteger> renderAndSave(Deque<Pair<RenderableCard, Optional<RenderableCard>>> cards) {
        int countStrLen = Integer.toString(cards.size()).length();
        int threadCount = Integer.min(options.has("threads") ? Integer.parseInt(options.getAsString("threads")) : 10, cards.size());

        if (threadCount > 0) {
            AtomicInteger finishedCards = new AtomicInteger();
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            Deque<String> errors = new ConcurrentLinkedDeque<>();

            LOG.info("Rendering {} cards on {} threads", cards.size(), threadCount);

            int i = 1;

            for (Pair<RenderableCard, Optional<RenderableCard>> card : cards) {
                if (threadCount > 1) {
                    int finalI = i;
                    executor.submit(() ->
                            this.render(card, finishedCards, errors, countStrLen, finalI, cards.size()));
                } else {
                    this.render(card, finishedCards, errors, countStrLen, i, cards.size());
                }

                i++;
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

    private void render(Pair<RenderableCard, Optional<RenderableCard>> card, AtomicInteger finishedCards, Deque<String> errors, int countStrLen, int i, int cardCount) {
        long cardTime = System.currentTimeMillis();

        RenderableCard front = card.left();
        String name = front.getName() + (card.right().isPresent() ? " // " + card.right().get().getName() : "");

        try {
            BufferedImage frontImage = new BufferedImage(front.getWidth(), front.getHeight(), BufferedImage.TYPE_INT_ARGB);

            Result<Void> result = front.render(new StatefulGraphics(frontImage)).ifError(errors::add);

            if (result.isOk()) {
                Path path = Path.of("images", "fronts", i + " " + front.getName().replaceAll("[^a-zA-Z0-9.\\-, ]", "_") + ".png");

                this.save(frontImage, path);

                if (card.right().isPresent()) {
                    RenderableCard back = card.right().get();
                    BufferedImage backImage = new BufferedImage(back.getWidth(), back.getHeight(), BufferedImage.TYPE_INT_ARGB);
                    result = back.render(new StatefulGraphics(backImage));

                    if (result.isOk()) {
                        path = Path.of("images", "backs", i + " " + back.getName()
                                .replaceAll("[^a-zA-Z0-9.\\-, ']", "_") + ".png");

                        this.save(backImage, path);
                    } else {
                        LOG.error(String.format("%" + countStrLen + "d/%" + countStrLen + "d  %5dms  %-55s {}FAILED{}", finishedCards.get(), cardCount, System.currentTimeMillis() - cardTime, name), Logging.ANSI_RED, Logging.ANSI_RESET);
                        return;
                    }
                }

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
}
