package dev.hephaestus.proximity.templates;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.hephaestus.proximity.ImageCache;
import dev.hephaestus.proximity.TextComponent;
import dev.hephaestus.proximity.cards.*;
import dev.hephaestus.proximity.text.Alignment;
import dev.hephaestus.proximity.text.Style;
import dev.hephaestus.proximity.text.Symbol;
import dev.hephaestus.proximity.util.DrawingUtil;
import dev.hephaestus.proximity.util.Option;
import dev.hephaestus.proximity.util.OptionContainer;
import dev.hephaestus.proximity.util.StatefulGraphics;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.function.Predicate;

public final class Template implements OptionContainer {
    private final Map<String, Style> styles;
    private final List<LayerFactory> layers = new ArrayList<>();
    private final OptionContainer wrappedOptions;

    private Template(Map<String, Style> styles, List<LayerFactoryFactory> factories, OptionContainer options) {
        this.styles = Map.copyOf(styles);
        this.wrappedOptions = options;

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

    @Override
    public <T> T getOption(String name) {
        return this.wrappedOptions.getOption(name);
    }

    @Override
    public Map<String, Object> getMap() {
        return this.wrappedOptions.getMap();
    }

    public static final class Builder {
        private final Map<String, Style> styles = new HashMap<>();
        private final List<LayerFactoryFactory> factories = new ArrayList<>();
        private final Map<String, Object> options = new HashMap<>();

        public Builder layer(LayerFactoryFactory factory) {
            this.factories.add(factory);

            return this;
        }

        public Builder style(String name, Style style) {
            this.styles.put(name, style);

            return this;
        }

        public <T> Builder option(String name, T value) {
            this.options.put(name, value);

            return this;
        }

        public Template build() {
            return new Template(this.styles, this.factories, new Implementation(this.options));
        }
    }

    public static class Parser {
        private final Map<String, Predicate<Card>> predicates = new HashMap<>();

        public Parser(Map<String, String> args) {
            for (var entry : args.entrySet()) {
                if (entry.getValue().equalsIgnoreCase("true") || entry.getValue().equalsIgnoreCase("false")) {
                    boolean bl = Boolean.parseBoolean(entry.getValue());
                    this.predicates.put(entry.getKey().replace("-", "_"), card -> bl);
                }
            }
        }

        public Template parse(JsonObject object, ImageCache cache) {
            Template.Builder builder = new Template.Builder();

            builder.option(Option.USE_OFFICIAL_ART, true);
            this.predicates.put("use_official_art", card -> card.getOption(Option.USE_OFFICIAL_ART));

            if (object.has("options")) {
                for (Map.Entry<String, JsonElement> entry : object.getAsJsonObject("options").entrySet()) {
                    if (entry.getValue().isJsonPrimitive()) {
                        String key = entry.getKey();
                        JsonPrimitive value = entry.getValue().getAsJsonPrimitive();

                        if (value.isString()) {
                            String stringValue = value.getAsString();
                            builder.option(key, stringValue);
                            this.predicates.put(key, card -> card.getOption(key).equals(stringValue));
                        } else if (value.isBoolean()) {
                            builder.option(entry.getKey(), value.getAsBoolean());
                            this.predicates.put(entry.getKey(), card -> card.getOption(entry.getKey()));
                        }
                    }
                }
            }

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
                    case "capitalization" -> styleBuilder.capitalization(Style.Capitalization.valueOf(styleEntry.getValue().getAsString().toUpperCase(Locale.ROOT)));
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
                case "group" -> parseGroup(object, cache);
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

        private LayerFactoryFactory parseGroup(JsonObject object, ImageCache cache) {
            List<LayerFactoryFactory> factories = new ArrayList<>();

            for (JsonElement element : object.getAsJsonArray("children")) {
                factories.add(parseLayer(element, cache));
            }

            return template -> {
                List<LayerFactory> layers = new ArrayList<>();

                for (LayerFactoryFactory factory : factories) {
                    layers.add(factory.create(template));
                }

                return (card, x, y) -> {
                    List<Layer> children = new ArrayList<>();

                    for (LayerFactory factory : layers) {
                        Layer layer = factory.create(card, x, y);

                        if (layer != Layer.EMPTY) children.add(layer);
                    }

                    return new Layer(x, y) {
                        @Override
                        protected Rectangle draw(StatefulGraphics out, Rectangle wrap) {
                            Rectangle bounds = null;

                            for (Layer layer : children) {
                                Rectangle rectangle = layer.draw(out, wrap);
                                bounds = bounds != null ?
                                        DrawingUtil.encompassing(bounds, rectangle)
                                        : rectangle;
                            }

                            return bounds;
                        }
                    };
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

            String string = object.get("value").getAsString();
            Integer width = object.has("width") ? object.get("width").getAsInt() : null;
            Integer height = object.has("height") ? object.get("height").getAsInt() : null;

            return template -> {
                Style style = object.has("style") && object.get("style").isJsonObject()
                        ? parseStyle(object.getAsJsonObject("style"))
                        : object.has("style") && object.get("style").isJsonPrimitive()
                                ? template.getStyle(object.get("style").getAsString())
                                : Style.EMPTY;

                return (card, x, y) -> {
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
                    } else if (string.equals("${card.artist}")) {
                        text = Collections.singletonList(Collections.singletonList(new TextComponent(card.getOption(Option.ARTIST))));
                    } else {
                        text = Collections.singletonList(Collections.singletonList(new TextComponent(substitute(string, card))));
                    }

                    text = applyCapitalization(text, style.capitalization(), style.size());

                    return new TextLayer(al, style, text, x, y, width != null && height != null ? new Rectangle(x, y, width, height) : null);
                };
            };
        }

        private static List<List<TextComponent>> applyCapitalization(List<List<TextComponent>> text, Style.Capitalization caps, Float fontSize) {
            if (caps == null || fontSize == null) return text;

            List<List<TextComponent>> result = new ArrayList<>();

            for (List<TextComponent> list : text) {
                List<TextComponent> level = new ArrayList<>();

                for (TextComponent component : list) {
                    switch (caps) {
                        case ALL_CAPS -> level.add(new TextComponent(component.string().toUpperCase(Locale.ROOT)));
                        case NO_CAPS -> level.add(new TextComponent(component.string().toLowerCase(Locale.ROOT)));
                        case SMALL_CAPS -> {
                            Style uppercase = component.style() == null
                                    ? new Style.Builder().size(fontSize).build()
                                    : component.style().size(fontSize);

                            Style lowercase = component.style() == null
                                    ? new Style.Builder().size(fontSize * 0.75F).build()
                                    : component.style().size(fontSize * 0.75F);

                            for (char c : component.string().toCharArray()) {
                                boolean bl = Character.isUpperCase(c);

                                level.add(new TextComponent(bl ? uppercase : lowercase, Character.toUpperCase(c)));
                            }
                        }
                    }
                }

                result.add(level);
            }

            return result;
        }

        private LayerFactoryFactory parseArt(JsonObject object) {
            Integer width = object.has("width") ? object.get("width").getAsInt() : null;
            Integer height = object.has("height") ? object.get("height").getAsInt() : null;

            if (width != null || height != null) {
                return template -> (card, x, y) -> {
                    if (card.image() == null || !(boolean) card.getOption(Option.USE_OFFICIAL_ART)) return Layer.EMPTY;

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
                    case "hybrid" -> predicates.add(card -> {
                        if (card instanceof Spell spell && spell.colors().size() == 2) {
                            for (TextComponent component : spell.manaCost()) {
                                switch (component.string()) {
                                    case "w":
                                    case "u":
                                    case "b":
                                    case "r":
                                    case "g":
                                        return !entry.getValue().getAsBoolean();
                                }
                            }

                            return entry.getValue().getAsBoolean();
                        }

                        return false;
                    });
                    case "or" -> {
                        List<List<Predicate<Card>>> orPredicates = new ArrayList<>();

                        for (JsonElement element : entry.getValue().getAsJsonArray()) {
                            orPredicates.add(parseConditions(element.getAsJsonObject()));
                        }

                        predicates.add(card -> {
                            boolean result = false;

                            for (List<Predicate<Card>> list : orPredicates) {
                                boolean bl = true;

                                for (Predicate<Card> predicate : list) {
                                    bl &= predicate.test(card);
                                }

                                result |= bl;
                            }

                            return result;
                        });
                    }
                    case "types" -> predicates.add(card -> {
                        for (Map.Entry<String, JsonElement> type : entry.getValue().getAsJsonObject().entrySet()) {
                            if (card.getTypes().has(type.getKey()) != type.getValue().getAsBoolean()) return false;
                        }

                        return true;
                    });
                    case "frame_effects" -> predicates.add(card -> {
                        for (Map.Entry<String, JsonElement> effect : entry.getValue().getAsJsonObject().entrySet()) {
                            if (card.hasFrameEffect(effect.getKey()) != effect.getValue().getAsBoolean()) return false;
                        }

                        return true;
                    });
                    case "options" -> predicates.add(card -> {
                        for (Map.Entry<String, JsonElement> option : entry.getValue().getAsJsonObject().entrySet()) {
                            if (option.getValue().getAsJsonPrimitive().isString() && !this.predicates.get(option.getKey()).test(card)) return false;
                            if (this.predicates.get(option.getKey()).test(card) != option.getValue().getAsBoolean()) return false;
                        }

                        return true;
                    });
                    default -> throw new RuntimeException("Unexpected condition: " + entry.getKey());
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
