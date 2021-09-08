package dev.hephaestus.proximity.templates;


import dev.hephaestus.proximity.TemplateFiles;
import dev.hephaestus.proximity.TextComponent;
import dev.hephaestus.proximity.json.JsonArray;
import dev.hephaestus.proximity.json.JsonElement;
import dev.hephaestus.proximity.json.JsonObject;
import dev.hephaestus.proximity.json.JsonPrimitive;
import dev.hephaestus.proximity.text.Alignment;
import dev.hephaestus.proximity.text.Style;
import dev.hephaestus.proximity.util.DrawingUtil;
import dev.hephaestus.proximity.util.Keys;
import dev.hephaestus.proximity.util.StatefulGraphics;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Template implements TemplateFiles  {
    private static final Pattern SUBSTITUTE = Pattern.compile("\\$(\\w*)\\{(\\w+(?:\\.\\w+)*)}");

    private final Map<String, Style> styles;
    private final List<LayerFactory> layers = new ArrayList<>();
    private final Map<String, List<Predicate<JsonElement>>> conditions;
    private final JsonObject options;
    private final TemplateFiles files;

    private Template(TemplateFiles files, List<LayerFactoryFactory> factories, JsonObject options, Map<String, Style> styles, Map<String, List<Predicate<JsonElement>>> conditions) {
        this.files = files;
        this.styles = Map.copyOf(styles);
        this.options = options;

        for (LayerFactoryFactory factory : factories) {
            this.layers.add(factory.create(this));
        }

        this.conditions = new HashMap<>(conditions);
    }

    public Style getStyle(String name) {
        if (name == null) return Style.EMPTY;

        return this.styles.getOrDefault(name, Style.EMPTY);
    }

    public void draw(JsonObject card, BufferedImage out) {
        StatefulGraphics graphics = new StatefulGraphics(out);

        for (LayerFactory factory : this.layers) {
            Layer layer = factory.create(card, 0, 0);

            if (layer != null) {
                layer.draw(graphics, null);
            }
        }
    }

    @Override
    public BufferedImage getImage(String... images) {
        return this.files.getImage(images);
    }

    @Override
    public InputStream getInputStream(String first, String... more) throws IOException {
        return this.files.getInputStream(first, more);
    }

    public JsonObject getOptions() {
        return this.options;
    }

    public Map<String, Style> getStyles() {
        return this.styles;
    }

    public static final class Builder {
        private final TemplateFiles files;
        private final Map<String, Style> styles = new HashMap<>();
        private final List<LayerFactoryFactory> factories = new ArrayList<>();
        private final Map<String, List<Predicate<JsonElement>>> conditions = new HashMap<>();
        private final JsonObject options = new JsonObject();

        public Builder(TemplateFiles files) {
            this.files = files;
        }

        public Builder layer(LayerFactoryFactory factory) {
            this.factories.add(factory);

            return this;
        }

        public Builder condition(String name, Predicate<JsonElement> predicate) {
            this.conditions.computeIfAbsent(name, k -> new ArrayList<>()).add(predicate);

            return this;
        }

        public Builder style(String name, Style style) {
            this.styles.put(name, style);

            return this;
        }

        public <T> Builder option(String name, JsonElement value) {
            this.options.add(name, value);

            return this;
        }

        public Template build() {
            return new Template(this.files, this.factories, this.options.deepCopy(), this.styles, this.conditions);
        }
    }

    public static class Parser {
        private final JsonObject object;
        private final TemplateFiles files;
        private final Template.Builder builder;

        public Parser(JsonObject object, TemplateFiles files, Map<String, String> args) {
            this.object = object;
            this.files = files;
            this.builder = new Template.Builder(files);

            for (var entry : args.entrySet()) {
                if (entry.getValue().equalsIgnoreCase("true") || entry.getValue().equalsIgnoreCase("false")) {
                    boolean bl = Boolean.parseBoolean(entry.getValue());
                    this.builder.options.add(new String[] { entry.getKey().replace("-", "_") }, bl);
                }
            }
        }

        public Template parse() {
            builder.options.add(Keys.USE_OFFICIAL_ART, true);

            if (object.has("options")) {
                for (Map.Entry<String, JsonElement> entry : object.getAsJsonObject("options").entrySet()) {
                    if (entry.getValue().isJsonPrimitive()) {
                        String key = entry.getKey();
                        JsonPrimitive value = entry.getValue().getAsJsonPrimitive();

                        if (value.isString()) {
                            String stringValue = value.getAsString();
                            builder.options.add(new String[] {entry.getKey()}, stringValue);
                        } else if (value.isBoolean()) {
                            builder.options.add(new String[] {entry.getKey()}, value.getAsBoolean());
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

            if (object.has("conditions")) {
                for (Map.Entry<String, JsonElement> entry : object.getAsJsonObject("conditions").entrySet()) {
                    if (entry.getValue().isJsonObject()) {
                        builder.condition(entry.getKey(), (e -> parseConditions(entry.getValue().getAsJsonObject()).stream().allMatch(p -> p.test(e))));
                    }
                }
            }

            if (object.has("layers")) {
                for (JsonElement layer : object.getAsJsonArray("layers")) {
                    builder.layer(parseLayer(layer, files));
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

        private LayerFactoryFactory parseLayer(JsonElement element, TemplateFiles cache) {
            if (element.isJsonArray()) {
                return parseArray(element.getAsJsonArray(), cache);
            } else if (element.isJsonObject()) {
                return parseLayerFromObject(element.getAsJsonObject(), cache);
            } else {
                throw new RuntimeException(String.format("Layer '%s' is an invalid format", element));
            }
        }

        private LayerFactoryFactory parseArray(JsonArray array, TemplateFiles cache) {
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

        private LayerFactoryFactory parseLayerFromObject(JsonObject object, TemplateFiles cache) {
            List<Predicate<JsonElement>> predicates = object.has("conditions")
                    ? parseConditions(object.getAsJsonObject("conditions"))
                    : Collections.emptyList();

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
                    for (Predicate<JsonElement> predicate : predicates) {
                        if (!predicate.test(card)) return Layer.EMPTY;
                    }

                    return function.create(card, dX, dY);
                };
            };
        }

        private LayerFactoryFactory parseGroup(JsonObject object, TemplateFiles cache) {
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

        private LayerFactoryFactory parseSquish(JsonObject object, TemplateFiles cache) {
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

                            return mainLayerBounds != null && flexLayerBounds != null
                                    ? DrawingUtil.encompassing(mainLayerBounds, flexLayerBounds)
                                    : mainLayerBounds == null ? flexLayerBounds : mainLayerBounds;
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
            Rectangle wrap = object.has("wrap") ? parseRectangle(object.get("wrap").getAsJsonObject()) : null;

            return template -> {
                Style style = object.has("style") && object.get("style").isJsonObject()
                        ? parseStyle(object.getAsJsonObject("style"))
                        : object.has("style") && object.get("style").isJsonPrimitive()
                                ? template.getStyle(object.get("style").getAsString())
                                : Style.EMPTY;

                return (card, x, y) -> {
                    List<List<TextComponent>> text;

                    text = string.equals("$oracle_and_flavor_text{}")
                            ? parseOracleAndFlavorText(style, card)
                            : parseText(style, string, card);

                    text = applyCapitalization(text, style.capitalization(), style.size());

                    return new TextLayer(template, style, text, x, y, alignment, width != null && height != null ? new Rectangle(x, y, width, height) : null, wrap);
                };
            };
        }

        private List<List<TextComponent>> parseOracleAndFlavorText(Style base, JsonObject card) {
            List<List<TextComponent>> text = TextFunction.oracleText(this.builder.styles, this.builder.styles.getOrDefault("oracle", base), card.getAsString("oracle_text"));

            if (card.has("flavor_text")) {
                List<List<TextComponent>> flavor = TextFunction.flavorText(this.builder.styles, this.builder.styles.getOrDefault("flavor", base), card.getAsString("flavor_text"));

                if (!flavor.isEmpty()) {
                    text.add(Collections.singletonList(new TextComponent(base, "\n\n")));
                    text.addAll(flavor);
                }
            }

            return text;
        }

        private static Rectangle parseRectangle(JsonObject object) {
            return new Rectangle(
                    object.get("x").getAsInt(),
                    object.get("y").getAsInt(),
                    object.get("width").getAsInt(),
                    object.get("height").getAsInt()
            );
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
                    if (card.getAsBoolean(Keys.USE_OFFICIAL_ART)) {
                        try {
                            return new ImageLayer(
                                    card.getAsString("image_uris", "art_crop"),
                                    ImageIO.read(new URL(card.getAsString("image_uris", "art_crop")).openStream()), width, height, x, y);
                        } catch (IOException exception) {
                            exception.printStackTrace();
                        }
                    }

                    return Layer.EMPTY;
                };
            } else {
                throw new RuntimeException("Art layer requires at least one field 'width' and/or 'height'");
            }
        }

        private LayerFactoryFactory parseImage(JsonObject object, TemplateFiles cache) {
            return template -> (card, x, y) -> {
                String sub = substitute(object.get("src").getAsString(), card);
                BufferedImage image = cache.getImage(sub);
                return new ImageLayer(sub, image, image.getWidth(), image.getHeight(), x, y);
            };
        }

        private List<Predicate<JsonElement>> parseConditions(JsonObject conditions) {
            if (conditions == null) return Collections.emptyList();

            List<Predicate<JsonElement>> predicates = new ArrayList<>();

            for (Map.Entry<String, JsonElement> entry : conditions.entrySet()) {
                String key = entry.getKey().toLowerCase(Locale.ROOT);

                //noinspection SwitchStatementWithTooFewBranches
                switch (key) {
                    case "or" -> {
                        List<List<Predicate<JsonElement>>> orPredicates = new ArrayList<>();

                        for (JsonElement element : entry.getValue().getAsJsonArray()) {
                            orPredicates.add(parseConditions(element.getAsJsonObject()));
                        }

                        predicates.add(card -> {
                            boolean result = false;

                            for (List<Predicate<JsonElement>> list : orPredicates) {
                                boolean bl = true;

                                for (Predicate<JsonElement> predicate : list) {
                                    bl &= predicate.test(card);
                                }

                                result |= bl;
                            }

                            return result;
                        });
                    }
                    default -> {
                        if (this.builder.conditions.containsKey(key)) {
                            predicates.addAll(this.builder.conditions.get(key));
                        } else if (entry.getValue() instanceof JsonPrimitive primitive) {
                            if (primitive.isString()) {
                                predicates.add(element -> element instanceof JsonObject object && object.has(key) && object.get(key).isJsonPrimitive() && object.get(key).getAsJsonPrimitive().isString() && object.getAsString(key).equalsIgnoreCase(primitive.getAsString()));
                            } else if (primitive.isNumber()) {
                                predicates.add(element -> element instanceof JsonObject object && object.has(key) && object.get(key).isJsonPrimitive() && object.get(key).getAsJsonPrimitive().isNumber() && object.getAsInt(key) == primitive.getAsInt());
                            } else if (primitive.isBoolean()) {
                                predicates.add(element -> {
                                    if (element instanceof JsonObject object && object.has(key) && object.get(key).isJsonPrimitive() && object.get(key).getAsJsonPrimitive().isBoolean()) {
                                        return object.getAsBoolean(key) == primitive.getAsBoolean();
                                    } else if (element instanceof JsonArray array) {
                                        return array.contains(key) == primitive.getAsBoolean();
                                    }

                                    return false;
                                });
                            }
                        } else if (entry.getValue() instanceof JsonObject object) {
                            List<Predicate<JsonElement>> p = parseConditions(object);
                            predicates.add(o -> p.stream().allMatch(predicate -> predicate.test(o.getAsJsonObject().get(key))));
                        }
                    }
                }
            }

            return predicates;
        }

        private String substitute(String string, JsonObject card) {
            Matcher matcher = SUBSTITUTE.matcher(string);

            String s = string;

            while (matcher.find()) {
                String[] key = matcher.group(2).split("\\.");

                JsonElement element = card.get(key);

                String replacement;

                if (element == null) {
                    replacement = "null";
                } else if (element.isJsonArray()) {
                    StringBuilder builder = new StringBuilder();

                    for (JsonElement e : element.getAsJsonArray()) {
                        builder.append(e.getAsString());
                    }

                    replacement = builder.toString();
                } else if (element.isJsonPrimitive()) {
                    replacement = element.getAsString();
                } else {
                    throw new UnsupportedOperationException();
                }

                s = s.replace("${" + matcher.group(2) + "}", replacement);
            }

            return s;
        }

        private List<List<TextComponent>> parseText(Style baseStyle, String string, JsonObject card) {
            Matcher matcher = SUBSTITUTE.matcher(string);

            List<List<TextComponent>> result = new ArrayList<>();

            int previousEnd = 0;

            while (matcher.find()) {
                String priors = string.substring(previousEnd, matcher.start());

                if (!priors.isEmpty()) {
                    result.add(Collections.singletonList(new TextComponent(baseStyle, priors)));
                }

                String function = matcher.group(1);
                String[] key = matcher.group(2).split("\\.");

                JsonElement element = card.get(key);

                String replacement;

                if (element == null) {
                    replacement = "null";
                } else if (element.isJsonArray()) {
                    StringBuilder builder = new StringBuilder();

                    for (JsonElement e : element.getAsJsonArray()) {
                        builder.append(e.getAsString());
                    }

                    replacement = builder.toString();
                } else if (element.isJsonPrimitive()) {
                    replacement = element.getAsString();
                } else {
                    throw new UnsupportedOperationException();
                }

                previousEnd = matcher.end();

                result.addAll(TextFunction.apply(this.builder.styles, baseStyle, function, replacement));
            }

            if (previousEnd != string.length()) {
                String priors = string.substring(previousEnd);
                result.add(Collections.singletonList(new TextComponent(baseStyle, priors)));
            }

            return result;
        }
    }
}
