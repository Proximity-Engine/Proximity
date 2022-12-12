package dev.hephaestus.proximity.app.impl.rendering.elements;

import dev.hephaestus.proximity.app.api.Option;
import dev.hephaestus.proximity.app.api.Parent;
import dev.hephaestus.proximity.app.api.RenderJob;
import dev.hephaestus.proximity.app.api.rendering.elements.*;
import dev.hephaestus.proximity.app.api.rendering.util.BoundingBoxes;
import dev.hephaestus.proximity.app.api.rendering.util.ThrowingFunction;
import dev.hephaestus.proximity.app.api.text.TextStyle;
import dev.hephaestus.proximity.app.impl.rendering.DocumentImpl;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class SelectorImpl<D extends RenderJob<?>> extends ElementImpl<D> implements ParentImpl<D>, Selector<D> {
    private final List<Element<D>> children = new ArrayList<>(3);
    private final VisibilityProperty<Selector<D>> visibility;

    private Option<ElementImpl<D>, ?, D> option;

    public SelectorImpl(DocumentImpl<D> document, String id, ElementImpl<D> parent) {
        super(document, id, parent);

        this.visibility = new VisibilityProperty<Selector<D>>(this, document.getData());
    }

    @Override
    public Image<D> image(String id, Predicate<D> visibilityPredicate) {
        return this.image(id).visibility().set(visibilityPredicate);
    }

    @Override
    public Text<D> text(Function<D, String> textGetter) {
        return this.text(null, textGetter);
    }

    @Override
    public Text<D> text() {
        return this.createElement(null, TextImpl<D>::new);
    }

    @Override
    public TextBox<D> textBox() {
        return this.textBox(null);
    }

    @Override
    public TextBox<D> textBox(int x, int y, int width, int height) {
        return this.textBox(null, x, y, width, height);
    }

    @Override
    public BoundingBoxes getBounds() {
        for (Element<D> element : this.children) {
            ElementImpl<D> e = (ElementImpl<D>) element;

            if (e.visibility().get()) {
                return e.getBounds();
            }
        }

        return BoundingBoxes.EMPTY;
    }

    @Override
    public VisibilityProperty<Selector<D>> visibility() {
        return this.visibility;
    }

    @Override
    public void addChild(ElementImpl<D> child) {
        this.children.add(child);
    }

    @Override
    public <E extends ElementImpl<D>> E createElement(String id, Constructor<D, E> constructor) {
        return ParentImpl.super.createElement(id, constructor);
    }

    @Override
    public Element<D> group(String id, Predicate<D> visibilityPredicate, Consumer<Group<D>> groupBuilder) {
        return ParentImpl.super.group(id, visibilityPredicate, groupBuilder);
    }

    @Override
    public Element<D> group(String id, Consumer<Group<D>> groupBuilder) {
        return ParentImpl.super.group(id, groupBuilder);
    }

    @Override
    public Element<D> select(String id, Predicate<D> visibilityPredicate, Consumer<Selector<D>> selectorBuilder) {
        return ParentImpl.super.select(id, visibilityPredicate, selectorBuilder);
    }

    @Override
    public Element<D> select(String id, Consumer<Selector<D>> selectorBuilder) {
        return ParentImpl.super.select(id, selectorBuilder);
    }

    @Override
    public Element<D> tree(Consumer<Tree<D>> treeBuilder, Consumer<Selector<D>> elementBuilder) {
        return ParentImpl.super.tree(treeBuilder, elementBuilder);
    }

    @Override
    public Element<D> tree(Consumer<Tree<D>> treeBuilder, Consumer<Selector<D>> elementBuilder, Predicate<D> visibilityPredicate) {
        return ParentImpl.super.tree(treeBuilder, elementBuilder, visibilityPredicate);
    }

    @Override
    public Element<D> tree(String id, Consumer<Tree<D>> treeBuilder, Consumer<Selector<D>> elementBuilder) {
        return ParentImpl.super.tree(id, treeBuilder, elementBuilder);
    }

    @Override
    public Element<D> tree(String id, Consumer<Tree<D>> treeBuilder, Consumer<Selector<D>> elementBuilder, Predicate<D> visibilityPredicate) {
        return ParentImpl.super.tree(id, treeBuilder, elementBuilder, visibilityPredicate);
    }

    @Override
    public Image<D> image(String id) {
        return ParentImpl.super.image(id);
    }

    @Override
    public Element<D> select(Predicate<D> visibilityPredicate, Consumer<Selector<D>> selectorBuilder) {
        return ParentImpl.super.select(null, visibilityPredicate, selectorBuilder);
    }

    @Override
    public Element<D> select(Consumer<Selector<D>> selectorBuilder) {
        return ParentImpl.super.select(null, d -> true, selectorBuilder);
    }

    @Override
    public Image<D> image(String id, String src) {
        return ParentImpl.super.image(id, src);
    }

    @Override
    public Image<D> image(String id, Option<String, ?, ? super D> src) {
        return ParentImpl.super.image(id, src);
    }

    @Override
    public Image<D> image(String id, String src, Predicate<D> visibilityPredicate) {
        return ParentImpl.super.image(id, src, visibilityPredicate);
    }

    @Override
    public Image<D> image(Function<D, String> id, Predicate<D> visibilityPredicate) {
        return ParentImpl.super.image(id, visibilityPredicate);
    }

    @Override
    public Image<D> image(ThrowingFunction<D, URL, IOException> srcGetter) {
        return this.image(null, srcGetter);
    }

    @Override
    public Image<D> image(String id, ThrowingFunction<D, URL, IOException> srcGetter) {
        return ParentImpl.super.image(id, srcGetter);
    }

    @Override
    public Text<D> text(String id, Function<D, String> textGetter, int x, int y, TextStyle base) {
        return ParentImpl.super.text(id, textGetter, x, y, base);
    }

    @Override
    public Text<D> text(String id, Function<D, String> textGetter) {
        return ParentImpl.super.text(id, textGetter);
    }

    @Override
    public Text<D> text(String id, String text) {
        return ParentImpl.super.text(id, text);
    }

    @Override
    public Text<D> text(String id) {
        return ParentImpl.super.text(id);
    }

    @Override
    public TextBox<D> textBox(String id) {
        return ParentImpl.super.textBox(id);
    }

    @Override
    public TextBox<D> textBox(String id, int x, int y, int width, int height) {
        return ParentImpl.super.textBox(id, x, y, width, height);
    }

    @Override
    public Optional<Element<D>> getSelected() {
        return Optional.ofNullable(this.getDocument().getData().getOption(this.option));
    }

    public Optional<Element<D>> getFirstVisible() {
        for (Element<D> node : this.children) {
            if (node instanceof Child<D> child && child.visibility().get() || node instanceof Parent<?> parent && parent.isVisible()) {
                return Optional.of(node);
            }
        }

        return Optional.empty();
    }

    @Override
    public boolean isVisible() {
        return this.visibility.get();
    }

    @NotNull
    @Override
    public Iterator<Element<D>> iterator() {
        return this.children.iterator();
    }

    public void setOption(Option<ElementImpl<D>, ?, D> option) {
        this.option = option;
    }
}
