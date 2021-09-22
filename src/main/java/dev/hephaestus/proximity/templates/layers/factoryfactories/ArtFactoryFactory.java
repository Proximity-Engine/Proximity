package dev.hephaestus.proximity.templates.layers.factoryfactories;

import dev.hephaestus.proximity.cards.predicates.CardPredicate;
import dev.hephaestus.proximity.templates.LayerFactoryFactory;
import dev.hephaestus.proximity.templates.Template;
import dev.hephaestus.proximity.templates.layers.factories.ArtFactory;
import dev.hephaestus.proximity.util.Result;
import org.w3c.dom.Element;

import java.util.List;

public class ArtFactoryFactory extends LayerFactoryFactory<ArtFactory> {
    private final Integer width, height;

    protected ArtFactoryFactory(String id, int x, int y, List<CardPredicate> predicates, Integer width, Integer height) {
        super(id, x, y, predicates);
        this.width = width;
        this.height = height;
    }

    @Override
    public Result<ArtFactory> createFactory(Template template) {
        return Result.of(new ArtFactory(this.id, this.x, this.y, this.predicates, this.width, this.height));
    }

    public static Result<LayerFactoryFactory<?>> parse(Element element, String id, int x, int y, List<CardPredicate> predicates) {
        if (!element.hasAttribute("width") && !element.hasAttribute("height")) {
            return Result.error("Image layer must have either 'width' or 'height' attribute");
        }

        Integer width = element.hasAttribute("width") ? Integer.decode(element.getAttribute("width")) : null;
        Integer height = element.hasAttribute("height") ? Integer.decode(element.getAttribute("height")) : null;

        return Result.of(new ArtFactoryFactory(id, x, y, predicates, width, height));
    }
}
