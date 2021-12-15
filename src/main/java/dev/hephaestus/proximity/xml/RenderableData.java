package dev.hephaestus.proximity.xml;

import dev.hephaestus.proximity.Proximity;
import dev.hephaestus.proximity.api.Values;
import dev.hephaestus.proximity.api.tasks.AttributeModifier;
import dev.hephaestus.proximity.api.tasks.TemplateModification;
import dev.hephaestus.proximity.cards.predicates.CardPredicate;
import dev.hephaestus.proximity.api.json.JsonArray;
import dev.hephaestus.proximity.api.json.JsonElement;
import dev.hephaestus.proximity.api.json.JsonObject;
import dev.hephaestus.proximity.api.json.JsonPrimitive;
import dev.hephaestus.proximity.plugins.TaskHandler;
import dev.hephaestus.proximity.templates.TemplateSource;
import dev.hephaestus.proximity.text.Style;
import dev.hephaestus.proximity.text.Symbol;
import dev.hephaestus.proximity.text.TextComponent;
import dev.hephaestus.proximity.util.*;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RenderableData extends JsonObject implements TemplateSource {
    public static final Pattern SUBSTITUTE = Pattern.compile("\\$(?<function>[\\w.]*)\\{(?<value>[.[^}]]*)}");
    public static final Pattern KEY = Pattern.compile("^(?<key>[a-zA-Z0-9_]+)(?<range>\\[(?<start>[0-9]+)(?<end>:-?[0-9]+)?])?$");

    private final Proximity proximity;
    private final TaskHandler taskHandler;
    private final TemplateSource.Compound source;
    private final XMLElement root;
    private final Map<String, Style> styles = new HashMap<>();
    private final Map<String, CardPredicate> predicates = new HashMap<>();
    private final Map<String, Element> gradients = new LinkedHashMap<>();
    private final List<Symbol> symbols = new ArrayList<>();
    private final Map<String, LayerRenderer> layerRenderers;

    public RenderableData(Proximity proximity, TemplateSource.Compound source, Element root, JsonObject card) {
        this.proximity = proximity;
        this.taskHandler = proximity.getTaskHandler().derive();
        this.copyAll(card);
        this.source = source;
        this.root = new XMLElement(null, root);
        this.layerRenderers = proximity.createLayerRenderers(this);
    }

    public Proximity getProximity() {
        return this.proximity;
    }

    public final String getName() {
        return this.getAsString("name");
    }

    public final int getWidth() {
        return Integer.decode(root.getAttribute("width"));
    }

    public final int getHeight() {
        return Integer.decode(root.getAttribute("height"));
    }

    public Style getStyle(String name) {
        return this.styles.getOrDefault(name, Style.EMPTY);
    }

    public CardPredicate getPredicate(String name) {
        return this.predicates.get(name);
    }

    public boolean hasGradient(String id) {
        return this.gradients.containsKey(id);
    }

    public Element getGradient(String id) {
        return this.gradients.get(id);
    }

    public Iterable<Element> getGradients() {
        return this.gradients.values();
    }

    public List<Symbol> getApplicable(String text) {
        List<Symbol> result = null;

        for (Symbol symbol : this.symbols) {
            if (symbol.anyMatches(this, text)) {
                if (result == null) result = new ArrayList<>();
                result.add(symbol);
            }
        }

        return result == null ? Collections.emptyList() : result;
    }

    public Result<Void> render(StatefulGraphics graphics) {
        Result<Void> init = this.parseOptions()
                .then(this::parseStyles)
                .then(this::parsePredicates)
                .then(this::parseSymbols);

        if (init.isError()) return init;

        if (!Values.HELP.exists(this) || !Values.HELP.get(this)) {
            this.root.apply("Layers", layers -> {
                List<TemplateModification> modifications = this.taskHandler.getTasks(TemplateModification.DEFINITION);

                for (TemplateModification modification : modifications) {
                    modification.apply(this, layers);
                }
            });

            return this.root.apply("Layers", (Function<XMLElement, Result<Void>>) layers -> {
                List<String> errors = new ArrayList<>(0);

                layers.iterate((layer, i) -> {
                    LayerRenderer renderable = this.layerRenderers.get(layer.getTagName());

                    if (renderable != null) {
                        renderable.render(this, layer, graphics, null, true, new Box<>(0F), null)
                                .ifError(errors::add);
                    }
                });

                return errors.isEmpty() ? Result.of(null)
                        : Result.error("Error(s) rendering cards:\n\t%s", String.join("\n\t", errors));
            }).orElse(Result.of(null));
        } else {
            return init;
        }
    }

    private Result<Void> parseSymbols() {
        List<String> errors = new ArrayList<>(0);

        this.root.iterate("Symbols", (symbols, i) -> symbols.iterate((symbol, j) -> {
            String representation = symbol.getAttribute("representation");
            List<CardPredicate> predicates = new ArrayList<>(0);
            List<TextComponent> glyphs = new ArrayList<>(3);

            XMLUtil.iterate(symbol, "Conditions", (predicate, k) ->
                    XMLUtil.parsePredicate(predicate, e -> null, this::exists)
                            .ifError(errors::add)
                            .ifPresent(predicates::add));

            symbol.iterate((glyph, k) -> {
                Result<Style> style = Style.parse(glyph);

                if (style.isOk()) {
                    glyphs.add(new TextComponent.Literal(style.get(), glyph.getAttribute("glyphs")));
                } else {
                    errors.add(style.getError());
                }
            });

            this.symbols.add(new Symbol(representation, glyphs, predicates));
        }));

        return errors.isEmpty() ? Result.of(null)
                : Result.error("Error(s) parsing symbols:\n\t%s", String.join("\n\t", errors));
    }

    public Result<Void> parseOptions() {
        List<String> errors = new ArrayList<>();
        JsonObject options = this.getAsJsonObject(Keys.OPTIONS);
        boolean help = Values.HELP.exists(this) && Values.HELP.get(this);

        XMLUtil.iterate(this.root, "Options", (optionList, i) ->
                XMLUtil.iterate(optionList, (option, j) -> {
                    String id = option.getAttribute("id");

                    if (help) {
                        System.out.printf("Key: %s%n", id);

                        if (option.hasAttribute("default")) {
                            System.out.printf("Default: %s%n", option.wrapped.getAttribute("default"));
                        }
                    }

                    try {
                        switch (option.getTagName()) {
                            case "Enumeration" -> {
                                String defaultValue = option.getAttribute("default");
                                Box<Boolean> defaultValuePresent = new Box<>(false);

                                if (help) {
                                    System.out.print("Possible Values: ");
                                }

                                XMLUtil.iterate(option, (element, k) -> {
                                    String value = element.getAttribute("value");

                                    if (help) {
                                        if (k > 0) {
                                            System.out.print(", ");
                                        }

                                        System.out.print(value);
                                    } else {
                                        defaultValuePresent.set(defaultValuePresent.get() || value.equals(defaultValue));
                                    }
                                });

                                if (!help) {
                                    if (defaultValuePresent.get() && !options.has(id)) {
                                        options.addProperty(id, defaultValue);
                                    } else if (!defaultValuePresent.get()) {
                                        errors.add(String.format("Default value '%s' not present in Enumeration '%s'", defaultValue, id));
                                    }
                                }
                            }
                            case "ToggleOption" -> {
                                if (help) {
                                    System.out.println("Possible Values: [true|false]");
                                } else {
                                    if (!options.has(id) && option.hasAttribute("default")) {
                                        options.addProperty(id, Boolean.parseBoolean(option.getAttribute("default")));
                                    }
                                }
                            }
                            case "StringOption" -> {
                                if (!help) {
                                    if (!options.has(id) && option.hasAttribute("default")) {
                                        options.addProperty(id, option.getAttribute("default"));
                                    }
                                }
                            }
                        }

                        if (help) {
                            if (option.wrapped.getUserData("comment") != null) {
                                System.out.println(option.wrapped.getUserData("comment"));
                            }

                            System.out.println();
                        }
                    } catch (NoSuchAttributeException e) {
                        Proximity.LOG.debug(e.getMessage());
                    }
                })
        );

        if (errors.isEmpty()) {
            return Result.of(null);
        } else {
            return Result.error("Error(s) parsing options:\n\t%s", String.join("\n\t", errors));
        }
    }

    private Result<Void> parseStyles() {
        List<String> errors = new ArrayList<>();

        this.root.iterate("Styles", (styles, i) ->
                styles.iterate((style, j) -> {
                    switch (style.getTagName()) {
                        case "Style" -> {
                            if (!style.hasAttribute("name")) {
                                errors.add(String.format("Style #%d missing name attribute", j));
                            }

                            String name = style.getAttribute("name");

                            Result<Style> r = Style.parse(style);

                            if (r.isError()) {
                                errors.add(r.getError());
                            } else {
                                this.styles.put(name, r.get());
                            }
                        }
                        case "linearGradient", "radialGradient" -> {
                            List<CardPredicate> predicates = new ArrayList<>();

                            style.apply("Conditions", (XMLElement conditions) -> conditions.iterate((predicate, k) ->
                                    XMLUtil.parsePredicate(predicate, RenderableData.this::getPredicate, RenderableData.this::exists)
                                            .ifError(errors::add)
                                            .ifPresent(predicates::add)));

                            if (!errors.isEmpty()) {
                                Proximity.LOG.warn("Error(s) parsing predicates:\n\t{}", String.join("\n\t", errors));
                            }

                            for (CardPredicate predicate : predicates) {
                                Result<Boolean> result = predicate.test(RenderableData.this);

                                if (result.isOk() && !result.get()) {
                                    return;
                                }
                            }

                            Element element = style.wrapped;

                            Optional<Element> conditions = style.apply("Conditions", e -> e.wrapped);

                            conditions.ifPresent(element::removeChild);

                            this.gradients.put(style.getAttribute("id"), (Element) element.cloneNode(true));
                        }
                    }
                })
        );

        if (errors.isEmpty()) {
            return Result.of(null);
        } else {
            return Result.error("Error(s) parsing styles:\n\t%s", String.join("\n\t", errors));
        }
    }

    private Result<Void> parsePredicates() {
        List<String> errors = new ArrayList<>();

        XMLUtil.iterate(this.root, "ConditionDefinitions", (element, i) ->
                XMLUtil.iterate(element, "Definition", (definition, j) -> {
                    String name = definition.getAttribute("name");
                    List<CardPredicate> definedPredicates = new ArrayList<>();

                    XMLUtil.iterate(definition, (predicate, k) ->
                            XMLUtil.parsePredicate(predicate, e -> null, this::exists)
                                    .ifError(errors::add)
                                    .ifPresent(definedPredicates::add));

                    this.predicates.put(name, definedPredicates.size() == 1 ? definedPredicates.get(0) : new CardPredicate.And(definedPredicates));
                })
        );

        if (errors.isEmpty()) {
            return Result.of(null);
        } else {
            return Result.error("Error(s) parsing predicates:\n\t%s", String.join("\n\t", errors));
        }
    }

    @Override
    public BufferedImage getImage(String file) {
        return this.source.getImage(file);
    }

    @Override
    public InputStream getInputStream(String file) throws IOException {
        return this.source.getInputStream(file);
    }

    @Override
    public boolean exists(String file) {
        return this.source.exists(file);
    }

    @Override
    public String getTemplateName() {
        return this.source.getTemplateName();
    }

    public Map<String, Style> getStyles() {
        return this.styles;
    }

    public TaskHandler getTaskHandler() {
        return this.taskHandler;
    }

    public LayerRenderer getLayerRenderer(String tagName) {
        return this.layerRenderers.get(tagName);
    }

    public final class XMLElement {
        private final Element wrapped;
        private final Map<LayerProperty<?>, Object> properties = new WeakHashMap<>();
        private final Deque<Pair<String, String>> attributes = new ArrayDeque<>();
        private final XMLElement parent;

        public XMLElement(XMLElement parent, Element wrapped) {
            this.parent = parent;
            this.wrapped = wrapped;

            this.iterate("AttributeModifier", (element, i) -> {
                List<String> errors = new ArrayList<>();
                List<CardPredicate> predicates = new ArrayList<>();

                XMLUtil.iterate(element, (predicate, j) ->
                            XMLUtil.parsePredicate(predicate, RenderableData.this::getPredicate, RenderableData.this::exists)
                                    .ifError(errors::add)
                                    .ifPresent(predicates::add));

                if (!errors.isEmpty()) {
                    Proximity.LOG.warn("Error(s) parsing predicates:\n\t{}", String.join("\n\t", errors));
                }

                for (CardPredicate predicate : predicates) {
                    Result<Boolean> result = predicate.test(RenderableData.this);

                    if (result.isOk() && !result.get()) {
                        return;
                    }
                }

                this.setAttribute(element.getAttribute("name"), element.getAttribute("value"));
            });

            this.iterate("CardModifier", (element, i) -> {
                List<String> errors = new ArrayList<>();
                List<CardPredicate> predicates = new ArrayList<>();

                XMLUtil.iterate(element, (predicate, j) ->
                        XMLUtil.parsePredicate(predicate, RenderableData.this::getPredicate, RenderableData.this::exists)
                                .ifError(errors::add)
                                .ifPresent(predicates::add));

                if (!errors.isEmpty()) {
                    Proximity.LOG.warn("Error(s) parsing predicates:\n\t{}", String.join("\n\t", errors));
                }

                for (CardPredicate predicate : predicates) {
                    Result<Boolean> result = predicate.test(RenderableData.this);

                    if (result.isOk() && !result.get()) {
                        return;
                    }
                }

                Result<JsonElement> value = ParsingUtil.parseStringValue(element.getAttribute("value"));

                if (value.isOk()) {
                    RenderableData.this.add(element.getAttribute("key").split("\\."), value.get());
                } else {
                    Proximity.LOG.warn("Error parsing value:\n\t{}", value.getError());
                }
            });

            this.iterate((element, i) -> {
                LayerProperty<?> property = LayerProperty.get(element.getTagName());

                if (property != null) {
                    List<String> errors = new ArrayList<>();
                    List<CardPredicate> predicates = new ArrayList<>();

                    element.apply("Conditions", (XMLElement conditions) -> conditions.iterate((predicate, j) ->
                            XMLUtil.parsePredicate(predicate, RenderableData.this::getPredicate, RenderableData.this::exists)
                                    .ifError(errors::add)
                                    .ifPresent(predicates::add)));

                    if (!errors.isEmpty()) {
                        Proximity.LOG.warn("Error(s) parsing predicates:\n\t{}", String.join("\n\t", errors));
                    }

                    for (CardPredicate predicate : predicates) {
                        Result<Boolean> result = predicate.test(RenderableData.this);

                        if (result.isOk() && !result.get()) {
                            return;
                        }
                    }

                    Result<?> result = property.parse(element);

                    if (result.isOk()) {
                        this.properties.put(property, result.get());
                    } else {
                        Proximity.LOG.warn("Failed to parse property: {}", result.getError());
                    }
                }
            });
        }

        public String getParentId() {
            return this.parent == null ? "" : this.parent.getId();
        }

        public String getId() {
            return id(this.parent == null ? "" : this.parent.getId(), this.wrapped.getAttribute("id"));
        }

        public String getTagName() {
            return this.wrapped.getTagName();
        }

        public boolean hasAttribute(String name) {
            return this.wrapped.hasAttribute(name);
        }

        public String getAttribute(String name) {
            String value = this.wrapped.getAttribute(name);
            Matcher matcher = SUBSTITUTE.matcher(value);
            StringBuilder result = new StringBuilder();

            int previousEnd = 0;

            String priors;

            while (matcher.find()) {
                 priors = value.substring(previousEnd, matcher.start());

                if (!priors.isEmpty()) {
                    result.append(priors);
                }

                String function = matcher.group("function");

                JsonElement element = RenderableData.this.getFromFullPath(matcher.group("value"));

                previousEnd = matcher.end();

                AttributeModifier attributeModifier = RenderableData.this.taskHandler.getTask(AttributeModifier.DEFINITION, function);

                if (element == null) {
                    throw new NoSuchAttributeException(String.format("Element '%s' not found. Used by element '%s', line number '%s'", matcher.group("value"), this.getId(), this.wrapped.getUserData("lineNumber")));
                }

                if (attributeModifier != null) {
                    result.append(attributeModifier.apply(element, RenderableData.this));
                } else {
                    result.append(element instanceof JsonPrimitive primitive && primitive.isString()
                            ? element.getAsString()
                            : element.toString());
                }
            }

            String tail = value.substring(previousEnd);

            if (!tail.isEmpty()) {
                result.append(tail);
            }

            return result.toString();
        }

        public static JsonElement handle(JsonElement element, int beginning, int end) {
            if (end > 0 && beginning > end) {
                throw new UnsupportedOperationException("Beginning index cannot be before end index");
            } else if (end >= (element instanceof JsonArray array ? array.size() : element.getAsString().length())) {
                throw new UnsupportedOperationException("End index out of bounds");
            } else if (beginning < 0) {
                throw new UnsupportedOperationException("Beginning index out of bounds");
            }

            if (element instanceof JsonArray array) {
                if (beginning == 0 && (end == -1 || end == array.size() - 1)) {
                    return array;
                } else {
                    JsonArray result = new JsonArray();

                    for (int i = beginning; i < (end < 0 ? array.size() + 1 + end : end); ++i) {
                        result.add(array.get(i));
                    }

                    return result;
                }
            } else {
                String s = element.getAsString();
                return new JsonPrimitive(s.substring(beginning, (end < 0 ? s.length() + 1 + end : end)));
            }
        }

        public int getInteger(String name) {
            return this.hasAttribute(name) ? (int) Long.decode(this.getAttribute(name)).longValue() : 0;
        }

        public void setAttribute(String name, String value) {
            if (value == null) {
                this.wrapped.removeAttribute(name);
            } else {
                this.wrapped.setAttribute(name, value);
            }
        }

        public void pushAttribute(String name, String value) {
            this.attributes.addFirst(new Pair<>(name, this.wrapped.hasAttribute(name) ? this.wrapped.getAttribute(name) : null));
            this.setAttribute(name, value);
        }

        public void pushAttribute(String name, int value) {
            this.pushAttribute(name, Integer.toString(value));
        }

        public void popAttribute() {
            Pair<String, String> attribute = this.attributes.removeFirst();
            this.setAttribute(attribute.left(), attribute.right());
        }

        public void popAttributes(int count) {
            for (int i = 0; i < count; ++i) {
                this.popAttribute();
            }
        }

        @SuppressWarnings("unchecked")
        public <T> T getProperty(LayerProperty<T> property) {
            return this.properties.containsKey(property) ? (T) this.properties.get(property)
                    : this.parent == null ? null : this.parent.getProperty(property);
        }

        public <T> T getProperty(LayerProperty<T> property, T orDefault) {
            T value = this.getProperty(property);

            return value == null ? orDefault : value;
        }

        public <T> void setProperty(LayerProperty<T> property, T value) {
            this.properties.put(property, value);
        }

        public int iterate(String tagName, BiConsumer<XMLElement, Integer> elementConsumer) {
            return this.iterate(this.wrapped.getElementsByTagName(tagName), elementConsumer);
        }

        public int iterate(BiConsumer<XMLElement, Integer> elementConsumer) {
            return this.iterate(this.wrapped.getChildNodes(), elementConsumer);
        }

        private int iterate(NodeList nodes, BiConsumer<XMLElement, Integer> elementConsumer) {
            int index = 0;

            for (int i = 0; i < nodes.getLength(); ++i) {
                Node node = nodes.item(i);

                if (node instanceof Element && node.getParentNode() == this.wrapped) {
                    elementConsumer.accept(new XMLElement(this, (Element) node), index++);
                }
            }

            return index;
        }

        public <T> Optional<T> apply(Function<XMLElement, T> function) {

            NodeList nodes = this.wrapped.getChildNodes();

            for (int i = 0; i < nodes.getLength(); ++i) {
                Node node = nodes.item(i);

                if (node instanceof Element && node.getParentNode() == this.wrapped) {
                    return Optional.ofNullable(function.apply(new XMLElement(this, (Element) node)));
                }
            }

            return Optional.empty();
        }

        public <T> Optional<T> apply(String tagName, Function<XMLElement, T> function) {

            NodeList nodes = this.wrapped.getChildNodes();

            for (int i = 0; i < nodes.getLength(); ++i) {
                Node node = nodes.item(i);

                if (node instanceof Element && node.getParentNode() == this.wrapped && ((Element) node).getTagName().equals(tagName)) {
                    return Optional.ofNullable(function.apply(new XMLElement(this, (Element) node)));
                }
            }

            return Optional.empty();
        }

        public void apply(String tagName, Consumer<XMLElement> consumer) {
            NodeList nodes = this.wrapped.getChildNodes();

            for (int i = 0; i < nodes.getLength(); ++i) {
                Node node = nodes.item(i);

                if (node instanceof Element && node.getParentNode() == this.wrapped && ((Element) node).getTagName().equals(tagName)) {
                    consumer.accept(new XMLElement(this, (Element) node));
                    break;
                }
            }
        }

        public static String id(String parentId, String id) {
            return parentId + (parentId.isEmpty() || id.isEmpty() ? "" : ".") + id;
        }

        public XMLElement getParent() {
            return this.parent;
        }

        public String getAttributeRaw(String key) {
            return this.wrapped.getAttribute(key);
        }
    }
}
