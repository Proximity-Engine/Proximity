package dev.hephaestus.proximity.templates.layers.factoryfactories;

import dev.hephaestus.proximity.cards.predicates.CardPredicate;
import dev.hephaestus.proximity.json.JsonObject;
import dev.hephaestus.proximity.templates.LayerFactory;
import dev.hephaestus.proximity.templates.LayerFactoryFactory;
import dev.hephaestus.proximity.templates.Template;
import dev.hephaestus.proximity.templates.layers.factories.ForkFactory;
import dev.hephaestus.proximity.text.Style;
import dev.hephaestus.proximity.util.Result;
import dev.hephaestus.proximity.util.XMLUtil;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Element;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Function;

public class ForkFactoryFactory extends LayerFactoryFactory<ForkFactory> {
    private final List<LayerFactoryFactory<?>> factories;
    private final Map<String, List<CardPredicate>> branches;

    protected ForkFactoryFactory(String id, int x, int y, List<CardPredicate> predicates, List<LayerFactoryFactory<?>> factories, Map<String, List<CardPredicate>> branches) {
        super(id, x, y, predicates);
        this.factories = new ArrayList<>(factories);
        this.branches = new LinkedHashMap<>(branches);
    }

    @Override
    public Result<ForkFactory> createFactory(Template template) {
        List<LayerFactory<?>> factories = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (LayerFactoryFactory<?> factoryFactory : this.factories) {
            factoryFactory.createFactory(template)
                    .ifPresent(factories::add)
                    .ifError(errors::add);
        }

        if (!errors.isEmpty()) {
            return Result.error("Error creating child factories for layer %s:\n\t%s", this.id, String.join("\n\t"));
        }

        return Result.of(new ForkFactory(this.id, this.x, this.y, this.predicates, factories, this.branches));
    }

    public static Result<LayerFactoryFactory<?>> parse(Element element, String id, int x, int y, List<CardPredicate> predicates, Logger log, Function<String, CardPredicate> definedPredicateGetter, Function<JsonObject, Style> style, Function<JsonObject, Rectangle> wrap) {
        List<LayerFactoryFactory<?>> factories = new ArrayList<>();
        Map<String, List<CardPredicate>> branches = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();

        XMLUtil.iterate(element, (child, i) -> {
            if (child.getTagName().equals("branches")) {
                XMLUtil.iterate(child, (branch, j) -> {
                    int conditionsCount = XMLUtil.iterate(branch, (predicate, k) -> {
                        Result<CardPredicate> r = XMLUtil.parsePredicate(predicate, definedPredicateGetter);

                        if (r.isError()) {
                            errors.add(r.getError());
                        } else {
                            branches.computeIfAbsent(branch.getAttribute("id"), key -> new ArrayList<>())
                                    .add(r.get());
                        }
                    });

                    if (conditionsCount == 0) {
                        branches.put(branch.getAttribute("id"), Collections.singletonList(c -> Result.of(true)));
                    }
                });
            } else if (!child.getTagName().equals("conditions")) {
                parse(child, x, y, log, definedPredicateGetter, style, wrap)
                        .ifError(errors::add)
                        .ifPresent(factories::add);
            }
        });

        List<CardPredicate> branchPredicates = new ArrayList<>();

        for (var branch : branches.values()) {
            branchPredicates.add(new CardPredicate.And(branch));
        }

        predicates.add(new CardPredicate.Or(branchPredicates));

        if (errors.isEmpty()) {
            return Result.of(new ForkFactoryFactory(id, x, y, predicates, factories, branches));
        } else {
            return Result.error("Error(s) while parsing Fork: \n\t%s", String.join("\n\t%s", errors));
        }
    }
}
