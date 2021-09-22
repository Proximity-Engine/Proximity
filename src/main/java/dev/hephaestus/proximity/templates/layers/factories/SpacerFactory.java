package dev.hephaestus.proximity.templates.layers.factories;

import dev.hephaestus.proximity.cards.predicates.CardPredicate;
import dev.hephaestus.proximity.json.JsonObject;
import dev.hephaestus.proximity.templates.LayerFactory;
import dev.hephaestus.proximity.templates.layers.SpacerLayer;
import dev.hephaestus.proximity.util.Result;

import java.util.List;

public class SpacerFactory extends LayerFactory<SpacerLayer> {
    private final int width, height;

    public SpacerFactory(String id, int x, int y, List<CardPredicate> predicates, int width, int height) {
        super(id, x, y, predicates);
        this.width = width;
        this.height = height;
    }

    @Override
    public Result<SpacerLayer> createLayer(String parentId, JsonObject card) {
        return Result.of(new SpacerLayer(parentId, this.id, this.x, this.y, this.width, this.height));
    }
}
