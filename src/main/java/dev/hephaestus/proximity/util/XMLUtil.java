package dev.hephaestus.proximity.util;

import dev.hephaestus.proximity.cards.predicates.CardPredicate;
import dev.hephaestus.proximity.cards.predicates.IsEquals;
import dev.hephaestus.proximity.cards.predicates.IsPresent;
import dev.hephaestus.proximity.cards.predicates.Range;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public final class XMLUtil {
    private XMLUtil() {}

    public static <T> void apply(Element element, String attribute, Function<String, T> func, Consumer<T> consumer) {
        if (element.hasAttribute(attribute)) {
            consumer.accept(func.apply(element.getAttribute(attribute)));
        }
    }

    public static void apply(Element element, String attribute, Consumer<String> consumer) {
        apply(element, attribute, s -> s, consumer);
    }

    public static int iterate(Element element, BiConsumer<Element, Integer> elementConsumer) {
        return iterate(element, element.getChildNodes(), elementConsumer);
    }

    public static int iterate(Element element, String tagName, BiConsumer<Element, Integer> elementConsumer) {
        return iterate(element, element.getElementsByTagName(tagName), elementConsumer);
    }

    public static int iterate(Element element, NodeList nodes, BiConsumer<Element, Integer> elementConsumer) {
        int index = 0;

        for (int i = 0; i < nodes.getLength(); ++i) {
            Node node = nodes.item(i);

            if (node instanceof Element && node.getParentNode() == element) {
                elementConsumer.accept((Element) node, index++);
            }
        }

        return index;
    }

    public static <T> Result<T> applyToFirstElement(Element element, String tagName, Function<Element, T> function) {
        return applyToFirstElement(element, element.getElementsByTagName(tagName), function);
    }

    public static <T> Result<T> applyToFirstElement(Element element, Function<Element, T> function) {
        return applyToFirstElement(element, element.getChildNodes(), function);
    }

    public static <T> Result<T> applyToFirstElement(Element element, NodeList nodes, Function<Element, T> function) {
        for (int i = 0; i < nodes.getLength(); ++i) {
            Node node = nodes.item(i);

            if (node instanceof Element e && node.getParentNode() == element) {
                return Result.of(function.apply(e));
            }
        }

        return Result.error("Element '%s' does not contain any elements", element);
    }

    public static Result<CardPredicate> parsePredicate(Element element, Function<String, CardPredicate> definedPredicateGetter) {
        if (element.hasAttribute("key")) {
            CardPredicate predicate = definedPredicateGetter.apply(element.getAttribute("key"));

            if (predicate != null) {
                return Result.of(predicate);
            }
        }

        switch (element.getTagName()) {
            case "IsPresent" -> {
                String[] key = element.getAttribute("key").split("\\.");

                boolean value = !element.hasAttribute("value") || Boolean.parseBoolean(element.getAttribute("value"));

                return Result.of(new IsPresent(key, value));
            }
            case "IsEquals" -> {
                String[] key = element.getAttribute("key").split("\\.");

                String value = element.hasAttribute("value") ? element.getAttribute("value") : "true";

                return Result.of(new IsEquals(key, value));
            }
            case "Range" -> {
                String[] key = element.getAttribute("key").split("\\.");

                int min, max;

                if (element.hasAttribute("value")) {
                    min = max = Integer.decode(element.getAttribute("value"));
                } else if (element.hasAttribute("min") || element.hasAttribute("max")) {
                    min = element.hasAttribute("min") ? Integer.decode(element.getAttribute("min")) : Integer.MIN_VALUE;
                    max = element.hasAttribute("max") ? Integer.decode(element.getAttribute("max")) : Integer.MAX_VALUE;
                } else {
                    return Result.error("IntegerCondition requires one of min, max, or value");
                }

                return Range.of(key, min, max);
            }
            case "And", "Or" -> {
                List<CardPredicate> predicates = new ArrayList<>();
                List<String> errors = new ArrayList<>();

                XMLUtil.iterate(element, (child, i) ->
                        parsePredicate(child, definedPredicateGetter)
                                .ifError(errors::add)
                                .ifPresent(predicates::add));

                if (!errors.isEmpty()) {
                    return Result.error("Error(s) parsing style:\n\t%s", String.join("\n\t%s", errors));
                }

                return Result.of(element.getTagName().equals("And")
                        ? new CardPredicate.And(predicates)
                        : new CardPredicate.Or(predicates));
            }
            case "Not" -> {
                Result<CardPredicate> result = applyToFirstElement(element, p ->
                        parsePredicate(p, definedPredicateGetter)
                ).unwrap();

                return result.then((CardPredicate e) -> Result.of(new CardPredicate.Not(e)));
            }
            default -> throw new IllegalStateException("Unexpected tag: " + element.getTagName());
        }
    }
}
