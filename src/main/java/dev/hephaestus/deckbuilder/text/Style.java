package dev.hephaestus.deckbuilder.text;

import com.google.gson.JsonObject;
import dev.hephaestus.deckbuilder.TextComponent;
import dev.hephaestus.deckbuilder.util.DrawingUtil;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;

public record Style(String fontName, Float size, Integer color,
                    Alignment alignment,
                    Shadow shadow,
                    Outline outline) {

    private int offset(Graphics2D graphics, Text text) {
        int x = 0;

        if (this.alignment == Alignment.RIGHT) {
            for (dev.hephaestus.deckbuilder.TextComponent component : text.components()) {
                x -= graphics.getFontMetrics().stringWidth(component.string());
            }
        } else if (this.alignment == Alignment.CENTER) {
            int width = 0;

            for (TextComponent component : text.components()) {
                width += graphics.getFontMetrics().stringWidth(component.string());
            }

            x -= width / 2;
        }

        return x;
    }

    private void draw(Graphics2D graphics, TextComponent text, int x, int y) {
        // Retrieve the original color
        Color color = graphics.getColor();

        // Get shape of the text being drawn
        FontRenderContext fontRenderContext = graphics.getFontRenderContext();
        TextLayout textLayout = new TextLayout(text.string(), graphics.getFont(), fontRenderContext);
        Shape shape = textLayout.getOutline(null);

        // Stroke needs to apply to shadow as well, so we have to set it before shadow drawing
        if (this.outline != null) {
            graphics.setStroke(new BasicStroke(this.outline.weight));
        }

        // Actually
        if (this.shadow != null) {
            graphics.getTransform().translate(x + this.shadow.dX, y + this.shadow.dY);
            graphics.setColor(DrawingUtil.getColor(this.shadow.color));
            graphics.draw(shape);
            graphics.getTransform().translate(-x - this.shadow.dX, -y - this.shadow.dY);
        }

        // Translate origin so we draw the shape at the right position
        graphics.getTransform().translate(x, y);

        if (this.outline != null) {
            // Draw outline
            graphics.setColor(DrawingUtil.getColor(this.outline.color));
            graphics.draw(shape);
            graphics.setStroke(null);
        }

        // Draw original shape
        graphics.setColor(text.color() == null ? color : DrawingUtil.getColor(text.color()));
        graphics.draw(shape);

        // Reset drawing origin
        graphics.getTransform().translate(-x, -y);

        // Reset color state
        graphics.setColor(color);
    }

    public void draw(Graphics2D graphics, Text text, int x, int y) {
        float size = this.size == null ? graphics.getFont().getSize() : this.size;
        Font font = DrawingUtil.getFont(this.fontName, size);
        if (font != null) graphics.setFont(font);

        x += this.offset(graphics, text);

        for (TextComponent component : text.components()) {
            this.draw(graphics, component, x, y);

            x += graphics.getFontMetrics().stringWidth(component.string());
        }
    }

    public static final class Builder {
        private String fontName;
        private Float size;
        private Integer color;
        private Alignment alignment;
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

        public Builder alignment(Alignment alignment) {
            this.alignment = alignment;
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
            return new Style(this.fontName, this.size, this.color, this.alignment, this.shadow, this.outline);
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
