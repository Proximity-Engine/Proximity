package dev.hephaestus.deckbuilder.text;

import com.google.gson.JsonObject;

public record Style(String fontName, Float size, Integer color,
                    Shadow shadow,
                    Outline outline) {
    public static final Style EMPTY = new Style.Builder().build();

    public static final class Builder {
        private String fontName;
        private Float size;
        private Integer color;
        private Shadow shadow;
        private Outline outline;

        public Builder font(String fontName) {
            this.fontName = fontName;
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
            return new Style(this.fontName, this.size, this.color, this.shadow, this.outline);
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
