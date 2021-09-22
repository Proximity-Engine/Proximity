package dev.hephaestus.proximity.templates.layers.factoryfactories;

import dev.hephaestus.proximity.cards.predicates.CardPredicate;
import dev.hephaestus.proximity.templates.LayerFactoryFactory;
import dev.hephaestus.proximity.templates.Template;
import dev.hephaestus.proximity.templates.layers.factories.TextFactory;
import dev.hephaestus.proximity.text.Alignment;
import dev.hephaestus.proximity.text.Style;
import dev.hephaestus.proximity.util.Box;
import dev.hephaestus.proximity.util.Result;
import dev.hephaestus.proximity.util.XMLUtil;
import org.w3c.dom.Element;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TextFactoryFactory extends LayerFactoryFactory<TextFactory> {
    private final Alignment alignment;
    private final Integer width, height;
    private final Style style;
    private final String styleName;
    private final Rectangle wrap;
    private final String value;

    protected TextFactoryFactory(String id, int x, int y, List<CardPredicate> predicates, Alignment alignment, Integer width, Integer height, Style style, String styleName, Rectangle wrap, String value) {
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
        return Result.of(new TextFactory(
                this.id,
                this.x,
                this.y,
                this.predicates,
                this.alignment,
                this.width,
                this.height,
                this.style == null
                        ? this.styleName == null
                            ? Style.EMPTY
                            : template.getStyle(this.styleName)
                        : this.style,
                this.wrap,
                this.value,
                template
        ));
    }

    public static Result<LayerFactoryFactory<?>> parse(Element element, String id, int x, int y, List<CardPredicate> predicates) {
        if (element.hasAttribute("width") ^ element.hasAttribute("height")) {
            return Result.error("Text layer must have both 'width' and 'height' attributes or neither");
        }

        List<String> errors = new ArrayList<>();

        Alignment alignment = element.hasAttribute("alignment") ? Alignment.valueOf(element.getAttribute("alignment").toUpperCase(Locale.ROOT)) : Alignment.LEFT;
        Integer width = element.hasAttribute("width") ? Integer.decode(element.getAttribute("width")) : null;
        Integer height = element.hasAttribute("height") ? Integer.decode(element.getAttribute("height")) : null;
        Box<Style> style = new Box<>();
        Box<Rectangle> wrap = new Box<>();
        String string = element.getAttribute("value");

        XMLUtil.iterate(element, (child, i) -> {
            switch (child.getTagName()) {
                case "style" -> Style.parse(child).ifError(errors::add).ifPresent(style::set);
                case "wrap" -> {
                    if (!(child.hasAttribute("x") && child.hasAttribute("y") && child.hasAttribute("width") && child.hasAttribute("height"))) {
                        errors.add("Wrap must have x, y, width, and height attributes");
                        return;
                    }

                    wrap.set(new Rectangle(
                            Integer.parseInt(child.getAttribute("x")),
                            Integer.parseInt(child.getAttribute("y")),
                            Integer.parseInt(child.getAttribute("width")),
                            Integer.parseInt(child.getAttribute("height"))
                    ));
                }
                case "conditions" -> {}
                default -> errors.add("Unexpected value: " + child.getTagName());
            }
        });

        if (!errors.isEmpty()) {
            return Result.error("Error(s) creating layer %s:\n\t%s", id, String.join("\n\t%s", errors));
        }


        return Result.of(new TextFactoryFactory(
                id,
                x,
                y,
                predicates,
                alignment,
                width,
                height,
                style.get(),
                element.hasAttribute("style") ? element.getAttribute("style") : null,
                wrap.get(),
                string));
    }
}
