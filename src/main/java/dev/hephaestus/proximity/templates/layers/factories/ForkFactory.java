package dev.hephaestus.proximity.templates.layers.factories;

import dev.hephaestus.proximity.cards.predicates.CardPredicate;
import dev.hephaestus.proximity.json.JsonObject;
import dev.hephaestus.proximity.templates.LayerFactory;
import dev.hephaestus.proximity.templates.layers.Group;
import dev.hephaestus.proximity.templates.layers.Layer;
import dev.hephaestus.proximity.util.Result;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ForkFactory extends LayerFactory<Group> {
    private final List<LayerFactory<?>> factories;
    private final Map<String, List<CardPredicate>> branches;

    public ForkFactory(String id, int x, int y, List<CardPredicate> predicates, List<LayerFactory<?>> factories, Map<String, List<CardPredicate>> branches) {
        super(id, x, y, predicates);
        this.factories = new ArrayList<>(factories);
        this.branches = new LinkedHashMap<>(branches);
    }

    @Override
    public Result<Group> createLayer(String parentId, JsonObject card) {
        String branch = null;

        for (var entry : this.branches.entrySet()) {
            boolean pass = true;

            for (CardPredicate p : entry.getValue()) {
                Result<Boolean> r = p.test(card);

                pass &= (!r.isError()) && r.get();
            }

            if (pass) {
                branch = entry.getKey();
                break;
            }
        }

        if (branch == null) {
            return Result.error("No branches of Fork '%s' match card '%s'", Layer.id(parentId, this.id), card.getAsString("name"));
        }

        List<Layer> layers = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        String id = this.id == null || this.id.isEmpty()
                ? branch
                : this.id + "." + branch;

        for (LayerFactory<?> factory : this.factories) {
            factory.create(Layer.id(parentId, id), card)
                    .ifError(errors::add)
                    .ifPresent(layers::add);
        }

        if (errors.isEmpty()) {
            return Result.of(new Group(parentId, id, this.x, this.y, layers));
        } else {
            return Result.error("Error creating child factories for layer %s:\n\t%s", Layer.id(parentId, id), String.join("\n\t%s", errors));
        }
    }
}
