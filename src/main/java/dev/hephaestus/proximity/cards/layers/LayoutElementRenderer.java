package dev.hephaestus.proximity.cards.layers;

import dev.hephaestus.proximity.util.*;
import dev.hephaestus.proximity.xml.LayerProperty;
import dev.hephaestus.proximity.xml.LayerRenderer;
import dev.hephaestus.proximity.xml.RenderableCard;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

public class LayoutElementRenderer extends ParentLayerRenderer {
    private final String inLine, offLine;
    private final Function<Rectangle2D, Double> inLineSizeGetter;
    private final Function<Rectangle2D, Double> offLineSizeGetter;

    public LayoutElementRenderer(String inLine, String offLine, Function<Rectangle2D, Double> inLineSizeGetter, Function<Rectangle2D, Double> offLineSizeGetter) {
        this.inLine = inLine;
        this.offLine = offLine;
        this.inLineSizeGetter = inLineSizeGetter;
        this.offLineSizeGetter = offLineSizeGetter;
    }

    @Override
    protected Result<Optional<Rectangle2D>> renderLayer(RenderableCard card, RenderableCard.XMLElement element, StatefulGraphics graphics, Rectangle2D wrap, boolean draw, float scale, Rectangle2D bounds, List<Pair<RenderableCard.XMLElement, LayerRenderer>> children) {
        int x = (element.hasAttribute("x") ? Integer.decode(element.getAttribute("x")) : 0);
        int y = (element.hasAttribute("y") ? Integer.decode(element.getAttribute("y")) : 0);
        Integer width = element.hasAttribute("width") ? Integer.decode(element.getAttribute("width")) : null;
        Integer height = element.hasAttribute("height") ? Integer.decode(element.getAttribute("height")) : null;
        ContentAlignment alignment = element.hasAttribute("alignment") ? ContentAlignment.valueOf(element.getAttribute("alignment").toUpperCase(Locale.ROOT)) : ContentAlignment.START;
        Rectangle2D outerBounds = width == null || height == null ? null : new Rectangle2D.Float(x, y, width, height);
        wrap = element.getProperty(LayerProperty.WRAP);

        List<String> errors = new ArrayList<>();

        Rectangle2D resultBounds;

        int tries = 100;
        int inLine = element.getInteger(this.inLine);

        do {
            --tries;

            resultBounds = render(card, graphics, wrap, false, scale, children, inLine, element.getInteger(this.offLine), outerBounds, errors);

            if (resultBounds != null && outerBounds != null) {
                inLine = (int) (element.getInteger(this.inLine) + switch (alignment) {
                    case START -> 0;
                    case MIDDLE -> ((this.inLineSizeGetter.apply(outerBounds) - this.inLineSizeGetter.apply(resultBounds)) / 2);
                    case END -> (this.inLineSizeGetter.apply(outerBounds) - this.inLineSizeGetter.apply(resultBounds));
                });

                resultBounds.setRect(resultBounds.getX(), inLine, resultBounds.getWidth(), resultBounds.getHeight());
            }

            if (resultBounds != null && ((outerBounds != null && !outerBounds.contains(resultBounds)) || wrap != null && resultBounds.intersects(wrap))) {
                scale -= 0.25;
                resultBounds = null;
            } else {
                resultBounds = render(card, graphics, wrap, draw, scale, children, inLine, element.getInteger(this.offLine), outerBounds, errors);
            }
        } while (resultBounds == null && tries > 0);

        if (!errors.isEmpty()) {
            return Result.error("Error creating child factories for layer %s:\n\t%s", element.getId(), String.join("\n\t", errors));
        }

        return Result.of(Optional.ofNullable(resultBounds));
    }

    private Rectangle2D render(RenderableCard card, StatefulGraphics graphics, Rectangle2D wrap, boolean draw, float scale, List<Pair<RenderableCard.XMLElement, LayerRenderer>> children, int inLine, int offLine, Rectangle2D outerBounds, List<String> errors) {
        int dInLine = inLine;
        Rectangle2D resultBounds = null;

        for (var pair : children) {
            RenderableCard.XMLElement e = pair.left();
            e.pushAttribute(this.inLine, dInLine);

            if (outerBounds != null) {
                e.pushAttribute(this.offLine, offLine);
                pair.left().setProperty(LayerProperty.BOUNDS, new Rectangle2D.Double(
                        offLine,
                        inLine,
                        this.offLineSizeGetter.apply(outerBounds).intValue(),
                        (int) (this.inLineSizeGetter.apply(outerBounds) - (dInLine - inLine)))
                );
            }

            Result<Optional<Rectangle2D>> result = pair.right().render(card, e, graphics, wrap, draw, scale, outerBounds);

            if (outerBounds != null) {
                e.popAttribute();
            }

            e.popAttribute();

            if (result.isError()) {
                errors.add(result.getError());
            } else if (errors.isEmpty() && result.get().isPresent()) {
                Rectangle2D layerBounds = result.get().get();

                resultBounds = resultBounds == null
                        ? this.offLineSizeGetter.apply(layerBounds) > 0 && this.inLineSizeGetter.apply(layerBounds) > 0
                        ? layerBounds
                        : null
                        : this.offLineSizeGetter.apply(layerBounds) > 0 && this.inLineSizeGetter.apply(layerBounds) > 0
                        ? DrawingUtil.encompassing(resultBounds, layerBounds)
                        : resultBounds;

                dInLine += this.inLineSizeGetter.apply(layerBounds);
            }
        }

        if (draw && outerBounds != null && card.getAsJsonObject(Keys.OPTIONS).getAsBoolean("debug")) {
            graphics.push(new BasicStroke(5), Graphics2D::setStroke, Graphics2D::getStroke);
            graphics.push(DrawingUtil.getColor(0xFF00FFFF), Graphics2D::setColor, Graphics2D::getColor);
            graphics.drawRect((int) outerBounds.getX(), (int) outerBounds.getY(), (int) outerBounds.getWidth(), (int) outerBounds.getHeight());
            graphics.pop(2);
        }

        return resultBounds;
    }
}
