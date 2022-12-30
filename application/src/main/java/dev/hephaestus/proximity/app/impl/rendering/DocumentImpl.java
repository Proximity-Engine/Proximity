package dev.hephaestus.proximity.app.impl.rendering;

import dev.hephaestus.proximity.app.api.Option;
import dev.hephaestus.proximity.app.api.options.DropdownOption;
import dev.hephaestus.proximity.app.api.rendering.Document;
import dev.hephaestus.proximity.app.api.rendering.RenderData;
import dev.hephaestus.proximity.app.api.rendering.Template;
import dev.hephaestus.proximity.app.api.rendering.elements.Element;
import dev.hephaestus.proximity.app.api.rendering.elements.Group;
import dev.hephaestus.proximity.app.impl.rendering.elements.ElementImpl;
import dev.hephaestus.proximity.app.impl.rendering.elements.GroupImpl;
import dev.hephaestus.proximity.app.impl.rendering.elements.SelectorImpl;
import dev.hephaestus.proximity.json.api.Json;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DocumentImpl<D extends RenderData> implements Document<D> {
    private final D data;
    private final Template<D> template;
    private final GroupImpl<D> root = new GroupImpl<>(null, this, null);
    private final List<Option<ElementImpl<D>, ?, D>> selectorOverrides = new ArrayList<>();
    private final ObservableList<String> errors;

    public DocumentImpl(D data, Template<D> template, ObservableList<String> errors) {
        this.data = data;
        this.template = template;
        this.errors = errors;
        this.template.build(this.data, this.root);

        for (Element element : this.root) {
            this.initialize((ElementImpl<D>) element);
        }
    }

    @Override
    public ObservableList<String> getErrors() {
        return this.errors;
    }

    @Override
    public Group getElements() {
        return this.root;
    }

    private void initialize(ElementImpl<D> element) {
        if (element instanceof GroupImpl<D> group) {
            for (Element e : group) {
                this.initialize((ElementImpl<D>) e);
            }
        } else if (element instanceof SelectorImpl<D> selector) {
            DropdownOption.Builder<ElementImpl<D>, D> builder = DropdownOption.builder(element.path, ElementImpl::getPath, e -> Json.create(e.getPath()), json -> null /* TODO */);
            Optional<Element> firstVisible = selector.getSelected();
            int rootOffset = selector.path.length() + 1;

            boolean hasDefault = false;

            for (ElementImpl<D> e : selector.children()) {
                this.addChildren(rootOffset, e, builder, selector.isVisible() && firstVisible.isPresent() && e == firstVisible.get());

                if (selector.isVisible() && e.isAlwaysVisible()) {
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
            Optional<Element> firstVisible = selector.getSelected();

            for (ElementImpl<D> e : selector.children()) {
                this.addChildren(rootOffset, e, builder, selector.isVisible() && isFirstVisible && firstVisible.isPresent() && e == firstVisible.get());
            }
        } else {
//            element.visibility(true);

            builder.add(element.getPath().substring(rootOffset), element, d -> isFirstVisible);
        }
    }

    @Override
    public D getRenderData() {
        return this.data;
    }

    @Override
    public Template<D> getTemplate() {
        return this.template;
    }

    public Iterable<Option<ElementImpl<D>, ?, D>> getSelectorOverrides() {
        return this.selectorOverrides;
    }
}
