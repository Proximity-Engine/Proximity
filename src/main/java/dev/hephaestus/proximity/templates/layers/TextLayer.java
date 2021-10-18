package dev.hephaestus.proximity.templates.layers;

import dev.hephaestus.proximity.Proximity;
import dev.hephaestus.proximity.util.*;
import dev.hephaestus.proximity.xml.RenderableCard;
import dev.hephaestus.proximity.text.TextComponent;
import dev.hephaestus.proximity.text.TextAlignment;
import dev.hephaestus.proximity.text.Style;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.List;

public class TextLayer extends Layer {
    private final RenderableCard card;
    private final TextAlignment alignment;
    private final Style style;
    private final List<List<TextComponent>> text;
    private final int maxFontSize;

    public TextLayer(String id, int x, int y, RenderableCard card, Style style, List<List<TextComponent>> text, TextAlignment alignment, Rectangle textBox) {
        super(id, x, y);
        this.card = card;
        this.alignment = alignment;
        this.style = style;
        this.text = text;
        this.bounds = textBox == null ? null : new Rectangle(
                textBox.x,
                y,
                textBox.width,
                textBox.height
        );

        int maxFontSize = 0;

        for (var list : text) {
            for (var component : list) {
                if (component.style().size() != null) {
                    maxFontSize = Math.max(maxFontSize, component.style().size());
                }
            }
        }

        this.maxFontSize = maxFontSize;
    }

