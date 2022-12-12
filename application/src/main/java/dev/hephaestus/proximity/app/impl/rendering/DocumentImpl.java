package dev.hephaestus.proximity.app.impl.rendering;

import dev.hephaestus.proximity.app.api.Option;
import dev.hephaestus.proximity.app.api.RenderJob;
import dev.hephaestus.proximity.app.api.Template;
import dev.hephaestus.proximity.app.api.options.DropdownOption;
import dev.hephaestus.proximity.app.api.rendering.Document;
import dev.hephaestus.proximity.app.api.rendering.elements.Element;
import dev.hephaestus.proximity.app.api.rendering.properties.VisibilityProperty;
import dev.hephaestus.proximity.app.impl.rendering.elements.ElementImpl;
import dev.hephaestus.proximity.app.impl.rendering.elements.GroupImpl;
import dev.hephaestus.proximity.app.impl.rendering.elements.ParentImpl;
import dev.hephaestus.proximity.app.impl.rendering.elements.SelectorImpl;
import dev.hephaestus.proximity.json.api.Json;
import dev.hephaestus.proximity.utils.UnmodifiableIterator;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public class DocumentImpl<D extends RenderJob<?>> implements ParentImpl<D>, Document<D> {
    private final D data;
    private final Template<D> template;
    private final List<ElementImpl<D>> children = new ArrayList<>(5);
    private final List<Option<ElementImpl<D>, ?, D>> selectorOverrides = new ArrayList<>();
    private final ObservableList<String> errors;

    public DocumentImpl(D data, Template<D> template, ObservableList<String> errors) {
        this.data = data;
        this.template = template;
        this.template.createLayers(this);

        for (ElementImpl<D> element : this.children) {
            this.initialize(element);
        }

        this.errors = errors;
    }

    public ObservableList<String> getErrors() {
        return this.errors;
    }

    private void initialize(ElementImpl<D> element) {
        if (element instanceof GroupImpl<D> group) {
            for (Element<D> e : group) {
                this.initialize((ElementImpl<D>) e);
            }
        } else if (element instanceof SelectorImpl<D> selector) {
            String[] path = element.getPath().split("/");
            DropdownOption.Builder<ElementImpl<D>, D> builder = DropdownOption.builder(path[path.length - 1], ElementImpl::getPath, e -> Json.create(e.getPath()), json -> null /* TODO */);
            Optional<Element<D>> firstVisible = selector.getFirstVisible();
            int rootOffset = selector.getPath().length() + 1;

            boolean hasDefault = false;

            for (Element<D> e : selector) {
                this.addChildren(rootOffset, (ElementImpl<D>) e, builder, selector.isVisible() && firstVisible.isPresent() && e == firstVisible.get());

                if (selector.isVisible() && ((ElementImpl<D>) e).isAlwaysVisible()) {
                    hasDefault = true;
                }
            }

            if (!hasDefault) {
                builder.add("None", null, d -> true);
            }

            Option<ElementImpl<D>, ?, D> option = builder.build();
            selector.setOption(option);
            this.selectorOverrides.add(option);
        }
    }

    private void addChildren(int rootOffset, ElementImpl<D> element, DropdownOption.Builder<ElementImpl<D>, ?> builder, boolean isFirstVisible) {
        if (element instanceof SelectorImpl<D> selector) {
            Optional<Element<D>> firstVisible = selector.getFirstVisible();

            for (Element<D> e : selector) {
                this.addChildren(rootOffset, (ElementImpl<D>) e, builder, selector.isVisible() && isFirstVisible && firstVisible.isPresent() && e == firstVisible.get());
            }
        } else {
            ((VisibilityProperty<D, ?>) element.visibility()).set(true);

            builder.add(element.getPath().substring(rootOffset), element, d -> isFirstVisible);
        }
    }

    @Override
    public void addChild(ElementImpl<D> child) {
        this.children.add(child);
    }

    @Override
    public DocumentImpl<D> getDocument() {
        return this;
    }

    @NotNull
    @Override
    public Iterator<Element<D>> iterator() {
        return new UnmodifiableIterator<>(this.children);
    }

    public D getData() {
        return this.data;
    }

    public Template<D> getTemplate() {
        return this.template;
    }

    @Override
    public InputStream getResource(String src, String... alternateFileExtensions) {
        String path = src;

        // Check for files when the file extension is not specified
        for (int i = 0; !this.getTemplate().hasResource(path) && i < alternateFileExtensions.length; i++) {
            path = src + "." + alternateFileExtensions[i];
        }

        if (this.getTemplate().hasResource(path)) {
            return this.getTemplate().getResource(path);
        } else {
            return this.getTemplate().getResource(src);
        }
    }

    @Override
    public boolean isVisible() {
        return true;
    }

    public Iterable<Option<ElementImpl<D>, ?, D>> getSelectorOverrides() {
        return this.selectorOverrides;
    }
}
