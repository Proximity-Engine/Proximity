package dev.hephaestus.proximity.xml.layers;

import dev.hephaestus.proximity.json.JsonObject;
import dev.hephaestus.proximity.templates.Template;
import dev.hephaestus.proximity.templates.layers.Group;
import dev.hephaestus.proximity.templates.layers.Layer;
import dev.hephaestus.proximity.util.Result;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;

public class GroupElement extends ParentElement<Group> {
    public GroupElement(Element element) {
        super(element);
    }

    @Override
    protected Result<LayerElement<Group>> createLayerFactory(Template template) {
        return Result.of(this);
    }

    @Override
    public Result<Group> createLayer(String parentId, JsonObject card) {
        List<Layer> layers = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (LayerElement<?> factory : this.getChildren()) {
            factory.create(Layer.id(parentId, this.getId()), card)
                    .ifError(errors::add)
                    .ifPresent(layers::add);
        }

        if (errors.isEmpty()) {
            return Result.of(new Group(parentId, this.getId(), this.getX(), this.getY(), layers));
        } else {
            return Result.error("Error creating child factories for layer %s:\n\t%s", Layer.id(parentId, this.getId()), String.join("\n\t%s", errors));
        }
    }
}
