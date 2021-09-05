package dev.hephaestus.proximity.templates;

import dev.hephaestus.proximity.TextComponent;
import dev.hephaestus.proximity.text.Alignment;
import dev.hephaestus.proximity.text.Style;
import dev.hephaestus.proximity.util.DrawingUtil;
import dev.hephaestus.proximity.util.StatefulGraphics;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.util.List;
import java.util.*;

public class TextLayer extends Layer {
    private final Alignment alignment;
    private final Style style;
    private final List<List<TextComponent>> text;
    private final int originalTextBoxHeight;
    private Rectangle textBox;

    public TextLayer(Alignment alignment, Style style, List<List<TextComponent>> text, int x, int y, Rectangle textBox) {
        super(x, y);
        this.alignment = alignment;
        this.style = style;
        this.text = text;
        this.originalTextBoxHeight = textBox == null ? -1 : textBox.height;
        this.textBox = textBox == null ? null : new Rectangle(
                textBox.x,
                this.y,
                textBox.width,
                (int) (textBox.height + text.get(0).get(0).style().size())
        );
    }

    private int offset(StatefulGraphics graphics, float fontSizeChange, List<List<TextComponent>> components) {
        int x = 0;

        if (this.alignment == Alignment.RIGHT) {
            for (List<TextComponent> text : components) {
                for (TextComponent c : text) {
                    Style style = c.style() == null ? this.style : c.style();

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
                    TextLayout textLayout = new TextLayout(c.string(), graphics.getFont(), graphics.getFontRenderContext());
                    x -= textLayout.getAdvance();
                    graphics.pop();
                }
            }
        } else if (this.alignment == Alignment.CENTER) {
            int width = 0;

            for (List<TextComponent> text : components) {
                for (TextComponent c : text) {
                    Style style = c.style() == null ? this.style : c.style();

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
                    TextLayout textLayout = new TextLayout(c.string(), graphics.getFont(), graphics.getFontRenderContext());
                    width += textLayout.getAdvance();
                    graphics.pop();
                }
            }

            x -= width / 2;
        }

        return x;
    }

