package dev.hephaestus.proximity.cards.layers;

import dev.hephaestus.proximity.cards.predicates.CardPredicate;
import dev.hephaestus.proximity.util.*;
import dev.hephaestus.proximity.xml.LayerRenderer;
import dev.hephaestus.proximity.xml.RenderableCard;

import java.awt.geom.Rectangle2D;
import java.util.*;

public class ForkLayerRenderer extends ParentLayerRenderer {
    @Override
    protected Result<Optional<Rectangles>> renderLayer(RenderableCard card, RenderableCard.XMLElement element, StatefulGraphics graphics, Rectangles wrap, boolean draw, Box<Float> scale, Rectangle2D bounds, List<Pair<RenderableCard.XMLElement, LayerRenderer>> children) {
        List<String> errors = new ArrayList<>();
        Map<String, List<CardPredicate>> branches = new LinkedHashMap<>();

        element.apply("branches", branchesElement -> {
            branchesElement.iterate((branch, i) -> {
                List<CardPredicate> predicates = new ArrayList<>();

                branch.iterate((predicate, j) -> XMLUtil.parsePredicate(predicate, card::getPredicate, card::exists)
                        .ifPresent(predicates::add)
                        .ifError(errors::add));

                branches.put(branch.getAttribute("id"), predicates);
            });
        });

        if (!errors.isEmpty()) {
            return Result.error("Error(s) while parsing Fork: \n\t%s", String.join("\n\t", errors));
        }

        String passingBranch = null;

        for (var branch : branches.entrySet()) {
            boolean p = true;

            for (var condition : branch.getValue()) {
                Result<Boolean> r = condition.test(card).ifError(errors::add);

                if (r.isOk() && !r.get()) {
                    p = false;
                }
            }

            if (p) {
                passingBranch = RenderableCard.XMLElement.id(element.getAttribute("id"), branch.getKey());
                break;
            }
        }

        if (!errors.isEmpty()) {
            return Result.error("Error(s) while parsing Fork: \n\t%s", String.join("\n\t", errors));
        }

        if (passingBranch == null) {
            return Result.of(Optional.empty());
        }

        Rectangles resultBounds = new Rectangles();

        element.setAttribute("id", passingBranch);

        for (var pair : children) {
            Result<Optional<Rectangles>> result = pair.right().render(card, pair.left(), graphics, wrap, draw, scale, bounds)
                    .ifError(errors::add);

            if (result.isOk() && result.get().isPresent()) {
                resultBounds.addAll(result.get().get());
            }
        }

        return !errors.isEmpty()
                ? Result.error("Error rendering children for layer %s:\n\t%s", element.getId(), String.join("\n\t", errors))
                : Result.of(Optional.of(resultBounds));
    }
}
