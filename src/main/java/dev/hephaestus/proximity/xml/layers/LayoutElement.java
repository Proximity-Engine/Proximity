package dev.hephaestus.proximity.xml.layers;

import dev.hephaestus.proximity.json.JsonObject;
import dev.hephaestus.proximity.templates.Template;
import dev.hephaestus.proximity.templates.layers.Layer;
import dev.hephaestus.proximity.templates.layers.Layout;
import dev.hephaestus.proximity.util.ContentAlignment;
import dev.hephaestus.proximity.util.Result;
import org.w3c.dom.Element;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class LayoutElement extends ParentElement<Layout> {
    private final BiConsumer<Layer, Integer> inLineSetter;
    private final BiConsumer<Layer, Integer> offLineSetter;
    private final Function<Layer, Integer> inLineGetter;
    private final Function<Layer, Integer> offLineGetter;
    private final Function<Rectangle, Double> inLineSizeGetter;
    private final Function<Rectangle, Double> offLineSizeGetter;

    private Integer width, height;
    private ContentAlignment alignment;

    public LayoutElement(Element element, BiConsumer<Layer, Integer> inLineSetter, BiConsumer<Layer, Integer> offLineSetter, Function<Layer, Integer> inLineGetter, Function<Layer, Integer> offLineGetter, Function<Rectangle, Double> inLineSizeGetter, Function<Rectangle, Double> offLineSizeGetter) {
        super(element);
        this.inLineSetter = inLineSetter;
        this.offLineSetter = offLineSetter;
        this.inLineGetter = inLineGetter;
        this.offLineGetter = offLineGetter;
        this.inLineSizeGetter = inLineSizeGetter;
        this.offLineSizeGetter = offLineSizeGetter;
    }

    @Override
    protected Result<LayerElement<Layout>> createLayerFactory(Template template) {
        this.width = this.element.hasAttribute("width") ? Integer.decode(this.element.getAttribute("width")) : null;
        this.height = this.element.hasAttribute("height") ? Integer.decode(this.element.getAttribute("height")) : null;
        this.alignment = this.element.hasAttribute("alignment") ? ContentAlignment.valueOf(this.element.getAttribute("alignment").toUpperCase(Locale.ROOT)) : ContentAlignment.START;

        return Result.of(this);
    }

    @Override
    public Result<Layout> createLayer(String parentId, JsonObject card) {
        java.util.List<Layer> layers = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (LayerElement<?> factory : this.getChildren()) {
            factory.create(Layer.id(parentId, this.getId()), card)
                    .ifError(errors::add)
                    .ifPresent(layers::add);
        }

        if (errors.isEmpty()) {
            return Result.of(new Layout(parentId, this.getId(), this.getX(), this.getY(), layers, this.width, this.height, this.alignment, this.inLineSetter, this.offLineSetter, this.inLineGetter, this.offLineGetter, this.inLineSizeGetter, this.offLineSizeGetter));
        } else {
            return Result.error("Error creating child factories for layer %s:\n\t%s", Layer.id(parentId, this.getId()), String.join("\n\t%s", errors));
        }
    }
}
