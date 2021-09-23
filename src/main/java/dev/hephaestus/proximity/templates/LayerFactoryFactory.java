package dev.hephaestus.proximity.templates;

import dev.hephaestus.proximity.cards.predicates.CardPredicate;
import dev.hephaestus.proximity.json.JsonObject;
import dev.hephaestus.proximity.templates.layers.factoryfactories.*;
import dev.hephaestus.proximity.text.Style;
import dev.hephaestus.proximity.util.Pair;
import dev.hephaestus.proximity.util.Result;
import dev.hephaestus.proximity.util.XMLUtil;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Element;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public abstract class LayerFactoryFactory<Factory extends LayerFactory<?>> {
    protected final String id;
    protected final int x, y;
    protected final List<CardPredicate> predicates;

    protected LayerFactoryFactory(String id, int x, int y, List<CardPredicate> predicates) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.predicates = predicates;
    }

    public abstract Result<Factory> createFactory(Template template);

    protected static Result<LayerFactoryFactory<?>> parse(Element element, int x, int y, Logger log, Function<String, CardPredicate> definedPredicateGetter, Function<JsonObject, Style> styleFunction, Function<JsonObject, Rectangle> wrap) {
        String id = element.hasAttribute("id") ? element.getAttribute("id") : "";
        x += (element.hasAttribute("x") ? Integer.decode(element.getAttribute("x")) : 0);
        y += (element.hasAttribute("y") ? Integer.decode(element.getAttribute("y")) : 0);

        List<CardPredicate> predicates = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        XMLUtil.iterate(element, "conditions", (child, i) ->
                XMLUtil.iterate(child, (predicate, j) ->
                        XMLUtil.parsePredicate(predicate, definedPredicateGetter)
                                .ifError(errors::add)
                                .ifPresent(predicates::add)));

        List<Pair<CardPredicate, Style>> styles = new ArrayList<>();

        XMLUtil.iterate(element, "Style", (e, i) -> {
            Result<Style> style = Style.parse(e);

            style.ifPresent(s -> {
                List<CardPredicate> p = new ArrayList<>();

                XMLUtil.iterate(e, (child, j) -> {
                    if (!child.getTagName().equals("outline") && !child.getTagName().equals("shadow")) {
                        XMLUtil.parsePredicate(child, definedPredicateGetter)
                                .ifError(errors::add)
                                .ifPresent(p::add);
                    }
                });

                styles.add(new Pair<>(new CardPredicate.And(p), style.get()));
            });
        });

        Function<JsonObject, Style> f = styleFunction;
        styleFunction = object -> {
            for (var pair : styles) {
                Result<Boolean> r = pair.left().test(object);

                if (!r.isError() && r.get()) {
                    return f.apply(object).merge(pair.right());
                }
            }

            return f.apply(object);
        };

        List<Pair<CardPredicate, Rectangle>> wraps = new ArrayList<>();

        XMLUtil.iterate(element, "wrap", (e, i) -> {

            List<CardPredicate> p = new ArrayList<>();

            XMLUtil.iterate(e, (child, j) ->
                    XMLUtil.parsePredicate(child, definedPredicateGetter)
                            .ifError(errors::add)
                            .ifPresent(p::add));

            wraps.add(new Pair<>(new CardPredicate.And(p), new Rectangle(
                    Integer.parseInt(e.getAttribute("x")),
                    Integer.parseInt(e.getAttribute("y")),
                    Integer.parseInt(e.getAttribute("width")),
                    Integer.parseInt(e.getAttribute("height"))
            )));
        });

        Function<JsonObject, Rectangle> wf = object -> {
            for (var pair : wraps) {
                Result<Boolean> r = pair.left().test(object);

                if (!r.isError() && r.get()) {
                    return pair.right();
                }
            }

            return wrap == null ? null : wrap.apply(object);
        };

        if (!errors.isEmpty()) {
            return Result.error("Error(s) parsing conditions for layer '%s':\n\t%s", id, String.join("\n\t%s", errors));
        }

        return switch (element.getTagName()) {
            case "ImageLayer" -> ImageFactoryFactory.parse(element, id, x, y, predicates);
            case "ArtLayer" -> ArtFactoryFactory.parse(element, id, x, y, predicates);
            case "TextLayer" -> TextFactoryFactory.parse(element, id, x, y, predicates, styleFunction, wf);
            case "Group", "main", "flex" -> GroupFactoryFactory.parse(element, id, x, y, predicates, log, definedPredicateGetter, styleFunction, wf);
            case "SquishBox" -> SquishBoxFactoryFactory.parse(element, id, x, y, predicates, log, definedPredicateGetter, styleFunction, wf);
            case "Selector" -> SelectorFactoryFactory.parse(element, id, x, y, predicates, log, definedPredicateGetter, styleFunction, wf);
            case "SpacingLayer" -> SpacerFactoryFactory.parse(element, id, x, y, predicates);
            case "Fork" -> ForkFactoryFactory.parse(element, id, x, y, predicates, log, definedPredicateGetter, styleFunction, wf);
            case "HorizontalLayout", "VerticalLayout" -> LayoutFactoryFactory.parse(element, id, x, y, predicates, log, definedPredicateGetter, styleFunction, wf);
            default -> Result.error("Unexpected layer type: %s", element.getTagName());
        };
    }
}
