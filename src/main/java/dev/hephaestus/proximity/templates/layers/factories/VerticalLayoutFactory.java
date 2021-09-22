package dev.hephaestus.proximity.templates.layers.factories;

import dev.hephaestus.proximity.cards.predicates.CardPredicate;
import dev.hephaestus.proximity.json.JsonObject;
import dev.hephaestus.proximity.templates.LayerFactory;
import dev.hephaestus.proximity.templates.layers.HorizontalLayout;
import dev.hephaestus.proximity.templates.layers.Layer;
import dev.hephaestus.proximity.templates.layers.VerticalLayout;
import dev.hephaestus.proximity.util.Result;

import java.util.ArrayList;
import java.util.List;

public class VerticalLayoutFactory extends LayerFactory<VerticalLayout> {
    private final List<LayerFactory<?>> factories;

    public VerticalLayoutFactory(String id, int x, int y, List<CardPredicate> predicates, List<LayerFactory<?>> factories) {
        super(id, x, y, predicates);
        this.factories = new ArrayList<>(factories);
    }

    @Override
    public Result<VerticalLayout> createLayer(String parentId, JsonObject card) {
        List<Layer> layers = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (LayerFactory<?> factory : this.factories) {
            factory.create(Layer.id(parentId, this.id), card)
                    .ifError(errors::add)
                    .ifPresent(layers::add);
        }

        if (errors.isEmpty()) {
            return Result.of(new VerticalLayout(parentId, this.id, this.x, this.y, layers));
        } else {
            return Result.error("Error creating child factories for layer %s:\n\t%s", Layer.id(parentId, this.id), String.join("\n\t%s", errors));
        }
    }
}
