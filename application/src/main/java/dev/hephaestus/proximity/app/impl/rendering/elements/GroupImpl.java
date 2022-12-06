package dev.hephaestus.proximity.app.impl.rendering.elements;

import dev.hephaestus.proximity.app.api.RenderJob;
import dev.hephaestus.proximity.app.api.rendering.elements.Element;
import dev.hephaestus.proximity.app.api.rendering.elements.Group;
import dev.hephaestus.proximity.app.api.rendering.util.BoundingBox;
import dev.hephaestus.proximity.app.api.rendering.util.BoundingBoxes;
import dev.hephaestus.proximity.app.impl.rendering.DocumentImpl;
import dev.hephaestus.proximity.utils.UnmodifiableIterator;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GroupImpl<D extends RenderJob> extends ElementImpl<D> implements ParentImpl<D>, Group<D> {
    private final List<ElementImpl<D>> children = new ArrayList<>(3);
    private final VisibilityProperty<Group<D>> visibility;

    public GroupImpl(DocumentImpl<D> document, String id, ElementImpl<D> parent) {
        super(document, id, parent);

        this.visibility = new VisibilityProperty<Group<D>>(this, document.getData());
    }

    @Override
    public BoundingBoxes getBounds() {
        ArrayList<BoundingBox> list = new ArrayList<>(this.children.size());

        for (ElementImpl<D> element : this.children) {
            if (element.visibility().get()) {
                for (BoundingBox box : element.getBounds()) {
                    if (!box.isEmpty()) {
                        list.add(box);
                    }
                }
            }
        }

        return new BoundingBoxes(list);
    }

    @Override
    public VisibilityProperty<Group<D>> visibility() {
        return this.visibility;
    }

    @Override
    public void addChild(ElementImpl<D> child) {
        this.children.add(child);
    }

    @NotNull
    @Override
    public Iterator<Element<D>> iterator() {
        return new UnmodifiableIterator<>(this.children);
    }

    @Override
    public boolean isVisible() {
        return this.visibility.get();
    }
}
