package dev.hephaestus.proximity.templates.layers.factoryfactories;

import dev.hephaestus.proximity.cards.predicates.CardPredicate;
import dev.hephaestus.proximity.templates.LayerFactoryFactory;
import dev.hephaestus.proximity.templates.Template;
import dev.hephaestus.proximity.templates.layers.factories.ImageFactory;
import dev.hephaestus.proximity.util.Result;
import org.w3c.dom.Element;

import java.util.List;

public class ImageFactoryFactory extends LayerFactoryFactory<ImageFactory> {
    private final String src;

    protected ImageFactoryFactory(String id, int x, int y, List<CardPredicate> predicates, String src) {
        super(id, x, y, predicates);

        this.src = src;
    }

    @Override
    public Result<ImageFactory> createFactory(Template template) {
        return Result.of(new ImageFactory(this.id, this.x, this.y, this.predicates, template.getSource(), this.src));
    }

    public static Result<LayerFactoryFactory<?>> parse(Element element, String id, int x, int y, List<CardPredicate> predicates) {
        if (!element.hasAttribute("src") && !element.hasAttribute("id")) {
            return Result.error("Image layer must have either 'src' or 'id' attribute");
        }

        String src = element.hasAttribute("src") ? element.getAttribute("src") : id;

        return Result.of(new ImageFactoryFactory(id, x, y, predicates, src));
    }
}
