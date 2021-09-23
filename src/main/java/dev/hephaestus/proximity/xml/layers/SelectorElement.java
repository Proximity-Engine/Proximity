package dev.hephaestus.proximity.xml.layers;

import dev.hephaestus.proximity.json.JsonObject;
import dev.hephaestus.proximity.templates.Template;
import dev.hephaestus.proximity.templates.layers.Layer;
import dev.hephaestus.proximity.util.Result;
import org.w3c.dom.Element;

public class SelectorElement extends ParentElement<Layer> {
    public SelectorElement(Element element) {
        super(element);
    }

    @Override
    protected Result<LayerElement<Layer>> createLayerFactory(Template template) {
        return Result.of(this);
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public Result<Layer> createLayer(String parentId, JsonObject card) {
        for (LayerElement<?> element : this.getChildren()) {
            Result layer = element.create(Layer.id(parentId, this.getId()), card);

            if (layer.isError()) {
                return Result.error(layer.getError());
            } else if (layer.get() != Layer.EMPTY) {
                return layer;
            }
        }

        return Result.of(Layer.EMPTY);
    }
}
