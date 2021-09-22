package dev.hephaestus.proximity.text;


import dev.hephaestus.proximity.json.JsonObject;
import dev.hephaestus.proximity.util.Result;
import dev.hephaestus.proximity.util.XMLUtil;
import org.w3c.dom.Element;

import java.util.Locale;

import static dev.hephaestus.proximity.util.XMLUtil.apply;

public record Style(String fontName, String italicFontName, Integer size, Float kerning, Shadow shadow,
                    Outline outline, Capitalization capitalization, Integer color
) {
    public static final Style EMPTY = new Style.Builder().build();

    public Style font(String fontName, String italicFontName) {
        return new Style(
                fontName,
                italicFontName,
                this.size,
                this.kerning, this.shadow, this.outline, null, this.color
        );
    }

    public Style shadow(Shadow shadow) {
        return new Style(
                this.fontName,
                this.italicFontName,
                this.size,
                this.kerning, shadow, this.outline, this.capitalization, this.color
        );
    }

    public Style italic() {
        if (this.italicFontName == null) return this;

        return new Style(this.italicFontName, this.italicFontName, this.size, this.kerning, this.shadow, this.outline, this.capitalization, this.color);
    }

    public Style color(Integer color) {
        return new Style(this.fontName, this.italicFontName, this.size, this.kerning, this.shadow, this.outline, this.capitalization, color);
    }

    public Style merge(Style modifications) {
        if (modifications == null) return this;

        return new Style(
                modifications.fontName == null ? this.fontName : modifications.fontName,
                modifications.italicFontName == null ? this.italicFontName : modifications.italicFontName,
                modifications.size == null ? this.size : modifications.size,
                this.kerning, modifications.shadow == null ? this.shadow : modifications.shadow, modifications.outline == null ? this.outline : modifications.outline, modifications.capitalization == null ? this.capitalization : modifications.capitalization, modifications.color == null ? this.color : modifications.color
        );
    }

    public Style size(int fontSize) {
        return new Style(this.fontName, this.italicFontName, fontSize, this.kerning, this.shadow, this.outline, this.capitalization, this.color);
    }

    public static Result<Style> parse(Element style) {
        Style.Builder builder = new Style.Builder();

        builder.font(style.getAttribute("font"));

        apply(style, "italicFont", builder::italics);
        apply(style, "capitalization", Style.Capitalization::parse, builder::capitalization);
        apply(style, "color", Integer::decode, builder::color);
        apply(style, "size", Integer::parseInt, builder::size);
        apply(style, "kerning", Float::parseFloat, builder::kerning);

        XMLUtil.iterate(style, (child, i) -> {
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

    public static final class Builder {
        private String fontName;
        private String italicFontName;
        private Integer size;
        private Float kerning;
        private Integer color;
        private Shadow shadow;
        private Outline outline;
        private Capitalization capitalization;

        public Builder font(String fontName) {
            this.fontName = fontName;
            return this;
        }

        public Builder italics(String fontName) {
            this.italicFontName = fontName;
            return this;
        }

        public Builder size(Integer size) {
            this.size = size;
            return this;
        }

        public Builder kerning(float kerning) {
            this.kerning = kerning;
            return this;
        }

        public Builder color(Integer color) {
            this.color = color;
            return this;
        }

        public Builder shadow(Shadow shadow) {
            this.shadow = shadow;
            return this;
        }

        public Builder outline(Outline outline) {
            this.outline = outline;
            return this;
        }

        public Builder capitalization(Capitalization capitalization) {
            this.capitalization = capitalization;
            return this;
        }

        public Style build() {
            return new Style(this.fontName, this.italicFontName, this.size, this.kerning, this.shadow, this.outline, this.capitalization, this.color);
        }
    }

    public static record Shadow(int color, int dX, int dY) {
        public static Shadow parse(JsonObject object) {
            return new Shadow(Integer.decode(object.get("color").getAsString()), object.get("dX").getAsInt(), object.get("dY").getAsInt());
        }
    }

    public static record Outline(int color, int weight) {
        public static Outline parse(JsonObject object) {
            return new Outline(Integer.decode(object.get("color").getAsString()), object.get("weight").getAsInt());
        }
    }

    public enum Capitalization {
        ALL_CAPS, NO_CAPS, SMALL_CAPS;

        public static Capitalization parse(String string) {
            return Capitalization.valueOf(string.toUpperCase(Locale.ROOT));
        }
    }
}
