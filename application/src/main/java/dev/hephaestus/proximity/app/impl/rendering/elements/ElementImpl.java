package dev.hephaestus.proximity.app.impl.rendering.elements;

import dev.hephaestus.proximity.app.api.RenderJob;
import dev.hephaestus.proximity.app.api.rendering.elements.Element;
import dev.hephaestus.proximity.app.api.rendering.util.BoundingBoxes;
import dev.hephaestus.proximity.app.api.rendering.util.Stateful;
import dev.hephaestus.proximity.app.impl.rendering.DocumentImpl;

import java.util.ArrayDeque;
import java.util.function.Predicate;

public abstract class ElementImpl<D extends RenderJob> implements Element<D>, Stateful {
    private final DocumentImpl<D> document;
    private final String id;
    private final String path;
    private final ElementImpl<D> parent;

    public ElementImpl(DocumentImpl<D> document, String id, ElementImpl<D> parent) {
        this.document = document;
        this.id = id == null ? parent.getId() : id;
        this.parent = parent;

        ArrayDeque<String> builder = new ArrayDeque<>();

        builder.add(this.id);

        while (parent != null) {
            //noinspection StringEquality
            if (parent.id != builder.peekFirst()) {
                builder.addFirst(parent.id);
            }

            parent = parent.parent;
        }

        this.path = String.join("/", builder);
    }

    @Override
    public final String getId() {
        return this.id;
    }

    @Override
    public final String getPath() {
        return this.path;
    }

    public final DocumentImpl<D> getDocument() {
        return this.document;
    }

    public abstract BoundingBoxes getBounds();
    public abstract VisibilityProperty<?> visibility();

    public boolean isAlwaysVisible() {
        return this.visibility().isAlwaysVisible();
    }

    public interface Constructor<D extends RenderJob, E extends ElementImpl<D>> {
        E construct(DocumentImpl<D> document, String id, ElementImpl<D> parent);
    }

    protected class VisibilityProperty<R extends Stateful> implements dev.hephaestus.proximity.app.api.rendering.properties.VisibilityProperty<D, R> {
        private final D data;
        private final R result;

        private boolean always = false;
        private Predicate<D> value = d -> true;

        public VisibilityProperty(R result, D data) {
            this.data = data;
            this.result = result;
        }

        @Override
        public boolean get() {
            return this.value.test(this.data);
        }

        @Override
        public R set(boolean value) {
            this.value = d -> value;

            return this.result;
        }

        @Override
        public R set(Predicate<D> getter) {
            this.value = getter;
            this.always = false;

            return this.result;
        }

        private boolean isAlwaysVisible() {
            return this.always;
        }
    }
}
