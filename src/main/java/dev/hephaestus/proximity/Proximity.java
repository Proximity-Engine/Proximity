package dev.hephaestus.proximity;

import dev.hephaestus.proximity.cards.CardPrototype;
import dev.hephaestus.proximity.cards.layers.*;
import dev.hephaestus.proximity.json.JsonObject;
import dev.hephaestus.proximity.util.*;
import dev.hephaestus.proximity.xml.LayerRenderer;
import dev.hephaestus.proximity.xml.RenderableCard;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quiltmc.json5.JsonReader;

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

    private Result<Deque<Pair<RenderableCard, Optional<RenderableCard>>>> getCardInfo(Deque<CardPrototype> prototypes) {
        LOG.info("Fetching info for {} cards...", prototypes.size());

        long lastRequest = 0;

       Deque<Pair<RenderableCard, Optional<RenderableCard>>> cards = new ArrayDeque<>();

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
                       JsonObject card = prototype.parse(raw);

                       for (int j = 0; j < card.getAsInt(Keys.COUNT); ++j) {
                           Result<RenderableCard> front = XMLUtil.load(prototype.source()).ifError(LOG::warn)
                                   .then(root -> Result.of(new RenderableCard(prototype.source(), cache, root, card)));

                           Result<Optional<RenderableCard>> back = card.getAsBoolean(Keys.DOUBLE_SIDED) ? XMLUtil.load(prototype.source()).ifError(LOG::warn)
                                   .then(root -> Result.of(Optional.of(new RenderableCard(prototype.source(), cache, root, card.getAsJsonObject(Keys.FLIPPED).deepCopy()))))
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
                    executor.submit(() -> this.render(card, finishedCards, errors, countStrLen, finalI, cards.size()));
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
