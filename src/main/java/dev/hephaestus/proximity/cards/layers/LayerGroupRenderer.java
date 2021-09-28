package dev.hephaestus.proximity.cards.layers;

import dev.hephaestus.proximity.util.DrawingUtil;
import dev.hephaestus.proximity.util.Pair;
import dev.hephaestus.proximity.util.Result;
import dev.hephaestus.proximity.util.StatefulGraphics;
import dev.hephaestus.proximity.xml.LayerRenderer;
import dev.hephaestus.proximity.xml.RenderableCard;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LayerGroupRenderer extends ParentLayerRenderer {
    @Override
    protected Result<Optional<Rectangle2D>> renderLayer(RenderableCard card, RenderableCard.XMLElement element, StatefulGraphics graphics, Rectangle2D wrap, boolean draw, float scale, Rectangle2D bounds, List<Pair<RenderableCard.XMLElement, LayerRenderer>> children) {
        List<String> errors = new ArrayList<>();
        Rectangle2D resultBounds = null;

        for (var pair : children) {
            Result<Optional<Rectangle2D>> result = pair.right().render(card, pair.left(), graphics, wrap, draw, scale, bounds)
                    .ifError(errors::add);

            if (result.isOk() && result.get().isPresent()) {
                resultBounds = resultBounds != null
                        ? DrawingUtil.encompassing(resultBounds, result.get().get())
                        : result.get().get();
            }
        }

        return !errors.isEmpty()
                ? Result.error("Error(s) rendering children for layer %s:\n\t%s", element.getId(), String.join("\n\t", errors))
                : Result.of(Optional.ofNullable(resultBounds));
    }
}
