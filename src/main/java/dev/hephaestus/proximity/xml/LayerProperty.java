package dev.hephaestus.proximity.xml;

import dev.hephaestus.proximity.text.Style;
import dev.hephaestus.proximity.util.*;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;

import static dev.hephaestus.proximity.util.XMLUtil.apply;

public abstract class LayerProperty<T> {
    private static final Map<String, LayerProperty<?>> PROPERTIES = new HashMap<>();

    public static final LayerProperty<Style> STYLE = register(new LayerProperty<>() {
        @Override
        public Result<Style> parse(RenderableCard.XMLElement element) {
            Style.Builder builder = new Style.Builder();

            builder.font(element.getAttribute("font"));

            apply(element, "italicFont", builder::italics);
            apply(element, "capitalization", Style.Capitalization::parse, builder::capitalization);
            apply(element, "color", Integer::decode, builder::color);
            apply(element, "size", Integer::parseInt, builder::size);
            apply(element, "kerning", Float::parseFloat, builder::kerning);

            XMLUtil.iterate(element, (child, i) -> {
                switch (child.getTagName()) {
                    case "shadow" -> builder.shadow(new Shadow(
                            Integer.decode(child.getAttribute("color")),
                            Integer.decode(child.getAttribute("dX")),
                            Integer.decode(child.getAttribute("dY"))
                    ));
                    case "outline" -> builder.outline(new Outline(
                            Integer.decode(child.getAttribute("color")),
                            Float.parseFloat(child.getAttribute("weight"))
                    ));
                }
            });

            return Result.of(element.getParent() == null ? builder.build()
                    : element.getParent().getProperty(LayerProperty.STYLE, Style.EMPTY).merge(builder.build()));
        }
    }, "Style");

    public static final LayerProperty<Rectangles> WRAP = register(new LayerProperty<>() {
        @Override
        public Result<Rectangles> parse(RenderableCard.XMLElement element) {
            Rectangles wrap = new Rectangles();

            element.iterate("rect", (r, i) -> {
                wrap.add(new Rectangle(
                        Integer.parseInt(r.getAttribute("x")),
                        Integer.parseInt(r.getAttribute("y")),
                        Integer.parseInt(r.getAttribute("width")),
                        Integer.parseInt(r.getAttribute("height"))
                ));
            });

            return Result.of(wrap);
        }
    }, "Wrap", "wrap");

    public static final LayerProperty<Rectangle2D> BOUNDS = register(new LayerProperty<>() {
        @Override
        public Result<Rectangle2D> parse(RenderableCard.XMLElement element) {
            return Result.of(new Rectangle2D.Double(
                    Integer.parseInt(element.getAttribute("x")),
                    Integer.parseInt(element.getAttribute("y")),
                    Integer.parseInt(element.getAttribute("width")),
                    Integer.parseInt(element.getAttribute("height"))
            ));
        }
    }, "Bounds");

    public static final LayerProperty<Outline> OUTLINE = register(new LayerProperty<>() {
        @Override
        public Result<Outline> parse(RenderableCard.XMLElement element) {
            return Result.of(new Outline(
                    Integer.decode(element.getAttribute("color")),
                    Float.parseFloat(element.getAttribute("weight"))
            ));
        }
    }, "Outline", "outline");

    public static <T> LayerProperty<T> register(LayerProperty<T> property, String... tagNames) {
        for (String tagName : tagNames) {
            PROPERTIES.put(tagName, property);
        }

        return property;
    }

    public abstract Result<T> parse(RenderableCard.XMLElement element);

    @SuppressWarnings("unchecked")
    public static <T > LayerProperty<T> get(String tagName) {
        return (LayerProperty<T>) PROPERTIES.get(tagName);
    }
}
