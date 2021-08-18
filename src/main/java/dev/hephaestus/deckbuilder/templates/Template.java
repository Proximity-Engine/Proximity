package dev.hephaestus.deckbuilder.templates;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.hephaestus.deckbuilder.ImageCache;
import dev.hephaestus.deckbuilder.cards.Card;
import dev.hephaestus.deckbuilder.cards.Color;
import dev.hephaestus.deckbuilder.text.Alignment;
import dev.hephaestus.deckbuilder.text.Style;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Template {
    private final Map<String, Style> styles;
    private final List<Function<Card, Layer>> layers = new ArrayList<>();

    private Template(Map<String, Style> styles, List<Layer.Factory> factories) {
        this.styles = Map.copyOf(styles);

        for (Layer.Factory factory : factories) {
            this.layers.add(factory.create(this));
        }
    }

    public Style getStyle(String name) {
        return this.styles.get(name);
    }

    public void draw(Card card, BufferedImage out) {
        Graphics2D graphics = out.createGraphics();

        for (Function<Card, Layer> function : this.layers) {
            Layer layer = function.apply(card);

            if (layer != null) {
                layer.draw(graphics);
            }
        }
    }

    public static final class Builder {
        private final Map<String, Style> styles = new HashMap<>();
        private final List<Layer.Factory> factories = new ArrayList<>();

        public Builder layer(Layer.Factory factory) {
            this.factories.add(factory);

            return this;
        }

        public Builder style(String name, Style style) {
            this.styles.put(name, style);

            return this;
        }

        public Template build() {
            return new Template(this.styles, this.factories);
        }
    }

    public static Template parse(JsonObject object, ImageCache cache) {
        Template.Builder builder = new Template.Builder();

        if (object.has("styles")) {
            for (Map.Entry<String, JsonElement> entry : object.getAsJsonObject("styles").entrySet()) {
                if (entry.getValue().isJsonObject()) {
                    Style.Builder styleBuilder = new Style.Builder();

                    for (Map.Entry<String, JsonElement> styleEntry : entry.getValue().getAsJsonObject().entrySet()) {
                        switch (styleEntry.getKey().toLowerCase(Locale.ROOT)) {
                            case "font" -> styleBuilder.font(styleEntry.getValue().getAsString());
                            case "size" -> styleBuilder.size(styleEntry.getValue().getAsFloat());
                            case "color" -> styleBuilder.color(styleEntry.getValue() == null || styleEntry.getValue().isJsonNull() ? null : Integer.decode(styleEntry.getValue().getAsString()));
                            case "alignment" -> styleBuilder.alignment(Alignment.valueOf(styleEntry.getValue().getAsString().toUpperCase(Locale.ROOT)));
                            case "shadow" -> styleBuilder.shadow(Style.Shadow.parse(styleEntry.getValue().getAsJsonObject()));
                            case "outline" -> styleBuilder.outline(Style.Outline.parse(styleEntry.getValue().getAsJsonObject()));
                            default -> throw new IllegalStateException("Unexpected value: " + styleEntry.getKey().toLowerCase(Locale.ROOT));
                        }
                    }

                    builder.style(entry.getKey(), styleBuilder.build());
                }
            }
        }

        if (object.has("layers")) {
            for (JsonElement layer : object.getAsJsonArray("layers")) {
                builder.layer(parse(layer, cache));
            }
        }

        if (object.has("text")) {
            // TODO
        }

        return builder.build();
    }

    private static Layer.Factory parse(JsonElement element, ImageCache cache) {
        if (element.isJsonPrimitive() && ((JsonPrimitive) element).isString()) {
            return parseString(element.getAsString(), cache);
        } else if (element.isJsonArray()) {
            return parseArray(element.getAsJsonArray(), cache);
        } else {
            return parseObject(element.getAsJsonObject(), cache);
        }
    }

    private static Layer.Factory parseString(String string, ImageCache cache) {
        return template -> card -> {
            String sub = substitute(string, card);
            BufferedImage image = cache.get(sub);
            return new ImageLayer(sub,image, image.getWidth(), image.getHeight());
        };
    }

    private static Layer.Factory parseArray(JsonArray array, ImageCache cache) {
        List<Layer.Factory> factories = new ArrayList<>();

        for (JsonElement element : array) {
            factories.add(parse(element, cache));
        }

        return template -> {
            List<Function<Card, Layer>> layers = new ArrayList<>();

            for (Layer.Factory factory : factories) {
                layers.add(factory.create(template));
            }

            return card -> {
                for (Function<Card, Layer> function : layers) {
                    Layer layer = function.apply(card);

                    if (layer != Layer.EMPTY) return layer;
                }

                return Layer.EMPTY;
            };
        };
    }

    private static Layer.Factory parseObject(JsonObject object, ImageCache cache) {
        Integer width = object.has("width") ? object.get("width").getAsInt() : null;
        Integer height = object.has("height") ? object.get("height").getAsInt() : null;

        Layer.Factory factory = object.get("src").isJsonPrimitive() && (width != null || height != null)
                ? template -> card -> art(card, width, height)
                : parse(object.get("src"), cache);

        List<Predicate<Card>> predicates = new ArrayList<>();

        if (object.has("conditions")) {
            for (Map.Entry<String, JsonElement> entry : object.getAsJsonObject("conditions").entrySet()) {
                switch (entry.getKey().toLowerCase(Locale.ROOT)) {
                    case "colorcount" -> predicates.add(card -> card.colors().size() == entry.getValue().getAsInt());
                    case "hybrid" -> predicates.add(card -> false == entry.getValue().getAsBoolean()); // TODO
                    default -> predicates.add(card -> card.getTypes().has(entry.getKey().toLowerCase(Locale.ROOT)) == entry.getValue().getAsBoolean());
                }
            }
        }

        int dX = object.has("x") ? object.get("x").getAsInt() : 0;
        int dY = object.has("y") ? object.get("y").getAsInt() : 0;

        return template -> {
            Function<Card, Layer> function = factory.create(template);

            return card -> {
                for (Predicate<Card> predicate : predicates) {
                    if (!predicate.test(card)) return Layer.EMPTY;
                }

                Layer layer = function.apply(card);

                return graphics -> {
                    AffineTransform transform = graphics.getTransform();

                    transform.translate(dX, dY);
                    graphics.setTransform(transform);

                    layer.draw(graphics);

                    transform.translate(-dX, -dY);
                    graphics.setTransform(transform);
                };
            };
        };
    }

    private static Layer art(Card card, Integer width, Integer height) {
        if (card.image() == null) return Layer.EMPTY;

        try {
            return new ImageLayer(card.image().toString(), ImageIO.read(card.image().openStream()), width, height);
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        return Layer.EMPTY;
    }

    private static String substitute(String src, Card card) {
        return src.replace("${color}", join(card.colors()));
    }

    private static String join(Iterable<Color> colors) {
        StringBuilder builder = new StringBuilder();

        for (Color color : colors) {
            builder.append(color.symbol());
        }

        return builder.toString();
    }
}
