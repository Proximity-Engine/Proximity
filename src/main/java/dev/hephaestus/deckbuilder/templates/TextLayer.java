package dev.hephaestus.deckbuilder.templates;

import dev.hephaestus.deckbuilder.TextComponent;
import dev.hephaestus.deckbuilder.text.Alignment;
import dev.hephaestus.deckbuilder.text.Style;
import dev.hephaestus.deckbuilder.util.DrawingUtil;
import dev.hephaestus.deckbuilder.util.StatefulGraphics;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TextLayer extends Layer {
    private final Alignment alignment;
    private final Style style;
    private final List<TextComponent> text;

    public TextLayer(Alignment alignment, Style style, List<TextComponent> text, int x, int y) {
        super(x, y);
        this.alignment = alignment;
        this.style = style;
        this.text = text;
    }

    public TextLayer(Alignment alignment, Style style, TextComponent[] text, int x, int y) {
        this(alignment, style, new ArrayList<>(Arrays.asList(text)), x, y);
    }

    private int offset(StatefulGraphics graphics, float fontSizeChange, List<TextComponent> components) {
        int x = 0;

        if (this.alignment == Alignment.RIGHT) {
            for (TextComponent text : components) {
                Style style = text.style() == null ? this.style : text.style();

                float size = (style.size() == null && this.style.size() == null
                        ? graphics.getFont().getSize()
                        : style.size() == null ? this.style.size() : style.size()) + fontSizeChange;

                Font font = style.fontName() == null && this.style.fontName() == null
                        ? graphics.getFont().deriveFont(size)
                        : DrawingUtil.getFont(style.fontName() == null
                        ? this.style.fontName()
                        : style.fontName(), size
                );

                graphics.push(font, Graphics2D::setFont, Graphics2D::getFont);
                TextLayout textLayout = new TextLayout(text.string(), graphics.getFont(), graphics.getFontRenderContext());
                x -= textLayout.getAdvance();
                graphics.pop();
            }
        } else if (this.alignment == Alignment.CENTER) {
            int width = 0;

            for (TextComponent text : components) {
                Style style = text.style() == null ? this.style : text.style();

                float size = (style.size() == null && this.style.size() == null
                        ? graphics.getFont().getSize()
                        : style.size() == null ? this.style.size() : style.size()) + fontSizeChange;

                Font font = style.fontName() == null && this.style.fontName() == null
                        ? graphics.getFont().deriveFont(size)
                        : DrawingUtil.getFont(style.fontName() == null
                        ? this.style.fontName()
                        : style.fontName(), size
                );

                graphics.push(font, Graphics2D::setFont, Graphics2D::getFont);
                TextLayout textLayout = new TextLayout(text.string(), graphics.getFont(), graphics.getFontRenderContext());
                width += textLayout.getAdvance();
                graphics.pop();
            }

            x -= width / 2;
        }

        return x;
    }

    private Rectangle draw(StatefulGraphics graphics, TextComponent text, int x, float fontSizeChange) {
        graphics.push("TextComponent");

        Style style = text.style() == null ? this.style : text.style();

        float size = (style.size() == null && this.style.size() == null
                ? graphics.getFont().getSize()
                : style.size() == null ? this.style.size() : style.size()) + fontSizeChange;

        Font font = style.fontName() == null && this.style.fontName() == null
                ? graphics.getFont().deriveFont(size)
                : DrawingUtil.getFont(style.fontName() == null
                ? this.style.fontName()
                : style.fontName(), size
        );

        Color textColor = style.color() == null && this.style.color() == null
                ? graphics.getColor()
                : DrawingUtil.getColor(style.color() == null ? this.style.color() : style.color());

        graphics.push(textColor, Graphics2D::setColor, Graphics2D::getColor);
        graphics.push(font, Graphics2D::setFont, Graphics2D::getFont);
        // Get shape of the text being drawn
        FontRenderContext fontRenderContext = graphics.getFontRenderContext();
        TextLayout textLayout = new TextLayout(text.string(), graphics.getFont(), fontRenderContext);
        Shape shape = textLayout.getOutline(null);

        graphics.push(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        graphics.push(x, 0);

        // Stroke needs to apply to shadow as well, so we have to set it before shadow drawing
        if (style.outline() != null) {
            graphics.push(new BasicStroke(style.outline().weight()), Graphics2D::setStroke, Graphics2D::getStroke);
        }

        // Actually
        if (style.shadow() != null) {
            graphics.push(style.shadow().dX(), style.shadow().dY());
            graphics.push(DrawingUtil.getColor(style.shadow().color()), Graphics2D::setColor, Graphics2D::getColor);
            graphics.fill(shape);
            graphics.pop(2); // Pop shadow color and the translation
        }

        if (style.outline() != null) {
            // Draw outline
            graphics.push(DrawingUtil.getColor(style.outline().color()), Graphics2D::setColor, Graphics2D::getColor);
            graphics.draw(shape);
            graphics.pop(2); // Pop outline color and the stroke
        }

        // Draw original shape
        graphics.fill(shape);

        graphics.pop("TextComponent");

        return new Rectangle(
                (int) graphics.getTransform().getTranslateX(),
                (int) graphics.getTransform().getTranslateY(),
                (int) textLayout.getAdvance(),
                (int) size
        );
    }


    @Override
    protected Rectangle draw(StatefulGraphics graphics, Rectangle wrap) {
        graphics.push(this.x, this.y);
        float fontSizeChange = 0F;
        Rectangle bounds = null;

        loop: for (int i = 0; i < 100; ++i) {
            int x = this.offset(graphics, fontSizeChange, this.text);

            for (TextComponent component : this.text) {
                Rectangle rectangle = this.draw(graphics, component, x, fontSizeChange);
                bounds = bounds == null ? rectangle : DrawingUtil.encompassing(bounds, rectangle);
                x += rectangle.width;

                if (wrap != null && bounds.intersects(wrap)) {
                    bounds = null;
                    fontSizeChange -= 0.5F;
                    continue loop;
                }
            }

            break;
        }

        graphics.pop();

        return bounds == null ? new Rectangle() : bounds;
    }
}
