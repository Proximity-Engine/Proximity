package dev.hephaestus.proximity.app.impl.rendering.elements;

import dev.hephaestus.proximity.app.api.Option;
import dev.hephaestus.proximity.app.api.rendering.Document;
import dev.hephaestus.proximity.app.api.rendering.RenderData;
import dev.hephaestus.proximity.app.api.rendering.elements.Element;
import dev.hephaestus.proximity.app.api.rendering.elements.Selector;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ListProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Group;
import javafx.scene.Node;

import java.util.Optional;

public class SelectorImpl<D extends RenderData> extends ParentImpl<D> implements Selector {
    private final Property<Element> selected = new SimpleObjectProperty<>();

    public SelectorImpl(String id, Document<D> document, ParentImpl<D> parent) {
        super(id, document, parent);

        this.selected.bind(Bindings.createObjectBinding(() -> {
            return this.getSelected().orElse(null);
        }, this.children));
    }

    @Override
    public Node render() {
        Group group = new Group();

        this.render(group);
        this.selected.addListener(o -> this.render(group));

        return group;
    }

    private void render(Group group) {
        var e = this.selected.getValue();

        if (e != null) {
            group.getChildren().setAll(((ElementImpl<D>) e).render());
        }
    }

    @Override
    public Optional<Element> getSelected() {
        for (var child : this.children) {
            if (child.isVisible()) return Optional.of(child);
        }

        return Optional.empty();
    }

    public ListProperty<ElementImpl<D>> children() {
        return this.children;
    }

    public void setOption(Option<ElementImpl<D>, ?, D> option) {
        this.selected.bind(this.document.getRenderData().getOption(option));
    }
}
