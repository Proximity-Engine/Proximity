package dev.hephaestus.deckbuilder.text;

import com.google.gson.JsonObject;

public record Style(String fontName, String italicFontName, Float size, Integer color,
                    Shadow shadow,
                    Outline outline) {
    public static final Style EMPTY = new Style.Builder().build();

    public Style font(String fontName, String italicFontName) {
        return new Style(
                fontName,
                italicFontName,
                this.size,
                this.color,
                this.shadow,
                this.outline
        );
    }

    public Style shadow(Shadow shadow) {
        return new Style(
                this.fontName,
                this.italicFontName,
                this.size,
                this.color,
                shadow,
                this.outline
        );
    }

    public Style italic() {
        if (this.italicFontName == null) return this;

        return new Style(this.italicFontName, this.italicFontName, this.size, this.color, this.shadow, this.outline);
    }

    public Style color(Integer color) {
        return new Style(this.fontName, this.italicFontName, this.size, color, this.shadow, this.outline);
    }

    public Style merge(Style modifications) {
        if (modifications == null) return this;

        return new Style(
                modifications.fontName == null ? this.fontName : modifications.fontName,
                modifications.italicFontName == null ? this.italicFontName : modifications.italicFontName,
                modifications.size == null ? this.size : modifications.size,
                modifications.color == null ? this.color : modifications.color,
                modifications.shadow == null ? this.shadow : modifications.shadow,
                modifications.outline == null ? this.outline : modifications.outline
        );
    }

    public static final class Builder {
        private String fontName;
        private String italicFontName;
        private Float size;
        private Integer color;
        private Shadow shadow;
        private Outline outline;

        public Builder font(String fontName) {
            this.fontName = fontName;
            return this;
        }

        public Builder italics(String fontName) {
            this.italicFontName = fontName;
            return this;
        }

        public Builder size(Float size) {
            this.size = size;
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

        public Style build() {
            return new Style(this.fontName, this.italicFontName, this.size, this.color, this.shadow, this.outline);
        }
    }

    public static record Shadow(int color, int dX, int dY) {
        public static Shadow parse(JsonObject object) {
            return new Shadow(Integer.decode(object.get("color").getAsString()), object.get("dX").getAsInt(), object.get("dY").getAsInt());
        }
    }

    public static record Outline(int color, int weight) {
        public static Outline parse(JsonObject object) {
            return new Outline(object.get("weight").getAsInt(), Integer.decode(object.get("color").getAsString()));
        }
    }
}
