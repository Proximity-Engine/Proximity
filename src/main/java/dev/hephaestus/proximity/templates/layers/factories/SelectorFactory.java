package dev.hephaestus.proximity.templates.layers.factories;

import dev.hephaestus.proximity.cards.predicates.CardPredicate;
import dev.hephaestus.proximity.json.JsonObject;
import dev.hephaestus.proximity.templates.LayerFactory;
import dev.hephaestus.proximity.templates.layers.Layer;
import dev.hephaestus.proximity.util.Result;

import java.util.ArrayList;
import java.util.List;

public class SelectorFactory extends LayerFactory<Layer> {
    private final List<LayerFactory<?>> factories;

    public SelectorFactory(String id, int x, int y, List<CardPredicate> predicates, List<LayerFactory<?>> factories) {
        super(id, x, y, predicates);

        this.factories = new ArrayList<>(factories);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Result<Layer> createLayer(String parentId, JsonObject card) {
        for (LayerFactory<?> factory : this.factories) {
            Result layer = factory.create(Layer.id(parentId, this.id), card);

            if (!layer.isError() && layer.get() != Layer.EMPTY) {
                return layer;
            }
        }

        return Result.of(Layer.EMPTY);
    }
}
