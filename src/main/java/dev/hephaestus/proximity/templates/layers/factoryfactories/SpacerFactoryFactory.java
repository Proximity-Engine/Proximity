package dev.hephaestus.proximity.templates.layers.factoryfactories;

import dev.hephaestus.proximity.cards.predicates.CardPredicate;
import dev.hephaestus.proximity.templates.LayerFactoryFactory;
import dev.hephaestus.proximity.templates.Template;
import dev.hephaestus.proximity.templates.layers.factories.SpacerFactory;
import dev.hephaestus.proximity.util.Result;
import org.w3c.dom.Element;

import java.util.List;

public class SpacerFactoryFactory extends LayerFactoryFactory<SpacerFactory> {
    private final int width, height;

    protected SpacerFactoryFactory(String id, int x, int y, List<CardPredicate> predicates, int width, int height) {
        super(id, x, y, predicates);
        this.width = width;
        this.height = height;
    }

    @Override
    public Result<SpacerFactory> createFactory(Template template) {
        return Result.of(new SpacerFactory(this.id, this.x, this.y, this.predicates, this.width, this.height));
    }

    public static Result<LayerFactoryFactory<?>> parse(Element element, String id, int x, int y, List<CardPredicate> predicates) {
        return Result.of(new SpacerFactoryFactory(id,
                x,
                y,
                predicates,
                Integer.decode(element.getAttribute("width")),
                Integer.decode(element.getAttribute("height"))
        ));
    }
}
