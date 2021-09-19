package dev.hephaestus.proximity.templates;


import dev.hephaestus.proximity.cards.TextBody;
import dev.hephaestus.proximity.cards.TextParser;
import dev.hephaestus.proximity.text.TextComponent;
import dev.hephaestus.proximity.cards.ElementPredicate;
import dev.hephaestus.proximity.cards.Predicate;
import dev.hephaestus.proximity.json.JsonArray;
import dev.hephaestus.proximity.json.JsonElement;
import dev.hephaestus.proximity.json.JsonObject;
import dev.hephaestus.proximity.json.JsonPrimitive;
import dev.hephaestus.proximity.text.Alignment;
import dev.hephaestus.proximity.text.Style;
import dev.hephaestus.proximity.util.DrawingUtil;
import dev.hephaestus.proximity.util.Keys;
import dev.hephaestus.proximity.util.Result;
import dev.hephaestus.proximity.util.StatefulGraphics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// TODO: The main class has been broken up, but this one still needs some work.
public final class Template {
    private static final Pattern SUBSTITUTE = Pattern.compile("\\$(\\w*)\\{(\\w+(?:\\.\\w+)*)}");

    private final Map<String, Style> styles;
    private final List<LayerFactory> layers = new ArrayList<>();
    private final JsonObject options;
    private final TemplateSource source;
    private final Logger log;

