package dev.hephaestus.proximity.xml;

import dev.hephaestus.proximity.cards.predicates.CardPredicate;
import dev.hephaestus.proximity.json.JsonObject;
import dev.hephaestus.proximity.util.Pair;
import dev.hephaestus.proximity.util.Result;
import dev.hephaestus.proximity.util.XMLUtil;
import org.w3c.dom.Element;

import java.util.*;
import java.util.function.Function;

public abstract class XMLElement<T> {
    protected final Element element;
    private final LayerProperty<?>[] properties;

    public XMLElement(Element element, LayerProperty<?>... properties) {
        this.element = element;
        this.properties = properties;
    }

    public XMLElement(Element element) {
        this(element, LayerProperty.STYLE, LayerProperty.WRAP);
    }

    protected abstract Result<T> parseElement(Context context, List<AttributeModifier> modifiers, Properties properties);

    public final Result<T> parse(Context context, Properties inheritedProperties) {
        Result<List<AttributeModifier>> modifiers = this.parseAttributeModifiers(context);

        if (modifiers.isError()) return Result.error(modifiers.getError());

        Result<Map<LayerProperty<?>, Function<JsonObject, ?>>> properties = this.parseProperties(context);

        if (properties.isError()) return Result.error(properties.getError());

        return this.parseElement(context, modifiers.get(), new Properties(inheritedProperties, properties.get()::get));
    }

    private Result<Map<LayerProperty<?>, Function<JsonObject, ?>>> parseProperties(Context context) {
        Map<LayerProperty<?>, List<Pair<CardPredicate, ?>>> properties = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();

        for (LayerProperty<?> property : this.properties) {
            XMLUtil.iterate(this.element, property.tagName, (element, i) -> {
                Result<List<CardPredicate>> predicates = XMLUtil.applyToFirstElement(element, "conditions", e ->
                        parseConditions(e, context), Result.of(Collections.emptyList())).unwrap();

                if (predicates.isError()) {
                    errors.add(predicates.getError());
                } else {
                    property.parse(element)
                            .ifError(errors::add)
                            .ifPresent(p -> properties.computeIfAbsent(property, k -> new ArrayList<>())
                                    .add(new Pair<>(new CardPredicate.And(predicates.get()), p)));
                }
            });
        }

        if (!errors.isEmpty()) {
            return Result.error("Error(s) parsing attribute modifiers:\n\t%s", String.join("\n\t%s", errors));
        } else {
            Map<LayerProperty<?>, Function<JsonObject, ?>> propertyFunctions = new LinkedHashMap<>();

            for (var entry : properties.entrySet()) {
                propertyFunctions.put(entry.getKey(), object -> {
                    for (var pair : entry.getValue()) {
                        Result<Boolean> result = pair.left().test(object);

                        if (result.isError()) {
                            errors.add(result.getError());
                        } else {
                            return pair.right();
                        }
                    }

                    return null;
                });
            }

            return Result.of(propertyFunctions);
        }
    }

    private Result<List<AttributeModifier>> parseAttributeModifiers(Context context) {
        List<AttributeModifier> modifiers = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        XMLUtil.iterate(this.element, "AttributeModifier", (element, i) ->
                parseConditions(element, context)
                        .ifError(errors::add)
                        .ifPresent(predicates -> modifiers.add(new AttributeModifier(
                                element.getAttribute("name"),
                                element.getAttribute("value"),
                                new CardPredicate.And(predicates)
                                ))
                        )
        );

        if (!errors.isEmpty()) {
            return Result.error("Error(s) parsing attribute modifiers:\n\t%s", String.join("\n\t%s", errors));
        } else {
            return Result.of(modifiers);
        }
    }

    protected final Result<List<CardPredicate>> parseConditions(Element element, Context context) {
        List<CardPredicate> predicates = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        XMLUtil.iterate(element, (predicate, j) ->
                XMLUtil.parsePredicate(predicate, context.definedPredicateGetter)
                        .ifError(errors::add)
                        .ifPresent(predicates::add));

        if (!errors.isEmpty()) {
            return Result.error("Error(s) parsing conditions:\n\t%s", String.join("\n\t%s", errors));
        } else {
            return Result.of(predicates);
        }
    }

    public record Context(Function<String, CardPredicate> definedPredicateGetter) {
    }
}
