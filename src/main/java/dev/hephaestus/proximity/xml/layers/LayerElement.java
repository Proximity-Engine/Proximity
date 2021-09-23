package dev.hephaestus.proximity.xml.layers;

import dev.hephaestus.proximity.cards.predicates.CardPredicate;
import dev.hephaestus.proximity.json.JsonElement;
import dev.hephaestus.proximity.json.JsonObject;
import dev.hephaestus.proximity.templates.Template;
import dev.hephaestus.proximity.templates.layers.Layer;
import dev.hephaestus.proximity.util.Result;
import dev.hephaestus.proximity.util.XMLUtil;
import dev.hephaestus.proximity.xml.AttributeModifier;
import dev.hephaestus.proximity.xml.Properties;
import dev.hephaestus.proximity.xml.XMLElement;
import org.w3c.dom.Element;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class LayerElement<L extends Layer> extends XMLElement<LayerElement<L>> {
    protected static final Pattern SUBSTITUTE = Pattern.compile("\\$(\\w*)\\{(\\w+(?:\\.\\w+)*)?}");

    private static final Map<String, Factory<?>> LAYER_FACTORIES = new HashMap<>();

    private String id;
    private Integer x, y;
    private CardPredicate predicate;

    public LayerElement(Element element) {
        super(element);
    }


    public final String getId() {
        Objects.requireNonNull(this.id);

        return this.id;
    }

    protected final int getX() {
        Objects.requireNonNull(this.x);

        return this.x;
    }

    protected final int getY() {
        Objects.requireNonNull(this.y);

        return this.y;
    }

    protected final void addPredicate(CardPredicate predicate) {
        this.predicate = new CardPredicate.And(this.predicate, predicate);
    }

    public abstract Result<LayerElement<L>> createFactory(Template template);
    protected abstract Result<LayerElement<L>> parseLayer(Context context, Properties properties);
    protected abstract Result<L> createLayer(String parentId, JsonObject card);

    public final Result<? extends Layer> create(String parentId, JsonObject card) {
        Result<Boolean> r = this.predicate.test(card);

        if (r.isError()) {
            return Result.error(r.getError());
        } else if (r.get()) {
            return this.createLayer(parentId, card);
        } else {
            return Result.of(Layer.EMPTY);
        }
    }

    @Override
    protected final Result<LayerElement<L>> parseElement(Context context, List<AttributeModifier> modifiers, Properties properties) {
        this.id = this.element.hasAttribute("id") ? this.element.getAttribute("id") : "";
        this.x = (this.element.hasAttribute("x") ? Integer.decode(this.element.getAttribute("x")) : 0);
        this.y = (this.element.hasAttribute("y") ? Integer.decode(this.element.getAttribute("y")) : 0);

        Result<List<CardPredicate>> predicates = XMLUtil.applyToFirstElement(this.element, "conditions", e ->
                parseConditions(e, context), Result.of(Collections.emptyList()));

        if (predicates.isError()) {
            return Result.error(predicates.getError());
        } else {
            this.predicate = new CardPredicate.And(predicates.get());
        }

        return parseLayer(context, properties);
    }

    public static <T extends LayerElement<?>> Factory<T> register(Factory<T> factory, String... tagNames) {
        for (String tagName : tagNames) {
            LAYER_FACTORIES.put(tagName, factory);
        }

        return factory;
    }

    @SuppressWarnings("unchecked")
    public static <T extends LayerElement<?>> Factory<T> getFactory(String tagName) {
        return (Factory<T>) LAYER_FACTORIES.get(tagName);
    }

    public interface Factory<T extends LayerElement<?>> {
        T create(Element element);
    }

    protected static String substitute(String string, JsonObject card) {
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
}