    private int offset(StatefulGraphics graphics, Box<Float> scale, List<List<TextComponent>> components) {
        int x = 0;

        if (this.alignment == TextAlignment.RIGHT) {
            for (List<TextComponent> text : components) {
                for (TextComponent c : text) {
                    if (c.string().isEmpty()) continue;

                    Style style = c.style() == null ? this.style : c.style();

                    float size = (style.size() == null && this.style.size() == null
                            ? graphics.getFont().getSize()
                            : style.size() == null ? this.style.size() : style.size()) + (scale.get());

                    Font font = style.fontName() == null && this.style.fontName() == null
                            ? graphics.getFont().deriveFont(size)
                            : DrawingUtil.getFont(this.card, style.fontName() == null
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
                            : style.size() == null ? this.style.size() : style.size()) + (scale.get());

                    Font font = style.fontName() == null && this.style.fontName() == null
                            ? graphics.getFont().deriveFont(size)
                            : DrawingUtil.getFont(this.card, style.fontName() == null
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

    private Pair<Rectangle2D, Integer> draw(StatefulGraphics graphics, TextComponent text, int x, Box<Float> scale, boolean draw) {
        graphics.push("TextComponent");

        Style style = text.style() == null ? this.style : text.style();

        float size = (style.size() == null && this.style.size() == null
                ? graphics.getFont().getSize()
                : style.size() == null ? this.style.size() : style.size()) + (scale.get());

        if (size <= 0.001) {
            return new Pair<>(new Rectangle2D.Double(
                    (int) graphics.getTransform().getTranslateX() + x,
                    (int) (graphics.getTransform().getTranslateY()),
                    0,
                  0
            ), 0);

        }

        Font font = style.fontName() == null && this.style.fontName() == null
                ? graphics.getFont().deriveFont(size)
                : DrawingUtil.getFont(this.card, style.fontName() == null
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

        Outline outline = style.outline() == null ? this.style.outline() : style.outline();

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

    private Pair<Rectangle2D, Integer> draw(StatefulGraphics graphics, List<TextComponent> text, int x, Box<Float> scale, boolean draw) {
        Rectangle2D bounds = null;
        Integer height = null;

        for (TextComponent component : text) {
            Style style = component.style();
            float kerning = style.kerning() == null && this.style.kerning() == null ? 0
                    : style.kerning() == null ? this.style.kerning() : style.kerning();

            if (component.string().isEmpty()) continue;

            Pair<Rectangle2D, Integer> pair = this.draw(graphics, component, x, scale, draw);
            Rectangle2D componentBounds = pair.left();

            bounds = bounds == null ? componentBounds : DrawingUtil.encompassing(bounds, componentBounds);
            height = height == null ? pair.right() : Math.max(height, pair.right());

            x += componentBounds.getWidth() + kerning;
        }

        if (bounds != null && this.card.getAsJsonObject(Keys.OPTIONS).getAsBoolean("debug")) {
            Proximity.LOG.info(text);
            Proximity.LOG.info(String.format(
                    "%s  %10s  %5f %5f %5f %5f", draw ? "DRAW" : "NOPE", "", bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight()
            ));
        }

        return new Pair<>(bounds, height);
    }

    @Override
    public Rectangles draw(StatefulGraphics graphics, Rectangles wrap, boolean draw, Box<Float> scale) {
        if (this.maxFontSize - scale.get() <= 0) {
            return new Rectangles();
        }

        return this.draw(graphics, wrap == null ? this.wrap : wrap, scale, draw, true).bounds;
    }

    protected Result draw(StatefulGraphics graphics, Rectangles wrap, Box<Float> scale, boolean draw, boolean measure) {
        Rectangles bounds = new Rectangles();
        int firstRowHeight = 0;
        boolean firstRow;

        graphics.push("Text");

        if (measure && this.bounds != null) {
            Result measurement = this.draw(graphics, wrap, scale, false, false);

            if (measurement.bounds.isInfinite()) {
                return measurement;
            }

            if (measurement.lastLineWidth < this.bounds.getWidth() / 5 && measurement.lastLineBroken && measurement.firstRowHeight * 10 > this.bounds.getWidth() / 5) {
                return new Result(Rectangles.infinity(), measurement.firstRowHeight, measurement.lastLineWidth, false);
            }

            graphics.push(this.getX(), this.getY() + measurement.firstRowHeight);
        } else {
            graphics.push(this.getX(), this.getY());
        }

        if (draw && this.wrap != null && this.card.getAsJsonObject(Keys.OPTIONS).getAsBoolean("debug")) {
            graphics.push(new BasicStroke(5), Graphics2D::setStroke, Graphics2D::getStroke);
            graphics.push(DrawingUtil.getColor(0xF0F0F0), Graphics2D::setColor, Graphics2D::getColor);

            for (Rectangle2D rectangle : this.wrap) {
                graphics.drawRect((int) rectangle.getX(), (int) rectangle.getY(), (int) rectangle.getWidth(), (int) rectangle.getHeight());
            }

            graphics.pop(2);
        }

        int lastLineWidth = 0;
        boolean lastLineBroken = false;

        loop: for (int i = 0; i < 100; ++i) {
            graphics.push("Loop");
            int x = this.offset(graphics, scale, this.text);
            int minX = x;

            firstRowHeight = 0;
            firstRow = true;

            List<TextComponent> lastTextComponent = null;
            Deque<List<TextComponent>> deque = new ArrayDeque<>();

            for (List<TextComponent> text : this.text) {
                deque.add(new ArrayList<>(text));
            }

            lastLineBroken = false;

            while(!deque.isEmpty()) {
                List<TextComponent> text = deque.pop();

                if (text.isEmpty()) continue;

                if (text.get(0).string().startsWith("\n")) {
                    if (lastLineWidth < this.bounds.getWidth() / 5 && lastLineBroken && firstRowHeight * 10 > this.bounds.getWidth() / 5) {
                        graphics.pop("Text");
                        return new Result(Rectangles.infinity(), firstRowHeight, lastLineWidth, false);
                    }

                    lastLineBroken = false;
                    x = minX;
                    lastLineWidth = 0;

                    float size = text.get(0).style().size() == null ? this.style.size() : text.get(0).style().size();

                    graphics.push(0, (int) (size + scale.get()));

                    if (text.get(0).string().startsWith("\n\n")) {
                        graphics.push(0, (int) ((size + scale.get()) * 0.325));
                    }

                    if (text.get(0).string().startsWith("\n\n\n")) {
                        graphics.push(0, (int) ((size + scale.get()) * 0.325));
                    }

                    if (!deque.isEmpty()) {
                        text = deque.pop();

                        if (text.get(0).string().startsWith(" ")) {
                            text.set(0, new TextComponent.Literal(text.get(0).style(), text.get(0).string().substring(1)));
                        }
                    }
                }

                Pair<Rectangle2D, Integer> pair = this.draw(graphics, text, x, scale, false);

                if (pair.left() == null || pair.right() == null) continue;

                Rectangle2D rectangle = pair.left();

                if (firstRow) {
                    firstRowHeight = Integer.max(firstRowHeight, pair.right());
                }

                if (this.bounds != null && rectangle.getX() + rectangle.getWidth() > this.bounds.getX() + this.bounds.getWidth() && text != lastTextComponent) {
                    x = minX;
                    lastLineWidth = 0;
                    graphics.push(0, (int) (text.get(0).style().size() + scale.get()));
                    lastLineBroken = true;

                    if (text.get(0).string().startsWith(" ")) {
                        text.set(0, new TextComponent.Literal(text.get(0).style(), text.get(0).string().substring(1)));
                    }

                    deque.addFirst(text);
                    lastTextComponent = text;
                    firstRow = false;
                    continue;
                }

                if (wrap != null) {
                    if (this.bounds == null) {
                        bounds.add(rectangle);

                        if (bounds.intersects(wrap)) {
                            bounds.clear();
                            scale.set(scale.get() - 1);
                            graphics.pop("Loop");
                            continue loop;
                        }
                    } else if (wrap.intersects(rectangle)) {
                        x = minX;
                        lastLineBroken = true;
                        lastLineWidth = 0;
                        graphics.push(0, (int) (text.get(0).style().size() + scale.get()));

                        if (text.get(0).string().startsWith(" ")) {
                            text.set(0, new TextComponent.Literal(text.get(0).style(), text.get(0).string().substring(1)));
                        }

                        deque.addFirst(text);
                        lastTextComponent = text;
                        firstRow = false;
                        continue;
                    }
                }

                if (draw) {
                    this.draw(graphics, text, x, scale, true);
                }

                x += rectangle.getWidth();
                lastLineWidth += rectangle.getWidth();
                lastTextComponent = text;
                bounds.add(rectangle);
            }

            break;
        }

        graphics.pop("Text");

        if (draw && !bounds.isEmpty() && this.card.getAsJsonObject(Keys.OPTIONS).getAsBoolean("debug")) {
            graphics.push(new BasicStroke(5), Graphics2D::setStroke, Graphics2D::getStroke);
            graphics.push(DrawingUtil.getColor(0xFF0000FF), Graphics2D::setColor, Graphics2D::getColor);

            for (Rectangle2D rectangle : bounds) {
                graphics.drawRect((int) rectangle.getX(), (int) rectangle.getY(), (int) rectangle.getWidth(), (int) rectangle.getHeight());
            }

            graphics.pop(2);
        }

        if (draw && wrap != null && this.card.getAsJsonObject(Keys.OPTIONS).getAsBoolean("debug")) {
            graphics.push(new BasicStroke(5), Graphics2D::setStroke, Graphics2D::getStroke);
            graphics.push(DrawingUtil.getColor(0xFF0000FF), Graphics2D::setColor, Graphics2D::getColor);

            for (Rectangle2D rectangle : wrap) {
                graphics.drawRect((int) rectangle.getX(), (int) rectangle.getY(), (int) rectangle.getWidth(), (int) rectangle.getHeight());
            }

            graphics.pop(2);
        }

        return new Result(bounds, firstRowHeight, lastLineWidth, lastLineBroken);
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

    private record Result(Rectangles bounds, int firstRowHeight, int lastLineWidth, boolean lastLineBroken) {
    }
}
