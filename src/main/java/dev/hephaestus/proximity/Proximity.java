package dev.hephaestus.proximity;

import dev.hephaestus.proximity.cards.Card;
import dev.hephaestus.proximity.cards.CardPrototype;
import dev.hephaestus.proximity.json.JsonObject;
import dev.hephaestus.proximity.json.JsonPrimitive;
import dev.hephaestus.proximity.templates.Template;
import dev.hephaestus.proximity.templates.TemplateLoader;
import dev.hephaestus.proximity.templates.layers.Layer;
import dev.hephaestus.proximity.util.Keys;
import dev.hephaestus.proximity.util.Logging;
import dev.hephaestus.proximity.util.ParsingUtil;
import dev.hephaestus.proximity.util.Result;
import dev.hephaestus.proximity.xml.layers.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quiltmc.json5.JsonReader;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.FileReader;
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
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Proximity {
    public static Logger LOG = LogManager.getLogger("Proximity");

    private final JsonObject options;
    private final JsonObject overrides;
    private final List<TemplateLoader> loaders;

    static {
        LayerElement.register(ArtElement::new, "ArtLayer");
        LayerElement.register(FillElement::new, "Fill", "SpacingLayer", "Spacer");
        LayerElement.register(ForkElement::new, "Fork");
        LayerElement.register(GroupElement::new, "Group", "main");
        LayerElement.register(ImageElement::new, "ImageLayer");
        LayerElement.register(SelectorElement::new, "Selector", "flex");
        LayerElement.register(SquishBoxElement::new, "SquishBox");
        LayerElement.register(TextElement::new, "TextLayer");

        LayerElement.register(element -> new LayoutElement(element,
                Layer::setX,
                Layer::setY,
                Layer::getX,
                Layer::getY,
                Rectangle::getWidth,
                Rectangle::getHeight
        ), "HorizontalLayout");

        LayerElement.register(element -> new LayoutElement(element,
                Layer::setY,
                Layer::setX,
                Layer::getY,
                Layer::getX,
                Rectangle::getHeight,
                Rectangle::getWidth
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
                .then(this::parseTemplate)
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

    private Result<Template> parseTemplate(String name) {
        for (TemplateLoader loader : this.loaders) {
            Result<Template> files = loader.getTemplateFiles(name)
                    .then(fs -> loader.load(fs, this.options))
                    .ifError(LOG::warn);

            if (!files.isError()) return files;
        }

        return Result.error("Template '%s' not found.", name);
    }

    private Result<Deque<CardPrototype>> loadCardsFromFile(Template defaultTemplate) {
        Deque<CardPrototype> result = new ArrayDeque<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(this.options.getAsString("cards")))) {
            Pattern pattern = Pattern.compile("([0-9]+)x? (.+)");

            int cardNumber = 1;

            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                Matcher matcher = pattern.matcher(line);

                if (matcher.matches()) {
                    JsonObject cardOptions = new JsonObject();
                    JsonObject overrides = new JsonObject();
                    String cardName = parseCardName(matcher.group(2));

                    int count = matcher.groupCount() == 2 ? Integer.parseInt(matcher.group(1)) : 1;
                    cardOptions.addProperty("count", count);

                    if (line.contains("(") && line.contains(")")) {
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
                                key = split[0];
                                value = split.length == 2 ? split[1] : null;
                            }

                            overrides.add(key, ParsingUtil.parseStringValue(value));
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

                    Template template = defaultTemplate;

                    if (cardOptions.has("template")) {
                        Result<Template> templateResult = parseTemplate(cardOptions.getAsString("template"));

                        if (templateResult.isError()) {
                            LOG.error("Failed to parse template for card {}: {}", cardName, templateResult.getError());
                            LOG.warn("Using default template");
                        } else {
                            template = templateResult.get();
                        }
                    }

                    JsonObject options = template.getOptions().deepCopy().copyAll(cardOptions);

                    result.add(new CardPrototype(cardName, cardNumber++, options, template, overrides));
                } else {
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

    private Result<Deque<Card>> getCardInfo(Deque<CardPrototype> prototypes) {
        LOG.info("Fetching info for {} cards...", prototypes.size());

        long lastRequest = 0;

       Deque<Card> cards = new ArrayDeque<>();

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
                   .ifPresent(json -> {
                       json = prototype.parse(json);
                       json.copyAll(this.overrides);

                       cards.add(new Card(json, prototype.template()));
                   })
                   .ifError(LOG::warn);
       }

       LOG.info("Successfully found {} cards", cards.size());

        return Result.of(cards);
    }

    private Result<JsonObject> getCardInfo(CardPrototype prototype) {
        StringBuilder uri = new StringBuilder("https://api.scryfall.com/cards/named?");

        uri.append("fuzzy=").append(URLEncoder.encode(prototype.cardName(), StandardCharsets.UTF_8));

        if (prototype.options().has("set_code")) {
            uri.append("&set=").append(prototype.options().getAsString("set_code"));
        }

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri.toString()))
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

    }

    private Result<Deque<String>> renderAndSave(Deque<Card> cards) {
        int cardCount = cards.stream().map(card -> card.representation().getAsInt("proximity", "options", "count"))
                .reduce(0, Integer::sum);

        int countStrLen = Integer.toString(cardCount).length();
        int threadCount = Integer.min(options.has("threads") ? Integer.parseInt(options.getAsString("threads")) : 10, cardCount);

        if (threadCount > 0) {
            Deque<String> finishedCards = new ConcurrentLinkedDeque<>();
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);

            LOG.info("Rendering {} cards on {} threads", cardCount, threadCount);

            for (Card card : cards) {
                executor.submit(() -> {
                    long cardTime = System.currentTimeMillis();

                    try {
                        BufferedImage frontImage = new BufferedImage(card.template().getWidth(), card.template().getHeight(), BufferedImage.TYPE_INT_ARGB);
                        BufferedImage backImage = null;

                        card.template().draw(card.representation(), frontImage);

                        if (card.representation().getAsBoolean(Keys.DOUBLE_SIDED)) {
                            backImage = new BufferedImage(card.template().getWidth(), card.template().getHeight(), BufferedImage.TYPE_INT_ARGB);
                            card.template().draw(card.representation().getAsJsonObject(Keys.FLIPPED), backImage);
                        }

                        for (int i = 0; i < card.representation().getAsInt(Keys.COUNT); ++i) {
                            Path front = Path.of("images", "fronts", card.representation().getAsInt(Keys.CARD_NUMBER) + i + " " + card.representation().getAsString("name").replaceAll("[^a-zA-Z0-9.\\-, ]", "_") + ".png");

                            Path back = Path.of("images", "backs", card.representation().getAsInt(Keys.CARD_NUMBER) + i + " " + (card.representation().getAsBoolean(Keys.DOUBLE_SIDED)
                                    ? card.representation().getAsJsonObject(Keys.FLIPPED).getAsString("name")
                                    : card.representation().getAsString("name")
                            ).replaceAll("[^a-zA-Z0-9.\\-, ]", "_") + ".png");

                            try {
                                if (!Files.isDirectory(front.getParent())) {
                                    Files.createDirectories(front.getParent());
                                }

                                OutputStream stream = Files.newOutputStream(front);

                                ImageIO.write(frontImage, "png", stream);
                                stream.close();

                                if (!Files.isDirectory(back.getParent())) {
                                    Files.createDirectories(back.getParent());
                                }

                                if (card.representation().getAsBoolean(Keys.DOUBLE_SIDED) && backImage != null) {
                                    stream = Files.newOutputStream(back);

                                    ImageIO.write(backImage, "png", stream);
                                    stream.close();
                                }

                                finishedCards.add(card.representation().getAsString("name"));
                                LOG.info(String.format("%" + countStrLen + "d/%" + countStrLen + "d  %5dms  %-45s {}SAVED{}", finishedCards.size(), cardCount, System.currentTimeMillis() - cardTime, card.representation().getAsString("name")), Logging.ANSI_GREEN, Logging.ANSI_RESET);
                            } catch (Throwable throwable) {
                                finishedCards.add(card.representation().getAsString("name"));
                                LOG.error(String.format("%" + countStrLen + "d/%" + countStrLen + "d  %5dms  %-45s {}FAILED{}", finishedCards.size(), cardCount, System.currentTimeMillis() - cardTime, card.representation().getAsString("name")), Logging.ANSI_RED, Logging.ANSI_RESET);
                                LOG.error(throwable.getMessage());
                            }

                            cardTime = System.currentTimeMillis();
                        }
                    } catch (Throwable throwable) {
                        finishedCards.add(card.representation().getAsString("name"));
                        LOG.error(String.format("%" + countStrLen + "d/%" + countStrLen + "d  %5dms  %-45s {}FAILED{}", finishedCards.size(), cardCount, System.currentTimeMillis() - cardTime, card.representation().getAsString("name")), Logging.ANSI_RED, Logging.ANSI_RESET);
                        LOG.error(throwable.getMessage());
                    }
                });
            }

            try {
                executor.shutdown();

                //noinspection StatementWithEmptyBody
                while (!executor.awaitTermination(Integer.MAX_VALUE, TimeUnit.NANOSECONDS)) {

                }

                return Result.of(finishedCards);
            } catch (InterruptedException e) {
                return Result.error(e.getMessage());
            }
        }

        return Result.error("No cards to render");
    }
}
