package dev.hephaestus.proximity.templates.layers.factoryfactories;

import dev.hephaestus.proximity.cards.predicates.CardPredicate;
import dev.hephaestus.proximity.json.JsonObject;
import dev.hephaestus.proximity.templates.LayerFactory;
import dev.hephaestus.proximity.templates.LayerFactoryFactory;
import dev.hephaestus.proximity.templates.Template;
import dev.hephaestus.proximity.templates.layers.factories.SquishBoxFactory;
import dev.hephaestus.proximity.text.Style;
import dev.hephaestus.proximity.util.Result;
import dev.hephaestus.proximity.util.XMLUtil;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Element;

import java.awt.*;
import java.util.List;
import java.util.function.Function;

public class SquishBoxFactoryFactory extends LayerFactoryFactory<SquishBoxFactory> {
    private final LayerFactoryFactory<?> main, flex;

    private SquishBoxFactoryFactory(String id, int x, int y, List<CardPredicate> predicates, LayerFactoryFactory<?> main, LayerFactoryFactory<?> flex) {
        super(id, x, y, predicates);
        this.main = main;
        this.flex = flex;
    }

    public static Result<LayerFactoryFactory<?>> parse(Element element, String id, int x, int y, List<CardPredicate> predicates, Logger log, Function<String, CardPredicate> definedPredicateGetter, Function<JsonObject, Style> style, Function<JsonObject, Rectangle> wrap) {
        Result<LayerFactoryFactory<?>> main = XMLUtil.applyToFirstElement(element, "main", e -> parse(e, x, y, log, definedPredicateGetter, style, wrap)).unwrap();
        Result<LayerFactoryFactory<?>> flex = XMLUtil.applyToFirstElement(element, "flex", e -> parse(e, x, y, log, definedPredicateGetter, style, wrap)).unwrap();

        if (main.isError() ^ flex.isError()) {
            return Result.error((main.isError() ? main : flex).getError());
        } else if (main.isError()) {
            return Result.error("Error creating factories for layer %s:\n\tmain: %s\n\tflex: %s",
                    id, main.getError(), flex.getError()
            );
        }

        return Result.of(new SquishBoxFactoryFactory(id, x, y, predicates, main.get(), flex.get()));
    }

    @Override
    public Result<SquishBoxFactory> createFactory(Template template) {
        Result<? extends LayerFactory<?>> mainFactory = this.main.createFactory(template);
        Result<? extends LayerFactory<?>> flexFactory = this.flex.createFactory(template);

        if (mainFactory.isError() ^ flexFactory.isError()) {
            return Result.error((mainFactory.isError() ? mainFactory : flexFactory).getError());
        } else if (mainFactory.isError()) {
            return Result.error("Error creating factories for layer %s:\n\tmain: %s\n\tflex: %s",
                    id, mainFactory.getError(), flexFactory.getError()
            );
        }

        return Result.of(new SquishBoxFactory(this.id, this.x, this.y, this.predicates, mainFactory.get(), flexFactory.get()));
    }
}
