package dev.hephaestus.proximity.xml;

import dev.hephaestus.proximity.text.Style;
import dev.hephaestus.proximity.util.Result;
import dev.hephaestus.proximity.util.XMLUtil;
import org.w3c.dom.Element;

import java.awt.*;

import static dev.hephaestus.proximity.util.XMLUtil.apply;

public abstract class LayerProperty<T> {
    public static final LayerProperty<Style> STYLE = new LayerProperty<>("Style") {
        @Override
        public Result<Style> parse(Element element) {
            Style.Builder builder = new Style.Builder();

            builder.font(element.getAttribute("font"));

            apply(element, "italicFont", builder::italics);
            apply(element, "capitalization", Style.Capitalization::parse, builder::capitalization);
            apply(element, "color", Integer::decode, builder::color);
            apply(element, "size", Integer::parseInt, builder::size);
            apply(element, "kerning", Float::parseFloat, builder::kerning);

            XMLUtil.iterate(element, (child, i) -> {
                switch (child.getTagName()) {
                    case "shadow" -> builder.shadow(new Style.Shadow(
                            Integer.decode(child.getAttribute("color")),
                            Integer.decode(child.getAttribute("dX")),
                            Integer.decode(child.getAttribute("dY"))
                    ));
                    case "outline" -> builder.outline(new Style.Outline(
                            Integer.decode(child.getAttribute("color")),
                            Integer.decode(child.getAttribute("weight"))
                    ));
                }
            });

            return Result.of(builder.build());
        }
    };

    public static final LayerProperty<Rectangle> WRAP = new LayerProperty<>("wrap") {
        @Override
        public Result<Rectangle> parse(Element element) {
            return Result.of(new Rectangle(
                    Integer.parseInt(element.getAttribute("x")),
                    Integer.parseInt(element.getAttribute("y")),
                    Integer.parseInt(element.getAttribute("width")),
                    Integer.parseInt(element.getAttribute("height"))
            ));
        }
    };

    public final String tagName;

    public LayerProperty(String tagName) {
        this.tagName = tagName;
    }

    public abstract Result<T> parse(Element element);
}
