package dev.hephaestus.proximity.templates;

import dev.hephaestus.proximity.cards.predicates.CardPredicate;
import dev.hephaestus.proximity.templates.layers.factoryfactories.*;
import dev.hephaestus.proximity.util.Result;
import dev.hephaestus.proximity.util.XMLUtil;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Element;

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

    protected static Result<LayerFactoryFactory<?>> parse(Element element, int x, int y, Logger log, Function<String, CardPredicate> definedPredicateGetter) {
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

        if (!errors.isEmpty()) {
            return Result.error("Error(s) parsing conditions for layer '%s':\n\t%s", id, String.join("\n\t%s", errors));
        }

        return switch (element.getTagName()) {
            case "ImageLayer" -> ImageFactoryFactory.parse(element, id, x, y, predicates);
            case "ArtLayer" -> ArtFactoryFactory.parse(element, id, x, y, predicates);
            case "TextLayer" -> TextFactoryFactory.parse(element, id, x, y, predicates);
            case "Group", "main", "flex" -> GroupFactoryFactory.parse(element, id, x, y, predicates, log, definedPredicateGetter);
            case "SquishBox" -> SquishBoxFactoryFactory.parse(element, id, x, y, predicates, log, definedPredicateGetter);
            case "Selector" -> SelectorFactoryFactory.parse(element, id, x, y, predicates, log, definedPredicateGetter);
            case "SpacingLayer" -> SpacerFactoryFactory.parse(element, id, x, y, predicates);
            case "Fork" -> ForkFactoryFactory.parse(element, id, x, y, predicates, log, definedPredicateGetter);
            case "HorizontalLayout" -> HorizontalLayoutFactoryFactory.parse(element, id, x, y, predicates, log, definedPredicateGetter);
            case "VerticalLayout" -> VerticalLayoutFactoryFactory.parse(element, id, x, y, predicates, log, definedPredicateGetter);
            default -> Result.error("Unexpected layer type: %s", element.getTagName());
        };
    }
}
