package dev.hephaestus.deckbuilder.templates;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.hephaestus.deckbuilder.ImageCache;
import dev.hephaestus.deckbuilder.TextComponent;
import dev.hephaestus.deckbuilder.cards.*;
import dev.hephaestus.deckbuilder.text.Alignment;
import dev.hephaestus.deckbuilder.text.Style;
import dev.hephaestus.deckbuilder.text.Symbol;
import dev.hephaestus.deckbuilder.util.DrawingUtil;
import dev.hephaestus.deckbuilder.util.StatefulGraphics;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.function.Predicate;

public final class Template {
    private final Map<String, Style> styles;
    private final List<LayerFactory> layers = new ArrayList<>();

    private Template(Map<String, Style> styles, List<LayerFactoryFactory> factories) {
        this.styles = Map.copyOf(styles);

        for (LayerFactoryFactory factory : factories) {
            this.layers.add(factory.create(this));
        }
    }

    public Style getStyle(String name) {
        if (name == null) return Style.EMPTY;

        return this.styles.getOrDefault(name, Style.EMPTY);
    }

    public void draw(Card card, BufferedImage out) {
        StatefulGraphics graphics = new StatefulGraphics(out);

        for (LayerFactory factory : this.layers) {
            Layer layer = factory.create(card, 0, 0);

            if (layer != null) {
                layer.draw(graphics, null);
            }
        }
    }

    public static final class Builder {
        private final Map<String, Style> styles = new HashMap<>();
        private final List<LayerFactoryFactory> factories = new ArrayList<>();

