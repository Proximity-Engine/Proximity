package dev.hephaestus.proximity.templates.layers.factories;

import dev.hephaestus.proximity.cards.predicates.CardPredicate;
import dev.hephaestus.proximity.json.JsonObject;
import dev.hephaestus.proximity.templates.LayerFactory;
import dev.hephaestus.proximity.templates.layers.Layer;
import dev.hephaestus.proximity.templates.layers.Layout;
import dev.hephaestus.proximity.util.ContentAlignment;
import dev.hephaestus.proximity.util.Result;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class LayoutFactory extends LayerFactory<Layout> {
    private final Integer width, height;
    private final List<LayerFactory<?>> factories;
    private final ContentAlignment alignment;
    private final BiConsumer<Layer, Integer> inLineSetter;
    private final BiConsumer<Layer, Integer> offLineSetter;
    private final Function<Layer, Integer> inLineGetter;
    private final Function<Layer, Integer> offLineGetter;
    private final Function<Rectangle, Double> inLineSizeGetter;
    private final Function<Rectangle, Double> offLineSizeGetter;

    public LayoutFactory(String id, int x, int y, List<CardPredicate> predicates, Integer width, Integer height, List<LayerFactory<?>> factories, ContentAlignment alignment, BiConsumer<Layer, Integer> inLineSetter, BiConsumer<Layer, Integer> offLineSetter, Function<Layer, Integer> inLineGetter, Function<Layer, Integer> offLineGetter, Function<Rectangle, Double> inLineSizeGetter, Function<Rectangle, Double> offLineSizeGetter) {
        super(id, x, y, predicates);
        this.width = width;
        this.height = height;
        this.factories = new ArrayList<>(factories);
        this.alignment = alignment;
        this.inLineSetter = inLineSetter;
        this.offLineSetter = offLineSetter;
        this.inLineGetter = inLineGetter;
        this.offLineGetter = offLineGetter;
        this.inLineSizeGetter = inLineSizeGetter;
        this.offLineSizeGetter = offLineSizeGetter;
    }

    @Override
    public Result<Layout> createLayer(String parentId, JsonObject card) {
        List<Layer> layers = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (LayerFactory<?> factory : this.factories) {
            factory.create(Layer.id(parentId, this.id), card)
                    .ifError(errors::add)
                    .ifPresent(layers::add);
        }

        if (errors.isEmpty()) {
            return Result.of(new Layout(parentId, this.id, this.x, this.y, layers, this.width, this.height, this.alignment, this.inLineSetter, this.offLineSetter, this.inLineGetter, this.offLineGetter, this.inLineSizeGetter, this.offLineSizeGetter));
        } else {
            return Result.error("Error creating child factories for layer %s:\n\t%s", Layer.id(parentId, this.id), String.join("\n\t%s", errors));
        }
    }
}
