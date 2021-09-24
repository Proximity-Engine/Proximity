package dev.hephaestus.proximity.templates.layers;

import dev.hephaestus.proximity.Proximity;
import dev.hephaestus.proximity.templates.Template;
import dev.hephaestus.proximity.util.Pair;
import dev.hephaestus.proximity.text.TextComponent;
import dev.hephaestus.proximity.text.TextAlignment;
import dev.hephaestus.proximity.text.Style;
import dev.hephaestus.proximity.util.DrawingUtil;
import dev.hephaestus.proximity.util.StatefulGraphics;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.util.*;
import java.util.List;

public class TextLayer extends Layer {
    private final Template template;
    private final TextAlignment alignment;
    private final Style style;
    private final List<List<TextComponent>> text;

    public TextLayer(String parentId, String id, int x, int y, Template template, Style style, List<List<TextComponent>> text, TextAlignment alignment, Rectangle textBox) {
        super(parentId, id, x, y);
        this.template = template;
        this.alignment = alignment;
        this.style = style;
        this.text = text;
        this.bounds = textBox == null ? null : new Rectangle(
                textBox.x,
                y,
                textBox.width,
                textBox.height
        );
    }

    private int offset(StatefulGraphics graphics, float fontSizeChange, List<List<TextComponent>> components) {
        int x = 0;

        if (this.alignment == TextAlignment.RIGHT) {
            for (List<TextComponent> text : components) {
                for (TextComponent c : text) {
                    if (c.string().isEmpty()) continue;

                    Style style = c.style() == null ? this.style : c.style();

                    float size = (style.size() == null && this.style.size() == null
                            ? graphics.getFont().getSize()
                            : style.size() == null ? this.style.size() : style.size()) + fontSizeChange;

                    Font font = style.fontName() == null && this.style.fontName() == null
                            ? graphics.getFont().deriveFont(size)
                            : DrawingUtil.getFont(this.template.getSource(), style.fontName() == null
                            ? this.style.fontName()
                            : style.fontName(), size
                    );

                    if (font != null) {
                        Map<TextAttribute, Object> attributes = new HashMap<>();

                        float kerning = style.kerning() == null && this.style.kerning() == null ? 0
                                : style.kerning() == null ? this.style.kerning() : style.kerning();

                        attributes.put(TextAttribute.TRACKING, kerning);

                        font = font.deriveFont(attributes);
                    }

                    graphics.push(font, Graphics2D::setFont, Graphics2D::getFont);
                    TextLayout textLayout = new TextLayout(c.string(), graphics.getFont(), graphics.getFontRenderContext());
                    x -= textLayout.getAdvance();
                    graphics.pop();
                }
            }
        } else if (this.alignment == TextAlignment.CENTER) {
            int width = 0;

            for (List<TextComponent> text : components) {
                for (TextComponent c : text) {
                    Style style = c.style() == null ? this.style : c.style();

                    float size = (style.size() == null && this.style.size() == null
                            ? graphics.getFont().getSize()
                            : style.size() == null ? this.style.size() : style.size()) + fontSizeChange;

                    Font font = style.fontName() == null && this.style.fontName() == null
                            ? graphics.getFont().deriveFont(size)
                            : DrawingUtil.getFont(this.template.getSource(), style.fontName() == null
                            ? this.style.fontName()
                            : style.fontName(), size
                    );

                    if (font != null) {
                        Map<TextAttribute, Object> attributes = new HashMap<>();

                        attributes.put(TextAttribute.KERNING, this.style.kerning());

                        font = font.deriveFont(attributes);
                    }

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

    private Pair<Rectangle, Integer> draw(StatefulGraphics graphics, TextComponent text, int x, float fontSizeChange, boolean draw) {
        graphics.push("TextComponent");

        Style style = text.style() == null ? this.style : text.style();

        float size = (style.size() == null && this.style.size() == null
                ? graphics.getFont().getSize()
                : style.size() == null ? this.style.size() : style.size()) + fontSizeChange;

        if (size <= 0.001) {
            return new Pair<>(new Rectangle(
                    (int) graphics.getTransform().getTranslateX() + x,
                    (int) (graphics.getTransform().getTranslateY()),
                    0,
                  0
            ), 0);

        }

        Font font = style.fontName() == null && this.style.fontName() == null
                ? graphics.getFont().deriveFont(size)
                : DrawingUtil.getFont(this.template.getSource(), style.fontName() == null
                ? this.style.fontName()
                : style.fontName(), size
        );

        float kerning = style.kerning() == null && this.style.kerning() == null ? 0
                : style.kerning() == null ? this.style.kerning() : style.kerning();

        if (font != null) {
            Map<TextAttribute, Object> attributes = new HashMap<>();

            attributes.put(TextAttribute.TRACKING, kerning / font.getSize());

            font = font.deriveFont(attributes);
        }

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
        if (outline != null && outline.weight() > 0) {
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

        if (outline != null && outline.weight() > 0) {
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

        return new Pair<>(new Rectangle(
                (int) graphics.getTransform().getTranslateX() + x,
                (int) (graphics.getTransform().getTranslateY()) + shape.getBounds().y,
                (int) textLayout.getAdvance(),
                shape.getBounds().height
        ), (int) textLayout.getAscent());
    }

    private Pair<Rectangle, Integer> draw(StatefulGraphics graphics, List<TextComponent> text, int x, float fontSizeChange, boolean draw) {
        Rectangle bounds = null;
        Integer height = null;

        for (TextComponent component : text) {
            if (component.string().isEmpty()) continue;

            Pair<Rectangle, Integer> pair = this.draw(graphics, component, x, fontSizeChange, draw);
            Rectangle componentBounds = pair.left();

            bounds = bounds == null ? componentBounds : DrawingUtil.encompassing(bounds, componentBounds);
            height = height == null ? pair.right() : Math.max(height, pair.right());

            x += componentBounds.width;
        }

        if (bounds != null && this.template.getOptions().getAsBoolean("debug")) {
            Proximity.LOG.info(text);
            Proximity.LOG.info(String.format(
                    "%s  %10s  %5d %5d %5d %5d", draw ? "DRAW" : "NOPE", "", bounds.x, bounds.y, bounds.width, bounds.height
            ));
        }

        return new Pair<>(bounds, height);
    }

    @Override
    public Rectangle draw(StatefulGraphics graphics, Rectangle wrap, boolean draw, int scale) {
        return this.draw(graphics, wrap == null ? this.wrap : wrap, 5 * scale, draw).left();
    }

    protected Pair<Rectangle, Integer> draw(StatefulGraphics graphics, Rectangle wrap, float fontSizeChange, boolean draw) {
        Rectangle bounds = null;
        int firstRowHeight = 0;
        boolean firstRow;

        graphics.push("Text");

        if (draw && this.wrap != null && this.template.getOptions().getAsBoolean("debug")) {
            graphics.push(new BasicStroke(5), Graphics2D::setStroke, Graphics2D::getStroke);
            graphics.push(DrawingUtil.getColor(0xF0F0F0), Graphics2D::setColor, Graphics2D::getColor);
            graphics.drawRect((int) this.wrap.getX(), (int) this.wrap.getY(), this.wrap.width, this.wrap.height);
            graphics.pop(2);
        }

        if (draw && this.bounds != null) {
            Pair<Rectangle, Integer> pair = this.draw(graphics, wrap, fontSizeChange, false);
            Integer drawnFirstRowHeight = pair.right();

            graphics.push(0, drawnFirstRowHeight);
            pair = this.draw(graphics, wrap, fontSizeChange, false);
            Rectangle drawnBounds = pair.left();
            drawnFirstRowHeight = pair.right();
            graphics.pop();

            if (this.template.getOptions().getAsBoolean("debug")) {
                graphics.push(new BasicStroke(5), Graphics2D::setStroke, Graphics2D::getStroke);
                graphics.push(DrawingUtil.getColor(0xFFFFFF00), Graphics2D::setColor, Graphics2D::getColor);
                graphics.drawRect(drawnBounds.x, drawnBounds.y, drawnBounds.width, drawnBounds.height);
                graphics.pop(2);
            }

            while(this.bounds.height < drawnBounds.height) {
                fontSizeChange -= 5;
                graphics.push(0, drawnFirstRowHeight);
                pair = this.draw(graphics, wrap, fontSizeChange, false);
                drawnBounds = pair.left();
                drawnFirstRowHeight = pair.right();
                graphics.pop();

                if (this.template.getOptions().getAsBoolean("debug")) {
                    graphics.push(new BasicStroke(5), Graphics2D::setStroke, Graphics2D::getStroke);
                    graphics.push(DrawingUtil.getColor(0xFFFFFF00), Graphics2D::setColor, Graphics2D::getColor);
                    graphics.drawRect(drawnBounds.x, drawnBounds.y, drawnBounds.width, drawnBounds.height);
                    graphics.pop(2);
                }
            }

            if (this.template.getOptions().getAsBoolean("debug")) {
                graphics.push(new BasicStroke(5), Graphics2D::setStroke, Graphics2D::getStroke);
                graphics.push(DrawingUtil.getColor(0xFFFF0000), Graphics2D::setColor, Graphics2D::getColor);
                graphics.drawRect(this.getX(), this.getY(), this.bounds.width, this.bounds.height);
                graphics.pop(2);
            }

            graphics.push(0, drawnFirstRowHeight);
        }

        graphics.push(this.getX(), this.getY());

        List<Rectangle> textBounds = new ArrayList<>();

        loop: for (int i = 0; i < 100; ++i) {
            graphics.push("Loop");
            int x = this.offset(graphics, fontSizeChange, this.text);
            int minX = x;

            firstRowHeight = 0;
            firstRow = true;


            List<TextComponent> lastTextComponent = null;
            Deque<List<TextComponent>> deque = new ArrayDeque<>();

            for (List<TextComponent> text : this.text) {
                deque.add(new ArrayList<>(text));
            }

            while(!deque.isEmpty()) {
                List<TextComponent> text = deque.pop();

                if (text.isEmpty()) continue;

                if (text.get(0).string().startsWith("\n")) {
                    x = minX;

                    float size = text.get(0).style().size() == null ? this.style.size() : text.get(0).style().size();

                    graphics.push(0, (int) (size + fontSizeChange));

                    if (text.get(0).string().startsWith("\n\n")) {
                        graphics.push(0, (int) ((size + fontSizeChange) * 0.325));
                    }

                    if (text.get(0).string().startsWith("\n\n\n")) {
                        graphics.push(0, (int) ((size + fontSizeChange) * 0.325));
                    }

                    if (!deque.isEmpty()) {
                        text = deque.pop();

                        if (text.get(0).string().startsWith(" ")) {
                            text.set(0, new TextComponent(text.get(0).style(), text.get(0).string().substring(1)));
                        }
                    }
                }

                Pair<Rectangle, Integer> pair = this.draw(graphics, text, x, fontSizeChange, false);

                if (pair.left() == null || pair.right() == null) continue;

                Rectangle rectangle = pair.left();

                if (firstRow) {
                    firstRowHeight = Integer.max(firstRowHeight, pair.right());
                }

                if (this.bounds != null && rectangle.x + rectangle.width > this.bounds.x + this.bounds.width && text != lastTextComponent) {
                    x = minX;
                    graphics.push(0, (int) (text.get(0).style().size() + fontSizeChange));

                    if (text.get(0).string().startsWith(" ")) {
                        text.set(0, new TextComponent(text.get(0).style(), text.get(0).string().substring(1)));
                    }

                    deque.addFirst(text);
                    lastTextComponent = text;
                    firstRow = false;
                    continue;
                }

                if (wrap != null) {
                    if (this.bounds == null) {
                        bounds = bounds == null ? rectangle : DrawingUtil.encompassing(bounds, rectangle);

                        if (bounds.intersects(wrap)) {
                            bounds = null;
                            fontSizeChange -= 5;
                            continue loop;
                        }
                    } else if (rectangle.intersects(wrap)) {
                        x = minX;
                        graphics.push(0, (int) (text.get(0).style().size() + fontSizeChange));

                        if (text.get(0).string().startsWith(" ")) {
                            text.set(0, new TextComponent(text.get(0).style(), text.get(0).string().substring(1)));
                        }

                        deque.addFirst(text);
                        lastTextComponent = text;
                        firstRow = false;
                        continue;
                    }
                }

                if (draw) {
                    this.draw(graphics, text, x, fontSizeChange, true);
                }

                x += rectangle.width;
                textBounds.add(rectangle);
                lastTextComponent = text;
                bounds = bounds == null ? rectangle : DrawingUtil.encompassing(bounds, rectangle);
            }

            break;
        }

        graphics.pop("Text");

        if (draw && bounds != null && this.template.getOptions().getAsBoolean("debug")) {
            graphics.push(new BasicStroke(5), Graphics2D::setStroke, Graphics2D::getStroke);
            graphics.push(DrawingUtil.getColor(0xFF0000FF), Graphics2D::setColor, Graphics2D::getColor);
            graphics.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
            graphics.pop(2);

            for (Rectangle rectangle : textBounds) {
                graphics.push(new BasicStroke(5), Graphics2D::setStroke, Graphics2D::getStroke);
                graphics.push(DrawingUtil.getColor(0xFF00FF00), Graphics2D::setColor, Graphics2D::getColor);
                graphics.drawRect(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
                graphics.pop(2);
            }
        }

        return new Pair<>(bounds == null ? new Rectangle() : bounds, firstRowHeight);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        for (List<TextComponent> list: this.text) {
            for (TextComponent component : list) {
                builder.append(component.string());
            }
        }

        return builder.toString();
    }
}
