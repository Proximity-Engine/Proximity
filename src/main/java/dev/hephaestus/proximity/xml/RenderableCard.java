package dev.hephaestus.proximity.xml;

import dev.hephaestus.proximity.Proximity;
import dev.hephaestus.proximity.cards.predicates.CardPredicate;
import dev.hephaestus.proximity.json.JsonElement;
import dev.hephaestus.proximity.json.JsonObject;
import dev.hephaestus.proximity.templates.TemplateSource;
import dev.hephaestus.proximity.text.Style;
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

public final class RenderableCard extends JsonObject implements TemplateSource {
    public static final Pattern SUBSTITUTE = Pattern.compile("\\$(\\w*)\\{(\\w+(?:\\.\\w+)*)?}");

    private final TemplateSource source;
    private final XMLElement root;
    private final Map<String, Style> styles = new HashMap<>();
    private final Map<String, CardPredicate> predicates = new HashMap<>();

    public RenderableCard(TemplateSource source, Element root, JsonObject card) {
        this.copyAll(card);
        this.source = source;
        this.root = new XMLElement(null, root);
    }

    public final String getName() {
        return this.getAsString("name");
    }

    public final String getSet() {
        return this.getAsString("set");
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

    public Result<Void> render(StatefulGraphics graphics) {
        Result<Void> init = this.parseOptions()
                .then(this::parseStyles)
                .then(this::parsePredicates);

        if (init.isError()) return init;

        return this.root.apply("layers", (Function<XMLElement, Result<Void>>) layers -> {
            List<String> errors = new ArrayList<>();

            layers.iterate((layer, i) -> {
                LayerRenderer renderable = LayerRenderer.get(layer.getTagName());

                if (renderable != null) {
                    renderable.render(this, layer, graphics, null, true, 0, null)
                            .ifError(errors::add);
                }
            });

            return errors.isEmpty() ? Result.of(null)
                    : Result.error("Error rendering cards:\n\t%s", String.join("\n\t", errors));
        }).orElse(Result.of(null));
    }

    private Result<Void> parseOptions() {
        List<String> errors = new ArrayList<>();
        JsonObject options = this.getAsJsonObject(Keys.OPTIONS);

        XMLUtil.iterate(this.root, "options", (optionList, i) ->
                XMLUtil.iterate(optionList, (option, j) -> {
                    String id = option.getAttribute("id");

                    switch (option.getTagName()) {
                        case "Enumeration" -> {
                            String defaultValue = option.getAttribute("default");
                            Box<Boolean> defaultValuePresent = new Box<>(false);

                            XMLUtil.iterate(option, (element, k) ->
                                    defaultValuePresent.set(defaultValuePresent.get()
                                            || element.getAttribute("value").equals(defaultValue)));

                            if (defaultValuePresent.get() && !options.has(id)) {
                                options.addProperty(id, defaultValue);
                            } else if (!defaultValuePresent.get()) {
                                errors.add(String.format("Default value '%s' not present in Enumeration '%s'", defaultValue, id));
                            }
                        }
                        case "ToggleOption" -> {
                            if (!options.has(id)) {
                                options.addProperty(id, Boolean.parseBoolean(option.getAttribute("default")));
                            }
                        }
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

        this.root.iterate("styles", (styles, i) ->
                styles.iterate("Style", (style, j) -> {
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

        XMLUtil.iterate(this.root, "condition_definitions", (element, i) ->
                XMLUtil.iterate(element, "ConditionDefinition", (definition, j) -> {
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

    public final class XMLElement {
        private final Element wrapped;
        private final Map<LayerProperty<?>, Object> properties = new WeakHashMap<>();
        private final Deque<Pair<String, String>> attributes = new ArrayDeque<>();
        private final XMLElement parent;

        private XMLElement(XMLElement parent, Element wrapped) {
            this.parent = parent;
            this.wrapped = wrapped;

            this.iterate("AttributeModifier", (element, i) -> {
                List<String> errors = new ArrayList<>();
                List<CardPredicate> predicates = new ArrayList<>();

                XMLUtil.iterate(element, (predicate, j) ->
                            XMLUtil.parsePredicate(predicate, RenderableCard.this::getPredicate, RenderableCard.this::exists)
                                    .ifError(errors::add)
                                    .ifPresent(predicates::add));

                if (!errors.isEmpty()) {
                    Proximity.LOG.warn("Error(s) parsing predicates:\n\t{}", String.join("\n\t", errors));
                }

                for (CardPredicate predicate : predicates) {
                    Result<Boolean> result = predicate.test(RenderableCard.this);

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
                        XMLUtil.parsePredicate(predicate, RenderableCard.this::getPredicate, RenderableCard.this::exists)
                                .ifError(errors::add)
                                .ifPresent(predicates::add));

                if (!errors.isEmpty()) {
                    Proximity.LOG.warn("Error(s) parsing predicates:\n\t{}", String.join("\n\t", errors));
                }

                for (CardPredicate predicate : predicates) {
                    Result<Boolean> result = predicate.test(RenderableCard.this);

                    if (result.isOk() && !result.get()) {
                        return;
                    }
                }

                RenderableCard.this.add(element.getAttribute("key").split("\\."), ParsingUtil.parseStringValue(element.getAttribute("value")));
            });

            this.iterate((element, i) -> {
                LayerProperty<?> property = LayerProperty.get(element.getTagName());

                if (property != null) {
                    List<String> errors = new ArrayList<>();
                    List<CardPredicate> predicates = new ArrayList<>();

                    element.apply("conditions", (XMLElement conditions) -> conditions.iterate((predicate, j) ->
                            XMLUtil.parsePredicate(predicate, RenderableCard.this::getPredicate, RenderableCard.this::exists)
                                    .ifError(errors::add)
                                    .ifPresent(predicates::add)));

                    if (!errors.isEmpty()) {
                        Proximity.LOG.warn("Error(s) parsing predicates:\n\t{}", String.join("\n\t", errors));
                    }

                    for (CardPredicate predicate : predicates) {
                        Result<Boolean> result = predicate.test(RenderableCard.this);

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

            while (matcher.find()) {
                String[] key = matcher.group(2).split("\\.");

                JsonElement element = RenderableCard.this.get(key);

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

                value = value.replace("${" + matcher.group(2) + "}", replacement);
            }

            return value;
        }

        public int getInteger(String name) {
            return this.hasAttribute(name) ? Integer.decode(this.getAttribute(name)) : 0;
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
    }
}
