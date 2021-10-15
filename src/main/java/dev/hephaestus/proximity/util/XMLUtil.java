package dev.hephaestus.proximity.util;

import dev.hephaestus.proximity.cards.predicates.CardPredicate;
import dev.hephaestus.proximity.cards.predicates.IsEquals;
import dev.hephaestus.proximity.cards.predicates.IsPresent;
import dev.hephaestus.proximity.cards.predicates.Range;
import dev.hephaestus.proximity.templates.TemplateSource;
import dev.hephaestus.proximity.xml.RenderableCard;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public final class XMLUtil {
    private XMLUtil() {}

    public static <T> void apply(RenderableCard.XMLElement element, String attribute, Function<String, T> func, Consumer<T> consumer) {
        if (element.hasAttribute(attribute)) {
            consumer.accept(func.apply(element.getAttribute(attribute)));
        }
    }

    public static void apply(RenderableCard.XMLElement element, String attribute, Consumer<String> consumer) {
        apply(element, attribute, s -> s, consumer);
    }

    public static int iterate(RenderableCard.XMLElement element, BiConsumer<RenderableCard.XMLElement, Integer> elementConsumer) {
        return element.iterate(elementConsumer);
    }

    public static int iterate(RenderableCard.XMLElement element, String tagName, BiConsumer<RenderableCard.XMLElement, Integer> elementConsumer) {
        return element.iterate(tagName, elementConsumer);
    }

    public static Result<CardPredicate> parsePredicate(RenderableCard.XMLElement element, Function<String, CardPredicate> definedPredicateGetter, Function<String, Boolean> filePresenceChecker) {
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
                        parsePredicate(child, definedPredicateGetter, filePresenceChecker)
                                .ifError(errors::add)
                                .ifPresent(predicates::add));

                if (!errors.isEmpty()) {
                    return Result.error("Error(s) parsing style:\n\t%s", String.join("\n\t", errors));
                }

                return Result.of(element.getTagName().equals("And")
                        ? new CardPredicate.And(predicates)
                        : new CardPredicate.Or(predicates));
            }
            case "Not" -> {
                Optional<Result<CardPredicate>> result = element.apply((RenderableCard.XMLElement p) ->
                        parsePredicate(p, definedPredicateGetter, filePresenceChecker)
                );

                return result.orElse(Result.error("Not cannot be empty")).then((CardPredicate e) -> Result.of(new CardPredicate.Not(e)));
            }
            case "FileExists" -> {
                return Result.of(card -> {
                    String src = element.getAttribute("src");

                    String file = ParsingUtil.getFileLocation(element.getParentId(), element.getId(), src);

                    boolean r = filePresenceChecker.apply(file);

                    return Result.of(r);
                });
            }
            default -> throw new IllegalStateException("Unexpected tag: " + element.getTagName());
        }
    }

    public static Result<Element> load(TemplateSource source, String fileName) {
        String name = source.getTemplateName();

        try {
            DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = documentBuilder.parse(source.getInputStream(fileName));

            return Result.of(document.getDocumentElement());
        } catch (IOException | ParserConfigurationException | SAXException e) {
            return Result.error("Exception loading template '%s': %s", name, e.getMessage());
        }
    }
}
