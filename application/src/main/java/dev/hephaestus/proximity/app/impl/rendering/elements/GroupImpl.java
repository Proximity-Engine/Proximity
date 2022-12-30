package dev.hephaestus.proximity.app.impl.rendering.elements;

import dev.hephaestus.proximity.app.api.rendering.Document;
import dev.hephaestus.proximity.app.api.rendering.RenderData;
import dev.hephaestus.proximity.app.api.rendering.elements.Element;
import dev.hephaestus.proximity.app.api.rendering.elements.Group;
import dev.hephaestus.proximity.app.api.rendering.elements.Selector;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

public class GroupImpl<D extends RenderData> extends ParentImpl<D> implements Group {
    public GroupImpl(String id, Document<D> document, ParentImpl<D> parent) {
        super(id, document, parent);
    }

    @Override
    public Node render() {
        StackPane pane = new StackPane();

        pane.setAlignment(Pos.TOP_LEFT);

        for (var child : this.children) {
            Node node = child.render();

            if (!(child instanceof Selector)) {
                // Selectors are always visible
                // Their visibility is inherited by their children
                node.visibleProperty().bind(child.visibility);
            }

            pane.getChildren().add(node);
        }

        return pane;
    }

    @NotNull
    @Override
    public Iterator<Element> iterator() {
        return new Iterator<>() {
            final Iterator<ElementImpl<D>> itr = GroupImpl.this.children.iterator();

            @Override
            public boolean hasNext() {
                return this.itr.hasNext();
            }

            @Override
            public Element next() {
                return this.itr.next();
            }
        };
    }
}
