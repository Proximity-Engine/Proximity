package dev.hephaestus.proximity.cards.layers;

import dev.hephaestus.proximity.util.*;
import dev.hephaestus.proximity.xml.LayerProperty;
import dev.hephaestus.proximity.xml.LayerRenderer;
import dev.hephaestus.proximity.xml.RenderableCard;
import org.jetbrains.annotations.Nullable;

import java.awt.geom.Rectangle2D;
import java.util.*;
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

        List<String> errors = new ArrayList<>();

        Rectangle2D resultBounds = null;

        int i = 0;
        int inLine = element.getInteger(this.inLine);

        Map<RenderableCard.XMLElement, Rectangle2D> drawnBounds = new WeakHashMap<>();

        do {
            for (var pair : children) {
                RenderableCard.XMLElement e = pair.left();
                e.pushAttribute(this.inLine, inLine);

                if (outerBounds != null) {
                    e.pushAttribute(this.offLine, element.getInteger(this.offLine));
                    pair.left().setProperty(LayerProperty.BOUNDS, new Rectangle2D.Double(
                            element.getInteger(this.offLine),
                            inLine,
                            this.offLineSizeGetter.apply(outerBounds).intValue(),
                            (int) (this.inLineSizeGetter.apply(outerBounds) - (inLine - element.getInteger(this.inLine))))
                    );
                }

                Result<Optional<Rectangle2D>> result = pair.right().render(card, e, graphics, wrap, false, scale, outerBounds);

                if (outerBounds != null) {
                    e.popAttribute();
                }

                e.popAttribute();

                if (result.isError()) {
                    errors.add(result.getError());
                } else if (errors.isEmpty() && result.get().isPresent()) {
                    Rectangle2D layerBounds = result.get().get();
                    drawnBounds.put(e, layerBounds);

                    resultBounds = resultBounds == null
                            ? this.offLineSizeGetter.apply(layerBounds) > 0 && this.inLineSizeGetter.apply(layerBounds) > 0
                                ? layerBounds
                                : null
                            : this.offLineSizeGetter.apply(layerBounds) > 0 && this.inLineSizeGetter.apply(layerBounds) > 0
                                ? DrawingUtil.encompassing(resultBounds, layerBounds)
                                : resultBounds;

                    inLine += this.inLineSizeGetter.apply(layerBounds);
                }
            }

            if (resultBounds != null && outerBounds != null && (this.offLineSizeGetter.apply(resultBounds) > this.offLineSizeGetter.apply(outerBounds) || this.inLineSizeGetter.apply(resultBounds) > this.inLineSizeGetter.apply(outerBounds))) {
                scale -= 0.25;
                resultBounds = null;
                inLine = element.getInteger(this.inLine);
            }

            ++i;
        } while (resultBounds == null && i < 100);

        resultBounds = render(card, element, graphics, wrap, false, scale, children, alignment, outerBounds, errors, resultBounds, drawnBounds);
        resultBounds = render(card, element, graphics, wrap, draw, scale, children, alignment, outerBounds, errors, resultBounds, drawnBounds);

        if (!errors.isEmpty()) {
            return Result.error("Error creating child factories for layer %s:\n\t%s", element.getId(), String.join("\n\t", errors));
        }

        return Result.of(Optional.ofNullable(resultBounds));
    }

    @Nullable
    private Rectangle2D render(RenderableCard card, RenderableCard.XMLElement element, StatefulGraphics graphics, Rectangle2D wrap, boolean draw, float scale, List<Pair<RenderableCard.XMLElement, LayerRenderer>> children, ContentAlignment alignment, Rectangle2D outerBounds, List<String> errors, Rectangle2D resultBounds, Map<RenderableCard.XMLElement, Rectangle2D> drawnBounds) {
        int d = 0;

        if (outerBounds != null) {
            d = switch (alignment) {
                case START -> 0;
                case MIDDLE -> resultBounds == null ? 0 : (this.inLineSizeGetter.apply(outerBounds).intValue() - this.inLineSizeGetter.apply(resultBounds).intValue()) / 2;
                case END -> resultBounds == null ? 0 : this.inLineSizeGetter.apply(outerBounds).intValue() - this.inLineSizeGetter.apply(resultBounds).intValue();
            };
        }

        resultBounds = null;

        for (var pair : children) {
            RenderableCard.XMLElement e = pair.left();
            e.pushAttribute(this.inLine, (int) (e.getInteger(this.inLine) + (resultBounds == null ? 0 : this.inLineSizeGetter.apply(resultBounds)) + d));

            Rectangle2D layerBounds = null;

            if (outerBounds != null && drawnBounds.get(pair.left()) != null) {
                layerBounds = new Rectangle2D.Double(
                        element.getInteger(this.offLine),
                        e.getInteger(this.inLine) + d,
                        this.offLineSizeGetter.apply(outerBounds),
                        this.inLineSizeGetter.apply(drawnBounds.get(pair.left()))
                );
            }

            Result<Optional<Rectangle2D>> result = pair.right().render(card, pair.left(), graphics, wrap, draw, scale, layerBounds);

            e.popAttribute();

            if (result.isOk() && result.get().isPresent() && errors.isEmpty()) {
                layerBounds = result.get().get();

                resultBounds = resultBounds == null
                        ? this.offLineSizeGetter.apply(layerBounds) > 0 && this.inLineSizeGetter.apply(layerBounds) > 0
                        ? layerBounds : null
                        : this.offLineSizeGetter.apply(layerBounds) > 0 && this.inLineSizeGetter.apply(layerBounds) > 0
                        ? DrawingUtil.encompassing(resultBounds, layerBounds) : resultBounds;

            } else if (result.isError()) {
                errors.add(result.getError());
            }
        }
        return resultBounds;
    }


}