    private Template(TemplateSource source, List<LayerFactoryFactory> factories, JsonObject options, Map<String, Style> styles, Logger log) {
        this.source = source;
        this.styles = Map.copyOf(styles);
        this.options = options;
        this.log = log;

        for (LayerFactoryFactory factory : factories) {
            this.layers.add(factory.create(this));
        }

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

    public TemplateSource getSource() {
        return this.source;
    }

    public JsonObject getOptions() {
        return this.options;
    }

    public Logger log() {
        return this.log;
    }

    public static final class Builder {
        private final TemplateSource files;
        private final Map<String, Style> styles = new HashMap<>();
        private final List<LayerFactoryFactory> factories = new ArrayList<>();
        private final Map<String, JsonObject> conditions = new HashMap<>();
        private final JsonObject options = new JsonObject();

        private Logger log;

        public Builder(TemplateSource files) {
            this.files = files;
        }

        public Builder log(Logger log) {
            this.log = log;

            return this;
        }

        public Builder layer(LayerFactoryFactory factory) {
            this.factories.add(factory);

            return this;
        }

        public Builder condition(String name, JsonObject condition) {
            this.conditions.put(name, condition);

            return this;
        }

        public Builder style(String name, Style style) {
            this.styles.put(name, style);

            return this;
        }

        public Template build() {
            return new Template(this.files, this.factories, this.options.deepCopy(), this.styles, this.log);
        }
    }

    public static class Parser {
        private final Logger log;
        private final JsonObject object;
        private final TemplateSource files;
        private final Template.Builder builder;

        public Parser(String name, JsonObject object, TemplateSource files, JsonObject options) {
            this.log = LogManager.getLogger("Proximity/" + name);
            this.object = object;
            this.files = files;
            this.builder = new Template.Builder(files);

            for (var entry : options.entrySet()) {
                this.builder.options.add(entry.getKey(), entry.getValue().deepCopy());
            }
        }

        public Template parse() {
            builder.options.add(Keys.USE_OFFICIAL_ART, true);

            builder.log(this.log);

            if (object.has("options")) {
                for (Map.Entry<String, JsonElement> entry : object.getAsJsonObject("options").entrySet()) {
                    if (entry.getValue().isJsonPrimitive()) {
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
                        builder.condition(entry.getKey(), entry.getValue().getAsJsonObject());
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

        private LayerFactoryFactory parseLayer(JsonElement element, TemplateSource cache) {
            if (element.isJsonArray()) {
                return parseArray(element.getAsJsonArray(), cache);
            } else if (element.isJsonObject()) {
                return parseLayerFromObject(element.getAsJsonObject(), cache);
            } else {
                throw new RuntimeException(String.format("Layer '%s' is an invalid format", element));
            }
        }

        private LayerFactoryFactory parseArray(JsonArray array, TemplateSource cache) {
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

        private LayerFactoryFactory parseLayerFromObject(JsonObject object, TemplateSource cache) {
            List<Predicate> predicates = object.has("conditions")
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
                    Layer layer = Layer.EMPTY;

                    if (builder.options.getAsBoolean("debug")) {
                        try {
                            layer = function.create(card, dX, dY);
                            log.debug("\n\nLAYER: {}", layer);
                        } catch (Throwable throwable) {
                            log.debug("\n\nLAYER: ???");
                        }
                    }

                    for (Predicate predicate : predicates) {
                        Result<Boolean> result = predicate.test(card);

                        if (result.isError() || !result.get()) {
                            return Layer.EMPTY;
                        }
                    }

                    if (!builder.options.getAsBoolean("debug")) {
                        layer = function.create(card, dX, dY);
                    }

                    return layer;
                };
            };
        }

        private LayerFactoryFactory parseGroup(JsonObject object, TemplateSource cache) {
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

        private LayerFactoryFactory parseSquish(JsonObject object, TemplateSource cache) {
            LayerFactoryFactory main = parseLayer(object.get("main"), cache);
            LayerFactoryFactory flex = parseLayer(object.get("flex"), cache);

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
                    Alignment al = alignment;

                    if (string.equals("$oracle_and_flavor_text{}")) {
                        TextBody oracle = new TextParser(card.getAsString("oracle_text"), this.builder.styles, builder.styles.getOrDefault("oracle", style), "\n\n", this.builder.options).parseOracle();

                        if (card.has("flavor_text")) {
                            text = oracle.text();

                            text.add(Collections.singletonList(new TextComponent(style, "\n\n")));
                            text.addAll(TextFunction.flavorText(this.builder.styles, this.builder.styles.getOrDefault("flavor", style), card.getAsString("flavor_text"), this.builder.options));
                        } else {
                            if (oracle.alignment() == Alignment.CENTER && width != null) {
                                al = Alignment.CENTER;
                                x += width / 2;
                            }

                            text = oracle.text();
                        }
                    } else {
                        text = parseText(style, string, card);
                    }

                    text = applyCapitalization(text, style.capitalization(), style.size());

                    return new TextLayer(template, style, text, x, y, al, width != null && height != null ? new Rectangle(x, y, width, height) : null, wrap);
                };
            };
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

        private LayerFactoryFactory parseImage(JsonObject object, TemplateSource cache) {
            return template -> (card, x, y) -> {
                String sub = substitute(object.get("src").getAsString(), card);
                BufferedImage image = cache.getImage(sub);
                return new ImageLayer(sub, image, image.getWidth(), image.getHeight(), x, y);
            };
        }

        private List<Predicate> flb(JsonObject conditions, String... path) {
            if (conditions == null) return Collections.emptyList();

            List<Predicate> result = new ArrayList<>();

            for (var entry : conditions.entrySet()) {
                String key = entry.getKey();
                JsonElement value = entry.getValue();

                if (key.equals("or")) {
                    List<Predicate> ps = new ArrayList<>();

                    for (JsonElement element : value.getAsJsonArray()) {
                        ps.addAll(flb(element.getAsJsonObject(), path));
                    }

                    result.add(new Predicate.Or(ps));
                } else if (this.builder.conditions.containsKey(key)) {
                    String[] p = Arrays.copyOf(path, path.length + 1);
                    p[path.length] = key;

                    result.addAll(flb(builder.conditions.get(key), p));
                } else if (value.isJsonObject()) {
                    String[] p = Arrays.copyOf(path, path.length + 1);
                    p[path.length] = key;

                    result.addAll(flb(value.getAsJsonObject(), p));
                } else if (value.isJsonPrimitive()) {
                    String[] p = Arrays.copyOf(path, path.length + 1);
                    p[path.length] = key;

                    ElementPredicate.of(log, p, value)
                            .ifPresent(result::add)
                            .ifError(log::error);
                }
            }

            return result;
        }

        private List<Predicate> parseConditions(JsonObject conditions) {
            if (conditions == null) return Collections.emptyList();

            List<Predicate> result = flb(conditions);

            if (conditions.has("or")) {
                List<Predicate> or = new ArrayList<>();

                for (var entry : conditions.getAsJsonArray("or")) {
                    or.addAll(parseConditions(entry.getAsJsonObject()));
                }

                result.add(new Predicate.Or(or));
            }

            return result;
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

                result.addAll(TextFunction.apply(this.builder.styles, baseStyle, function, replacement, this.builder.options));
            }

            if (previousEnd != string.length()) {
                String priors = string.substring(previousEnd);
                result.add(Collections.singletonList(new TextComponent(baseStyle, priors)));
            }

            return result;
        }
    }
}
