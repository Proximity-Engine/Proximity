package dev.hephaestus.proximity.templates.layers.factoryfactories;

import dev.hephaestus.proximity.cards.predicates.CardPredicate;
import dev.hephaestus.proximity.json.JsonObject;
import dev.hephaestus.proximity.templates.LayerFactory;
import dev.hephaestus.proximity.templates.LayerFactoryFactory;
import dev.hephaestus.proximity.templates.layers.Layer;
import dev.hephaestus.proximity.templates.layers.factories.LayoutFactory;
import dev.hephaestus.proximity.text.Style;
import dev.hephaestus.proximity.util.ContentAlignment;
import dev.hephaestus.proximity.util.Result;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Element;

import java.awt.*;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class LayoutFactoryFactory extends ParentFactoryFactory<LayoutFactory> {
    private final Integer width, height;
    private final ContentAlignment alignment;
    private final BiConsumer<Layer, Integer> inLineSetter;
    private final BiConsumer<Layer, Integer> offLineSetter;
    private final Function<Layer, Integer> inLineGetter;
    private final Function<Layer, Integer> offLineGetter;
    private final Function<Rectangle, Double> inLineSizeGetter;
    private final Function<Rectangle, Double> offLineSizeGetter;

    private LayoutFactoryFactory(String id, int x, int y, List<CardPredicate> predicates, Integer width, Integer height, List<LayerFactoryFactory<?>> factories, ContentAlignment alignment, BiConsumer<Layer, Integer> inLineSetter, BiConsumer<Layer, Integer> offLineSetter, Function<Layer, Integer> inLineGetter, Function<Layer, Integer> offLineGetter, Function<Rectangle, Double> inLineSizeGetter, Function<Rectangle, Double> offLineSizeGetter) {
        super(id, x, y, predicates, factories);
        this.width = width;
        this.height = height;
        this.alignment = alignment;
        this.inLineSetter = inLineSetter;
        this.offLineSetter = offLineSetter;
        this.inLineGetter = inLineGetter;
        this.offLineGetter = offLineGetter;
        this.inLineSizeGetter = inLineSizeGetter;
        this.offLineSizeGetter = offLineSizeGetter;
    }

    @Override
    protected LayoutFactory create(String id, int x, int y, List<LayerFactory<?>> factories) {
        return new LayoutFactory(id, x, y, this.predicates, this.width, this.height, factories, this.alignment, this.inLineSetter, this.offLineSetter, inLineGetter, offLineGetter, inLineSizeGetter, offLineSizeGetter);
    }

    public static Result<LayerFactoryFactory<?>> parse(Element element, String id, int x, int y, List<CardPredicate> predicates, Logger log, Function<String, CardPredicate> definedPredicateGetter, Function<JsonObject, Style> style, Function<JsonObject, Rectangle> wrap) {
        Integer width = element.hasAttribute("width") ? Integer.decode(element.getAttribute("width")) : null;
        Integer height = element.hasAttribute("height") ? Integer.decode(element.getAttribute("height")) : null;
        ContentAlignment alignment = element.hasAttribute("alignment") ? ContentAlignment.valueOf(element.getAttribute("alignment").toUpperCase(Locale.ROOT)) : ContentAlignment.START;

        return ParentFactoryFactory.parse(element, id, x, y, predicates, log, definedPredicateGetter, style, wrap, ((id1, x1, y1, predicates1, factories) ->
                element.getTagName().equals("VerticalLayout")
                        ? new LayoutFactoryFactory(id1, x1, y1, predicates1, width, height, factories, alignment, Layer::setY, Layer::setX, Layer::getY, Layer::getX, Rectangle::getHeight, Rectangle::getWidth)
                        : new LayoutFactoryFactory(id1, x1, y1, predicates1, width, height, factories, alignment, Layer::setX, Layer::setY, Layer::getX, Layer::getY, Rectangle::getWidth, Rectangle::getHeight)
        ));
    }
}
