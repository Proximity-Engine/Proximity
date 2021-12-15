package dev.hephaestus.proximity.xml;

import dev.hephaestus.proximity.Proximity;
import dev.hephaestus.proximity.api.Values;
import dev.hephaestus.proximity.api.tasks.Effect;
import dev.hephaestus.proximity.templates.layers.renderers.EffectLayerRenderer;
import dev.hephaestus.proximity.templates.layers.renderers.LayerGroupRenderer;
import dev.hephaestus.proximity.cards.predicates.CardPredicate;
import dev.hephaestus.proximity.util.*;

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.*;

public abstract class LayerRenderer {
    protected final RenderableData data;

    protected LayerRenderer(RenderableData data) {
        this.data = data;
    }

    public boolean scales(RenderableData card, RenderableData.XMLElement element) {
        return false;
    }

    public abstract Result<Optional<Rectangles>> renderLayer(RenderableData card, RenderableData.XMLElement element, StatefulGraphics graphics, Rectangles wrap, boolean draw, Box<Float> scale, Rectangle2D bounds);

    public final Result<Optional<Rectangles>> render(RenderableData card, RenderableData.XMLElement element, StatefulGraphics graphics, Rectangles wrap, boolean draw, Box<Float> scale, Rectangle2D bounds) {
        float oldScale = scale.get();

        List<String> errors = new ArrayList<>();
        List<CardPredicate> predicates = new ArrayList<>();
        boolean render = true;

        element.apply("Conditions", conditions -> {
            conditions.iterate((condition, i) -> XMLUtil.parsePredicate(condition, card::getPredicate, card::exists)
                    .ifPresent(predicates::add)
                    .ifError(errors::add));
        });

        for (CardPredicate predicate : predicates) {
            Result<Boolean> r = predicate.test(card);

            if (r.isOk() && !r.get()) {
                render = false;
                break;
            }
        }

        if (!errors.isEmpty()) {
            return Result.error("Error(s) parsing predicates:\n\t%s", String.join("\n\t", errors));
        }

        if (!render) {
            return Result.of(Optional.empty());
        }

        if (draw && !element.getId().isEmpty()) {
            Proximity.LOG.debug("Rendering {}", element.getId());
        }

        Optional<Pair<RenderableData.XMLElement, LayerRenderer>> mask = element.apply("Mask", e -> {
            return new Pair<>(e, new LayerGroupRenderer(this.data));
        });

        Optional<Pair<RenderableData.XMLElement, LayerRenderer>> erase = element.apply("Erase", e -> {
            return new Pair<>(e, new LayerGroupRenderer(this.data));
        });

        Optional<Pair<RenderableData.XMLElement, LayerRenderer>> coloration = element.apply("Color", e -> {
            return new Pair<>(e, new LayerGroupRenderer(this.data));
        });

        List<Pair<RenderableData.XMLElement, Effect>> effects = new ArrayList<>();

        element.iterate("Effects", (efs, i) -> efs.iterate("Effect", (e, j) -> {
            List<CardPredicate> effectPredicates = new ArrayList<>();

            element.apply("Conditions", conditions -> {
                conditions.iterate((condition, k) -> XMLUtil.parsePredicate(condition, card::getPredicate, card::exists)
                        .ifPresent(effectPredicates::add)
                        .ifError(errors::add));
            });

            for (CardPredicate predicate : effectPredicates) {
                Result<Boolean> r = predicate.test(card);

                if (r.isOk() && !r.get()) {
                    return;
                }
            }

            if (!errors.isEmpty()) {
                throw new RuntimeException(String.format("Error(s) parsing predicates:\n\t%s", String.join("\n\t", errors)));
            }

            Effect effect = card.getTaskHandler().getTask(Effect.DEFINITION, e.getAttribute("name"));

            if (effect != null) {
                effects.add(new Pair<>(e, effect));
            }
        }));

        Result<Optional<Rectangles>> result;

        if (mask.isPresent() || erase.isPresent() || coloration.isPresent() || !effects.isEmpty()) {
            int width = graphics.getImage().getWidth(), height = graphics.getImage().getHeight();

            BufferedImage maskImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Result<Optional<Rectangles>> maskResult = mask.isPresent() ? mask.get().right().render(card, mask.get().left(), new StatefulGraphics(maskImage), wrap, draw,scale, bounds) : Result.of(Optional.empty());

            if (maskResult.isError()) return maskResult;

            BufferedImage eraseImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Result<Optional<Rectangles>> eraseResult = erase.isPresent() ? erase.get().right().render(card, erase.get().left(), new StatefulGraphics(eraseImage), wrap, draw,scale, bounds) : Result.of(Optional.empty());

            if (eraseResult.isError()) return eraseResult;

            BufferedImage colorationImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Result<Optional<Rectangles>> colorationResult = coloration.isPresent() ? coloration.get().right().render(card, coloration.get().left(), new StatefulGraphics(colorationImage), wrap, draw,scale, bounds) : Result.of(Optional.empty());

            if (colorationResult.isError()) return colorationResult;

            BufferedImage layerImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            StatefulGraphics layerGraphics = new StatefulGraphics(layerImage);

            if (this instanceof EffectLayerRenderer) {
                layerGraphics.drawImage(graphics.getImage(), null, null);
            }

            Result<Optional<Rectangles>> layerResult = this.renderLayer(card, element, new StatefulGraphics(layerImage), wrap, draw,scale, bounds);

            if (layerResult.isError()) return layerResult;

            if (mask.isPresent() || erase.isPresent() || coloration.isPresent()) {
                int[] layer = layerImage.getRGB(0, 0, width, height, null, 0, width);
                int[] masks = mask.isPresent() ? maskImage.getRGB(0, 0, width, height, null, 0, width) : null;
                int[] erasure = erase.isPresent() ? eraseImage.getRGB(0, 0, width, height, null, 0, width) : null;
                int[] colorations = coloration.isPresent() ? colorationImage.getRGB(0, 0, width, height, null, 0, width) : null;
                float[] hsb1 = new float[3];
                float[] hsb2 = new float[3];

                for (int i = 0; i < layer.length; i++) {
                    int color = layer[i];

                    if (colorations != null) {
                        int r = (color >> 16) & 0xFF;
                        int g = (color >> 8) & 0xFF;
                        int b = color & 0xFF;

                        Color.RGBtoHSB(r, g, b, hsb1);

                        int c = colorations[i];

                        r = (c >> 16) & 0xFF;
                        g = (c >> 8) & 0xFF;
                        b = c & 0xFF;

                        Color.RGBtoHSB(r, g, b, hsb2);

                        color = Color.HSBtoRGB(hsb2[0], hsb1[1], hsb1[2]);
                    }

                    color &= 0x00FFFFFF;

                    int originalAlpha = layer[i] >>> 24;
                    int alphaModifier = ((masks == null ? 0xFF000000 : masks[i]) >>> 24) - (erasure == null ? 0 : (erasure[i] >>> 24));
                    int alpha = Math.round(255 * ((originalAlpha / 255F) * (alphaModifier / 255F))) << 24;

                    layer[i] = color | alpha;
                }

                layerImage.setRGB(0, 0, width, height, layer, 0, width);
            }

            for (var pair : effects) {
                pair.right().apply(card, layerImage, pair.left());
            }

            graphics.drawImage(layerImage, null, null);

            result = maskResult;
        } else {
            result = this.renderLayer(card, element, graphics, wrap, draw, scale, bounds);
        }

        if (draw && result.isOk() && result.get().isPresent() && Values.DEBUG.get(card)) {
            if (result.get().get().isEmpty()) {
                Proximity.LOG.debug("Rendered {}: EMPTY", element.getId());
            } else {
                Rectangle2D resultBounds = result.get().get().getBounds();
                Proximity.LOG.debug("Rendered {}: [x={}, y={}, width={}, height={}]",
                        element.getId(), resultBounds.getX(), resultBounds.getY(), resultBounds.getWidth(), resultBounds.getHeight());
            }
        }

        scale.set(oldScale);

        return result;

    }

    public interface Factory<T extends LayerRenderer> {
        T create(RenderableData card);
    }
}
