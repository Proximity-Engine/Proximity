package dev.hephaestus.proximity.text;


import dev.hephaestus.proximity.json.JsonObject;
import dev.hephaestus.proximity.util.Outline;
import dev.hephaestus.proximity.util.Result;
import dev.hephaestus.proximity.util.Shadow;
import dev.hephaestus.proximity.util.XMLUtil;
import dev.hephaestus.proximity.xml.RenderableCard;

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

    public JsonObject toJson() {
        JsonObject result = new JsonObject();

        if (this.fontName != null) result.addProperty("fontName", this.fontName);
        if (this.italicFontName != null) result.addProperty("italicFontName", this.italicFontName);
        if (this.size != null) result.addProperty("size", this.size);
        if (this.kerning != null) result.addProperty("kerning", this.kerning);

        if (this.shadow != null) {
            JsonObject shadow = result.getAsJsonObject("shadow");
            shadow.addProperty("color", this.shadow.color());
            shadow.addProperty("dX", this.shadow.dX());
            shadow.addProperty("dY", this.shadow.dY());
        }

        if (this.outline != null) {
            JsonObject outline = result.getAsJsonObject("outline");
            outline.addProperty("color", this.outline.color());
            outline.addProperty("weight", this.outline.weight());
        }

        if (this.capitalization != null) result.addProperty("capitalization", this.capitalization.toString());
        if (this.color != null) result.addProperty("color", this.color);

        return result;
    }

    public static Result<Style> parse(RenderableCard.XMLElement style) {
        Style.Builder builder = new Style.Builder();

        builder.font(style.getAttribute("font"));

        apply(style, "italicFont", builder::italics);
        apply(style, "capitalization", Style.Capitalization::parse, builder::capitalization);
        apply(style, "color", Integer::decode, builder::color);
        apply(style, "size", Integer::parseInt, builder::size);
        apply(style, "kerning", Float::parseFloat, builder::kerning);

        XMLUtil.iterate(style, (child, i) -> {
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

    public enum Capitalization {
        ALL_CAPS, NO_CAPS, SMALL_CAPS;

        public static Capitalization parse(String string) {
            return Capitalization.valueOf(string.toUpperCase(Locale.ROOT));
        }
    }
}
