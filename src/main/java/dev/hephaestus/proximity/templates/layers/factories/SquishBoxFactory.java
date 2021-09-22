package dev.hephaestus.proximity.templates.layers.factories;

import dev.hephaestus.proximity.cards.predicates.CardPredicate;
import dev.hephaestus.proximity.json.JsonObject;
import dev.hephaestus.proximity.templates.LayerFactory;
import dev.hephaestus.proximity.templates.layers.Layer;
import dev.hephaestus.proximity.templates.layers.SquishBoxLayer;
import dev.hephaestus.proximity.util.Result;

import java.util.List;

public class SquishBoxFactory extends LayerFactory<SquishBoxLayer> {
    private final LayerFactory<?> main;
    private final LayerFactory<?> flex;

    public SquishBoxFactory(String id, int x, int y, List<CardPredicate> predicates, LayerFactory<?> main, LayerFactory<?> flex) {
        super(id, x, y, predicates);
        this.main = main;
        this.flex = flex;
    }

    @Override
    public Result<SquishBoxLayer> createLayer(String parentId, JsonObject card) {
        Result<? extends Layer> mainResult = this.main.create(this.id, card);
        Result<? extends Layer> flexResult = this.flex.create(this.id, card);

        if (mainResult.isError() ^ flexResult.isError()) {
            return Result.error((mainResult.isError() ? mainResult : flexResult).getError());
        } else if (mainResult.isError()) {
            return Result.error("Error creating factories for layer %s:\n\tmain: %s\n\tflex: %s",
                    Layer.id(parentId, this.id), mainResult.getError(), flexResult.getError()
            );
        }

        return Result.of(new SquishBoxLayer(parentId, this.id, this.x, this.y,
                mainResult.get(),
                flexResult.get()
        ));
    }
}
