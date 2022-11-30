package dev.hephaestus.proximity.app.api;

import dev.hephaestus.proximity.app.api.text.TextStyle;
import dev.hephaestus.proximity.app.api.rendering.elements.*;
import dev.hephaestus.proximity.app.api.rendering.util.ThrowingFunction;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public interface Parent<D extends RenderJob> {
    Element<D> group(String id, Predicate<D> visibilityPredicate, Consumer<Group<D>> groupBuilder);
    Element<D>  group(String id, Consumer<Group<D>> groupBuilder);

    Element<D>  branch(String id, Function<D, String> branch, Predicate<D> visibilityPredicate, Consumer<Selector<D>> treeBuilder);
    Element<D>  branch(String id, Function<D, String> branch, Consumer<Selector<D>> treeBuilder);
    Element<D>  branch(Function<D, String> id, Predicate<D> visibilityPredicate, Consumer<Selector<D>> treeBuilder);
    Element<D>  branch(Function<D, String> id, Consumer<Selector<D>> treeBuilder);

    Element<D> select(String id, Predicate<D> visibilityPredicate, Consumer<Selector<D>> selectorBuilder);
    Element<D>  select(String id, Consumer<Selector<D>> selectorBuilder);
    Image<D> image(String id);
    Image<D> image(String id, String src);
    Image<D> image(String id, Option<String, ?, ? super D> src);
    Image<D> image(String id, String src, Predicate<D> visibilityPredicate);
    Image<D> image(Function<D, String> id, Predicate<D> visibilityPredicate);
    Image<D> image(String id, ThrowingFunction<D, URL, MalformedURLException> srcGetter);
    Text<D> text(String id, Function<D, String> textGetter, int x, int y, TextStyle base);
    Text<D> text(String id, Function<D, String> textGetter);
    Text<D> text(String id, String text);
    Text<D> text(String id);

    TextBox<D> textBox(String id);

    TextBox<D> textBox(String id, int x, int y, int width, int height);

    boolean isVisible();
}
