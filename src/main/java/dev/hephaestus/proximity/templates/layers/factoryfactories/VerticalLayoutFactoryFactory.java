package dev.hephaestus.proximity.templates.layers.factoryfactories;

import dev.hephaestus.proximity.cards.predicates.CardPredicate;
import dev.hephaestus.proximity.templates.LayerFactory;
import dev.hephaestus.proximity.templates.LayerFactoryFactory;
import dev.hephaestus.proximity.templates.layers.factories.VerticalLayoutFactory;
import dev.hephaestus.proximity.util.Result;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Element;

import java.util.List;
import java.util.function.Function;

public class VerticalLayoutFactoryFactory extends ParentFactoryFactory<VerticalLayoutFactory> {
    private VerticalLayoutFactoryFactory(String id, int x, int y, List<CardPredicate> predicates, List<LayerFactoryFactory<?>> factories) {
        super(id, x, y, predicates, factories);
    }

    @Override
    protected VerticalLayoutFactory create(String id, int x, int y, List<LayerFactory<?>> factories) {
        return new VerticalLayoutFactory(id, x, y, this.predicates, factories);
    }

    public static Result<LayerFactoryFactory<?>> parse(Element element, String id, int x, int y, List<CardPredicate> predicates, Logger log, Function<String, CardPredicate> definedPredicateGetter) {
        return ParentFactoryFactory.parse(element, id, x, y, predicates, log, definedPredicateGetter, VerticalLayoutFactoryFactory::new);
    }
}
