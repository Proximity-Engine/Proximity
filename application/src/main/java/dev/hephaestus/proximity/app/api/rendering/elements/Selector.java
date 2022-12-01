package dev.hephaestus.proximity.app.api.rendering.elements;

import dev.hephaestus.proximity.app.api.Option;
import dev.hephaestus.proximity.app.api.RenderJob;
import dev.hephaestus.proximity.app.api.rendering.util.ThrowingFunction;
import org.jetbrains.annotations.ApiStatus;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

@ApiStatus.NonExtendable
public interface Selector<D extends RenderJob> extends Element<D> {
    Element<D> group(String id, Predicate<D> visibilityPredicate, Consumer<Group<D>> groupBuilder);
    Element<D> group(String id, Consumer<Group<D>> groupBuilder);

    Element<D> branch(String id, Function<D, String> branch, Predicate<D> visibilityPredicate, Consumer<Selector<D>> treeBuilder);
    Element<D> branch(String id, Function<D, String> branch, Consumer<Selector<D>> treeBuilder);
    Element<D> branch(Function<D, String> branch, Predicate<D> visibilityPredicate, Consumer<Selector<D>> treeBuilder);
    Element<D> branch(Function<D, String> branch, Consumer<Selector<D>> treeBuilder);

    Element<D> select(String id, Predicate<D> visibilityPredicate, Consumer<Selector<D>> selectorBuilder);
    Element<D> select(String id, Consumer<Selector<D>> selectorBuilder);
    Element<D> select(Predicate<D> visibilityPredicate, Consumer<Selector<D>> selectorBuilder);
    Element<D> select(Consumer<Selector<D>> selectorBuilder);

    Image<D> image(String id);
    Image<D> image(String id, Predicate<D> visibilityPredicate);
    Image<D> image(String id, String src);
    Image<D> image(String id, Option<String, ?, ? super D> src);
    Image<D> image(String id, String src, Predicate<D> visibilityPredicate);
    Image<D> image(Function<D, String> id, Predicate<D> visibilityPredicate);
    Image<D> image(ThrowingFunction<D, InputStream, IOException> srcGetter);

    Text<D> text(Function<D, String> textGetter);
    Text<D> text();

    TextBox<D> textBox();

    TextBox<D> textBox(int x, int y, int width, int height);

    Optional<? extends Element<?>> getSelected();

    boolean isVisible();
}