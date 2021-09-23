package dev.hephaestus.proximity.templates.layers.factoryfactories;

import dev.hephaestus.proximity.cards.predicates.CardPredicate;
import dev.hephaestus.proximity.json.JsonObject;
import dev.hephaestus.proximity.templates.LayerFactoryFactory;
import dev.hephaestus.proximity.templates.Template;
import dev.hephaestus.proximity.templates.layers.factories.TextFactory;
import dev.hephaestus.proximity.text.Style;
import dev.hephaestus.proximity.text.TextAlignment;
import dev.hephaestus.proximity.util.Result;
import org.w3c.dom.Element;

import java.awt.*;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

public class TextFactoryFactory extends LayerFactoryFactory<TextFactory> {
    private final TextAlignment alignment;
    private final Integer width, height;
    private final Function<JsonObject, Style> style;
    private final String styleName;
    private final Function<JsonObject, Rectangle> wrap;
    private final String value;

    protected TextFactoryFactory(String id, int x, int y, List<CardPredicate> predicates, TextAlignment alignment, Integer width, Integer height, Function<JsonObject, Style> style, String styleName, Function<JsonObject, Rectangle> wrap, String value) {
        super(id, x, y, predicates);
        this.alignment = alignment;
        this.width = width;
        this.height = height;
        this.style = style;
        this.styleName = styleName;
        this.wrap = wrap;
        this.value = value;
    }

    @Override
    public Result<TextFactory> createFactory(Template template) {
        Function<JsonObject, Style> style;

        if (this.styleName == null) {
            style = this.style;
        } else if (template.getStyle(this.styleName) != null) {
            Style s = template.getStyle(this.styleName);
            style = object -> s.merge(this.style.apply(object));
        } else {
            style = this.style;
        }

        return Result.of(new TextFactory(
                this.id,
                this.x,
                this.y,
                this.predicates,
                this.alignment,
                this.width,
                this.height,
                style,
                this.wrap,
                this.value,
                template
        ));
    }

    public static Result<LayerFactoryFactory<?>> parse(Element element, String id, int x, int y, List<CardPredicate> predicates, Function<JsonObject, Style> style, Function<JsonObject, Rectangle> wrap) {
        if (element.hasAttribute("width") ^ element.hasAttribute("height")) {
            return Result.error("Text layer must have both 'width' and 'height' attributes or neither");
        }

        TextAlignment alignment = element.hasAttribute("alignment") ? TextAlignment.valueOf(element.getAttribute("alignment").toUpperCase(Locale.ROOT)) : TextAlignment.LEFT;
        Integer width = element.hasAttribute("width") ? Integer.decode(element.getAttribute("width")) : null;
        Integer height = element.hasAttribute("height") ? Integer.decode(element.getAttribute("height")) : null;
        String string = element.getAttribute("value");

        return Result.of(new TextFactoryFactory(
                id,
                x,
                y,
                predicates,
                alignment,
                width,
                height,
                style,
                element.hasAttribute("style") ? element.getAttribute("style") : null,
                wrap,
                string));
    }
}
