package dev.hephaestus.proximity.templates.layers.factoryfactories;

import dev.hephaestus.proximity.cards.predicates.CardPredicate;
import dev.hephaestus.proximity.json.JsonObject;
import dev.hephaestus.proximity.templates.LayerFactory;
import dev.hephaestus.proximity.templates.LayerFactoryFactory;
import dev.hephaestus.proximity.templates.Template;
import dev.hephaestus.proximity.text.Style;
import dev.hephaestus.proximity.util.Result;
import dev.hephaestus.proximity.util.XMLUtil;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Element;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public abstract class ParentFactoryFactory<T extends LayerFactory<?>> extends LayerFactoryFactory<T> {
    private final List<LayerFactoryFactory<?>> factories;

    protected ParentFactoryFactory(String id, int x, int y, List<CardPredicate> predicates, List<LayerFactoryFactory<?>> factories) {
        super(id, x, y, predicates);
        this.factories = new ArrayList<>(factories);
    }

    protected abstract T create(String id, int x, int y, List<LayerFactory<?>> factories);

    @Override
    public final Result<T> createFactory(Template template) {
        List<LayerFactory<?>> factories = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (LayerFactoryFactory<?> factoryFactory : this.factories) {
            factoryFactory.createFactory(template)
                    .ifPresent(factories::add)
                    .ifError(errors::add);
        }

        if (!errors.isEmpty()) {
            return Result.error("Error creating child factories for layer %s:\n\t%s", id, String.join("\n\t"));
        }

        return Result.of(this.create(this.id, this.x, this.y, factories));
    }

    protected static Result<LayerFactoryFactory<?>> parse(Element element, String id, int x, int y, List<CardPredicate> predicates, Logger log, Function<String, CardPredicate> definedPredicateGetter, Function<JsonObject, Style> style, Function<JsonObject, Rectangle> wrap, Factory factory) {
        List<LayerFactoryFactory<?>> factories = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        XMLUtil.iterate(element, (e, i) -> {
            if (!e.getTagName().equals("conditions") && !e.getTagName().equals("Style") && !e.getTagName().equals("wrap")) {
                Result<LayerFactoryFactory<?>> r = parse(e, x, y, log, definedPredicateGetter, style, wrap);

                if (r.isError()) {
                    errors.add(r.getError());
                } else {
                    factories.add(r.get());
                }
            }
        });

        if (errors.isEmpty()) {
            return Result.of(factory.create(id, x, y, predicates, factories));
        } else {
            return Result.error("Error(s) while parsing layer %s: \n\t%s", id, String.join("\n\t%s", errors));
        }
    }

    protected interface Factory {
        LayerFactoryFactory<?> create(String id, int x, int y, List<CardPredicate> predicates, List<LayerFactoryFactory<?>> factories);
    }
}
