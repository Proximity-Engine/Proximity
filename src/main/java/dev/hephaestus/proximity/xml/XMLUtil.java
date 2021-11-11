package dev.hephaestus.proximity.xml;

import dev.hephaestus.proximity.cards.predicates.CardPredicate;
import dev.hephaestus.proximity.cards.predicates.IsEquals;
import dev.hephaestus.proximity.cards.predicates.IsPresent;
import dev.hephaestus.proximity.cards.predicates.Range;
import dev.hephaestus.proximity.templates.TemplateSource;
import dev.hephaestus.proximity.util.ParsingUtil;
import dev.hephaestus.proximity.util.Result;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.parsers.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public final class XMLUtil {
    private XMLUtil() {}

    public static <T> void apply(RenderableData.XMLElement element, String attribute, Function<String, T> func, Consumer<T> consumer) {
        if (element.hasAttribute(attribute)) {
            consumer.accept(func.apply(element.getAttribute(attribute)));
        }
    }

    public static void apply(RenderableData.XMLElement element, String attribute, Consumer<String> consumer) {
        apply(element, attribute, s -> s, consumer);
    }

    public static void iterate(RenderableData.XMLElement element, BiConsumer<RenderableData.XMLElement, Integer> elementConsumer) {
        element.iterate(elementConsumer);
    }

    public static void iterate(RenderableData.XMLElement element, String tagName, BiConsumer<RenderableData.XMLElement, Integer> elementConsumer) {
        element.iterate(tagName, elementConsumer);
    }

    public static Result<CardPredicate> parsePredicate(RenderableData.XMLElement element, Function<String, CardPredicate> definedPredicateGetter, Function<String, Boolean> filePresenceChecker) {
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
                Optional<Result<CardPredicate>> result = element.apply((RenderableData.XMLElement p) ->
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
            Document document = read(source.getInputStream(fileName));

            return Result.of(document.getDocumentElement());
        } catch (IOException | SAXException e) {
            return Result.error("Exception loading template '%s': %s", name, e.getMessage());
        }
    }

    public static Document read(final InputStream is) throws IOException, SAXException {
        final Document doc;
        SAXParser parser;

        try {
            final SAXParserFactory factory = SAXParserFactory.newInstance();
            parser = factory.newSAXParser();
            final DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            doc = docBuilder.newDocument();
        } catch (final ParserConfigurationException e) {
            throw new RuntimeException("Can't create SAX parser / DOM builder.", e);
        }

        XMLReader reader = parser.getXMLReader();
        XMLHandler handler = new XMLHandler(doc);

        reader.setProperty("http://xml.org/sax/properties/lexical-handler", handler);

        parser.parse(is, handler);

        return doc;
    }
}
