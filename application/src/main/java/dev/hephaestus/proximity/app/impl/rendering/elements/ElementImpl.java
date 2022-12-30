package dev.hephaestus.proximity.app.impl.rendering.elements;

import dev.hephaestus.proximity.app.api.rendering.Document;
import dev.hephaestus.proximity.app.api.rendering.RenderData;
import dev.hephaestus.proximity.app.api.rendering.elements.Element;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;

import java.util.ArrayDeque;
import java.util.function.Consumer;

public abstract class ElementImpl<D extends RenderData> implements Element {
    public final String id;
    public final String path;
    public final Document<D> document;
    public final ParentImpl<D> parent;

    protected final BooleanProperty visibility = new SimpleBooleanProperty(true);

    private boolean isAlwaysVisible = true;

    public ElementImpl(String id, Document<D> document, ParentImpl<D> parent) {
        this.id = id;
        this.document = document;
        this.parent = parent;

        ArrayDeque<String> builder = new ArrayDeque<>();

        if (this.id != null) {
            builder.add(this.id);
        }

        while (parent != null) {
            //noinspection StringEquality
            if (parent.id != null && parent.id != builder.peekFirst()) {
                builder.addFirst(parent.id);
            }

            parent = parent.parent;
        }

        this.path = String.join("/", builder);
    }

    protected abstract void getAttributes(Consumer<Observable> attributes);

    protected abstract Node render();

    @Override
    public final void visibility(boolean visible) {
        this.visibility.setValue(visible);
        this.isAlwaysVisible = false;
    }

    public final void bindVisibility(ObservableValue<? extends Boolean> binding) {
        this.visibility.bind(binding);
        this.isAlwaysVisible = false;
    }

    @Override
    public final boolean isVisible() {
        return this.visibility.get();
    }

    public final boolean isAlwaysVisible() {
        return this.isAlwaysVisible;
    }

    public String getPath() {
        return this.path;
    }

    interface Constructor<D extends RenderData, E extends ElementImpl<D>> {
        E construct(String id, Document<D> document, ParentImpl<D> parent);
    }

}