        public Builder layer(LayerFactoryFactory factory) {
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

    public static class Parser {
        private final Map<String, Predicate<Card>> predicates = new HashMap<>();

        public Parser(Map<String, String> args) {
            // Default values
            this.predicates.put("use_art", card -> true);
            this.predicates.put("border", card -> true);

            // Passed values
            for (var entry : args.entrySet()) {
                if (entry.getValue().equalsIgnoreCase("true") || entry.getValue().equalsIgnoreCase("false")) {
                    boolean bl = Boolean.parseBoolean(entry.getValue());
                    this.predicates.put(entry.getKey().replace("-", "_"), card -> bl);
                }
            }
        }

        public Template parse(JsonObject object, ImageCache cache) {
            Template.Builder builder = new Template.Builder();

            if (object.has("styles")) {
                for (Map.Entry<String, JsonElement> entry : object.getAsJsonObject("styles").entrySet()) {
                    if (entry.getValue().isJsonObject()) {
                        builder.style(entry.getKey(), parseStyle(entry.getValue().getAsJsonObject()));
                    }
                }
            }

            if (object.has("layers")) {
                for (JsonElement layer : object.getAsJsonArray("layers")) {
                    builder.layer(parseLayer(layer, cache));
                }
            }

            return builder.build();
        }

        private Style parseStyle(JsonObject object) {
            Style.Builder styleBuilder = new Style.Builder();

            for (Map.Entry<String, JsonElement> styleEntry : object.entrySet()) {
                switch (styleEntry.getKey().toLowerCase(Locale.ROOT)) {
                    case "font" -> styleBuilder.font(styleEntry.getValue().getAsString());
                    case "size" -> styleBuilder.size(styleEntry.getValue().getAsFloat());
                    case "color" -> styleBuilder.color(styleEntry.getValue() == null || styleEntry.getValue().isJsonNull() ? null : Integer.decode(styleEntry.getValue().getAsString()));
                    case "shadow" -> styleBuilder.shadow(Style.Shadow.parse(styleEntry.getValue().getAsJsonObject()));
                    case "outline" -> styleBuilder.outline(Style.Outline.parse(styleEntry.getValue().getAsJsonObject()));
                    case "italic_font" -> styleBuilder.italics(styleEntry.getValue().getAsString());
                    default -> throw new IllegalStateException("Unexpected value: " + styleEntry.getKey().toLowerCase(Locale.ROOT));
                }
            }

            return styleBuilder.build();
        }

        private LayerFactoryFactory parseLayer(JsonElement element, ImageCache cache) {
            if (element.isJsonArray()) {
                return parseArray(element.getAsJsonArray(), cache);
            } else if (element.isJsonObject()) {
                return parseLayerFromObject(element.getAsJsonObject(), cache);
            } else {
                throw new RuntimeException(String.format("Layer '%s' is an invalid format", element));
            }
        }

        private LayerFactoryFactory parseArray(JsonArray array, ImageCache cache) {
            List<LayerFactoryFactory> factories = new ArrayList<>();

            for (JsonElement element : array) {
                factories.add(parseLayer(element, cache));
            }

            return template -> {
                List<LayerFactory> layers = new ArrayList<>();

                for (LayerFactoryFactory factory : factories) {
                    layers.add(factory.create(template));
                }

                return (card, x, y) -> {
                    for (LayerFactory factory : layers) {
                        Layer layer = factory.create(card, x, y);

                        if (layer != Layer.EMPTY) return layer;
                    }

                    return Layer.EMPTY;
                };
            };
        }

        private LayerFactoryFactory parseLayerFromObject(JsonObject object, ImageCache cache) {
            List<Predicate<Card>> predicates = parseConditions(object.getAsJsonObject("conditions"));

            int dX = object.has("x") ? object.get("x").getAsInt() : 0;
            int dY = object.has("y") ? object.get("y").getAsInt() : 0;

            LayerFactoryFactory factory = switch (object.get("type").getAsString()) {
                case "art" -> parseArt(object);
                case "compound" -> parseArray(object.getAsJsonArray("children"), cache);
                case "image" -> parseImage(object, cache);
                case "squish" -> parseSquish(object, cache);
                case "text" -> parseText(object);
                case "spacer" -> parseSpacer(object);
                default -> throw new IllegalStateException("Unexpected value: " + object.get("type").getAsString());
            };

            return template -> {
                LayerFactory function = factory.create(template);

                return (card, x, y) -> {
                    for (Predicate<Card> predicate : predicates) {
                        if (!predicate.test(card)) return Layer.EMPTY;
                    }

                    return function.create(card, dX, dY);
                };
            };
        }

        private LayerFactoryFactory parseSpacer(JsonObject object) {
            int width = object.get("width").getAsInt();
            int height = object.get("height").getAsInt();

            return template -> (card, x, y) -> new Layer(x, y) {
                @Override
                protected Rectangle draw(StatefulGraphics out, Rectangle wrap) {
                    return new Rectangle(x, y, width, height);
                }
            };
        }

        private LayerFactoryFactory parseSquish(JsonObject object, ImageCache cache) {
            LayerFactoryFactory main = parseLayer(object.get("main").getAsJsonObject(), cache);
            LayerFactoryFactory flex = parseLayer(object.get("flex").getAsJsonObject(), cache);

            return template -> {
                LayerFactory mainFunction = main.create(template);
                LayerFactory flexFunction = flex.create(template);

                return (card, x, y) -> {
                    Layer mainLayer = mainFunction.create(card, x, y);
                    Layer flexLayer = flexFunction.create(card, x, y);

                    return new Layer(0, 0) {
                        @Override
                        protected Rectangle draw(StatefulGraphics out, Rectangle wrap) {
                            Rectangle mainLayerBounds = mainLayer.draw(out, wrap);
                            Rectangle flexLayerBounds = flexLayer.draw(out, mainLayerBounds);

                            return DrawingUtil.encompassing(mainLayerBounds, flexLayerBounds);
                        }
                    };
                };
            };
        }

        private LayerFactoryFactory parseText(JsonObject object) {
            Alignment alignment = object.has("alignment") ?
                    Alignment.valueOf(object.get("alignment").getAsString().toUpperCase(Locale.ROOT))
                    : Alignment.LEFT;

            String style = object.has("style") ? object.get("style").getAsString() : null;
            String string = object.get("value").getAsString();
            Integer width = object.has("width") ? object.get("width").getAsInt() : null;
            Integer height = object.has("height") ? object.get("height").getAsInt() : null;

            return template -> (card, x, y) -> {
                List<List<TextComponent>> text;

                Alignment al = alignment;

                if (string.equals("${card.cost}") && card instanceof Spell spell) {
                    text = Collections.singletonList(spell.manaCost());
                } else if (string.equals("${card.oracle}")) {
                    OracleText oracle = card.getOracle();
                    al = oracle.alignment();
                    text = oracle.text();

                    if (al == Alignment.CENTER && width != null) {
                        x += width / 2;
                    }
                } else {
                    text = Collections.singletonList(Collections.singletonList(new TextComponent(substitute(string, card))));
                }

                return new TextLayer(al, template.getStyle(style), text, x, y, width != null && height != null ? new Rectangle(x, y, width, height) : null);
            };
        }

        private LayerFactoryFactory parseArt(JsonObject object) {
            Integer width = object.has("width") ? object.get("width").getAsInt() : null;
            Integer height = object.has("height") ? object.get("height").getAsInt() : null;

            if (width != null || height != null) {
                return template -> (card, x, y) -> {
                    if (card.image() == null) return Layer.EMPTY;

                    try {
                        return new ImageLayer(card.image().toString(), ImageIO.read(card.image().openStream()), width, height, x, y);
                    } catch (IOException exception) {
                        exception.printStackTrace();
                    }

                    return Layer.EMPTY;
                };
            } else {
                throw new RuntimeException("Art layer requires at least one color 'width' and/or 'height'");
            }
        }

        private LayerFactoryFactory parseImage(JsonObject object, ImageCache cache) {
            return template -> (card, x, y) -> {
                String sub = substitute(object.get("src").getAsString(), card);
                BufferedImage image = cache.get(sub);
                return new ImageLayer(sub, image, image.getWidth(), image.getHeight(), x, y);
            };
        }

        private List<Predicate<Card>> parseConditions(JsonObject conditions) {
            if (conditions == null) return Collections.emptyList();

            List<Predicate<Card>> predicates = new ArrayList<>();

            for (Map.Entry<String, JsonElement> entry : conditions.entrySet()) {
                String key = entry.getKey().toLowerCase(Locale.ROOT);

                switch (key) {
                    case "colorcount" -> predicates.add(card -> card.colors().size() == entry.getValue().getAsInt());
                    case "hybrid" -> predicates.add(card -> false == entry.getValue().getAsBoolean()); // TODO
                    default -> predicates.add(this.predicates.containsKey(key)
                            ? card -> this.predicates.get(key).test(card) == entry.getValue().getAsBoolean()
                            : card -> card.getTypes().has(entry.getKey().toLowerCase(Locale.ROOT)) == entry.getValue().getAsBoolean());
                }
            }

            return predicates;
        }

        private String substitute(String src, Card card) {
            if (card instanceof Creature creature) {
                src = src.replace("${card.pt}", creature.power() + "/" + ((Creature) card).toughness());
            }

            return src.replace("${card.color}", join(card.colors()))
                    .replace("${card.name}", card.name())
                    .replace("${card.type}", card.type());
        }

        private String join(Iterable<Symbol> symbols) {
            StringBuilder builder = new StringBuilder();

            for (Symbol symbol : symbols) {
                builder.append(symbol.glyphs());
            }

            return builder.toString();
        }
    }
}
