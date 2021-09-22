package dev.hephaestus.proximity.templates;

import dev.hephaestus.proximity.cards.predicates.CardPredicate;
import dev.hephaestus.proximity.json.JsonObject;
import dev.hephaestus.proximity.text.Style;
import dev.hephaestus.proximity.util.Box;
import dev.hephaestus.proximity.util.Result;
import dev.hephaestus.proximity.util.XMLUtil;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record TemplateParser(Logger log) {
    public Result<Template> parse(Document document, TemplateSource source, JsonObject options) {
        return this.init(document, source, options)
                .then(this::parseOptions)
                .then(this::parseStyles)
                .then(this::parsePredicates)
                .then(this::parseLayers);
    }

    private Result<Info1> init(Document document, TemplateSource source, JsonObject options) {
        Element root = document.getDocumentElement();

        return Result.of(new Info1(
                document,
                root,
                root.getAttribute("name"),
                Integer.decode(root.getAttribute("width")),
                Integer.decode(root.getAttribute("height")),
                root.getAttribute("author"),
                source,
                options.deepCopy()
        ));
    }

    private Result<Info1> parseOptions(Info1 info) {
        List<String> errors = new ArrayList<>();

        XMLUtil.iterate(info.root, "options", (options, i) ->
                XMLUtil.iterate(options, (option, j) -> {
                        String id = option.getAttribute("id");

                        switch (option.getTagName()) {
                            case "Enumeration" -> {
                                String defaultValue = option.getAttribute("default");
                                Box<Boolean> defaultValuePresent = new Box<>(false);

                                XMLUtil.iterate(option, (element, k) ->
                                        defaultValuePresent.set(defaultValuePresent.get()
                                                || element.getAttribute("value").equals(defaultValue)));

                                if (defaultValuePresent.get()) {
                                    info.options.addProperty(id, defaultValue);
                                } else {
                                    errors.add(String.format("Default value '%s' not present in Enumeration '%s'", defaultValue, id));
                                }
                            }
                            case "ToggleOption" -> {
                                if (!info.options.has(id)) {
                                    info.options.addProperty(id, Boolean.parseBoolean(option.getAttribute("default")));
                                }
                            }
                        }
                })
        );

        if (errors.isEmpty()) {
            return Result.of(info);
        } else {
            return Result.error("Error(s) parsing options:\n\t%s", String.join("\n\t%s", errors));
        }
    }

    private Result<Info2> parseStyles(Info1 info) {
        Map<String, Style> stylesMap = new HashMap<>();
        List<String> errors = new ArrayList<>();

        XMLUtil.iterate(info.root, "styles", (styles, i) ->
                XMLUtil.iterate(styles, "Style", (style, j) -> {
                        if (!style.hasAttribute("name")) {
                            errors.add(String.format("Style #%d missing name attribute", j));
                        }

                        String name = style.getAttribute("name");

                        Result<Style> r = Style.parse(style);

                        if (r.isError()) {
                            errors.add(r.getError());
                        } else {
                            stylesMap.put(name, r.get());
                        }
                })
        );

        if (errors.isEmpty()) {
            return Result.of(new Info2(
                    info.document,
                    info.root,
                    info.name,
                    info.width,
                    info.height,
                    info.author,
                    info.source,
                    info.options,
                    stylesMap
            ));
        } else {
            return Result.error("Error(s) parsing styles:\n\t%s", String.join("\n\t%s", errors));
        }
    }

    private Result<Info3> parsePredicates(Info2 info) {
        Map<String, CardPredicate> predicateMap = new HashMap<>();
        List<String> errors = new ArrayList<>();

        XMLUtil.iterate(info.root, "condition_definitions", (element, i) ->
                XMLUtil.iterate(element, "ConditionDefinition", (definition, j) -> {
                        String name = definition.getAttribute("name");
                        List<CardPredicate> definedPredicates = new ArrayList<>();

                        XMLUtil.iterate(definition, (predicate, k) ->
                                XMLUtil.parsePredicate(predicate, e -> null)
                                        .ifError(errors::add)
                                        .ifPresent(definedPredicates::add));

                        predicateMap.put(name, definedPredicates.size() == 1 ? definedPredicates.get(0) : new CardPredicate.And(definedPredicates));
                })
        );

        if (errors.isEmpty()) {
            return Result.of(new Info3(
                    info.document,
                    info.root,
                    info.name,
                    info.width,
                    info.height,
                    info.author,
                    info.source,
                    info.options,
                    info.styles,
                    predicateMap
            ));
        } else {
            return Result.error("Error(s) parsing predicates:\n\t%s", String.join("\n\t%s", errors));
        }
    }

    private Result<Template> parseLayers(Info3 info) {
        List<LayerFactoryFactory<?>> layerList = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        XMLUtil.iterate(info.root, "layers", (layers, i) ->
                XMLUtil.iterate(layers, (layer, j) ->
                        LayerFactoryFactory.parse(layer, 0, 0, this.log, info.predicates::get)
                                .ifError(errors::add)
                                .ifPresent(layerList::add)));

        if (errors.isEmpty()) {
            return Result.of(new Template(info.source, info.width, info.height, layerList, info.options, info.styles, this.log));
        }
        {
            return Result.error("Error(s) parsing template:\n\t %s", String.join("\n\t%s", errors));
        }
    }

    private static record Info1(Document document, Element root, String name, int width, int height, String author,
                                TemplateSource source, JsonObject options) {
    }

    private static record Info2(Document document, Element root, String name, int width, int height, String author,
                                TemplateSource source, JsonObject options, Map<String, Style> styles) {
    }

    private static record Info3(Document document, Element root, String name, int width, int height, String author,
                                TemplateSource source, JsonObject options, Map<String, Style> styles,
                                Map<String, CardPredicate> predicates) {
    }
}
