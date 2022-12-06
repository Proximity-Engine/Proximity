package dev.hephaestus.proximity.app.api.text;

import java.awt.Color;
import java.util.function.Function;

public final class TextStyle {
    private final TextStyle parent;

    private final Value<String> fontName, italicFontName;
    private final Value<Double> size;
    private final Value<Shadow> shadow;
    private final Value<Outline> outline;
    private final Value<Capitalization> capitalization;
    private final Value<Color> color;

    public TextStyle() {
        this(null);
    }

    public TextStyle(TextStyle parent) {
        this.parent = parent;
        this.fontName = new Value<>();
        this.italicFontName = new Value<>();
        this.size = new Value<>();
        this.shadow = new Value<>();
        this.outline = new Value<>();
        this.capitalization = new Value<>();
        this.color = new Value<>();
    }

    public TextStyle(TextStyle parent, TextStyle original) {
        this.parent = parent;
        this.fontName = original.fontName.copy();
        this.italicFontName = original.italicFontName.copy();
        this.size = original.size.copy();
        this.shadow = original.shadow.copy();
        this.outline = original.outline.copy();
        this.capitalization = original.capitalization.copy();
        this.color = original.color.copy();
    }

    public String getFontName() {
        return this.fontName.isSet() ? this.fontName.getValue() : this.parent == null ? null : this.parent.getFontName();
    }

    public String getItalicFontName() {
        return this.italicFontName.isSet() ? this.italicFontName.getValue() : this.parent == null ? null : this.parent.getItalicFontName();
    }

    public Double getSize() {
        return this.size.isSet() ? this.size.getValue() : this.parent == null ? null : this.parent.getSize();
    }

    public boolean hasShadow() {
        return this.shadow.isSet() && this.shadow.getValue() != null || this.parent != null && this.parent.hasShadow();
    }

    public Shadow getShadow() {
        return this.shadow.isSet() ? this.shadow.getValue() : this.parent == null ? null : this.parent.getShadow();
    }

    public boolean hasOutline() {
        return this.outline.isSet() && this.outline.getValue() != null || this.parent != null && this.parent.hasOutline();
    }

    public Outline getOutline() {
        return this.outline.isSet() ? this.outline.getValue() : this.parent == null ? null : this.parent.getOutline();
    }

    public Capitalization getCapitalization() {
        return this.capitalization.isSet() ? this.capitalization.getValue() : this.parent == null ? null : this.parent.getCapitalization();
    }

    public Color getColor() {
        return this.color.isSet() ? this.color.getValue() : this.parent == null ? null : this.parent.getColor();
    }

    public TextStyle setFontName(String fontName) {
        this.fontName.setValue(fontName);

        return this;
    }

    public TextStyle setItalicFontName(String italicFontName) {
        this.italicFontName.setValue(italicFontName);

        return this;
    }

    public TextStyle setSize(Double size) {
        this.size.setValue(size);

        return this;
    }

    public TextStyle setShadow(Shadow shadow) {
        this.shadow.setValue(shadow);

        return this;
    }

    public TextStyle setOutline(Outline outline) {
        this.outline.setValue(outline);

        return this;
    }

    public TextStyle setCapitalization(Capitalization capitalization) {
        this.capitalization.setValue(capitalization);

        return this;
    }

    public TextStyle setColor(Color color) {
        this.color.setValue(color);

        return this;
    }

    public TextStyle derive(TextStyle parent) {
        return new TextStyle(parent, this);
    }

    public enum Capitalization {
        ALL_CAPS, NO_CAPS, SMALL_CAPS;
    }

    public record Shadow(Color color, int dX, int dY) {
    }

    public record Outline(Color color, float weight) {
    }

    public static final class LineSpacing {
        private final Function<Integer, Integer> spacingFunction;

        private LineSpacing(Function<Integer, Integer> spacingFunction) {
            this.spacingFunction = spacingFunction;
        }

        public int getSpacing(int fontSize) {
            return this.spacingFunction.apply(fontSize);
        }

        public static LineSpacing fixed(int spacing) {
            return new LineSpacing(size -> spacing);
        }

        public static LineSpacing ratio(float ratio) {
            return new LineSpacing(size -> (int) (size * ratio));
        }
    }

    private static final class Value<T> {
        private T value;
        private boolean set;

        public Value(T value) {
            this.value = value;
            this.set = true;
        }

        public Value() {

        }

        public T getValue() {
            return this.value;
        }

        public void setValue(T value) {
            this.value = value;
            this.set = true;
        }

        public boolean isSet() {
            return this.set;
        }

        public Value<T> copy() {
            if (this.isSet()) {
                return new Value<>(this.value);
            } else {
                return new Value<>();
            }
        }
    }
}