    private Rectangle draw(StatefulGraphics graphics, TextComponent text, int x, float fontSizeChange, boolean draw) {
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

        Style.Outline outline = style.outline() == null ? this.style.outline() : style.outline();

        graphics.push(textColor, Graphics2D::setColor, Graphics2D::getColor);
        graphics.push(font, Graphics2D::setFont, Graphics2D::getFont);
        // Get shape color the glyphs being drawn
        FontRenderContext fontRenderContext = graphics.getFontRenderContext();
        TextLayout textLayout = new TextLayout(text.string(), graphics.getFont(), fontRenderContext);
        Shape shape = textLayout.getOutline(null);

        graphics.push(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        graphics.push(x, 0);

        // Stroke needs to apply to shadow as well, so we have to set it before shadow drawing
        if (outline != null) {
            graphics.push(new BasicStroke(outline.weight()), Graphics2D::setStroke, Graphics2D::getStroke);
        }

        // Actually
        if (style.shadow() != null) {
            graphics.push(style.shadow().dX(), style.shadow().dY());
            graphics.push(DrawingUtil.getColor(style.shadow().color()), Graphics2D::setColor, Graphics2D::getColor);

            if (draw) {
                graphics.fill(shape);
            }

            graphics.pop(2); // Pop shadow color and the translation
        }

        if (outline != null) {
            // Draw outline
            graphics.push(DrawingUtil.getColor(outline.color()), Graphics2D::setColor, Graphics2D::getColor);

            if (draw) {
                graphics.draw(shape);
            }

            graphics.pop(2); // Pop outline color and the stroke
        }

        // Draw original shape
        if (draw) {
            graphics.fill(shape);
        }

        graphics.pop("TextComponent");

        return new Rectangle(
                (int) graphics.getTransform().getTranslateX() + x,
                (int) (graphics.getTransform().getTranslateY() - shape.getBounds().height),
                (int) textLayout.getAdvance(),
                shape.getBounds().height
        );
    }

    private Rectangle draw(StatefulGraphics graphics, List<TextComponent> text, int x, float fontSizeChange, boolean draw) {
        Rectangle bounds = null;

        for (TextComponent component : text) {
            Rectangle componentBounds = this.draw(graphics, component, x, fontSizeChange, draw);

            bounds = bounds == null ? componentBounds : DrawingUtil.encompassing(bounds, componentBounds);

            x += componentBounds.width;
        }

        return bounds;
    }

    @Override
    protected Rectangle draw(StatefulGraphics graphics, Rectangle wrap) {
        return this.draw(graphics, wrap, 0F, true);
    }

    protected Rectangle draw(StatefulGraphics graphics, Rectangle wrap, float fontSizeChange, boolean draw) {
        Rectangle bounds = null;

        graphics.push("Text");

        if (draw && this.textBox != null) {
            Rectangle drawnBounds = this.draw(graphics, wrap, fontSizeChange, false);

            while(drawnBounds.height > this.textBox.height) {
                this.textBox = new Rectangle(
                        this.textBox.x,
                        this.y,
                        this.textBox.width,
                        (int) (this.originalTextBoxHeight + this.text.get(0).get(0).style().size() + fontSizeChange)
                );

                fontSizeChange -= 5;
                drawnBounds = this.draw(graphics, wrap, fontSizeChange, false);
            }

            if (false /* DEBUG */) {
                graphics.push(new BasicStroke(5), Graphics2D::setStroke, Graphics2D::getStroke);
                graphics.push(DrawingUtil.getColor(0xFFFF0000), Graphics2D::setColor, Graphics2D::getColor);
                graphics.drawRect(this.x, this.y, this.textBox.width, this.textBox.height);
                graphics.pop(2);
            }

            graphics.push(0, (int) ((this.textBox.height - drawnBounds.height) / 2 + this.text.get(0).get(0).style().size() + fontSizeChange));
        }

        graphics.push(this.x, this.y);

        loop: for (int i = 0; i < 100; ++i) {
            graphics.push("Loop");
            int x = this.offset(graphics, fontSizeChange, this.text);
            int minX = x;

            List<TextComponent> lastTextComponent = null;
            Deque<List<TextComponent>> deque = new ArrayDeque<>();

            for (List<TextComponent> text : this.text) {
                deque.add(new ArrayList<>(text));
            }

            while(!deque.isEmpty()) {
                List<TextComponent> text = deque.pop();

                if (text.get(0).string().startsWith("\n")) {
                    x = minX;
                    graphics.push(0, (int) ((text.get(0).style().size() + fontSizeChange) * 1.5));

                    if (!deque.isEmpty()) {
                        text = deque.pop();

                        if (text.get(0).string().startsWith(" ")) {
                            text.set(0, new TextComponent(text.get(0).style(), text.get(0).string().substring(1)));
                        }
                    }
                }

                Rectangle rectangle = this.draw(graphics, text, x, fontSizeChange, false);

                if (this.textBox != null && rectangle.x + rectangle.width > this.textBox.x + this.textBox.width && text != lastTextComponent) {
                    x = minX;
                    graphics.push(0, (int) (text.get(0).style().size() + fontSizeChange));

                    if (text.get(0).string().startsWith(" ")) {
                        text.set(0, new TextComponent(text.get(0).style(), text.get(0).string().substring(1)));
                    }

                    deque.addFirst(text);
                    lastTextComponent = text;
                    continue;
                }

                bounds = bounds == null ? rectangle : DrawingUtil.encompassing(bounds, rectangle);

                if ((wrap != null && bounds.intersects(wrap))) {
                    bounds = null;
                    fontSizeChange -= 5;
                    continue loop;
                }

                if (draw) {
                    this.draw(graphics, text, x, fontSizeChange, true);
                }

                x += rectangle.width;

                lastTextComponent = text;
            }

            break;
        }

        graphics.pop("Text");

        if (draw && bounds != null && false /* DEBUG */) {
            graphics.push(new BasicStroke(5), Graphics2D::setStroke, Graphics2D::getStroke);
            graphics.push(DrawingUtil.getColor(0xFF0000FF), Graphics2D::setColor, Graphics2D::getColor);
            graphics.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
            graphics.pop(2);
        }

        return bounds == null ? new Rectangle() : bounds;
    }
}
